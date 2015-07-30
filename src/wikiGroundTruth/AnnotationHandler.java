package wikiGroundTruth;

import it.unimi.dsi.util.Interval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spotting.NodePotentials;

public class AnnotationHandler extends HttpServlet {
	public String xmlDir = "/home/kanika/wikiTraining/xmlscheck/";

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		WikiAnnotationInterface wi = new WikiAnnotationInterface();

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		String[] value = request.getParameterValues("entities");
		String offset = request.getParameter("hiddenOffset");
		String mention = request.getParameter("hiddenMention");
		String docName = request.getParameter("hiddenfileName");
		String annotateTrue = request.getParameter("annotate");
		String otherent = request.getParameter("OtherEntity");
		String secondent = request.getParameter("SecondEntity");
		String otherment = request.getParameter("OtherMention");
		String filename = null;

		/**
		 * to see if the file is completed
		 */
		String doneFile = request.getParameter("done");
		String docNameDone = request.getParameter("hiddenfileNameDone");
		System.out.println("doc when done:: " + docNameDone);

		HashMap<Integer, ArrayList<NodePotentials>> currentFileMap = new HashMap<Integer, ArrayList<NodePotentials>>();

		if (doneFile == null) {
			wi.doc = (Document) Serializer.decode(
					"/home/kanika/wikiTraining/NewObjects/", docName + "wiki");
			filename = docName;
			wi.manualDoc = (Document) Serializer
					.decode("/home/kanika/wikiTraining/NewObjects/", docName
							+ "manual");
			currentFileMap = getHashMap(
					"/home/kanika/wikiTraining/NewObjects/", docName, wi);
			System.out.println("---------------------- "
					+ currentFileMap.size() + " ---------------------");

		} else {
			wi.doc = (Document) Serializer.decode(
					"/home/kanika/wikiTraining/NewObjects/", docNameDone
							+ "wiki");
			filename = docNameDone;
			wi.manualDoc = (Document) Serializer.decode(
					"/home/kanika/wikiTraining/NewObjects/", docNameDone
							+ "manual");
			currentFileMap = getHashMap(
					"/home/kanika/wikiTraining/NewObjects/", docNameDone, wi);

		}

