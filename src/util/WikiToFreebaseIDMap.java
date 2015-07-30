package util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileReader;

public class WikiToFreebaseIDMap {
	
	public HashMap<String, String> mapping = null;
	public HashMap<String, Vector<String> > mentionEntityMap = null;
	
	private static WikiToFreebaseIDMap wikiToFbmap = null;

	public static WikiToFreebaseIDMap getInstance() {
		return wikiToFbmap;
	}
	
	public Vector<String> getAllWikiTitles(String mention) {
		return mentionEntityMap.get(mention);
	}
	
	public String getFreeBaseID(String wikiTitle) {
		return mapping.get(wikiTitle);
	}
	
	public void setMap(HashMap<String, String> mapping) {
		this.mapping = mapping;
	}
	
	public void setmentionEntityMap(HashMap<String, Vector<String> > mentionEntityMap) {
		this.mentionEntityMap = mentionEntityMap;
	}

	public static void init(String datafile) throws FileNotFoundException,
			IOException {
		wikiToFbmap = new WikiToFreebaseIDMap();
		
        BufferedReader bReader = new BufferedReader(
                new FileReader(datafile));
        
        HashMap<String, String> map = new HashMap();
        
        HashMap<String, Vector<String> > meMap = new HashMap();
 
        String line;
 
        /**
         * Looping the read block until all lines in the file are read.
         */
        while ((line = bReader.readLine()) != null) {
 
            /**
             * Splitting the content of tabbed separated line
             */
            String datavalue[] = line.split("\t");
            String freebaseid = datavalue[0];
            String mention = datavalue[1];
            String wikititle = datavalue[2];
            
            /**
             * Printing the value read from file to the console
             */
    		Pattern quotedCharPattern = Pattern.compile("\\$([0-9A-Fa-f]{4})");
    		
    		String s = (String) wikititle;
            StringBuffer sb = new StringBuffer();
            
            int last = 0;
            Matcher m = quotedCharPattern.matcher(s);
            while (m.find()) {
                int start = m.start();
                int end = m.end();
                if (start > last) {
                    sb.append(s.substring(last, start));
                }
                last = end;
                
                sb.append((char)Integer.parseInt(s.substring(start + 1, end), 16));
            }
            
            if (last < s.length()) {
                sb.append(s.substring(last));
            }
            
            mention = mention.replace("@en", "").toLowerCase();
            mention = mention.replace("\"", "");
            
            map.put(sb.toString(), freebaseid);
            
           // System.out.println("mention : " +  mention + " entity : " + sb.toString());
            
            Vector<String> entities = meMap.get(mention);
            
            if(entities == null){
            	entities = new  Vector<String>();
            }
            
            entities.add(sb.toString());
            meMap.put(mention, entities);
            
            //System.out.println(freebaseid + "\t" + sb.toString());
        }
        bReader.close();
        
        wikiToFbmap.setMap(map);
        wikiToFbmap.setmentionEntityMap(meMap);
        
//        Vector<String> freebaseTitles = meMap.get("montclair elementary school");
//		for(String fbTitle : freebaseTitles){
//			System.out.println("fbTitle : " + fbTitle);
//		}
	}
	
    public static void main(String args[]) throws Exception {
        /**
         * Source file to read data from.
         */
        String dataFileName = "/media/sunny/E/ERD/entity.tsv";
 
        BufferedReader bReader = new BufferedReader(
                new FileReader(dataFileName));
        
        HashMap<String, String> map = new HashMap();
 
        String line;
 
        /**
         * Looping the read block until all lines in the file are read.
         */
        while ((line = bReader.readLine()) != null) {
 
            /**
             * Splitting the content of tabbed separated line
             */
            String datavalue[] = line.split("\t");
            String freebaseid = datavalue[0];
            String entity = datavalue[1];
            String wikititle = datavalue[2];
            
            /**
             * Printing the value read from file to the console
             */
            System.out.println(freebaseid + "\t" + wikititle);
            
            map.put(wikititle, freebaseid);

        }
        bReader.close();
        //wikiToFbmap.setMap(map);
    }
}
