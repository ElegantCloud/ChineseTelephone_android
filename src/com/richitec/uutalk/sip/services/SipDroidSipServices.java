package com.richitec.uutalk.sip.services;

import java.util.ArrayList;
import java.util.List;

import org.sipdroid.sipua.SipdroidEngine;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.RegisterService;
import org.sipdroid.sipua.ui.Settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.richitec.uutalk.R;
import com.richitec.uutalk.sip.SipRegisterBean;
import com.richitec.uutalk.sip.listeners.SipInviteStateListener;
import com.richitec.uutalk.sip.listeners.SipRegistrationStateListener;

public class SipDroidSipServices extends BaseSipServices implements
		ISipServices {

	private static final String LOG_TAG = SipDroidSipServices.class
			.getCanonicalName();

	public SipDroidSipServices() {
		super();

		// add sip register intent filter action
		SIPEGISTER_INTENTFILTER
				.addAction(SipdroidEngine.ACTION_REGISTRATION_EVENT);

		// add sip invite intent filter action
		SIPINVITE_INTENTFILTER.addAction(SipdroidEngine.ACTION_INVITE_EVENT);
	}

	@Override
	public void registerSipAccount(SipRegisterBean sipAccount,
			SipRegistrationStateListener sipRegistrationStateListener) {
		// update sip registration state listener
//		_mSipRegistrationStateListener = sipRegistrationStateListener;

		// register
		// init sip registration state broadcast receiver
		_mRegistrationStateBroadcastReceiver = new RegistrationStateBroadcastReceiver();
		((RegistrationStateBroadcastReceiver) _mRegistrationStateBroadcastReceiver).addSipRegistrationStateListener(sipRegistrationStateListener);
		
		// register sip registration state broadcast receiver
		_appContext.registerReceiver(_mRegistrationStateBroadcastReceiver,
				SIPEGISTER_INTENTFILTER);

		// update configuration
		Editor _edit = PreferenceManager.getDefaultSharedPreferences(
				_appContext).edit();

		// set network, use both 3g and wifi
		_edit.putBoolean(Settings.PREF_WLAN, true);
		_edit.putBoolean(Settings.PREF_3G, true);
		_edit.putBoolean(Settings.PREF_EDGE, true);

		// set credentials
		_edit.putString(Settings.PREF_USERNAME, sipAccount.getSipUserName());
		_edit.putString(Settings.PREF_PASSWORD, sipAccount.getSipPwd());
		_edit.putString(Settings.PREF_SERVER, sipAccount.getSipServer());
		_edit.putString(Settings.PREF_DOMAIN, sipAccount.getSipServer());
		_edit.putString(Settings.PREF_DNS, sipAccount.getSipServer());
		_edit.putString(Settings.PREF_PORT, sipAccount.getSipPort().toString());

		_edit.putBoolean(Settings.PREF_ON, true);

		// commit changes
		_edit.commit();

		Receiver.mSipdroidEngine = null;
		// sip account register
		Receiver.engine(_appContext).registerMore();

		setSipRegisterCalled(true);
	}

	@Override
	public void unregisterSipAccount(
			SipRegistrationStateListener sipRegistrationStateListener) {
		Receiver.engine(_appContext).unregister();

		// release sipdroid registration state broadcast receiver
		if (null != _mRegistrationStateBroadcastReceiver) {
			_appContext
					.unregisterReceiver(_mRegistrationStateBroadcastReceiver);

			_mRegistrationStateBroadcastReceiver = null;
		}

		setSipRegisterCalled(false);
	}

	@Override
	public boolean makeDirectDialSipVoiceCall(String calleeName,
			final String calleePhone) {
		// define return result
		boolean _ret = false;

		// invite
		// init sip call audio/video session state receiver
		_mAVSessionStateBroadcastReceiver = new AVSessionStateBroadcastReceiver();

		// register sipdroid audio/video session state receiver
		_appContext.registerReceiver(_mAVSessionStateBroadcastReceiver,
				SIPINVITE_INTENTFILTER);

		// check sipdroid engine sip account is registered
		if (Receiver.engine(_appContext).isRegistered()) {
			// sipdroid make an new sip voice call
			_ret = Receiver.engine(_appContext).call(calleePhone, true);
		} else {
			// add sip account register again listener to register sip
			// registration state broadcast receiver handle list
			((RegistrationStateBroadcastReceiver) _mRegistrationStateBroadcastReceiver)
					.addSipRegistrationStateListener(new SipRegistrationStateListener() {

						@Override
						public void onUnRegisterSuccess() {
						}

						@Override
						public void onUnRegisterFailed() {
						}

						@Override
						public void onRegistering() {
						}

						@Override
						public void onRegisterSuccess() {
							// remove sip account register again listener to
							// register sip registration state broadcast
							// receiver handle list
							((RegistrationStateBroadcastReceiver) _mRegistrationStateBroadcastReceiver)
									.removeSipRegistrationStateListener(this);

							// sipdroid make an new sip voice
							// call
							if (false == Receiver.engine(_appContext).call(
									calleePhone, true)) {
								// call failed
								getSipInviteStateListener().onCallFailed();
							}
						}

						@Override
						public void onRegisterFailed() {
							// remove sip account register again listener to
							// register sip registration state broadcast
							// receiver handle list
							((RegistrationStateBroadcastReceiver) _mRegistrationStateBroadcastReceiver)
									.removeSipRegistrationStateListener(this);

							// call failed
							getSipInviteStateListener().onCallFailed();

							// show call failed toast
							Toast.makeText(_appContext,
									R.string.makeDirectDialSipVoiceCallFailed,
									Toast.LENGTH_LONG).show();
						}
					});

			// register again
			Receiver.engine(_appContext).registerMore();

			// flag return result
			_ret = true;
		}

		return _ret;
	}

	@Override
	public boolean hangupSipVoiceCall() {
		// define return result
		boolean _ret = true;

		// sipdroid hangup current sip voice call
		Receiver.engine(_appContext).rejectcall();

		// check sipdroid engine sip account is registered
		if (!Receiver.engine(_appContext).isRegistered()) {
			_ret = false;
		}

		return _ret;
	}

	@Override
	public void muteSipVoiceCall() {
		// update current sip voice call muted flag
		setSipVoiceCallMuted(true);

		// sipdroid mute current sip voice call
		Receiver.engine(_appContext).togglemute();
	}

	@Override
	public void unmuteSipVoiceCall() {
		// update current sip voice call muted flag
		setSipVoiceCallMuted(false);

		// sipdroid unmute current sip voice call
		Receiver.engine(_appContext).togglemute();
	}

	@Override
	public void sentDTMF(String dtmfCode) {
		Log.d(LOG_TAG, "Send dtmf to current sip voice call, dtmf code = "
				+ dtmfCode);

		// sipdroid send dtmf to current sip voice call
		Receiver.engine(_appContext).info(dtmfCode.charAt(0), 250);
	}

	@Override
	public void destroySipEngine() {
		Editor _edit = PreferenceManager.getDefaultSharedPreferences(
				_appContext).edit();
		_edit.putBoolean(Settings.PREF_ON, false);
		_edit.commit();

		// process receiver
		Receiver.pos(true);
		Receiver.engine(_appContext).halt();
		Receiver.mSipdroidEngine = null;
		Receiver.reRegister(0);

		// stop register service
		_appContext.stopService(new Intent(_appContext, RegisterService.class));
	}

	@Override
	public void setSipVoiceCallUsingLoudspeaker() {
		// do super
		super.setSipVoiceCallUsingLoudspeaker();

		// sipdroid set current sip voice call in normal
		Receiver.engine(_appContext).speaker(AudioManager.MODE_NORMAL);
	}

	@Override
	public void setSipVoiceCallUsingEarphone() {
		// do super
		super.setSipVoiceCallUsingEarphone();

		// sipdroid set current sip voice call in call
		Receiver.engine(_appContext).speaker(AudioManager.MODE_IN_CALL);
	}

	// inner class
	// sipdroid registration state broadcast receiver
	class RegistrationStateBroadcastReceiver extends BroadcastReceiver {

		// sip register listener list
		private List<SipRegistrationStateListener> _mSipRegisterListenerList = new ArrayList<SipRegistrationStateListener>();

		// add sip registration state listener
		public List<SipRegistrationStateListener> addSipRegistrationStateListener(
				SipRegistrationStateListener sipRegistrationStateListener) {
			// add sip registration state listener to handle list
			_mSipRegisterListenerList.add(sipRegistrationStateListener);

			return _mSipRegisterListenerList;
		}

		// remove sip registration state listener
		public boolean removeSipRegistrationStateListener(
				SipRegistrationStateListener sipRegistrationStateListener) {
			boolean _ret = true;

			if (_mSipRegisterListenerList
					.contains(sipRegistrationStateListener)) {
				// remove sip registration state listener from handle list
				_mSipRegisterListenerList.remove(_mSipRegisterListenerList
						.indexOf(sipRegistrationStateListener));
			} else {
				Log.w(LOG_TAG, "Sip registration state listener = "
						+ sipRegistrationStateListener
						+ " not existed in handle list = "
						+ _mSipRegisterListenerList);

				_ret = false;
			}

			return _ret;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			// check the action for sipdroid registration Event
			if (SipdroidEngine.ACTION_REGISTRATION_EVENT.equals(intent
					.getAction())) {
				// get sipdroid registration event arguments
				String _registrationEventArgs = intent
						.getStringExtra(SipdroidEngine.EXTRA_EMBEDDED);

				// check the arguments
				if (null == _registrationEventArgs) {
					Log.e(LOG_TAG,
							"Sipdroid registration event arguments is null");
				} else {
					// check registration event type
					if (_registrationEventArgs.equalsIgnoreCase("succeed")) {
						Log.d(LOG_TAG, "You are now registered :)");

						for (SipRegistrationStateListener sipRegisterListener : _mSipRegisterListenerList) {
							sipRegisterListener.onRegisterSuccess();
						}
					} else if (_registrationEventArgs
							.equalsIgnoreCase("failed")) {
						Log.d(LOG_TAG, "Failed to register :(");

						for (SipRegistrationStateListener sipRegisterListener : _mSipRegisterListenerList) {
							sipRegisterListener.onRegisterFailed();
						}
					} else if (_registrationEventArgs
							.equalsIgnoreCase("registering")) {
						Log.d(LOG_TAG, "registering");

						for (SipRegistrationStateListener sipRegisterListener : _mSipRegisterListenerList) {
							sipRegisterListener.onRegistering();
						}
					}
				}
			}
		}

	}

	// sipdroid audio/video session state broadcast receiver
	class AVSessionStateBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// check the action for sipdroid invite event
			if (SipdroidEngine.ACTION_INVITE_EVENT.equals(intent.getAction())) {
				// get sipdroid invite event arguments
				String _inviteEventArgs = intent
						.getStringExtra(SipdroidEngine.EXTRA_EMBEDDED);

				// check the arguments
				if (null == _inviteEventArgs) {
					Log.e(LOG_TAG, "Sipdroid invite event arguments is null");
				} else {
					// get sip invite listener
					SipInviteStateListener _sipInviteStateListener = getSipInviteStateListener();

					// check sipdroid invite event type
					if (_inviteEventArgs.equalsIgnoreCase("terminated")) {
						_sipInviteStateListener.onCallTerminated();
					} else if (_inviteEventArgs.equalsIgnoreCase("incall")) {
						_sipInviteStateListener.onCallSpeaking();
					} else {
						Log.d(LOG_TAG, "Sipdroid other invite state = "
								+ _inviteEventArgs);
					}
				}
			}
		}

	}

}
