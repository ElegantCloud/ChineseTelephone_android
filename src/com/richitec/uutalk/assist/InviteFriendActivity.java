package com.richitec.uutalk.assist;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adsmogo.adapters.AdsMogoCustomEventPlatformEnum;
import com.adsmogo.adview.AdsMogoLayout;
import com.adsmogo.controller.listener.AdsMogoListener;
import com.richitec.commontoolkit.activityextension.NavigationActivity;
import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.MyToast;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.uutalk.R;
import com.richitec.uutalk.assist.share.ContactLisInviteFriendActivity;
import com.richitec.uutalk.assist.share.QzoneShareActivity;
import com.richitec.uutalk.constant.SystemConstants;
import com.richitec.uutalk.constant.TelUser;
import com.richitec.uutalk.utils.AppDataSaveRestoreUtil;

public class InviteFriendActivity extends NavigationActivity implements
		AdsMogoListener {
	private String inviteLink;
	private AdsMogoLayout adsMogoLayout;
	private LinearLayout adSection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_invite_friend_layout);

		inviteLink = getIntent().getStringExtra("inviteLink");
		Log.d("inviteLink", inviteLink);

		setTitle(R.string.earn_money);

		adsMogoLayout = (AdsMogoLayout) findViewById(R.id.adsMogoView);
		adsMogoLayout.setAdsMogoListener(this);
		adSection = (LinearLayout) findViewById(R.id.ad_section);

		loadDescription();
		getRegedUserCountViaShare();
	}

	private void loadDescription() {
		UserBean user = UserManager.getInstance().getUser();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("countryCode",
				(String) user.getValue(TelUser.countryCode.name()));
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.getRegInviteDescription_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedLoadDesc);
	}

	private OnHttpRequestListener onFinishedLoadDesc = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			try {
				JSONObject data = new JSONObject(
						responseResult.getResponseText());
				TextView descTV = (TextView) findViewById(R.id.invite_reg_descirption_tv);
				String desc = data.getString("reg_gift_desc_text");
				descTV.setText(desc);
				descTV.setVisibility(View.VISIBLE);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
		}
	};

	private void getRegedUserCountViaShare() {
		UserBean user = UserManager.getInstance().getUser();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("countryCode",
				(String) user.getValue(TelUser.countryCode.name()));
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.getRegedUserCountViaShare_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedGetUserCount);
	}

	private OnHttpRequestListener onFinishedGetUserCount = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			TextView userCountTV = (TextView) findViewById(R.id.shared_user_count_tv);
			try {
				JSONObject data = new JSONObject(
						responseResult.getResponseText());
				int count = data.getInt("shared_user_count");
				if (count > 0) {
					userCountTV.setText(String.format(
							getString(R.string.current_user_shared_reg_count),
							count));
				} else {
					userCountTV.setText(R.string.no_user_shared_reg);
				}
				userCountTV.setVisibility(View.VISIBLE);
			} catch (JSONException e) {
				e.printStackTrace();
				userCountTV.setVisibility(View.GONE);
			}

		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			TextView userCountTV = (TextView) findViewById(R.id.shared_user_count_tv);
			userCountTV.setVisibility(View.GONE);
		}
	};

	public void smsInvite(View v) {
//		HashMap<String, Object> params = new HashMap<String, Object>();
//		params.put("inviteLink", inviteLink);
//		pushActivity(ContactLisInviteFriendActivity.class, params);
		
		Uri uri = Uri.parse("smsto:");
		Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
		String inviteMessage = getString(R.string.invite_message).replace(
				"***", inviteLink);
		intent.putExtra("sms_body", inviteMessage);
		startActivity(intent);
	}

	public void onShareToQQzone(View v) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		String summary = getString(R.string.invite_message).replace("***",
				inviteLink);
		String title = getString(R.string.share_to_qzone_title);
		String url = getString(R.string.share_to_qzone_link_url);
		params.put("summary", summary);
		params.put("title", title);
		params.put("url", url);
		params.put("images", getString(R.string.share_to_qzone_images));
		pushActivity(QzoneShareActivity.class, params);
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
	public Class getCustomEvemtPlatformAdapterClass(
			AdsMogoCustomEventPlatformEnum arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onClickAd(String arg0) {
		Log.d(SystemConstants.TAG, "AdShowActivity - onClickAd: " + arg0);

		UserBean user = UserManager.getInstance().getUser();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TelUser.countryCode.name(),
				(String) user.getValue(TelUser.countryCode.name()));
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.ad_click_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedAdClick);
	}

	private OnHttpRequestListener onFinishedAdClick = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			try {
				JSONObject data = new JSONObject(
						responseResult.getResponseText());
				boolean result = data.getBoolean("result");
				Double money = data.getDouble("money");
				if (result) {
					MyToast.show(InviteFriendActivity.this, String.format(
							getString(R.string.u_got_gift_money),
							String.format("%.2f", money.floatValue())),
							Toast.LENGTH_SHORT);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			// TODO Auto-generated method stub

		}
	};

	@Override
	public boolean onCloseAd() {
		Log.d(SystemConstants.TAG, "AdShowActivity - onCloseAd");
		adSection.setVisibility(View.GONE);
		return false;
	}

	@Override
	public void onCloseMogoDialog() {
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
		adSection.setVisibility(View.VISIBLE);
	}

	@Override
	public void onRequestAd(String arg0) {
		// TODO Auto-generated method stub
		Log.d(SystemConstants.TAG, "AdShowActivity - onRequestAd: " + arg0);
	}
}
