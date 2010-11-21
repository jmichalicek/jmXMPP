package jm.android.jmxmpp;

import android.app.ListActivity;
import android.os.Bundle;

public class ContactListView extends ListActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}