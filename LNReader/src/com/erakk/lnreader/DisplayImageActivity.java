package com.erakk.lnreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import com.erakk.lnreader.callback.DownloadCallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.ImageModel;

public class DisplayImageActivity extends Activity {
	private NovelsDao dao = new NovelsDao(this);
	private WebView imgWebView;
	private LoadImageTask task;
	private boolean refresh = false;
	private String url;

	private ProgressDialog dialog;
	
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	// set before create any view
    	if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("invert_colors", false)) {    		
    		setTheme(R.style.AppTheme2);
    	}
    	else {
    		setTheme(R.style.AppTheme);
    	}
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        url = intent.getStringExtra(Constants.EXTRA_IMAGE_URL);
        
        imgWebView = (WebView) findViewById(R.id.webView1);
        imgWebView.getSettings().setAllowFileAccess(true);
        imgWebView.getSettings().setBuiltInZoomControls(true);
        imgWebView.getSettings().setLoadWithOverviewMode(true);
        imgWebView.getSettings().setUseWideViewPort(true);
        
        task = new LoadImageTask();
        task.execute(new String[] {url});
        
    }
    @Override
    protected void onStop() {
    	// check running task
    	if(task != null){
    		if(!(task.getStatus() == Status.FINISHED)) {
    			Toast.makeText(this, "Canceling task: " + task.toString(), Toast.LENGTH_SHORT).show();
    			task.cancel(true);    			
    		}
    	}
    	super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_display_image, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
			startActivity(launchNewIntent);
			return true;
		case R.id.menu_refresh_image:
			
			/*
			 * Implement code to refresh image content
			 */
			refresh = true;
			task = new LoadImageTask();
			task.execute(url);
			
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case android.R.id.home:
			//NavUtils.navigateUpFromSameTask(this);
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    @SuppressLint("NewApi")
	private void ToggleProgressBar(boolean show) {
		if(show) {
			dialog = ProgressDialog.show(this, "", "Loading. Please wait...", false);
//			dialog.setCanceledOnTouchOutside(true);
		
			if(refresh) {
				dialog.setMessage("Refreshing...");
			}
			else {
				dialog.setMessage("Loading...");
			}
		}
		else {
			dialog.dismiss();
		}
	}
    

	
    @SuppressLint("NewApi")
	public class LoadImageTask extends AsyncTask<String, ICallbackEventData, AsyncTaskResult<ImageModel>> implements ICallbackNotifier {
    	String url = "";
    	public String toString() {
    		return "LoadImageTask: " + url;
    	}
    	
    	public void onCallback(ICallbackEventData message) {
    		publishProgress(message);
    	}
    	
    	@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
    	
		@Override
		protected AsyncTaskResult<ImageModel> doInBackground(String... params) {
			this.url = params[0];			
			try{
				if(refresh) {
					return new AsyncTaskResult<ImageModel>(dao.getImageModelFromInternet(url, this));
				}
				else {
					return new AsyncTaskResult<ImageModel>(dao.getImageModel(url, this));
				}
			} catch (Exception e) {
				return new AsyncTaskResult<ImageModel>(e);
			}			
		}
		
		@Override
		protected void onProgressUpdate (ICallbackEventData... values){
			//executed on UI thread.
			ICallbackEventData data = values[0];
			dialog.setMessage(data.getMessage());

			if(data.getClass() == DownloadCallbackEventData.class) {
				DownloadCallbackEventData downloadData = (DownloadCallbackEventData) data;
				int percent = downloadData.getPercentage();
				synchronized (dialog) {
					if(percent > -1) {
						// somehow doesn't works....
						dialog.setIndeterminate(false);
						dialog.setSecondaryProgress(percent);
						dialog.setMax(100);
						dialog.setProgress(percent);
						dialog.setMessage(data.getMessage());
					}
					else {
						dialog.setIndeterminate(true);
						dialog.setMessage(data.getMessage());
					}
				}
			}
		}
		
		@Override
		protected void onPostExecute(AsyncTaskResult<ImageModel> result) {
			Exception e = result.getError();
			if(e == null) {
				imgWebView = (WebView) findViewById(R.id.webView1);
				String imageUrl = "file:///" + result.getResult().getPath(); 
				imgWebView.loadUrl(imageUrl);
				Log.d("LoadImageTask", "Loading: " + imageUrl);
			}
			else{
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), e.getClass() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			ToggleProgressBar(false);
		}
    }
}
