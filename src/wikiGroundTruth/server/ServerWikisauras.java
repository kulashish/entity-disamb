package wikiGroundTruth.server;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.model.Redirect;

import spotting.Wikisaurus;
import util.DisambProperties;

public class ServerWikisauras {
	private static int port = 4448, maxConnections = 0;

	static public String ganchor = "GETANCHOR";
	static public String gTitle = "GETTITLE";
	static public String gSenses = "GETSENSES";
	static public String garticle = "GETARTICLE";

	public ServerWikisauras() {
		Wikisaurus dummy = new Wikisaurus();
		System.out.println("STORE LOADED");
	}

	public static void main(String[] args) {
		String propsFile = args[0];
		try {
			DisambProperties.init(propsFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ServerWikisauras loadData = new ServerWikisauras();
		int i = 0;
		try {
			ServerSocket listener = new ServerSocket(port, 0, InetAddress
					.getLocalHost());
			Socket server;
			while ((i++ < maxConnections) || (maxConnections == 0)) {
				server = listener.accept();
				doComms5 conn_c = new doComms5(server);
				Thread t = new Thread(conn_c);
				t.start();
			}
		} catch (IOException ioe) {
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
		}
	}
}

class doComms5 implements Runnable {
	private Socket server;
	private String line;

	doComms5(Socket server) {
		this.server = server;

	}

	public void run() {

		try {
			// Get input from the client
			DataInputStream in = new DataInputStream(server.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(server
					.getOutputStream());
			line = in.readLine();
			out.writeObject(process(line));
			server.close();
		} catch (IOException ioe) {
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
		}
	}

	public Object process(String command) {
		StringTokenizer st = new StringTokenizer(command, "\t");
		String curr = null;

		while (st.hasMoreTokens()) {
			curr = st.nextToken();
			if (curr.equals(ServerWikisauras.ganchor)) {
				if (st.hasMoreTokens()) {
					String page_title = st.nextToken();
					return Wikisaurus.getAnchorText(page_title);
				}
			} else if (curr.equals(ServerWikisauras.gTitle)) {
				if (st.hasMoreTokens()) {
					String page_title = st.nextToken();
					// return Wikisaurus.getAnchor(page_title);
				}

			} else if (curr.equals(ServerWikisauras.garticle)) {
				if (st.hasMoreTokens()) {
					String page_title = st.nextToken();
					ArrayList<String> redirectNames = new ArrayList<String>();
					redirectNames.add(page_title);
					Article article = Wikisaurus._wikipedia
							.getArticleByTitle(page_title);
					Redirect[] redirects = null;
					if (article != null)
						redirects = article.getRedirects();
					if (redirects != null) {
						for (int j = 0; j < redirects.length; j++) {
							redirectNames.add(redirects[j].getTitle()
									.replaceAll("_", " "));
						}
					}
					return redirectNames;
				}
			} else if (curr.equals(ServerWikisauras.gSenses)) {
				if (st.hasMoreTokens()) {
					String term = st.nextToken();
					try {
						Label.Sense[] label = Wikisaurus.getAllSenses(term);
						if (label == null)
							return null;
						else {
							// System.out.println("in server ### " +
							// label.length);
							LabelSense temp = new LabelSense(label);
							// System.out.println("in server "
							// + temp.wikiMinerCandidate.length);
							return temp;
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

}
