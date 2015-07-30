package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ERDResultHolder {
	
	static List<String> results = null;
	static int counter = 0;
	static HashMap<String,String> queries = null;
	static int querycounter = 0;
	
	public static void init(String file) throws FileNotFoundException{
		
		if(results == null){
			results = new ArrayList<String>();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			int counter = 0;
			try {
				while ((line = reader.readLine()) != null) {
					if(line.equals("#BEGIN#")){
						counter++;
						String Text = "";
						while ((line = reader.readLine()) != null) {
							if(line.equals("#END#"))
								break;
							
							Text = Text.concat(line);
							Text = Text.concat("\n");
						}
						results.add(Text);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("ERDResultCreator Initialized.");
		}
		if(queries == null)
			queries = new HashMap();
	}
	
	public static void addQuery(String TextID,String Text){
		if(queries == null)
			queries = new HashMap();
		
		queries.put(TextID, Text);
		System.out.println("In ERDResultHolder addquery " + queries.size());
	}
	
	public static String getQueryInstance(String TextID){
		
		System.out.println("In ERDResultHolder getQueryInstance " + (querycounter+1) + ".");
		
		String response = queries.get(TextID);
		
		System.out.println(response);
		
		return response;
	}
	
	public static String getInstance(){
		
		System.out.println("In ERDResultHolder getInstance " + (counter+1) + ".");
		
		String response = results.get(counter++);
		
		System.out.println(response);
		
		return response;
	}

}
