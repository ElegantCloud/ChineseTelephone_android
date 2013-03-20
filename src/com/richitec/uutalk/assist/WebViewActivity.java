package com.richitec.uutalk.assist;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.richitec.commontoolkit.activityextension.NavigationActivity;
import com.richitec.uutalk.R;
import com.richitec.uutalk.utils.AppDataSaveRestoreUtil;

public class WebViewActivity extends NavigationActivity {
	private int maxNumber;
	private LinearLayout loadingLayout;
	private ProgressBar progressBar;
	private TextView loadingTV;
	private WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set content view
		setContentView(R.layout.webview_activity_layout);

		maxNumber = Integer
				.parseInt(getString(R.string.fee_loading_progressBar_max));

		loadingLayout = (LinearLayout) findViewById(R.id.loading_linearLayout);
		progressBar = (ProgressBar) findViewById(R.id.loading_progressBar);
		loadingTV = (TextView) findViewById(R.id.loading_textView);
		// get support webView
		webView = (WebView) findViewById(R.id.webView);
		webView.setWebViewClient(new MyWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true); 
		// add web chrome client for loading progress changed
		webView.setWebChromeClient(new WebChromeClient() {

			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				super.onProgressChanged(view, newProgress);

				// set support loading progressBar progress
				progressBar.setProgress(newProgress);

				// set support loading textView text
				loadingTV
						.setText(getString(R.string.fee_loading_textView_textHeader)
								+ newProgress + "%");

				// check support page loading completed
				if (maxNumber == newProgress) {
					// support loading completed, remove support loading
					// linearLayout
					loadingLayout.setVisibility(View.GONE);
				}
			}

		});
	}

	public void loadUrl(String url) {
		// load support url
		webView.clearCache(true);
		webView.loadUrl(url);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		AppDataSaveRestoreUtil.onRestoreInstanceState(savedInstanceState);
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		AppDataSaveRestoreUtil.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}
	
	class MyWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
		
	}
}
