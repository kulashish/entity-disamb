package wikiGroundTruth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class RecursiveFileDisplay {

	public static ArrayList<String> filenames = new ArrayList<String>();
	
	public static void main(String[] args) throws Exception {
		File currentDir = new File("/home/kanika/annotated_wikiextractor/extracted"); // current directory
		displayDirectoryContents(currentDir);
		int c=0;
		for(String f:filenames){
			System.out.println(f);
			BufferedReader br=new BufferedReader(new FileReader(f));
			String str;
			while((str=br.readLine())!=null){
				c++;
			}
			br.close();
		}
		System.out.println("c "+c);
	}

	public static void displayDirectoryContents(File dir) {
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					//System.out.println("directory:" + file.getCanonicalPath());	
					displayDirectoryContents(file);
				} else {
					//System.out.println("     file:" + file.getCanonicalPath());
					filenames.add(file.getCanonicalPath());
				}
			}
	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
