package com.richitec.chinesetelephone.assist;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.richitec.chinesetelephone.R;
import com.richitec.chinesetelephone.call.SipCallMode;
import com.richitec.chinesetelephone.sip.SipUtils;
import com.richitec.chinesetelephone.utils.AppDataSaveRestoreUtil;
import com.richitec.commontoolkit.activityextension.NavigationActivity;
import com.richitec.commontoolkit.utils.VersionUtils;

public class AboutActivity extends NavigationActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set content view
		setContentView(R.layout.about_activity_layout);

		// set title text
		setTitle(R.string.about_nav_title_text);

		// set product version name
		((TextView) findViewById(R.id.product_versionName_textView))
				.setText(VersionUtils.versionName());
	}

	public void onDialServicePhone(View v) {
		// make sip voice call
		SipUtils.makeSipVoiceCall(getString(R.string.service_phone_desc),
				getString(R.string.service_phone), SipCallMode.DIRECT_CALL);
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
}
