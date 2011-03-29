package jm.android.jmxmpp;

import org.jivesoftware.smack.XMPPException;

import jm.android.jmxmpp.service.XmppConnectionService;
import jm.android.jmxmpp.service.XmppConnectionService.LocalBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class ConnectView extends Activity implements OnSharedPreferenceChangeListener{
	EditText usernameEntry;
	EditText passwordEntry;
	CheckBox saveUsernameCheckbox;
	CheckBox savePasswordCheckbox;
	
	Boolean startedService = false;
	//IXmppConnectionService mConnectionService = null;
	XmppConnectionService mConnectionService = null;
	
	String mHostname;
	int mPort;
	
	private static final String DEFAULT_SERVER = "talk.google.com";
	private static final String DEFAULT_PORT = "5222";
	
	private static final int DIALOG_CONNECTION_ERROR = 0;
	private static final int DIALOG_INVALID_LOGIN = 1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_view);
        
        usernameEntry = (EditText)findViewById(R.id.username_entry);
        passwordEntry = (EditText)findViewById(R.id.password_entry);
        saveUsernameCheckbox = (CheckBox)findViewById(R.id.save_username_checkbox);
        savePasswordCheckbox = (CheckBox)findViewById(R.id.save_password_checkbox);
        
        Button connectButton = (Button)findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				connectToServer();
			}
        });
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        populateFields();
        
        //Start service
      //use startService() first so that the lifetime of the service
		//is not tied to this activity
		startService(new Intent(this,jm.android.jmxmpp.service.XmppConnectionService.class));
		startedService = true;
		
		// Maybe should make this an explicit
		//bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
		//		mConnection,Context.BIND_AUTO_CREATE);
    }
	
	@Override
    protected void onStop(){
       super.onStop();
       savePrefs();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_CONNECTION_ERROR:
			return new AlertDialog.Builder(this)
            .setIcon(R.drawable.icon)
            .setTitle(R.string.connection_error)
            .setMessage(R.string.connection_error)
            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
            	}
            })
            .create();
		case DIALOG_INVALID_LOGIN:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setTitle(R.string.connection_error)
			.setMessage(R.string.invalid_login)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			}).create();
		}
		return null;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.connect_view_menu, menu);
	    return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
    	switch(item.getItemId()) {
    	case R.id.launch_contacts:
    	{
    		Intent i = new Intent("jm.android.jmxmpp.SHOW_CONTACT_LIST");
    		i.setClassName("jm.android.jmxmpp", "jm.android.jmxmpp.ContactListView");
			startActivity(i);
    	}
    		break;
    	case R.id.launch_options:
    	{
    		Intent i = new Intent(this,jm.android.jmxmpp.PreferencesEditor.class);
    		startActivity(i);
    	}
    		break;
    	}
    	return true;
    }
	
	//This is a handler which waits for us to connect to the XmppConnectionService
	//and then sets up the mConnectionService object so that calls can be made
	//through the IDL interface
	public ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			//TODO: Just get reference to the service from the IBinder
			// which requires changes in the service, too
			
			//mConnectionService = IXmppConnectionService.Stub.asInterface(service);
			LocalBinder binder = (LocalBinder)service;
			mConnectionService = binder.getService();
			
			if(mConnectionService != null && mConnectionService.isAuthenticated()) {
				Intent i = new Intent("jm.android.jmxmpp.SHOW_CONTACT_LIST");
				startActivity(i);
			}
			
			
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
	
	private void connectToServer() {
		startService(new Intent(this,jm.android.jmxmpp.service.XmppConnectionService.class));
		bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
				mConnection,Context.BIND_AUTO_CREATE);
		/*if(mConnectionService != null) {
			ConnectThread cThread = new ConnectThread();
			cThread.execute();
		}*/
		/*
		if(!startedService) {
			//use startService() first so that the lifetime of the service
			//is not tied to this activity
			startService(new Intent(this,jm.android.jmxmpp.service.XmppConnectionService.class));
			startedService = true;
			
			// Maybe should make this an explicit
			bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
					mConnection,Context.BIND_AUTO_CREATE);
		}*/
	}
	
	private void populateFields() {
		SharedPreferences preferences =
        	PreferenceManager.getDefaultSharedPreferences(this);
		
		String username = preferences.getString("username", "");
		String password = preferences.getString("password", "");
		Boolean saveUsername = preferences.getBoolean("save_username", false);
		Boolean savePassword = preferences.getBoolean("save_password", false);
		
		usernameEntry.setText(username);
		passwordEntry.setText(password);
		saveUsernameCheckbox.setChecked(saveUsername);
		savePasswordCheckbox.setChecked(savePassword);
		
		//Maybe not the most obvious place for this based on the name
		//of the method, but it keeps it with the rest of the preferences
		mHostname = preferences.getString("xmpp_server", DEFAULT_SERVER);
		mPort = Integer.parseInt(preferences.getString("server_port", DEFAULT_PORT));
	}
	
	private void savePrefs() {
		SharedPreferences preferences =
        	PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = preferences.edit();
		
		if(saveUsernameCheckbox.isChecked()) {
			editor.putBoolean("save_username", true);
			editor.putString("username", usernameEntry.getText().toString());
		} else {
			editor.putBoolean("save_username", false);
			editor.putString("username", "");
		}

		if(savePasswordCheckbox.isChecked()) {
			editor.putBoolean("save_password", true);
			editor.putString("password", passwordEntry.getText().toString());
		} else {
			editor.putBoolean("save_password", false);
			editor.putString("password", "");
		}
		
		editor.commit();
	}

	//Thread for connecting to the xmpp server
	private class ConnectThread extends AsyncTask<Void, Void, Boolean> {
		ProgressDialog progressDialog;
		Integer errorDialog = null;
		
		@Override
		protected void onPreExecute() {
			//TODO: Use strings from strings.xml here
			progressDialog = ProgressDialog.show(ConnectView.this,
					"Connecting", "Logging in to server", true, false);
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			try {
				if(mConnectionService.connect(mHostname, mPort)) {
					return mConnectionService.login(usernameEntry.getText().toString(),
							passwordEntry.getText().toString());
				}
			} catch (XMPPException e) {
				/* This seems like a bad way to handle this
				 * but XMPPException is a bit vague
				 */
				if(e.getCause().toString() != null &&
						e.getCause().toString()
						.equalsIgnoreCase("SASL authentication failed using mechanism PLAIN: ")) {
					errorDialog = DIALOG_INVALID_LOGIN;
				} else {
					errorDialog = DIALOG_CONNECTION_ERROR;
				}
			}
			
			return false;
		}
		
		@Override
		protected void onPostExecute(Boolean loggedIn) {
			progressDialog.dismiss();
			if(loggedIn) {
				Intent i = new Intent("jm.android.jmxmpp.SHOW_CONTACT_LIST");
				startActivity(i);
			} else {
				showDialog(errorDialog);
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if(key.equals("xmpp_server")) {
			mHostname = prefs.getString("xmpp_server", DEFAULT_SERVER);
		} else if (key.equals("server_port")) {
			mPort = Integer.valueOf(prefs.getString("server_port", DEFAULT_PORT));
		}
		
	}
}
