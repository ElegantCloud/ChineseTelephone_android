package com.richitec.uutalk.call;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.richitec.commontoolkit.CTApplication;
import com.richitec.commontoolkit.addressbook.AddressBookManager;
import com.richitec.commontoolkit.animation.CTRotate3DAnimation;
import com.richitec.commontoolkit.animation.CTRotate3DAnimation.ThreeDimensionalRotateDirection;
import com.richitec.commontoolkit.call.TelephonyManagerExtension;
import com.richitec.commontoolkit.customadapter.CTListAdapter;
import com.richitec.commontoolkit.customcomponent.ListViewQuickAlphabetBar;
import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.CommonUtils;
import com.richitec.commontoolkit.utils.DisplayScreenUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.NetworkInfoUtils;
import com.richitec.commontoolkit.utils.NetworkInfoUtils.NoActiveNetworkException;
import com.richitec.commontoolkit.utils.StringUtils;
import com.richitec.commontoolkit.utils.ToneGeneratorUtils;
import com.richitec.uutalk.ChineseTelephoneAppLaunchActivity;
import com.richitec.uutalk.R;
import com.richitec.uutalk.assist.SettingActivity.NoLocalAreaCodePopupWindow;
import com.richitec.uutalk.call.SipCallModeSelector.SipCallModeSelectPattern;
import com.richitec.uutalk.constant.SystemConstants;
import com.richitec.uutalk.constant.TelUser;
import com.richitec.uutalk.sip.SipUtils;
import com.richitec.uutalk.sip.listeners.SipInviteStateListener;
import com.richitec.uutalk.sip.listeners.SipRegistrationStateListener;
import com.richitec.uutalk.sip.listeners.SipRegistrationStateListenerImp;
import com.richitec.uutalk.sip.services.BaseSipServices;
import com.richitec.uutalk.sip.services.ISipServices.SipCallSponsor;
import com.richitec.uutalk.tab7tabcontent.CTContactListViewQuickAlphabetToast;
import com.richitec.uutalk.tab7tabcontent.ChineseTelephoneTabActivity;
import com.richitec.uutalk.tab7tabcontent.ContactListTabContentActivity;
import com.richitec.uutalk.tab7tabcontent.ContactListTabContentActivity.ContactsInABListViewQuickAlphabetBarOnTouchListener;
import com.richitec.uutalk.utils.AppDataSaveRestoreUtil;
import com.richitec.uutalk.utils.SipRegisterManager;

