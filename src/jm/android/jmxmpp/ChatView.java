package jm.android.jmxmpp;
/**
 * Currently only handles single user chat, no file transfers, etc.
 * May need some reworking for MUC
 */

import java.util.List;

import jm.android.jmxmpp.service.IXmppConnectionService;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ChatView extends Activity {
	IXmppConnectionService mConnectionService = null;
	//Just single user chat for now
	JmRosterEntry mParticipant = null;
	EditText messageEntry = null;
	TextView chatMessages = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_view);
		connectToXmppService();
		
		Intent i = getIntent();
		if(i != null) {
			unBundle(i);
		}
	
		chatMessages = (TextView)findViewById(R.id.chat_messages);
		messageEntry = (EditText)findViewById(R.id.send_message_input);
		Button sendButton = (Button)findViewById(R.id.send_message_button);
		sendButton.setOnClickListener(sendButtonClickListener);
		
		registerReceiver(messageBroadcastListener,
				new IntentFilter("jm.android.jmxmpp.INCOMING_MESSAGE"));
		
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		this.mParticipant = savedInstanceState.getParcelable("participant");
		this.chatMessages.setText(savedInstanceState.getString("messages"));
		
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable("participant", mParticipant);
		outState.putString("messages", chatMessages.getText().toString());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//for now just save the whole current message log as a single
		//string.  It may turn out to be better to save separate strings per message
		try {
			mConnectionService.addMessagesToQueue(mParticipant.getUser(), new String[] {chatMessages.getText().toString()});
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

			if(mConnectionService != null) {
				try {
					List<String> queuedMessages = 
						mConnectionService.getQueuedMessages(mParticipant.getUser());

					if(queuedMessages != null) {
						for(String currentMessage:queuedMessages) {
							String sender = (mParticipant.getName() != null) ?
									mParticipant.getName() : mParticipant.getUser();

							chatMessages.append(sender + ": ");
							chatMessages.append(currentMessage);
							chatMessages.append("\n\n");
						}
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					mConnectionService.clearQueuedMessages(mParticipant.getUser());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
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
	
	BroadcastReceiver messageBroadcastListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			Bundle data = intent.getExtras();
			if(data != null) {
				JmRosterEntry from = data.getParcelable("from");
				if(!from.getUser().equals(mParticipant.getUser())) {
					return;
				}
				
				String message = data.getString("message");
				String sender = (mParticipant.getName() != null) ? mParticipant.getName()
						: mParticipant.getUser();
				
				chatMessages.setTextColor(Color.GREEN);
				chatMessages.append(sender + ": ");
				chatMessages.setTextColor(Color.WHITE);
				chatMessages.append(message);
				chatMessages.append("\n\n");
				
				setResultCode(Activity.RESULT_OK);
			}
		}
		
	};

	//Threads
	private class SendMessageThread extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			if(mConnectionService != null) {
				String message = messageEntry.getText().toString();
				try {
					mConnectionService.sendMessage(mParticipant.getUser(), message);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void unused)  {
			chatMessages.append("Me: ");
			chatMessages.append(messageEntry.getText().toString());
			chatMessages.append("\n\n");
			
			messageEntry.setText(null);
		}
	};
}
