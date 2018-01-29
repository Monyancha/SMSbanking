package com.khizhny.smsbanking;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import static com.khizhny.smsbanking.MyApplication.LOG;

public class PrefActivity extends PreferenceActivity {
    private static int prefs = R.xml.preferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getClass().getMethod("getFragmentManager");
            AddResourceApi11AndGreater();
        } catch (NoSuchMethodException e) { //Api < 11
            AddResourceApiLessThan11();
        }
    }


    @SuppressWarnings("deprecation")
    protected void AddResourceApiLessThan11() {
        addPreferencesFromResource(prefs);
    }

    @TargetApi(11)
    protected void AddResourceApi11AndGreater() {
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PF()).commit();
    }

    @TargetApi(11)
    public static class PF extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(PrefActivity.prefs); //outer class
            // private members seem to be visible for inner class, and
            // making it static made things so much easier
        }
    }

    @Override
    protected void onStop() {
        Log.d(LOG, "PrefActivity.onStop() started");
        // restarting app if laguage changed
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String new_lang;
        if (settings.contains("language")){
            new_lang = settings.getString("language","(System language)");
        }else{
            new_lang = "(System language)";
        }
        if (!MyApplication.language.equals(new_lang)) {
            MyApplication.restart(getBaseContext(),1);
        }
        Log.d(LOG, "PrefActivity.onStop() finnished");
        super.onStop();
    }

}