		if (doneFile == null && annotateTrue != null) {

			System.out.println("Annotated  " + mention);
			ArrayList<NodePotentials> npList = new ArrayList<NodePotentials>();

			if (!otherent.equals("")) {
				NodePotentials np = new NodePotentials();
				np.name = otherent;

				if (otherment.equals("")) {
					np.mention = mention;
					np.offset = Integer.parseInt(offset);
					np.interval = Interval.valueOf(Integer.parseInt(offset),
							Integer.parseInt(offset) + mention.length());
				} else {
					np.mention = otherment;
					System.out.println(otherment);
					// System.out.println(wi.getDocument().docText);
					np.offset = wi.getDocument().docText.indexOf(otherment);
					np.interval = Interval.valueOf(np.offset, np.offset
							+ otherment.length());
				}
				npList.add(np);
			}

			if (!secondent.equals("")) {
				NodePotentials np = new NodePotentials();
				np.name = secondent;

				if (otherment.equals("")) {
					np.mention = mention;
					np.offset = Integer.parseInt(offset);
					np.interval = Interval.valueOf(Integer.parseInt(offset),
							Integer.parseInt(offset) + mention.length());
				} else {
					np.mention = otherment;
					System.out.println(otherment);
					// System.out.println(wi.getDocument().docText);
					np.offset = wi.getDocument().docText.indexOf(otherment);
					np.interval = Interval.valueOf(np.offset, np.offset
							+ otherment.length());
				}
				npList.add(np);
			}

			if (value != null) {
				for (int i = 0; i < value.length; i++) {
					NodePotentials np = new NodePotentials();
					np.name = value[i];
					// System.out.println(np.name);
					if (otherment.equals("")) {
						np.mention = mention;
						np.offset = Integer.parseInt(offset);
						np.interval = Interval.valueOf(
								Integer.parseInt(offset), Integer
										.parseInt(offset)
										+ mention.length());
					} else {
						np.mention = otherment;
						// System.out.println(otherment);
						// System.out.println(wi.getDocument().docText);
						np.offset = wi.getDocument().docText.indexOf(otherment);
						np.interval = Interval.valueOf(np.offset, np.offset
								+ otherment.length());
					}
					npList.add(np);
				}
			}

			for (int i = 0; i < npList.size(); i++) {
				System.out.println(npList.get(i).mention + " "
						+ npList.get(i).name);
			}

			if (annotateTrue != null && WikiAnnotationInterface.correcting) {
				String m = npList.get(0).mention;

				for (int key : currentFileMap.keySet()) {
					if (key != npList.get(0).offset) {
						ArrayList<NodePotentials> var = currentFileMap.get(key);
						if (var.get(0).mention.equals(m)) {
							ArrayList<NodePotentials> newList = new ArrayList<NodePotentials>();
							for (NodePotentials n : npList) {
								NodePotentials nnew = n.clone();
								newList.add(nnew);
							}
							for (int k = 0; k < newList.size(); k++) {
								newList.get(k).offset = key;
							}
							currentFileMap.put(key, newList);

							HashMap<String, HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>> OBJ = (HashMap<String, HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>>) Serializer
									.decode("/home/kanika/wikiTraining/CorrectionCheck");

							if (OBJ == null) {
								OBJ = new HashMap<String, HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>>();
							}
							HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>> temp = OBJ
									.get(filename);
							if (temp == null) {
								temp = new HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>();
								OBJ.put(filename, temp);
							}
							temp.put(var, newList);
							Serializer
									.encode(OBJ,
											"/home/kanika/wikiTraining/CorrectionCheck");
						}
					}
				}
				ArrayList<NodePotentials> var = currentFileMap.get(npList
						.get(0).offset);
				currentFileMap.put(npList.get(0).offset, npList);

				HashMap<String, HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>> OBJ = (HashMap<String, HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>>) Serializer
						.decode("/home/kanika/wikiTraining/CorrectionCheck");

				if (OBJ == null) {
					OBJ = new HashMap<String, HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>>();
				}
				HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>> temp = OBJ
						.get(filename);
				if (temp == null) {
					temp = new HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>();
					OBJ.put(filename, temp);
				}
				temp.put(var, npList);
				Serializer.encode(OBJ,
						"/home/kanika/wikiTraining/CorrectionCheck");

			}
			if (!WikiAnnotationInterface.correcting)
				currentFileMap.put(npList.get(0).offset, npList);
			// System.out.println("ut into hashmap");

			persist(currentFileMap, "/home/kanika/wikiTraining/NewObjects/",
					docName);

		} else if (doneFile != null) {

			/*
			 * when done
			 */

			BufferedWriter br = new BufferedWriter(new FileWriter(
					"/home/kanika/wikiTraining/logs/AnnotatedFiles.txt", true));
			br.write(docNameDone);
			br.write("\n");
			br.close();

			Check.onGoingFiles.remove(docNameDone);
			Check.allFilesInTrainDir.remove(docNameDone);
			// System.out.println(currentFileMap.keySet());
			Iterator iter = currentFileMap.keySet().iterator();
			HashMap<Integer, ArrayList<NodePotentials>> extra = new HashMap<Integer, ArrayList<NodePotentials>>();
			while (iter.hasNext()) {

				Integer key = (Integer) iter.next();
				// System.out.println(currentFileMap.get(key).size());
				String ment = currentFileMap.get(key).get(0).mention;
				HashSet<Integer> mentOff = new HashSet<Integer>();
				int off = 0;
				while (off != -1) {
					off = wi.doc.docText.indexOf(ment, off);
					if (off != key && !mentOff.contains(off) && off != -1) {
						mentOff.add(off);
						off = off + ment.length();
					}
					if (off == key) {
						off = off + ment.length();
					}
				}

				Iterator itr = mentOff.iterator();
				while (itr.hasNext()) {
					Integer o = (Integer) itr.next();
					// System.out.println("oo "+o);
					if (!currentFileMap.containsKey(o)) {
						ArrayList<NodePotentials> tem = new ArrayList<NodePotentials>();
						for (int j = 0; j < currentFileMap.get(key).size(); j++) {
							NodePotentials n = new NodePotentials();
							n.offset = o;
							n.interval = Interval.valueOf(o, o + ment.length());
							n.mention = ment;
							n.name = currentFileMap.get(key).get(j).name;
							tem.add(n);
						}
						extra.put(o, tem);
					}
				}
			}
			currentFileMap.putAll(extra);
			// System.out.println(currentFileMap.keySet());
			System.out.println("writing xml for " + docNameDone);
			BufferedWriter outXml = new BufferedWriter(new FileWriter(xmlDir
					+ docNameDone + ".xml"));
			outXml.write("<iitb.wikipedia.entityAnnotations>");
			outXml.write("\n");
			for (int key : currentFileMap.keySet()) {
				for (int i = 0; i < currentFileMap.get(key).size(); i++) {
					outXml.write("<annotation>");
					outXml.write("\n");
					outXml.write("<docName>"
							+ docNameDone.replaceAll("&", "&amp;")
							+ "</docName>");

					outXml.write("\n");
					outXml.write("<wikiName>"
							+ currentFileMap.get(key).get(i).name.replaceAll(
									"&", "&amp;") + "</wikiName>");
					outXml.write("\n");
					// System.out.println(key);
					outXml.write("<offset>" + key + "</offset>");
					outXml.write("\n");
					outXml.write("<mention>"
							+ currentFileMap.get(key).get(i).mention
									.replaceAll("&", "&amp;") + "</mention>");
					outXml.write("\n");
					outXml.write("<length>"
							+ currentFileMap.get(key).get(i).mention.length()
							+ "</length>");
					outXml.write("\n");
					outXml.write("</annotation>");
					outXml.write("\n");
				}
			}
			outXml.write("</iitb.wikipedia.entityAnnotations>");
			outXml.close();

			// File f = new File(docNameDone);
			// boolean success = f.delete();
			// if(success==true){
			// System.out.println(docNameDone +
			// "  file HashMap object deleted after creating xml");
			// }
		}

