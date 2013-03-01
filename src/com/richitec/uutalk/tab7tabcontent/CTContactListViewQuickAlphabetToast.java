package com.richitec.uutalk.tab7tabcontent;

import android.content.Context;
import android.view.Gravity;

import com.richitec.commontoolkit.customcomponent.CTToast;
import com.richitec.uutalk.R;

//Chinese Telephone contact list quick alphabet toast
public class CTContactListViewQuickAlphabetToast extends CTToast {

	public CTContactListViewQuickAlphabetToast(Context context) {
		super(context, R.layout.contactlist_quickalphabet_toast_content_layout);

		// set text, duration and gravity
		setText("");
		setDuration(LENGTH_TRANSIENT);
		setGravity(Gravity.CENTER, 0, 0);
	}

}