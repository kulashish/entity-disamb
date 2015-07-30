package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class KDDGroundCorrector {

	private Map<String, ArrayList<XMLTagInfo>> globalXMLMap;
	private String docsPath;
	private String xmlsPathSource;
	private String xmlsPathTarget;

	public KDDGroundCorrector(String singleXMLPath, String xmlFolderSource,
			String xmlFolderTarget, String docFolder) throws Exception {
		docsPath = docFolder;
		xmlsPathSource = xmlFolderSource;
		xmlsPathTarget = xmlFolderTarget;
		globalXMLMap = new ParseXML().parseXML(singleXMLPath);
	}

	public void correctAll() throws Exception {
		for (File f : new File(docsPath).listFiles()) {
			correct(f);
		}
	}

	private void correct(File f) throws Exception {
		String docName = f.getName();
		String docString = readDocument(f);
		String xmlPath = xmlsPathSource + docName + ".xml";
		String userName = getUsername(xmlPath);
		Map<String, ArrayList<XMLTagInfo>> xmlMap = new ParseXML()
				.parseXML(xmlPath);
		ArrayList<XMLTagInfo> xmlTags = xmlMap.get("KDD_" + docName);
		ArrayList<XMLTagInfo> origXMLTags = globalXMLMap.get(docName);
		System.out.println("Number of tags before union:" + xmlTags.size());
		for (XMLTagInfo tagInfo : origXMLTags) {
			if (!isNA(tagInfo))
				continue;
			tagInfo.wikiEntity = "NA";
			tagInfo.mention = docString.substring(tagInfo.offset,
					tagInfo.offset + tagInfo.length);
			xmlTags.add(tagInfo);
		}
		System.out.println("Number of tags after union:" + xmlTags.size());
		Collections.sort(xmlTags, new Comparator<XMLTagInfo>() {
			public int compare(XMLTagInfo a, XMLTagInfo b) {
				return Integer.signum(a.offset - b.offset);
			}
		});
		serializeXML(userName, xmlTags, docName);
	}

	private void serializeXML(String userName, ArrayList<XMLTagInfo> xmlTags,
			String docName) throws IOException {
		String targetXMLPath = xmlsPathTarget + docName + ".xml";
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(targetXMLPath));
		writer.append("<iitb.wikipedia.entityAnnotations>");
		writer.newLine();
		writer.append("<userName>" + userName + "</userName>");
		writer.newLine();
		for (XMLTagInfo tag : xmlTags) {
			writer.append("<annotation>");
			writer.newLine();
			writer.append("<docName>" + docName + "</docName>");
			writer.newLine();
			writer.append("<wikiName>" + tag.wikiEntity + "</wikiName>");
			writer.newLine();
			writer.append("<offset>" + tag.offset + "</offset>");
			writer.newLine();
			writer.append("<mention>" + tag.mention + "</mention>");
			writer.newLine();
			writer.append("<length>" + tag.length + "</length>");
			writer.newLine();
			writer.append("</annotation>");
			writer.newLine();
		}
		writer.append("</iitb.wikipedia.entityAnnotations>");
		writer.flush();
		writer.close();
	}

	private String getUsername(String xmlPath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(xmlPath));
		String line = null;
		String username = null;
		while ((line = br.readLine()) != null
				&& line.indexOf("<userName>") == -1)
			;
		if (null != line)
			username = line.substring(line.indexOf("<userName>") + 10,
					line.indexOf("</userName>"));
		return username;
	}

	private String readDocument(File doc) {
		StringBuilder s = new StringBuilder();
		BufferedReader br = null;
		int r;
		try {
			br = new BufferedReader(new FileReader(doc));
			while ((r = br.read()) != -1) {
				char ch = (char) r;
				s.append(ch);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s.toString();
	}

	private boolean isNA(XMLTagInfo tagInfo) {
		return null == tagInfo.wikiEntity;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		KDDGroundCorrector corrector = null;
		try {
			// singleXML, XML folder source, XML folder target, Document folder
			corrector = new KDDGroundCorrector(args[0], args[1], args[2],
					args[3]);
			corrector.correctAll();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
