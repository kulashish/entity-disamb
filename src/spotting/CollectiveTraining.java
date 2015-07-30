package spotting;

import in.ac.iitb.cse.mrf.data.AMNWeights;
import in.ac.iitb.cse.mrf.data.MRFGraph;
import in.ac.iitb.cse.mrf.data.WikiEdge;
import in.ac.iitb.cse.mrf.data.WikiNode;
import in.ac.iitb.cse.mrf.exception.LearningException;
import in.ac.iitb.cse.mrf.learn.MultiLearner;
import in.ac.iitb.cse.mrf.learn.State;
import in.ac.iitb.cse.mrf.util.LearningProperties;
import in.ac.iitb.cse.mrf.util.WikiEdgeProvider;
import in.ac.iitb.cse.mrf.util.WikiNodeProvider;
import it.unimi.dsi.util.Interval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.lucene.search.TopDocs;
import org.jgrapht.WeightedGraph;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.VertexNameProvider;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Redirect;
import org.xml.sax.SAXException;

import spotting.KeywordsGroundTruth.Mention;
import util.DisambProperties;
import util.GraphCreator;
import util.ParseXML;
import util.ParseXMLAQ;
import util.XMLTagInfo;
import wikiGroundTruth.server.ClientWikisauras;
import exception.TrainingException;

public class CollectiveTraining {
	private static final Logger logger = Logger.getLogger("spotting");
	public static boolean kdd = true;
	static Wikisaurus thesaurus = null;

	private String trainDir; // = "/home/kanika/wikiTraining/train1/";
	private String groundFilenameWiki; // = //
	private String datasetName;
	// "/home/kanika/wikiTraining/ground/wikipediaGroundtruth.xml";
	private String groundFilenameManual; // = //
	// "/home/kanika/wikiTraining/xmls/Mayan.xml";
	private String filename;
	private TrainingData trainData;
	int maxNodes = 20;
	int consolidationSize = 5;
	int csize = Config.filterChunkThreshold;
	private WeightedGraph<WikiNode, WikiEdge> mrfgraph;

	private HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapManual;
	static HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapWiki = new HashMap<String, ArrayList<XMLTagInfo>>();

	private ClientWikisauras clientWikisauras;
	
	public KeywordsGroundTruth wikiminerKeywords;

	public TrainingData getData() {
		return trainData;
	}

