package Evaluation;

import it.unimi.dsi.util.Interval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import edu.stanford.nlp.util.XMLUtils.XMLTag;

import util.DisambProperties;
import util.XMLTagInfo;

public class Evaluator {
	private HashMap<Mention, ArrayList<Entity>> mentionEntitiesMap;
	private String docName;
	private Statistics stats;

	public Evaluator(HashMap<Mention, ArrayList<Entity>> m2eMap, String docname) {
		mentionEntitiesMap = new HashMap<Mention, ArrayList<Entity>>();
		mentionEntitiesMap = m2eMap;
		docName = docname;
		stats = new Statistics();
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, ArrayList<XMLTagInfo>> loadFromFile(String file)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruth = (HashMap<String, ArrayList<XMLTagInfo>>) ois
				.readObject();
		ois.close();
		return fileGroundTruth;
	}

	public boolean contains(Interval bigger, Interval smaller) {
		return bigger.left <= smaller.left && bigger.right >= smaller.right;
	}

	public boolean disjoint(Interval span1, Interval span2) {
		return span1.right < span2.left || span2.right < span1.left;
	}

	public boolean overlaps(Interval span1, Interval span2) {
		return !disjoint(span1, span2);
	}

	public ArrayList<String> checkMention(Mention mention,
			ArrayList<XMLTagInfo> groundMentionList) {
		Interval spottedInterval = Interval.valueOf(mention.offset,
				mention.offset + mention.name.length() - 1);
		ArrayList<String> entNames = new ArrayList<String>();
		for (int i = 0; i < groundMentionList.size(); i++) {
			if (overlaps(groundMentionList.get(i).interval, spottedInterval)) {
				if (groundMentionList.get(i).wikiEntity != null
						&& !groundMentionList.get(i).wikiEntity
								.equalsIgnoreCase("NA"))
					entNames.add(groundMentionList.get(i).wikiEntity);
			}
		}
		if (entNames.size() != 0)
			return entNames;
		else
			return null;
	}

	public int checkNAMention(Mention mention,
			ArrayList<XMLTagInfo> groundMentionList) {
		Interval spottedInterval = Interval.valueOf(mention.offset,
				mention.offset + mention.name.length() - 1);
		int result = 0;
		for (int i = 0; i < groundMentionList.size(); i++) {
			if (overlaps(groundMentionList.get(i).interval, spottedInterval)) {
				result = 1;
				if (groundMentionList.get(i).wikiEntity == null
						|| groundMentionList.get(i).wikiEntity
								.equalsIgnoreCase("NA")) {
					result = 2;
					break;
				}
			}
		}
		return result;
	}

	// pass as parameter : "/mnt/bag1/kanika/KddGroundTruth.object"

	public Statistics KddEvaluator(String groundObjectfileName) {
		System.out.println(groundObjectfileName + " " + docName);
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = new HashMap<String, ArrayList<XMLTagInfo>>();
		ArrayList<XMLTagInfo> groundInfo = new ArrayList<XMLTagInfo>();
		try {
			fileGroundTruthMap = loadFromFile(groundObjectfileName);

			groundInfo = fileGroundTruthMap.get(docName);
			for (int i = 0; i < groundInfo.size(); i++) {
				Interval interval = Interval
						.valueOf(groundInfo.get(i).offset,
								groundInfo.get(i).offset
										+ groundInfo.get(i).length - 1);
				groundInfo.get(i).interval = interval;
			}
			Collections.sort(groundInfo);

		} catch (Exception e) {
			e.printStackTrace();
		}

		int totalNonNASpotsInGroundTruth = 0;
		for (int i = 0; i < groundInfo.size(); i++) {
			if (groundInfo.get(i).wikiEntity != null
					&& !groundInfo.get(i).wikiEntity.equalsIgnoreCase("NA"))
				totalNonNASpotsInGroundTruth++;
		}

		for (Mention mention : mentionEntitiesMap.keySet()) {
			if (!mention.isNA) {
				ArrayList<String> groundEntities = checkMention(mention,
						groundInfo);
				if (groundEntities != null) {
					Boolean someEntityAttached = false;
					for (int i = 0; i < mentionEntitiesMap.get(mention).size(); i++) {
						Entity entity = mentionEntitiesMap.get(mention).get(i);
						if (entity.predictedLabel == 1
								&& groundEntities.contains(entity.name)) {
							stats.incrementTruePos();
							someEntityAttached = true;
						}
						if (entity.predictedLabel == 1
								&& !groundEntities.contains(entity.name)) {
							stats.incrementFalsePos();
							someEntityAttached = true;
						}
					}
					if (someEntityAttached == false) {
						stats.incrementFalsePos();
					}
				}
			}
		}
		stats.setFalseNeg(totalNonNASpotsInGroundTruth - stats.getTruePos());
		return stats;
	}

