package br.com.wpsystem.app;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class SessionListener implements HttpSessionListener {

	public SessionListener() {
	}

	public void sessionCreated(HttpSessionEvent arg0) {

		String sessionId = arg0.getSession().getId();

		if (CometTest.users.get(sessionId) != null) {
			return;
		}

		CometTest.addUser(sessionId);
		String username = CometTest.users.get(sessionId);
		CometTest.sendMsg(sessionId, "[SYSTEM] Welcome!");
		CometTest.sendBroadcast("[SYSTEM] " + username + " joined.");
	}

	public void sessionDestroyed(HttpSessionEvent arg0) {
		HttpSession session = arg0.getSession();
		String username = CometTest.users.get(session);
		CometTest.sendBroadcast("[SYSTEM] " + username + " left.");
	}
}