package wikiGroundTruth;

import in.ac.iitb.cse.mrf.data.AMNWeights;
import in.ac.iitb.cse.mrf.util.MathHelper;
import it.unimi.dsi.util.Interval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import spotting.CollectiveTraining;
import spotting.Config;
import spotting.KeywordsGroundTruth;
import spotting.LuceneIndexWrapper;
import spotting.NodePotentials;
import spotting.NodePotentialsSet;
import spotting.TrainingData;
import spotting.KeywordsGroundTruth.Mention;
import util.DisambProperties;
import util.GraphCreator;
import util.ParseXML;
import util.XMLTagInfo;
import wikiGroundTruth.server.LabelSense;
import disamb.LPInference;

public class AjaxHandler extends HttpServlet {
	private static String groundFilename = "/home/kanika/wikiTraining/ground/wikipediaGroundtruth.xml";
	private static String kddGroundFilename = "/home/kanika/wikiTraining/ground/KddGroundTruth.xml";
	private static String trainDir = "/home/kanika/wikiTraining/CorrectedDocs/";
	private static String xmlDir = "/home/kanika/wikiTraining/annotationXmls/";

	private static String logRemainingFiles = "/home/kanika/wikiTraining/logs/remainingFiles.txt";
	private static String logOngoingFiles = "/home/kanika/wikiTraining/logs/ongoingFiles.txt";
	private static String logCompletedFiles = "/home/kanika/wikiTraining/logs/completedFiles.txt";
	private static String logModelFiles = "/home/kanika/wikiTraining/logs/modelFiles.txt";

	private static int maxNodes = 50;
	private static int consolidationSize = 8;
	private static int csize = 40;

	private static String learningPropertiesFile = "/home/kanika/wikiTraining/learning.properties";
	private static String amnWeightsFile = "/home/kanika/wikiTraining/logs/amnWeights.txt";

	private boolean isInitialized = false;
	private HashSet<String> remainingFiles;
	private HashSet<String> ongoingFiles;
	private HashSet<String> completedFiles;
	private HashSet<String> modelFiles;
	private HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap;
	private HashMap<String, ArrayList<XMLTagInfo>> kddGroundTruthMap;
	
	private KeywordsGroundTruth wikiminerKeywords;

	private AMNWeights amnWeights;
	private String username;
	//LuceneIndexWrapper luceneIndex;
	
	//public AjaxHandler(LuceneIndexWrapper _luceneIndex){
	public AjaxHandler(){
		//luceneIndex = _luceneIndex;
	}
	
	public KeywordsGroundTruth getKeywordsGroundTruth(){
		return wikiminerKeywords;
	}

