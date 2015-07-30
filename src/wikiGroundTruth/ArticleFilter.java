package wikiGroundTruth;


import it.unimi.di.mg4j.index.snowball.EnglishStemmer;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

public class ArticleFilter {
	public static ArrayList<String> mentions = new ArrayList<String>();
	public static ArrayList<String> entities = new ArrayList<String>();

	private static boolean matchSpaces(String sentence, int matches) {

		int c =0;
		for (int i=0; i< sentence.length(); i++) {
			if (sentence.charAt(i) == ' ') c++;
			if (c == matches) return true;
		}
		return false;
	}

	static Pattern pat=Pattern.compile("\\{\\{(.*?)\\}\\}");

	public static String removeCurlyBrackets(String input){
		String result=null;
		if(input!=null){
			result=pat.matcher(input).replaceAll("");
			result.trim();
		}
		return result;
	}

	static Pattern references=Pattern.compile("<[Rr]ef(.*?)</[Rr]ef>");

	public static String removeReferences(String input){
		String result=null;
		if(input!=null){
			result=references.matcher(input).replaceAll("");
			result.trim();
		}
		return result;
	}

	public static String removeStarLines(String input){
		String result=null;
		if(input!=null){
			if(input.startsWith("*")){
				return null;
			}
			else{
				return input.trim();
			}
		}
		else{
			return null;
		}
	}
	static Pattern links=Pattern.compile("\\[\\[(.*?)\\]\\]");
	static Pattern badlinks=Pattern.compile("\\[\\[#(.*?)\\]\\]");
	static Pattern files = Pattern.compile("\\[\\[File:(.*?)\\]\\]");
	static Pattern image = Pattern.compile("\\[\\[Image:(.*?)\\]\\]");
	static Pattern html=Pattern.compile("<(.*?)>");
	static Pattern htmlbraces = Pattern.compile("\\{(.*?)\\}");

	public static String removeBadLinks(String input){
		String result=null;
		if(input!=null){
			result=badlinks.matcher(input).replaceAll("");
			result.trim();
		}
		return result;
	}
	
	public static String removeHtmlBraces(String input){
		String result=null;
		if(input!=null){
			result=htmlbraces.matcher(input).replaceAll("");
			result.trim();
		}
		return result;
	}
	public static String removeFiles(String input){
		String result=null;
		if(input!=null){
			result=files.matcher(input).replaceAll("");
			result.trim();
		}
		return result;
	}
	public static String removeImage(String input){
		String result=null;
		if(input!=null){
			result=image.matcher(input).replaceAll("");
			result.trim();
		}
		return result;
	}
	public static String removeHtml(String input){
		String result=null;
		if(input!=null){
			result=html.matcher(input).replaceAll("");
			result.trim();
		}
		return result;
	}
	public static String removeLinks(String input){
		String result=null;
		if(input!=null){
			StringBuffer sb= new StringBuffer();
			Matcher matcher = links.matcher(input);
			while (matcher.find()) {
				//System.out.println(matcher.group(0));
				String mentionEnt = matcher.group(1);
				//	System.out.println(mentionEnt);
				String mention,ent;
				if(mentionEnt.contains("|")){
					String[] temp = mentionEnt.split("\\|");
					if(temp.length>1){
						ent = temp[0];
						mention = temp[1];
					}
					else{

						mention=temp[0];
						ent=temp[0];
						mention = mention.replaceAll("\\s+", " ");
						mention = mention.replaceAll("\\[\\[", " ");
						ent = ent.replaceAll("\\s+", " ");
						ent = ent.replaceAll("\\[\\[", " ");
						//                    					System.out.println(mentionEnt+"   ******************problem********************");
						//                    					System.exit(1);
					}
				}
				else{
					//	System.out.println("  "+mentionEnt);
					
					ent = mentionEnt;
					mention = mentionEnt;
					mention = mention.replaceAll("\\s+", " ");
					mention = mention.replaceAll("\\[\\[", " ");
					ent = ent.replaceAll("\\s+", " ");
					ent = ent.replaceAll("\\[\\[", " ");
				}

				mention = mention.replaceAll("\\$","\\\\\\$");
				mention = mention.replaceAll("\\s+", " ");
				matcher.appendReplacement(sb,"[["+mention+"]] ");
				//	System.out.println(mention+"  "+ent);
				boolean add=true;
				int UrangeLow = 1234;
				int UrangeHigh = 2345;
				for(int iLetter = 0; iLetter < mention.length() ; iLetter++) {
					int cp = mention.codePointAt(iLetter);
					if (cp >= UrangeLow && cp <= UrangeHigh) {
						// word is NOT English
						add=false;
					}
				}
				if(add==true){
						mentions.add(mention);
						entities.add(ent);
				}
				
			}
			result=sb.toString();
			result.trim();
		}
		return result;
	}

	public static String removeSquare(String input){
		String result=null;
		if(input!=null){
			result=input.replaceAll("\\[", "");
			result=result.replaceAll("\\]", "");
		}
		return result;
	}

	static Pattern categ=Pattern.compile("\\[\\[Category:(.*?)\\]\\]");

	public static String removeCat(String input){
		String result=null;
		if(input!=null){
			result=categ.matcher(input).replaceAll("");
			result.trim();
		}
		return result;
	}

	public static String removeEqualStart(String input){
		String result=null;
		if(input!=null){
			if(!input.startsWith("==")){
				result=input;
			}
			else{
				result=input.replaceAll("==", "");
				result.trim();
			}
		}
		return result;
	}

