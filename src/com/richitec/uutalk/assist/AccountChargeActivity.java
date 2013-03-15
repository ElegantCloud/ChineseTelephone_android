package com.richitec.uutalk.assist;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.richitec.commontoolkit.activityextension.NavigationActivity;
import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.uutalk.R;
import com.richitec.uutalk.assist.charge.AlipayWapActivity;
import com.richitec.uutalk.assist.charge.UUTalkCardChargeActivity;
import com.richitec.uutalk.assist.charge.YeepayChargeActivity;
import com.richitec.uutalk.constant.ChargeType;
import com.richitec.uutalk.constant.TelUser;
import com.richitec.uutalk.utils.AppDataSaveRestoreUtil;

public class AccountChargeActivity extends NavigationActivity {
	private ProgressDialog mProgress = null;

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

	// close the progress bar
	// 关闭进度框
	private void closeProgress() {
		try {
			if (mProgress != null) {
				mProgress.dismiss();
				mProgress = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			closeProgress();
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
			closeProgress();
			// MyToast.show(AccountChargeActivity.this,
			// R.string.get_balance_error, Toast.LENGTH_SHORT);
		}
	};

	public void aliPayBtnAction(View v) {
		pushActivity(AlipayWapActivity.class);
	}

	public void onCardChargeBtnClick(View v) {
		pushActivity(UUTalkCardChargeActivity.class);
	}
	
	public void onSZXCardCharge(View v) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("charge_type", ChargeType.szx_card.name());
		pushActivity(YeepayChargeActivity.class, data);
	}
	
	public void onUnicomCardCharge(View v) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("charge_type", ChargeType.unicom_card.name());
		pushActivity(YeepayChargeActivity.class, data);
	}

	public void onTelecomCardCharge(View v) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("charge_type", ChargeType.telecom_card.name());
		pushActivity(YeepayChargeActivity.class, data);
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
