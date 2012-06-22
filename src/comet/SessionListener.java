package comet;

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
		CometTest.sendMsg(sessionId, "[SYSTEM] Bem vindo!");
		CometTest.sendBroadcast("[SYSTEM] " + username + " acabou de entrar");
	}

	public void sessionDestroyed(HttpSessionEvent arg0) {
		HttpSession session = arg0.getSession();
		String username = CometTest.users.get(session);
		CometTest.sendBroadcast("[SYSTEM] " + username + " saiu do chat.");
	}
}