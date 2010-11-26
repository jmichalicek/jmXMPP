package jm.android.jmxmpp.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemStatus;

public class XmppConnectionService extends Service {

	private XMPPConnection mConnection;
	private ConnectionConfiguration mConnConfig;
	
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
		Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onDestroy() {
		Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();

	}
	
	private boolean connectToServer(String host, int port) {
		Toast.makeText(this, "Called connect() method", Toast.LENGTH_LONG).show();
		ConnectionConfiguration connConfig =
    		new ConnectionConfiguration(host, port,"gmail.com");
    	
        mConnection = new XMPPConnection(connConfig);
        boolean connected = false;
        try {
            mConnection.connect();
            System.out.println("Connected!");
            connected = true;
        } catch (XMPPException ex) {
        	System.err.println(ex.getMessage());
        }
        
        return connected;
	}
	
	private void setStatus() {
		
	}
	
	private void sendMessage() {
		
	}
	
	public final IXmppConnectionService.Stub mBinder
		= new IXmppConnectionService.Stub() {
			
			@Override
			public void disconnect() throws RemoteException {
				
			}
			
			@Override
			public boolean connect(String host, int port) throws RemoteException {
				//return connectToServer(host, port);
				mConnConfig =
		    		new ConnectionConfiguration(host, port,"gmail.com");
				mConnection = new XMPPConnection(mConnConfig);
		        boolean connected = false;
		        try {
		            mConnection.connect();
		            System.out.println("Connected!");
		            connected = true;
		        } catch (XMPPException ex) {
		        	System.err.println(ex.getMessage());
		        	RemoteException e = new RemoteException();
		        	e.initCause(ex);
		        	throw e;
		        }
		        
		        return connected;
			}

			@Override
			public boolean login(String username, String password)
					throws RemoteException {
				
				try {
		            mConnection.login(username, password);
		            System.out.println("Logged in!");

		            return true;
		        } catch (XMPPException ex) {
		        	System.err.println(ex.getMessage());
		        	RemoteException e = new RemoteException();
		        	e.initCause(ex);
		        	throw e;
		        }
			}

			@Override
			public List<jm.android.jmxmpp.JmRosterEntry> getRoster() throws RemoteException {
				Roster roster = mConnection.getRoster();
				Collection<RosterEntry> rosterEntries = roster.getEntries();
				Iterator<RosterEntry> i = rosterEntries.iterator();

				List<jm.android.jmxmpp.JmRosterEntry> entryList =
					new ArrayList<jm.android.jmxmpp.JmRosterEntry>();

				while(i.hasNext()) {
					RosterEntry currentEntry = i.next();
					String status = null;
					
					if(currentEntry.getStatus() != null) {
						status = currentEntry.getStatus().toString();
					}
					
					
					jm.android.jmxmpp.JmRosterEntry temp =
						new jm.android.jmxmpp.JmRosterEntry(
								currentEntry.getName(),currentEntry.getUser(),
								status, null);
					entryList.add(temp);
				}
				return entryList;
			}
	};
}
