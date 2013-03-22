package com.richitec.chinesetelephone.account;

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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.richitec.chinesetelephone.R;
import com.richitec.chinesetelephone.constant.SystemConstants;
import com.richitec.chinesetelephone.constant.TelUser;
import com.richitec.chinesetelephone.tab7tabcontent.ChineseTelephoneTabActivity;
import com.richitec.chinesetelephone.utils.AppDataSaveRestoreUtil;
import com.richitec.chinesetelephone.utils.CountryCodeManager;
import com.richitec.commontoolkit.user.User;
import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.DataStorageUtils;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.commontoolkit.utils.MyToast;
import com.richitec.commontoolkit.utils.StringUtils;

public class AccountDirectRegisterActivity extends Activity {
	private AlertDialog chooseCountryDialog;
	private int lastSelect = 0;
	CountryCodeManager countryCodeManager;
	private ProgressDialog progressDlg;
	private String passwordText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.account_direct_regist);

		countryCodeManager = CountryCodeManager.getInstance();

		EditText phoneNumberET = (EditText) findViewById(R.id.regist_phone_edittext);
		EditText pwdET = (EditText) findViewById(R.id.regist_pwd_edittext);
		EditText pwd1ET = (EditText) findViewById(R.id.regist_pwd1_edittext);

		OnChangeEditTextBGListener list = new OnChangeEditTextBGListener();
		phoneNumberET.setOnFocusChangeListener(list);
		pwdET.setOnFocusChangeListener(list);
		pwd1ET.setOnFocusChangeListener(list);
	}

	public void chooseCountry(View v) {
		AlertDialog.Builder chooseCountryDialogBuilder = new AlertDialog.Builder(
				this);
		chooseCountryDialogBuilder.setTitle(R.string.countrycode_list);
		chooseCountryDialogBuilder.setSingleChoiceItems(
				countryCodeManager.getCountryNameList(), lastSelect,
				new ChooseCountryListener());
		chooseCountryDialogBuilder.setNegativeButton(R.string.cancel, null);
		chooseCountryDialog = chooseCountryDialogBuilder.create();
		chooseCountryDialog.show();
	}

	private void dismissProgressDlg() {
		if (progressDlg != null)
			progressDlg.dismiss();
	}

	class ChooseCountryListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			lastSelect = which;
			((Button) (AccountDirectRegisterActivity.this
					.findViewById(R.id.regist_choose_country_btn)))
					.setText(countryCodeManager.getCountryName(which));
			chooseCountryDialog.dismiss();
		}

	}

	public void onRegisterAction(View v) {
		hideSoftKeyboard();
		EditText phoneNumberET = (EditText) findViewById(R.id.regist_phone_edittext);
		EditText pwdET = (EditText) findViewById(R.id.regist_pwd_edittext);
		EditText pwd1ET = (EditText) findViewById(R.id.regist_pwd1_edittext);
		Button countryCodeBt = (Button) findViewById(R.id.regist_choose_country_btn);

		String country = countryCodeBt.getText().toString().trim();
		Log.d(SystemConstants.TAG, "country: " + country);
		String countryCode = countryCodeManager.getCountryCode(country);
		if (countryCode == null) {
			MyToast.show(this, R.string.pls_select_country, Toast.LENGTH_SHORT);
			return;
		}

		String phoneNumber = phoneNumberET.getText().toString().trim();
		String pwd = pwdET.getText().toString().trim();
		String pwd1 = pwd1ET.getText().toString().trim();

		passwordText = pwd;

		if (phoneNumber == null || phoneNumber.equals("")) {
			MyToast.show(this, R.string.number_cannot_be_null,
					Toast.LENGTH_SHORT);
			return;
		} else if (countryCodeManager.hasCountryCodePrefix(phoneNumber)) {
			MyToast.show(this,
					R.string.phone_number_cannot_start_with_countrycode,
					Toast.LENGTH_SHORT);
			return;
		}

		if (pwd == null || pwd.equals("")) {
			MyToast.show(this, R.string.pls_input_pwd, Toast.LENGTH_SHORT);
			return;
		}

		if (pwd1 == null || pwd1.equals("")) {
			MyToast.show(this, R.string.pls_input_confirm_pwd,
					Toast.LENGTH_SHORT);
			return;
		}

		if (!pwd.equals(pwd1)) {
			MyToast.show(this, R.string.pwd1_is_different_from_pwd2,
					Toast.LENGTH_SHORT);
			return;
		}

		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.finishing_register));

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("countryCode", countryCode);
		params.put("phoneNumber", phoneNumber);
		params.put("password", pwd);

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
				+ getString(R.string.direct_reg_url),
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
						telUser.setValue(TelUser.dialCountryCode.name(),
								countryCode);
						telUser.setPassword(StringUtils.md5(passwordText));
						telUser.setUserKey(userKey);
						telUser.setValue(TelUser.vosphone.name(), vosphone);
						telUser.setValue(TelUser.vosphone_pwd.name(),
								vosphone_psw);
						telUser.setValue(TelUser.bindphone.name(), bindPhone);
						telUser.setValue(TelUser.bindphone_country_code.name(),
								bindPhoneCountryCode);
						saveUserAccount();

						Intent intent = new Intent(
								AccountDirectRegisterActivity.this,
								ChineseTelephoneTabActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
								| Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra("email", email);
						intent.putExtra("reg_given_money", regGivenMoney);
						startActivity(intent);
						finish();

					} catch (JSONException e) {
						e.printStackTrace();
						new AlertDialog.Builder(
								AccountDirectRegisterActivity.this)
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

				} else if (result.equals("1")) {
					MyToast.show(AccountDirectRegisterActivity.this,
							R.string.number_cannot_be_null, Toast.LENGTH_SHORT);
				} else if (result.equals("2")) {
					MyToast.show(AccountDirectRegisterActivity.this,
							R.string.phone_wrong_format, Toast.LENGTH_SHORT);
				} else if (result.equals("3")) {
					MyToast.show(AccountDirectRegisterActivity.this,
							R.string.existed_phone_number, Toast.LENGTH_SHORT);
				} else if (result.equals("4")) {
					MyToast.show(AccountDirectRegisterActivity.this,
							R.string.pls_input_pwd, Toast.LENGTH_SHORT);
				} else {
					MyToast.show(AccountDirectRegisterActivity.this,
							R.string.error_in_regsiter, Toast.LENGTH_SHORT);
				}
			} catch (Exception e) {
				e.printStackTrace();
				MyToast.show(AccountDirectRegisterActivity.this,
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
			MyToast.show(AccountDirectRegisterActivity.this,
					R.string.error_in_regsiter, Toast.LENGTH_SHORT);
		}
	};

	class OnChangeEditTextBGListener implements OnFocusChangeListener {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			// TODO Auto-generated method stub
			if (hasFocus) {
				((LinearLayout) v.getParent())
						.setBackgroundDrawable(AccountDirectRegisterActivity.this
								.getResources().getDrawable(
										R.drawable.bg_edit_s));
			} else {
				((LinearLayout) v.getParent())
						.setBackgroundDrawable(AccountDirectRegisterActivity.this
								.getResources().getDrawable(
										R.drawable.bg_edityzm));
			}
		}

	}

	private void hideSoftKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
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
}
