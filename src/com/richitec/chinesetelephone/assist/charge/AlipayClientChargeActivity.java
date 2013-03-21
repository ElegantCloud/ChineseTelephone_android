package com.richitec.chinesetelephone.assist.charge;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.richitec.chinesetelephone.R;
import com.richitec.chinesetelephone.alipay.AlixId;
import com.richitec.chinesetelephone.alipay.BaseHelper;
import com.richitec.chinesetelephone.alipay.MobileSecurePayHelper;
import com.richitec.chinesetelephone.alipay.MyRC4;
import com.richitec.chinesetelephone.alipay.PartnerConfig;
import com.richitec.chinesetelephone.alipay.ResultChecker;
import com.richitec.chinesetelephone.bean.ProductBean;
import com.richitec.chinesetelephone.constant.AliPay;
import com.richitec.chinesetelephone.constant.ChargeMoneyConstants;
import com.richitec.chinesetelephone.constant.TelUser;
import com.richitec.chinesetelephone.utils.AliPayManager;
import com.richitec.chinesetelephone.utils.AppDataSaveRestoreUtil;
import com.richitec.commontoolkit.activityextension.NavigationActivity;
import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.commontoolkit.utils.MyToast;

public class AlipayClientChargeActivity extends NavigationActivity {
	private ChargeMoneyListAdapter chargeMoneyListAdapter;
	private MobileSecurePayHelper mspHelper = null;
	private ProgressDialog mProgress = null;
	private String TAG = "AlipayClientChargeActivity";
	private boolean hasLoadChargeList = false;
	private JSONObject currentChargeMoney;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			AppDataSaveRestoreUtil.onRestoreInstanceState(savedInstanceState);
		}

		super.onCreate(savedInstanceState);

		setContentView(R.layout.alipay_client_charge_activity_layout);
		setTitle(R.string.alipay_pattern);

		mspHelper = new MobileSecurePayHelper(this);

		chargeMoneyListAdapter = new ChargeMoneyListAdapter(this);
		ListView chargeMoneyList = (ListView) findViewById(R.id.alipay_charge_money_list);
		chargeMoneyList.setAdapter(chargeMoneyListAdapter);
		chargeMoneyList.setOnItemClickListener(onChargeMoneySelectedListener);
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
		if (!hasLoadChargeList) {
			fetchChargeMoneyList();
			hasLoadChargeList = true;
		}
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

	private OnItemClickListener onChargeMoneySelectedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			currentChargeMoney = (JSONObject) chargeMoneyListAdapter
					.getItem(position);

			boolean isMobile_spExist = mspHelper.detectMobile_sp();
			if (!isMobile_spExist)
				return;

			if (PartnerConfig.PARTNER.equals("")) {

				UserBean telUser = UserManager.getInstance().getUser();
				HashMap<String, String> params = new HashMap<String, String>();
				params.put("countryCode",
						(String) telUser.getValue(TelUser.countryCode.name()));

				HttpUtils.postSignatureRequest(getString(R.string.server_url)
						+ getString(R.string.get_seller_partner_key),
						PostRequestFormat.URLENCODED, params, null,
						HttpRequestType.ASYNCHRONOUS, onGetPrivateKeyListener);
			} else {
				if (currentChargeMoney != null) {
					try {
						int chargeMoneyId = currentChargeMoney
								.getInt(ChargeMoneyConstants.id.name());
						double chargeMoney = currentChargeMoney
								.getDouble(ChargeMoneyConstants.charge_money
										.name());
						chargeMoney(chargeMoneyId, chargeMoney);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		}
	};
	private OnHttpRequestListener onGetPrivateKeyListener = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {

			UserBean telUser = UserManager.getInstance().getUser();
			String encryStr = responseResult.getResponseText();

			String decryData = MyRC4.decryptPro(encryStr, telUser.getUserKey());

			try {
				JSONObject data = new JSONObject(decryData);
				String partnerId = data.getString("partner_id");
				String sellerId = data.getString("seller");
				// String private_key = data.getString("private_key");

				PartnerConfig.PARTNER = partnerId;
				PartnerConfig.SELLER = sellerId;
				// PartnerConfig.RSA_PRIVATE = private_key;

				if (currentChargeMoney != null) {
					try {
						int chargeMoneyId = currentChargeMoney
								.getInt(ChargeMoneyConstants.id.name());
						double chargeMoney = currentChargeMoney
								.getDouble(ChargeMoneyConstants.charge_money
										.name());
						chargeMoney(chargeMoneyId, chargeMoney);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
				closeProgress();
				MyToast.show(AlipayClientChargeActivity.this,
						R.string.get_alipay_info_from_server_failed,
						Toast.LENGTH_SHORT);
			}
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			closeProgress();
			MyToast.show(AlipayClientChargeActivity.this,
					R.string.get_alipay_info_from_server_failed,
					Toast.LENGTH_SHORT);
		}

	};

	private void chargeMoney(final int chargeMoneyId, double money) {
		boolean isMobile_spExist = mspHelper.detectMobile_sp();
		if (!isMobile_spExist)
			return;

		final String price = String.format("%.2f", money);
		AlertDialog.Builder tDialog = new AlertDialog.Builder(this);
		tDialog.setIcon(R.drawable.alipay_install_info);
		tDialog.setTitle(getString(R.string.ensure_charge_title));
		tDialog.setMessage(getString(R.string.charge_alipay_hint).replace(
				"***", price));
		tDialog.setNegativeButton(getString(R.string.cancel), null);
		tDialog.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						ProductBean p = new ProductBean();
						p.setChargeMoneyId(chargeMoneyId);
						p.setBody(AliPay.aliPayBody);
						p.setSubject(AliPay.aliPaySubject);
						p.setPrice(price);
						charging(p);
					}
				});
		tDialog.show();
	}

	private void charging(ProductBean p) {
		AliPayManager aliPayManager = new AliPayManager(mHandler, this);
		if (aliPayManager.checkInfo()) {

			aliPayManager.pay(p);
			// show the progress bar to indicate that we have started
			// paying.
			// 显示“正在支付”进度条
			closeProgress();
			mProgress = BaseHelper.showProgress(this, null,
					getString(R.string.is_charging), false, true);
		} else {
			BaseHelper.showDialog(this, getString(R.string.alert_title),
					getString(R.string.alipay_info_check_failed),
					R.drawable.infoicon);
			return;
		}
	}

	// the handler use to receive the pay result.
	// 这里接收支付结果，支付宝手机端同步通知
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			try {
				String strRet = (String) msg.obj;
				// Log.e(TAG, strRet); //
				// strRet范例：resultStatus={9000};memo={};result={partner="2088201564809153"&seller="2088201564809153"&out_trade_no="050917083121576"&subject="123456"&body="2010新款NIKE 耐克902第三代板鞋 耐克男女鞋 386201 白红"&total_fee="0.01"&notify_url="http://notify.java.jpxx.org/index.jsp"&success="true"&sign_type="RSA"&sign="d9pdkfy75G997NiPS1yZoYNCmtRbdOP0usZIMmKCCMVqbSG1P44ohvqMYRztrB6ErgEecIiPj9UldV5nSy9CrBVjV54rBGoT6VSUF/ufjJeCSuL510JwaRpHtRPeURS1LXnSrbwtdkDOktXubQKnIMg2W0PreT1mRXDSaeEECzc="}
				switch (msg.what) {
				case AlixId.RQF_PAY: {
					//
					closeProgress();

					BaseHelper.log(TAG, strRet);

					// 处理交易结果
					try {
						// 获取交易状态码，具体状态代码请参看文档
						String tradeStatus = "resultStatus={";
						int imemoStart = strRet.indexOf("resultStatus=");
						imemoStart += tradeStatus.length();
						int imemoEnd = strRet.indexOf("};memo=");
						tradeStatus = strRet.substring(imemoStart, imemoEnd);

						// 先验签通知
						ResultChecker resultChecker = new ResultChecker(strRet);
						int retVal = resultChecker.checkSign();
						// 验签失败
						if (retVal == ResultChecker.RESULT_CHECK_SIGN_FAILED) {
							BaseHelper.showDialog(
									AlipayClientChargeActivity.this,
									getString(R.string.alert_title),
									getResources().getString(
											R.string.check_sign_failed),
									android.R.drawable.ic_dialog_alert);
						} else {// 验签成功。验签成功后再判断交易状态码
							if (tradeStatus.equals("9000")) {// 判断交易状态码，只有9000表示交易成功
								// getRemainMoney();
								BaseHelper.showDialog(
										AlipayClientChargeActivity.this,
										getString(R.string.alert_title),
										getString(R.string.trade_success)
												+ tradeStatus,
										R.drawable.infoicon);
							} else
								BaseHelper.showDialog(
										AlipayClientChargeActivity.this,
										getString(R.string.alert_title),
										getString(R.string.trade_failed)
												+ tradeStatus,
										R.drawable.infoicon);
						}

					} catch (Exception e) {
						e.printStackTrace();
						BaseHelper.showDialog(AlipayClientChargeActivity.this,
								getString(R.string.alert_title), strRet,
								R.drawable.infoicon);
					}
				}
					break;
				default:
					closeProgress();
				}

				super.handleMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private void fetchChargeMoneyList() {
		mProgress = ProgressDialog.show(AlipayClientChargeActivity.this, null,
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
				// showAlipayChargeContent();
			} catch (JSONException e) {
				e.printStackTrace();
				MyToast.show(AlipayClientChargeActivity.this,
						R.string.get_alipay_info_from_server_failed,
						Toast.LENGTH_SHORT);
			}

		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			closeProgress();
			MyToast.show(AlipayClientChargeActivity.this,
					R.string.get_alipay_info_from_server_failed,
					Toast.LENGTH_SHORT);
		}
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		AppDataSaveRestoreUtil.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}
}