	public CollectiveTraining(String dirOfDocument, String groundWikifile,
			String groundManualFile, String docname, String datasetname) {
		if (Config.Server)
			clientWikisauras = new ClientWikisauras();
		trainDir = dirOfDocument;
		groundFilenameManual = groundManualFile;
		groundFilenameWiki = groundWikifile;
		datasetName = datasetname;
		filename = docname;
		trainData = new TrainingData();
		fileGroundTruthMapManual = new HashMap<String, ArrayList<XMLTagInfo>>();
		ParseXML pm = new ParseXML();
		try {
			if (!groundFilenameWiki.equals(""))
				fileGroundTruthMapWiki = pm.parseXML(groundFilenameWiki);
			else {
				fileGroundTruthMapWiki = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean contains(Interval bigger, Interval smaller) {
		//return bigger.left <= smaller.left && bigger.right >= smaller.right;
		return bigger.left < smaller.left && bigger.right > smaller.right;
	}

	public boolean disjoint(Interval span1, Interval span2) {
		return span1.right < span2.left || span2.right < span1.left;
	}

	public boolean overlaps(Interval span1, Interval span2) {
		return !disjoint(span1, span2);
	}

	public void spotterForText(String queryString) {
		logger.log(Level.INFO, "Entred spotterForText");
		//String queryString = readFromFile();
		System.out.println("querystring " + queryString);
		ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
		KeywordsGroundTruth kw1;
		KeywordsGroundTruth kw2;
		
		try {
			long kw1startTime = System.currentTimeMillis();
			kw1 = kw_extractor.extractWikiMiner(queryString,
					new ArrayList<String>(), Config.contextSize);
			long kw1endTime = System.currentTimeMillis();
			
			long diff1 = (kw1endTime - kw1startTime);

			System.out.println("Time taken by kw1 : " + diff1 + " milliseconds");
			
			wikiminerKeywords = kw1; 

//			System.out.println("Printing keywords kw1:");
//			for(Mention me : kw1.getKeywords()){
//				System.out.println(" name : " + me.name + " context: " + me.context);
//			}
			for (int i = 0; i < kw1.keywords.size(); i++) {
				byte[] kwBytes = kw1.keywords.get(i).name.getBytes(Charset.forName("UTF-8"));
				kw1.getKeywords().get(i).interval = Interval.valueOf(
						kw1.keywords.get(i).offset, kw1.keywords.get(i).offset
								+ kw1.keywords.get(i).length);
//						+ kwBytes.length);
			}

			//Trying to check whether using only kw1 can decrease the web service call time or not. using this extractor may not be that much useful for long text
//			System.out.println("going for kw2:");
			long kw2startTime = System.currentTimeMillis();
			kw2 = kw_extractor.extractFromString(queryString,
					new ArrayList<String>(), Config.contextSize);
			long kw2endTime = System.currentTimeMillis();
			
			long diff3 = (kw2endTime - kw2startTime);

			System.out.println("Time taken by kw2 : " + diff3 + " milliseconds");
			
			for (int i = 0; i < kw2.keywords.size(); i++) {
				kw2.getKeywords().get(i).interval = Interval.valueOf(
						kw2.keywords.get(i).offset, kw2.keywords.get(i).offset
								+ kw2.keywords.get(i).length);
			}
			
			System.out.println("Printing keywords kw2:");
			for(Mention me : kw2.getKeywords()){
				System.out.println(" name : " + me.name + " context: " + me.context);
			}
			
			LuceneSpotter ls = new LuceneSpotter(queryString,3);
			if(Config.useLuceneSpotter)
				ls.extractKeywords();
			KeywordsGroundTruth kw3 = ls.getKWGroundTruth();
			
			DatasetSpotter dsspotter = new DatasetSpotter(queryString,3);
			
			dsspotter.extractKeywords();
			KeywordsGroundTruth kw4 = dsspotter.getKWGroundTruth();
			//kw4.keywords.addAll(kw1.keywords);
			
			for (int i = 0; i < kw2.keywords.size(); i++) {
//				byte[] kwBytes = kw2.keywords.get(i).name.getBytes(Charset.forName("UTF-8"));
				kw2.getKeywords().get(i).interval = Interval.valueOf(
						kw2.keywords.get(i).offset, kw2.keywords.get(i).offset
								+ kw2.keywords.get(i).length);
//								+ kwBytes.length);
				boolean found = false;
				int j = 0;
				for (; j < kw1.keywords.size(); j++) {
					if (!disjoint(kw2.keywords.get(i).interval,
							kw1.keywords.get(j).interval)) {
						found = true;
						break;
					}
				}
				if (!found) {
					kw1.keywords.add(kw2.keywords.get(i));
				}
			}
			
			KeywordsGroundTruth kw5 = new KeywordsGroundTruth(queryString);
			
			for (int i = 0; i < kw1.keywords.size(); i++) {
				kw1.getKeywords().get(i).interval = Interval.valueOf(
						kw1.keywords.get(i).offset, kw1.keywords.get(i).offset + kw1.keywords.get(i).length);
				
				boolean found = false;
				int j = 0;
				for (;j < kw3.keywords.size(); j++) {
					kw3.getKeywords().get(j).interval = Interval.valueOf(
							kw3.keywords.get(j).offset, kw3.keywords.get(j).offset + kw3.keywords.get(j).length);
					if (!disjoint(kw1.keywords.get(i).interval,kw3.keywords.get(j).interval)) {
						found = true;
						break;
					}
				}
				if (!found) {
					kw5.keywords.add(kw1.keywords.get(i));
				}
				else{
					if(contains(kw3.keywords.get(j).interval, kw1.keywords.get(i).interval)){
						kw5.keywords.add(kw3.keywords.get(j));
					}
					else if(contains(kw1.keywords.get(i).interval, kw3.keywords.get(j).interval)){
						kw5.keywords.add(kw1.keywords.get(i));
					}
					else
						kw5.keywords.add(kw1.keywords.get(i));
				}
			}
			
			for (int i = 0; i < kw4.keywords.size(); i++) {
				kw4.getKeywords().get(i).interval = Interval.valueOf(
						kw4.keywords.get(i).offset, kw4.keywords.get(i).offset + kw4.keywords.get(i).length);
				
				boolean found = false;
				int j = 0;
				for (;j < kw5.keywords.size(); j++) {
					kw5.getKeywords().get(j).interval = Interval.valueOf(
							kw5.keywords.get(j).offset, kw5.keywords.get(j).offset + kw5.keywords.get(j).length);
					if (!disjoint(kw4.keywords.get(i).interval,kw5.keywords.get(j).interval)) {
						found = true;
						break;
					}
				}
				if (!found) {
					kw5.keywords.add(kw4.keywords.get(i));
				}
			}
			
			logger.log(Level.INFO, "extractFromString done!");
//			DisambProperties props = DisambProperties.getInstance();
//			logger.log(Level.INFO,"props object created!");
//			LuceneIndexWrapper luceneIndex = new LuceneIndexWrapper(
//					props.getCompleteIndex(), props.getRedirectIndex(),
//					props.getInlinkIndex(), props.getDisambIndex(),
//					props.getAnchorIndex());
//			logger.log(Level.INFO,"lucene index object created!");
			TrainingData result = new TrainingData();
			logger.log(Level.INFO,"calling extractNodesNewConsolidation!");
			//ArrayList<String> keywords = kw1.getMentionNames();
			//kw5 to be used. Its the consolidated one.
			ArrayList<String> keywords = kw5.getMentionNames();
			System.out.println("kw5 size : " + kw5.getMentionNames().size());
			
			/*
			int kwcount = keywords.size();
			int kwstart = 0;
			while(kwcount > 0){
				int threadpoolsize = (kwcount > 50 ? 50 : kwcount);
				
				ExecutorService pool = Executors.newFixedThreadPool(threadpoolsize);
			    Set<Future<NodePotentialsSet>> set = new HashSet<Future<NodePotentialsSet>>();
			    
				for (int i = kwstart; i < kwstart + threadpoolsize; i++) {
				      //Callable<NodePotentialsSet> callable = new IndexSearchCallable(kw1, Config.maxCandidates, consolidationSize,filename, i, luceneIndex);
					  Callable<NodePotentialsSet> callable = new IndexSearchCallable(kw1, Config.maxCandidates, consolidationSize,filename, i);
				      Future<NodePotentialsSet> future = pool.submit(callable);
				      set.add(future);
				}
				
				int cnt = 0;
				for (Future<NodePotentialsSet> future : set) {
					try {
						long lucstartTime = System.currentTimeMillis();
						NodePotentialsSet localresult = future.get();
						result.nodes.mention_queries.putAll(localresult.mention_queries);
						result.nodes.potentials_set.addAll(localresult.potentials_set);
						cnt++;
						//System.out.println("Cnt : " + cnt + " mention_queries : " + localresult.mention_queries.size() + " potentials_set : " + localresult.potentials_set.size());
						//System.out.println("Done with : " + cnt);
						long lucendTime = System.currentTimeMillis();
						
						long diff2 = (lucendTime - lucstartTime);

						System.out.println("Time taken by lucene for call " + cnt + " : " + diff2 + " milliseconds");

					} catch (ExecutionException ex) {
						ex.getCause().printStackTrace();
					}
				}
				kwcount = kwcount - threadpoolsize;
				kwstart = kwstart + threadpoolsize;
			}*/
			
			ExecutorService pool = Executors.newFixedThreadPool(keywords.size());
		    Set<Future<NodePotentialsSet>> set = new HashSet<Future<NodePotentialsSet>>();
		    
			for (int i = 0; i < keywords.size(); i++) {
				  Callable<NodePotentialsSet> callable = new IndexSearchCallable(kw5, Config.maxCandidates, consolidationSize,filename, i);
			      Future<NodePotentialsSet> future = pool.submit(callable);
			      set.add(future);
			}
			
			int cnt = 0;
			for (Future<NodePotentialsSet> future : set) {
				try {
					long lucstartTime = System.currentTimeMillis();
					NodePotentialsSet localresult = future.get();
					result.nodes.mention_queries.putAll(localresult.mention_queries);
					result.nodes.potentials_set.addAll(localresult.potentials_set);
					cnt++;
					//System.out.println("Cnt : " + cnt + " mention_queries : " + localresult.mention_queries.size() + " potentials_set : " + localresult.potentials_set.size());
					//System.out.println("Done with : " + cnt);
					long lucendTime = System.currentTimeMillis();
					
					long diff2 = (lucendTime - lucstartTime);

					//System.out.println("Time taken by lucene for call " + cnt + " : " + diff2 + " milliseconds");

				} catch (ExecutionException ex) {
					ex.getCause().printStackTrace();
				}
			}
			
			pool.shutdown();
//		    System.out.println("keywords size : " + keywords.size() + " cnt : " + cnt);
			
			/*
			for (int i = 0; i < keywords.size(); i++) {
				long lucstartTime = System.currentTimeMillis();
				LuceneIndexWrapper luceneIndex = new LuceneIndexWrapper();
				NodePotentialsSet npset = luceneIndex.extractNodesNewConsolidation_singleKW(kw1,Config.maxCandidates, consolidationSize, filename,i);
				result.nodes.mention_queries.putAll(npset.mention_queries);
				result.nodes.potentials_set.addAll(npset.potentials_set);
				
				long lucendTime = System.currentTimeMillis();
				
				long diff2 = (lucendTime - lucstartTime);

				System.out.println("Time taken by lucene for call " + (i+1) + " : " + diff2 + " milliseconds");
				//System.out.println("Cnt : " + (i+1) + " mention_queries : " + npset.mention_queries.size() + " potentials_set : " + npset.potentials_set.size());
			}*/
//			
		    System.out.println("Total mention_queries : " + result.nodes.mention_queries.size() + " potentials_set : " + result.nodes.potentials_set.size());
			//result.nodes = luceneIndex.extractNodesNewConsolidation(kw1,
			//result.nodes = luceneIndex.extractNodesNewConsolidation(kw2,
					//Config.maxCandidates, consolidationSize, filename);
			
			
			logger.log(Level.INFO,
					"luceneIndex.extractNodesNewConsolidation done!");
			if (result.nodes == null) {
				System.out
						.println("CollectiveTrainingTemp.spotterNew::result NodePotentialSet::null");
			}
			trainData = result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.log(Level.INFO, "Exiting spotterForText");
	}
	
	public class IndexSearchCallable implements Callable<NodePotentialsSet> {
		KeywordsGroundTruth keywordGroundTruth;
		int numNodes;
		int maxLength;
		String filename;
		int i;
		LuceneIndexWrapper obj;

		public IndexSearchCallable(KeywordsGroundTruth _keywordGroundTruth, int _numNodes,
				//int _maxLength, String _filename,int _i,LuceneIndexWrapper _obj) {
				int _maxLength, String _filename,int _i) {
			keywordGroundTruth = _keywordGroundTruth;
			numNodes = _numNodes;
			maxLength = _maxLength;
			filename = _filename;
			i = _i;
			long lucstartTime = System.currentTimeMillis();
			obj = new LuceneIndexWrapper();
			long lucendTime = System.currentTimeMillis();
			
			long diff2 = (lucendTime - lucstartTime);

			//System.out.println("Time taken for creating lucene object  " + (i+1) + " : " + diff2 + " milliseconds");
			
		}

		@Override
		public NodePotentialsSet call() throws Exception {
			return obj.extractNodesNewConsolidation_singleKW(keywordGroundTruth,numNodes, maxLength, filename,i);
		}
	}
	

	public void spotterWikiMiner() {
		logger.log(Level.INFO, "Entred spotterWikiMiner");
		String queryString = readFromFile();
		ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
		KeywordsGroundTruth kw1;
		KeywordsGroundTruth kw2;
		try {
			kw1 = kw_extractor.extractWikiMiner(queryString,
					new ArrayList<String>(), Config.contextSize);
			for (int i = 0; i < kw1.keywords.size(); i++) {
				kw1.getKeywords().get(i).interval = Interval.valueOf(
						kw1.keywords.get(i).offset, kw1.keywords.get(i).offset
								+ kw1.keywords.get(i).length);
			}
	
			kw2 = kw_extractor.extractFromString(queryString,
					new ArrayList<String>(), Config.contextSize);

			for (int i = 0; i < kw2.keywords.size(); i++) {
				kw2.getKeywords().get(i).interval = Interval.valueOf(
						kw2.keywords.get(i).offset, kw2.keywords.get(i).offset
								+ kw2.keywords.get(i).length);
				boolean found = false;
				for (int j = 0; j < kw1.keywords.size(); j++) {
					if (!disjoint(kw2.keywords.get(i).interval,
							kw1.keywords.get(j).interval)) {
						found = true;
					}
				}
				if (!found) {
					// System.out.println("added " + kw2.keywords.get(i).name
					// + " " + kw2.keywords.get(i).offset);
					kw1.keywords.add(kw2.keywords.get(i));

				}
			}
			logger.log(Level.INFO, "extractFromString done!");
			DisambProperties props = DisambProperties.getInstance();
			LuceneIndexWrapper luceneIndex = new LuceneIndexWrapper(
					props.getCompleteIndex(), props.getRedirectIndex(),
					props.getInlinkIndex(), props.getDisambIndex(),
					props.getAnchorIndex());
			TrainingData result = new TrainingData();
			result.nodes = luceneIndex.extractNodesNewConsolidation(kw1,
					Config.maxCandidates, consolidationSize, filename);
			logger.log(Level.INFO,
					"luceneIndex.extractNodesNewConsolidation done!");
			if (result.nodes == null) {
				System.out
						.println("CollectiveTrainingTemp.spotterNew::result NodePotentialSet::null");
			}
			result.groundtruth = kw1;

			if (datasetName.equals(props.getIITBdataset())
					|| datasetName.equals(props.getWIKIdataset())
					|| datasetName.equals(props.getIITBcurdataset())) {

				if (!"".equals(groundFilenameManual)) {
					ParseXML pm1 = new ParseXML();
					fileGroundTruthMapManual = pm1
							.parseXML(groundFilenameManual);
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
					if (it.next().offset >= kw1.getDocumentText().length() - 1) {
						it.remove();
					}
				}

				if (fileGroundTruthMapWiki.containsKey(filename))
					kw1.setGroundTruth(fileGroundTruthMapWiki.get(filename));
				if (fileGroundTruthMapManual != null)
					kw1.setGroundTruth(fileGroundTruthMapManual
							.get(kdd ? "KDD_" + filename : filename));
			}

			if (datasetName.equals(props.getAQUAINTdataset())
					|| datasetName.equals(props.getMSNBCdataset())) {

				if (!"".equals(groundFilenameManual)) {
					ParseXMLAQ pm1 = new ParseXMLAQ();
					fileGroundTruthMapManual = pm1
							.parseXMLAQ(groundFilenameManual);
					logger.log(Level.INFO, "parsed groundFilenameManual done!");
				} else {
					fileGroundTruthMapManual = null;
				}
				if (fileGroundTruthMapManual != null)
					kw1.setGroundTruth(fileGroundTruthMapManual.get(filename));
			}

			ArrayList<String> groundtruth = kw1.getGroundTruth();
			ArrayList<String> groundtruthWithRedirections = new ArrayList<String>();
			if (!Config.Server && thesaurus == null) {
				thesaurus = new Wikisaurus();
			}
			for (int i = 0; i < groundtruth.size(); i++) {
				if (!(null == groundtruth.get(i) || groundtruth.get(i)
						.equalsIgnoreCase("NA"))) {
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
				for (int j = 0; j < candidateEntitiesWithRedirects.size(); j++) {
					candidateEntitiesWithRedirects
							.set(j, candidateEntitiesWithRedirects.get(j)
									.toLowerCase());
				}

				for (int i = 0; i < groundtruthWithRedirections.size(); i++) {
					if (candidateEntitiesWithRedirects
							.contains(groundtruthWithRedirections.get(i)
									.toLowerCase()))
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
		logger.log(Level.INFO, "Exiting spotterWikiminer");

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
				kw.setGroundTruth(fileGroundTruthMapManual.get(kdd ? "KDD_"
						+ filename : filename));

			ArrayList<String> groundtruth = kw.getGroundTruth();
			ArrayList<String> groundtruthWithRedirections = new ArrayList<String>();
			if (!Config.Server && thesaurus == null) {
				thesaurus = new Wikisaurus();
			}
			for (int i = 0; i < groundtruth.size(); i++) {
				if (!(null == groundtruth.get(i) || groundtruth.get(i)
						.equalsIgnoreCase("NA"))) {
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
				for (int j = 0; j < candidateEntitiesWithRedirects.size(); j++) {
					candidateEntitiesWithRedirects
							.set(j, candidateEntitiesWithRedirects.get(j)
									.toLowerCase());
				}

				for (int i = 0; i < groundtruthWithRedirections.size(); i++) {
					if (candidateEntitiesWithRedirects
							.contains(groundtruthWithRedirections.get(i)
									.toLowerCase()))
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

	public String readFromFile() {

		String str = "";
		BufferedReader br;
		int r;
		StringBuilder s = new StringBuilder();
		try {
			int line = 0;
			br = new BufferedReader(new FileReader(trainDir + filename));
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
		return str;
	}

	public void spotter() {

		ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
		KeywordsGroundTruth kw;
		try {
			kw = kw_extractor.extractForTrainingwithGroundTruth(trainDir,
					filename, groundFilenameWiki, groundFilenameManual, csize);
			FeatureExtractor ft_extractor = new FeatureExtractor();

			long start = System.currentTimeMillis();

			trainData = ft_extractor.extractFeaturesForTraining(kw, maxNodes,
					consolidationSize);

			long elapsedTimeMillis = System.currentTimeMillis() - start;
			float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);

			System.out.println("For file " + kw.filename
					+ " process completed in  " + elapsedTimeMin + " mins");

			// for (NodePotentials np : trainData.nodes.potentials_set) {
			// // System.out.println(np.label + " " + np.mention + "        "
			// // + np.name + "  " + np.context_score_frequent +
			// // "  "+np.context_score_synopsis+" "+np.context_score_vbdj+" "
			// // + np.inlink_count + " " + np.outlink_count + " "
			// // +
			// // np.page_title_score+" "+np.disambiguation+" "+np.redirection);
			// System.out.println(np.label + " " + np.mention + "        "
			// + np.sense_probability);
			// }

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		try {
			DisambProperties
					.init("/home/kanika/EntityDisamb/src/disamb.properties");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DisambProperties props = DisambProperties.getInstance();
		CollectiveTraining ct = new CollectiveTraining(
				props.getMSNBCtextFilesFolder(), "",
				props.getMSNBCxmlFilesFolder() + "Bus16451112.txt",
				"Bus16451112.txt", "MSNBC");
		ct.spotterWikiMiner();
		for (int i = 0; i < ct.trainData.nodes.potentials_set.size(); i++) {
			System.out.println(ct.trainData.nodes.potentials_set.get(i).mention
					+ " " + ct.trainData.nodes.potentials_set.get(i).name);
		}
		// try {
		// new LearningProperties(args[0]);
		// DisambProperties.init(args[1]);
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// String trainTextPath = LearningProperties.getTrainingTextPath();
		// String trainGraphPath = LearningProperties.getTrainingPath();
		// String trainxmlPath = LearningProperties.getTrainingXMLPath();
		// String trainxmlwikiPath =
		// LearningProperties.getTrainingXMLWikiPath();
		// File trainFolder = new File(trainTextPath);
		// for (File trainFile : trainFolder.listFiles())
		// graph(trainTextPath, trainFile, trainGraphPath, trainxmlPath,
		// trainxmlwikiPath);
	}

	public static AMNWeights train(String propsFile) throws TrainingException {
		State optimal = null;
		try {
			new LearningProperties(propsFile);
			String trainTextFilesLog = LearningProperties
					.getTrainingTextFilesLog();
			String trainTextPath = LearningProperties.getTrainingTextPath();
			String trainGraphPath = LearningProperties.getTrainingPath();
			String trainxmlPath = LearningProperties.getTrainingXMLPath();
			String trainxmlwikiPath = LearningProperties
					.getTrainingXMLWikiPath();

			BufferedReader reader = new BufferedReader(new FileReader(
					trainTextFilesLog));
			String trainFileName = null;
			while (null != (trainFileName = reader.readLine()))
				graph(trainTextPath, new File(trainTextPath + trainFileName),
						trainGraphPath, trainxmlPath, trainxmlwikiPath);
			reader.close();

			// Wikisaurus._wikipedia.close();

			MultiLearner learner = new MultiLearner(
					LearningProperties.getLearningIterations());

			learner.loadTrainingData(LearningProperties.getTrainingPath());
			optimal = learner.learn();
			optimal.logall(Level.INFO);
		} catch (FileNotFoundException e) {
			throw new TrainingException(e);
		} catch (IOException e) {
			throw new TrainingException(e);
		} catch (LearningException e) {
			throw new TrainingException(e);
		}
		return optimal.getWeights();
	}

	public static void graph(String trainPath, File trainFile,
			String graphPath, String trainxmlPath, String trainxmlwikiPath) {
		String docName = trainFile.getName();
		if (!exists(graphPath + docName)) {
			System.out.println("Creating graph for " + docName);
			String docXML = (null != trainxmlwikiPath && !""
					.equalsIgnoreCase(trainxmlwikiPath)) ? trainxmlwikiPath
					+ docName + ".xml" : "";
			CollectiveTraining ct1 = new CollectiveTraining(trainPath,
					trainxmlPath, docXML, docName, "");
			CollectiveTraining.kdd = LearningProperties.isKDD();
			long start = System.currentTimeMillis();
//			ct1.spotterNew();
			ct1.spotterWikiMiner();
			long elapsedTimeMillis = System.currentTimeMillis() - start;
			float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);
			System.out.println("spotting took " + elapsedTimeMin + " mins");
			try {
				ct1.serializeSpots(graphPath + docName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		// start = System.currentTimeMillis();
		// ct1.createGraph();
		// elapsedTimeMillis = System.currentTimeMillis() - start;
		// elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);
		// System.out.println("graph creation took " + elapsedTimeMin +
		// " mins");
		// try {
		// ct1.serializeGraph(graphPath + docName);
		// } catch (IOException e) {
		// e.printStackTrace();
		// } catch (TransformerConfigurationException e) {
		// e.printStackTrace();
		// } catch (SAXException e) {
		// e.printStackTrace();
		// }
	}

	private static boolean exists(String filename) {
		File file = new File(filename + ".spots");
		return file.exists();
	}

	private void serializeSpots(String name) throws FileNotFoundException,
			IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				name + ".spots"));
		oos.writeObject(trainData);
		oos.flush();
		oos.close();
	}

	private void serializeGraph(String name) throws IOException,
			TransformerConfigurationException, SAXException {
		// Export the graph object
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				name + ".graph"));
		oos.writeObject(mrfgraph);
		oos.flush();
		oos.close();

		// Also export it to a text file in GraphML format
		VertexNameProvider<WikiNode> vertexProvider = new WikiNodeProvider();
		EdgeNameProvider<WikiEdge> edgeProvider = new WikiEdgeProvider();
		new GraphMLExporter<WikiNode, WikiEdge>(vertexProvider, vertexProvider,
				edgeProvider, edgeProvider).export(new FileWriter(name
				+ ".graphml"), mrfgraph);
	}

	private void createGraph() {
		mrfgraph = GraphCreator.getInstance().createGraph(trainData);
		MRFGraph mrf = new MRFGraph(mrfgraph);
		mrf.normalizeFeatures(Config.NODE_FEATURE_DIM, Config.EDGE_FEATURE_DIM);
		mrfgraph = mrf.getGraph();
	}

}
