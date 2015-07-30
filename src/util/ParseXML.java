package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ParseXML {

	// public HashMap<String,ArrayList<XMLTagInfo>> fileGroundTruthMap = new
	// HashMap<String,ArrayList<XMLTagInfo>>();
	HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = new HashMap<String, ArrayList<XMLTagInfo>>();

	public HashMap<String, ArrayList<XMLTagInfo>> parseXML(String groundfilename)
			throws Exception {

		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {

				boolean bannotation = false;
				boolean bdocName = false;
				boolean bwikiName = false;
				boolean boffset = false;
				boolean blength = false;
				boolean bmention = false;

				String dname;
				XMLTagInfo obj;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {

					// System.out.println("Start Element :" + qName);
					// System.out.println("Attribute :" + attributes);

					if (qName.equalsIgnoreCase("annotation")) {
						bannotation = true;
						obj = new XMLTagInfo();

					}
					if (qName.equalsIgnoreCase("docName")) {
						bdocName = true;
					}

					if (qName.equalsIgnoreCase("wikiName")) {
						bwikiName = true;
					}

					if (qName.equalsIgnoreCase("offset")) {
						boffset = true;
					}

					if (qName.equalsIgnoreCase("length")) {
						blength = true;
					}
					if (qName.equalsIgnoreCase("mention")) {
						bmention = true;
					}

				}

				public void characters(char ch[], int start, int length)
						throws SAXException {

					if (bdocName) {
						dname = new String(ch, start, length);
						if (!fileGroundTruthMap.containsKey(dname)) {
							ArrayList<XMLTagInfo> li = new ArrayList<XMLTagInfo>();
							fileGroundTruthMap.put(dname, li);
						}

					}
					if (bwikiName) {
						obj.wikiEntity = new String(ch, start, length);
					}
					if (boffset)
						obj.offset = Integer.parseInt(new String(ch, start,
								length));

					if (blength)
						obj.length = Integer.parseInt(new String(ch, start,
								length));

					if (bmention)
						obj.mention = (new String(ch, start, length));

				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {

					if (qName.equalsIgnoreCase("annotation")) {
						bannotation = false;
						fileGroundTruthMap.get(dname).add(obj);
					}

					if (qName.equalsIgnoreCase("wikiName")) {
						bwikiName = false;
					}

					if (qName.equalsIgnoreCase("mention")) {
						bmention = false;
					}

					if (qName.equalsIgnoreCase("docName")) {
						bdocName = false;
					}
					if (qName.equalsIgnoreCase("offset")) {
						boffset = false;
					}
					if (qName.equalsIgnoreCase("length")) {
						blength = false;
					}

				}

			};
			File file = new File(groundfilename);
			InputStream inputStream = new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream, "UTF-8");

			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");
			saxParser.parse(groundfilename, handler);
			//
			// ArrayList<String> files = new ArrayList<String>();
			// File folder = new File("data/crawledDocs/");
			// File[] listOfFiles = folder.listFiles();
			// for (int i = 0; i < listOfFiles.length; i++)
			// if (listOfFiles[i].isFile()){
			// String filename = listOfFiles[i].getName();
			// System.out.println("File " + filename);
			// files.add(filename);
			// }
			// int c=0;
			// for(String key : fileGroundTruthMap.keySet()){
			// if(files.contains(key)){
			// c++;
			// System.out.println("\n********************key  "+
			// key+"   List*****************************\n");
			// for(int i = 0 ; i < fileGroundTruthMap.get(key).size() ; i++){
			// System.out.println(fileGroundTruthMap.get(key).get(i).wikiEntity+"\t"+fileGroundTruthMap.get(key).get(i).offset+"\t"+fileGroundTruthMap.get(key).get(i).length);
			// }
			// }
			// }
			// System.out.println("In crawled folder = "+ files.size());
			// System.out.println("total files = "+fileGroundTruthMap.size());
			// FileWriter fstream = new FileWriter("data/out.txt");
			// BufferedWriter out = new BufferedWriter(fstream);
			// for(String key : fileGroundTruthMap.keySet()){
			// out.write(key);
			// out.write("\n");
			//
			// }
			// out.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("ParseXML.parseXML()");
		}
		;
		return fileGroundTruthMap;
	}

	public static void main(String[] args) throws Exception {
		String groundFilename = "/home/kanika/ground/wikipediaGroundtruth.xml";
		ParseXML pm = new ParseXML();
		// HashMap<String,ArrayList<XMLTagInfo>> fileGroundTruthMap =
		// pm.parseXML("/mnt/bag1/kanika/wikipediaGroundtruth.xml");
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = pm
				.parseXML(args[0]);
		int c = 0;
		for (String key : fileGroundTruthMap.keySet()) {
			if (fileGroundTruthMap.get(key).size() > 10) {
				c += fileGroundTruthMap.get(key).size();
				System.out.println("key " + key + "  values "
						+ fileGroundTruthMap.get(key).size());
			}
			for (int i = 0; i < fileGroundTruthMap.get(key).size(); i++) {
				fileGroundTruthMap.get(key).get(i).cleanMention();
				System.out
						.println(fileGroundTruthMap.get(key).get(i).mention
								+ " , "
								+ fileGroundTruthMap.get(key).get(i).length
								+ " , "
								+ fileGroundTruthMap.get(key).get(i).wikiEntity);
			}
		}
		System.out.println("c " + c);
	}
}
