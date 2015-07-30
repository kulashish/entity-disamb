package spotting;

import it.unimi.dsi.util.Interval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;
import org.apache.lucene.document.Document;
import org.tartarus.snowball.ext.PorterStemmer;
import org.wikipedia.miner.model.Category;
import org.wikipedia.miner.model.Label;

import util.DisambProperties;
import util.TestJAWS;
import util.WikiToFreebaseIDMap;
import util.XMLTagInfo;
import weka.classifiers.bayes.net.search.SearchAlgorithm;
import weka.core.Stopwords;
import wikiGroundTruth.server.ClientWikisauras;
import wikiGroundTruth.server.LabelSense;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class KeywordsGroundTruth implements Serializable {
	static Wikisaurus thesaurus = null;

	public int contextSize;

	public class Mention implements Serializable {
		public String key;
		public String name;
		public int offset;
		public int length;
		public String query;
		public String redirectquery;
		public String context;
		public String contextAroundMention;
		// public ArrayList<String> context;
		public ArrayList<String> context_nouns;
		public ArrayList<String> context_vbadj;
		public Interval interval;
		public LabelSense senses;

		public Mention() {

			key = new String();
			name = new String();
			query = new String();
			redirectquery = new String();
			// context = new ArrayList<String>();
			context = "";
			contextAroundMention = "";
			context_nouns = new ArrayList<String>();
			context_vbadj = new ArrayList<String>();
			senses = null;

		}

		public Mention(Mention mention) {
		}
	}

	public String TrainDir;
	public String GroundTruthDir;
	public String filename; // name of file from data is extracted

	// this array list of type mention has all keyword+query related information
	// for each document
	public ArrayList<Mention> keywords;

	// all four array lists are for ground truth
	private ArrayList<String> groundTruth; // list of ground truth entities
	// (wikipedia names) which occur in
	// the document
	public ArrayList<Integer> offset; // character offsets of the ground truth
	// entities in the original document
	public ArrayList<Integer> length; // can be used for matching entities to
	// mentions
	public ArrayList<String> groundMention;
	public String document; // optional - store the entire text of the document
	// in String so the File need
	public String tagged_document;

	public static MaxentTagger tagger = null;
	public static DisambProperties props = DisambProperties.getInstance();
	public static final String taggerModel = props.getTaggerModel();
	public static Pattern pattern = Pattern.compile("(.*)[/_]([A-Z]*)");
	public static HashSet<String> noun_tags = new HashSet<String>();
	public static HashSet<String> verb_tags = new HashSet<String>();
	public static HashSet<String> adj_tags = new HashSet<String>();
	public static HashSet<String> adverb_tags = new HashSet<String>();
	public static HashSet<String> extra_tags = new HashSet<String>();

	public static Pattern ptn_numeric = Pattern.compile("([0-9]+[\\.,:-]*)+");

	public KeywordsGroundTruth(String file, String trainDir, String groundDir,
			int contSize) {
		init(file, trainDir, groundDir, contSize);
		readDocument();
		try {
			setKeywords(false);
			if (groundDir != null)
				setGroundTruth();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// used for collective training data generation where we have ground
	// mentions and want to set context etc
	public KeywordsGroundTruth(String file, String trainDir, int contSize,
			HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapWiki,
			HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapManual) {
		init(file, trainDir, null, contSize);
		if (CollectiveTraining.kdd)
			readDoc();
		else
			readDocTraining();

	}

	public KeywordsGroundTruth(String file, String trainDir, int contSize) {
		init(file, trainDir, null, contSize);
		// readDocument();
		readDoc();
		try {
			setKeywords(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public KeywordsGroundTruth(String doc, ArrayList<String> ground,
			int contSize) {
		System.out.println("in KeywordsGroundTruth");
	
		init("", "", "", contSize);
		groundTruth = ground;
		document = doc;
		try {
			setKeywords(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// for wikipedia miner spotter

	public KeywordsGroundTruth(String doc, ArrayList<String> ground,
			int contsize, boolean wikiminer) {
		init("", "", "", contsize);
		groundTruth = ground;
		document = doc;
		setKeywordsWikiMiner();
	}
	
	public KeywordsGroundTruth(String doc) {
		init("", "", "", 100);
		groundTruth = new ArrayList<String>();
		document = doc;
	}
	
	private void init(String file, String trainDir, String groundDir,
			int contSize) {
		try {
			contextSize = contSize;
			filename = file;
			TrainDir = trainDir;
			GroundTruthDir = groundDir;
			keywords = new ArrayList<Mention>();
			offset = new ArrayList<Integer>();
			length = new ArrayList<Integer>();
			groundMention = new ArrayList<String>();
			groundTruth = new ArrayList<String>();
			document = new String();
			if (tagger == null) {
				tagger = new MaxentTagger(taggerModel);
				noun_tags.add("NN");
				noun_tags.add("NNP");
				noun_tags.add("NNPS");
				noun_tags.add("NNS");
				verb_tags.add("VB");
				verb_tags.add("VBD");
				verb_tags.add("VBG");
				verb_tags.add("VBN");
				verb_tags.add("VBP");
				verb_tags.add("VBZ");
				adj_tags.add("JJ");
				adj_tags.add("JJR");
				adj_tags.add("JJS");
				adverb_tags.add("RB");
				adverb_tags.add("RBR");
				adverb_tags.add("RBS");
				adverb_tags.add("WRB");
				// extra_tags.add("CC");
				extra_tags.add("CD");
				extra_tags.add("DT");
				extra_tags.add("PDT");

			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// required in case of collective training as we deal with characters
	public void readDocTraining() {
		BufferedReader br;
		String str;
		int r;
		StringBuilder s = new StringBuilder();
		try {
			int line = 0;
			br = new BufferedReader(new FileReader(TrainDir + filename));
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
		str = str.substring(0, str.lastIndexOf(" "));
		document = str;
	}

	public void readDoc() {
		BufferedReader br;
		String str;
		int r;
		StringBuilder s = new StringBuilder();
		try {
			int line = 0;
			br = new BufferedReader(new FileReader(TrainDir + filename));
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
		document = str;
	}

	public void readDocument() {
		try {
			String sCurrentLine;
			FileReader f = new FileReader(TrainDir + filename);
			BufferedReader br = new BufferedReader(f);
			int line = 0;
			while ((sCurrentLine = br.readLine()) != null) {
				if (line == 0)
					document += sCurrentLine;
				else
					document += " " + sCurrentLine;
			}
			f.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// not used anywhere...had written for checking purpose
	public void readAndStemDocument(boolean stem) {
		try {

			String text = "";
			ArrayList<String> stemtoken = new ArrayList<String>();

			String sCurrentLine;

			FileReader f = new FileReader(TrainDir + filename);
			BufferedReader br = new BufferedReader(f);
			while ((sCurrentLine = br.readLine()) != null) {
				text += " " + sCurrentLine;
			}
			f.close();

			PorterStemmer stemmer = new PorterStemmer();
			StringTokenizer str = new StringTokenizer(text);
			while (str.hasMoreTokens()) {

				String token = str.nextToken();
				if (token == null)
					continue;
				token = token.replaceAll("[^a-z\\sA-Z\\-]", "");
				if ("".equals(token))
					continue;
				if (stem) {
					stemmer.setCurrent(token);
					stemmer.stem();
					stemtoken.add(stemmer.getCurrent().toLowerCase()); // PorterStemmer
					// keywords.add(ls.getStem(token)); //LovinStemmer
				} else {
					stemtoken.add(token);
					// keywords.add(ls.getStem(token)); //LovinStemmer
				}
			}
			for (int i = 0; i < stemtoken.size(); i++) {
				document += " " + stemtoken.get(i);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// for collective training as we already have ground mentions
	public void setKeywordsTraining(
			HashMap<String, ArrayList<XMLTagInfo>> groundMapWiki,
			HashMap<String, ArrayList<XMLTagInfo>> groundMapManual, String file) {
		ArrayList<XMLTagInfo> mapForTrainFile = groundMapWiki.get(file);
		for (int i = 0; i < mapForTrainFile.size(); i++) {
			Mention mention = new Mention();
			mention.key = mapForTrainFile.get(i).mention;
			mention.name = mapForTrainFile.get(i).mention;
			mention.length = mapForTrainFile.get(i).length;
			mention.offset = mapForTrainFile.get(i).offset;
			if (null == mention.name)
				mention.name = document.substring(mention.offset,
						mention.offset + mention.length);
			if (mention.offset < document.length() - 1) {
				int context_lo = Math.max(0, (mention.offset) - contextSize);
				int context_hi = Math.min(document.length() - 1,
						(mention.offset) + contextSize);
				String contextString = document.substring(context_lo,
						context_hi);
				mention.context = contextString.replaceAll(
						"[^0-9a-z\\sA-Z/\\-]", "");
				int con_lo = Math.max(0, mention.offset - 10);
				int con_hi = Math.min(document.length() - 1,
						mention.offset + 10);
				mention.contextAroundMention = document.substring(con_lo,
						con_hi).replaceAll("[^0-9a-z\\sA-Z]", " ");
				mention.contextAroundMention += " "
						+ mention.name.replaceAll("[^0-9a-z\\sA-Z]", " ");
				keywords.add(mention);
			}
		}
		if (groundMapManual != null) {
			ArrayList<XMLTagInfo> mapForTrainFile1 = groundMapManual.get(file);
			for (int i = 0; i < mapForTrainFile1.size(); i++) {
				Mention mention = new Mention();
				mention.key = mapForTrainFile1.get(i).mention;
				mention.name = mapForTrainFile1.get(i).mention;
				mention.length = mapForTrainFile1.get(i).mention.length();
				mention.offset = mapForTrainFile1.get(i).offset;
				int context_lo = Math.max(0, mention.offset - contextSize);
				int context_hi = Math.min(document.length() - 1, mention.offset
						+ contextSize);
				String contextString = document.substring(context_lo,
						context_hi);
				mention.context = contextString.replaceAll(
						"[^0-9a-z\\sA-Z/\\-]", "");
				int con_lo = Math.max(0, mention.offset - 10);
				int con_hi = Math.min(document.length() - 1,
						mention.offset + 10);
				mention.contextAroundMention = document.substring(con_lo,
						con_hi).replaceAll("[^0-9a-z\\sA-Z]", " ");
				mention.contextAroundMention += " "
						+ mention.name.replaceAll("[^0-9a-z\\sA-Z]", " ");
				keywords.add(mention);

			}
		}

	}

	// for wikiminer spotter

	public void setKeywordsWikiMiner() {
		try {
			WikipediaAnnotator annotator = new WikipediaAnnotator();
			long annstartTime = System.currentTimeMillis();

			HashMap<String, Label.Sense[]> ment2ent = annotator
					.annotate(document);
			
			long annendTime = System.currentTimeMillis();
			
			long diff1 = (annendTime - annstartTime);

			System.out.println("Time taken by annotater : " + diff1 + " milliseconds");
			
			for (String key : ment2ent.keySet()) {
				
				//sunny:adding code to check if sense is within freebase dataset
				//adding it only if we find it.
				
//				Vector<Label.Sense> updatedsenses = new Vector();
//				for(Label.Sense s : ment2ent.get(key)){
//					String entity = s.getTitle().replace(" ", "_");
//					String freebaseid = WikiToFreebaseIDMap.getInstance().getFreeBaseID("\"/wikipedia/en_title/" + entity + "\"");
//
//					if(freebaseid != null){
//						updatedsenses.add(s);
//					}
//				}
//				
//				Label.Sense[] sensearray = new Label.Sense[updatedsenses.size()];
//				
//				updatedsenses.toArray(sensearray);
				
				LabelSense senses = new LabelSense(ment2ent.get(key));
				//LabelSense senses = new LabelSense(sensearray);
				Mention mention = new Mention();
				//System.out.println("key from ment2ent : " + key);
				String ment = key.split("_")[0];

				//System.out.println("ment from ment2ent : " + ment);
				int off = Integer.parseInt(key.split("_")[1]);
				mention.key = ment;
				mention.name = ment;
				mention.length = ment.length();
				mention.offset = off;
				mention.context = getContext(off, mention.length, contextSize);
				mention.contextAroundMention = getContext(off, mention.length,
						10);

				mention.senses = senses;
				keywords.add(mention);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void setKeywords(boolean stem) throws Exception {
		tagged_document = tagger.tagString(document);
		ArrayList<String> tokens = new ArrayList<String>();
		
		//System.out.println("tagged document : " + tagged_document);

		StringTokenizer str = new StringTokenizer(tagged_document);

		while (str.hasMoreTokens()) {
			String token = str.nextToken();
			if (token == null || "".equals(token) || " ".equals(token))
				continue;
			if (!Stopwords.isStopword(token.split("_")[0])
					|| noun_tags.contains(token.split("_")[1])
					|| adj_tags.contains(token.split("_")[1])
					|| extra_tags.contains(token.split("_")[1]))
						tokens.add(token);
			//System.out.println("token : " + token);
			if (!Stopwords.isStopword(token.split("_")[0])){
//				System.out.println("token added.");
				tokens.add(token);
			}
		}

		String prev_tag = null; // if previous token was a noun then add n-gram
		// noun clause

		int curr_offset = 0,currbyte = 0;
		for (int i = 0; i < tokens.size(); i++) {
			// System.out.print(" "+tokens.get(i));
			if (tokens.get(i) == null)
				continue;
			Matcher matcher = pattern.matcher(tokens.get(i));
			matcher.find();
			String word = matcher.group(1);
			String tag = matcher.group(2);
			
			//System.out.println("word: " + word + " tag: " + tag);

			if (word == null || "".equals(word)) {
				prev_tag = null;
				continue;
			}
			String token = word.replaceAll("[^0-9a-z\\sA-Z/\\-]", "");
			if ("".equals(token) || "/".equals(token)) {
				prev_tag = null;
				continue;
			}
			if (!(noun_tags.contains(tag) || adj_tags.contains(tag) || extra_tags
					.contains(tag))) {
				prev_tag = null;
				continue;
			}
			Mention mention = new Mention();

			if (tag.equals("JJ")) {
				String temp = TestJAWS.getNounForm(token);
				if (temp != null && !"".equals(temp)) {
					mention.key = temp;
					prev_tag = null;
				} else {
					mention.key = token;
					prev_tag = null;

				}
			} else {
				mention.key = token;
			}

			mention.name = word;
			mention.length = word.length();
			curr_offset = document.indexOf(word, curr_offset);
			
			mention.offset = curr_offset;
			mention.context = getContext(curr_offset, mention.length,
					contextSize);
			mention.contextAroundMention = getContext(curr_offset,
					mention.length, 10);
			// StringTokenizer str1 = new StringTokenizer(contextString);
			// while(str1.hasMoreTokens()){
			// String w=str1.nextToken();
			// if (w == null || "".equals(w) || " ".equals(w)) continue;
			// mention.context.add(w);
			// }
			// parseContext(mention);
			//System.out.println("mention.name : " + mention.name + " offset : " + mention.offset);
			keywords.add(mention);
		}
		// System.out.println("Keywords: "+getMentionNames());
		consolidateMentions(6);
		//consolidateMentions(4);
	}

	public String getContext(int curr_offset, int mention_length,
			int context_size) {
		int context_lo = Math.max(0, curr_offset - context_size);
		int context_hi = Math.min(document.length(), curr_offset
				+ mention_length + context_size);
		// context_lo = Math.max(0,document.lastIndexOf(" ",context_lo)+1);
		// context_hi = Math.min(document.length(),
		// document.indexOf(" ",context_hi));
		String contextString = document.substring(context_lo, context_hi);
		return contextString.replaceAll("[^0-9a-z\\sA-Z/\\-]", "");
	}

	public void consolidateMentions(int maxLength) {

		if (!Config.Server && thesaurus == null) {
			thesaurus = new Wikisaurus();
		}

		ClientWikisauras obj = new ClientWikisauras();
		
//		LuceneIndexWrapper luceneIndex = new LuceneIndexWrapper(
//				props.getCompleteIndex(), props.getRedirectIndex(),
//				props.getInlinkIndex(), props.getDisambIndex(),
//				props.getAnchorIndex());

		ArrayList<Mention> mentions = new ArrayList<Mention>();
		mentions.addAll(keywords);
		//System.out.println("consolidating mentions size:" + mentions.size());
		keywords = new ArrayList<Mention>();
		Integer[] token_type = new Integer[mentions.size()];
		for (int i = 0; i < token_type.length; i++)
			token_type[i] = 0;

		int curr_offset = 0;
		String curr_mention = "";
		for (int i = 0; i < mentions.size(); i++) {
			if (token_type[i] != 0) {
				//i++;
				continue;
			}
			curr_offset = mentions.get(i).offset;
			curr_mention = mentions.get(i).name;
			//System.out.println("offset: " + curr_offset + " curr_mention: " + curr_mention + " context: " + mentions.get(i).context);

			String[] allWords = new String[maxLength];
			Integer[] allOffset = new Integer[maxLength];
			String currWord = curr_mention;
			Integer currWordEnd = curr_offset + curr_mention.length() + 1;

			allWords[0] = currWord;
			allOffset[0] = curr_offset;
			int k = 1;
			for (; k < maxLength; k++) {
				currWordEnd = document.indexOf(" ", currWordEnd + 1);
				if (currWordEnd == -1)
					currWordEnd = document.length();
				if (curr_offset < 0 || curr_offset >= document.length()) {
					k--;
					break;
				}
				currWord = document.substring(curr_offset, currWordEnd);
				allWords[k] = currWord;
				allOffset[k] = currWordEnd;
				if (currWordEnd >= document.length())
					break;
			}
			if (k == maxLength)
				k--;

			for (; k >= 0; k--) {
				LabelSense senses = null;
				//System.out.println("allwords[" + k + "] : " + allWords[k]);
				try {
					if (Config.Server)
						senses = obj.getSenses(allWords[k]);
					else {
						//String possibleMention = WordUtils.capitalize(allWords[k]);
						Label.Sense[] temp = thesaurus.getSenses(allWords[k]);
						
//						List<String> qwords = Arrays.asList(allWords[k].split(" "));
//						
//						boolean nostopword = true;
//						for(String item : qwords){
//							if(Stopwords.isStopword(item)){
//								nostopword = false;
//							}
//						}
						
						if (temp != null)
							senses = new LabelSense(temp);
						
//						Vector<String> sensewmc = new Vector();
//						Vector<Double> sensewmp = new Vector();
						
						//hard coded search for the word in the freebase dataset. should not be done for long text hence commenting
//						if(!Stopwords.isStopword(allWords[k].toLowerCase())){
//							Vector<String> freebaseTitles = WikiToFreebaseIDMap.getInstance().getAllWikiTitles(allWords[k].toLowerCase());
//							//String title = "/wikipedia/en_title/" + allWords[k].replace(" ", "_");
//							//java.util.regex.Pattern pa = java.util.regex.Pattern.compile(title.toLowerCase());
//							//java.util.regex.Matcher ma = pa.matcher("");
//							
//							if(freebaseTitles != null){
//								for(String fbTitle : freebaseTitles){
//									fbTitle = fbTitle.replace("/wikipedia/en_title/", "");
//									fbTitle = fbTitle.replace("\"", "");
//									fbTitle = fbTitle.replace("_", " ");
//									System.out.println("fbTitle : " + fbTitle);
//									sensewmc.add(fbTitle);
//									sensewmp.add(new Double(1.0/(freebaseTitles.size())));
//									//sensewmp.add(new Double(0));
//								}	
//							}
//						}
						
//						if((temp != null) || (sensewmc.size() > 0)){
//							senses = new LabelSense();
//							
//							int scount= 0,total = 0;
//							
//							if(temp != null){
//								total = temp.length + sensewmc.size();
//								senses.wikiMinerCandidate = new String[temp.length + sensewmc.size()];
//								senses.wikiMinerProbability = new double[temp.length + sensewmc.size()];
//								
//								for(;scount<temp.length;++scount){
//									senses.wikiMinerCandidate[scount] = temp[scount].getTitle();
//									senses.wikiMinerProbability[scount] = temp[scount].getPriorProbability();
//								}
//								scount = temp.length; 
//							}
//							else{
//								total = sensewmc.size();
//								senses.wikiMinerCandidate = new String[sensewmc.size()];
//								senses.wikiMinerProbability = new double[sensewmc.size()];
//							}
//
//							for(int cnt = 0;scount < total;++scount,++cnt){
//								senses.wikiMinerCandidate[scount] = sensewmc.elementAt(cnt);
//								senses.wikiMinerProbability[scount] = sensewmp.elementAt(cnt);
//							}
//						}
						
						//else if((k >= 1 && k <= 2) || ((k == 0) && (!Stopwords.isStopword(allWords[k].split("_")[0]))))
//						else if((k <= 2) && (nostopword == true))
//						{
//							String myquery = allWords[k];
//							
//							String query = luceneIndex.buildPhraseSearchQuery(myquery,null);
//							
//							System.out.println("query : " + query);
//							
//							if (query != null) {
//								luceneIndex.searchStringInIndex(query, 2);
//								
//								System.out.println("found : " + luceneIndex.hits.scoreDocs.length);
//								
//								Vector<String> sensewmc = new Vector();
//								Vector<Double> sensewmp = new Vector();
//								
//								for (int licount = 0;licount < luceneIndex.hits.scoreDocs.length;++licount) {
//									Document doc = luceneIndex.searcher.doc(luceneIndex.hits.scoreDocs[licount].doc); // get the next document
//									
//									String pagetitle = doc.get("page_title");
//									String disamb = doc.get("title_disamb");
//									if (!((disamb == null) || disamb.equals(""))){
//										pagetitle = pagetitle + " (" + disamb + ")";
//									}
//									
//									System.out.println("lucene hit: " + pagetitle + " score : " + luceneIndex.hits.scoreDocs[licount].score);
//									
//									//if(luceneIndex.hits.scoreDocs[licount].score < 0.5)
//									//	continue;
//									
//									sensewmc.add(pagetitle);
//									sensewmp.add(new Double(luceneIndex.hits.scoreDocs[licount].score));
//									
//									if(sensewmc.size() == 3)
//										break;
//								}
//								
//								if(sensewmc.size() > 0){
//									senses = new LabelSense();
//									
//									if(sensewmc.size() == 3){
//										senses.wikiMinerCandidate = new String[3];
//										senses.wikiMinerProbability = new double[3];
//									}
//									else{
//										senses.wikiMinerCandidate = new String[sensewmc.size()];
//										senses.wikiMinerProbability = new double[sensewmc.size()];
//									}
//								}
//								
//								for(int scount=0;scount<sensewmc.size();++scount){
//									senses.wikiMinerCandidate[scount] = sensewmc.elementAt(scount);
//									senses.wikiMinerProbability[scount] = sensewmp.elementAt(scount);
//								}
//							}
//						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				if (null != senses) {
					
//					Vector<String> updatedsensewmc = new Vector();
//					Vector<Double> updatedsensewmp = new Vector();
//					for(int x=0;x<senses.wikiMinerCandidate.length;++x){
//						System.out.println("senses: " + senses.wikiMinerCandidate[x]);
//						String entity = senses.wikiMinerCandidate[x].replace(" ", "_");
//						String freebaseid = WikiToFreebaseIDMap.getInstance().getFreeBaseID("\"/wikipedia/en_title/" + entity + "\"");
//						if(freebaseid != null){
//							updatedsensewmc.add(senses.wikiMinerCandidate[x]);
//							updatedsensewmp.add(senses.wikiMinerProbability[x]);
//						}
//					}
//					
//					LabelSense lsense = new LabelSense();
//
//					lsense.wikiMinerCandidate = new String[updatedsensewmc.size()];
//					lsense.wikiMinerProbability = new double[updatedsensewmc.size()];
//
//					for(int scount=0;scount<updatedsensewmc.size();++scount){
//						lsense.wikiMinerCandidate[scount] = updatedsensewmc.elementAt(scount);
//						lsense.wikiMinerProbability[scount] = updatedsensewmp.elementAt(scount);
//					}
					
					Mention new_mention = new Mention();
					new_mention.name = allWords[k];
					new_mention.length = new_mention.name.length();
					new_mention.offset = curr_offset;
					new_mention.context = getContext(curr_offset,
							new_mention.length, contextSize);
					new_mention.contextAroundMention = getContext(curr_offset,
							new_mention.length, 10);
					if (k == 0)
						new_mention.key = mentions.get(i).key;
					//new_mention.senses = lsense;
					new_mention.senses = senses;
					
					System.out.println("wikiminer candidate for : " +  new_mention.name);
					for(int ic=0;ic<senses.wikiMinerCandidate.length;++ic){
						System.out.println("\t" + senses.wikiMinerCandidate[ic] + "  " + senses.wikiMinerProbability[ic]);
					}
					
					keywords.add(new_mention);
					//System.out.println("new_mention offset + length : " + new_mention.offset + " " + new_mention.length);
					if (!isArticleToken(curr_mention)) {
						for (int j = i; j < mentions.size()
								&& mentions.get(j).offset < (new_mention.offset + new_mention.length); j++)
							token_type[j] = 1;
					} else {
						token_type[i] = 2;
					}
					break;
				}
			}

			if (token_type[i] == 0 && !isArticleToken(curr_mention)
					&& isValidToken(curr_mention)) {
				keywords.add(mentions.get(i));
			}
		}
	}

	public boolean isArticleToken(String term) {
		return ("".equals(term) || "the".equals(term.toLowerCase())
				|| "an".equals(term.toLowerCase()) || "a".equals(term
				.toLowerCase()));
	}

	public boolean isValidToken(String term) {
		if (Stopwords.isStopword(term))
			return false;

		Matcher matcher = ptn_numeric.matcher(term);
		if (matcher.find())
			return false;

		return true;

	}

	// public void parseContext(Mention mention) {
	// for (String token : mention.context) {
	// Matcher matcher = pattern.matcher(token);
	// matcher.find();
	// String word = matcher.group(1);
	// String tag = matcher.group(2);
	// if (word == null || "".equals(word)) {
	// continue;
	// }
	// word = word.replaceAll("[^a-z\\sA-Z/]","");
	// if ("".equals(word) || "/".equals(word)) {
	// continue;
	// }
	// if (noun_tags.contains(tag)) {
	// //mention.context_nouns.add(word);
	// mention.context_nouns.addAll(MorphologicalAnalyzer.analyze(word));
	// }
	// if (verb_tags.contains(tag) || adj_tags.contains(tag))
	// mention.context_vbadj.addAll(MorphologicalAnalyzer.analyze(word));
	// }
	// }

	public void setGroundMention(ArrayList<XMLTagInfo> groundtruth)
			throws Exception {
		groundMention.clear();
		keywords.clear();
		for (int i = 0; i < groundtruth.size(); i++) {
			int off = groundtruth.get(i).offset;
			int len = groundtruth.get(i).length;
			groundMention.add(document.substring(off, off + len));
			Mention m = new Mention();
			int context_lo = Math.max(0, off - contextSize);
			int context_hi = Math.min(document.length() - 1, off + contextSize);
			String contextString = document.substring(context_lo, context_hi);
			m.context = contextString.replaceAll("[^0-9a-z\\sA-Z/\\-]", "")
					.toLowerCase();
			int con_lo = Math.max(0, m.offset - 10);
			int con_hi = Math.min(document.length() - 1, m.offset + 10);
			m.contextAroundMention = document.substring(con_lo, con_hi)
					.replaceAll("[^0-9a-z\\sA-Z]", " ").toLowerCase();
			m.contextAroundMention += " "
					+ m.name.replaceAll("[^0-9a-z\\sA-Z]", " ").toLowerCase();
			m.name = document.substring(off, off + len);
			m.context.replaceAll("\\sand", "");
			m.contextAroundMention.replaceAll("\\sand", "");
			m.context.replaceAll("\\snot", "");
			m.contextAroundMention.replaceAll("\\snot", "");
			m.length = len;
			m.offset = off;
			keywords.add(m);
		}

	}

	public void setGroundTruth(ArrayList<XMLTagInfo> groundtruth)
			throws Exception {
		for (int i = 0; i < groundtruth.size(); i++) {
			groundTruth.add(groundtruth.get(i).wikiEntity);
			offset.add(groundtruth.get(i).offset);
			length.add(groundtruth.get(i).length);
			groundMention.add(groundtruth.get(i).mention == null ? document
					.substring(groundtruth.get(i).offset,
							groundtruth.get(i).offset
									+ groundtruth.get(i).length) : groundtruth
					.get(i).mention);
		}
	}

	// public void setGroundMention(){
	// String text = this.getDocumentText();
	// for(int i=0; i<this.getGroundOffset().size(); i++){
	// int start = this.getGroundOffset().get(i);
	// int end = this.getGroundOffset().get(i) + this.getGroundLength().get(i);
	// //+1 for others
	// //this line is commented only for wikipedia ground entity creation
	// purpose
	// //this.groundMention.add(i, text.substring(this.getGroundOffset().get(i),
	// this.getGroundOffset().get(i)+this.getGroundLength().get(i)+1));
	//
	//
	// if(end>=this.getDocumentText().length()||start==-1||start>=end){
	// System.out.println(this.filename+"  "+" end="+end+" start= "+start+" "+this.getDocumentText().length());
	// System.out.println(this.getDocumentText());
	// }
	// this.groundMention.add(i, text.substring(start,end));
	// }
	// }

	public ArrayList<String> getGroundMentionNames() {
		return groundMention;
	}

	public void setGroundTruth() throws Exception {
		FileReader f = new FileReader(GroundTruthDir + filename);
		BufferedReader br = null;
		try {
			String sCurrentLine;
			br = new BufferedReader(f);
			while ((sCurrentLine = br.readLine()) != null) {
				groundTruth.add(sCurrentLine);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Mention> getKeywords() {
		return keywords;
	}

	public ArrayList<String> getMentionNames() {
		ArrayList<String> names = new ArrayList<String>();
		for (Mention m : keywords) {
			names.add(m.name);
		}
		return names;
	}

	public ArrayList<String> getGroundTruth() {
		return groundTruth;
	}

	public ArrayList<Integer> getGroundOffset() {
		return offset;
	}

	public ArrayList<Integer> getGroundLength() {
		return length;
	}

	public String getDocumentText() {
		return document;
	}

	// public static void main(String[] args) {
	// KeywordsGroundTruth doc1 = new KeywordsGroundTruth("doc1.txt");
	// System.out.println("keywords   "+ doc1.getKeywords());
	// System.out.println("ground Truth   "+ doc1.getGroundTruth());
	// KeywordsGroundTruth doc2 = new KeywordsGroundTruth("doc2.txt");
	// System.out.println("keywords   "+ doc2.getKeywords());
	// System.out.println("ground Truth   "+ doc2.getGroundTruth());
	// }
}
