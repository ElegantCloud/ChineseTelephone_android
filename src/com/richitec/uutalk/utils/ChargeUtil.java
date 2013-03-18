package com.richitec.uutalk.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.richitec.commontoolkit.user.UserManager;
import com.richitec.uutalk.constant.ChargeType;

public class ChargeUtil {

	public static String getOrderNum(String type) {

		Date currTime = new Date();
		SimpleDateFormat sf = new SimpleDateFormat("_yyyyMMdd_HHmmss_",
				Locale.US);
		String returnStr = type + sf.format(currTime)
				+ UserManager.getInstance().getUser().getName() + "_";

		java.util.Random r = new java.util.Random();

		returnStr = returnStr + r.nextInt();

		return returnStr;
	}

}
