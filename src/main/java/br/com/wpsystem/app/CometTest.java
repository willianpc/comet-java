package br.com.wpsystem.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 
 * @author willian
 * 
 *         This servlet implements COMET specification. In order to get things
 *         done you must user Servlet API 3.0
 */

@WebServlet(urlPatterns = "/comet", displayName = "comet",
				asyncSupported = true)
public final class CometTest extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Async requests queue
	 */
	private final Queue<AsyncContext> queue = new ConcurrentLinkedQueue<AsyncContext>();

	/**
	 * counter used by username generator for the chat example purpose
	 */
	private static int counter = 0;

	/**
	 * hashmap with all conected users
	 */
	public static Map<String, String> users = new HashMap<String, String>();

	/**
	 * hashmap with the messages
	 */
	private static Map<String, List<String>> notifications = new HashMap<String, List<String>>();

	/**
	 * thread that listens for async requests on the queue
	 */
	private final Thread generator = new Thread() {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {

				while (!queue.isEmpty()) {
					try {
						process(queue.peek());
					} catch (Exception e) {
						System.out.println("freaking error");
						e.printStackTrace();
					}
				}
			}
		}
	};

	@Override
	public void init() throws ServletException {
		generator.start();
	}

	@Override
	public void destroy() {
		generator.interrupt();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
					throws ServletException, IOException {

		/**
		 * it turns the request to an ascyn request and sets its timeout to 40
		 * seconds
		 */
		AsyncContext actx = req.startAsync();
		actx.setTimeout(40000);
		queue.offer(actx);

		/**
		 * Listens for timeout requests and removes them from the queue
		 */
		actx.addListener(new AsyncListener() {
			public void onTimeout(AsyncEvent arg0) throws IOException {

				HttpServletResponse res = (HttpServletResponse) arg0
								.getAsyncContext().getResponse();
				PrintWriter pw = res.getWriter();
				pw.write("{}");
				res.setStatus(HttpServletResponse.SC_OK);
				res.setContentType("application/json");

				queue.remove(arg0.getAsyncContext());
			}

			public void onStartAsync(AsyncEvent arg0) throws IOException {
			}

			public void onError(AsyncEvent arg0) throws IOException {
				queue.remove(arg0.getAsyncContext());
			}

			public void onComplete(AsyncEvent arg0) throws IOException {
				queue.remove(arg0.getAsyncContext());
			}
		});
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res)
					throws ServletException, IOException {

		/**
		 * it gets a message from chat, gets username and sends a broadcast
		 */
		String msg = req.getParameter("notification");
		String username = users.get(req.getSession().getId());
		sendBroadcast(username + ": " + msg);
	}

	private void process(AsyncContext ctx) {
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
		HttpServletResponse res = (HttpServletResponse) ctx.getResponse();
		HttpSession session = req.getSession();

		if (session == null) {
			return;
		}

		String sessId = session.getId();
		List<String> msgs = notifications.get(sessId);

		if (!msgs.isEmpty()) {
			try {
				String s = "[";

				for (String msg : msgs) {
					s += "{\"msg\": " + "\"" + msg + "\"}, ";
				}

				s = s.substring(0, s.length() - 2);
				s += "]";

				msgs.removeAll(msgs);

				res.getWriter().write("" + s + "");
				res.setStatus(HttpServletResponse.SC_OK);
				res.setContentType("application/json");

				ctx.complete();
				queue.remove(ctx);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String addUser(String sessionId) {
		String user = "User" + counter++;
		users.put(sessionId, user);
		return user;
	}

	public static void removeUser(HttpSession session) {
		users.remove(session);
	}

	public static void sendBroadcast(String msg) {
		for (Iterator<String> it = users.keySet().iterator(); it.hasNext();) {
			String s = it.next();
			sendMsg(s, msg);
		}
	}

	public static void sendMsg(String to, String msg) {
		List<String> msgs = notifications.get(to);

		if (msgs == null) {
			notifications.put(to, new ArrayList<String>());
			msgs = notifications.get(to);
		}

		msgs.add(msg);
	}
}