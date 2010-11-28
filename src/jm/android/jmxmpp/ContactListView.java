package jm.android.jmxmpp;

import java.util.Iterator;
import java.util.List;

import jm.android.jmxmpp.service.IXmppConnectionService;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ContactListView extends ListActivity {
	IXmppConnectionService mConnectionService = null;
	ArrayAdapter<String> mRosterArrayAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.roster_view);
		mRosterArrayAdapter = new ArrayAdapter<String>(this,R.layout.roster_row);
		connectToXmppService();
	}

	public ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mConnectionService = IXmppConnectionService.Stub.asInterface(service);
			if(mConnectionService != null) {
				UpdateRosterThread updater = new UpdateRosterThread();
				updater.execute();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mConnectionService = null;
		}
	};

	public void connectToXmppService() {
		bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
				mConnection,Context.BIND_AUTO_CREATE);
	}
	
	private class UpdateRosterThread extends AsyncTask<Void,Void,Void> {
		List<JmRosterEntry> rosterList = null;
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				rosterList = mConnectionService.getRoster();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void a) {
			ListView rosterListView = (ListView)findViewById(android.R.id.list);

			//new stuff
			//ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,R.layout.roster_row);
			Iterator<JmRosterEntry> i = rosterList.iterator();
			while(i.hasNext()) {
				JmRosterEntry current = i.next();
				String rosterString = new String();
				
				if(current.mName != null) {
					rosterString = current.mName;
				} else {
					rosterString = current.mUser;
				}
				
				if(current.mPresence != null) {
					rosterString += "\n" + current.mPresence;
				}
				//TODO: get presence of user
				
				mRosterArrayAdapter.add(rosterString);
			}
			rosterListView.setAdapter(mRosterArrayAdapter);
		}
		
	}
}