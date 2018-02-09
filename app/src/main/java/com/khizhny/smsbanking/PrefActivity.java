package com.khizhny.smsbanking;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import static com.khizhny.smsbanking.MyApplication.KEY_EXTRA_1_NAME;
import static com.khizhny.smsbanking.MyApplication.KEY_EXTRA_2_NAME;
import static com.khizhny.smsbanking.MyApplication.KEY_EXTRA_3_NAME;
import static com.khizhny.smsbanking.MyApplication.KEY_EXTRA_4_NAME;
import static com.khizhny.smsbanking.MyApplication.KEY_LANGUAGE;

public class PrefActivity extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
				try {
						getClass().getMethod("getFragmentManager");
						getFragmentManager().beginTransaction().replace(android.R.id.content,
										new PreferecnceFragment()).commit();
				} catch (NoSuchMethodException e) {
						e.printStackTrace();
				}
    }

		@Override
		protected void onStop() {
				super.onStop();
				restartApplication();
		}

		private void restartApplication(){
				// restarting app if laguage changed
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				String new_lang;
				if (settings.contains(KEY_LANGUAGE)){
						new_lang = settings.getString(KEY_LANGUAGE,getString(R.string.system_language));
				}else{
						new_lang = getString(R.string.system_language);
				}
				if (!MyApplication.language.equals(new_lang)) {
						MyApplication.restart(this);
				}
		}

		public static class PreferecnceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
				@Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
						EditTextPreference p1 = (EditTextPreference) findPreference(KEY_EXTRA_1_NAME);
						EditTextPreference p2 = (EditTextPreference) findPreference(KEY_EXTRA_2_NAME);
						EditTextPreference p3 = (EditTextPreference) findPreference(KEY_EXTRA_3_NAME);
						EditTextPreference p4 = (EditTextPreference) findPreference(KEY_EXTRA_4_NAME);

						p1.setOnPreferenceChangeListener(this);
						p2.setOnPreferenceChangeListener(this);
						p3.setOnPreferenceChangeListener(this);
						p4.setOnPreferenceChangeListener(this);

						SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
						p1.setSummary(settings.getString(KEY_EXTRA_1_NAME,""));
						p2.setSummary(settings.getString(KEY_EXTRA_2_NAME,""));
						p3.setSummary(settings.getString(KEY_EXTRA_3_NAME,""));
						p4.setSummary(settings.getString(KEY_EXTRA_4_NAME,""));

						ListPreference lang = (ListPreference) findPreference(KEY_LANGUAGE);
						lang.setSummary(settings.getString(KEY_LANGUAGE, getString(R.string.system_language)));
						lang.setOnPreferenceChangeListener(this);
				}
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
						preference.setSummary(newValue.toString());
						return true;
				}
    }
}
