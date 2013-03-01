package com.richitec.uutalk.utils;

import android.util.Log;

import com.richitec.commontoolkit.user.UserBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.uutalk.constant.SystemConstants;
import com.richitec.uutalk.constant.TelUser;
import com.richitec.uutalk.sip.SipRegisterBean;
import com.richitec.uutalk.sip.SipUtils;
import com.richitec.uutalk.sip.listeners.SipRegistrationStateListener;

public class SipRegisterManager {
	
	public static void registSip(SipRegistrationStateListener sipRegistStateListener, String vosServerAddress){
		Log.d(SystemConstants.TAG, "SipRegisterManager - registSip");
		UserBean userBean = UserManager.getInstance().getUser();	
		String sipName = (String) userBean.getValue(TelUser.vosphone.name());
		String sipPsw = (String) userBean.getValue(TelUser.vosphone_pwd.name());
		
		if(sipName!=null&&!sipName.equals("")&&sipPsw!=null&&!sipPsw.equals("")){
			// generate sip register account
			SipRegisterBean _sipAccount = new SipRegisterBean();
	
			// set sip account
			_sipAccount.setSipUserName(sipName);
			_sipAccount.setSipPwd(sipPsw);
			_sipAccount.setSipServer(vosServerAddress);
			_sipAccount.setSipPort(7788);
			_sipAccount.setSipDomain("richitec.com");
			_sipAccount.setSipRealm("richitec.com");
		
			SipUtils.registerSipAccount(_sipAccount,sipRegistStateListener);
		}
	}
}
