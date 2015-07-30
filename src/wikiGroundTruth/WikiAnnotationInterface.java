package wikiGroundTruth;

import it.unimi.dsi.util.Interval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spotting.CollectiveTraining;
import spotting.ExtractKeywordsGroundTruth;
import spotting.FeatureExtractor;
import spotting.KeywordsGroundTruth;
import spotting.LuceneIndexWrapper;
import spotting.NodePotentials;
import spotting.NodePotentialsSet;
import spotting.TrainingData;
import util.ParseXML;
import util.XMLTagInfo;

public class WikiAnnotationInterface extends HttpServlet {
	public String wikiDocDir = "/home/kanika/wikiTraining/CorrectedDocs/";
	public String trainDir = "/home/kanika/wikiTraining/CorrectedDocs/";
	public String groundDir = "/home/kanika/wikiTraining/ground";
	public String groundFilename = "/home/kanika/wikiTraining/ground/wikipediaGroundtruth.xml";
	public int maxNodes = 50; // hardcoded max number of entities to be extracted from
	// lucene
	public int consolidationSize = 8;
	public int csize = 40;
	public Document doc;
	private ArrayList<Annotation> spots = new ArrayList<Annotation>();
	private HashSet<String> addedMentions = new HashSet<String>();
	public HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapManual = new HashMap<String, ArrayList<XMLTagInfo>>();
	public String groundFilenameManual;
	static boolean correcting = false;
	public Document manualDoc;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		String filename = null;
		Iterator<String> it = Check.allFilesInTrainDir.iterator();
		while (it.hasNext()) {
			String filen = it.next().toString();
			if (Check.onGoingFiles.contains(filen)) {
				continue;
			} else {
				filename = filen;
				Check.onGoingFiles.add(filename);
				break;
			}
		}
		if (filename == null) {
			BufferedWriter br = new BufferedWriter(new FileWriter(
					"/home/kanika/wikiTraining/logs/onGoingFiles.txt"));
			for (String fil : Check.onGoingFiles) {
				br.write(fil);
				br.write("\n");
			}
			br.close();
			out
					.println("all files have been annotated in the present directory");
			out.println("restart the server with some other directory");
		} else {
			System.out.println("File found ::" + filename);
			spots = new ArrayList<Annotation>();
			// if (correcting) {
			// spots = getDataCorrecting(filename);
			// } else {
			spots = getDataNew(filename);
			// }
			// for(int i=0;i<spots.size();i++){
			// if(spots.get(i).isPresentInWiki)
			// System.out.println(spots.get(i).wikiEntity);
			// }
			System.out.println("got data");

			out.println("<html>");
			out
					.print("<span id='doc' name='doc' style='color: black; font-size: 14pt;'><a href=\"http://en.wikipedia.org"
							+ "/wiki/"
							+ filename
							+ "\" title=\"wikipedia link\" target=\"_blank\">"
							+ filename + "</a></span>");
			out.println("<script>");

			BufferedReader br = new BufferedReader(new FileReader(
					"src/wikiGroundTruth/getQueryString"));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

			out.println(sb.toString());

			br = new BufferedReader(new FileReader(
					"src/wikiGroundTruth/ajaxCall"));
			sb.delete(0, sb.length());
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

			out.println("function hi(){}");
			out.println("function reloadPage(){location.reload(true)}");
			out
					.println("function ajaxCall(off){"
							+
							// "alert(off);"+
							"var str=getquerystring('myForm');"
							+ "var xmlhttp;"
							+ "if (window.XMLHttpRequest){xmlhttp=new XMLHttpRequest();}else{xmlhttp=new ActiveXObject(\"Microsoft.XMLHTTP\");"
							+ "};"
							+ "xmlhttp.onreadystatechange=function(){if (xmlhttp.readyState==4 && xmlhttp.status==200)"
							+ "{document.getElementById(\"form\").innerHTML='Annotation Recorded';"
							+ "var oldHTML = document.getElementById(off).innerHTML;"
							+
							// "alert(oldHTML);"+
							// "alert('hi');"+
							"var newHTML = \"<span style='color:red'>\" + oldHTML + \"</span>\";"
							+ "document.getElementById(off).innerHTML = newHTML;"
							+ "}" + "};"
							+ "xmlhttp.open(\"GET\",\"check1?\"+str,true);"
							+ "xmlhttp.send();}");

			out
					.println("function ajaxCall1(){"
							+ "var str=getquerystring('myForm1');"
							+ "var xmlhttp;"
							+ "if (window.XMLHttpRequest){xmlhttp=new XMLHttpRequest();}else{xmlhttp=new ActiveXObject(\"Microsoft.XMLHTTP\");"
							+ "};"
							+ "xmlhttp.onreadystatechange=function(){if (xmlhttp.readyState==4 && xmlhttp.status==200)"
							+ "{document.getElementById(\"form\").innerHTML='Document Complete. Click On Next Document';"
							+ "};" + "};"
							+ "xmlhttp.open(\"GET\",\"check1?\"+str,true);"
							+ "xmlhttp.send();}");

			out
					.println("function change(mention,fileName,offset,arr,score,label){mention=mention.replace(/ /g,\"&#32\");"
							+ "var a=\"<form method='post' name='myForm' onsubmit='ajaxCall(\"+offset+\");return false;'><table>\";"
							+ "for (var i = 0; i < arr.length; i++) {"
							+ "arr[i]=arr[i].replace(/'/g,\"&#39\");"
							+ "a=a+\"<tr><td><input type='checkbox' name='entities' value='\"+arr[i]+\"'/><a href='http://en.wikipedia.org/wiki/\"+arr[i]+\"' target='_blank'>\"+arr[i]+\"[ \"+label[i]+\"]</a></td></tr>\";}"
							+ "a=a+\"<tr><td>Other Entity 1: <input type='text' name='OtherEntity'></td></tr>\";"
							+ "a=a+\"<tr><td>Other Entity 2: <input type='text' name='SecondEntity'></td></tr>\";"
							+ "a=a+\"<tr><td>Other Mention: <input type='text' name='OtherMention'></td></tr>\";"
							+ "a=a+\"<tr><td><input type='submit' name='annotate' value='annotate'/></td></tr>\";"
							+ "a=a+\"</table>"
							+ "<input type='hidden' name='hiddenOffset' value=\"+offset+\">"
							+ "<input type='hidden' name='hiddenfileName' value=\"+fileName+\">"
							+ "<input type='hidden' name='hiddenMention' value=\"+mention+\"></form>\";"
							+ "document.getElementById(\"form\").innerHTML=a;"
							+ "}");

			out.println("</script>");

			out.println("<body>");
			out.println("<table width=100% border=1><tr>");
			out.println("<td width=\"70%\">");
			out.println("<div id='maintext'>");

			int index = 0;
			int key = -1;
			String curr_file = doc.docTitle;
			for (int j = 0; j < doc.docText.length();) {
				if (index < spots.size())
					key = spots.get(index).interval.left;
				else
					key = doc.docText.length();
				if (j < key) {
					while (j < key) {
						out.print(doc.docText.charAt(j));
						j++;
					}
				} else if (j == key) {
					String mention = spots.get(index).mention;
					if (addedMentions.contains(mention)) {
						out.print(mention);
						j += mention.length();
						index++;
					} else {
						addedMentions.add(mention);

						int offset = spots.get(index).interval.left;
						StringBuilder sb1 = new StringBuilder();
						StringBuilder sb2 = new StringBuilder();
						StringBuilder sb3 = new StringBuilder();
						String ent = spots.get(index).candidateEntities.get(0).name;
						sb1.append("[");
						sb2.append("[");
						sb3.append("[");
						ArrayList<NodePotentials> npList = spots.get(index).candidateEntities;
						Collections.sort(npList,
								new Comparator<NodePotentials>() {
									@Override
									public int compare(NodePotentials o1,
											NodePotentials o2) {
										return Double.compare(o2.label,
												o1.label);
									}
								});

						HashSet<String> addedEnt = new HashSet<String>();
						for (NodePotentials obj : npList) {
							if (!addedEnt.contains(obj.name)) {
								obj.name = obj.name.replace("'", "&#39;");
								sb1.append("\"" + obj.name + "\",");
								sb3.append("\"" + obj.label + "\",");
								sb2.append("\""
										+ new DecimalFormat("0.####")
												.format(obj.logistic_score)
										+ "\",");
								addedEnt.add(obj.name);
							}
						}

						sb1.append("]");
						sb3.append("]");
						sb2.append("]");
						// color:#0000FF
						if (spots.get(index).isPresentInWiki) {
							// System.out.println("present wiki "+ ent);
							out
									.println("<span id='subtext' style=\"background-color: yellow; font-size: 14pt\"'><a href=\"http://en.wikipedia.org/wiki/"
											+ ent
											+ "\" title=\"wikipedia link\" target=\"_blank\">"
											+ doc.docText.substring(j, j
													+ mention.length())
											+ "</a></span>");
						} else {
							out
									.println("<span id='"
											+ j
											+ "' style=\"color: blue; font-size: 14pt;\" onClick='change(\""
											+ doc.docText.substring(j, j
													+ mention.length())
											+ "\",\""
											+ curr_file
											+ "\",\""
											+ offset
											+ "\","
											+ sb1.toString()
											+ ","
											+ sb2.toString()
											+ ","
											+ sb3.toString()
											+ ");'>"
											+ "[ "
											+ doc.docText.substring(j, j
													+ mention.length()) + " ]"
											+ "</span>");
						}
						j += mention.length();
						index++;
					}
				}
			}
			System.out.println("out of for loop");
			out.println("</div></td>");
			out
					.println("<td valign='top'><div id=\"form\" width=\"30%\">blank text</div></td>");
			out.println("</tr>");
			out.println("</table>");
			//			
			out
					.println("<form method='post' name='myForm1' onsubmit='ajaxCall1();return false;'>"
							+ "<input type='submit' name='done' value='Done'/><input type='hidden' name='hiddenfileNameDone' value=\""
							+ filename + "\"></form>");
			out
					.println("<input type='button' value='Next Document' onclick='reloadPage()'>");
			out.println("<div id='MyResult'></div>");
			out.println("</body>");
			out.println("</html>");
			System.out.println("rendered page");
		}
	}

	public ArrayList<Annotation> getSpots() {
		return spots;
	}

	public HashSet<String> getAddedMentions() {
		return addedMentions;
	}

	public Document getDocument() {
		return doc;
	}

	public ArrayList<Annotation> getDataCorrecting(String filename) {
		ArrayList<Annotation> wikiAndSpotter = new ArrayList<Annotation>();
		try {
			BufferedReader br;
			String str;
			int r;
			StringBuilder s = new StringBuilder();
			try {
				int line = 0;
				br = new BufferedReader(new FileReader(wikiDocDir + filename));
				while ((r = br.read()) != -1) {
					char ch = (char) r;
					s.append(ch);
				}
				br.close();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			str = s.toString();
			str = str.substring(0, str.lastIndexOf(" "));
			ArrayList<XMLTagInfo> infoList = new ArrayList<XMLTagInfo>();
			infoList = Check.fileGroundTruthMap.get(filename);
			doc = new Document();
			doc.docText = str;
			doc.docTitle = filename;
			for (XMLTagInfo in : infoList) {
				if (in.offset >= doc.docText.length() - 1
						|| in.offset + in.length >= doc.docText.length()) {
					continue;
				}
				doc.offset.add(in.offset);
				WikiMention wm = new WikiMention();
				wm.mention = in.mention;
				wm.label = 1;
				wm.groundEntity = in.wikiEntity;
				// System.out.println(wm.groundEntity);
				doc.entity.add(wm);
			}

			groundFilenameManual = "/home/kanika/wikiTraining/CorrectedXmls/"
					+ filename + ".xml";
			ParseXML pm = new ParseXML();
			fileGroundTruthMapManual = pm.parseXML(groundFilenameManual);

			manualDoc = new Document();
			manualDoc.docText = str;
			// System.out.println(doc.docText.length());
			ArrayList<String> ground = new ArrayList<String>();
			manualDoc.docTitle = filename;
			for (XMLTagInfo in : fileGroundTruthMapManual.get(filename)) {
				if (in.offset >= manualDoc.docText.length() - 1
						|| in.offset + in.length >= manualDoc.docText.length()) {
					continue;
				}
				manualDoc.offset.add(in.offset);
				WikiMention wm = new WikiMention();
				wm.mention = in.mention;
				wm.label = 1;
				wm.groundEntity = in.wikiEntity;
				ground.add(in.wikiEntity);
				// System.out.println(wm.groundEntity);
				manualDoc.entity.add(wm);
			}

			Serializer.encode(doc, "/home/kanika/wikiTraining/NewObjects/",
					filename + "wiki");

			Serializer.encode(manualDoc,
					"/home/kanika/wikiTraining/NewObjects/", filename
							+ "manual");

			System.out.println("Doc and ManualDoc written");

			groundFilenameManual = "/home/kanika/wikiTraining/CorrectedXmls/"
					+ filename + ".xml";

			HashMap<Integer, ArrayList<NodePotentials>> m2eManual = new HashMap<Integer, ArrayList<NodePotentials>>();
			HashMap<Integer, ArrayList<NodePotentials>> m2eWiki = new HashMap<Integer, ArrayList<NodePotentials>>();

			for (int i = 0; i < manualDoc.offset.size(); i++) {
				ArrayList<NodePotentials> np = new ArrayList<NodePotentials>();
				NodePotentials n = new NodePotentials();
				n.interval = Interval.valueOf((int) manualDoc.offset.get(i),
						manualDoc.offset.get(i)
								+ manualDoc.entity.get(i).mention.length() - 1);
				n.name = manualDoc.entity.get(i).groundEntity;
				n.mention = manualDoc.entity.get(i).mention;
				n.label = 1;
				n.isPresentInWiki = false;
				np.add(n);
				m2eManual.put(manualDoc.offset.get(i), np);
			}

			for (int i = 0; i < doc.offset.size(); i++) {
				ArrayList<NodePotentials> np = new ArrayList<NodePotentials>();
				NodePotentials n = new NodePotentials();
				// n.interval =
				// Interval.valueOf(doc.offset.get(i),doc.offset.get(i)+doc.entity.get(i).mention.length()-1);
				n.interval = Interval.valueOf((int) doc.offset.get(i),
						doc.offset.get(i) + doc.entity.get(i).mention.length()
								- 1);
				n.name = doc.entity.get(i).groundEntity;
				n.mention = doc.entity.get(i).mention;
				n.label = 1;
				n.isPresentInWiki = true;
				np.add(n);
				// System.out.println("---------------------------------------------------------");
				// System.out.println("found in WikiGroundTruth corresponding to mention "+n.mention+"   "+doc.offset.get(i));
				m2eWiki.put(doc.offset.get(i), np);
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

			for (int off : m2eManual.keySet()) {
				Annotation an = new Annotation();
				an.interval = m2eManual.get(off).get(0).interval;
				an.mention = m2eManual.get(off).get(0).mention;
				an.candidateEntities = m2eManual.get(off);
				an.wikiEntity = m2eManual.get(off).get(0).name;
				// System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&& "+an.wikiEntity);
				an.isPresentInWiki = false;
				wikiAndSpotter.add(an);
			}

			ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
			KeywordsGroundTruth kw;

			kw = kw_extractor.extractFromString(manualDoc.docText,
					new ArrayList<String>(), csize);
			LuceneIndexWrapper luceneIndex = new LuceneIndexWrapper(
					"/mnt/bag1/querysystem/indexing/CompleteIndex",
					"/home/pararth/index/RedirectIndex",
					"/home/ashish/wikiindex/index1",
					"/home/pararth/index/DisambIndex",
					"/mnt/bag1/ashish/anchoronly_index");
			TrainingData result = new TrainingData();
			result.nodes = luceneIndex.extractNodesNewConsolidation(kw,
					maxNodes, consolidationSize, filename);

			if (result.nodes == null) {
				System.out
						.println("CollectiveTrainingTemp.spotterNew::result NodePotentialSet::null");
			}
			result.groundtruth = kw;

			HashMap<Integer, ArrayList<NodePotentials>> m2eSpotter = new HashMap<Integer, ArrayList<NodePotentials>>();

			for (NodePotentials np : result.nodes.potentials_set) {
				String mentionName = np.mention.split("_")[0];
				int off = np.interval.left;
				if (m2eSpotter.get(off) == null)
					m2eSpotter.put(off, new ArrayList<NodePotentials>());

				m2eSpotter.get(off).add(np);
			}

			HashSet<Integer> addedoff = new HashSet<Integer>();
			for (int off : m2eSpotter.keySet()) {
				for (int off1 : m2eManual.keySet()) {
					if (off1 == off) {
						if (m2eManual.get(off1).get(0).mention
								.equals(m2eSpotter.get(off).get(0).mention
										.split("_")[0])) {
							m2eManual.get(off1).addAll(m2eSpotter.get(off));
							addedoff.add(off);
						}
					}
				}
			}
			System.out.println(addedoff.size() + " "
					+ addedoff.toArray().toString());

			System.out.println(m2eSpotter.size() + "     "
					+ m2eSpotter.keySet());
			Iterator<Entry<Integer, ArrayList<NodePotentials>>> itr = m2eSpotter
					.entrySet().iterator();

			while (itr.hasNext()) {
				int o = itr.next().getKey();
				if (addedoff.contains(o))
					itr.remove();
			}

			System.out
					.println(m2eSpotter.size() + "    " + m2eSpotter.keySet());
			for (int off : m2eSpotter.keySet()) {
				Annotation an = new Annotation();
				an.interval = m2eSpotter.get(off).get(0).interval;
				an.mention = m2eSpotter.get(off).get(0).mention.split("_")[0];
				an.candidateEntities = m2eSpotter.get(off);
				an.wikiEntity = m2eSpotter.get(off).get(0).name;
				// System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&& "+an.wikiEntity);
				an.isPresentInWiki = false;
				wikiAndSpotter.add(an);
			}

			Iterator<Annotation> it = wikiAndSpotter.iterator();
			while (it.hasNext()) {
				Annotation a = it.next();
				if (a.interval.left >= doc.docText.length() - 1
						|| a.interval.right >= doc.docText.length()) {
					it.remove();
				}
			}

			Collections.sort(wikiAndSpotter);
			removeContainedSpotsWithinWikiSpots(wikiAndSpotter);
			removeNonMaximalAnnotationofSpotter(wikiAndSpotter);

			for (int i = 0; i < wikiAndSpotter.size(); i++) {
				if (wikiAndSpotter.get(i).isPresentInWiki == false) {
					NodePotentials n = new NodePotentials();
					n.context_score_frequent = 0;
					n.inlink_count = 0;
					n.outlink_count = 0;
					n.interval = wikiAndSpotter.get(i).candidateEntities.get(0).interval;
					n.mention = wikiAndSpotter.get(i).candidateEntities.get(0).mention;
					n.name = "NA";
					wikiAndSpotter.get(i).candidateEntities.add(n);
				}
			}

			System.out.println("data computed");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return wikiAndSpotter;
	}

	public ArrayList<Annotation> getDataNew(String filename) {
		ArrayList<Annotation> wikiAndSpotter = new ArrayList<Annotation>();
		CollectiveTraining ct1 = new CollectiveTraining(trainDir,
				groundFilename, "", filename,"");
		ct1.spotterNew();
		TrainingData trainData = new TrainingData();
		trainData = ct1.getData();
		NodePotentialsSet nps = new NodePotentialsSet();
		nps = trainData.nodes;

		ArrayList<XMLTagInfo> infoList = new ArrayList<XMLTagInfo>();
		infoList = Check.fileGroundTruthMap.get(filename);

		doc = new Document();
		doc.docText = trainData.groundtruth.getDocumentText();
		// System.out.println(doc.docText.length());
		doc.docTitle = filename;
		for (XMLTagInfo in : infoList) {
			if (in.offset >= doc.docText.length() - 1
					|| in.offset + in.length >= doc.docText.length()) {
				continue;
			}
			doc.offset.add(in.offset);
			WikiMention wm = new WikiMention();
			wm.mention = in.mention;
			wm.label = 1;
			wm.groundEntity = in.wikiEntity;
			// System.out.println(wm.groundEntity);
			doc.entity.add(wm);
		}

		Serializer.encode(doc, "/home/kanika/wikiTraining/NewObjects/",
				filename + "wiki");

		HashMap<Integer, ArrayList<NodePotentials>> m2eSpotter = new HashMap<Integer, ArrayList<NodePotentials>>();
		HashMap<Integer, ArrayList<NodePotentials>> m2eWiki = new HashMap<Integer, ArrayList<NodePotentials>>();

		for (int i = 0; i < doc.offset.size(); i++) {
			ArrayList<NodePotentials> np = new ArrayList<NodePotentials>();
			NodePotentials n = new NodePotentials();
			// n.interval =
			//
			Interval.valueOf(doc.offset.get(i), doc.offset.get(i)
					+ doc.entity.get(i).mention.length() - 1);
			n.interval = Interval.valueOf((int) doc.offset.get(i), doc.offset
					.get(i)
					+ doc.entity.get(i).mention.length() - 1);
			n.name = doc.entity.get(i).groundEntity;
			n.mention = doc.entity.get(i).mention;
			n.label = 1;
			n.isPresentInWiki = false;
			np.add(n);
			m2eWiki.put(doc.offset.get(i), np);
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
			if (m2eSpotter.containsKey(Integer
					.parseInt(np.mention.split("_")[1]))) {
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

		Iterator<Annotation> it = wikiAndSpotter.iterator();
		while (it.hasNext()) {
			Annotation a = it.next();
			if (a.interval.left >= doc.docText.length() - 1
					|| a.interval.right >= doc.docText.length()) {
				it.remove();
			}
		}

		// for (int i = 0; i < wikiAndSpotter.size(); i++) {
		// System.out.println(wikiAndSpotter.get(i).interval.left + ","
		// + wikiAndSpotter.get(i).interval.right + "," + ","
		// + wikiAndSpotter.get(i).mention.length() + ","
		// + wikiAndSpotter.get(i).mention + ","
		// + wikiAndSpotter.get(i).isPresentInWiki);
		// }

		// System.out
		// .println("----------------------------------------------------------------------------------------------------------");
		Collections.sort(wikiAndSpotter);
		removeContainedSpotsWithinWikiSpots(wikiAndSpotter);
		removeNonMaximalAnnotationofSpotter(wikiAndSpotter);

		for (int i = 0; i < wikiAndSpotter.size(); i++) {
			System.out.println(wikiAndSpotter.get(i).interval.left
					+ ","
					+ wikiAndSpotter.get(i).interval.right
					+ ","
					+ (wikiAndSpotter.get(i).mention.length() + wikiAndSpotter
							.get(i).interval.left) + ","
					+ wikiAndSpotter.get(i).mention + ","
					+ wikiAndSpotter.get(i).isPresentInWiki);
		}

		// adding NA to candidate entities options
		for (int i = 0; i < wikiAndSpotter.size(); i++) {
			if (wikiAndSpotter.get(i).isPresentInWiki == false) {
				NodePotentials n = new NodePotentials();
				n.context_score_frequent = 0;
				n.inlink_count = 0;
				n.outlink_count = 0;
				n.interval = wikiAndSpotter.get(i).candidateEntities.get(0).interval;
				n.mention = wikiAndSpotter.get(i).candidateEntities.get(0).mention;
				n.name = "NA";
				wikiAndSpotter.get(i).candidateEntities.add(n);
			}
		}
		System.out.println("data computed");
		return wikiAndSpotter;
	}

	private ArrayList<Annotation> getData(String filename) {
		ArrayList<Annotation> wikiAndSpotter = new ArrayList<Annotation>();
		try {
			BufferedReader br;
			String str;
			int r;
			StringBuilder s = new StringBuilder();
			try {
				int line = 0;
				br = new BufferedReader(new FileReader(wikiDocDir + filename));
				while ((r = br.read()) != -1) {
					char ch = (char) r;
					s.append(ch);
				}
				br.close();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			str = s.toString();
			str = str.substring(0, str.lastIndexOf(" "));
			ArrayList<XMLTagInfo> infoList = new ArrayList<XMLTagInfo>();
			infoList = Check.fileGroundTruthMap.get(filename);

			// System.out.println("fileGroundMap ::"+filename);
			// for(int i=0;i<infoList.size();i++){
			// System.out.println(infoList.get(i).wikiEntity);
			// }

			doc = new Document();
			doc.docText = str;
			// System.out.println(doc.docText.length());
			doc.docTitle = filename;
			for (XMLTagInfo in : infoList) {
				if (in.offset >= doc.docText.length() - 1
						|| in.offset + in.length >= doc.docText.length()) {
					continue;
				}
				doc.offset.add(in.offset);
				WikiMention wm = new WikiMention();
				wm.mention = in.mention;
				wm.label = 1;
				wm.groundEntity = in.wikiEntity;
				// System.out.println(wm.groundEntity);
				doc.entity.add(wm);
			}

			Serializer.encode(doc, "/home/kanika/wikiTraining/NewObjects/",
					filename + "wiki");

			TrainingData trainData = new TrainingData();
			ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
			KeywordsGroundTruth kw;
			kw = kw_extractor.extractFilewithGroundTruth(groundFilename,
					filename, trainDir, csize);
			FeatureExtractor ft_extractor = new FeatureExtractor();
			long start = System.currentTimeMillis();

			trainData = ft_extractor.extractFeatures(kw, maxNodes,
					consolidationSize);

			long elapsedTimeMillis = System.currentTimeMillis() - start;
			float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);
			System.out.println("For file " + kw.filename
					+ " process completed in  " + elapsedTimeMin + " mins");

			NodePotentialsSet nps = new NodePotentialsSet();
			nps = trainData.nodes;

			HashMap<Integer, ArrayList<NodePotentials>> m2eSpotter = new HashMap<Integer, ArrayList<NodePotentials>>();
			HashMap<Integer, ArrayList<NodePotentials>> m2eWiki = new HashMap<Integer, ArrayList<NodePotentials>>();

			for (int i = 0; i < doc.offset.size(); i++) {
				ArrayList<NodePotentials> np = new ArrayList<NodePotentials>();
				NodePotentials n = new NodePotentials();
				// n.interval =
				// Interval.valueOf(doc.offset.get(i),doc.offset.get(i)+doc.entity.get(i).mention.length()-1);
				n.interval = Interval.valueOf((int) doc.offset.get(i),
						doc.offset.get(i) + doc.entity.get(i).mention.length()
								- 1);
				n.name = doc.entity.get(i).groundEntity;
				n.mention = doc.entity.get(i).mention;
				n.label = 1;
				n.isPresentInWiki = true;
				np.add(n);
				// System.out.println("---------------------------------------------------------");
				// System.out.println("found in WikiGroundTruth corresponding to mention "+n.mention+"   "+doc.offset.get(i));
				m2eWiki.put(doc.offset.get(i), np);
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
				if (m2eSpotter.containsKey(Integer.parseInt(np.mention
						.split("_")[1]))) {
					// System.out.println("found in NodePotentials corresponding to mention "+np.mention.split("_")[0]+"   "+np.mention.split("_")[1]);
					m2eSpotter.get(Integer.parseInt(np.mention.split("_")[1]))
							.add(np);
				} else {
					m2eSpotter.put(Integer.parseInt(np.mention.split("_")[1]),
							new ArrayList<NodePotentials>());
					m2eSpotter.get(Integer.parseInt(np.mention.split("_")[1]))
							.add(np);
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

			Iterator<Annotation> it = wikiAndSpotter.iterator();
			while (it.hasNext()) {
				Annotation a = it.next();
				if (a.interval.left >= doc.docText.length() - 1
						|| a.interval.right >= doc.docText.length()) {
					it.remove();
				}
			}

			// for (int i = 0; i < wikiAndSpotter.size(); i++) {
			// System.out.println(wikiAndSpotter.get(i).interval.left + ","
			// + wikiAndSpotter.get(i).interval.right + "," + ","
			// + wikiAndSpotter.get(i).mention.length() + ","
			// + wikiAndSpotter.get(i).mention + ","
			// + wikiAndSpotter.get(i).isPresentInWiki);
			// }

			// System.out
			// .println("----------------------------------------------------------------------------------------------------------");
			Collections.sort(wikiAndSpotter);
			removeContainedSpotsWithinWikiSpots(wikiAndSpotter);
			removeNonMaximalAnnotationofSpotter(wikiAndSpotter);

			//
			// for (int i = 0; i < wikiAndSpotter.size(); i++) {
			// System.out
			// .println(wikiAndSpotter.get(i).interval.left
			// + ","
			// + wikiAndSpotter.get(i).interval.right
			// + ","
			// + (wikiAndSpotter.get(i).mention.length() + wikiAndSpotter
			// .get(i).interval.left) + ","
			// + wikiAndSpotter.get(i).mention + ","
			// + wikiAndSpotter.get(i).isPresentInWiki);
			// }

			// adding NA to candidate entities options
			for (int i = 0; i < wikiAndSpotter.size(); i++) {
				if (wikiAndSpotter.get(i).isPresentInWiki == false) {
					NodePotentials n = new NodePotentials();
					n.context_score_frequent = 0;
					n.inlink_count = 0;
					n.outlink_count = 0;
					n.interval = wikiAndSpotter.get(i).candidateEntities.get(0).interval;
					n.mention = wikiAndSpotter.get(i).candidateEntities.get(0).mention;
					n.name = "NA";
					wikiAndSpotter.get(i).candidateEntities.add(n);
				}
			}
			System.out.println("data computed");

		} catch (Exception e1) {

			e1.printStackTrace();
			System.out.println("abcd.getData()");
		}
		return wikiAndSpotter;
	}

	private void removeContainedSpotsWithinWikiSpots(
			ArrayList<Annotation> npList) {
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
							} else if (npList.get(j).isPresentInWiki) {
								importance[i] = 0;
								importance[j] = 1;
							} else {
								importance[i] = 1;
								importance[j] = 0;
							}

						} else {
							importance[j] = 0;
						}
						// else if(npList.get(j).isPresentInWiki){
						// importance[j]=1;
						// importance[i]=0;
						// }
						// else{
						// importance[j]=1;
						// importance[i]=1;
						// }
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

	private void removeNonMaximalAnnotationofSpotter(
			ArrayList<Annotation> npList) {
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
						} else if (npList.get(j).isPresentInWiki) {
							importance[j] = 1;
							importance[i] = 0;
						} else if (npList.get(j).interval.length() > npList
								.get(i).interval.length()) {
							importance[j] = 1;
							importance[i] = 0;
						} else {
							importance[i] = 1;
							importance[j] = 0;
						}
					}
				}
			}
		}
		/*
		 * Iterator<Annotation> iterator = npList.iterator(); int i=1;
		 * Annotation pres = iterator.hasNext() ? iterator.next() : null; while
		 * (iterator.hasNext()) { Annotation next = iterator.next(); if
		 * (nps.overlaps(pres.interval, next.interval)) { if
		 * (pres.isPresentInWiki==false && next.isPresentInWiki==true) {
		 * importance[i-1]=0; importance[i]=1; pres=next; i++; } else
		 * if(pres.isPresentInWiki==true && next.isPresentInWiki==false){
		 * importance[i-1]=1; importance[i]=0; pres=next; i++; } else{
		 * if(pres.interval.length()>next.interval.length()){ importance[i-1]=1;
		 * importance[i]=0;
		 * 
		 * } else{ importance[i]=1; importance[i-1]=0; }
		 * 
		 * pres=next; i++; } } else{ importance[i]=1; i++; pres=next; } }
		 */
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

	private static void display(
			TreeMap<Integer, ArrayList<NodePotentials>> tempMap) {
		for (Integer key : tempMap.keySet()) {
			int end = (key
					+ tempMap.get(key).get(0).mention.split("_")[0].length() - 1);
			System.out.println(key + "," + end + ","
					+ tempMap.get(key).get(0).isPresentInWiki + ","
					+ tempMap.get(key).get(0).mention.split("_")[0]);
		}
	}

}
