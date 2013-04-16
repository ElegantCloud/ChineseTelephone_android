package com.richitec.uutalk.account;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.richitec.commontoolkit.user.User;
import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.DataStorageUtils;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.StringUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.commontoolkit.utils.MyToast;
import com.richitec.uutalk.R;
import com.richitec.uutalk.constant.SystemConstants;
import com.richitec.uutalk.constant.TelUser;
import com.richitec.uutalk.tab7tabcontent.ChineseTelephoneTabActivity;
import com.richitec.uutalk.utils.AppDataSaveRestoreUtil;
import com.richitec.uutalk.utils.CountryCodeManager;

public class AccountRegistActivity extends Activity {
	private AlertDialog chooseCountryDialog;
	private int lastSelect = 0;
	CountryCodeManager countryCodeManager;
	private ProgressDialog progressDlg;

	private String passwordText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.account_regist_layout_step1);

		countryCodeManager = CountryCodeManager.getInstance();

		EditText phoneET = (EditText) findViewById(R.id.regist_phone_edittext);
		String phone = phoneET.getText().toString().trim();
		if (phone == null || phone.equals("")) {
			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String number = tm.getLine1Number();
			if (number != null) {
				if (number.startsWith("+86") && number.length() > 3) {
					number = number.substring(3);
				}
				phoneET.setText(number);
			}
		}
	}

	public void chooseCountry(View v) {
		AlertDialog.Builder chooseCountryDialogBuilder = new AlertDialog.Builder(
				this);
		chooseCountryDialogBuilder.setTitle(R.string.countrycode_list);
		chooseCountryDialogBuilder.setSingleChoiceItems(
				countryCodeManager.getCountryNameList(), lastSelect,
				new chooseCountryListener());
		chooseCountryDialogBuilder.setNegativeButton(R.string.cancel, null);
		chooseCountryDialog = chooseCountryDialogBuilder.create();
		chooseCountryDialog.show();
	}

	public void onFinishRegist(View v) {
		EditText pwd1ET = (EditText) findViewById(R.id.regist_psw_edittext);
		EditText pwd2ET = (EditText) findViewById(R.id.verify_psw_edittext);
		String pwd1 = pwd1ET.getText().toString().trim();
		String pwd2 = pwd2ET.getText().toString().trim();
		passwordText = pwd1;
		// Log.d("psw", pwd1+":"+pwd2);

		if (pwd1 == null || pwd1.equals("")) {
			MyToast.show(this, R.string.pls_input_pwd, Toast.LENGTH_SHORT);
			return;
		}

		if (pwd2 == null || pwd2.equals("")) {
			MyToast.show(this, R.string.pls_input_confirm_pwd,
					Toast.LENGTH_SHORT);
			return;
		}

		if (!pwd1.equals(pwd2)) {
			MyToast.show(this, R.string.pwd1_is_different_from_pwd2,
					Toast.LENGTH_SHORT);
			return;
		}

		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.finishing_register));
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("password", pwd1);
		params.put("password1", pwd2);
		params.put("source", getString(R.string.client_source));

		params.put("brand", Build.BRAND);
		params.put("model", Build.MODEL);
		params.put("release", Build.VERSION.RELEASE);
		params.put("sdk", Build.VERSION.SDK);

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		int nowWidth = dm.widthPixels; // 当前分辨率 宽度
		int nowHeigth = dm.heightPixels; // 当前分辨率高度
		params.put("width", Integer.toString(nowWidth));
		params.put("height", Integer.toString(nowHeigth));

		HttpUtils.postRequest(getString(R.string.server_url)
				+ getString(R.string.user_register_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedRegister);

	}

	private OnHttpRequestListener onFinishedRegister = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();
			try {
				JSONObject data = new JSONObject(
						responseResult.getResponseText());
				String result = data.getString("result");
				if (result.equals("0")) {
					// login automatically
					try {
						String userName = data.getString("username");
						String countryCode = data.getString("countrycode");
						String userKey = data.getString("userkey");
						String vosphone = data.getString("vosphone");
						String vosphone_psw = data.getString("vosphone_pwd");
						String bindPhone = data.getString("bindphone");
						String bindPhoneCountryCode = data
								.getString("bindphone_country_code");
						String email = null;
						try {
							email = data.getString("email");
						} catch (JSONException e) {
						}
						Double regGivenMoney = data
								.getDouble("reg_given_money");

						UserBean telUser = UserManager.getInstance().getUser();
						telUser.setName(userName);
						telUser.setValue(TelUser.countryCode.name(),
								countryCode);
						telUser.setRememberPwd(true);
						telUser.setValue(TelUser.dialCountryCode.name(), countryCode);
						telUser.setPassword(StringUtils.md5(passwordText));
						telUser.setUserKey(userKey);
						telUser.setValue(TelUser.vosphone.name(), vosphone);
						telUser.setValue(TelUser.vosphone_pwd.name(),
								vosphone_psw);
						telUser.setValue(TelUser.bindphone.name(), bindPhone);
						telUser.setValue(TelUser.bindphone_country_code.name(),
								bindPhoneCountryCode);
						saveUserAccount();

						Intent intent = new Intent(AccountRegistActivity.this,
								ChineseTelephoneTabActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
								| Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra("email", email);
						intent.putExtra("reg_given_money", regGivenMoney);
						startActivity(intent);
						finish();

					} catch (JSONException e) {
						e.printStackTrace();
						new AlertDialog.Builder(AccountRegistActivity.this)
								.setTitle(R.string.alert_title)
								.setMessage(R.string.register_ok)
								.setPositiveButton(R.string.ok,
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												finish();
											}
										}).show();
					}

				} else if (result.equals("6")) {
					MyToast.show(AccountRegistActivity.this,
							R.string.register_timeout, Toast.LENGTH_SHORT);
					setBody(R.layout.account_regist_layout_step1);
				} else {
					MyToast.show(AccountRegistActivity.this,
							R.string.error_in_regsiter, Toast.LENGTH_SHORT);
				}
			} catch (Exception e) {
				e.printStackTrace();
				MyToast.show(AccountRegistActivity.this,
						R.string.error_in_regsiter, Toast.LENGTH_SHORT);
			}
		}

		private void saveUserAccount() {
			Log.d(SystemConstants.TAG, "save user account");
			UserBean user = UserManager.getInstance().getUser();
			Log.d(SystemConstants.TAG, "user: " + user.toString());
			DataStorageUtils.putObject(User.username.name(), user.getName());
			DataStorageUtils.putObject(TelUser.countryCode.name(),
					user.getValue(TelUser.countryCode.name()));
			DataStorageUtils.putObject(TelUser.dialCountryCode.name(),
					user.getValue(TelUser.dialCountryCode.name()));

			DataStorageUtils.putObject(TelUser.vosphone.name(),
					user.getValue(TelUser.vosphone.name()));
			DataStorageUtils.putObject(TelUser.vosphone_pwd.name(),
					user.getValue(TelUser.vosphone_pwd.name()));
			DataStorageUtils.putObject(User.userkey.name(), user.getUserKey());
			DataStorageUtils.putObject(TelUser.bindphone.name(),
					user.getValue(TelUser.bindphone.name()));
			DataStorageUtils.putObject(TelUser.bindphone_country_code.name(),
					user.getValue(TelUser.bindphone_country_code.name()));
			if (user.isRememberPwd()) {
				DataStorageUtils.putObject(User.password.name(),
						user.getPassword());
			} else {
				DataStorageUtils.putObject(User.password.name(), "");
			}
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(AccountRegistActivity.this,
					R.string.error_in_regsiter, Toast.LENGTH_SHORT);
		}
	};

	public void onVerifyAuthCodeAction(View v) {
		String authcode = ((EditText) (findViewById(R.id.auth_code_edittext)))
				.getText().toString().trim();
		if (authcode == null || authcode.equals("")) {
			MyToast.show(AccountRegistActivity.this,
					R.string.auth_code_cannot_be_null, Toast.LENGTH_SHORT);
			return;
		}
		if (!authcode.matches("(^[0-9]*)")) {
			MyToast.show(AccountRegistActivity.this,
					R.string.authcode_wrong_format, Toast.LENGTH_SHORT);
			return;
		}

		// Log.d("authcode", authcode);

		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.verifying_auth_code));
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("code", authcode);
		HttpUtils.postRequest(getString(R.string.server_url)
				+ getString(R.string.check_auth_code_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedVerifyAuthCode);
		// setBody(R.layout.account_regist_layout_step3);
	}

	private OnHttpRequestListener onFinishedVerifyAuthCode = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();

			try {
				JSONObject data = new JSONObject(
						responseResult.getResponseText());
				String result = data.getString("result");

				if (result.equals("0")) {
					// check phone code successfully, jump to step 3 to fill
					// password
					setBody(R.layout.account_regist_layout_step3);
				} else if (result.equals("2")) {
					MyToast.show(AccountRegistActivity.this,
							R.string.wrong_auth_code, Toast.LENGTH_SHORT);
				} else if (result.equals("6")) {
					MyToast.show(AccountRegistActivity.this,
							R.string.auth_code_timeout, Toast.LENGTH_SHORT);
					setBody(R.layout.account_regist_layout_step1);
				}

			} catch (Exception e) {
				e.printStackTrace();
				MyToast.show(AccountRegistActivity.this, R.string.auth_error,
						Toast.LENGTH_SHORT);
			}
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(AccountRegistActivity.this, R.string.auth_error,
					Toast.LENGTH_SHORT);
		}
	};

	public void onGetAuthCode(View v) {
		String phone = ((EditText) (findViewById(R.id.regist_phone_edittext)))
				.getText().toString().trim();
		// String countrycode = countryCodeManager
		// .getCountryCode(((Button)
		// findViewById(R.id.regist_choose_country_btn))
		// .getText().toString().trim());
		String countrycode = getResources().getString(
				R.string.default_country_code);
		if (countrycode == null) {
			MyToast.show(this, R.string.pls_select_country, Toast.LENGTH_SHORT);
			return;
		}

		if (phone == null || phone.equals("")) {
			MyToast.show(AccountRegistActivity.this,
					R.string.number_cannot_be_null, Toast.LENGTH_SHORT);
			return;
		} else if (countryCodeManager.hasCountryCodePrefix(phone)) {
			MyToast.show(this,
					R.string.phone_number_cannot_start_with_countrycode,
					Toast.LENGTH_SHORT);
			return;
		}
		if (!phone.matches("(^[0-9]*)")) {
			MyToast.show(AccountRegistActivity.this,
					R.string.phone_wrong_format, Toast.LENGTH_SHORT);
			return;
		}
		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.sending_request));

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("phone", phone);
		params.put("countryCode", countrycode);
		HttpUtils.postRequest(getString(R.string.server_url)
				+ getString(R.string.retrieve_auth_code_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedGetAuthCode);
		// setBody(R.layout.account_regist_layout_step2);
	}

	private OnHttpRequestListener onFinishedGetAuthCode = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();

			try {
				JSONObject data = new JSONObject(
						responseResult.getResponseText());
				String result = data.getString("result");

				if (result.equals("0")) {
					// get phone code successfully, jump to step 2
					setBody(R.layout.account_regist_layout_step2);
				} else if (result.equals("2")) {
					MyToast.show(AccountRegistActivity.this,
							R.string.invalid_phone_number, Toast.LENGTH_SHORT);
				} else if (result.equals("3")) {
					MyToast.show(AccountRegistActivity.this,
							R.string.existed_phone_number, Toast.LENGTH_SHORT);
				} else {
					MyToast.show(AccountRegistActivity.this,
							R.string.error_in_retrieve_auth_code,
							Toast.LENGTH_SHORT);
				}

			} catch (Exception e) {
				e.printStackTrace();
				MyToast.show(AccountRegistActivity.this,
						R.string.error_in_retrieve_auth_code,
						Toast.LENGTH_SHORT);
			}

		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(AccountRegistActivity.this,
					R.string.error_in_retrieve_auth_code, Toast.LENGTH_SHORT);
		}
	};

	private void setBody(int resID) {
		LinearLayout body = (LinearLayout) getBody();
		body.removeAllViewsInLayout();
		LayoutInflater.from(this).inflate(resID, body);
	}

	public LinearLayout getBody() {
		return (LinearLayout) findViewById(R.id.body);
	}

	private void dismissProgressDlg() {
		if (progressDlg != null)
			progressDlg.dismiss();
	}

	class chooseCountryListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			lastSelect = which;
			((Button) (AccountRegistActivity.this
					.findViewById(R.id.regist_choose_country_btn)))
					.setText(countryCodeManager.getCountryName(which));
			chooseCountryDialog.dismiss();
		}

	}

	class OnChangeEditTextBGListener implements OnFocusChangeListener {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			// TODO Auto-generated method stub
			if (hasFocus) {
				((LinearLayout) v.getParent())
						.setBackgroundDrawable(AccountRegistActivity.this
								.getResources().getDrawable(
										R.drawable.bg_edit_s));
			} else {
				((LinearLayout) v.getParent())
						.setBackgroundDrawable(AccountRegistActivity.this
								.getResources().getDrawable(
										R.drawable.bg_edityzm));
			}
		}
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

	@Override
	public void onBackPressed() {
		startActivity(new Intent(this, AccountSettingActivity.class));
		finish();
	}
	
	
}