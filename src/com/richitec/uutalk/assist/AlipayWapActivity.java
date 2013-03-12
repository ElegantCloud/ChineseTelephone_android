package com.richitec.uutalk.assist;

import android.os.Bundle;

import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.uutalk.R;
import com.richitec.uutalk.utils.AppDataSaveRestoreUtil;

public class AlipayWapActivity extends WebViewActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			AppDataSaveRestoreUtil.onRestoreInstanceState(savedInstanceState);
		}
		super.onCreate(savedInstanceState);

		// set title text
		setTitle(R.string.alipay_pattern);

		UserBean user = UserManager.getInstance().getUser();
		loadUrl(getString(R.string.server_url)
				+ getString(R.string.alipay_wap_charge_url) + "?username="
				+ user.getName());

	}
}
