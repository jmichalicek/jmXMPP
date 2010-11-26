package jm.android.jmxmpp.service;
import java.util.List;
import jm.android.jmxmpp.JmRosterEntry;

interface IXmppConnectionService {
	boolean connect(in String host, int port);
	boolean login(in String username, in String password);
	List<JmRosterEntry> getRoster();
	void disconnect();
}
