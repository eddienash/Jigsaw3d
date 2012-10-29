package com.toychest3d.Jigsaw3d.Help;

import com.toychest3d.Jigsaw3d.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.help);

		mWebView = ((WebView) findViewById(R.id.txtHelp));
		mWebView.loadUrl("file:///android_asset/html/help.html");
	}

	@Override
	public void onBackPressed() {

		if (mWebView.canGoBack())
			mWebView.goBack();

		else
			super.onBackPressed();
	}

	private WebView mWebView;
}
