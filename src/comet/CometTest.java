package comet;

import java.io.IOException;
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
 *         Essa servlet implementa a especificação COMET, que cria um polling de
 *         longa duração (long polling) para atender requests do client.
 * 
 *         Basicamente, o client envia um request ao app server que por sua vez
 *         suspende a requisição até que ocorra timeout ou alguma resposta
 *         esteja disponível. Quando o client recebe uma resposta (seja timeout
 *         ou um response) o request é finalizado e, na sequencia o client abre
 *         outro request.
 * 
 *         Para que a servlet funcione pe necessário que a especificação da
 *         Servlet seja a 3.0 ou posterior (implementação 2.5) pois é a partir
 *         dessa versão que está disponível o AsyncContex.
 */

@WebServlet(urlPatterns = "/comet", displayName = "comet", asyncSupported = true)
public final class CometTest extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Fila que irá manter os requests do client assincronos
	 */
	private final Queue<AsyncContext> queue = new ConcurrentLinkedQueue<AsyncContext>();

	/**
	 * contado usado para "cadastrar" um usuario no exemplo do chat
	 */
	private static int counter = 0;

	/**
	 * hashmap com a lista de usuarios conectados
	 */
	public static Map<String, String> users = new HashMap<String, String>();

	/**
	 * hashmap com a lista de notificacoes onde a chave é o id da sessao do
	 * client
	 */
	private static Map<String, List<String>> notifications = new HashMap<String, List<String>>();

	/**
	 * thread que fica escutando os requests assincronos da fila
	 */
	private final Thread generator = new Thread() {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {

				while (!queue.isEmpty()) {
					try {
						/**
						 * peek recupera o AsyncContext da fila, mas nao o
						 * consome (diferente do metodo poll)
						 */
						process(queue.peek());
					} catch (Exception e) {
						System.out.println("ERRO VIOLENTO");
						e.printStackTrace();
					}
				}
			}
		}
	};

	/**
	 * starta a thread
	 */
	@Override
	public void init() throws ServletException {
		generator.start();
	}

	/**
	 * interrompe a thread
	 */
	@Override
	public void destroy() {
		generator.interrupt();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		/**
		 * transforma o request em assincrono, seta o timeout para 40 segundos e
		 * o adiciona na fila
		 */
		AsyncContext actx = req.startAsync();
		actx.setTimeout(40000);
		queue.offer(actx);

		/**
		 * Este listener é responsável por remover o request assincrono da fila
		 * após o timeout ser atingido
		 */
		actx.addListener(new AsyncListener() {
			@Override
			public void onTimeout(AsyncEvent arg0) throws IOException {
				queue.remove(arg0.getAsyncContext());
			}

			@Override
			public void onStartAsync(AsyncEvent arg0) throws IOException {
			}

			@Override
			public void onError(AsyncEvent arg0) throws IOException {
				queue.remove(arg0.getAsyncContext());
			}

			@Override
			public void onComplete(AsyncEvent arg0) throws IOException {
				queue.remove(arg0.getAsyncContext());
			}
		});
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		/**
		 * recupera a mensagem do chat, recupera o username e envia um broadcast
		 * da mensagem a todos conectados ao chat
		 */
		String msg = req.getParameter("notification");
		String username = users.get(req.getSession().getId());
		sendBroadcast(username + ": " + msg);
	}

	/**
	 * 
	 * @param ctx
	 * 
	 *            processa o AsyncContext recebido da thread
	 */
	private void process(AsyncContext ctx) {
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
		HttpServletResponse res = (HttpServletResponse) ctx.getResponse();
		HttpSession session = req.getSession();

		if (session == null) {
			return;
		}

		String sessId = session.getId();

		/**
		 * recupera a lisa de mensagens por session id
		 */
		List<String> msgs = notifications.get(sessId);

		// se a lista de mensagens possuir elementos, itera e monta um JSON com
		// um array de mensagens para enviar ao client
		if (!msgs.isEmpty()) {
			try {
				String s = "[";

				for (String msg : msgs) {
					s += "{\"msg\": " + "\"" + msg + "\"}, ";
				}

				s = s.substring(0, s.length() - 2);
				s += "]";

				/**
				 * remove todas as mensagens processadas da lista
				 */
				msgs.removeAll(msgs);

				/**
				 * responde o JSON ao client
				 */
				res.getWriter().write("" + s + "");
				res.setStatus(HttpServletResponse.SC_OK);
				res.setContentType("application/json");

				/**
				 * flaga a requisição assincrona como complete e a remove da
				 * fila manualmente. Se a requisição tomar timeout será removida
				 * pelo listener
				 */
				ctx.complete();
				queue.remove(ctx);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param sessionId
	 * @return
	 * 
	 *         Adiciona um novo usuario no hash
	 */
	public static String addUser(String sessionId) {
		String user = "User" + counter++;
		users.put(sessionId, user);
		return user;
	}

	/**
	 * 
	 * @param session
	 * 
	 *            remove um usuario do hash. feito para ser usado pelo metodo
	 *            sessionDestroyed no HttpSessionListener
	 */
	public static void removeUser(HttpSession session) {
		users.remove(session);
	}

	/**
	 * 
	 * @param msg
	 * 
	 *            Percorre os usuarios logados e entrega a mensagem a todos
	 */
	public static void sendBroadcast(String msg) {
		for (Iterator<String> it = users.keySet().iterator(); it.hasNext();) {
			String s = it.next();
			sendMsg(s, msg);
		}
	}

	/**
	 * 
	 * @param to
	 * @param msg
	 * 
	 * publica uma mensagem para um sessionid especifico
	 */
	public static void sendMsg(String to, String msg) {
		List<String> msgs = notifications.get(to);

		if (msgs == null) {
			notifications.put(to, new ArrayList<String>());
			msgs = notifications.get(to);
		}

		msgs.add(msg);
	}

}