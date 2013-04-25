package com.richitec.uutalk.assist;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.adsmogo.adapters.AdsMogoCustomEventPlatformEnum;
import com.adsmogo.adview.AdsMogoLayout;
import com.adsmogo.controller.listener.AdsMogoListener;
import com.adsmogo.util.AdsMogoTargeting;
import com.richitec.commontoolkit.activityextension.NavigationActivity;
import com.richitec.uutalk.R;
import com.richitec.uutalk.constant.SystemConstants;

public class AdShowActivity extends NavigationActivity implements AdsMogoListener {
	private AdsMogoLayout adsMogoLayoutFull;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.adshow_activity_layout);
		setTitle(R.string.click_ad_to_earn_money);
		
		LinearLayout adBody = (LinearLayout) findViewById(R.id.ad_body);
		adsMogoLayoutFull = new AdsMogoLayout(this, getString(R.string.mogo_id),
				AdsMogoTargeting.FULLSCREEN_AD);
		adsMogoLayoutFull.setAdsMogoListener(this);
		FrameLayout.LayoutParams paramsFull = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		adsMogoLayoutFull.setLayoutParams(paramsFull);
		adBody.addView(adsMogoLayoutFull);
	}
	
	@Override
	public Class getCustomEvemtPlatformAdapterClass(
			AdsMogoCustomEventPlatformEnum arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onClickAd(String arg0) {
		Log.d(SystemConstants.TAG, "AdShowActivity - onClickAd");
		
	}

	@Override
	public boolean onCloseAd() {
		Log.d(SystemConstants.TAG, "AdShowActivity - onCloseAd");
		return false;
	}

	@Override
	public void onCloseMogoDialog() {
		// TODO Auto-generated method stub
		Log.d(SystemConstants.TAG, "AdShowActivity - onCloseMogoDialog");
	}

	@Override
	public void onFailedReceiveAd() {
		// TODO Auto-generated method stub
		Log.d(SystemConstants.TAG, "AdShowActivity - onFailedReceiveAd");
	}

	@Override
	public void onRealClickAd() {
		// TODO Auto-generated method stub
		Log.d(SystemConstants.TAG, "AdShowActivity - onRealClickAd");
	}

	@Override
	public void onReceiveAd(ViewGroup arg0, String arg1) {
		// TODO Auto-generated method stub
		Log.d(SystemConstants.TAG, "AdShowActivity - onReceiveAd: " + arg1);
	}

	@Override
	public void onRequestAd(String arg0) {
		// TODO Auto-generated method stub
		Log.d(SystemConstants.TAG, "AdShowActivity - onRequestAd: " + arg0);
	}
}
