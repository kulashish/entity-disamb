package wikiGroundTruth.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientWikisauras {

	private Socket echoSocket;
	private PrintWriter out;
	private ObjectInputStream in;

	private static InetAddress address;
	private static int port = 4448;

	public ClientWikisauras() {
		String ip = "127.0.1.1";
		try {
			this.address = InetAddress.getLocalHost();//InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void openSocket() {
		try {
			// System.out.println(address.toString());
			echoSocket = new Socket(address, port);
			out = new PrintWriter(echoSocket.getOutputStream(), true);
			in = new ObjectInputStream(echoSocket.getInputStream());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeSocket() {
		try {
			echoSocket.close();
			out.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public LabelSense getSenses(String term) throws Exception {
		LabelSense temp = null;
		openSocket();
		String userInput = ServerWikisauras.gSenses + "\t" + term;
		out.println(userInput);
		try {
			temp = (LabelSense) in.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		closeSocket();
		return temp;
	}

	public String getAnchor(String pageTitle) {
		String temp = null;
		openSocket();
		String userInput = ServerWikisauras.ganchor + "\t" + pageTitle;
		out.println(userInput);
		try {
			temp = (String) in.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		closeSocket();
		return temp;
	}

	public ArrayList<String> getArticle(String pagetitle) {
		ArrayList<String> temp = new ArrayList<String>();
		openSocket();
		String userInput = ServerWikisauras.garticle + "\t" + pagetitle;
		out.println(userInput);
		try {
			temp = (ArrayList<String>) in.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		closeSocket();
		return temp;
	}

	public String getTitle(String pageTitle) {
		String temp = null;
		openSocket();
		String userInput = ServerWikisauras.gTitle + "\t" + pageTitle;
		out.println(userInput);
		try {
			temp = (String) in.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		closeSocket();
		return temp;
	}

	public static void main(String[] args) throws IOException {

		ClientWikisauras obj = new ClientWikisauras();
		System.out.println("here");
		System.out.println(InetAddress.getLocalHost().toString());
		System.out.println("::::::::::" + obj.getArticle("Kali Yuga").get(0));
		try {
			LabelSense temp = obj.getSenses("American Film Institute");
			System.out.println(temp.wikiMinerCandidate.length);
			System.out.println(temp.wikiMinerCandidate[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
