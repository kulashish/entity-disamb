package spotting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import util.DisambProperties;
import util.ParseXML;
import util.XMLTagInfo;

public class ExtractKeywordsGroundTruth {
	private static String testDir = "/home/pararth/Projects/rndproj/EntityDisamb/data/";

	// Extracts single instance of keywords and ground truth from given filename
	// (absolute path)
	// note : ground truth filename is same as train file name but they are in
	// different folders
	public KeywordsGroundTruth extractSingleFile(String filename, int contSize) {
		String trainDir = "/home/kanika/workspace/WikiTraining/data/finalTrainingData/";
		String groundDir = "//home/kanika/workspace/WikiTraining/data/ground/";
		KeywordsGroundTruth doc1 = new KeywordsGroundTruth(filename, trainDir,
				groundDir, contSize);
		return doc1;
	}

	// Extracts list of keywords & ground truth from all files in given
	// directory path
	public List<KeywordsGroundTruth> extractDirectory(String trainDir,
			String groundDir, int contSize) {
		ArrayList<KeywordsGroundTruth> allFiles = new ArrayList<KeywordsGroundTruth>();
		File folder = new File(trainDir);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("List of files  " + listOfFiles[i]);
				String filename = listOfFiles[i].getName();
				// System.out.println("File " + filename);
				KeywordsGroundTruth temp = new KeywordsGroundTruth(filename,
						trainDir, groundDir, contSize);
				allFiles.add(temp);
			}
		}
		return allFiles;
	}

	public List<KeywordsGroundTruth> extractTestDirectory(String testDir,
			int contSize) throws Exception {
		ArrayList<KeywordsGroundTruth> allFiles = new ArrayList<KeywordsGroundTruth>();
		File folder = new File(testDir);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("List of files  " + listOfFiles[i]);
				String filename = listOfFiles[i].getName();
				// System.out.println("File " + filename);
				KeywordsGroundTruth temp = new KeywordsGroundTruth(filename,
						testDir, contSize);
				// if(ParseXML.fileGroundTruthMap.containsKey(filename)){
				// temp.setGroundTruth(ParseXML.fileGroundTruthMap.get(filename));
				// }
				allFiles.add(temp);
			}
		}

		return allFiles;
	}

	public KeywordsGroundTruth extractFilewithGroundTruth(
			String groundfilename, String filename, String trainDir,
			int contextsize) throws Exception {
		KeywordsGroundTruth temp = new KeywordsGroundTruth(filename, trainDir,
				contextsize);

		temp.contextSize = contextsize;
		ParseXML pm = new ParseXML();
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = pm
				.parseXML(groundfilename);

		Iterator<XMLTagInfo> it = fileGroundTruthMap.get(filename).iterator();
		while (it.hasNext()) {
			if (it.next().offset >= temp.getDocumentText().length() - 1) {
				it.remove();
			}
		}
		if (fileGroundTruthMap.containsKey(filename)) {
			temp.setGroundTruth(fileGroundTruthMap.get(filename));
		}
		return temp;
	}

	// give full path for trainDir and xml format ground Truth File.
	public List<KeywordsGroundTruth> extractDirectorywithGroundTruth(
			String trainDir, String groundFilename, int contextsize)
			throws Exception {
		ArrayList<KeywordsGroundTruth> allFiles = new ArrayList<KeywordsGroundTruth>();
		File folder = new File(trainDir);
		File[] listOfFiles = folder.listFiles();
		ParseXML pm = new ParseXML();
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = pm
				.parseXML(groundFilename);

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				// System.out.println("List of files  " + listOfFiles[i]);
				String filename = listOfFiles[i].getName();
				// System.out.println("File " + filename);
				KeywordsGroundTruth temp = new KeywordsGroundTruth(filename,
						trainDir, contextsize);
				if (fileGroundTruthMap.containsKey(filename)) {
					temp.setGroundTruth(fileGroundTruthMap.get(filename));
					temp.setGroundMention(fileGroundTruthMap.get(filename));
				}
				allFiles.add(temp);
			}
		}

		return allFiles;
	}

	// for training data generation purposes

	public KeywordsGroundTruth extractForTrainingwithGroundTruth(
			String trainDir, String filename, String groundFilenameWiki,
			String groundFilenameManual, int contextsize) throws Exception {

		// System.out.println("done keywords ground truth in extractFilewithGroundTruth");
		ParseXML pm = new ParseXML();
		// System.out.println(groundFilenameWiki);
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapWiki = pm
				.parseXML(groundFilenameWiki);
		// System.out.println("Ground truth Map : " + fileGroundTruthMapWiki);
		// System.out.println("done wiki parsing");
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapManual;
		if (!"".equals(groundFilenameManual)) {
			ParseXML pm1 = new ParseXML();
			fileGroundTruthMapManual = pm1.parseXML(groundFilenameManual);
		} else {
			fileGroundTruthMapManual = null;
		}

		String entity = null;
		for (int i = 0; i < fileGroundTruthMapWiki.get(filename).size(); i++) {
			entity = fileGroundTruthMapWiki.get(filename).get(i).wikiEntity;
			if (null != entity)
				fileGroundTruthMapWiki.get(filename).get(i).wikiEntity = entity
						.replaceAll("_", " ");

		}

		KeywordsGroundTruth file_kwt = new KeywordsGroundTruth(filename,
				trainDir, contextsize, fileGroundTruthMapWiki,
				fileGroundTruthMapManual);

		file_kwt.contextSize = contextsize;

		Iterator<XMLTagInfo> it = fileGroundTruthMapWiki.get(filename)
				.iterator();
		while (it.hasNext()) {
			if (it.next().offset >= file_kwt.getDocumentText().length() - 1) {
				it.remove();
			}
		}
		file_kwt.setKeywordsTraining(fileGroundTruthMapWiki,
				fileGroundTruthMapManual, filename);

		if (fileGroundTruthMapWiki.containsKey(filename)) {
			file_kwt.setGroundTruth(fileGroundTruthMapWiki.get(filename));
		}

		if (fileGroundTruthMapManual != null)
			file_kwt.setGroundTruth(fileGroundTruthMapManual.get(filename));

		return file_kwt;
	}

	// for debugging purposes
	public KeywordsGroundTruth extractFromString(String document,
			ArrayList<String> groundTruth, int csize) throws Exception {
		KeywordsGroundTruth doc1 = new KeywordsGroundTruth(document,
				groundTruth, csize);
		doc1.contextSize = csize;
		return doc1;
	}

	public KeywordsGroundTruth extractWikiMiner(String document,
			ArrayList<String> groundTruth, int csize) {
		KeywordsGroundTruth doc1 = new KeywordsGroundTruth(document,
				groundTruth, csize, true);
		doc1.contextSize = csize;
		return doc1;
	}

	public static void main(String args[]) throws Exception {
		if (args.length >= 1) {
			Config.useDisambPages = false;
			Config.useManualAMNWeights = false;
			Config.wikiSense = false;
			System.out.println("Reading file " + args[0]);
			String query;

			BufferedReader br;
			String str;
			int r;
			StringBuilder s = new StringBuilder();
			try {
				int line = 0;
				br = new BufferedReader(new FileReader(testDir + args[0]));
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

			ExtractKeywordsGroundTruth ex = new ExtractKeywordsGroundTruth();
			KeywordsGroundTruth obj = ex.extractFromString(query,
					new ArrayList<String>(), 300);
			// System.out.println("\nDocument: "+obj.getDocumentText());
			System.out.println("Keywords: " + obj.getMentionNames());
			// System.out.println("ground Truth: "+obj.getGroundTruth());
			DisambProperties props = DisambProperties.getInstance();
			LuceneIndexWrapper luceneIndex = new LuceneIndexWrapper(
					props.getCompleteIndex(), props.getRedirectIndex(),
					props.getInlinkIndex(), props.getDisambIndex(),
					props.getAnchorIndex());
			NodePotentialsSet np_set = luceneIndex
					.extractNodesNewConsolidation(obj, 20, 6, obj.filename);
			for (NodePotentials np : np_set.potentials_set) {
				System.out.print(np.name + ": " + np.mention + "\n");
			}
			System.out.println();
		}
	}
}