	public Statistics DisambiguationEvaluator(String groundObjectfileName) {
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = new HashMap<String, ArrayList<XMLTagInfo>>();
		ArrayList<XMLTagInfo> groundInfo = new ArrayList<XMLTagInfo>();
		int totalNonNASpotsInGroundTruth = 0;
		try {
			fileGroundTruthMap = loadFromFile(groundObjectfileName);
			groundInfo = fileGroundTruthMap.get(docName);

			for (int i = 0; i < groundInfo.size(); i++) {
				Interval interval = Interval
						.valueOf(groundInfo.get(i).offset,
								groundInfo.get(i).offset
										+ groundInfo.get(i).length - 1);
				groundInfo.get(i).interval = interval;
			}
			Collections.sort(groundInfo);

			for (int i = 0; i < groundInfo.size(); i++) {
				if (groundInfo.get(i).wikiEntity != null
						&& !groundInfo.get(i).wikiEntity.equalsIgnoreCase("NA"))
					totalNonNASpotsInGroundTruth++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (Mention mention : mentionEntitiesMap.keySet()) {
			if (!mention.isNA) {
				// list of ground entities if the spotted mention overlaps with
				// some ground mention
				ArrayList<String> groundEntities = checkMention(mention,
						groundInfo);
				// evaluate only if the spotted mention overlaps with some
				// ground mention
				if (groundEntities != null) {
					for (int i = 0; i < mentionEntitiesMap.get(mention).size(); i++) {
						Entity entity = mentionEntitiesMap.get(mention).get(i);
						if (entity.trueLabel == 1 && entity.predictedLabel == 1) {
							stats.incrementTruePos();
						} else if (entity.trueLabel == 0
								&& entity.predictedLabel == 1) {
							stats.incrementFalsePos();
						} else if (entity.trueLabel == 0
								&& entity.predictedLabel == 0) {
							stats.incrementTrueNeg();
						} else if (entity.trueLabel == 1
								&& entity.predictedLabel == 0) {
							stats.incrementFalseNeg();
						}
					}
				}
			}
		}

		return stats;
	}

	public Statistics CompleteEvaluator(String groundObjectfileName) {
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = new HashMap<String, ArrayList<XMLTagInfo>>();
		ArrayList<XMLTagInfo> groundInfo = new ArrayList<XMLTagInfo>();
		ArrayList<String> groundEntity = new ArrayList<String>();
		int totalNonNASpotsInGroundTruth = 0;
		try {
			fileGroundTruthMap = loadFromFile(groundObjectfileName);
			groundInfo = fileGroundTruthMap.get(docName);
			if (null == groundInfo)
				groundInfo = fileGroundTruthMap.get("KDD_" + docName);
			for (int i = 0; i < groundInfo.size(); i++) {
				Interval interval = Interval
						.valueOf(groundInfo.get(i).offset,
								groundInfo.get(i).offset
										+ groundInfo.get(i).length - 1);
				groundInfo.get(i).interval = interval;
			}
			Collections.sort(groundInfo);
			XMLTagInfo info = null;
			for (int i = 0; i < groundInfo.size(); i++) {
				info = groundInfo.get(i);
				// System.out.println(info.offset + ","
				// + (info.offset + info.length) + "," + info.mention
				// + "," + info.wikiEntity);
				if (info.wikiEntity != null
						&& !info.wikiEntity.equalsIgnoreCase("NA")) {
					groundEntity.add(info.wikiEntity);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		ArrayList<String> candidateEntities = new ArrayList<String>();

		for (Mention mention : mentionEntitiesMap.keySet()) {
			// System.out.println("Mention : " + mention.name + ", NA? "
			// + mention.isNA + " Offset : " + mention.offset);
			if (!mention.isNA) {
				ArrayList<String> groundEntities = checkMention(mention,
						groundInfo);
				// System.out.println("ground entities null? "
				// + (null == groundEntities));
				if (groundEntities != null) {

					for (int i = 0; i < mentionEntitiesMap.get(mention).size(); i++) {
						Entity entity = mentionEntitiesMap.get(mention).get(i);
						candidateEntities.add(entity.name);
						if (entity.trueLabel == 1 && entity.predictedLabel == 1) {
							stats.incrementTruePos();
						} else if (entity.trueLabel == 0
								&& entity.predictedLabel == 1) {
							stats.incrementFalsePos();
						} else if (entity.trueLabel == 0
								&& entity.predictedLabel == 0) {
							stats.incrementTrueNeg();
						} else if (entity.trueLabel == 1
								&& entity.predictedLabel == 0) {
							stats.incrementFalseNeg();
						}

					}
				} else { // mention is NA
					int result = checkNAMention(mention, groundInfo);
					if (result != 0)
						stats.incrementTotalNA();
					if (result == 2)
						stats.incrementTrueNA();
				}
			}
		}

		for (String groundEnt : groundEntity) {
			if (!candidateEntities.contains(groundEnt)) {
				stats.incrementFalseNeg();
				stats.incrementSpotterError();
			}
		}

		return stats;
	}

	private ArrayList<XMLTagInfo> getGroundInfo(String groundObjectfileName)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		ArrayList<XMLTagInfo> groundInfo = new ArrayList<XMLTagInfo>();
		HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMap = loadFromFile(groundObjectfileName);
		groundInfo = fileGroundTruthMap.get(docName);
		if (null == groundInfo)
			groundInfo = fileGroundTruthMap.get("KDD_" + docName);
		for (int i = 0; i < groundInfo.size(); i++) {
			Interval interval = Interval.valueOf(groundInfo.get(i).offset,
					groundInfo.get(i).offset + groundInfo.get(i).length - 1);
			groundInfo.get(i).interval = interval;
		}
		Collections.sort(groundInfo);
		return groundInfo;
	}

	private ArrayList<XMLTagInfo> consolidateGroundInfo(
			ArrayList<XMLTagInfo> curatedGroundInfo,
			ArrayList<XMLTagInfo> originalGroundInfo) {
		ArrayList<XMLTagInfo> groundInfo = new ArrayList<XMLTagInfo>(
				curatedGroundInfo);
		for (XMLTagInfo info : originalGroundInfo) {
			if (!XMLTagInfo.contains(groundInfo, info))
				groundInfo.add(info);
		}
		Collections.sort(groundInfo);
		return groundInfo;
	}

	public Statistics CompleteEvaluator(String groundObjectfileName,
			String originalGroundObject) {
		ArrayList<XMLTagInfo> groundInfo = null;
		ArrayList<String> groundEntity = new ArrayList<String>();

		try {
			ArrayList<XMLTagInfo> curatedGroundInfo = getGroundInfo(groundObjectfileName);
			ArrayList<XMLTagInfo> originalGroundInfo = getGroundInfo(originalGroundObject);
			groundInfo = consolidateGroundInfo(curatedGroundInfo,
					originalGroundInfo);

			XMLTagInfo info = null;
			for (int i = 0; i < groundInfo.size(); i++) {
				info = groundInfo.get(i);
				// System.out.println(info.offset + ","
				// + (info.offset + info.length) + "," + info.mention
				// + "," + info.wikiEntity);
				if (info.wikiEntity != null
						&& !info.wikiEntity.equalsIgnoreCase("NA")) {
					groundEntity.add(info.wikiEntity);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		ArrayList<String> candidateEntities = new ArrayList<String>();

		for (Mention mention : mentionEntitiesMap.keySet()) {
			// System.out.println("Mention : " + mention.name + ", NA? "
			// + mention.isNA);
			if (!mention.isNA) {
				ArrayList<String> groundEntities = checkMention(mention,
						groundInfo);
				// System.out.println("ground entities null? "
				// + (null == groundEntities));
				if (groundEntities != null) {

					for (int i = 0; i < mentionEntitiesMap.get(mention).size(); i++) {
						Entity entity = mentionEntitiesMap.get(mention).get(i);
						candidateEntities.add(entity.name);
						if (entity.trueLabel == 1 && entity.predictedLabel == 1) {
							stats.incrementTruePos();
						} else if (entity.trueLabel == 0
								&& entity.predictedLabel == 1) {
							stats.incrementFalsePos();
						} else if (entity.trueLabel == 0
								&& entity.predictedLabel == 0) {
							stats.incrementTrueNeg();
						} else if (entity.trueLabel == 1
								&& entity.predictedLabel == 0) {
							stats.incrementFalseNeg();
						}

					}
				}
			}
		}

		for (String groundEnt : groundEntity) {
			if (!candidateEntities.contains(groundEnt)) {
				stats.incrementFalseNeg();
				stats.incrementSpotterError();
			}
		}

		return stats;
	}

	public static void main(String[] args) {
		// String mapFolder = args[0];
		String mapFolder = "/Users/ashish/wiki/orig_vs_cur_analysis/maps_cur";
		ObjectInputStream ois = null;
		HashMap<Mention, ArrayList<Entity>> m2eMap = null;
		Evaluator eval = null;
		String docname = null;
		ArrayList<Statistics> statsList = new ArrayList<Statistics>();
		Statistics stat = null;
		try {
			// DisambProperties.init(args[1]);
			for (File f : new File(mapFolder).listFiles()) {
				docname = f.getName().substring(0, f.getName().indexOf('.'));
				docname = docname.startsWith("yn") ? docname : docname + ".txt";
				ois = new ObjectInputStream(new FileInputStream(f));
				m2eMap = (HashMap<Mention, ArrayList<Entity>>) ois.readObject();
				// for (Mention m : m2eMap.keySet()) {
				// System.out.print(m.name + "(" + m.offset + ") " + "-->");
				// for (Entity e : m2eMap.get(m))
				// System.out.print(e.name + ", ");
				// System.out.println();
				// }
				eval = new Evaluator(m2eMap, docname);
				// stat = eval.CompleteEvaluator(DisambProperties.getInstance()
				// .getkddCuratedObjectFolder() + docname + ".xml.object");
				stat = eval
						.CompleteEvaluator(
								"/Users/ashish/wiki/orig_vs_cur_analysis/kddCuratedGroundFilesNew/"
										+ docname + ".xml.object",
								"/Users/ashish/wiki/orig_vs_cur_analysis/KddGroundTruth.object");
				// stat = eval
				// .CompleteEvaluator("/Users/ashish/wiki/orig_vs_cur_analysis/KddGroundTruth.object");
				statsList.add(stat);
				System.out.println(docname + ", " + stat.getTruePos() + ", "
						+ stat.getFalsePos() + ", " + stat.getFalseNeg());
			}
			System.out.println("Micro Recall : "
					+ Statistics.getMicroRecall(statsList));
			System.out.println("Micro Precision : "
					+ Statistics.getMicroPrecision(statsList));
			System.out.println("Micro F1 : "
					+ Statistics.getMicroFmeasure(statsList));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
