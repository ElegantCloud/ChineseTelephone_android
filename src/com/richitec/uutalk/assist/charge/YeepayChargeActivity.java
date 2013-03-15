package com.richitec.uutalk.assist.charge;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
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
import com.richitec.uutalk.constant.ChargeMoneyConstants;
import com.richitec.uutalk.constant.ChargeType;
import com.richitec.uutalk.constant.TelUser;
import com.richitec.uutalk.utils.AppDataSaveRestoreUtil;
import com.richitec.uutalk.utils.ChargeUtil;
import com.richitec.uutalk.utils.YeepayUtils;
import com.yeepay.android.plugin.YeepayPlugin;

public class YeepayChargeActivity extends NavigationActivity {
	private String TAG = "YeepayChargeActivity";

	private static final String CUSTOMER_NUMBER = "10011958584";
	private final static String KEY = "y947j0qi9eJw7CyEr8395v9DcQ6Ww0w8X23eJc563kJ2prA709oS36GJ198H";
	private ProgressDialog mProgress = null;
	private ChargeMoneyListAdapter chargeMoneyListAdapter;
	private String chargeType;

	// private HashMap<String, String> productMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			AppDataSaveRestoreUtil.onRestoreInstanceState(savedInstanceState);
		}

		super.onCreate(savedInstanceState);

		// set content view
		setContentView(R.layout.yeepay_charge_activity_layout);

		Intent intent = getIntent();
		chargeType = intent.getStringExtra("charge_type");

		int titleId = R.string.yeepay_charge;
		if (ChargeType.szx_card.name().equals(chargeType)) {
			titleId = R.string.szx_card_charge;
		} else if (ChargeType.unicom_card.name().equals(chargeType)) {
			titleId = R.string.unicom_card_charge;
		} else if (ChargeType.telecom_card.name().equals(chargeType)) {
			titleId = R.string.telecom_card_charge;
		}
		// set title text
		setTitle(titleId);

		chargeMoneyListAdapter = new ChargeMoneyListAdapter(this);
		ListView chargeMoneyList = (ListView) findViewById(R.id.alipay_charge_money_list);
		chargeMoneyList.setAdapter(chargeMoneyListAdapter);
		chargeMoneyList.setOnItemClickListener(onChargeMoneySelectedListener);

		// productMap = new HashMap<String, String>();
	}
	
	

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		fetchChargeMoneyList();
	}

	// close the progress bar
	// 关闭进度框
	private void closeProgress() {
		if (mProgress != null) {
			mProgress.dismiss();
			mProgress = null;
		}
	}

	private void fetchChargeMoneyList() {
		mProgress = ProgressDialog.show(YeepayChargeActivity.this, null,
				getString(R.string.getting_charge_money_list));

		UserBean telUser = UserManager.getInstance().getUser();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("countryCode",
				(String) telUser.getValue(TelUser.countryCode.name()));
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.getChargeMoneyList_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedFetchChargeMoneyList);
	}

	private OnHttpRequestListener onFinishedFetchChargeMoneyList = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			closeProgress();
			try {
				JSONArray data = new JSONArray(responseResult.getResponseText());
				chargeMoneyListAdapter.setData(data);
			} catch (JSONException e) {
				e.printStackTrace();
				MyToast.show(YeepayChargeActivity.this,
						R.string.get_charge_money_list_failed,
						Toast.LENGTH_SHORT);
			}

		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			closeProgress();
			MyToast.show(YeepayChargeActivity.this,
					R.string.get_charge_money_list_failed, Toast.LENGTH_SHORT);
		}
	};

	private OnItemClickListener onChargeMoneySelectedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			JSONObject chargeMoneyObj = (JSONObject) chargeMoneyListAdapter
					.getItem(position);
			if (chargeMoneyObj != null) {
				try {
					int chargeMoneyId = chargeMoneyObj
							.getInt(ChargeMoneyConstants.id.name());
					double chargeMoney = chargeMoneyObj
							.getDouble(ChargeMoneyConstants.charge_money.name());
					chargeMoney(chargeMoneyId, chargeMoney);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	};

	private void chargeMoney(int chargeMoneyId, double money) {
		Log.d(TAG, "charge money");

		String price = String.format("%.2f", money);
		String orderNum = ChargeUtil.getOrderNum(chargeType);
		String time = "" + System.currentTimeMillis();

		// productMap.put("price", price);
		// productMap.put("order_num", orderNum);
		// productMap.put("time", time);
		// productMap.put("product_name", getString(R.string.product_subject));

		StringBuilder builder = new StringBuilder();
		builder.append(CUSTOMER_NUMBER).append("$");
		builder.append(orderNum).append("$");
		builder.append(price).append("$");
		builder.append(getString(R.string.product_subject)).append("$");
		builder.append(time);

		// UserBean user = UserManager.getInstance().getUser();
		//
		// HashMap<String, String> params = new HashMap<String, String>();
		// params.put("charge_money_id", String.valueOf(chargeMoneyId));
		// params.put("content", builder.toString());
		// params.put("order_num", orderNum);
		// params.put("money", price);
		// params.put("countryCode",
		// (String) user.getValue(TelUser.countryCode.name()));
		// HttpUtils.postSignatureRequest(getString(R.string.server_url)
		// + getString(R.string.yeepaySign_url),
		// PostRequestFormat.URLENCODED, params, null,
		// HttpRequestType.ASYNCHRONOUS, onFinishedSign);

		// ======
		String hmac = YeepayUtils.hmacSign(builder.toString(), KEY);

		Intent intent = new Intent(getBaseContext(), YeepayPlugin.class);
		intent.putExtra("customerNumber", CUSTOMER_NUMBER);
		intent.putExtra("requestId", orderNum);
		intent.putExtra("amount", price);
		intent.putExtra("productName", getString(R.string.product_subject));
		intent.putExtra("time", time);
		intent.putExtra("productDesc", "");
		intent.putExtra("support", "CH_MOBILE");
		intent.putExtra("environment", "");
		intent.putExtra("hmac", hmac);

		startActivityForResult(intent, 200);
	}

	// private OnHttpRequestListener onFinishedSign = new
	// OnHttpRequestListener() {
	//
	// @Override
	// public void onFinished(HttpResponseResult responseResult) {
	// try {
	// JSONObject data = new JSONObject(
	// responseResult.getResponseText());
	// String sign = data.getString("sign");
	// Log.d(TAG, "sign: " + sign);
	//
	// Intent intent = new Intent(getBaseContext(),
	// YeepayPlugin.class);
	// intent.putExtra("customerNumber", CUSTOMER_NUMBER);
	// intent.putExtra("requestId", productMap.get("order_num"));
	// intent.putExtra("amount", productMap.get("price"));
	// intent.putExtra("productName", productMap.get("product_name"));
	// intent.putExtra("time", productMap.get("time"));
	// intent.putExtra("productDesc", "");
	// intent.putExtra("support", "CH_MOBILE");
	// intent.putExtra("environment", "");
	// intent.putExtra("hmac", sign);
	//
	// startActivityForResult(intent, 200);
	// } catch (JSONException e) {
	// e.printStackTrace();
	// MyToast.show(YeepayChargeActivity.this,
	// R.string.get_sign_info_failed, Toast.LENGTH_SHORT);
	// }
	//
	// }
	//
	// @Override
	// public void onFailed(HttpResponseResult responseResult) {
	// if (responseResult.getStatusCode() == -1) {
	// MyToast.show(YeepayChargeActivity.this,
	// R.string.cannot_connet_server, Toast.LENGTH_SHORT);
	// } else {
	// MyToast.show(YeepayChargeActivity.this,
	// R.string.get_sign_info_failed, Toast.LENGTH_SHORT);
	// }
	//
	// }
	// };

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		AppDataSaveRestoreUtil.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.e(TAG, "requestCode=" + requestCode + "  resultCode=" + resultCode
				+ " (data==null)=" + (data == null));

		if (data != null) {
			Bundle params = data.getExtras();
			String requestId = params.getString("requestId");
			if (requestId == null) {
				requestId = "";
			}
			String amount = params.getString("amount");
			Log.e(TAG, "mPayBackInfo.nCode=" + params.getString("returnCode"));
			Log.e(TAG,
					"mPayBackInfo.customerNumber="
							+ params.getString("customerNumber"));
			Log.e(TAG, "mPayBackInfo.requestId=" + requestId);
			Log.e(TAG, "mPayBackInfo.amount=" + amount);
			Log.e(TAG, "mPayBackInfo.appId=" + params.getString("appId"));
			Log.e(TAG,
					"mPayBackInfo.errMsg=" + params.getString("errorMessage"));
			Log.e(TAG, "mPayBackInfo.time=" + params.getString("time"));
			Log.e(TAG, "mPayBackInfo.hmac=" + params.getString("hmac"));

			String message = params.getString("errorMessage");
			if (message == null) {
				message = "";
			}

			if (amount == null) {
				amount = "";
			}
			if (TextUtils.isEmpty(requestId) && TextUtils.isEmpty(amount)) {
				// new AlertDialog.Builder(YeepayChargeActivity.this)
				// .setTitle(R.string.pay_result)
				// .setMessage(
				// getString(R.string.pay_failed)
				// + String.format(
				// getString(R.string.pay_failed_error_msg),
				// message))
				// .setPositiveButton(R.string.Ensure, null).show();
				Log.d(TAG, "charge failed: " + message);
			} else {
				// new AlertDialog.Builder(YeepayChargeActivity.this)
				// .setTitle(R.string.pay_result)
				// .setMessage(
				// String.format(getString(R.string.pay_success),
				// amount))
				// .setPositiveButton(R.string.Ensure, null).show();
				Log.d(TAG, "charge success");
			}
		} else {
			// new
			// AlertDialog.Builder(YeepayChargeActivity.this).setTitle(R.string.pay_result)
			// .setMessage(R.string.pay_failed)
			// .setPositiveButton(R.string.Ensure, null).show();
			Log.d(TAG, "charge failed");
		}
	}
}
