package com.richitec.uutalk.assist.charge;

import java.util.HashMap;

import org.apache.http.HttpStatus;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.richitec.commontoolkit.activityextension.NavigationActivity;
import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.commontoolkit.utils.MyToast;
import com.richitec.uutalk.R;
import com.richitec.uutalk.constant.SystemConstants;
import com.richitec.uutalk.constant.TelUser;
import com.richitec.uutalk.utils.AppDataSaveRestoreUtil;

public class UUTalkCardChargeActivity extends NavigationActivity {
	private ProgressDialog mProgress = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set content view
		setContentView(R.layout.uutalk_card_charge_layout);

		// set title text
		setTitle(R.string.card_charge);

	}

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

	public void onDoCardChargeClick(View v) {
		EditText numberET = (EditText) findViewById(R.id.card_number_et);
		EditText pwdET = (EditText) findViewById(R.id.card_pwd_et);
		String cardNumber = numberET.getText().toString().trim();
		String cardPwd = pwdET.getText().toString().trim();
		if (cardNumber == null || cardNumber.equals("")) {
			Toast.makeText(this, R.string.input_card_number, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		if (cardPwd == null || cardPwd.equals("")) {
			Toast.makeText(this, R.string.input_card_pwd, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
				.hideSoftInputFromWindow(pwdET.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);

		mProgress = ProgressDialog.show(this, null,
				getString(R.string.charging_now));
		UserBean user = UserManager.getInstance().getUser();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("pin", cardNumber);
		params.put("password", cardPwd);
		params.put("countryCode",
				(String) user.getValue(TelUser.countryCode.name()));
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.card_charge_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedCharge);
	}

	private OnHttpRequestListener onFinishedCharge = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			closeProgress();
			EditText numberET = (EditText) findViewById(R.id.card_number_et);
			EditText pwdET = (EditText) findViewById(R.id.card_pwd_et);
			numberET.setText("");
			pwdET.setText("");
			new AlertDialog.Builder(UUTalkCardChargeActivity.this)
					.setTitle(R.string.alert_title)
					.setMessage(R.string.charge_successfully)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).show();
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			closeProgress();
			int status = responseResult.getStatusCode();
			Log.d(SystemConstants.TAG, "status code: " + status);
			switch (status) {
			case HttpStatus.SC_NOT_FOUND:
				MyToast.show(UUTalkCardChargeActivity.this,
						R.string.charge_failed_no_account_exist,
						Toast.LENGTH_SHORT);
				break;

			case HttpStatus.SC_BAD_REQUEST:
				MyToast.show(UUTalkCardChargeActivity.this,
						R.string.charge_failed_invalid_card_number,
						Toast.LENGTH_SHORT);
				break;
			case HttpStatus.SC_CONFLICT:
				MyToast.show(UUTalkCardChargeActivity.this,
						R.string.card_already_used, Toast.LENGTH_SHORT);
				break;
			default:
				MyToast.show(UUTalkCardChargeActivity.this,
						R.string.charge_failed, Toast.LENGTH_SHORT);
				break;
			}

		}
	};

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
