package wikiGroundTruth;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import it.unimi.dsi.util.Interval;

import util.XMLTagInfo;
import spotting.NodePotentials;

public class ParseAnnotations {

    public static String username = "";

	public static ArrayList<XMLTagInfo> parseXML(String filename) {
		final ArrayList<XMLTagInfo> xmlList = new ArrayList<XMLTagInfo>();
		final ArrayList<String> userNames = new ArrayList<String>();
		ArrayList<Annotation> annList = new ArrayList<Annotation>();
		
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
				boolean busername = false;
			
				String dname;
				XMLTagInfo obj;
		
				public void startElement(String uri, String localName,String qName, 
						Attributes attributes) throws SAXException {

					//System.out.println("Start Element :" + qName);
					//System.out.println("Attribute :" + attributes);

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
					
					if (qName.equalsIgnoreCase("userName")) {
						busername = true;
					}

				}

				public void characters(char ch[], int start, int length) throws SAXException {

					if(bdocName){
						

					}
					if(bwikiName){
						obj.wikiEntity=new String(ch, start, length);
					}
					if(boffset){
					    try {
						    obj.offset = Integer.parseInt(new String(ch, start, length));
						} catch (Exception e){
						    obj.offset = 0;
						}
					}

					if(blength){
						//obj.length=Integer.parseInt(new String(ch, start, length));
					}
					
					if(bmention) {
						obj.mention = new String(ch, start, length);
						obj.mention = obj.mention.split("_")[0];
						obj.length = obj.mention.length();
				    }
						
					if (busername)
					    username = new String(ch, start, length);

				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {

					if (qName.equalsIgnoreCase("annotation")) {
						bannotation = false;
						xmlList.add(obj);
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
					
					if (qName.equalsIgnoreCase("userName")) {
						busername = false;
					}
					
					if (qName.equalsIgnoreCase("iitb.wikipedia.entityAnnotations")) {
					
					}
				}
			};
			/*File file = new File(filename);
			InputStream inputStream= new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream,"UTF-8");
			 
			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");*/
			
			saxParser.parse(filename, handler);
			/*if (userNames.size() > 0)
			    username = userNames.get(0);
			
			HashMap<String, ArrayList<XMLTagInfo>> xmlMap = new HashMap<String, ArrayList<XMLTagInfo>>();
			for (XMLTagInfo info: xmlList){
			    if (!xmlMap.containsKey(info.mention)){
			        xmlMap.put(info.mention, new ArrayList<XMLTagInfo>());
			    }
			    xmlMap.get(info.mention).add(info);
			}
			
			for (String mention: xmlMap.keySet()){
			    Annotation ann = new Annotation();
			    ann.mention = mention;
			    ann.interval = Interval.valueOf(
			            xmlMap.get(mention).get(0).offset,
			            xmlMap.get(mention).get(0).offset+
			            xmlMap.get(mention).get(0).length-1);
			    ann.candidateEntities = new ArrayList<NodePotentials>();
			    for (XMLTagInfo info: xmlMap.get(mention)){
			        NodePotentials np = new NodePotentials();
			        np.name = info.wikiEntity;
			        np.mention = mention;
			        np.offset = info.offset;
			        np.length = info.length;
			        np.interval = ann.interval;
			        np.label = 1;
			        ann.candidateEntities.add(np);
			    }
			    annList.add(ann);
			}
			
			Collections.sort(annList);*/
			
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("ParseXML.parseXML()");
		};
		return xmlList;
	}
	
	
	public static void main (String[] args) throws Exception{
		
	}
}
