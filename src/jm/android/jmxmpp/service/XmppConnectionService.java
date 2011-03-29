package jm.android.jmxmpp.service;
//TODO: Update this so that UI activities can just get references to the service
// rather than using AIDL
//TODO: Above seems to be done and working, but now need to update methods
// to just return asmack objects such as Roster rather than the simple wrappers
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jm.android.jmxmpp.JmMessage;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

public class XmppConnectionService extends Service implements ConnectionListener{

	private XMPPConnection mConnection;
	private ConnectionConfiguration mConnConfig;
	private Roster mRoster;	
	private NotificationManager mNotificationManager = null;
	int mClientCount = 0;
	
	// Test return reference to the service directly
	private final IBinder myBinder = new LocalBinder();
	
	public class LocalBinder extends Binder {
        public XmppConnectionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return XmppConnectionService.this;
        }
	}
	
	public boolean login(String username, String password) throws XMPPException {

		mConnection.login(username, password);
		System.out.println("Logged in!");

		mRoster = mConnection.getRoster();
		mRoster.addRosterListener(rosterListener);

		mConnection.getChatManager().addChatListener(chatManagerListener);

		return true;
	}
	
	public boolean connect(String host, int port) throws XMPPException {
		//return connectToServer(host, port);
		mConnConfig =
			new ConnectionConfiguration(host, port,"gmail.com");
		mConnection = new XMPPConnection(mConnConfig);
		mConnection.connect();
		System.out.println("Connected!");

		return mConnection.isConnected();
	}
	
	public boolean isConnected() {
		boolean connected = false;
		if(mConnection != null) {
			connected = mConnection.isConnected();
		}
		return connected;
	}
	
	public boolean isAuthenticated() {
		boolean authenticated = false;
		if(mConnection != null) {
			authenticated = mConnection.isAuthenticated();
		}
		return authenticated;
	}
	
	public void sendMessage(String to, String message) throws XMPPException {
		//Why don't I have map of Chat objects?  Seems like I tried once and it
		//would not work for some reason.  Try again and comment the issue
		//if there is one
		
		Chat chat = mConnection.getChatManager().createChat(to,
				null);
		chat.sendMessage(message);
	}
	
	public void clearQueuedMessages(String from) {
		mQueuedMessages.remove(from);
		
	}
	
	public List<JmMessage> getQueuedMessages(String from) {
		if(mQueuedMessages.containsKey(from)) {
			return mQueuedMessages.get(from);
		}
		return null;
	}
	
	public void addMessagesToQueue(String from, JmMessage[] messages) {
		List<JmMessage> messageList = new ArrayList<JmMessage>();

		if(mQueuedMessages.containsKey(from)) {
			messageList = mQueuedMessages.get(from);
		}

		for(JmMessage message: messages) {
			messageList.add(message);
		}

		mQueuedMessages.put(from, messageList);
	}
	
	//public List<jm.android.jmxmpp.JmRosterEntry> getRoster() throws RemoteException {
	public Roster getRoster() {
		return mRoster;
	}

	//end test

	
	//Stores messages when there was no proper BroadcastReceiver
	//so that a ChatView can display them later
	//this may need revisited to deal properly with MUC
	//uses who our conversation is with as the key and then uses JmMessage
	//because the actual message being stored could be from ourselves TO the person
	//we are chatting with.
	private HashMap<String,List<JmMessage>> mQueuedMessages = new HashMap<String,List<JmMessage>>();
	
	private static final int NOTIFICATION_CONNECTION_STATUS = 1;

	@Override
	public IBinder onBind(Intent arg0) {
		displayServiceNotification();
		++mClientCount;
		return myBinder;
	}
	
	@Override
	public boolean onUnbind(Intent arg0) {
		--mClientCount;
		return true;
	}

	@Override
	public void onCreate() {
		
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		//onStart deprecated sometime after android 1.6
		//and replaced with onStartCommand()
		
		String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager)getSystemService(ns);
		displayServiceNotification();
		registerReceiver(stopConnectionServiceReceiver,
				new IntentFilter("jm.android.jmxmpp.STOP_CONNECTION_SERVICE"));
	}
	
	@Override
	public void onDestroy() {
		mNotificationManager.cancel(NOTIFICATION_CONNECTION_STATUS);
	}
	
	//TODO: Need to call this if service gets restarted, too
	//but that doesn't seem to be happening
	private void  displayServiceNotification() {
		//TODO: This message needs handled by strings.xml
		int icon = android.R.drawable.sym_action_chat;
		CharSequence tickerText = "XMPP Service Started";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= 
			Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		Intent i = new Intent("jm.android.jmxmpp.STOP_CONNECTION_SERVICE");
		PendingIntent contentIntent = PendingIntent.getBroadcast(
				XmppConnectionService.this, 0, i, 0);
		notification.setLatestEventInfo(getApplicationContext(),
				"jmXMPP Connected", "Click to disconnect", contentIntent);
		mNotificationManager.notify(NOTIFICATION_CONNECTION_STATUS,notification);
	}
	
	private RosterEntry findRosterEntryByUser(String user) {
		String temp = user.split("/", 2)[0];
		Iterator<RosterEntry> i = mRoster.getEntries().iterator();
		while(i.hasNext()) {
			RosterEntry current = i.next();
			if(current.getUser().equals(temp)) {
				return current;
			}
		}
		return null;
	}
	
	private void queueMessage(String user, JmMessage message) {
		if(!mQueuedMessages.containsKey(user)) {
			mQueuedMessages.put(user, new ArrayList<JmMessage>());
		}
		
		mQueuedMessages.get(user).add(message);
	}
	
	//Listener interfaces
	@Override
	public void connectionClosed() {
		// TODO Determine if something needs done here
		
	}

	@Override
	public void connectionClosedOnError(Exception arg0) {
		//TODO: strings here from strings.xml
		//TODO: Intent to stop reconnection attempts does not exist
		//or do anything yet
		int icon = android.R.drawable.sym_action_chat;
		CharSequence tickerText = "jmXMPP Connection Lost";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		Intent i = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(
				XmppConnectionService.this, 0, i, 0);
		notification.setLatestEventInfo(getApplicationContext(),
				"jmXMPP Reconnecting", "Click to cancel", contentIntent);
		mNotificationManager.notify(NOTIFICATION_CONNECTION_STATUS,notification);
	}

	@Override
	public void reconnectingIn(int arg0) {
		// TODO Determine if something needs done here
		
	}

	@Override
	public void reconnectionFailed(Exception arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reconnectionSuccessful() {
		// TODO: This is done a couple times... move to own method?
		//TODO: This message needs handled by strings.xml
		int icon = android.R.drawable.sym_action_chat;
		CharSequence tickerText = "XMPP Service Started";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		Intent i = new Intent("jm.android.jmxmpp.STOP_CONNECTION_SERVICE");
		PendingIntent contentIntent = PendingIntent.getBroadcast(
				XmppConnectionService.this, 0, i, 0);
		notification.setLatestEventInfo(getApplicationContext(),
				"jmXMPP Connected", "Click to disconnect", contentIntent);
		mNotificationManager.notify(NOTIFICATION_CONNECTION_STATUS,notification);
		
	}
	
	// Listeners
	BroadcastReceiver stopConnectionServiceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if(mConnection != null) {
				mConnection.disconnect();
			}
			stopSelf();
		}		
	};
	
	BroadcastReceiver sendMessageResultReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if(getResultCode() != Activity.RESULT_OK) {
				//JmRosterEntry messageFrom = null;
				String messageFrom = null;
				JmMessage message = null;
				Bundle data = intent.getExtras();
				if(data != null) {
					messageFrom = data.getString("from");
					message = data.getParcelable("message");
				}
				
				queueMessage(messageFrom,message);
				String messageText = message.getText();
				int icon = android.R.drawable.sym_action_chat;
				CharSequence tickerText = "New XMPP Message";
				long when = System.currentTimeMillis();
				Notification notification = new Notification(icon, tickerText, when);
				notification.flags = notification.defaults;
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				
				CharSequence contentTitle = "New jmXMPP Message from " + messageFrom;
				int messageLength = 15;
				if(messageText.length() <= 15) {
					messageLength = messageText.length();
				}
				CharSequence contentText = messageText.subSequence(0, messageLength);
				
				Intent i = new Intent("jm.android.jmxmpp.START_CHAT");
				i.setClassName("jm.android.jmxmpp", "jm.android.jmxmpp.ChatView");
				i.putExtra("participant", messageFrom);

				Context context = getApplicationContext();
				PendingIntent contentIntent = PendingIntent.getActivity(
						XmppConnectionService.this, 0, i, 0);
				notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

				//Use the hashcode of the message sender's user id
				//so that there's a unique id per message source.  This way
				//there will be a new notification per new source, but if that source
				//sends multiple messages, the notification for them will just update
				//with the latest message
				//This needs to be an int due to NotificationManager.notify() so the
				//hashcode is used rather than the username.
				//int notificationId = messageFrom.getUser().hashCode();
				int notificationId = messageFrom.hashCode();
				mNotificationManager.notify(notificationId, notification);
			}
		}
	};
	
	ChatManagerListener chatManagerListener = new ChatManagerListener() {
		@Override
		public void chatCreated(Chat arg0, boolean createdLocally) {
				//TODO: Handle chat from people NOT in the roster
				arg0.addMessageListener(messageListener);
		}
	};

	MessageListener messageListener = new MessageListener() {
		@Override
		public void processMessage(Chat chat, Message message) {
			if(message.getType() == Message.Type.chat) {
				RosterEntry re = findRosterEntryByUser(chat.getParticipant());
				//TODO: Handle chat from people NOT in the roster
				if(re != null) {					
					Intent i = new Intent();
					i.setAction("jm.android.jmxmpp.INCOMING_MESSAGE");
					// A bit redundant due to the JmMessage but for now
					// just make it work, it's not a huge problem
					i.putExtra("from", re.getUser());
					String sender = (re.getName() != null) ?
							re.getName() : re.getUser();
					i.putExtra("message", 
							new JmMessage(sender,message.getBody()));
					// Send broadcast and expect result back.  If no result then
					// then an appropriate activity is not alive and we should show a notification
					sendOrderedBroadcast(i,null,sendMessageResultReceiver,null,
							Activity.RESULT_CANCELED,null,null);
				} else {
					//What to do if they're not on the roster?
				}
			}
		}
	};

	RosterListener rosterListener = new RosterListener() {
		/*
		 * The way these are implemented could get slow if someone has a huge
		 * roster, but for now just send a Broadcast Intent saying there's been
		 * a change and views that need it will call the service's getRoster()
		 * and update as required.  Updating only the updated entry
		 * will require a more complicated object and service structure
		 * and likely more duplicated smack objects that are parcelable
		 */
		@Override
	    public void entriesDeleted(Collection<String> addresses) {
			Intent i = new Intent("jm.android.jmxmpp.ROSTER_UPDATED");
			sendBroadcast(i);
		}
		
		@Override
	    public void entriesUpdated(Collection<String> addresses) {
			Intent i = new Intent("jm.android.jmxmpp.ROSTER_UPDATED");
			sendBroadcast(i);
		}
		
		@Override
	    public void presenceChanged(Presence presence) {
			// maybe some day null getFrom() will be useful?
			// or maybe it's optional but is always included in real world use
			if(presence.getFrom() != null) {
				Intent i = new Intent("jm.android.jmxmpp.ROSTER_UPDATED");
				sendBroadcast(i);
			}		
	    }
		
		@Override
		public void entriesAdded(Collection<String> arg0) {
			Intent i = new Intent("jm.android.jmxmpp.ROSTER_UPDATED");
			sendBroadcast(i);
		}
	};
}