public class OutgoingCallActivity extends Activity implements
		SipInviteStateListener {

	private static final String LOG_TAG = OutgoingCallActivity.class
			.getCanonicalName();

	// sip services
	private static final BaseSipServices SIPSERVICES = SipUtils
			.getSipServices();

	// outgoing call activity onCreate param key
	public static final String OUTGOING_CALL_SPONSOR = "outgoing_call_sponsor";
	public static final String OUTGOING_CALL_MODE = "outgoing_call_mode";
	public static final String OUTGOING_CALL_PHONE = "outgoing_call_phone";
	public static final String OUTGOING_CALL_OWNERSHIP = "outgoing_call_ownership";

	// outgoing call mode
	private SipCallMode _mOutgoingCallMode;

	// outgoing call phone number
	private String _mCalleePhone;

	// audio manager
	private AudioManager _mAudioManager;

	// call duration chronometer
	private Chronometer _mCallDurationChronometer;

	// call state textView
	private TextView _mCallStateTextView;

	// is outgoing call established
	private boolean _mIsOutgoingCallEstablished;

	// send callback sip voice call http request listener
	private SendCallbackSipVoiceCallHttpRequestListener SEND_CALLBACKSIPVOICECALL_HTTPREQUESTLISTENER;

	// phone state broadcast receiver
	private BroadcastReceiver _mPhoneStateBroadcastReceiver;

	// Chinese telephone contact listView quick alphabet toast
	CTContactListViewQuickAlphabetToast _mContactListViewQuickAlphabetToast;

	// outgoing call center content relativeLayout
	private RelativeLayout _mCenterContentRelativeLayout;

	// outgoing call keyboard gridView
	private GridView _mKeyboardGridView;

	// hangup and hide keyboard image button
	private ImageButton _mHangupBtn;
	private ImageButton _mHideKeyboardBtn;

	// sip voice call terminated type, default value is passive
	private SipVoiceCallTerminatedType _mSipVoiceCallTerminatedType = SipVoiceCallTerminatedType.PASSIVE;

	// sip registration state listener
	private SipRegistrationStateListener _mSipRegistrationStateListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(SystemConstants.TAG, "OutgoingCallActivity - onCreate");
		super.onCreate(savedInstanceState);

		// keep outgoing call activity screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// set content view
		setContentView(R.layout.outgoing_call_activity_layout);

		SEND_CALLBACKSIPVOICECALL_HTTPREQUESTLISTENER = new SendCallbackSipVoiceCallHttpRequestListener();

		// set sip services sip invite state listener
		SIPSERVICES.setSipInviteStateListener(this);

		// define phone state intent filter and default filter action is phone
		// state
		IntentFilter _phoneStateIntentFilter = new IntentFilter(
				TelephonyManager.ACTION_PHONE_STATE_CHANGED);

		// add phone state intent filter action, new outgoing call
		_phoneStateIntentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);

		// set phone state broadcast receiver and register it
		_mPhoneStateBroadcastReceiver = new PhoneStateBroadcastReceiver();
		registerReceiver(_mPhoneStateBroadcastReceiver, _phoneStateIntentFilter);

		// get the intent parameter data
		Bundle _data = getIntent().getExtras();

		// Log.d(LOG_TAG, "on create bundle data = " + _data);
		// if (null != _data) {
		// for (String bundleKey : _data.keySet()) {
		// Log.d(LOG_TAG, "bundle data, key = " + bundleKey
		// + " and bundle value = " + _data.get(bundleKey));
		// }
		// }

		// check the data bundle again and get call phone
		if (null != _data
				&& SipCallSponsor.inner == _data.get(OUTGOING_CALL_SPONSOR)) {
			// init call state textView text
			((TextView) findViewById(R.id.callState_textView))
					.setText(R.string.outgoing_call_trying);

			// check and reset outgoing call mode
			if (null != _data.get(OUTGOING_CALL_MODE)) {
				_mOutgoingCallMode = (SipCallMode) _data
						.get(OUTGOING_CALL_MODE);
			}

			// init outgoing call phone and set callee textView text
			if (null != _data.getString(OUTGOING_CALL_PHONE)) {
				_mCalleePhone = _data.getString(OUTGOING_CALL_PHONE);

				((TextView) findViewById(R.id.callee_textView))
						.setText(null != _data
								.getString(OUTGOING_CALL_OWNERSHIP) ? _data
								.getString(OUTGOING_CALL_OWNERSHIP)
								: _mCalleePhone);
			}
		} else {
			// get user bean
			UserBean _userBean = UserManager.getInstance().getUser();

			// get exported dial phone and set as callee phone
			_mCalleePhone = StringUtils.trim(getIntent().getData()
					.getSchemeSpecificPart(), " ");

			// check user logined
			if (null != _userBean.getValue(SystemConstants.logined.name())
					&& (Boolean) _userBean.getValue(SystemConstants.logined
							.name())) {
				// init exported outgoing call
				initExportedOutgoingCall();
			} else {
				// load and get login account
				AppDataSaveRestoreUtil.loadAccount();

				// update user bean
				_userBean = UserManager.getInstance().getUser();

				if (_userBean.getPassword() != null
						&& !_userBean.getPassword().equals("")) {
					// init exported outgoing call
					initExportedOutgoingCall();
				} else {
					// save outgoing call callee phone
					_userBean.setValue(TelUser.exported_dial_phone.name(),
							_mCalleePhone);

					// goto Chinese telephone application launch activity
					Intent _chineseTelephoneAppLaunchIntent = new Intent(this,
							ChineseTelephoneAppLaunchActivity.class);
					_chineseTelephoneAppLaunchIntent
							.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(_chineseTelephoneAppLaunchIntent);

					// finish outgoing call activity
					finish();
				}
			}
		}

		// init audio manager
		_mAudioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);

		// set wallpaper as outgoing call background
		((ImageView) findViewById(R.id.outgoingcall_background_imageView))
				.setImageDrawable(getWallpaper());

		// get call duration chronometer
		_mCallDurationChronometer = (Chronometer) findViewById(R.id.callDuration_chronometer);

		// get call state textView
		_mCallStateTextView = (TextView) findViewById(R.id.callState_textView);
		_mCallStateTextView.setText(getString(R.string.outgoing_call_trying) + _mCalleePhone);
		
		// get center content relativeLayout
		_mCenterContentRelativeLayout = (RelativeLayout) findViewById(R.id.outgoingCall_centerRelativeLayout);

		// get call controller gridView
		GridView _callControllerGridView = (GridView) findViewById(R.id.callController_gridView);

		// set call controller gridView adapter
		_callControllerGridView.setAdapter(generateCallControllerAdapter());

		// set call controller gridView on item click listener
		_callControllerGridView
				.setOnItemClickListener(new CallControllerGridViewOnItemClickListener());

		// add hide contacts list button on click listener
		((Button) findViewById(R.id.hide_contactslist_button))
				.setOnClickListener(new HideContactsListOnClickListener());

		// get contacts in address book list view
		ListView _abContactsListView = (ListView) findViewById(R.id.contactInAB_listView);

		// set contact in address book listView adapter
		_abContactsListView.setAdapter(ContactListTabContentActivity
				.getInABContactAdapter(this));
		// init address book contacts listView quick alphabet bar and add on
		// touch listener
		new ListViewQuickAlphabetBar(
				_abContactsListView,
				_mContactListViewQuickAlphabetToast = new CTContactListViewQuickAlphabetToast(
						this))
				.setOnTouchListener(new ContactsInABListViewQuickAlphabetBarOnTouchListener());

		// init keyboard gridView
		_mKeyboardGridView = (GridView) findViewById(R.id.keyboard_gridView);

		// rotate keyboard gridView 180 degree
		CTRotate3DAnimation
				.static3DRotate4View(
						_mKeyboardGridView,
						ThreeDimensionalRotateDirection.HORIZONTAL_RIGHT,
						new Point(
								(DisplayScreenUtils.screenWidth() - (DisplayScreenUtils
										.dp2pix(2 * 16) + 2 * (int) getResources()
										.getDimension(
												R.dimen.keyboard_gridView_marginLeft7Right))) / 2,
								(DisplayScreenUtils.screenHeight()
										- DisplayScreenUtils.statusBarHeight() - DisplayScreenUtils
										.dp2pix(2 * (16 + getResources()
												.getDimension(
														R.dimen.keyboard_gridView_marginTop7Bottom)))) / 2));

		// set keyboard gridView adapter
		_mKeyboardGridView.setAdapter(generateKeyboardAdapter());

		// get back for waiting callback call button
		ImageButton _back4waitingCallbackCallImgBtn = (ImageButton) findViewById(R.id.back4waiting_callbackCall_button);

		// bind back for waiting callback call button on click listener
		_back4waitingCallbackCallImgBtn
				.setOnClickListener(new CancelExportedOutgoingCall6Back4WaitingCallbackCallBtnOnClickListener());

		// get cancel exported outgoing call button
		ImageButton _cancelExportedOutgoingCallImgBtn = (ImageButton) findViewById(R.id.cancel_exportedOutgoingCall_button);

		// bind cancel exported outgoing call button on click listener
		_cancelExportedOutgoingCallImgBtn
				.setOnClickListener(new CancelExportedOutgoingCall6Back4WaitingCallbackCallBtnOnClickListener());

		// get exported outgoing call linearLayout
		LinearLayout _exportedOutgoingCallLinearLayout = (LinearLayout) findViewById(R.id.exportedOutgoingCall_linearLayout);

		// set hangup outgoing call button and bind its on click listener
		_mHangupBtn = (ImageButton) findViewById(R.id.hangup_button);
		_mHangupBtn
				.setOnClickListener(new HangupOutgoingCallBtnOnClickListener());

		// set hide keyboard button and bind its on click listener
		_mHideKeyboardBtn = (ImageButton) findViewById(R.id.hideKeyboard_button);
		_mHideKeyboardBtn
				.setOnClickListener(new HideKeyboardBtnOnClickListener());

		// check outgoing call mode
		if (null != _mOutgoingCallMode) {
			switch (_mOutgoingCallMode) {
			case DIRECT_CALL:
				// hide cancel exported outgoing call linearLayout and button,
				// show call controller gridView and call controller footer
				// linearLayout
				_cancelExportedOutgoingCallImgBtn.setVisibility(View.GONE);
				_exportedOutgoingCallLinearLayout.setVisibility(View.GONE);

				_callControllerGridView.setVisibility(View.VISIBLE);
				((LinearLayout) findViewById(R.id.callController_footerLinearLayout))
						.setVisibility(View.VISIBLE);

				break;

			default:
			case CALLBACK:
				// hide cancel exported outgoing call linearLayout and button
				// and show back for waiting callback call button
				_cancelExportedOutgoingCallImgBtn.setVisibility(View.GONE);
				_exportedOutgoingCallLinearLayout.setVisibility(View.GONE);

				((ImageButton) findViewById(R.id.back4waiting_callbackCall_button))
						.setVisibility(View.VISIBLE);

				break;
			}
		} else {
			// get and check sip voice call mode select pattern
			SipCallModeSelectPattern _sipCallModeSelectPattern = SipCallModeSelector
					.getSipCallModeSelectPattern();

			if (SipCallModeSelectPattern.MANUAL != _sipCallModeSelectPattern) {
				// generate an new outgoing call
				if (NetworkInfoUtils.isCurrentActiveNetworkAvailable()) {
					// get callee name
					String _calleeName = (String) ((TextView) findViewById(R.id.callee_textView))
							.getText();

					// get and check sip call mode select pattern
					switch (SipCallModeSelector.getSipCallModeSelectPattern()) {
					case DIRECT_CALL:
						// check contact for generating an new outgoing call:
						// direct dial
						generateExportedOutgoingCall(_calleeName,
								_mCalleePhone, SipCallMode.DIRECT_CALL);
						break;

					case CALLBACK:
						// check contact for generating an new outgoing call:
						// callback
						generateExportedOutgoingCall(_calleeName,
								_mCalleePhone, SipCallMode.CALLBACK);
						break;

					default:
					case AUTO:
						try {
							// get and check current active network type
							switch (NetworkInfoUtils.getNetworkType()) {
							case ConnectivityManager.TYPE_WIFI:
								// check contact for generating an new outgoing
								// call: auto direct dial
								generateExportedOutgoingCall(_calleeName,
										_mCalleePhone, SipCallMode.DIRECT_CALL);
								break;

							case ConnectivityManager.TYPE_MOBILE:
							default:
								// check contact for generating an new outgoing
								// call: auto callback
								generateExportedOutgoingCall(_calleeName,
										_mCalleePhone, SipCallMode.CALLBACK);
								break;
							}
						} catch (NoActiveNetworkException e) {
							Log.e(LOG_TAG,
									"Generate an new outgoing call error, because here is no active network currently, exception message = "
											+ e.getMessage());

							e.printStackTrace();
						}
						break;
					}
				} else {
					// show there is no active and available network currently
					new AlertDialog.Builder(OutgoingCallActivity.this)
							.setTitle(
									R.string.noActiveAvailableNetwork_alertDialog_title)
							.setMessage(
									R.string.noActiveAvailableNetwork_alertDialog_message)
							.setNegativeButton(
									R.string.noActiveAvailableNetwork_alertDialog_setLaterBtn_title,
									null)
							.setPositiveButton(
									R.string.noActiveAvailableNetwork_alertDialog_setNowBtn_title,
									new ModifyWirelessSettingsBtnOnClickListener())
							.show();
				}
			}
		}
	}

	@Override
	protected void onResume() {
		Log.d(SystemConstants.TAG, "OutgoingCallActivity - onResume");
		// check sip registration state listener and register sip account
		if (null != _mSipRegistrationStateListener) {
			SipRegisterManager.registSip(_mSipRegistrationStateListener,
					getString(R.string.vos_server));
		}

		super.onResume();
	}

	@Override
	protected void onStart() {
		Log.d(SystemConstants.TAG, "OutgoingCallActivity - onStart");
		super.onStart();
	}

	@Override
	protected void onRestart() {
		Log.d(SystemConstants.TAG, "OutgoingCallActivity - onRestart");
		super.onRestart();
	}

	@Override
	public void onBackPressed() {
		// get contacts list sliding drawer
		SlidingDrawer _contactListSlidingDrawer = (SlidingDrawer) findViewById(R.id.contactslist_slidingDrawer);

		// check and hide contacts list sliding drawer
		if (_contactListSlidingDrawer.isShown()) {
			// check contact listView alphabet toast and visibility
			if (null != _mContactListViewQuickAlphabetToast
					&& _mContactListViewQuickAlphabetToast.isShowing()) {
				_mContactListViewQuickAlphabetToast.cancel();
			}

			// close contacts list sliding drawer
			_contactListSlidingDrawer.animateClose();
		}
	}

	@Override
	protected void onDestroy() {
		// unregister phone state broadcast receiver
		unregisterReceiver(_mPhoneStateBroadcastReceiver);

		// check sip registration state listener and cancel voip online status
		if (null != _mSipRegistrationStateListener) {
			SipRegistrationStateListenerImp.cancelVOIPOnlineStatus();
		}

		super.onDestroy();
	}

	@Override
	public void onCallInitializing() {
		// update call state textView text
		_mCallStateTextView.setText(R.string.outgoing_call_trying);
	}

	@Override
	public void onCallEarlyMedia() {
		// update call state textView text
		_mCallStateTextView
				.setText(R.string.outgoing_call_earlyMedia7RemoteRing);
	}

	@Override
	public void onCallRemoteRinging() {
		// update call state textView text
		_mCallStateTextView
				.setText(R.string.outgoing_call_earlyMedia7RemoteRing);
	}

	@Override
	public void onCallSpeaking() {
		// check current sip voice call using loudspeaker
		if (SIPSERVICES.isSipVoiceCallUsingLoudspeaker()) {
			// set current sip voice call loudspeaker
			// set mode
			_mAudioManager.setMode(AudioManager.MODE_NORMAL);

			// open speaker
			_mAudioManager.setSpeakerphoneOn(true);
		}

		// set outgoing call has been established
		_mIsOutgoingCallEstablished = true;

		// hide call state textView
		_mCallStateTextView.setVisibility(View.GONE);

		// set call duration chronometer base and start it
		_mCallDurationChronometer.setBase(SystemClock.elapsedRealtime());
		_mCallDurationChronometer.start();

		// show call duration chronometer
		_mCallDurationChronometer.setVisibility(View.VISIBLE);
	}

	@Override
	public void onCallFailed() {
		// update call state textView text
		_mCallStateTextView.setText(R.string.outgoing_call_failed);

		// sip voice call terminated
		onCallTerminated();
	}

	@Override
	public void onCallTerminating() {
		// update call state textView text
		_mCallStateTextView.setText(R.string.end_outgoing_call);

		onCallTerminated();
	}

	@Override
	public void onCallTerminated() {
		// check sip voice call terminated type
		if (SipVoiceCallTerminatedType.PASSIVE == _mSipVoiceCallTerminatedType) {
			// terminate current sip voice call
			terminateSipVoiceCall(SipVoiceCallTerminatedType.PASSIVE);
		}
	}

	public SendCallbackSipVoiceCallHttpRequestListener getSendCallbackSipVoiceCallHttpRequestListener() {
		return SEND_CALLBACKSIPVOICECALL_HTTPREQUESTLISTENER;
	}

	// generate call controller adapter
	private ListAdapter generateCallControllerAdapter() {
		// call controller item adapter data key
		final String CALL_CONTROLLER_ITEM_PARENTRELATIVELAYOUT = "call_controller_item_parentRelativeLayout";
		final String CALL_CONTROLLER_ITEM_ICON = "call_controller_item_icon";
		final String CALL_CONTROLLER_ITEM_LABEL = "call_controller_item_label";

		// define call controller gridView content and mute or unmute, setting
		// using loudspeaker or earphone on touch listener
		final int[][] _callControllerGridViewContentArray = new int[][] {
				{ R.drawable.callcontroller_contactitem6keyboard_1btn_bg,
						R.drawable.img_callcontroller_contactitem_normal,
						R.string.callController_contactItem_text },
				{ R.drawable.callcontroller_keyboarditem6keyboard_3btn_bg,
						R.drawable.img_callcontroller_keyboarditem_normal,
						R.string.callController_keyboardItem_text },
				{ R.drawable.callcontroller_muteitem_bg,
						R.drawable.img_callcontroller_muteitem_normal,
						R.string.callController_muteItem_text },
				{ R.drawable.callcontroller_handfreeitem_bg,
						R.drawable.img_callcontroller_handfreeitem_normal,
						R.string.callController_handfreeItem_text } };

		final OnTouchListener[] _callControllerGridViewOnTouchListenerArray = new OnTouchListener[] {
				new Mute6UnmuteCallControllerGridViewOnTouchListener(),
				new SetUsingLoudspeaker6EarphoneCallControllerGridViewOnTouchListener() };

		// set call controller data list
		List<Map<String, ?>> _callControllerDataList = new ArrayList<Map<String, ?>>();

		for (int i = 0; i < _callControllerGridViewContentArray.length; i++) {
			// generate data
			Map<String, Object> _dataMap = new HashMap<String, Object>();

			// put value
			// generate call controller item parent relative layout data map
			Map<String, Object> _callControllerItemParentRelaviteLayoutData = new HashMap<String, Object>();

			// put call controller item data value
			_callControllerItemParentRelaviteLayoutData
					.put(OutgoingCallControllerAdapter.CALL_CONTROLLER_ITEM_BACKGROUND,
							_callControllerGridViewContentArray[i][0]);
			if (2 <= i && 3 >= i) {
				_callControllerItemParentRelaviteLayoutData
						.put(OutgoingCallControllerAdapter.CALL_CONTROLLER_ITEM_ONTOUCHLISTENER,
								_callControllerGridViewOnTouchListenerArray[i - 2]);
			}

			_dataMap.put(CALL_CONTROLLER_ITEM_PARENTRELATIVELAYOUT,
					_callControllerItemParentRelaviteLayoutData);
			_dataMap.put(CALL_CONTROLLER_ITEM_ICON,
					_callControllerGridViewContentArray[i][1]);
			_dataMap.put(CALL_CONTROLLER_ITEM_LABEL,
					_callControllerGridViewContentArray[i][2]);

			// add data to list
			_callControllerDataList.add(_dataMap);
		}

		return new OutgoingCallControllerAdapter(
				this,
				_callControllerDataList,
				R.layout.call_controller_item,
				new String[] { CALL_CONTROLLER_ITEM_PARENTRELATIVELAYOUT,
						CALL_CONTROLLER_ITEM_ICON, CALL_CONTROLLER_ITEM_LABEL },
				new int[] { R.id.callController_item_relativeLayout,
						R.id.callController_item_iconImgView,
						R.id.callController_item_labelTextView });
	}

	// generate keyboard adapter
	private ListAdapter generateKeyboardAdapter() {
		// keyboard adapter data key
		final String KEYBOARD_BUTTON = "keyboard_button";

		// define keyboard gridView image resource content
		final int[] _keyboardGridViewImgResourceContentArray = {
				R.drawable.img_dial_1_btn, R.drawable.img_dial_2_btn,
				R.drawable.img_dial_3_btn, R.drawable.img_dial_4_btn,
				R.drawable.img_dial_5_btn, R.drawable.img_dial_6_btn,
				R.drawable.img_dial_7_btn, R.drawable.img_dial_8_btn,
				R.drawable.img_dial_9_btn, R.drawable.img_dial_star_btn,
				R.drawable.img_dial_0_btn, R.drawable.img_dial_pound_btn };

		// set keyboard button data list
		List<Map<String, ?>> _keyboardButtonDataList = new ArrayList<Map<String, ?>>();

		for (int i = 0; i < _keyboardGridViewImgResourceContentArray.length; i++) {
			// generate data
			Map<String, Object> _dataMap = new HashMap<String, Object>();

			// value map
			Map<String, Object> _valueMap = new HashMap<String, Object>();
			_valueMap.put(OutgoingCallKeyboardAdapter.KEYBOARD_BUTTON_CODE, i);
			_valueMap.put(OutgoingCallKeyboardAdapter.KEYBOARD_BUTTON_IMAGE,
					_keyboardGridViewImgResourceContentArray[i]);
			switch (i) {
			case 0:
				// set top left keyboard button background
				_valueMap.put(
						OutgoingCallKeyboardAdapter.KEYBOARD_BUTTON_BGRESOURCE,
						R.drawable.callcontroller_contactitem6keyboard_1btn_bg);
				break;

			case 2:
				// set top right keyboard button background
				_valueMap
						.put(OutgoingCallKeyboardAdapter.KEYBOARD_BUTTON_BGRESOURCE,
								R.drawable.callcontroller_keyboarditem6keyboard_3btn_bg);
				break;

			case 9:
				// set bottom left keyboard button background
				_valueMap.put(
						OutgoingCallKeyboardAdapter.KEYBOARD_BUTTON_BGRESOURCE,
						R.drawable.callcontroller_keyboard_starbtn_bg);
				break;

			case 11:
				// set bottom right keyboard button background
				_valueMap.put(
						OutgoingCallKeyboardAdapter.KEYBOARD_BUTTON_BGRESOURCE,
						R.drawable.callcontroller_keyboard_poundbtn_bg);
				break;

			default:
				// set normal keyboard button background
				_valueMap.put(
						OutgoingCallKeyboardAdapter.KEYBOARD_BUTTON_BGRESOURCE,
						R.drawable.keyboard_btn_bg);
				break;
			}
			_valueMap
					.put(OutgoingCallKeyboardAdapter.KEYBOARD_BUTTON_ONCLICKLISTENER,
							new KeyboardBtnOnClickListener());

			// put value
			_dataMap.put(KEYBOARD_BUTTON, _valueMap);

			// add data to list
			_keyboardButtonDataList.add(_dataMap);
		}

		return new OutgoingCallKeyboardAdapter(this, _keyboardButtonDataList,
				R.layout.keyboard_btn_layout, new String[] { KEYBOARD_BUTTON },
				new int[] { R.id.keyboardBtn_imageBtn });
	}

	// show or hide keyboard
	private void show6hideKeyboard(boolean isShowKeyboard) {
		// get dtmf textView
		TextView _dtmfTextView = (TextView) findViewById(R.id.dtmf_textView);

		// define an new 3D rotation is used to trigger the next animation
		CTRotate3DAnimation _rotate3DAnimation;

		// check is show keyboard
		if (isShowKeyboard) {
			// show hide keyboard image button
			_mHideKeyboardBtn.setVisibility(View.VISIBLE);

			// init the 3D rotation animation with the supplied parameter the
			// animation listener
			_rotate3DAnimation = new CTRotate3DAnimation(0.0f, -90.0f,
					_mCenterContentRelativeLayout.getWidth() / 2.0f,
					_mCenterContentRelativeLayout.getHeight() / 2.0f,
					1.4f * _mCenterContentRelativeLayout.getWidth() / 2.0f,
					true);
			_rotate3DAnimation
					.setAnimationListener(new Show7HideKeyboard3DRotationAnimationListener(
							ThreeDimensionalRotateDirection.HORIZONTAL_LEFT));

			// reset hangup image button source image
			_mHangupBtn.setImageResource(R.drawable.img_hangup_btn_short);

			// clear dtmf textView text
			_dtmfTextView.setText("");
		} else {
			// hide hide keyboard image button
			_mHideKeyboardBtn.setVisibility(View.GONE);

			// init the 3D rotation animation with the supplied parameter the
			// animation listener
			_rotate3DAnimation = new CTRotate3DAnimation(-180.0f, -90.0f,
					_mCenterContentRelativeLayout.getWidth() / 2.0f,
					_mCenterContentRelativeLayout.getHeight() / 2.0f,
					1.4f * _mCenterContentRelativeLayout.getWidth() / 2.0f,
					true);
			_rotate3DAnimation
					.setAnimationListener(new Show7HideKeyboard3DRotationAnimationListener(
							ThreeDimensionalRotateDirection.HORIZONTAL_RIGHT));

			// reset hangup image button source image
			_mHangupBtn.setImageResource(R.drawable.img_hangup_btn_long);

			// show callee textView and hide dtmf textView
			((TextView) findViewById(R.id.callee_textView))
					.setVisibility(View.VISIBLE);
			_dtmfTextView.setVisibility(View.GONE);
		}

		// set the 3D rotation animation duration, fill after state and
		// interpolator
		_rotate3DAnimation.setDuration(200);
		_rotate3DAnimation.setFillAfter(true);
		_rotate3DAnimation.setInterpolator(new AccelerateInterpolator());

		// start the 3D rotation animation of center content ralativeLayout
		_mCenterContentRelativeLayout.startAnimation(_rotate3DAnimation);
	}

	// terminate sip voice call
	private void terminateSipVoiceCall(SipVoiceCallTerminatedType terminatedType) {
		// update sip voice call terminated type
		_mSipVoiceCallTerminatedType = terminatedType;

		// update outgoingCall activity UI
		// disable hangup and hide keyboard button
		if (_mHangupBtn.isShown()) {
			_mHangupBtn.setEnabled(false);
		}
		if (_mHideKeyboardBtn.isShown()) {
			_mHideKeyboardBtn.setEnabled(false);
		}

		// get and release sip services audio/video session state broadcast
		// receiver
		BroadcastReceiver _avSessionStateBroadcastReceiver = SIPSERVICES
				.getAVSessionStateBroadcastReceiver();

		// check sip audio/video session state broadcast receiver
		if (null != _avSessionStateBroadcastReceiver) {
			CTApplication.getContext().unregisterReceiver(
					_avSessionStateBroadcastReceiver);

			SIPSERVICES.setAVSessionStateBroadcastReceiver(null);
		}

		// get call duration: seconds, chronomater getBase method return
		// milliseconds
		long _callDuration = _mIsOutgoingCallEstablished ? (SystemClock
				.elapsedRealtime() - _mCallDurationChronometer.getBase()) / 1000
				: 0L;

		// check sip voice call terminated type
		switch (terminatedType) {
		case INITIATIVE:
			// hangup current sip voice call
			if (!SIPSERVICES.hangupSipVoiceCall(_callDuration)) {
				// stop call duration chronometer
				_mCallDurationChronometer.stop();

				// check if or not need to go to Chinese telephone tab activity
				// before finish
				checkBeforeFinish();

				// force finish outgoing call activity
				finish();

				// return immediately
				return;
			} else {
				// update call state textView text
				_mCallStateTextView.setText(R.string.end_outgoing_call);
			}

			break;

		case PASSIVE:
		default:
			// update call log call duration time
			SIPSERVICES.updateSipVoiceCallLog(_callDuration);

			break;
		}

		// stop call duration chronometer
		_mCallDurationChronometer.stop();

		// delayed 0.5 second to terminating
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				// update call state textView text
				_mCallStateTextView.setText(R.string.outgoing_call_ended);

				// delayed 0.6 second to back
				new Handler().postDelayed(new Runnable() {

					@Override
					public void run() {
						checkBeforeFinish();

						// finish outgoing call activity
						finish();
					}
				}, 600);
			}
		}, 500);
	}

	private void initExportedOutgoingCall() {
		// init sip registration state listener
		_mSipRegistrationStateListener = new SipRegistrationStateListenerImp();

		// define callee name, default is callee phone
		String _calleeName = getResources().getString(
				R.string.dial_phone_callee_unknown);

		// define constant
		final String[] _projection = new String[] { PhoneLookup.DISPLAY_NAME };
		Uri _phoneLookupUri = Uri.withAppendedPath(
				PhoneLookup.CONTENT_FILTER_URI, Uri.encode(_mCalleePhone));

		// use contentResolver to query contacts table
		Cursor _displayNameCursor = CTApplication.getContext()
				.getContentResolver()
				.query(_phoneLookupUri, _projection, null, null, null);

		// check displayName cursor and traverse result
		if (null != _displayNameCursor) {
			// check has next
			if (_displayNameCursor.moveToNext()) {
				// update callee name
				_calleeName = _displayNameCursor.getString(_displayNameCursor
						.getColumnIndex(PhoneLookup.DISPLAY_NAME));
			}

			// close displayName cursor
			_displayNameCursor.close();
		}

		// set callee textView text
		((TextView) findViewById(R.id.callState_textView))
				.setText(_mCalleePhone);
		((TextView) findViewById(R.id.callee_textView)).setText(_calleeName);
	}

	private void generateExportedOutgoingCall(String calleeName,
			String calleePhone, SipCallMode callMode) {
		// get cancel exported outgoing call button and linearLayout
		ImageButton _cancelExportedOutgoingCallImgBtn = (ImageButton) findViewById(R.id.cancel_exportedOutgoingCall_button);
		LinearLayout _exportedOutgoingCallLinearLayout = (LinearLayout) findViewById(R.id.exportedOutgoingCall_linearLayout);

		UserBean telUser = UserManager.getInstance().getUser();
		if (calleePhone.matches("^[2-9]{1}\\d{2,7}")
				&& (null == (String) telUser.getValue(TelUser.local_area_code
						.name()) || "".equalsIgnoreCase((String) telUser
						.getValue(TelUser.local_area_code.name())))) {
			// hide cancel exported outgoing call linearLayout
			_exportedOutgoingCallLinearLayout.setVisibility(View.GONE);

			NoLocalAreaCodePopupWindow _noLocalAreaCodePopupWindow = new NoLocalAreaCodePopupWindow(
					R.layout.no_areacode_popupwindow_layout,
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

			_noLocalAreaCodePopupWindow
					.setDependentView(_mCenterContentRelativeLayout);

			_noLocalAreaCodePopupWindow.showAtLocation(
					_mCenterContentRelativeLayout, Gravity.CENTER, 0, 0);
		} else {
			// update call state textView text
			_mCallStateTextView.setText(R.string.outgoing_call_trying);

			// hide cancel exported outgoing call linearLayout and button
			_cancelExportedOutgoingCallImgBtn.setVisibility(View.GONE);
			_exportedOutgoingCallLinearLayout.setVisibility(View.GONE);

			switch (callMode) {
			case DIRECT_CALL:
				// show call controller gridView and call controller footer
				// linearLayout
				((GridView) findViewById(R.id.callController_gridView))
						.setVisibility(View.VISIBLE);
				((LinearLayout) findViewById(R.id.callController_footerLinearLayout))
						.setVisibility(View.VISIBLE);

				break;

			default:
			case CALLBACK:
				// show back for waiting callback call button
				((ImageButton) findViewById(R.id.back4waiting_callbackCall_button))
						.setVisibility(View.VISIBLE);

				break;
			}

			// make sip voice call
			SipUtils.makeSipVoiceCall(SipCallSponsor.outer, calleeName,
					calleePhone, callMode);
		}
	}

	private void checkBeforeFinish() {
		// get addressBook manager reference
		AddressBookManager _addressBookManagerRef = AddressBookManager
				.getInstance();

		if (null != _mSipRegistrationStateListener
				&& _addressBookManagerRef.getAllContactsInfoArray().isEmpty()) {
			Log.d(LOG_TAG,
					"Exported outgoing call, traversal address book again");

			// traversal address book
			AddressBookManager.setFilterMode(AddressBookManager.FILTER_DEFAULT);
			_addressBookManagerRef.traversalAddressBook();

			// init all name phonetic sorted contacts info array
			ContactListTabContentActivity
					.initNamePhoneticSortedContactsInfoArray();

			// goto Chinese telephone tab activity
			Intent _chineseTelephoneTabIntent = new Intent(this,
					ChineseTelephoneTabActivity.class);
			_chineseTelephoneTabIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			_chineseTelephoneTabIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(_chineseTelephoneTabIntent);
		}
	}

	public void onClickExportedOutgoingCallDirectDial(View v) {
		Log.d(LOG_TAG, "Exported outgoing call direct dial");

		// check contact for generating an new outgoing call:
		// direct dial
		if (NetworkInfoUtils.isCurrentActiveNetworkAvailable()) {
			// get callee name
			String _calleeName = (String) ((TextView) findViewById(R.id.callee_textView))
					.getText();

			generateExportedOutgoingCall(_calleeName, _mCalleePhone,
					SipCallMode.DIRECT_CALL);
		} else {
			// show there is no active and available network currently
			new AlertDialog.Builder(OutgoingCallActivity.this)
					.setTitle(
							R.string.noActiveAvailableNetwork_alertDialog_title)
					.setMessage(
							R.string.noActiveAvailableNetwork_alertDialog_message)
					.setNegativeButton(
							R.string.noActiveAvailableNetwork_alertDialog_setLaterBtn_title,
							null)
					.setPositiveButton(
							R.string.noActiveAvailableNetwork_alertDialog_setNowBtn_title,
							new ModifyWirelessSettingsBtnOnClickListener())
					.show();
		}
	}

	public void onClickExportedOutgoingCallCallback(View v) {
		Log.d(LOG_TAG, "Exported outgoing call callback");

		// check contact for generating an new outgoing call: callback
		if (NetworkInfoUtils.isCurrentActiveNetworkAvailable()) {
			// get callee name
			String _calleeName = (String) ((TextView) findViewById(R.id.callee_textView))
					.getText();

			generateExportedOutgoingCall(_calleeName, _mCalleePhone,
					SipCallMode.CALLBACK);
		} else {
			// show there is no active and available network currently
			new AlertDialog.Builder(OutgoingCallActivity.this)
					.setTitle(
							R.string.noActiveAvailableNetwork_alertDialog_title)
					.setMessage(
							R.string.noActiveAvailableNetwork_alertDialog_message)
					.setNegativeButton(
							R.string.noActiveAvailableNetwork_alertDialog_setLaterBtn_title,
							null)
					.setPositiveButton(
							R.string.noActiveAvailableNetwork_alertDialog_setNowBtn_title,
							new ModifyWirelessSettingsBtnOnClickListener())
					.show();
		}
	}

	public void onClickExportedOutgoingCallAutoSelect(View v) {
		Log.d(LOG_TAG, "Exported outgoing call auto select");

		// check contact for generating an new outgoing call:
		// direct dial
		if (NetworkInfoUtils.isCurrentActiveNetworkAvailable()) {
			// get callee name
			String _calleeName = (String) ((TextView) findViewById(R.id.callee_textView))
					.getText();

			try {
				// get and check current active network type
				switch (NetworkInfoUtils.getNetworkType()) {
				case ConnectivityManager.TYPE_WIFI:
					// check contact for generating an new outgoing
					// call: auto direct dial
					generateExportedOutgoingCall(_calleeName, _mCalleePhone,
							SipCallMode.DIRECT_CALL);
					break;

				case ConnectivityManager.TYPE_MOBILE:
				default:
					// check contact for generating an new outgoing
					// call: auto callback
					generateExportedOutgoingCall(_calleeName, _mCalleePhone,
							SipCallMode.CALLBACK);
					break;
				}
			} catch (NoActiveNetworkException e) {
				Log.e(LOG_TAG,
						"Generate an new outgoing call error, because here is no active network currently, exception message = "
								+ e.getMessage());

				e.printStackTrace();
			}
		} else {
			// show there is no active and available network currently
			new AlertDialog.Builder(OutgoingCallActivity.this)
					.setTitle(
							R.string.noActiveAvailableNetwork_alertDialog_title)
					.setMessage(
							R.string.noActiveAvailableNetwork_alertDialog_message)
					.setNegativeButton(
							R.string.noActiveAvailableNetwork_alertDialog_setLaterBtn_title,
							null)
					.setPositiveButton(
							R.string.noActiveAvailableNetwork_alertDialog_setNowBtn_title,
							new ModifyWirelessSettingsBtnOnClickListener())
					.show();
		}
	}

	// inner class
	// outgoing call controller adapter
	class OutgoingCallControllerAdapter extends CTListAdapter {

		private static final String LOG_TAG = "OutgoingCallControllerAdapter";

		// call controller gridView keys
		private static final String CALL_CONTROLLER_ITEM_BACKGROUND = "call_controller_item_background";
		private static final String CALL_CONTROLLER_ITEM_ONTOUCHLISTENER = "call_controller_item_onTouchListener";

		public OutgoingCallControllerAdapter(Context context,
				List<Map<String, ?>> data, int itemsLayoutResId,
				String[] dataKeys, int[] itemsComponentResIds) {
			super(context, data, itemsLayoutResId, dataKeys,
					itemsComponentResIds);
		}

		@Override
		protected void bindView(View view, Map<String, ?> dataMap,
				String dataKey) {
			// get item data object
			Object _itemData = dataMap.get(dataKey);

			// check view type
			// relativeLayout
			if (view instanceof RelativeLayout) {
				try {
					// define item data map and convert item data to map
					@SuppressWarnings("unchecked")
					Map<String, Object> _itemDataMap = (Map<String, Object>) _itemData;

					// get item data map values
					Integer _itemBackgroundResource = (Integer) _itemDataMap
							.get(CALL_CONTROLLER_ITEM_BACKGROUND);
					OnTouchListener _itemOnTouchListener = (OnTouchListener) _itemDataMap
							.get(CALL_CONTROLLER_ITEM_ONTOUCHLISTENER);

					// set call controller item background resource and on touch
					// listener
					((RelativeLayout) view)
							.setBackgroundResource(_itemBackgroundResource);
					if (null != _itemOnTouchListener) {
						((RelativeLayout) view)
								.setOnTouchListener(_itemOnTouchListener);
					}
				} catch (Exception e) {
					e.printStackTrace();

					Log.e(LOG_TAG,
							"Convert item data to map error, item data = "
									+ _itemData);
				}
			}
			// textView
			else if (view instanceof TextView) {
				// set view text
				if (null == _itemData) {
					((TextView) view).setText("");
				} else if (_itemData instanceof Integer) {
					((TextView) view).setText((Integer) _itemData);
				} else {
					((TextView) view).setText(_itemData.toString());
				}
			}
			// imageView
			else if (view instanceof ImageView) {
				try {
					// define item data integer and convert item data to integer
					Integer _itemDataInteger = (Integer) _itemData;

					// set imageView image resource
					((ImageView) view).setImageResource(_itemDataInteger);
				} catch (Exception e) {
					e.printStackTrace();

					Log.e(LOG_TAG,
							"Convert item data to integer error, item data = "
									+ _itemData);
				}
			}
		}

	}

	// call controller gridView on item click listener
	class CallControllerGridViewOnItemClickListener implements
			OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// check call controller gridView item on clicked
			switch (position) {
			case 0:
				// show contacts list
				// open contacts list sliding drawer
				((SlidingDrawer) findViewById(R.id.contactslist_slidingDrawer))
						.animateOpen();

				break;

			case 1:
				// show keyboard gridView and hide keyboard image button
				show6hideKeyboard(true);

				break;

			case 2:
				Log.e(LOG_TAG,
						"Mute or unmute call controller gridView item on item click listener error");

				break;

			case 3:
				Log.e(LOG_TAG,
						"Handfree or cancel handfree call controller gridView item on item click listener error");

				break;
			}
		}
	}

	// hide contacts list on click listener
	class HideContactsListOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// hide contacts list
			// check contact listView alphabet toast and visibility
			if (null != _mContactListViewQuickAlphabetToast
					&& _mContactListViewQuickAlphabetToast.isShowing()) {
				_mContactListViewQuickAlphabetToast.cancel();
			}

			// close contacts list sliding drawer
			((SlidingDrawer) findViewById(R.id.contactslist_slidingDrawer))
					.animateClose();
		}

	}

	// mute and unmute call controller gridView on item touch listener
	class Mute6UnmuteCallControllerGridViewOnTouchListener implements
			OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (MotionEvent.ACTION_DOWN == event.getAction()) {
				// check current sip voice call is muted
				// muted now, unmute it
				if (SIPSERVICES.isSipVoiceCallMuted()) {
					// update background resource
					((RelativeLayout) v)
							.setBackgroundResource(R.drawable.callcontroller_muteitem_bg);

					// unmute current sip voice call
					SIPSERVICES.unmuteSipVoiceCall();
				}
				// unmuted now, mute it
				else {
					// update background resource
					((RelativeLayout) v)
							.setBackgroundResource(R.drawable.callcontroller_unmuteitem_bg);

					// mute current sip voice call
					SIPSERVICES.muteSipVoiceCall();
				}
			}

			return true;
		}

	}

	// set using loudspeaker and earphone call controller gridView on item touch
	// listener
	class SetUsingLoudspeaker6EarphoneCallControllerGridViewOnTouchListener
			implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (MotionEvent.ACTION_DOWN == event.getAction()) {
				// check current sip voice call using loudspeaker or earphone
				// using loudspeaker now, set using earphone
				if (SIPSERVICES.isSipVoiceCallUsingLoudspeaker()) {
					// update background resource
					((RelativeLayout) v)
							.setBackgroundResource(R.drawable.callcontroller_handfreeitem_bg);

					// set using earphone
					SIPSERVICES.setSipVoiceCallUsingEarphone();
				}
				// using earphone now, set using loudspeaker
				else {
					// update background resource
					((RelativeLayout) v)
							.setBackgroundResource(R.drawable.callcontroller_cancelhandfreeitem_bg);

					// set using loudspeaker
					SIPSERVICES.setSipVoiceCallUsingLoudspeaker();
				}
			}

			return true;
		}

	}

	// outgoing call keyboard adapter
	class OutgoingCallKeyboardAdapter extends CTListAdapter {

		private static final String LOG_TAG = "OutgoingCallKeyboardAdapter";

		// keyboard gridView keys
		private static final String KEYBOARD_BUTTON_CODE = "keyboard_button_code";
		private static final String KEYBOARD_BUTTON_IMAGE = "keyboard_button_image";
		private static final String KEYBOARD_BUTTON_BGRESOURCE = "keyboard_button_background_resource";
		private static final String KEYBOARD_BUTTON_ONCLICKLISTENER = "keyboard_button_onClickListener";

		public OutgoingCallKeyboardAdapter(Context context,
				List<Map<String, ?>> data, int itemsLayoutResId,
				String[] dataKeys, int[] itemsComponentResIds) {
			super(context, data, itemsLayoutResId, dataKeys,
					itemsComponentResIds);
		}

		@Override
		protected void bindView(View view, Map<String, ?> dataMap,
				String dataKey) {
			// get item data object
			Object _itemData = dataMap.get(dataKey);

			// check view type
			// image button
			if (view instanceof ImageButton) {
				try {
					// define item data map and convert item data to map
					@SuppressWarnings("unchecked")
					Map<String, Object> _itemDataMap = (Map<String, Object>) _itemData;

					// set image button attributes
					((ImageButton) view).setTag(_itemDataMap
							.get(KEYBOARD_BUTTON_CODE));
					((ImageButton) view)
							.setImageResource((Integer) _itemDataMap
									.get(KEYBOARD_BUTTON_IMAGE));
					((ImageButton) view)
							.setBackgroundResource((Integer) _itemDataMap
									.get(KEYBOARD_BUTTON_BGRESOURCE));
					((ImageButton) view)
							.setOnClickListener((OnClickListener) _itemDataMap
									.get(KEYBOARD_BUTTON_ONCLICKLISTENER));
				} catch (Exception e) {
					e.printStackTrace();

					Log.e(LOG_TAG,
							"Convert item data to map error, item data = "
									+ _itemData);
				}
			}
		}

	}

	// keyboard button on click listener
	class KeyboardBtnOnClickListener implements OnClickListener {

		// define keyboard button value data
		private final String[] _keyboardPhoneButtonValueData = new String[] {
				"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#" };

		@Override
		public void onClick(View v) {
			// get callee and dtmf textView
			TextView _calleeTextView = (TextView) findViewById(R.id.callee_textView);
			TextView _dtmfTextView = (TextView) findViewById(R.id.dtmf_textView);

			// check callee and dtmf textView visible
			if (_calleeTextView.isShown()) {
				// hide callee textView
				_calleeTextView.setVisibility(View.GONE);
			}
			if (!_dtmfTextView.isShown()) {
				// show dtmf textView
				_dtmfTextView.setVisibility(View.VISIBLE);
			}

			// get clicked phone number
			String _clickedPhoneNumber = _keyboardPhoneButtonValueData[(Integer) v
					.getTag()];

			// define keyboard phone string builder
			StringBuilder _keyboardPhoneStringBuilder = new StringBuilder(
					_dtmfTextView.getText());

			// dial phone
			_keyboardPhoneStringBuilder.append(_clickedPhoneNumber);

			// reset dtmf textView text
			_dtmfTextView.setText(_keyboardPhoneStringBuilder);

			// play dial phone button dtmf sound with index
			ToneGeneratorUtils.getInstance()
					.playDTMFSound((Integer) v.getTag());

			// send dtmf signal
			SIPSERVICES.sentDTMF(_clickedPhoneNumber);
		}
	}

	// cancel exported outgoing call or back for waiting callback call button on
	// click listener
	class CancelExportedOutgoingCall6Back4WaitingCallbackCallBtnOnClickListener
			implements OnClickListener {

		@Override
		public void onClick(View v) {
			// check sip registration state listener and cancel voip online
			// status
			if (null != _mSipRegistrationStateListener) {
				SipRegistrationStateListenerImp.cancelVOIPOnlineStatus();
			}

			// finish outgoing call activity
			finish();
		}

	}

	// hangup outgoing call button on click listener
	class HangupOutgoingCallBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// terminate current sip voice call
			terminateSipVoiceCall(SipVoiceCallTerminatedType.INITIATIVE);
		}

	}

	// hide keyboard button on click listener
	class HideKeyboardBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// hide keyboard gridView and hide keyboard image button
			show6hideKeyboard(false);
		}

	}

	// show and hide keyboard gridView rotate 3D animation listener
	class Show7HideKeyboard3DRotationAnimationListener implements
			AnimationListener {

		// 3d rotate direction
		private ThreeDimensionalRotateDirection _m3DRotateDirection;

		public Show7HideKeyboard3DRotationAnimationListener(
				ThreeDimensionalRotateDirection rotateDirection) {
			super();

			// set 3d rotate direction
			_m3DRotateDirection = rotateDirection;
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			// add runnable to message queue to handle views for 3D rotate
			// swapping
			_mCenterContentRelativeLayout.post(new Runnable() {

				@Override
				public void run() {
					// define an new 3D rotation is used to swap views
					CTRotate3DAnimation _rotate3DAnimation = null;

					// check 3D rotate direction
					if (ThreeDimensionalRotateDirection.HORIZONTAL_LEFT == _m3DRotateDirection) {
						// show keyboard gridView and hide call controller
						// gridView
						_mKeyboardGridView.setVisibility(View.VISIBLE);
						if (null == _mCenterContentRelativeLayout
								.findViewById(R.id.keyboard_gridView)) {
							// rotate keyboard gridView 180 degree
							CTRotate3DAnimation
									.static3DRotate4View(
											_mKeyboardGridView,
											ThreeDimensionalRotateDirection.HORIZONTAL_RIGHT,
											new Point(
													(DisplayScreenUtils
															.screenWidth() - (DisplayScreenUtils
															.dp2pix(2 * 16) + 2 * (int) getResources()
															.getDimension(
																	R.dimen.keyboard_gridView_marginLeft7Right))) / 2,
													(DisplayScreenUtils
															.screenHeight()
															- DisplayScreenUtils
																	.statusBarHeight() - DisplayScreenUtils
															.dp2pix(2 * (16 + getResources()
																	.getDimension(
																			R.dimen.keyboard_gridView_marginTop7Bottom)))) / 2));

							_mCenterContentRelativeLayout
									.addView(_mKeyboardGridView);
						}
						((GridView) findViewById(R.id.callController_gridView))
								.setVisibility(View.GONE);

						// init the 3D rotation animation
						_rotate3DAnimation = new CTRotate3DAnimation(
								-90.0f,
								-180.0f,
								_mCenterContentRelativeLayout.getWidth() / 2.0f,
								_mCenterContentRelativeLayout.getWidth() / 2.0f,
								1.4f * _mCenterContentRelativeLayout.getWidth() / 2.0f,
								false);
					} else if (ThreeDimensionalRotateDirection.HORIZONTAL_RIGHT == _m3DRotateDirection) {
						// hide keyboard gridView and show call controller
						// gridView
						_mCenterContentRelativeLayout
								.removeView(_mKeyboardGridView);
						((GridView) findViewById(R.id.callController_gridView))
								.setVisibility(View.VISIBLE);

						// init the 3D rotation animation
						_rotate3DAnimation = new CTRotate3DAnimation(
								-90.0f,
								0.0f,
								_mCenterContentRelativeLayout.getWidth() / 2.0f,
								_mCenterContentRelativeLayout.getWidth() / 2.0f,
								1.4f * _mCenterContentRelativeLayout.getWidth() / 2.0f,
								false);
					} else {
						Log.e(LOG_TAG,
								"3D rotate direction vertical up and down not implement, 3D rotate direction = "
										+ _m3DRotateDirection);
					}

					// check rotate 3D animation
					if (null != _rotate3DAnimation) {
						// set the 3D rotation animation duration, fill after
						// state and interpolator
						_rotate3DAnimation.setDuration(200);
						_rotate3DAnimation.setFillAfter(true);
						_rotate3DAnimation
								.setInterpolator(new DecelerateInterpolator());

						// start the 3D rotation animation of center content
						// ralativeLayout
						_mCenterContentRelativeLayout
								.startAnimation(_rotate3DAnimation);
					}
				}

			});
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// nothing to do
		}

		@Override
		public void onAnimationStart(Animation animation) {
			// nothing to do
		}

	}

	// send callback sip voice call http request listener
	class SendCallbackSipVoiceCallHttpRequestListener extends
			OnHttpRequestListener {

		// define send callback sip voice call state tip text id, callback
		// waiting imageView image resource id and callback waiting textView
		// text
		Integer _sendCallbackSipVoiceCallStateTipTextId = R.string.send_callbackCallRequest_failed;
		Integer _callbackCallWaitingImageViewImgResId = R.drawable.img_sendcallbackcall_failed;
		String _callbackCallWaitingTextViewText = CTApplication.getContext()
				.getResources()
				.getString(R.string.callbackWaiting_textView_failed);

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			// check send callback sip voice call request response
			checkSendCallbackSipVoiceCallRequestResponse(responseResult);
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			Log.e(LOG_TAG, "Send callback sip voice call http request failed");
			Log.d(SystemConstants.TAG,
					"SendCallbackSipVoiceCallHttpRequestListener - status code: "
							+ responseResult.getStatusCode() + " text: "
							+ responseResult.getResponseText());

			// check send callback sip voice call request response
			checkSendCallbackSipVoiceCallRequestResponse(responseResult);
		}

		// check send callback sip voice call request response
		private void checkSendCallbackSipVoiceCallRequestResponse(
				HttpResponseResult responseResult) {
			_sendCallbackSipVoiceCallStateTipTextId = R.string.send_callbackCallRequest_failed;
			_callbackCallWaitingImageViewImgResId = R.drawable.img_sendcallbackcall_failed;
			_callbackCallWaitingTextViewText = CTApplication.getContext()
					.getResources()
					.getString(R.string.callbackWaiting_textView_failed);

			// update send callback sip voice call state tip text id, callback
			// waiting imageView image resource id and callback waiting textView
			// text
			if (responseResult.getStatusCode() == HttpStatus.SC_OK) {
				try {
					JSONObject data = new JSONObject(
							responseResult.getResponseText());
					if (200 == data.getInt("vos_status_code")) {
						_sendCallbackSipVoiceCallStateTipTextId = R.string.send_callbackCallRequest_succeed;
						_callbackCallWaitingImageViewImgResId = R.drawable.img_sendcallbackcall_succeed;
						UserBean user = UserManager.getInstance().getUser();
						_callbackCallWaitingTextViewText = String
								.format(getResources()
										.getString(
												R.string.callbackWaiting_textView_succeed),
										((String) user
												.getValue(TelUser.bindphone_country_code
														.name()))
												+ ((String) user
														.getValue(TelUser.bindphone
																.name())),
										_mCalleePhone);
					} else {
						// update sip voice call failed call log
						SIPSERVICES.updateSipVoiceCallLog(-1L);
					}

				} catch (JSONException e) {
					e.printStackTrace();
					// update sip voice call failed call log
					SIPSERVICES.updateSipVoiceCallLog(-1L);
				}

			} else {
				// update sip voice call failed call log
				SIPSERVICES.updateSipVoiceCallLog(-1L);

				if (responseResult.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					try {
						JSONObject data = new JSONObject(
								responseResult.getResponseText());
						String vosInfo = data.getString("vos_info");
						// update call state textView text
						((TextView) findViewById(R.id.callState_textView))
								.setText(vosInfo);

						// update callback waiting imageView image resource
						((ImageView) findViewById(R.id.callbackWaiting_imageView))
								.setImageResource(_callbackCallWaitingImageViewImgResId);

						// update callback waiting textView text
						((TextView) findViewById(R.id.callbackWaiting_textView))
								.setText(_callbackCallWaitingTextViewText);

						// show callback waiting relativeLayout
						((RelativeLayout) findViewById(R.id.callbackWaiting_relativeLayout))
								.setVisibility(View.VISIBLE);
						return;
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else if (responseResult.getStatusCode() == -1) {
					_sendCallbackSipVoiceCallStateTipTextId = R.string.network_timeout;
				}
			}

			// update call state textView text
			_mCallStateTextView
					.setText(_sendCallbackSipVoiceCallStateTipTextId);

			// update callback waiting imageView image resource
			((ImageView) findViewById(R.id.callbackWaiting_imageView))
					.setImageResource(_callbackCallWaitingImageViewImgResId);

			// update callback waiting textView text
			((TextView) findViewById(R.id.callbackWaiting_textView))
					.setText(_callbackCallWaitingTextViewText);

			// show callback waiting relativeLayout
			((RelativeLayout) findViewById(R.id.callbackWaiting_relativeLayout))
					.setVisibility(View.VISIBLE);
		}

	}

	// phone state broadcast receiver
	class PhoneStateBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// check the action for phone state
			if (!Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
				// incoming call
				// check incoming call state
				switch (((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
						.getCallState()) {
				case TelephonyManager.CALL_STATE_RINGING:
					// check outgoing call sip voice call mode
					if (SipCallMode.CALLBACK == _mOutgoingCallMode) {
						// callback
						// finish outgoing call activity for making callback sip
						// voice call if has incoming call
						finish();
					} else {
						// direct
						// incoming call
						Log.d(LOG_TAG,
								"There is a sip voice call, so reject the incoming call");

						// reject the incoming call
						TelephonyManagerExtension.rejectIncomingCall();
					}
					break;

				default:
					// nothing to do
					break;
				}
			}
		}

	}

	// sip voice call terminated type
	enum SipVoiceCallTerminatedType {
		// initiative or passive
		INITIATIVE, PASSIVE
	}

	// modify android system wireless settings button on click listener
	class ModifyWirelessSettingsBtnOnClickListener implements
			android.content.DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// define android system wireless settings intent
			Intent _wirelessSettingsIntent = new Intent(
					Settings.ACTION_WIRELESS_SETTINGS);

			// check wireless settings intent and start the activity
			if (CommonUtils.isIntentAvailable(_wirelessSettingsIntent)) {
				OutgoingCallActivity.this
						.startActivity(_wirelessSettingsIntent);
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
	protected void onPause() {
		Log.d(SystemConstants.TAG, "outgoingcall on pause");
		if (_mContactListViewQuickAlphabetToast != null
				&& _mContactListViewQuickAlphabetToast.isShowing()) {
			Log.d(SystemConstants.TAG, "cancel toast");
			_mContactListViewQuickAlphabetToast.cancel();
		}
		super.onPause();
	}

}