		/*
		 * when next doc
		 */
	}

	private void persist(
			HashMap<Integer, ArrayList<NodePotentials>> currentFile,
			String path, String filename) {
		System.out.println("Writing hashMap for file :: " + filename
				+ " current size of map :: " + currentFile.keySet().size());
		// for(int i:currentFile.keySet()){
		// System.out.println(currentFile.get(i).mention+" "+currentFile.get(i).name);
		// }
		Serializer.encode(currentFile, path, filename);
	}

	private HashMap<Integer, ArrayList<NodePotentials>> getHashMap(String path,
			String filename, WikiAnnotationInterface wi) {
		System.out.println("Retrieving hashMap for file :: " + filename);
		HashMap<Integer, ArrayList<NodePotentials>> temp = null;
		if (Check.onGoingFiles.contains(filename)) {
			HashMap<Integer, ArrayList<NodePotentials>> hashMap = (HashMap<Integer, ArrayList<NodePotentials>>) Serializer
					.decode(path, filename);
			temp = hashMap;
			// for(int key:temp.keySet()){
			// for(int i=0;i<temp.get(key).size();i++)
			// System.out.println("mention :: "+temp.get(key).get(i).mention+" entity :: "+temp.get(key).get(i).name);
			// }
		}
		if (temp == null) {
			if (WikiAnnotationInterface.correcting) {
				System.out
						.println("getting the hashMap from manual annotator ground truth");
				HashMap<Integer, ArrayList<NodePotentials>> m2eManual = new HashMap<Integer, ArrayList<NodePotentials>>();
				for (int i = 0; i < wi.manualDoc.offset.size(); i++) {
					ArrayList<NodePotentials> np = new ArrayList<NodePotentials>();
					NodePotentials n = new NodePotentials();
					n.interval = Interval.valueOf((int) wi.manualDoc.offset
							.get(i), wi.manualDoc.offset.get(i)
							+ wi.manualDoc.entity.get(i).mention.length() - 1);
					n.name = wi.manualDoc.entity.get(i).groundEntity;
					n.mention = wi.manualDoc.entity.get(i).mention;
					n.label = 1;
					n.isPresentInWiki = true;
					np.add(n);
					m2eManual.put(wi.manualDoc.offset.get(i), np);
				}
				temp = m2eManual;
				Check.onGoingFiles.add(filename);
			} else {
				System.out.println("Creating new hashMap for file ::  "
						+ filename);
				temp = new HashMap<Integer, ArrayList<NodePotentials>>();
				Check.onGoingFiles.add(filename);
			}
		}

		return temp;
	}

}
