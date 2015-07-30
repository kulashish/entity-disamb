package spotting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Redirect;

import util.DisambProperties;
import util.ParseXML;
import util.XMLTagInfo;
import wikiGroundTruth.server.ClientWikisauras;

public class CollectiveTrainingTemp {

	static Wikisaurus thesaurus = null;
	private String trainDir;
	private static final Logger logger = Logger.getLogger("spotting");
	private String groundFilenameWiki;
	private String groundFilenameManual;
	private String filename;
	private TrainingData trainData;
	int maxNodes = 6;
	int consolidationSize = 5;
	private ClientWikisauras clientWikisauras;
	private HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapManual;
	static HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapWiki = new HashMap<String, ArrayList<XMLTagInfo>>();

	public static boolean kdd = true;

	public CollectiveTrainingTemp(String dirOfDocument, String groundWikifile,
			String groundManualFile, String docname) {
		if (Config.Server)
			clientWikisauras = new ClientWikisauras();
		trainDir = dirOfDocument;
		groundFilenameManual = groundManualFile;
		groundFilenameWiki = groundWikifile;
		// System.out.println(groundFilenameWiki);
		filename = docname;
		trainData = new TrainingData();
		fileGroundTruthMapManual = new HashMap<String, ArrayList<XMLTagInfo>>();
		ParseXML pm = new ParseXML();
		try {
			fileGroundTruthMapWiki = pm.parseXML(groundFilenameWiki);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String readFromFile() {
		String str = "";
		try {
			String sCurrentLine;

			FileReader f = new FileReader(trainDir + filename);
			BufferedReader br = new BufferedReader(f);
			int line = 0;
			while ((sCurrentLine = br.readLine()) != null) {
				if (line == 0)
					str += sCurrentLine;
				else
					str += " " + sCurrentLine;
			}
			f.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return str;
	}

	/*
	 * Uses spotter to spot and the ground entities are not included explicitly
	 */
	public void spotterNew() {
		logger.log(Level.INFO, "Entred spotterNew");
		String queryString = readFromFile();
		ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
		KeywordsGroundTruth kw;
		try {
			kw = kw_extractor.extractFromString(queryString,
					new ArrayList<String>(), Config.contextSize);
			logger.log(Level.INFO, "extractFromString done!");
			DisambProperties props = DisambProperties.getInstance();
			LuceneIndexWrapper luceneIndex = new LuceneIndexWrapper(
					props.getCompleteIndex(), props.getRedirectIndex(),
					props.getInlinkIndex(), props.getDisambIndex(),
					props.getAnchorIndex());
			TrainingData result = new TrainingData();
			result.nodes = luceneIndex.extractNodesNewConsolidation(kw,
					maxNodes, consolidationSize, filename);
			logger.log(Level.INFO,
					"luceneIndex.extractNodesNewConsolidation done!");
			if (result.nodes == null) {
				System.out
						.println("CollectiveTrainingTemp.spotterNew::result NodePotentialSet::null");
			}
			result.groundtruth = kw;

			if (!"".equals(groundFilenameManual)) {
				ParseXML pm1 = new ParseXML();
				fileGroundTruthMapManual = pm1.parseXML(groundFilenameManual);
				logger.log(Level.INFO, "parsed groundFilenameManual done!");
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

			Iterator<XMLTagInfo> it = fileGroundTruthMapWiki.get(filename)
					.iterator();
			while (it.hasNext()) {
				if (it.next().offset >= kw.getDocumentText().length() - 1) {
					it.remove();
				}
			}

			if (fileGroundTruthMapWiki.containsKey(filename))
				kw.setGroundTruth(fileGroundTruthMapWiki.get(filename));
			if (fileGroundTruthMapManual != null)
				kw.setGroundTruth(fileGroundTruthMapManual.get(filename));

			ArrayList<String> groundtruth = kw.getGroundTruth();
			ArrayList<String> groundtruthWithRedirections = new ArrayList<String>();
			if (!Config.Server && thesaurus == null) {
				thesaurus = new Wikisaurus();
			}
			for (int i = 0; i < groundtruth.size(); i++) {
				if (null != groundtruth.get(i)) {
					if (!Config.Server) {
						groundtruthWithRedirections.add(groundtruth.get(i)
								.replaceAll("_", " "));

						Article article = Wikisaurus._wikipedia
								.getArticleByTitle(groundtruth.get(i)
										.replaceAll("_", " "));
						Redirect[] redirects = null;
						if (article != null)
							redirects = article.getRedirects();
						if (redirects != null) {
							for (int j = 0; j < redirects.length; j++) {
								groundtruthWithRedirections.add(redirects[j]
										.getTitle().replaceAll("_", " "));
							}
						}
					} else {
						groundtruthWithRedirections.addAll(clientWikisauras
								.getArticle(groundtruth.get(i).replaceAll("_",
										" ")));
					}
				}
			}
			for (NodePotentials np : result.nodes.potentials_set) {
				ArrayList<String> candidateEntitiesWithRedirects = new ArrayList<String>();

				if (Config.Server) {
					candidateEntitiesWithRedirects.addAll(clientWikisauras
							.getArticle(np.name.replaceAll("_", " ")));
				} else {
					candidateEntitiesWithRedirects.add(np.name.replaceAll("_",
							" "));
					Article article = Wikisaurus._wikipedia
							.getArticleByTitle(np.name.replaceAll("_", " "));
					Redirect[] redirects = null;
					if (article != null)
						redirects = article.getRedirects();
					if (redirects != null) {
						for (int j = 0; j < redirects.length; j++) {
							candidateEntitiesWithRedirects.add(redirects[j]
									.getTitle().replaceAll("_", " "));
						}
					}
				}
				for (int i = 0; i < groundtruthWithRedirections.size(); i++) {
					if (candidateEntitiesWithRedirects
							.contains(groundtruthWithRedirections.get(i)))
						np.label = 1;
				}
			}
			logger.log(Level.INFO, "label assignment done!");

			trainData = result;

			// for (NodePotentials np : trainData.nodes.potentials_set) {
			// System.out.println(np.label + "      " + np.mention + "       "
			// + np.name);
			// }

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.log(Level.INFO, "Exiting spotterNew");

	}

	// public void spotter() {
	//
	// ExtractKeywordsGroundTruth kw_extractor = new
	// ExtractKeywordsGroundTruth();
	// KeywordsGroundTruth kw;
	// try {
	//
	// kw = kw_extractor.extractForTrainingwithGroundTruth(trainDir,
	// filename, groundFilenameWiki, groundFilenameManual, csize);
	// FeatureExtractor ft_extractor = new FeatureExtractor();
	//
	// trainData = ft_extractor.extractFeaturesForTraining(kw, maxNodes,
	// consolidationSize);
	//
	// // for (NodePotentials np : trainData.nodes.potentials_set) {
	// // System.out.println(np.label + " " + np.mention + "        "
	// // + np.name + "  " + np.context_score_frequent +
	// // "  "+np.context_score_synopsis+" "+np.context_score_vbdj+" "
	// // + np.inlink_count + " " + np.outlink_count + " "
	// // + np.page_title_score+" "+np.disambiguation+" "+np.redirection);
	// // System.out.println(np.name+" "+np.label + " " + np.mention +
	// // "        "+np.sense_probability);
	// // }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }

	// public void edgeFeatures() {
	//
	// for (int j = 0; j < trainData.nodes.potentials_set.size(); j++) {
	// for (int k = 0; k < trainData.nodes.potentials_set.size(); k++) {
	// if (j != k) {
	// double catScore = ComputeCategorySimilarity
	// .computeCategSim(trainData.nodes.potentials_set
	// .get(j).name.replaceAll(" ", "_"),
	// trainData.nodes.potentials_set.get(k).name
	// .replaceAll(" ", "_"));
	//
	// double outLinkScore = FeatureExtractor.calcOutlinkSim(
	// trainData.nodes.potentials_set.get(j).outLinks,
	// trainData.nodes.potentials_set.get(k).outLinks);
	//
	// double inLinkScore = FeatureExtractor.calcInlinkSim(
	// trainData.nodes.potentials_set.get(j).inLinks,
	// trainData.nodes.potentials_set.get(k).inLinks);
	//
	// double contextScoreFreq = FeatureExtractor.calcContextSim(
	// trainData.nodes.potentials_set.get(j).bagOfWords_frequent,
	// trainData.nodes.potentials_set.get(k).bagOfWords_frequent,
	// trainData.nodes.potentials_set.get(j).idf_frequent,
	// trainData.nodes.potentials_set.get(k).idf_frequent);
	//
	// double contextScoreSyn = FeatureExtractor.calcContextSim(
	// trainData.nodes.potentials_set.get(j).bagOfWords_synopsis,
	// trainData.nodes.potentials_set.get(k).bagOfWords_synopsis,
	// trainData.nodes.potentials_set.get(j).idf_synopsis,
	// trainData.nodes.potentials_set.get(k).idf_synopsis);
	//
	// double contextScoreSynAdj = FeatureExtractor.calcContextSim(
	// trainData.nodes.potentials_set.get(j).bagOfWords_synopsis_vbadj,
	// trainData.nodes.potentials_set.get(k).bagOfWords_synopsis_vbadj,
	// trainData.nodes.potentials_set.get(j).idf_synopsis_vbadj,
	// trainData.nodes.potentials_set.get(k).idf_synopsis_vbadj);
	//
	// }
	// }
	// }
	//
	// }

	public static void main(String[] args) {
		double max[] = new double[Config.NODE_FEATURE_DIM];
		double min[] = new double[Config.NODE_FEATURE_DIM];
		Arrays.fill(max, 0.0d);
		Arrays.fill(min, Double.POSITIVE_INFINITY);

		// String csvFile = "/mnt/bag1/kanika/csvFilesUnNormalized/file" +
		// args[0]
		// + ".csv";
		// String filename = "/mnt/bag1/kanika/normalizationBounds/file" +
		// args[0]
		// + ".txt";

		String csvFile = "/mnt/bag1/kanika/csvFilesWikiCorrected/temp"
				+ args[0] + ".csv";
		String filename = "/mnt/bag1/kanika/csvFilesWikiCorrected/tempset"
				+ args[0] + ".txt";

		String propsFile = args[1];
		try {
			DisambProperties.init(propsFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(csvFile);
		ArrayList<TrainingData> trainDataList = new ArrayList<TrainingData>();
		// File folder = new File("/mnt/bag1/kanika/train" + args[0] + "/");
		File folder = new File("/home/kanika/wikiTraining/temporary/");
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String docName = listOfFiles[i].getName();
				long start = System.currentTimeMillis();

				CollectiveTrainingTemp ct1 = new CollectiveTrainingTemp(
						// "/mnt/bag1/kanika/train" + args[0] + "/", //
						"/home/kanika/wikiTraining/temporary/",
						"/mnt/bag1/kanika/wikipediaGroundtruth.xml",
						"/home/kanika/wikiTraining/annotationXmls/" + docName
								+ ".xml", docName);

				System.out.println(docName);
				// CollectiveTrainingTemp ct1 = new
				// CollectiveTrainingTemp("/home/kanika/wikiTraining/annotated/","/mnt/bag1/kanika/wikipediaGroundtruth.xml"
				// ,"/home/kanika/wikiTraining/xmls/" + docName + ".xml",
				// docName);

				ct1.spotterNew(); // call spotter function;

				long elapsedTimeMillis = System.currentTimeMillis() - start;
				float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);

				for (NodePotentials np : ct1.trainData.nodes.potentials_set) {
					if (np.anchorTextTerms == null)
						np.anchor_text_cosine = 0d;
					else {
						np.anchor_text_cosine = FeatureExtractor.calcCosine(
								np.anchorTextTerms, np.anchorTextTermsTf,
								np.anchorTextTermsIdf,
								np.contextTermsForInput2TfIdfAnchorText);
					}

					if (np.anchorTextContextTerms == null)
						np.anchor_text_context_cosine = 0d;
					else {
						np.anchor_text_context_cosine = FeatureExtractor
								.calcCosine(
										np.anchorTextContextTerms,
										np.anchorTextContextTermsTf,
										np.anchorTextContextTermsIdf,
										np.contextTermsForInput2TfIdfAnchorTextContext);
					}

					if (np.fullTextTerms == null)
						np.full_text_cosine = 0d;
					else {
						np.full_text_cosine = FeatureExtractor.calcCosine(
								np.fullTextTerms, np.fullTextTermsTf,
								np.fullTextTermsIdf,
								np.contextTermsForInput2TfIdfFullText);
					}

					// System.out.println(np.name + "," + np.label + ","
					// + np.logistic_score + "," + np.logistic_label);

					// + np.sense_probability + "," + np.anchor_text_score
					// + "," + np.anchor_text_cosine + ","
					// + np.full_text_cosine + ","
					// + np.anchor_text_context_cosine + "," + np.label);
				}
				// if (Config.normalizePerMention) {
				// LuceneIndexWrapper
				// .normalizePerMention(ct1.trainData.nodes.potentials_set);
				// }

				for (NodePotentials n : ct1.trainData.nodes.potentials_set) {
					double[] f = LuceneIndexWrapper.fillNodeFeaturesVector(n);
					for (int j = 0; j < Config.NODE_FEATURE_DIM; j++) {
						if (f[j] > max[j])
							max[j] = f[j];
						if (f[j] < min[j])
							min[j] = f[j];
					}
				}
				System.out.println("For file " + docName
						+ " process completed in  " + elapsedTimeMin + " mins");
				System.out.println("finished " + i + " files");
				trainDataList.add(ct1.trainData);
				if (i % 20 == 0) {
					LucenePotentialEntities.writeToCSVList(trainDataList,
							csvFile);
					trainDataList.clear();
					System.out.println("written another 20 files till" + i);
				}
			}
		}
		LucenePotentialEntities.writeToCSVList(trainDataList, csvFile);
		LucenePotentialEntities.writeMinMax(min, max, filename);
		// Wikisaurus._wikipedia.close();

	}
	// private WikiNode createNode(NodePotentials np) {
	// WikiNode node = null;
	// // check if there is already a node with the same label
	// for (WikiNode n : mrfgraph.vertexSet())
	// if (n.getLabel().equalsIgnoreCase(np.name)) {
	// node = n;
	// break;
	// }
	// // if not then create a new node
	// if (null == node) {
	// node = new WikiNode(np.name);
	// double[] f = null;
	// node.setIncut(np.label == 1);
	// f = new double[NODE_FEATURE_DIM];
	// f[0] = np.context_score_frequent;
	// f[1] = np.context_score_synopsis;
	// f[2] = np.context_score_vbdj;
	// f[3] = np.inlink_count;
	// f[4] = np.outlink_count;
	// f[5] = np.redirection;
	// f[6] = np.page_title_score;
	// node.setfVector(f);
	// }
	// return node;
	// }

}
