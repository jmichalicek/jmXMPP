package jm.android.jmxmpp.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jm.android.jmxmpp.JmMessage;
import jm.android.jmxmpp.JmRosterEntry;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import org.jivesoftware.smack.packet.XMPPError;

public class XmppConnectionService extends Service implements ConnectionListener{

	private XMPPConnection mConnection;
	private ConnectionConfiguration mConnConfig;
	private Roster mRoster;	
	private NotificationManager mNotificationManager = null;
	
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
		return mBinder;
		//return null;
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
		
		//TODO: This message needs handled by strings.xml
		//TODO: Make intent disconnect from XMPP and stop service
		int icon = android.R.drawable.sym_action_chat;
		CharSequence tickerText = "XMPP Service Started";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		Intent i = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(
				XmppConnectionService.this, 0, i, 0);
		notification.setLatestEventInfo(getApplicationContext(),
				"jmXMPP Connected", "Click to disconnect", contentIntent);
		mNotificationManager.notify(NOTIFICATION_CONNECTION_STATUS,notification);
		
	}
	
	@Override
	public void onDestroy() {

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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connectionClosedOnError(Exception arg0) {
		//TODO: strings here from strings.xml
		//TODO: Intent to stop reconnection attempts?
		int icon = android.R.drawable.sym_action_chat;
		CharSequence tickerText = "jmXMPP Connection Lost";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		Intent i = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(
				XmppConnectionService.this, 0, i, 0);
		notification.setLatestEventInfo(getApplicationContext(),
				"jmXMPP Reconnecting", "Click to cancel", contentIntent);
		mNotificationManager.notify(NOTIFICATION_CONNECTION_STATUS,notification);
	}

	@Override
	public void reconnectingIn(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reconnectionFailed(Exception arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reconnectionSuccessful() {
		// TODO: This is done a couple times... move to own method?
		//TODO: This message needs handled by strings.xml
		//TODO: Make intent disconnect from XMPP and stop service
		int icon = android.R.drawable.sym_action_chat;
		CharSequence tickerText = "XMPP Service Started";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		Intent i = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(
				XmppConnectionService.this, 0, i, 0);
		notification.setLatestEventInfo(getApplicationContext(),
				"jmXMPP Connected", "Click to disconnect", contentIntent);
		mNotificationManager.notify(NOTIFICATION_CONNECTION_STATUS,notification);
		
	}
	
	// Listeners
	BroadcastReceiver sendMessageResultReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if(getResultCode() != Activity.RESULT_OK) {
				JmRosterEntry messageFrom = null;
				JmMessage message = null;
				Bundle data = intent.getExtras();
				if(data != null) {
					messageFrom = data.getParcelable("from");
					message = data.getParcelable("message");
				}
				
				queueMessage(messageFrom.getUser(),message);
				String messageText = message.getText();
				int icon = android.R.drawable.sym_action_chat;
				CharSequence tickerText = "New XMPP Message";
				long when = System.currentTimeMillis();
				Notification notification = new Notification(icon, tickerText, when);
				notification.flags = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE |
					Notification.FLAG_AUTO_CANCEL;
				
				CharSequence contentTitle = "New jmXMPP Message from " + messageFrom.getUser();
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
				int notificationId = messageFrom.getUser().hashCode();
				mNotificationManager.notify(notificationId, notification);
			}
		}
	};
	
	ChatManagerListener chatManagerListener = new ChatManagerListener() {
		@Override
		public void chatCreated(Chat arg0, boolean createdLocally) {
				//RosterEntry re = findRosterEntryByUser(arg0.getParticipant());
				//TODO: Handle chat from people NOT in the roster
				//if(re == null) {
				//	return;
				//}
				
				arg0.addMessageListener(messageListener);
				
				/*
				String status = null;

				if(re.getStatus() != null) {
					status = re.getStatus().toString();
				}
				
				Presence presence = mRoster.getPresence(re.getUser());
				String pStatus = null;
				String pMode = null;
				if(presence != null) {
					pStatus = presence.getStatus();
					pMode = presence.getMode() == null ? null : presence.getMode().toString();
				}
				
				JmRosterEntry creator = new JmRosterEntry(re.getName(),
						re.getStatus().toString(),
						status,null,pStatus,pMode);
				*/
				//TODO: This should ask if we want to chat with this person
				// not just pop up the chat activity.  For the moment the app
				// will be rude about it, though.
				/*Intent i = new Intent("jm.android.jmxmpp.START_CHAT");
				i.putExtra("thread_id", arg0.getThreadID());
				i.putExtra("participant", creator);
				
				startActivity(i);*/
				
				//Intent i = new Intent();
				//i.setClassName("jm.android.jmxmpp", "jm.android.jmxmpp.ChatView");
				//i.setAction("jm.android.jmxmpp.INCOMING_MESSAGE");
				//i.putExtra("from", creator);
				//i.putExtra("message",message.getBody());
				//sendBroadcast(i);
		}
	};

	MessageListener messageListener = new MessageListener() {
		@Override
		public void processMessage(Chat chat, Message message) {
			if(message.getType() == Message.Type.chat) {
				RosterEntry re = findRosterEntryByUser(chat.getParticipant());
				//TODO: Handle chat from people NOT in the roster
				if(re != null) {
					String status = null;
					if(re.getStatus() != null) {
						status = re.getStatus().toString();
					}

					Presence presence = mRoster.getPresence(re.getUser());
					String pStatus = null;
					String pMode = null;
					if(presence != null) {
						pStatus = presence.getStatus();
						pMode = presence.getMode() == null ? null : presence.getMode().toString();
					}

					JmRosterEntry participant = new JmRosterEntry(re.getName(),
							re.getUser(),
							status,null,pStatus,pMode);

					Intent i = new Intent();
					//i.setClassName("jm.android.jmxmpp", "jm.android.jmxmpp.ChatView");
					i.setAction("jm.android.jmxmpp.INCOMING_MESSAGE");
					// A bit redundant due to the JmMessage but for now
					// just make it work, it's not a huge problem
					i.putExtra("from", participant);
					//i.putExtra("message",message.getBody());
					String sender = (participant.getName() != null) ?
							participant.getName() : participant.getUser();
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
				/* For now just use one receiver for all roster updates
				 * later more specific receivers/broadcasts may need implemented.
				 */
				Intent i = new Intent("jm.android.jmxmpp.ROSTER_UPDATED");
				sendBroadcast(i);
			}		
	    }
		
		@Override
		public void entriesAdded(Collection<String> arg0) {
			
		}
	};
	
	public final IXmppConnectionService.Stub mBinder
		= new IXmppConnectionService.Stub() {
			
			@Override
			public void disconnect() throws RemoteException {
				//TODO: Stop the service?
				mConnection.disconnect();
			}
			
			/**
			 * Connects to xmpp server
			 */
			@Override
			public boolean connect(String host, int port) throws RemoteException {
				//return connectToServer(host, port);
				mConnConfig =
		    		new ConnectionConfiguration(host, port,"gmail.com");
				mConnection = new XMPPConnection(mConnConfig);
		        try {
		            mConnection.connect();
		            System.out.println("Connected!");
		        } catch (XMPPException ex) {
		        	System.err.println(ex.getMessage());
		        	RemoteException e = new RemoteException();
		        	e.initCause(ex);
		        	throw e;
		        }

		        return mConnection.isConnected();
			}

			/**
			 * Logs into xmpp server and gets initial roster for service
			 */
			@Override
			public boolean login(String username, String password)
					throws RemoteException {
				
				try {
		            mConnection.login(username, password);
		            System.out.println("Logged in!");
		        } catch (XMPPException ex) {
		        	System.err.println(ex.getMessage());
		        	RemoteException e = new RemoteException();
		        	e.initCause(ex);
		        	throw e;
		        }
		        
		        mRoster = mConnection.getRoster();
		        mRoster.addRosterListener(rosterListener);
		        
		        mConnection.getChatManager().addChatListener(chatManagerListener);
		        
		        return true;
			}
			
			/**
			 * Looks at service's roster and returns List<JmRosterEntry>
			 */
			@Override
			public List<jm.android.jmxmpp.JmRosterEntry> getRoster() throws RemoteException {
				//Roster roster = mConnection.getRoster();
				if(mRoster == null){
					return null;
				}
				Collection<RosterEntry> rosterEntries = mRoster.getEntries();
				Iterator<RosterEntry> i = rosterEntries.iterator();

				List<jm.android.jmxmpp.JmRosterEntry> entryList =
					new ArrayList<jm.android.jmxmpp.JmRosterEntry>();

				while(i.hasNext()) {
					RosterEntry currentEntry = i.next();
					String status = null;

					if(currentEntry.getStatus() != null) {
						status = currentEntry.getStatus().toString();
					}
					
					Presence presence = mRoster.getPresence(currentEntry.getUser());
					String pStatus = null;
					String pMode = null;
					if(presence != null) {
						pStatus = presence.getStatus();
						pMode = presence.getMode() == null ? null : presence.getMode().toString();
					}
					jm.android.jmxmpp.JmRosterEntry temp =
						new JmRosterEntry(
								currentEntry.getName(),currentEntry.getUser(),
								status, null,pStatus,pMode);
					entryList.add(temp);
				}
				return entryList;
			}

			@Override
			public void sendMessage(String to, String message) throws RemoteException {
				Chat chat = mConnection.getChatManager().createChat(to,
						null);
				
				try {
					chat.sendMessage(message);
				} catch (XMPPException ex) {
					ex.printStackTrace();
					System.err.println(ex.getMessage());
		        	RemoteException e = new RemoteException();
		        	e.initCause(ex);
		        	throw e;
				}
			}

			@Override
			public void clearQueuedMessages(String from) throws RemoteException {
				mQueuedMessages.remove(from);
				
			}

			@Override
			public List<JmMessage> getQueuedMessages(String from)
					throws RemoteException {
				if(mQueuedMessages.containsKey(from)) {
					return mQueuedMessages.get(from);
				}
				return null;
			}

			@Override
			public void addMessagesToQueue(String from, JmMessage[] messages)
					throws RemoteException {
				List<JmMessage> messageList = new ArrayList<JmMessage>();
				
				if(mQueuedMessages.containsKey(from)) {
					messageList = mQueuedMessages.get(from);
				}
				
				for(JmMessage message: messages) {
					messageList.add(message);
				}
				
				mQueuedMessages.put(from, messageList);
			}

			@Override
			public boolean isConnected() throws RemoteException {
				return mConnection.isConnected();
			}

	};
}
