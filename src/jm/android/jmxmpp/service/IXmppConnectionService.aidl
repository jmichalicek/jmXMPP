package jm.android.jmxmpp.service;
import java.util.List;
import jm.android.jmxmpp.JmRosterEntry;
import jm.android.jmxmpp.JmMessage;

interface IXmppConnectionService {
	void addMessagesToQueue(in String from, in JmMessage[] messages);
	void clearQueuedMessages(in String from);
	boolean connect(in String host, int port);
	void disconnect();
	List<JmMessage> getQueuedMessages(in String from);
	List<JmRosterEntry> getRoster();
	boolean isConnected();
	boolean login(in String username, in String password);
	void sendMessage(in String to, in String message);
}
