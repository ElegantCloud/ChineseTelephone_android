package com.richitec.uutalk.sip.services;

import com.richitec.uutalk.call.SipCallMode;
import com.richitec.uutalk.sip.SipRegisterBean;
import com.richitec.uutalk.sip.listeners.SipRegistrationStateListener;

public interface ISipServices {

	// register sip account
	public void registerSipAccount(SipRegisterBean sipAccount,
			SipRegistrationStateListener sipRegistrationStateListener);

	// unregister sip account
	public void unregisterSipAccount(
			SipRegistrationStateListener sipRegistrationStateListener);

	// make sip voice call
	public void makeSipVoiceCall(String calleeName, String calleePhone,
			SipCallMode callMode);

	// hangup current sip voice call
	public boolean hangupSipVoiceCall(Long callDuration);

	// mute current sip voice call
	public void muteSipVoiceCall();

	// unmute current sip voice call
	public void unmuteSipVoiceCall();

	// set current sip voice call using loudspeaker
	public void setSipVoiceCallUsingLoudspeaker();

	// set current sip voice call using earphone
	public void setSipVoiceCallUsingEarphone();

	// send dtmf
	public void sentDTMF(String dtmfCode);

	// destroy sip engine
	public void destroySipEngine();

}
