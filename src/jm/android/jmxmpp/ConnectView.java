package jm.android.jmxmpp;

import jm.android.jmxmpp.service.IXmppConnectionService;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ConnectView extends Activity {
	SharedPreferences preferences;
	EditText usernameEntry;
	EditText passwordEntry;
	
	Boolean startedService = false;
	IXmppConnectionService mConnectionService = null;
	
	private static final String DEFAULT_SERVER = "talk.google.com";
	private static final int DEFAULT_PORT = 5222;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_view);
        
        usernameEntry = (EditText)findViewById(R.id.username_entry);
        passwordEntry = (EditText)findViewById(R.id.password_entry);
        
        Button connectButton = (Button)findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				connectToServer();
			}
        });
        
        preferences = PreferenceManager
        	.getDefaultSharedPreferences(this);

    }
	
	//This is a handler which waits for us to connect to the XmppConnectionService
	//and then sets up the mConnectionService object so that calls can be made
	//through the IDL interface
	public ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mConnectionService = IXmppConnectionService.Stub.asInterface(service);
			if(mConnectionService != null) {
				ConnectThread cThread = new ConnectThread();
				cThread.execute();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mConnectionService = null;
		}
	};
	
	//TODO: connectToServer should maybe be in a thread
	// so that a dialog can be popped up during the connection time
	// since it can take several seconds.
	private void connectToServer() {
		if(!startedService) {
			startService(new Intent(this,jm.android.jmxmpp.service.XmppConnectionService.class));
			startedService = true;
			
			// Maybe should make this an explicit
			bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
					mConnection,Context.BIND_AUTO_CREATE);
		} else {
			stopService(new Intent(this,jm.android.jmxmpp.service.XmppConnectionService.class));
			startedService = false;
		}
		
	}
	
	private void populateFields() {
		String username = preferences.getString("username", "");
		String password = preferences.getString("password", "");
		
		usernameEntry.setText(username);
		passwordEntry.setText(password);
	}
	
	private void savePrefs() {
		preferences.edit().putString("username", usernameEntry.getText().toString());
		preferences.edit().putString("password", passwordEntry.getText().toString());
	}

	//Thread for connecting to the xmpp server
	private class ConnectThread extends AsyncTask<Void, Void, Boolean> {
		ProgressDialog progressDialog;
		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(ConnectView.this,
					"Connecting", "Logging in to server", true, false);
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			Boolean success = false;
			try {
				success = mConnectionService.connect(DEFAULT_SERVER, DEFAULT_PORT);
				if(success) {
					success = mConnectionService.login(usernameEntry.getText().toString(),
						passwordEntry.getText().toString());
				}
			} catch (RemoteException e) {
				// TODO properly handle exception
				e.printStackTrace();
			}
			
			return success;
		}
		
		@Override
		protected void onPostExecute(Boolean loggedIn) {
			progressDialog.dismiss();
			Intent i = new Intent("jm.android.jmxmpp.SHOW_CONTACT_LIST");
			startActivity(i);
		}
	}
}
