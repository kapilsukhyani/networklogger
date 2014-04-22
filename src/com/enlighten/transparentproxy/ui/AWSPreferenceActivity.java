package com.enlighten.transparentproxy.ui;



import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.enlighten.transparentproxy.R;
import com.enlighten.transparentproxy.app.AppLog;
import com.enlighten.transparentproxy.constants.Constants;

public class AWSPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preference);
		
		setPreferenceChangeListener(Constants.PREF_DIRECTORY_LISTING, onPreferenceChange);
		setPreferenceChangeListener(Constants.PREF_DIRECTORY, onPreferenceChange);
	}
	
	private void setPreferenceChangeListener(String key, Preference.OnPreferenceChangeListener listener){
		Preference preference = findPreference(key);
		
		preference.setOnPreferenceChangeListener(onPreferenceChange);
	}
	
	private Preference.OnPreferenceChangeListener onPreferenceChange = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			AppLog.logInfo("Preference Change: " + preference.getKey() + ", " + newValue.toString());
			
			return true;
		}
	};
}
