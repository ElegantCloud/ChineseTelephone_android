package com.richitec.chinesetelephone.account;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.richitec.chinesetelephone.R;
import com.richitec.chinesetelephone.constant.Country;
import com.richitec.chinesetelephone.constant.SystemConstants;
import com.richitec.chinesetelephone.utils.CountryCodeManager;

public class CountryCodeListAdapter extends BaseAdapter {

	private CountryCodeManager ccm;
	private LayoutInflater inflater;
	public CountryCodeListAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		ccm = CountryCodeManager.getInstance();
	}
	
	@Override
	public int getCount() {
		return ccm.getCountryList().size();
	}

	@Override
	public Object getItem(int position) {
		return ccm.getCountryNameList()[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = inflater.inflate(R.layout.country_code_list_item, null);
			
			viewHolder.countryFlag = (ImageView) convertView.findViewById(R.id.country_flag);
			viewHolder.countryName = (TextView) convertView.findViewById(R.id.country_name);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		List<Map<String, Object>> list = ccm.getCountryList();
		Log.d(SystemConstants.TAG, "size: " + list.size());
		Map<String, Object> country = list.get(position);
		Log.d(SystemConstants.TAG, "pos: " + position + " country: " + country + " " + viewHolder.countryName);
		viewHolder.countryName.setText((String) country.get(Country.countryname.name()));
		viewHolder.countryFlag.setImageResource((Integer)country.get(Country.flag.name()));
		
		return convertView;
	}

	
	final class ViewHolder {
		ImageView countryFlag;
		TextView countryName;
		
	}
}
