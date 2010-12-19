package jm.android.jmxmpp.service;
import java.util.List;
import jm.android.jmxmpp.JmRosterEntry;
import jm.android.jmxmpp.JmMessage;

interface IXmppConnectionService {
	boolean connect(in String host, int port);
	boolean login(in String username, in String password);
	List<JmRosterEntry> getRoster();
	void disconnect();
	void sendMessage(in String to, in String message);
	List<JmMessage> getQueuedMessages(in String from);
	void clearQueuedMessages(in String from);
	void addMessagesToQueue(in String from, in JmMessage[] messages);
}
