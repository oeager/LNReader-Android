package com.erakk.lnreader.helper;

import android.content.SyncResult;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;

public class BakaTsukiWebChromeClient extends WebChromeClient {
	private static final String TAG = BakaTsukiWebChromeClient.class.toString();
	private static final String HIGHLIGHT_EVENT = "HIGHLIGHT_EVENT";
	private static final String ADD = "highlighted";
	private static final String REMOVE = "clear";
	private static final String SCROLL_EVENT = "SCROLL_EVENT";
	private DisplayLightNovelContentActivity caller;
	
	public BakaTsukiWebChromeClient(DisplayLightNovelContentActivity caller) {
		super();
		this.caller = caller;
	}
	
	@Override
	public boolean onConsoleMessage (ConsoleMessage consoleMessage) {
		if(consoleMessage.message().startsWith(HIGHLIGHT_EVENT)) {
			String data[] = consoleMessage.message().split(":");
			BookmarkModel bookmark = new BookmarkModel();
			bookmark.setPage(caller.content.getPage());
			bookmark.setpIndex(Integer.parseInt(data[1]));
			if(data[2].equalsIgnoreCase(ADD)) {
				bookmark.setExcerpt(data[3]);
				NovelsDao.getInstance(caller).addBookmark(bookmark);
			}
			else if(data[2].equalsIgnoreCase(REMOVE)) {
				NovelsDao.getInstance(caller).deleteBookmark(bookmark);
			}
			caller.refreshBookmarkData();
		}
		else if (consoleMessage.message().startsWith(SCROLL_EVENT)) {
			String data[] = consoleMessage.message().split(":");
			caller.updateLastLine(Integer.parseInt(data[1]));						
		}
		else {
			Log.d(TAG, "Console: " + consoleMessage.lineNumber() + ":" + consoleMessage.message());
		}
		return true;
	}
	
	@Override
	public boolean onJsAlert (WebView view, String url, String message, JsResult result) {
		Log.d(TAG, "JSAlert: " + message);
		return true;
	}
}