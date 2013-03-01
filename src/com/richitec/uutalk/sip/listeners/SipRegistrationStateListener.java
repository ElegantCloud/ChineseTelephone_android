package com.richitec.uutalk.sip.listeners;

public interface SipRegistrationStateListener {

	// register success
	public void onRegisterSuccess();

	// register failed
	public void onRegisterFailed();

	// unregister success
	public void onUnRegisterSuccess();

	// unregister failed
	public void onUnRegisterFailed();

	public void onRegistering();
}
