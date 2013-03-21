package com.richitec.chinesetelephone.assist;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.richitec.chinesetelephone.R;
import com.richitec.chinesetelephone.assist.charge.AlipayClientChargeActivity;
import com.richitec.chinesetelephone.assist.charge.UUTalkCardChargeActivity;
import com.richitec.chinesetelephone.constant.TelUser;
import com.richitec.chinesetelephone.utils.AppDataSaveRestoreUtil;
import com.richitec.commontoolkit.activityextension.NavigationActivity;
import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;

public class AccountChargeActivity extends NavigationActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_charge_layout);

		setTitle(R.string.charge_title_popwin);

	}

	@Override
	protected void onResume() {
		super.onResume();
		getRemainMoney();
	}

	private void getRemainMoney() {
		UserBean userBean = UserManager.getInstance().getUser();
		String username = userBean.getName();
		String countryCode = (String) userBean.getValue(TelUser.countryCode
				.name());

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("username", username);
		params.put("countryCode", countryCode);

		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.account_balance_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedGetBalance);
	}

	private OnHttpRequestListener onFinishedGetBalance = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			JSONObject data;
			try {
				data = new JSONObject(responseResult.getResponseText());
				double balance = AccountInfoActivity.formatRemainMoney(data
						.getDouble("balance") + "");
				String remainBalanceStr = AccountChargeActivity.this
						.getString(R.string.remain_balance_textfield);
				remainBalanceStr += getString(R.string.currency_sign) + balance
						+ getString(R.string.yuan);
				((TextView) findViewById(R.id.remain_balance))
						.setText(remainBalanceStr);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
		}
	};

	public void aliPayBtnAction(View v) {

		pushActivity(AlipayClientChargeActivity.class);
	}

	public void onCardChargeBtnClick(View v) {
		pushActivity(UUTalkCardChargeActivity.class);
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
