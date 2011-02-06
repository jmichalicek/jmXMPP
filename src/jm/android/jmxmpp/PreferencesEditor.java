package jm.android.jmxmpp;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferencesEditor extends PreferenceActivity {
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the XML preferences file
        addPreferencesFromResource(R.xml.preferences);
    }
}
