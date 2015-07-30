package util;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ParseXMLAQ extends DefaultHandler {

	private XMLTagInfo obj;
	private String temp;
	private String docName;
	private static HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap;
	private ArrayList<XMLTagInfo> objList;

	public HashMap<String, ArrayList<XMLTagInfo>> parseXMLAQ(
			String groundfilename) {

		if (fileGroundTruthMap == null)
			fileGroundTruthMap = new HashMap<String, ArrayList<XMLTagInfo>>();

		SAXParserFactory spfac = SAXParserFactory.newInstance();

		SAXParser sp;
		try {
			sp = spfac.newSAXParser();

			ParseXMLAQ handler = new ParseXMLAQ();
			sp.parse(groundfilename, handler);

			System.out.println(" in parseXMLAQ "
					+ fileGroundTruthMap.keySet().size());
		} catch (Exception e) {

			e.printStackTrace();
		}

		return fileGroundTruthMap;
	}

	public static void main(String[] args) throws Exception {

		ParseXMLAQ pm = new ParseXMLAQ();

		HashMap<String, ArrayList<XMLTagInfo>> info = pm
				.parseXMLAQ("/home/kanika/bat-env-0.1/benchmark/datasets/AQUAINT/Problems/APW19990526_0131.htm");

		for (String key : info.keySet()) {
			System.out
					.println("------------------------------------------------"
							+ key
							+ "-----------------------------------------------------");
			for (int i = 0; i < info.get(key).size(); i++) {
				System.out.println(info.get(key).get(i).mention + " "
						+ info.get(key).get(i).offset + " "
						+ info.get(key).get(i).wikiEntity + " "
						+ info.get(key).get(i).length);
			}
		}
	}

	/*
	 * When the parser encounters plain text (not XML elements), it calls(this
	 * method, which accumulates them in a string buffer
	 */
	public void characters(char[] buffer, int start, int length) {
		temp = new String(buffer, start, length);
		// System.out.println(temp.trim());
	}

	/*
	 * Every time the parser encounters the beginning of a new element, it calls
	 * this method, which resets the string buffer
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		temp = "";
		if (qName.equalsIgnoreCase("ReferenceProblem")) {
			objList = new ArrayList<XMLTagInfo>();
		}
		if (qName.equalsIgnoreCase("ReferenceInstance")) {
			obj = new XMLTagInfo();
		}
	}

	/*
	 * When the parser encounters the end of an element, it calls this method
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equalsIgnoreCase("ReferenceProblem")) {
			fileGroundTruthMap.put(docName, objList);
			System.out.println("in end element " + fileGroundTruthMap.size());
		} else if (qName.equalsIgnoreCase("ReferenceFileName")) {
			docName = temp.trim();
			System.out.println(docName);

		} else if (qName.equalsIgnoreCase("ReferenceInstance")) {
			objList.add(obj);
		} else if (qName.equalsIgnoreCase("SurfaceForm")) {
			// System.out.println(temp.trim());
			obj.mention = temp.trim();
		} else if (qName.equalsIgnoreCase("Offset")) {
			obj.offset = (Integer.parseInt(temp.trim()));
			// obj.offset = temp;
		} else if (qName.equalsIgnoreCase("Length")) {
			obj.length = (Integer.parseInt(temp.trim()));
			// obj.length = temp;
		} else if (qName.equalsIgnoreCase("Annotation")) {
			String link = temp.trim();
			// System.out.println(link);
			String[] linksplit = link.split("/wiki/");
			if (linksplit.length > 1)
				obj.wikiEntity = linksplit[1].replaceAll("_", " ");

		}

	}

	private void readList(HashMap<String, ArrayList<XMLTagInfo>> info) {
		System.out.println("No of files '" + info.keySet().size() + "'.");

		for (String key : info.keySet()) {
			System.out
					.println("------------------------------------------------"
							+ key
							+ "-----------------------------------------------------");
			for (int i = 0; i < info.get(key).size(); i++) {
				System.out.println(info.get(key).get(i).mention + " "
						+ info.get(key).get(i).offset + " "
						+ info.get(key).get(i).wikiEntity + " "
						+ info.get(key).get(i).length);
			}
		}

	}
}