	public static String replace(String input){
		String resString=null;
		if(input!=null){
			resString=input.replaceAll("\\|", " ");
		}
		return resString;
	}

	public static String removeQuotes(String input){
		String resString=null;
		if(input!=null){
			//     resString=input.replaceAll("\'", " ");
			resString=input.replaceAll("\"", " ");
		}
		return resString;
	}

	static Pattern br=Pattern.compile("<BR>");

	public static String removeBR(String input){
		String result=null;
		if(input!=null){
			result=br.matcher(input).replaceAll(" ");
			result.trim();
		}
		return result;
	}

	public static String clean(String line){
		if(line!=null)
			line=removeImage(line);
		if(line!=null)
			line=removeFiles(line);
		if(line!=null)
			line=removeCurlyBrackets(line);
		if(line!=null)
			line=removeReferences(line);
		if(line!=null)
			line=removeStarLines(line);
		if(line!=null)
			line=removeEqualStart(line);
		if(line!=null)
			line=removeQuotes(line);
		if(line!=null)
			line=removeBR(line);
		if(line!=null)
			line=removeCat(line);
		if(line!=null)
			line=removeHtml(line);
		if(line!=null)	
			line=removeHtmlBraces(line);
		if(line!=null)
			line=removeBadLinks(line);
		if(line!=null)
			line=removeLinks(line);
		if(line!=null)
			line=replace(line);
		if(line!=null)
			line=line.replaceAll(" = ", " ");
		if(line!=null)
		line=line.replaceAll("\\s+", " ");
		return line;
	}





	public static String process(String t) throws IOException {
		StringTokenizer st=new StringTokenizer(t,"\n");
		String temp;
		StringBuilder sb= new StringBuilder();
		while(st.hasMoreElements()){
			temp=st.nextToken();
			String pass=clean(temp);
			if(pass!=null){
				sb.append(pass+" ");
			}
		}
		st=new StringTokenizer(sb.toString());
		EnglishStemmer stermmer=new EnglishStemmer();
		MutableString str=new MutableString();
		sb.delete(0, sb.length());
		while(st.hasMoreElements()){
			str.append(st.nextToken());
			stermmer.processTerm(str);
			sb.append(str+" ");
			str.delete(0, str.length());
		}
		return (clean(sb.toString()));
	}


	public Document WikiTextToCleanText(String text,String filename) throws IOException{
		mentions.clear();
		entities.clear();
		String newStr = clean(text);
		if(newStr==null)
			return null;
		newStr=removeSquare(newStr);
		Document doc = new Document();
		int UrangeLow = 1234;
		int UrangeHigh = 2345;
		StringBuilder finalDoc = new StringBuilder();
		doc.docText = newStr;
		doc.docTitle=filename;
		int curr_offset = 0;
		for(int j=0;j<mentions.size();j++){ 
			if(mentions.get(j).contains("$")){
				mentions.set(j,mentions.get(j).replaceAll("\\\\\\$","\\$")); 	
			}
			curr_offset = newStr.indexOf(mentions.get(j), curr_offset);
			doc.offset.add(curr_offset);
			WikiMention wm = new WikiMention();
			wm.mention = mentions.get(j);
			wm.label = 1;
			wm.groundEntity = entities.get(j);
			doc.entity.add(wm);
		}
		return doc;
	}

	public static void main(String[] args) throws IOException, SAXException{
		
		File folder = new File("/home/kanika/wikipediaDocuments");
		File[] listOfFiles = folder.listFiles();
		BufferedWriter outXml = new BufferedWriter(new FileWriter("/home/kanika/wikipediaGroundtruth.xml"));
		outXml.write("<iitb.wikipedia.entityAnnotations>");
		outXml.write("\n");
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
			//	System.out.println("List of files  " + listOfFiles[i]);
				String filename = listOfFiles[i].getName();
				BufferedReader br=new BufferedReader(new FileReader("/home/kanika/wikipediaDocuments/"+filename));
				String str;
				StringBuilder s=new StringBuilder();
				while((str=br.readLine())!=null){
					s.append(str);
				}
				br.close();
				ArticleFilter af = new ArticleFilter();
				Document doc = af.WikiTextToCleanText(s.toString(),filename);
				if(doc==null){
					System.out.println("doc returned null from WikiTextToCleanText function");
					continue;
				}
				
				if(doc.docText==null||doc.offset==null||doc.entity==null){
					System.out.println("doc text null from WikiTextToCleanText function");
					continue;
				}

				for(int j=0;j<doc.offset.size();j++){ 

					outXml.write("<annotation>");
					outXml.write("\n");
					outXml.write("<docName>"+doc.docTitle.replaceAll("&","&amp;")+"</docName>");
					outXml.write("\n");
					outXml.write("<userId>Kanika</userId>");
					outXml.write("\n");
					outXml.write("<wikiName>"+doc.entity.get(j).groundEntity.replaceAll("&","&amp;")+"</wikiName>");
					outXml.write("\n");
					outXml.write("<offset>"+doc.offset.get(j)+"</offset>");
					outXml.write("\n");
					outXml.write("<length>"+doc.entity.get(j).mention.length()+"</length>");
					outXml.write("\n");
					outXml.write("</annotation>");
					outXml.write("\n");
				}
				BufferedWriter out = new BufferedWriter(new FileWriter("/home/kanika/cleanedWikiDocuments/"+filename));
				out.write(doc.docText);
				out.close();
			}

		}

		outXml.write("</iitb.wikipedia.entityAnnotations>");
		outXml.close();
	}
}


