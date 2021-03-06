package jm.android.jmxmpp;
/**
 * Currently only handles single user chat, no file transfers, etc.
 * May need some reworking for MUC
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPException;

//import jm.android.jmxmpp.service.IXmppConnectionService;
import jm.android.jmxmpp.service.XmppConnectionService;
import jm.android.jmxmpp.service.XmppConnectionService.LocalBinder;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ChatView extends Activity {
	//IXmppConnectionService mConnectionService = null;
	XmppConnectionService mConnectionService = null;
	//Just single user chat for now
	//JmRosterEntry mParticipant = null;
	RosterEntry mParticipant = null;
	EditText messageEntry = null;
	ListView chatMessages = null;
	
	SimpleAdapter mChatAdapter = null;
	List<HashMap<String,String>> mChatList = new ArrayList<HashMap<String,String>>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_view);
		connectToXmppService();
		
		/*
		Intent i = getIntent();
		if(i != null) {
			unBundle(i);
		}*/
	
		chatMessages = (ListView)findViewById(R.id.chat_messages);
		messageEntry = (EditText)findViewById(R.id.send_message_input);
		Button sendButton = (Button)findViewById(R.id.send_message_button);
		sendButton.setOnClickListener(sendButtonClickListener);
		
		//new
		mChatAdapter = new SimpleAdapter(this,mChatList,
				R.layout.chat_row,
				new String[] {"from","message"},
				new int[] {R.id.chat_message_from,R.id.chat_message}
		);
		//end new
		
		chatMessages.setAdapter(mChatAdapter);
		
		registerReceiver(messageBroadcastListener,
				new IntentFilter("jm.android.jmxmpp.INCOMING_MESSAGE"));
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		//this.mParticipant = savedInstanceState.getParcelable("participant");
		String temp = savedInstanceState.getString("participant");
		this.mParticipant = mConnectionService.getRoster().getEntry(temp);
		//this.chatMessages.setText(savedInstanceState.getString("messages"));
		
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		//outState.putParcelable("participant", mParticipant);
		outState.putString("participant", mParticipant.getUser());
		//outState.putString("messages", chatMessages.getText().toString());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onStop() {
		super.onStop();

		JmMessage[] messages = new JmMessage[mChatList.size()];
		Iterator<HashMap<String,String>> i = mChatList.iterator();
		int x = 0;
		while(i.hasNext()) {
			HashMap<String,String> m = i.next();
			//Strip the ": " off of the end of from
			//or when the user re-opens the chat they see <sender>: : <message
			//instead of the desired <sender>: <message>
			//This could also be resolved with a parallel array of JmMessage
			//to iterate over for things like this
			String from = m.get("from");
			from = from.substring(0, from.lastIndexOf(": "));
			messages[x] = new JmMessage(from,m.get("message"));
			x++;
		}
		
		try {
			mConnectionService.addMessagesToQueue(mParticipant.getUser(), messages);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void unBundle(Intent intent) {
		Bundle data = intent.getExtras();
		if(data != null) {
			//mParticipant temp = data.getParcelable("participant");
			String temp = data.getString("participant");
			mParticipant = mConnectionService.getRoster().getEntry(temp);
		}
	}

	private void connectToXmppService() {
		bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
				mConnection,Context.BIND_AUTO_CREATE);
	}
	
	private void addMessageToView(JmMessage message) {
		HashMap<String,String> incomingMessage = new HashMap<String,String>();
		incomingMessage.put("from", message.getFrom() + ": ");
		incomingMessage.put("message",message.getText());
		mChatList.add(incomingMessage); 
		mChatAdapter.notifyDataSetChanged();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder)service;
			mConnectionService = binder.getService();

			if(mConnectionService != null) {
				Intent i = getIntent();
				if(i != null) {
					unBundle(i);
				}
				List<JmMessage> queuedMessages = 
					mConnectionService.getQueuedMessages(mParticipant.getUser());

				if(queuedMessages != null) {
					for(JmMessage currentMessage:queuedMessages) {
						addMessageToView(currentMessage);
					}
				}
				
				mConnectionService.clearQueuedMessages(mParticipant.getUser());
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
				String from = data.getString("from");
				if(!from.equals(mParticipant.getUser())) {
					return;
				}

				JmMessage message = data.getParcelable("message");
				addMessageToView(message);
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

				} catch (IllegalStateException e) {
					// TODO Add proper error handling
					e.printStackTrace();
				} catch (XMPPException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void unused)  {
			JmMessage message = new JmMessage("Me",messageEntry.getText().toString());
			addMessageToView(message);
			messageEntry.setText(null);
		}
	};
}
