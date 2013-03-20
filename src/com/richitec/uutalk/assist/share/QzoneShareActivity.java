package com.richitec.uutalk.assist.share;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.richitec.uutalk.R;
import com.richitec.uutalk.assist.WebViewActivity;
import com.richitec.uutalk.constant.SystemConstants;

public class QzoneShareActivity extends WebViewActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String title = intent.getStringExtra("title");
		String url = intent.getStringExtra("url");
		String summary = intent.getStringExtra("summary");
		String images = intent.getStringExtra("images");

		// set title text
		setTitle(R.string.share_to_qzone);
		String requesturl = getString(R.string.server_url)
				+ getString(R.string.share_to_qzone_url) + "?title=" + title
				+ "&url=" + url + "&summary=" + summary + "&images=" + images;
		Log.d(SystemConstants.TAG, "share request url: " + requesturl);

		loadUrl(requesturl);

	}
}