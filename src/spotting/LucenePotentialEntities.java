package spotting;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LucenePotentialEntities {
	static String trainDir = "/home/kanika/workspace/EntityDisamb/data/temp/";
	// static String trainDir; //=
	// "/home/kanika/workspace/EntityDisamb/data/KDDTrainingData/";
	static String groundFilename = "/home/kanika/workspace/EntityDisamb/data/KddGroundTruth.xml"; // add
	// filename
	// here!
	// static String groundFilename; //=
	// "/home/kanika/wikipediaGroundtruth.xml"; // add filename here!
	static String csvFile = "/home/kanika/wikiTraining/nodePotentialLearning/KDDTestDataNewTraining.csv";
	static final String CSV_SEPARATOR = ",";
	static int maxNodes = 10; // hardcoded max number of entities to be
	// extracted from lucene
	static int consolidationSize = 5;
	static int csize = 80;

	public static void spotter(String trainDir, String groundfilename,
			String csvFilename, int maxNodes, int csize, int consolidationSize) {
		ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
		List<KeywordsGroundTruth> kws;
		try {
			kws = kw_extractor.extractDirectorywithGroundTruth(trainDir,
					groundfilename, csize);
			FeatureExtractor ft_extractor = new FeatureExtractor();
			for (int k = 0; k < kws.size(); k++) {
				KeywordsGroundTruth kw = kws.get(k);
				long start = System.currentTimeMillis();

				TrainingData temp = ft_extractor.extractFeatures(kw, maxNodes,
						consolidationSize);
				long elapsedTimeMillis = System.currentTimeMillis() - start;
				float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);
				writeToCSV(temp, csvFilename);
				System.out.println("For file " + kw.filename
						+ " process completed in  " + elapsedTimeMin + " mins");
				System.out.println("finished " + (k + 1) + " files");

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		ArrayList<TrainingData> trainDataList = new ArrayList<TrainingData>();

		try {

			ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
			List<KeywordsGroundTruth> kws = kw_extractor
					.extractDirectorywithGroundTruth(trainDir, groundFilename,
							csize);
			FeatureExtractor ft_extractor = new FeatureExtractor();

			for (int k = 0; k < kws.size(); k++) {
				KeywordsGroundTruth kw = kws.get(k);
				long start = System.currentTimeMillis();
				// TrainingData temp = ft_extractor.extractFeatures(kw,
				// maxNodes,consolidationSize);
				TrainingData temp = ft_extractor.extractFeaturesForTraining(kw,
						maxNodes, consolidationSize);
				trainDataList.add(temp);

				long elapsedTimeMillis = System.currentTimeMillis() - start;
				float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);

				System.out.println("For file " + kw.filename
						+ " process completed in  " + elapsedTimeMin + " mins");
				System.out.println("finished " + (k + 1) + " files ");

				// if(k%10==0){
				// writeToCSVList(trainDataList,csvFile);
				// trainDataList.clear();
				// System.out.println("written another 10 files till"+k);
				// }
				//
			}
			// writeToCSVList(trainDataList,csvFile);
			// System.out.println("written final set of files completed ");
			if (Config.wikiSense)
				Wikisaurus._wikipedia.close();

			for (int i = 0; i < trainDataList.size(); i++) {
				int l = 0;
				for (NodePotentials np : trainDataList.get(i).nodes.potentials_set) {
					if (np.label == 1)
						l++;
					System.out.println(np.mention + "  " + np.name + "  "
							+ np.logistic_score + "  " + np.label);
					// // System.out.println(np.mention
					// +"     ##111##     "+np.name+" ##222##  "+np.context_score_frequent+"  ##333##  "+np.context_score_synopsis+"   ##444##   "+np.context_score_vbdj+"   ##555##   "+"    ##666##    "+np.redirection+"    ##777##   "+np.page_title_score+"    ##888##    "+np.sense_probability);
					// // off =text.indexOf(np.mention.split("_")[0]);
				}
				System.out.println("l " + l);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		// Serializer.encode(trainDataListNew, "train.ser");
		// Serializer.decode("train.ser");

	}

	public static void writeMinMax(double[] min, double[] max, String filename) {
		try {
			FileWriter f1 = new FileWriter(filename, true);
			StringBuffer head = new StringBuffer();
			for (int i = 0; i < min.length; i++) {
				head.append(min[i]);
				head.append(CSV_SEPARATOR);
			}
			f1.write(head.toString());
			f1.write("\n");
			StringBuffer head1 = new StringBuffer();
			for (int i = 0; i < max.length; i++) {
				head1.append(max[i]);
				head1.append(CSV_SEPARATOR);
			}
			f1.write(head1.toString());
			f1.write("\n");
			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public static void writeToCSVList(ArrayList<TrainingData> tdList,
			String csvFile) {
		try {
			FileWriter f1 = new FileWriter(csvFile, true);
			StringBuffer head = new StringBuffer();
			// head
			// .append("sense,title,redirect,inlink,outlink,anchor,,anchor_cos,anchorcontext,full");
			// f1.write(head.toString());
			// f1.write("\n");
			for (int i = 0; i < tdList.size(); i++) {
				TrainingData td = tdList.get(i);
				for (NodePotentials np : td.nodes.potentials_set) {
					StringBuffer oneLine = new StringBuffer();

					oneLine.append(np.sense_probability);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.page_title_score);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.redirection);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.inlink_count);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.outlink_count);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.anchor_text_score);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.anchor_text_cosine);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.anchor_text_context_cosine);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.full_text_cosine);
					oneLine.append(CSV_SEPARATOR);

					oneLine.append(np.label);

					f1.write(oneLine.toString());
					f1.write("\n");

				}
			}
			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public static void writeToCSV(TrainingData td, String csvFile) {
		try {

			FileWriter f1 = new FileWriter(csvFile, true);
			for (NodePotentials np : td.nodes.potentials_set) {
				StringBuffer oneLine = new StringBuffer();

				oneLine.append(np.sense_probability);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.context_score_frequent);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.context_score_synopsis);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.context_score_vbdj);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.page_title_score);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.redirection);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.inlink_count);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.outlink_count);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.anchor_text_score);
				oneLine.append(CSV_SEPARATOR);

				oneLine.append(np.label);

				f1.write(oneLine.toString());
				f1.write("\n");

			}

			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public static void recallForSpottingWithRepeatedMentions(TrainingData td) {
		try {
			FileWriter f1 = new FileWriter("check/" + maxNodes + "_"
					+ consolidationSize + "_" + csize + "_"
					+ "recallWithRepeatedMention.txt", true);
			HashMap<String, ArrayList<String>> mentionToEntitiesMap = new HashMap<String, ArrayList<String>>();
			for (NodePotentials np : td.nodes.potentials_set) {
				String mentionName = np.mention.split("_")[0].toLowerCase()
						.replaceAll("[^0-9a-z\\sA-Z/\\-]", "");
				if (mentionToEntitiesMap.get(mentionName) == null)
					mentionToEntitiesMap.put(mentionName,
							new ArrayList<String>());
				mentionToEntitiesMap.get(mentionName).add(np.name);

			}
			/*
			 * for(String key : mentionToEntitiesMap.keySet()){
			 * System.out.println("key  "+key); for(int j=0;
			 * j<mentionToEntitiesMap.get(key).size();j++)
			 * System.out.println(mentionToEntitiesMap.get(key).get(j));
			 * 
			 * }
			 */

			int totalgroundUniqueMentions = td.groundtruth
					.getGroundMentionNames().size();
			int recallCount = 0;

			for (int j = 0; j < td.groundtruth.getGroundMentionNames().size(); j++) {
				String groundMent = td.groundtruth.getGroundMentionNames()
						.get(j).trim().toLowerCase()
						.replaceAll("[^0-9a-z\\sA-Z/\\-]", "");
				String groundEnt = td.groundtruth.getGroundTruth().get(j);
				if (mentionToEntitiesMap.containsKey(groundMent)) {
					if (mentionToEntitiesMap.get(groundMent)
							.contains(groundEnt) || groundEnt == null)
						recallCount++;
				}
			}
			double recallFraction = (double) recallCount
					/ (double) totalgroundUniqueMentions;
			// System.out.println("recall for document  "+
			// td.groundtruth.filename + "  is  "+recallFraction);
			f1.write(td.groundtruth.filename + "\t" + recallFraction);
			f1.write("\n");
			f1.close();
		} catch (Exception e) {
		}
	}

	public static void recallForSpottingWithUniqueMentions(TrainingData td) {
		try {
			FileWriter f1 = new FileWriter("check/" + maxNodes + "_"
					+ consolidationSize + "_" + csize + "_"
					+ "recallWithUniqueMention.txt", true);
			HashMap<String, ArrayList<String>> mentionToEntitiesMap = new HashMap<String, ArrayList<String>>();
			for (NodePotentials np : td.nodes.potentials_set) {
				String mentionName = np.mention.split("_")[0].toLowerCase()
						.replaceAll("[^0-9a-z\\sA-Z/\\-]", "");
				if (mentionToEntitiesMap.get(mentionName) == null)
					mentionToEntitiesMap.put(mentionName,
							new ArrayList<String>());
				mentionToEntitiesMap.get(mentionName).add(np.name);

			}

			// for(String key : mentionToEntitiesMap.keySet()){
			// System.out.println("key  "+key);
			// for(int j=0; j<mentionToEntitiesMap.get(key).size();j++)
			// System.out.println(mentionToEntitiesMap.get(key).get(j));
			//
			// }
			int recallCount = 0;
			HashMap<String, String> groundm2eMap = new HashMap<String, String>();

			for (int j = 0; j < td.groundtruth.getGroundMentionNames().size(); j++) {
				String groundMent = td.groundtruth.getGroundMentionNames()
						.get(j).trim().toLowerCase()
						.replaceAll("[^0-9a-z\\sA-Z/\\-]", "");
				String groundEnt = td.groundtruth.getGroundTruth().get(j);
				groundm2eMap.put(groundMent, groundEnt);
			}

			int totalgroundUniqueMentions = groundm2eMap.size();
			int entity = 0;
			int mention = 0;
			for (String groundKey : groundm2eMap.keySet()) {
				if (mentionToEntitiesMap.containsKey(groundKey)) {
					if (mentionToEntitiesMap.get(groundKey).contains(
							groundm2eMap.get(groundKey))
							|| groundm2eMap.get(groundKey) == null) {
						recallCount++;
					} else {
						entity++;
						// System.out.println("not matched entity    "+groundKey+"       "+groundm2eMap.get(groundKey));
					}
				} else {
					mention++;
					// System.out.println("not matched mention    "+groundKey+"       "+groundm2eMap.get(groundKey));
				}
			}

			// System.out.println("not matched entity  "+entity+"   not matched mention  "+mention+"   matched mention-entity  "+recallCount+"   total "+totalgroundUniqueMentions);
			double recallFraction = (double) recallCount
					/ (double) totalgroundUniqueMentions;
			// System.out.println("recall for document  "+
			// td.groundtruth.filename + "  is  "+recallFraction);
			f1.write(entity + "\t" + mention + "\t" + recallCount + "\t"
					+ totalgroundUniqueMentions + "\t"
					+ td.groundtruth.filename + "\t" + recallFraction);
			f1.write("\n");
			f1.close();
		} catch (Exception e) {

		}

	}

	public static void recallWithoutMentions(TrainingData trainData) {
		try {
			FileWriter f1 = new FileWriter("check/" + maxNodes + "_"
					+ consolidationSize + "_" + csize + "_"
					+ "recallWithoutMentions.txt", true);
			KeywordsGroundTruth kws = trainData.groundtruth;
			ArrayList<NodePotentials> temp = new ArrayList<NodePotentials>(
					trainData.nodes.potentials_set);
			// FileWriter f2 = new
			// FileWriter("check/"+"Candidates"+kws.filename);
			// for(NodePotentials n : temp){
			// f2.write(n.mention.split("_")[0]+"	"+Integer.parseInt(n.mention.split("_")[1])+"	"+
			// n.name+"\n");
			// }
			// f2.close();
			ArrayList<String> luceneEntityList = new ArrayList<String>();
			List<NodePotentials> np = trainData.nodes.potentials_set;
			for (NodePotentials n : np) {
				// System.out.println(n.mention + ": " + n.name);
				luceneEntityList.add(n.name);
			}
			ArrayList<String> groundList1 = kws.getGroundTruth();
			ArrayList<String> groundList = new ArrayList<String>();
			for (String k : groundList1) {
				if (k != null)
					groundList.add(k);
			}
			ArrayList<String> overlapList = intersection(luceneEntityList,
					groundList);

			// System.out.println("Retrieved list size : "+luceneEntityList.size()+" GR Size :  "+groundList.size()+" overlap lit size : "
			// +overlapList.size()+" Hit ratio : "+(double)overlapList.size()/(double)groundList.size());
			//
			// System.out.println("Writing recall.....");
			f1.write(luceneEntityList.size() + "\t" + groundList.size() + "\t"
					+ overlapList.size() + "\t"
					+ trainData.groundtruth.filename + "\t"
					+ (double) overlapList.size() / (double) groundList.size());
			f1.write("\n");
			f1.close();

			// writing the entites retrieved along with the common entities with
			// ground truth in a file for analysis purpose
			// FileWriter f0 = new FileWriter("check/"+kws.filename);
			// for (int k=0; k<overlapList.size(); k++) {
			// f0.write(overlapList.get(k));
			// f0.write("\t");
			// f0.write(overlapList.get(k));
			// f0.write("\t");
			// f0.write(overlapList.get(k));
			// f0.write("\n");
			//
			// }
			// for(int k=0;k<groundList.size();k++){
			// if(overlapList.contains(groundList.get(k)))
			// continue;
			// else{
			// f0.write("");
			// f0.write("\t");
			// f0.write(groundList.get(k));
			// f0.write("\t");
			// f0.write("");
			// f0.write("\n");
			// }
			//
			// }
			// for(int k=0;k<luceneEntityList.size();k++){
			// if(overlapList.contains(luceneEntityList.get(k)))
			// continue;
			// else{
			// f0.write(luceneEntityList.get(k));
			// f0.write("\t");
			// f0.write("");
			// f0.write("\t");
			// f0.write("");
			// f0.write("\n");
			// }
			//
			// }
			// f0.close();

		} catch (Exception e) {
			System.out.println("Error in recallAnalyzer : " + e.getMessage());
		}
	}

	public static float calcOutlinkSim(HashMap<Integer, Integer> out1,
			HashMap<Integer, Integer> out2) {
		int common = 0;
		int diff = 0;
		for (Integer id1 : out1.keySet()) {
			if (out2.containsKey(id1)) {
				common++;
			} else {
				diff++;
			}
		}
		float num = 2 * common;
		float deno = diff + common + out2.size();
		return num / deno;
	}

	public static ArrayList<String> intersection(ArrayList<String> list1,
			ArrayList<String> list2) {

		ArrayList<String> list = new ArrayList<String>();

		for (String str : list2) {
			if (list1.contains(str)) {
				list.add(str);
			}
		}

		return list;
	}

}