	public void init() {
		try {
			loadState(true);

			Config.wikiSense = true;
			Config.Server = false;

			ParseXML pm = new ParseXML();
			fileGroundTruthMap = pm.parseXML(groundFilename);
			kddGroundTruthMap = pm.parseXML(kddGroundFilename);

			isInitialized = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void destroy() {
		try {
			saveState();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadState(boolean flag) throws Exception {
		File remaining = new File(logRemainingFiles);
		if (remaining.exists()) {
			remainingFiles = readFileLines(logRemainingFiles);
			ongoingFiles = readFileLines(logOngoingFiles);
			completedFiles = readFileLines(logCompletedFiles);
			modelFiles = readFileLines(logModelFiles);
		} else {
			remainingFiles = new HashSet<String>();
			ongoingFiles = new HashSet<String>();
			completedFiles = new HashSet<String>();
			modelFiles = new HashSet<String>();
			File folder = new File(trainDir);
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					String filename = listOfFiles[i].getName();
					remainingFiles.add(filename);
				}
			}
		}

		if (flag) {
			ongoingFiles = new HashSet<String>();
		}

		File weights = new File(amnWeightsFile);
		if (weights.exists()) {
			amnWeights = new AMNWeights();
			amnWeights.readFromFile(amnWeightsFile);

			Config.amn_w0 = MathHelper.asString(amnWeights.getW0());
			Config.amn_w1 = MathHelper.asString(amnWeights.getW1());
			Config.amn_w00 = MathHelper.asString(amnWeights.getW00());
			Config.amn_w11 = MathHelper.asString(amnWeights.getW11());
		} else {
			amnWeights = null;
		}
	}

	public void saveState() throws Exception {
		writeFileLines(logRemainingFiles, remainingFiles);
		writeFileLines(logOngoingFiles, ongoingFiles);
		writeFileLines(logCompletedFiles, completedFiles);
		writeFileLines(logModelFiles, modelFiles);
		if (amnWeights != null)
			amnWeights.serialize(amnWeightsFile);
	}

	public HashSet<String> readFileLines(String filename) {
		HashSet<String> lines = new HashSet<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			while (line != null) {
				lines.add(line);
				line = br.readLine();
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lines;
	}

	public void writeFileLines(String filename, HashSet<String> lines) {
		if (lines == null)
			return;
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename,
					false));
			for (String line : lines) {
				bw.write(line);
				bw.write("\n");
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getNewFilename() throws IOException {
		for (String file : remainingFiles) {
			if (ongoingFiles.contains(file))
				continue;
			else {
				ongoingFiles.add(file);
				return file;
			}
		}
		return null;
	}

	public ArrayList<Annotation> AnnotateText(String text, Document doc,
			boolean local, boolean global, int localup, double globalup,
			double globallo) {
		
		// save text to wikitraining corrected docs;
/*
		String fileName = new SimpleDateFormat("yyyyMMddhhmm'.txt'")
				.format(new Date());

		//TODO: Remove this file writing.
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(trainDir + fileName));
			writer.write(text);

		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}

		File file = new File(logRemainingFiles);
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(file, true);
			BufferedWriter bufferFileWriter = new BufferedWriter(fileWriter);
			fileWriter.append(fileName + "\n");
			bufferFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
*/
		
		boolean lconstraint = local;
		double lg = globallo;
		boolean gconstraint = global;
		int ul = localup;
		double ug = globalup;
		//CollectiveTraining ct1 = new CollectiveTraining(trainDir, "", "",fileName,"");
		//changed to
		CollectiveTraining ct1 = new CollectiveTraining(trainDir, "", "","","");
		
		long spotterstartTime = System.currentTimeMillis();
		ct1.spotterForText(text);
		long spotterendTime = System.currentTimeMillis();
		
		long diff1 = (spotterendTime - spotterstartTime);

		System.out.println("Time taken by spotter: " + diff1 + " milliseconds");
		
		wikiminerKeywords = ct1.wikiminerKeywords;
		
		TrainingData trainData = ct1.getData();
		HashMap<String, Double> result = new HashMap<String, Double>();
		LPInference infer;
		long inferencestartTime = System.currentTimeMillis();
		
		infer = new LPInference(gconstraint, lconstraint, ug, lg, ul);
		result = infer.runLPInference(trainData);
		
		long inferenceendTime = System.currentTimeMillis();
		
		long diff2 = (inferenceendTime - inferencestartTime);

		System.out.println("Time taken by inference: " + diff2 + " milliseconds");
		
		NodePotentialsSet nps = trainData.nodes;
		System.out.println("List of NodePotentials:");
		for (NodePotentials np : nps.potentials_set) {
			if (result.containsKey(np.name)) {
				double val = result.get(np.name);
				np.label = (int) val;
			} else {
				np.label = 0;
			}
			System.out.println("\tnp name : " + np.name + " label : " + np.label);
		}
		doc.docText = text;
		// System.out.println(doc.docText.length());
		//SUNNY: commented file not saved anymore
		//doc.docTitle = fileName;

		ArrayList<Annotation> wikiAndSpotter = new ArrayList<Annotation>();
		HashMap<Integer, ArrayList<NodePotentials>> m2eSpotter = new HashMap<Integer, ArrayList<NodePotentials>>();
		HashSet<String> addedEntities = new HashSet<String>();

		for (NodePotentials np : nps.potentials_set) {
			if (addedEntities.contains(np.name))
				continue;
			if (m2eSpotter
					.containsKey(Integer.parseInt(np.mention.split("_")[1]))) {
				// System.out.println("found in NodePotentials corresponding to mention "+np.mention.split("_")[0]+"   "+np.mention.split("_")[1]);
				m2eSpotter.get(Integer.parseInt(np.mention.split("_")[1])).add(
						np);
			} else {
				m2eSpotter.put(Integer.parseInt(np.mention.split("_")[1]),
						new ArrayList<NodePotentials>());
				m2eSpotter.get(Integer.parseInt(np.mention.split("_")[1])).add(
						np);
			}
		}

		for (int off : m2eSpotter.keySet()) {
			Annotation an = new Annotation();
			an.interval = m2eSpotter.get(off).get(0).interval;
			an.mention = m2eSpotter.get(off).get(0).mention.split("_")[0];
			an.candidateEntities = m2eSpotter.get(off);
			an.isPresentInWiki = false;
			wikiAndSpotter.add(an);
		}
		ArrayList<Annotation> resultAnnotations = new ArrayList<Annotation>();
		Iterator<Annotation> it = wikiAndSpotter.iterator();
		while (it.hasNext()) {
			Annotation a = it.next();
			if (a == null || a.interval == null
					|| a.interval.left >= doc.docText.length() - 1
					|| a.interval.right >= doc.docText.length()) {
				continue;
			} else {
				resultAnnotations.add(a);
			}
		}
		
		Collections.sort(wikiAndSpotter);
		removeContainedSpots(wikiAndSpotter);
		
		removeNonMaxSpots(wikiAndSpotter);
		
		System.out.println("data computed");
		return wikiAndSpotter;

	}

	public ArrayList<Annotation> getAnnotatedDocument(String filename,
			Document doc, boolean runInference, boolean runLogistic) {
		ArrayList<Annotation> wikiAndSpotter = new ArrayList<Annotation>();
		// CollectiveTraining ct1 = new CollectiveTraining(trainDir,
		// groundFilename, "", filename);
		// ct1.spotterNew();
		// TrainingData trainData = ct1.getData();
		TrainingData trainData = new TrainingData();
		GraphCreator creator = GraphCreator.getInstance();
		String kddspotsPath = DisambProperties.getInstance()
				.getkddSpotFilesPath();
		String wikispotsPath = DisambProperties.getInstance()
				.getwikiSpotFilesPath();
		System.out.println(kddspotsPath + " " + wikispotsPath + " " + filename);
		if (filename.startsWith("KDD_")) {
			String kddfilename = filename.substring(4, filename.length());
			File file = new File(kddspotsPath + kddfilename + ".spots");
			try {
				trainData = creator.loadFromFile(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			File file = new File(wikispotsPath + filename + ".spots");
			try {
				trainData = creator.loadFromFile(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		HashMap<String, Double> result = new HashMap<String, Double>();
		if (runInference) {
			Config.useManualAMNWeights = true;
			LPInference infer = new LPInference();
			result = infer.runLPInference(trainData);
			// MinCutInference infer = new MinCutInference();
			// infer.loadEntityData(trainData);
			// result = infer.runInference();
		} else if (runLogistic && Config.logistic) {
			for (NodePotentials np : trainData.nodes.potentials_set) {
				result.put(np.name, np.logistic_label);
			}
		}

		NodePotentialsSet nps = trainData.nodes;
		for (NodePotentials np : nps.potentials_set) {
			if (result.containsKey(np.name)) {
				double val = result.get(np.name);
				np.label = (int) val;
			} else {
				np.label = 0;
			}
		}

		ArrayList<XMLTagInfo> infoList = new ArrayList<XMLTagInfo>();
		if (completedFiles.contains(filename)) {
			infoList = ParseAnnotations.parseXML(xmlDir + filename + ".xml");
			username = ParseAnnotations.username;
		} else if (filename.startsWith("KDD_")) {
			infoList = kddGroundTruthMap.get(filename.substring(4));
		} else {
			infoList = fileGroundTruthMap.get(filename);
		}

		doc.docText = trainData.groundtruth.getDocumentText();
		// System.out.println(doc.docText.length());
		doc.docTitle = filename;
		// Serializer.encode(doc, "/home/kanika/wikiTraining/NewObjects/",
		// filename + "wiki");

		HashMap<Integer, ArrayList<NodePotentials>> m2eSpotter = new HashMap<Integer, ArrayList<NodePotentials>>();
		HashMap<Integer, ArrayList<NodePotentials>> m2eWiki = new HashMap<Integer, ArrayList<NodePotentials>>();
		HashSet<String> addedEntities = new HashSet<String>();

		for (XMLTagInfo in : infoList) {
			if (in.wikiEntity == null || "".equals(in.wikiEntity)) {
				continue;
			}
			if (in.mention == null || in.mention.length() <= 0) {
				if (in.offset >= 0
						&& in.offset + in.length <= doc.docText.length()) {
					in.mention = doc.docText.substring(in.offset, in.offset
							+ in.length);
				} else {
					continue;
				}
			}
			in.mention = in.mention.split("_")[0];
			if (in.offset >= doc.docText.length() - 1
					|| in.offset + in.length >= doc.docText.length()) {
				continue;
			}

			if (!doc.docText.substring(in.offset,
					in.offset + in.mention.length()).equals(in.mention)) {
				int newoffset = doc.docText.indexOf(in.mention);
				if (newoffset >= 0) {
					in.offset = newoffset;
				} else {
					in.offset = doc.docText.length() + 1;
					doc.docText += " " + in.mention;
				}
			}
			NodePotentials n = new NodePotentials();
			n.interval = Interval.valueOf((int) in.offset, in.offset
					+ in.mention.length() - 1);
			n.name = in.wikiEntity;
			n.mention = in.mention;
			n.label = 1;
			n.isPresentInWiki = false;
			if (!m2eWiki.containsKey(in.offset)) {
				m2eWiki.put(in.offset, new ArrayList<NodePotentials>());
			}
			m2eWiki.get(in.offset).add(n);
			addedEntities.add(n.name);
		}

		for (int off : m2eWiki.keySet()) {
			Annotation an = new Annotation();
			an.interval = m2eWiki.get(off).get(0).interval;
			an.mention = m2eWiki.get(off).get(0).mention;
			an.candidateEntities = m2eWiki.get(off);
			an.wikiEntity = m2eWiki.get(off).get(0).name;
			// System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&& "+an.wikiEntity);
			an.isPresentInWiki = m2eWiki.get(off).get(0).isPresentInWiki;
			wikiAndSpotter.add(an);
		}
		for (NodePotentials np : nps.potentials_set) {
			if (addedEntities.contains(np.name))
				continue;
			if (m2eSpotter
					.containsKey(Integer.parseInt(np.mention.split("_")[1]))) {
				// System.out.println("found in NodePotentials corresponding to mention "+np.mention.split("_")[0]+"   "+np.mention.split("_")[1]);
				m2eSpotter.get(Integer.parseInt(np.mention.split("_")[1])).add(
						np);
			} else {
				m2eSpotter.put(Integer.parseInt(np.mention.split("_")[1]),
						new ArrayList<NodePotentials>());
				m2eSpotter.get(Integer.parseInt(np.mention.split("_")[1])).add(
						np);
			}
		}

		for (int off : m2eSpotter.keySet()) {
			Annotation an = new Annotation();
			an.interval = m2eSpotter.get(off).get(0).interval;
			an.mention = m2eSpotter.get(off).get(0).mention.split("_")[0];
			an.candidateEntities = m2eSpotter.get(off);
			an.isPresentInWiki = m2eSpotter.get(off).get(0).isPresentInWiki;
			wikiAndSpotter.add(an);
		}

		ArrayList<Annotation> resultAnnotations = new ArrayList<Annotation>();
		Iterator<Annotation> it = wikiAndSpotter.iterator();
		while (it.hasNext()) {
			Annotation a = it.next();
			if (a == null || a.interval == null
					|| a.interval.left >= doc.docText.length() - 1
					|| a.interval.right >= doc.docText.length()) {
				continue;
			} else {
				resultAnnotations.add(a);
			}
		}

		Collections.sort(wikiAndSpotter);
		removeContainedSpots(wikiAndSpotter);
		removeNonMaxSpots(wikiAndSpotter);

		/*
		 * for (int i = 0; i < wikiAndSpotter.size(); i++) {
		 * System.out.println(wikiAndSpotter.get(i).interval.left + "," +
		 * wikiAndSpotter.get(i).interval.right + "," +
		 * (wikiAndSpotter.get(i).mention.length() + wikiAndSpotter
		 * .get(i).interval.left) + "," + wikiAndSpotter.get(i).mention + "," +
		 * wikiAndSpotter.get(i).isPresentInWiki); }
		 */

		// adding NA to candidate entities options
		/*
		 * for (int i = 0; i < wikiAndSpotter.size(); i++) { if
		 * (wikiAndSpotter.get(i).isPresentInWiki == false) { NodePotentials n =
		 * new NodePotentials(); n.context_score_frequent = 0; n.inlink_count =
		 * 0; n.outlink_count = 0; n.interval =
		 * wikiAndSpotter.get(i).candidateEntities.get(0).interval; n.mention =
		 * wikiAndSpotter.get(i).candidateEntities.get(0).mention; n.name =
		 * "NA"; wikiAndSpotter.get(i).candidateEntities.add(n); } }
		 */
		System.out.println("data computed");
		return wikiAndSpotter;
	}

	private void removeContainedSpots(ArrayList<Annotation> npList) {
		Annotation an = new Annotation();
		int[] importance = new int[npList.size()];
		for (int i = 0; i < importance.length; i++)
			importance[i] = 1;

		for (int i = 0; i < npList.size(); i++) {
			for (int j = i + 1; j < npList.size(); j++) {
				if (i != j) {
					if (an.contains(npList.get(i).interval,
							npList.get(j).interval)) {
						//
						if (importance[i] != 0) {
							if (npList.get(i).isPresentInWiki) {
								importance[i] = 1;
								importance[j] = 0;
								mergeCandidateEntities(npList.get(j),
										npList.get(i));
							} else if (npList.get(j).isPresentInWiki) {
								importance[i] = 0;
								importance[j] = 1;
								mergeCandidateEntities(npList.get(i),
										npList.get(j));
							} else {
								importance[i] = 1;
								importance[j] = 0;
								mergeCandidateEntities(npList.get(j),
										npList.get(i));
							}

						} else {
							importance[j] = 0;
						}
					}
				}
			}
		}
		int i = 0;
		Iterator<Annotation> iter = npList.iterator();
		while (iter.hasNext()) {
			Annotation np = iter.next();
			if (importance[i] == 0) {
				iter.remove();
			}
			i++;
		}
	}

	private void removeNonMaxSpots(ArrayList<Annotation> npList) {
		Annotation an = new Annotation();
		NodePotentials nps = new NodePotentials();
		int[] importance = new int[npList.size()];
		for (int i = 0; i < importance.length; i++)
			importance[i] = 1;

		for (int i = 0; i < npList.size(); i++) {
			for (int j = i + 1; j < npList.size(); j++) {
				if (i != j) {
					if (!an.disjoint(npList.get(i).interval,
							npList.get(j).interval)) {
						if (npList.get(i).isPresentInWiki) {
							importance[i] = 1;
							importance[j] = 0;
							mergeCandidateEntities(npList.get(j), npList.get(i));
						} else if (npList.get(j).isPresentInWiki) {
							importance[j] = 1;
							importance[i] = 0;
							mergeCandidateEntities(npList.get(i), npList.get(j));
						} else if (npList.get(j).interval.length() > npList
								.get(i).interval.length()) {
							importance[j] = 1;
							importance[i] = 0;
							mergeCandidateEntities(npList.get(i), npList.get(j));
						} else {
							importance[i] = 1;
							importance[j] = 0;
							mergeCandidateEntities(npList.get(j), npList.get(i));
						}
					}
				}
			}
		}
		int i = 0;
		Iterator<Annotation> iter = npList.iterator();
		while (iter.hasNext()) {
			Annotation np = iter.next();
			if (importance[i] == 0) {
				iter.remove();
			}
			i++;
		}
	}

	private void mergeCandidateEntities(Annotation source, Annotation dest) {
		if (source.candidateEntities == null) {
			return;
		}
		if (dest.candidateEntities == null) {
			dest.candidateEntities = new ArrayList<NodePotentials>();
		}
		for (NodePotentials np : source.candidateEntities) {
			dest.candidateEntities.add(np);
		}
	}

	public String getAnnotationHTML(ArrayList<Annotation> spots, Document doc) {
		String html = "";
		int index = 0;
		int key = -1;
		String curr_file = doc.docTitle;
		HashSet<String> addedMentions = new HashSet<String>();
		System.out.println("building html");
		for (int j = 0; j < doc.docText.length();) {
			if (index < spots.size())
				key = spots.get(index).interval.left;
			else
				key = doc.docText.length();
			if (j < key) {
				while (j < key) {
					html += doc.docText.charAt(j);
					j++;
				}
			} else if (j >= key) {
				j = key;
				if (j >= doc.docText.length())
					continue;
				String mention = spots.get(index).mention;
				if (addedMentions.contains(mention)) {
					html += mention;
					j += mention.length();
					index++;
				} else {
					addedMentions.add(mention);
					html += "<span class=\"myTag\"><a href=\"javascript:void(0)\">"
							+ mention + "</a>\n";
					html += "<ul>\n";
					int offset = spots.get(index).interval.left;
					String ent = spots.get(index).candidateEntities.get(0).name;
					ArrayList<NodePotentials> npList = spots.get(index).candidateEntities;
					Collections.sort(npList, new Comparator<NodePotentials>() {
						@Override
						public int compare(NodePotentials o1, NodePotentials o2) {
							return Double.compare(o2.label, o1.label);
						}
					});

					HashSet<String> addedEnt = new HashSet<String>();
					for (NodePotentials obj : npList) {
						if (!addedEnt.contains(obj.name)) {
							obj.name = obj.name.replace("'", "&#39;").replace(
									" ", "_");
							String mymention = mention;
							if (obj.mention != null && !"".equals(obj.mention))
								mymention = obj.mention;
							String title = obj.name + ":" + key + ":"
									+ mymention.replace("'", "&#39;");
							if (obj.label == 0) {
								html += "<li class=\"spcl\"><a href=\"http://en.wikipedia.org/wiki/"
										+ obj.name
										+ "\" "
										+ "target=\"_blank\" title=\""
										+ title
										+ "\">"
										+ obj.name
										+ " <input type=\"checkbox\" class=\"annotate\"></a></li>\n";
							} else {
								html += "<li><a href=\"http://en.wikipedia.org/wiki/"
										+ obj.name
										+ "\" "
										+ "target=\"_blank\" title=\""
										+ title
										+ "\">"
										+ obj.name
										+ " <input type=\"checkbox\" class=\"annotate\" checked></a></li>\n";
							}
							addedEnt.add(obj.name);
						}
					}
					html += "<li><input type=\"text\" class=\"add-entities\" /></li>\n";
					html += "</ul></span>\n";
					j += mention.length();
					index++;
				}
			}
		}
		System.out.println("html constructed");
		// System.out.println("html: " + html);
		return html;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		System.out.println(request.getParameter("filename"));
		System.out.println(request.getParameter("type"));
		if (isInitialized == false) {
			try {
				init();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}

		String type = request.getParameter("type");
		if (type == null)
			return;
		else if ("init".equals(type))
			getInitResponse(request, response);
		else if ("list".equals(type))
			getListResponse(request, response);
		else if ("learn".equals(type))
			getLearnResponse(request, response);
		else if ("check".equals(type))
			getCheckResponse(request, response);
		else if ("manual".equals(type))
			getManualResponse(request, response);
		else if ("save".equals(type))
			getSaveResponse(request, response);
		else if ("userInput".equals(type)) {
			getTextResponse(request, response);
		}

	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	public void getInitResponse(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.print("{ \"prevSize\": \"" + modelFiles.size() + "\","
				+ "  \"currSize\": \"" + completedFiles.size() + "\"" + "}");
	}

	public void getListResponse(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		try {
			loadState(false);
		} catch (Exception e) {
			System.out.println("Error loading state");
			e.printStackTrace();
		}

		boolean flag = true;
		String remaining = "";
		for (String rem : remainingFiles) {
			if (ongoingFiles.contains(rem))
				continue;
			if (flag)
				flag = false;
			else
				remaining += ", ";
			remaining += "\"" + rem + "\"";
		}
		flag = true;
		String completed = "";
		for (String com : completedFiles) {
			if (flag)
				flag = false;
			else
				completed += ", ";
			completed += "\"" + com + "\"";
		}
		out.print("{ \"remaining\": [ " + remaining + " ], "
				+ "  \"completed\": [ " + completed + " ] " + "}");
	}

	public void getLearnResponse(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		try {
			amnWeights = CollectiveTraining.train(learningPropertiesFile);
			Config.amn_w0 = MathHelper.asString(amnWeights.getW0());
			Config.amn_w1 = MathHelper.asString(amnWeights.getW1());
			Config.amn_w00 = MathHelper.asString(amnWeights.getW00());
			Config.amn_w11 = MathHelper.asString(amnWeights.getW11());

			modelFiles = new HashSet<String>(completedFiles);

			saveState();

			out.print("{ \"result\": \"success\"}");
		} catch (Exception e) {
			e.printStackTrace();
			out.print("{ \"result\": \"failure\"}");
		}
	}

	public void getTextResponse(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		String text = request.getParameter("doc");
		String localval = request.getParameter("local");
		String globalval = request.getParameter("global");
		boolean local = false;
		boolean global = false;

		int lu = 1;
		int gu = 1;
		int gl = 1;

		if (!localval.equals("1"))
			local = true;

		if (localval.equals("2"))
			lu = 1;
		else if (localval.equals("3"))
			lu = 2;
		else if (localval.equals("4"))
			lu = 3;
		else if (localval.equals("5"))
			lu = 4;
		else if (localval.equals("6"))
			lu = 5;
		else if (localval.equals("7"))
			lu = 6;

		if (!globalval.equals("1"))
			global = true;

		if (globalval.equals("2")) {
			gu = 2;
			gl = 1;
		} else if (localval.equals("3")) {
			gu = 3;
			gl = 1;
		} else if (localval.equals("4")) {
			gu = 3;
			gl = 2;
		} else if (localval.equals("5")) {
			gu = 4;
			gl = 1;
		} else if (localval.equals("6")) {
			gu = 4;
			gl = 2;
		} else if (localval.equals("7")) {
			gu = 4;
			gl = 3;
		}

		System.out.println("Local: " + local + "lu " + lu);
		System.out.println("Global: " + global + "gu " + gu + " gl " + gl);
		System.out.println("Text ------------------>" + text);

		Document doc = new Document();
		ArrayList<Annotation> spots = null;
		spots = AnnotateText(text, doc, local, global, lu, gu, gl);
		System.out.println("spots size in getTextResponse :: " + spots.size());

		String html = getAnnotationHTML(spots, doc);
		out.print(html);
	}

	public void getCheckResponse(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		HttpSession session = request.getSession(true);

		String newFile = request.getParameter("filename");
		System.out.println("filename ------------------>" + newFile);
		if (newFile == null || "".equals(newFile) || "random".equals(newFile))
			newFile = getNewFilename();
		if (newFile == null) {
			out.print("All files have been annotated");
			return;
		}
		System.out.println("filename ------------------>" + newFile);
		// session.setAttribute("docName", newFile);
		out.print("<p class=\"docName\" style=\"display: none\">" + newFile
				+ "</p>");
		Document doc = new Document();
		ArrayList<Annotation> spots = null;
		if (completedFiles.contains(newFile)) {
			System.out.println("filename ------------------>" + newFile);
			spots = getAnnotatedDocument(newFile, doc, false, false);
			if (username != null && !"".equals(username))
				out.print("<p class=\"userName\">Tagged by: " + username
						+ "</p>");

		} else {
			Config.logistic = false;
			spots = getAnnotatedDocument(newFile, doc, true, false);
			System.out.println("spots size in getCheckResponse :: "
					+ spots.size());
		}
		String html = getAnnotationHTML(spots, doc);
		out.print(html);
	}

	public void getManualResponse(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		HttpSession session = request.getSession(true);

		String newFile = request.getParameter("filename");
		if (newFile == null || "".equals(newFile) || "random".equals(newFile))
			newFile = getNewFilename();
		if (newFile == null) {
			out.print("All files have been annotated");
			return;
		}

		// session.setAttribute("docName", newFile);
		out.print("<p class=\"docName\" style=\"display: none\">" + newFile
				+ "</p>");
		Document doc = new Document();
		ArrayList<Annotation> spots = null;
		if (completedFiles.contains(newFile)) {
			spots = getAnnotatedDocument(newFile, doc, false, false);
			if (username != null && !"".equals(username))
				out.print("<p class=\"userName\">Tagged by: " + username
						+ "</p>");

		} else {
			Config.logistic = true;
			spots = getAnnotatedDocument(newFile, doc, false, true);
		}

		String html = getAnnotationHTML(spots, doc);
		out.print(html);
	}

	public void getSaveResponse(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String docName = request.getParameter("doc");
		String userName = request.getParameter("user");
		String data = request.getParameter("data");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		System.out.println("writing xml for " + docName);
		try {
			BufferedWriter outXml = new BufferedWriter(new FileWriter(xmlDir
					+ docName + ".xml"));
			outXml.write("<iitb.wikipedia.entityAnnotations>");
			outXml.write("\n");
			if (userName != null && !"".equals(userName))
				outXml.write("<userName>" + userName + "</userName>");
			String[] dataset = data.split("\\|");
			for (String annotation : dataset) {
				if (annotation == null || "".equals(annotation))
					continue;
				System.out.println(annotation);
				String[] params = annotation.split(":");
				if (params.length < 3)
					continue;

				outXml.write("<annotation>");
				outXml.write("\n");

				outXml.write("<docName>" + docName.replaceAll("&", "&amp;")
						+ "</docName>");
				outXml.write("\n");

				outXml.write("<wikiName>" + params[0].replaceAll("&", "&amp;")
						+ "</wikiName>");
				outXml.write("\n");

				outXml.write("<offset>" + params[1] + "</offset>");
				outXml.write("\n");

				outXml.write("<mention>" + params[2].replaceAll("&", "&amp;")
						+ "</mention>");
				outXml.write("\n");

				outXml.write("<length>" + params[2].length() + "</length>");
				outXml.write("\n");

				outXml.write("</annotation>");
				outXml.write("\n");
			}
			outXml.write("</iitb.wikipedia.entityAnnotations>");
			outXml.close();

			ongoingFiles.remove(docName);
			remainingFiles.remove(docName);
			completedFiles.add(docName);

			saveState();

			out.print("{ \"result\": \"success\"}");
		} catch (Exception e) {
			e.printStackTrace();
			out.print("{ \"result\": \"failure\"}");
		}
	}

}
