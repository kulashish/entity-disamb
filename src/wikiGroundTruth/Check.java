package wikiGroundTruth;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;

import util.ParseXML;
import util.XMLTagInfo;

public class Check {
	static HashSet<String> onGoingFiles;
	static HashSet<String> allFilesInTrainDir;
	static String groundFilename = "/home/kanika/wikiTraining/ground/wikipediaGroundtruth.xml";
	static HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = new HashMap<String, ArrayList<XMLTagInfo>>();

	public static void main(String[] args) throws Exception {
		onGoingFiles = new HashSet<String>();
		allFilesInTrainDir = new HashSet<String>();
		ParseXML pm = new ParseXML();
		fileGroundTruthMap = pm.parseXML(groundFilename);
		WikiAnnotationInterface wi = new WikiAnnotationInterface();
		File folder = new File(wi.trainDir);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String filename = listOfFiles[i].getName();
				allFilesInTrainDir.add(filename);
			}
		}

		// Create the server
		HttpServer server = new HttpServer();

		// Create a port listener
		SocketListener listener = new SocketListener();
		listener.setPort(4080);
		server.addListener(listener);

		// Create a context
		HttpContext context = new HttpContext();
		context.setContextPath("/wiki/*");
		server.addContext(context);

		// Create a servlet container
		ServletHandler servlets = new ServletHandler();
		context.addHandler((HttpHandler)servlets);

		// Map a servlet onto the container
		servlets.addServlet("Dump", "org.mortbay.servlet.Dump");
		servlets.addServlet("check", "wikiGroundTruth.WikiAnnotationInterface");
		servlets.addServlet("check1", "wikiGroundTruth.AnnotationHandler");

		// Serve static content from the context

		context.setResourceBase("/home/kanika/");
		context.addHandler(new ResourceHandler());

		// Start the http server
		server.start();
	}
}
