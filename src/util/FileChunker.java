package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import spotting.Config;

public class FileChunker {

	public Map<String, ArrayList<String>> chunk(String dir) {
		Map<String, ArrayList<String>> queryList = new HashMap<String, ArrayList<String>>();

		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				// System.out.println("List of files  " + listOfFiles[i]);
				String filename = listOfFiles[i].getName();
				System.out.println("Reading file " + filename);
				String query;

				BufferedReader br;
				String str;
				int r;
				StringBuilder s = new StringBuilder();
				try {
					int line = 0;
					br = new BufferedReader(new FileReader(dir + filename));
					while ((r = br.read()) != -1) {
						char ch = (char) r;
						s.append(ch);
					}
					br.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				str = s.toString();
				query = str.substring(0, str.lastIndexOf(" "));
				if (query != null && !"".equals(query)) {
					if (!Config.chunkInputText) {
						ArrayList<String> querySet = new ArrayList<String>();
						querySet.add(query);
						queryList.put(filename, querySet);
						return null;
					}
					ArrayList<String> tempSet = new ArrayList<String>();
					BreakIterator boundary = BreakIterator
							.getSentenceInstance(Locale.US);
					boundary.setText(query);
					int start = boundary.first();
					for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary
							.next()) {
						tempSet.add(query.substring(start, end));
					}
					ArrayList<String> querySet = new ArrayList<String>();
					int chunksize = 0;
					String tempChunk = "";
					for (String tempQuery : tempSet) {
						tempChunk += tempQuery + " ";
						chunksize += tempQuery.length();
						if (chunksize >= Config.chunkSize) {
							// System.out.println("Chunk: " + tempChunk);
							querySet.add(tempChunk);
							chunksize = 0;
							tempChunk = tempQuery; // sliding window
							chunksize = tempQuery.length();
							if (chunksize >= Config.chunkSize) {
								tempChunk = "";
								chunksize = 0;
							}
						}
					}
					if (!"".equals(tempChunk)) {
						querySet.add(tempChunk);
					}
					queryList.put(filename, querySet);
				}
			}
		}
		System.out.println("Read files successfully");
		return queryList;
	}

}
