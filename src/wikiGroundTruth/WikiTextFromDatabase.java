package wikiGroundTruth;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WikiTextFromDatabase {

	private static final int PAGESIZE = 50000;
	private static final int maxTokens = 1000;
	public static void main(String[] args) {
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/wikipedia", "root", "aneedo");
			readDocs(conn);
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void readDocs(Connection conn) throws Exception {
		String sql = "SELECT * FROM Page limit ? offset ?";
		PreparedStatement ps = conn.prepareStatement(sql);
		Statement stmt = conn.createStatement();
		ps.setInt(1,PAGESIZE);
		boolean flag = true;
		int offset = 0;
		System.out.println("Reading from database...");
		while (flag){
			ps.setInt(2,offset*PAGESIZE);
			ResultSet rs = ps.executeQuery();
			int count = 0;
			flag = false;
			while(rs.next()){

				String page_id = Integer.toString(rs.getInt("id"));
				String page_text = rs.getString("text");
				String page_title = rs.getString("name");

				int UrangeLow = 1234;
				int UrangeHigh = 2345;
				for(int iLetter = 0; iLetter < page_title.length() ; iLetter++) {
					int cp = page_title.codePointAt(iLetter);
					if (cp >= UrangeLow && cp <= UrangeHigh) {
						// word is NOT English
						continue;
					} 
				}
				if(page_title.contains("/"))
					continue;	
				Pattern pattern = Pattern.compile("^[0-9]");
				Matcher matcher = pattern.matcher(page_title);
				if(matcher.matches()) {
					continue;
				}
				if(page_title.charAt(0)>='0'&& page_title.charAt(0)<='9')
					continue;
				
				if(page_text.length()<500){
					continue;
				}
				boolean add=true;
				for(int iLetter = 0; iLetter < page_text.length() ; iLetter++) {
					int cp = page_text.codePointAt(iLetter);
					if (cp >= UrangeLow && cp <= UrangeHigh) {
						// word is NOT English
						add=false;
					}
				}
				if(add==false)
					continue;
				System.out.println("name "+page_title);
				BufferedWriter out = new BufferedWriter(new FileWriter("/home/kanika/wikipediaDocuments/"+page_title+".txt"));
				StringTokenizer st = new StringTokenizer(page_text);
				for(int i = 0; i < maxTokens && st.hasMoreTokens(); i++ ) {
					out.write(st.nextToken());
					out.write(" ");
				}
				out.close();
				System.out.println("finished "+count+" documents");
				if(count==20008)
					break;
				count++;
			}

		}   
	}

}

