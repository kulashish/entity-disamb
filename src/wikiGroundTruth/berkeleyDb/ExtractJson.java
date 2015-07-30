package wikiGroundTruth.berkeleyDb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;

import wikiGroundTruth.Document;
import wikiGroundTruth.RecursiveFileDisplay;

public class ExtractJson {

	private static final String TAG_URL = "url";
	private static final String TAG_ID = "id";
	private static final String TAG_TEXT = "text";
	private static final String TAG_ANNOTATIONS = "annotations";
	private static final String TAG_MENTION = "surface_form";
	private static final String TAG_OFFSET = "offset";
	private static final String TAG_ENTITY = "uri";
	private static int filecount = 0;
	private static int maxCount = 20000;
	private static int maxSize = 2000;

	// private static BufferedWriter outXml;

	public static String readJsonLine(String filename) {
		String str = null;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));

			StringBuilder s = new StringBuilder();
			while ((str = br.readLine()) != null) {
				s.append(str);
			}
			br.close();
			str = s.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return str;
	}

	public static String getContext(char[] text, int offset, String anchor) {
		String context = new String();
		char[] left = Arrays.copyOfRange(text, Math.min(offset - 1000, 0),
				offset - 1);
		char[] right = Arrays.copyOfRange(text, offset + anchor.length(), Math
				.min(offset + anchor.length() + 1000, text.length));
		String leftStr = left.toString();
		String rightStr = right.toString();
		StringTokenizer st1 = new StringTokenizer(leftStr);
		StringTokenizer st2 = new StringTokenizer(rightStr);
		int lcount = st1.countTokens();
		int l = 0;
		int rcount = st2.countTokens();
		int r = 0;
		while (st1.hasMoreTokens() && l < lcount - 5) {
			st1.nextToken();
			l++;
		}
		while (st1.hasMoreTokens()) {
			context += st1.nextToken();
		}
		while (st2.hasMoreTokens() && r < 5) {
			context += st2.nextToken();
		}
		return context;
	}

	public static void main(String[] args) throws Exception {
		// outXml = new BufferedWriter(new
		// FileWriter("/mnt/bag1/kanika/wikipediaGroundtruth.xml"));
		// outXml.write("<iitb.wikipedia.entityAnnotations>");
		// outXml.write("\n");
		// String filename =
		// "/home/kanika/annotated_wikiextractor/extracted/AA/wiki01";
		File currentDir = new File(
				"/mnt/bag1/kanika/annotated_wikiextractor/extracted");
		RecursiveFileDisplay.displayDirectoryContents(currentDir);
		try {
			System.out.println(RecursiveFileDisplay.filenames.size());
			for (String filename : RecursiveFileDisplay.filenames) {
				BufferedReader br = new BufferedReader(new FileReader(filename));
				String jsonString;
				while ((jsonString = br.readLine()) != null) {

					JSONArray annotations = null;
					Document doc = new Document();
					// int id=0;

					JSONObject obj = new JSONObject(jsonString);
					String text = obj.getString(TAG_TEXT);
					StringBuilder s = new StringBuilder();
					int count = 0;
					;
					while (count < maxSize && count < text.length()) {
						s.append(text.charAt(count));
						count++;
					}
					doc.docText = s.toString();
					String title = obj.getString(TAG_URL);

					// System.out.println(title);
					doc.docTitle = title.split("/")[4];

					if (doc.docTitle.charAt(0) >= '0'
							&& doc.docTitle.charAt(0) <= '9'
							|| doc.docTitle.contains(".PAK"))
						continue;

					filecount++;

					annotations = obj.getJSONArray(TAG_ANNOTATIONS);
					for (int i = 0; i < annotations.length(); i++) {

						JSONObject ann = annotations.getJSONObject(i);
						String mention = ann.getString(TAG_MENTION);
						String ent = ann.getString(TAG_ENTITY);
						int offset = ann.getInt(TAG_OFFSET);
						doc.offset.add(offset);
						doc.entname.add(ent);
						doc.mention.add(mention);
						char[] docText = doc.docText.toCharArray();
						doc.anchorContext.add(getContext(docText, offset,
								mention));
					}

					// write doc text to file

					/*
					 * BufferedWriter out = new BufferedWriter(new
					 * FileWriter("/mnt/bag1/kanika/wikipediaDocumentsCleaned/"
					 * +doc.docTitle)); out.write(doc.docText); out.close();
					 */

					// write doc to xml

					/*
					 * for(int j=0;j<doc.offset.size();j++){
					 * 
					 * outXml.write("<annotation>"); outXml.write("\n");
					 * outXml.write
					 * ("<docName>"+doc.docTitle.replaceAll("&","&amp;"
					 * )+"</docName>"); outXml.write("\n");
					 * outXml.write("<userId>Kanika</userId>");
					 * outXml.write("\n");
					 * outXml.write("<wikiName>"+doc.entname.
					 * get(j).replaceAll("&","&amp;")+"</wikiName>");
					 * outXml.write("\n");
					 * outXml.write("<offset>"+doc.offset.get(j)+"</offset>");
					 * outXml.write("\n");
					 * outXml.write("<mention>"+doc.mention.get
					 * (j).replaceAll("&","&amp;")+"</mention>");
					 * outXml.write("\n");
					 * outXml.write("<length>"+doc.mention.get
					 * (j).length()+"</length>"); outXml.write("\n");
					 * outXml.write("</annotation>"); outXml.write("\n"); }
					 */
					// System.out.println("done "+filecount+" files");
				}
			}
			// outXml.write("</iitb.wikipedia.entityAnnotations>");
			// outXml.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
