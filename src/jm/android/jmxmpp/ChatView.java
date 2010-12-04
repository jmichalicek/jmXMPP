package jm.android.jmxmpp;

import jm.android.jmxmpp.service.IXmppConnectionService;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ChatView extends Activity {
	IXmppConnectionService mConnectionService = null;
	//Just single user chat for now
	JmRosterEntry mParticipant = null;
	EditText messageEntry = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_view);
		connectToXmppService();
		
		Intent i = getIntent();
		if(i != null) {
			unBundle(i);
		}
		
		messageEntry = (EditText)findViewById(R.id.send_message_input);
		Button sendButton = (Button)findViewById(R.id.send_message_button);
		sendButton.setOnClickListener(sendButtonClickListener);
		
	}
	
	private void unBundle(Intent intent) {
		Bundle data = intent.getExtras();
		if(data != null) {
			mParticipant = data.getParcelable("participant");
		}
	}
	
	private void connectToXmppService() {
		bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
				mConnection,Context.BIND_AUTO_CREATE);
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mConnectionService = IXmppConnectionService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mConnectionService = null;
		}
	};

	
	//Listeners
	OnClickListener sendButtonClickListener = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			SendMessageThread sendMessage = new SendMessageThread();
			sendMessage.execute();
		}
	};
	
	//Threads
	private class SendMessageThread extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			if(mConnectionService != null) {
				String message = messageEntry.getText().toString();
				try {
					mConnectionService.sendMessage(mParticipant.mUser, message);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}
	};
}
