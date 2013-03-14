package com.richitec.uutalk.assist;

import android.os.Bundle;

import com.richitec.uutalk.R;

public class FeeActivity extends WebViewActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set title text
		setTitle(R.string.fee_query_item);
		loadUrl(getString(R.string.fee_query_url));

	}

}
