package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import spotting.Config;
import spotting.ExtractKeywordsGroundTruth;
import spotting.FeatureExtractor;
import spotting.KeywordsGroundTruth;
import spotting.TrainingData;
import spotting.Wikisaurus;
import disamb.MinCutInference;

public class RunExperiments {

	HashMap<String, ArrayList<String>> queryList;
	HashMap<String, ArrayList<XMLTagInfo>> groundTruth;

	String outfile = "/home/pararth/Projects/rndproj/EntityDisamb/data/experiments.txt";

	public int maxNodes;
	public int contextSize = 300;
	public int consolidationSize = 6;

	HashSet<String> disamb_hi;
	HashSet<String> disamb_lo;

	HashMap<String, Double> filePrecisionMap;
	HashMap<String, Double> fileRecallMap;

	void readQueryFiles(String testDir) {
		queryList = new HashMap<String, ArrayList<String>>();

		File folder = new File(testDir);
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
					br = new BufferedReader(new FileReader(testDir + filename));
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
						continue;
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
	}

	void runAnnotation(ArrayList<String> querySet) throws Exception {
		disamb_hi = new HashSet<String>();
		disamb_lo = new HashSet<String>();
		for (String queryString : querySet) {
			System.out.println("Annotating chunk: " + queryString);
			ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
			KeywordsGroundTruth kw = kw_extractor.extractFromString(
					queryString, new ArrayList<String>(), contextSize);
			FeatureExtractor ft_extractor = new FeatureExtractor();

			// Config.useInlinkCount = true;
			Config.useLogisticScore = false;
			Config.useEdgePotentials = false;
			Config.useInlinkSim = true;

			TrainingData tdnew = ft_extractor.extractFeatures(kw, maxNodes,
					consolidationSize);
			/*
			 * FileWriter f2 = new
			 * FileWriter("/home/kanika/workspace/EntityDisamb/check/"
			 * +"Candidates.txt"); for(NodePotentials n :
			 * tdnew.nodes.potentials_set){
			 * f2.write(n.mention.split("_")[0]+"	"+
			 * Integer.parseInt(n.mention.split("_")[1])+"	"+ n.name+"\n"); }
			 * f2.close();
			 */
			/*
			 * HashMap<String, ArrayList<NodePotentials> > m2e = new
			 * HashMap<String, ArrayList<NodePotentials>>(); for (NodePotentials
			 * np : tdnew.nodes.potentials_set) { if
			 * (m2e.containsKey(np.mention)) { m2e.get(np.mention).add(np); }
			 * else { m2e.put(np.mention, new ArrayList<NodePotentials>());
			 * m2e.get(np.mention).add(np); } }
			 */

			/*
			 * QuadraticOptimizer qo = new QuadraticOptimizer();
			 * qo.loadEntityData(tdnew); HashMap<String,Double> disamb_val =
			 * qo.runDisambQO();
			 */
			MinCutInference mInf = new MinCutInference();
			mInf.loadEntityData(tdnew);
			HashMap<String, Double> disamb_val = mInf.runInference();

			if (disamb_val != null) {
				for (String node_name : disamb_val.keySet()) {
					Double val = disamb_val.get(node_name);
					if (val >= 0.5)
						disamb_hi.add(node_name);
					else
						disamb_lo.add(node_name);
				}
			}
		}
	}

	void runExperiment() throws Exception {
		filePrecisionMap = new HashMap<String, Double>();
		fileRecallMap = new HashMap<String, Double>();

		for (String filename : queryList.keySet()) {
			System.out.println("Annotating file " + filename);
			runAnnotation(queryList.get(filename));

			Set<String> resultEntities = new HashSet<String>(disamb_hi);
			Set<String> groundEntities = new HashSet<String>();

			for (XMLTagInfo gt : groundTruth.get(filename)) {
				groundEntities.add(gt.wikiEntity);
			}

			Set<String> commonEntities = new HashSet<String>(resultEntities);
			commonEntities.retainAll(groundEntities);
			Set<String> missingEntities = new HashSet<String>(groundEntities);
			missingEntities.removeAll(commonEntities);

			Double precision = 1.0 * commonEntities.size()
					/ resultEntities.size();
			Double recall = 1.0 * commonEntities.size() / groundEntities.size();

			filePrecisionMap.put(filename, precision);
			fileRecallMap.put(filename, recall);

			System.out.println("Writing results to log");

			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new FileWriter(outfile, true)));
				out.println(filename + " " + precision + " " + recall);
				out.println("common [" + commonEntities.size() + "]: "
						+ Arrays.toString(commonEntities.toArray()));
				out.println("missing [" + missingEntities.size() + "]: "
						+ Arrays.toString(missingEntities.toArray()));
				out.println("result [" + resultEntities.size() + "]: "
						+ Arrays.toString(resultEntities.toArray()));
				out.close();
			} catch (IOException e) {
				System.out.println("Error: cannot print " + filename);
			}
		}

	}

	public static void main(String[] args) throws Exception {
		RunExperiments exp = new RunExperiments();
		String testDataDir;
		String groundTruthFile;
		testDataDir = "/home/pararth/Projects/rndproj/EntityDisamb/data/KDDTrainingData/";
		groundTruthFile = "/home/pararth/Projects/rndproj/EntityDisamb/data/KddGroundTruth.xml";
		if (args.length < 1) {
			exp.outfile = "/home/pararth/Projects/rndproj/EntityDisamb/data/experiments.txt";
		} else {
			exp.outfile = "/home/pararth/Projects/rndproj/EntityDisamb/data/"
					+ args[0];
		}

		if (args.length >= 2) {
			Config.filterChunkThreshold = Integer.parseInt(args[1]);
		}

		if (args.length >= 3) {
			Config.filterInlinkThreshold = Double.parseDouble(args[2]);
		}

		if (args.length >= 4) {
			Config.maxCandidates = Integer.parseInt(args[3]);
		}

		if (args.length < 5) {
			exp.maxNodes = 5;
		} else {
			exp.maxNodes = Integer.parseInt(args[4]);
		}

		Config.chunkInputText = false;
		Config.useDisambPages = false;
		Config.useManualAMNWeights = false;
		Config.wikiSense = false;
		Config.filterChunk = false;
		Config.filterInlink = true;
		Config.normalizePerMention = true;
		exp.readQueryFiles(testDataDir);
		ParseXML pm = new ParseXML();
		exp.groundTruth = pm.parseXML(groundTruthFile);

		exp.runExperiment();

		Wikisaurus.closeWikipedia();
	}

}
