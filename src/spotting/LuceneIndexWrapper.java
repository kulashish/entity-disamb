package spotting;

import in.ac.iitb.cse.mrf.util.MathHelper;
import org.apache.commons.collections.ListUtils;
import it.unimi.dsi.util.Interval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;

import util.DisambProperties;
import util.MorphologicalAnalyzer;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Stopwords;
import wikiGroundTruth.server.ClientWikisauras;
import wikiGroundTruth.server.LabelSense;

public class LuceneIndexWrapper {

	static Wikisaurus thesaurus = null;

	static String indexName = null; // local copy of the configuration variable
	static String indexNameForRedirects = null;
	static String indexNameForDisamb = null;
	static String indexNameForInLinks = null;
	static String indexNameForAnchor = null;

	static IndexReader reader = null;
	static IndexReader readerForRedirects = null;
	static IndexReader readerForDisamb = null;
	static IndexReader readerForInLinks = null;
	static IndexReader readerForAnchor = null;

	IndexSearcher searcher = null; // the searcher used to open/search the index
	IndexSearcher searcherForId = null;
	IndexSearcher searcherForRedirects = null;
	IndexSearcher searcherForInLinks = null;
	IndexSearcher searcherForDisamb = null;
	IndexSearcher searcherForAnchor = null;

	static Analyzer analyzer = null;
	static Analyzer analyzer2 = null;
	static Analyzer analyzer3 = null;

	QueryParser qp = null;
	QueryParser qpredirects = null;
	QueryParser qpdisamb = null;
	QueryParser qpid = null;
	QueryParser qpinLinks = null;
	QueryParser qpanchor = null;

	Query query = null; // the Query created by the QueryParser
	Query queryGroundEnt = null; // the Query created by the QueryParser

	TopDocs hits = null; // the search results
	TopDocs hitsforGroundEnt = null;
	TopDocs hitsforRedirects = null;
	TopDocs hitsforDisamb = null;
	TopDocs hitsforId = null;
	TopDocs hitsforinLinks = null;
	TopDocs hitsforanchor = null;

	public final Version LuceneVersion = Version.LUCENE_31;
	public final Version LuceneVersion1 = Version.LUCENE_36; // 36
	
	public TopDocs getHits(){
		return hits;
	}
	
	public IndexSearcher getSearcher(){
		return searcher;
	}

	private final Pattern outlink_pattern = Pattern
			.compile("\\|([0-9]*)?/([0-9]*)?\\|");
	private final Pattern freq_pattern = Pattern
			.compile("\\|([a-zA-Z]*)?|([0-9]*)?\\|");
	private final Pattern synopsis_pattern = Pattern
			.compile("\\|([\\sa-zA-Z][\\s0-9a-zA-Z]*)?\\|");
	private final Pattern digit_pattern = Pattern.compile("[0-9]+");
	private final Pattern adj_pattern = Pattern.compile("\\s(.*)?\\s");
	Instances dataset;
	Classifier cls;
	static Classifier loadedcls;
	static Instances loadeddataset;
	

	public LuceneIndexWrapper() {
		//SUNNY: ctor not complete anymore

		try {
			//System.out.println("Time taken by line 1 : " + diff2 + " milliseconds");
			
			//dataset = new Instances(new BufferedReader(new FileReader(props.getTrainLRDataset())));
//			dataset = new Instances(loadeddataset);
//			DisambendTime = System.currentTimeMillis();
//
//			diff2 = (DisambendTime - DisambstartTime);
//
//			System.out.println("Time taken by line 2 : " + diff2 + " milliseconds");
//			
//			dataset.setClassIndex(dataset.numAttributes() - 1);
//			
//			DisambendTime = System.currentTimeMillis();
//
//			diff2 = (DisambendTime - DisambstartTime);
//
//			System.out.println("Time taken by line 3 : " + diff2 + " milliseconds");
//			cls = loadedcls;
//
//			DisambendTime = System.currentTimeMillis();
//			
//			diff2 = (DisambendTime - DisambstartTime);

//			System.out.println("Time taken  by line 4 : " + diff2 + " milliseconds");
			
			if(searcher == null)
				searcher = new IndexSearcher(reader);
			if(searcherForId == null)
				searcherForId = new IndexSearcher(reader);
			if(searcherForRedirects == null)
				searcherForRedirects = new IndexSearcher(readerForRedirects);
			if(searcherForDisamb == null)
				searcherForDisamb = new IndexSearcher(readerForDisamb);
			if(searcherForInLinks == null)
				searcherForInLinks = new IndexSearcher(readerForInLinks);
			if(searcherForAnchor == null)
				searcherForAnchor = new IndexSearcher(readerForAnchor);
			
			qp = new QueryParser(LuceneVersion, "NE_type", analyzer);
			qpredirects = new QueryParser(LuceneVersion, "redirect_text",
					analyzer);
			qpdisamb = new QueryParser(LuceneVersion, "page_title", analyzer);
			qpinLinks = new QueryParser(LuceneVersion1, "page_id", analyzer2);
			qpid = new QueryParser(LuceneVersion, "page_id", analyzer2);
			qpanchor = new QueryParser(LuceneVersion1, "page_title", analyzer2);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}

	// Kanika : common constructor for all 5 indices. (category index not added
	// till now)
	public LuceneIndexWrapper(String indexLoc, String indexLocForRedirects,
			String indexInlinks, String indexLocForDisamb,
			String indexLocForAnchor) {
		// if (Config.wikiSense)
		// thesaurus = new Wikisaurus();
		if(indexName == null)
			indexName = indexLoc;
		if(indexNameForRedirects == null)
			indexNameForRedirects = indexLocForRedirects;
		if(indexNameForDisamb == null)
			indexNameForDisamb = indexLocForDisamb;
		if(indexNameForInLinks == null)
			indexNameForInLinks = indexInlinks;
		if(indexNameForAnchor == null)
			indexNameForAnchor = indexLocForAnchor;

		try {
			DisambProperties props = DisambProperties.getInstance();
			
			loadeddataset = new Instances(new BufferedReader(new FileReader(
					props.getTrainLRDataset())));
			dataset = loadeddataset;
			dataset.setClassIndex(dataset.numAttributes() - 1);
			loadedcls = (Classifier) weka.core.SerializationHelper.read(props.getLRModel());
			cls = loadedcls;

			if(reader == null)
				reader = IndexReader.open(FSDirectory.open(new File(indexName)),true);
			if(readerForRedirects == null)
				readerForRedirects = IndexReader.open(FSDirectory.open(new File(indexNameForRedirects)),true);
			if(readerForDisamb == null)
				readerForDisamb = IndexReader.open(FSDirectory.open(new File(indexNameForDisamb)),true);
			if(readerForInLinks == null)
				readerForInLinks = IndexReader.open(FSDirectory.open(new File(indexNameForInLinks)),true);
			if(readerForAnchor == null)
				readerForAnchor = IndexReader.open(FSDirectory.open(new File(indexLocForAnchor)),true);

			if(searcher == null)
				searcher = new IndexSearcher(reader);
			if(searcherForId == null)
				searcherForId = new IndexSearcher(reader);
			if(searcherForRedirects == null)
				searcherForRedirects = new IndexSearcher(readerForRedirects);
			if(searcherForDisamb == null)
				searcherForDisamb = new IndexSearcher(readerForDisamb);
			if(searcherForInLinks == null)
				searcherForInLinks = new IndexSearcher(readerForInLinks);
			if(searcherForAnchor == null)
				searcherForAnchor = new IndexSearcher(readerForAnchor);

			if(analyzer == null)
				analyzer = new SimpleAnalyzer(LuceneVersion); // construct our usual
			// analyzer
			if(analyzer2 == null)
				analyzer2 = new KeywordAnalyzer();
		
			if(analyzer3 == null)
				analyzer3 = new WhitespaceAnalyzer(LuceneVersion1);

			qp = new QueryParser(LuceneVersion, "NE_type", analyzer);
			qpredirects = new QueryParser(LuceneVersion, "redirect_text",
					analyzer);
			qpdisamb = new QueryParser(LuceneVersion, "page_title", analyzer);
			qpinLinks = new QueryParser(LuceneVersion1, "page_id", analyzer2);
			qpid = new QueryParser(LuceneVersion, "page_id", analyzer2);
			qpanchor = new QueryParser(LuceneVersion1, "page_title", analyzer2);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}

	// for InlinkIndex and CategIndex
	public LuceneIndexWrapper(String indexLoc) {
		indexName = indexLoc;
		indexNameForRedirects = null;
		try {
			reader = IndexReader.open(FSDirectory.open(new File(indexName)),
					true);
			searcher = new IndexSearcher(reader);
			analyzer = new SimpleAnalyzer(LuceneVersion);
			qp = new QueryParser(LuceneVersion, "NE_type", analyzer);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}

	// for original LuceneIndex
	public LuceneIndexWrapper(String indexLoc, String indexLocForRedirects) {
		indexName = indexLoc;
		indexNameForRedirects = indexLocForRedirects;
		try {
			reader = IndexReader.open(FSDirectory.open(new File(indexName)),
					true);
			readerForRedirects = IndexReader.open(
					FSDirectory.open(new File(indexNameForRedirects)), true);
			searcher = new IndexSearcher(reader);
			searcherForId = new IndexSearcher(reader);
			searcherForRedirects = new IndexSearcher(readerForRedirects);
			analyzer = new SimpleAnalyzer(LuceneVersion); // construct our usual
			// analyzer
			analyzer2 = new KeywordAnalyzer();
			qp = new QueryParser(LuceneVersion, "NE_type", analyzer);
			qpredirects = new QueryParser(LuceneVersion, "redirect_text",
					analyzer);
			qpid = new QueryParser(LuceneVersion, "page_id", analyzer2);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}

	private String buildpageidQuery(String idString) {
		String title = QueryParser.escape(idString);
		String queryString = "+page_id:(" + "\"" + title + "\"" + ")";
		return queryString;
	}

	private String buildDisambQuery(String phrase,
			KeywordsGroundTruth keywordsGroundTruth, int firstkeywordIndex,
			int lengthofPhrase) {
		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title))
			return null;
		String queryString = "+page_title:(" + "\"" + title.toLowerCase()
				+ "\"" + ")";
		return queryString;
	}

	private String buildRedirectQuery(String phrase,
			KeywordsGroundTruth keywordsGroundTruth, int firstkeywordIndex,
			int lengthofPhrase) {
		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title))
			return null;
		String queryString = "+redirect_text:(" + "\"" + title.toLowerCase()
				+ "\"" + ")";
		return queryString;
	}

	private String buildGroundEntQuery(String phrase) {
		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title))
			return null;
		String queryString = "+page_title:(" + title.toLowerCase() + ")";
		return queryString;

	}

	private String buildPageTitleSearchQueryNewIndex(String phrase) {
		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title)) {
			System.exit(1);
		}
		String queryString = "+OrginalText:(" + "\"" + title + "\"" + ")";
		return queryString;
	}

	private String buildPageTitleSearchAnchorQuery(String phrase) {
		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title)) {
			System.exit(1);
		}
		String queryString = "+page_title:(" + "\"" + title + "\"" + ")";
		return queryString;
	}

	public String buildPageTitleSearchQuery(String phrase) {
		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title)) {
			System.exit(1);
		}
		String queryString = "+page_title:(" + "\"" + title.toLowerCase()
				+ "\"" + ")";
		return queryString;
	}

	private String buildPhraseSearchQueryforTraining(String phrase,
			KeywordsGroundTruth keywordsGroundTruth, int firstkeywordIndex,
			int lengthofPhrase, String ids) {
		ArrayList<KeywordsGroundTruth.Mention> keywords = keywordsGroundTruth
				.getKeywords();
		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title)) {
			System.exit(1);
		}
		String queryString = "+page_title:("
				+ title.toLowerCase().replaceAll("OR", "") + ")";
		if (keywords.get(firstkeywordIndex).context != ""
				|| keywords.get(firstkeywordIndex).context != null) {
			queryString += " synopsis:("
					+ keywords.get(firstkeywordIndex).context.replaceAll(
							"[^0-9a-z\\sA-Z]", "").replaceAll("OR", "") + ")^4";
			queryString += " frequent:("
					+ keywords.get(firstkeywordIndex).context.replaceAll(
							"[^0-9a-z\\sA-Z]", "").replaceAll("OR", "") + ")^4";
			queryString += " synopsis_vbadj:("
					+ keywords.get(firstkeywordIndex).context.replaceAll(
							"[^0-9a-z\\sA-Z]", "").replaceAll("OR", "") + ")^4";
		}
		return queryString;
	}
	
	public String buildPhraseSearchQuery(String phrase,String context) {
		String title = QueryParser.escape(phrase);
		
		title = title.toLowerCase();
		
		List<String> qwords = Arrays.asList(title.split(" "));
		
		StringBuilder sb = new StringBuilder();
		int wcount = 0;
		for(String item: qwords){
			wcount++;
			item = item.concat("~");
			
			sb.append(item);
			
		    if(wcount != qwords.size()){
		    	sb.append(" AND ");
		    }
		}
		
		title = sb.toString();
		
		if (title == null || "".equals(title) || " ".equals(title)) {
			return null;
		}
		// String queryString = "+page_title:(" + "\"" + title.toLowerCase()
		// + "\"" + ")";
		String queryString = "+page_title:(" + title + ")";
//		if (context != ""
//				|| context != null) {
//			queryString += " synopsis:("
//					+ context
//							.replaceAll("[^0-9a-z\\sA-Z]", "")
//							.replaceAll("OR", "").replaceAll("AND", "")
//							.replaceAll("NOT", "") + ")^4";
//			queryString += " frequent:("
//					+ context
//							.replaceAll("[^0-9a-z\\sA-Z]", "")
//							.replaceAll("OR", "").replaceAll("AND", "")
//							.replaceAll("NOT", "") + ")^4";
//			queryString += " synopsis_vbadj:("
//					+ context
//							.replaceAll("[^0-9a-z\\sA-Z]", "")
//							.replaceAll("OR", "").replaceAll("AND", "")
//							.replaceAll("NOT", "") + ")^4";
//			queryString += " synonym:("
//				+ context
//						.replaceAll("[^0-9a-z\\sA-Z]", "")
//						.replaceAll("OR", "").replaceAll("AND", "")
//						.replaceAll("NOT", "") + ")^4";
//		}
		return queryString;
	}

	private String buildPhraseSearchQuery(String phrase,
			KeywordsGroundTruth keywordsGroundTruth, int firstkeywordIndex,
			int lengthofPhrase, String ids) {
		ArrayList<KeywordsGroundTruth.Mention> keywords = keywordsGroundTruth
				.getKeywords();
		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title)) {
			return null;
		}
		// String queryString = "+page_title:(" + "\"" + title.toLowerCase()
		// + "\"" + ")";
		String queryString = "+page_title:(" + title.toLowerCase() + ")";
		if (keywords.get(firstkeywordIndex).context != ""
				|| keywords.get(firstkeywordIndex).context != null) {
			queryString += " synopsis:("
					+ keywords.get(firstkeywordIndex).context
							.replaceAll("[^0-9a-z\\sA-Z]", "")
							.replaceAll("OR", "").replaceAll("AND", "")
							.replaceAll("NOT", "") + ")^4";
			queryString += " frequent:("
					+ keywords.get(firstkeywordIndex).context
							.replaceAll("[^0-9a-z\\sA-Z]", "")
							.replaceAll("OR", "").replaceAll("AND", "")
							.replaceAll("NOT", "") + ")^4";
			queryString += " synopsis_vbadj:("
					+ keywords.get(firstkeywordIndex).context
							.replaceAll("[^0-9a-z\\sA-Z]", "")
							.replaceAll("OR", "").replaceAll("AND", "")
							.replaceAll("NOT", "") + ")^4";
		}
		return queryString;
	}

	private void searchStringInDisambIndex(String queryString, int numNodes)
			throws Exception {
		try {
			//QueryParser myqpdisamb = new QueryParser(LuceneVersion, "page_title", analyzer);
			query = qpdisamb.parse(queryString);
			//query = myqpdisamb.parse(queryString);
			// System.out.println("query: " + queryString);
		} catch (ParseException e) {
			System.out.println("Error in parsing query: " + e.getMessage());
			System.out.println("query: " + queryString);
			System.exit(1);
		}
		try {
			hitsforDisamb = searcherForDisamb.search(query, numNodes); // run the query
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
		if (hitsforDisamb.totalHits == 0) {
			// empty result
		}
	}

	private void searchStringInRedirectionIndex(String queryString, int numNodes)
			throws Exception {
		try {
			//QueryParser myqpredirects = new QueryParser(LuceneVersion, "redirect_text",analyzer);
			query = qpredirects.parse(queryString);
			//query = myqpredirects.parse(queryString);
			// System.out.println("query: " + queryString);
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Error in parsing query: " + e.getMessage());
			System.out.println("query: " + queryString);
			System.exit(1);
		}
		try {
			hitsforRedirects = searcherForRedirects.search(query, numNodes); // run the query
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
		if (hitsforRedirects.totalHits == 0) {
			// empty result
		}
	}

	// private void searchTitleinNewIndex(String queryString, int maxHits) {
	// try {
	// query = qpNew.parse(queryString);
	// hitsNew = searcherNew.search(query, 3 * maxHits);
	// } catch (Exception e) {
	// System.out.println("Error in parsing query: " + e.getMessage());
	// System.out.println("query: " + queryString);
	// e.printStackTrace();
	// System.exit(1);
	// }
	// if (hitsNew.totalHits == 0) {
	// // empty result
	// }
	// }

	private void searchTitleInAnchorIndex(String queryString, int maxHits) {
		try {
			//QueryParser myqpanchor = new QueryParser(LuceneVersion1, "page_title", analyzer2);
			//query = myqpanchor.parse(queryString);
			query = qpanchor.parse(queryString);
			hitsforanchor = searcherForAnchor.search(query, 3 * maxHits); // run
			// the//
			// query
		} catch (Exception e) {
			System.out.println("Error in parsing query: " + e.getMessage());
			System.out.println("query: " + queryString);
			e.printStackTrace();
			System.exit(1);
		}
		if (hitsforanchor.totalHits == 0) {
			// empty result
		}
	}

	private void searchidInIndex(String queryString, int maxHits)
			throws Exception {
		try {
			//QueryParser myqpid = new QueryParser(LuceneVersion, "page_id", analyzer2);
			query = qpid.parse(queryString);
//			query = myqpid.parse(queryString);
		} catch (ParseException e) {
			System.out.println("Error in parsing query: " + e.getMessage());
			System.out.println("query: " + queryString);
			System.exit(1);
		}
		try {
			hitsforId = searcherForId.search(query, 3 * maxHits); // run the
			// query
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
		if (hitsforId.totalHits == 0) {
			// empty result
		}
	}
	
	public void searchTitleInCompleteIndex(String queryString, int maxHits) {
		try {
			//QueryParser myqp = new QueryParser(LuceneVersion, "NE_type", analyzer);
			queryGroundEnt = qpdisamb.parse(queryString);
			//queryGroundEnt = myqp.parse(queryString);

		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Error in parsing query: " + e.getMessage());
			System.out.println("query: " + queryString);
			System.exit(1);
		}
		try {
			hits = searcher.search(queryGroundEnt, maxHits);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
		if (hits.totalHits == 0) {
			// empty result
		}
	}

	private void searchTitleInIndex(String queryString) {
		try {
			//QueryParser myqp = new QueryParser(LuceneVersion, "NE_type", analyzer);
			queryGroundEnt = qp.parse(queryString);
			//queryGroundEnt = myqp.parse(queryString);

		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Error in parsing query: " + e.getMessage());
			System.out.println("query: " + queryString);
			System.exit(1);
		}
		try {
			hitsforGroundEnt = searcher.search(queryGroundEnt, 1000);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
		if (hitsforGroundEnt.totalHits == 0) {
			// empty result
		}
	}

	public void searchStringInIndex(String queryString, int maxHits)
			throws Exception {
		try {
			//QueryParser myqp = new QueryParser(LuceneVersion, "NE_type", analyzer);
			//query = myqp.parse(queryString);
			query = qp.parse(queryString);
			// System.out.println("query: " + queryString);
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Error in parsing query: " + e.getMessage());
			System.out.println("query: " + queryString);
			System.exit(1);
		}
		try {
			hits = searcher.search(query, 3 * maxHits); // run the query
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
		if (hits.totalHits == 0) {
			// empty result
		}
	}

	private void searchidInInlinksIndex(String queryString, int maxHits)
			throws Exception {
		try {
			//QueryParser myqpinLinks = new QueryParser(LuceneVersion1, "page_id", analyzer2);
			query = qpinLinks.parse(queryString);
			//query = myqpinLinks.parse(queryString);
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Error in parsing query: " + e.getMessage());
			System.out.println("query: " + queryString);
			System.exit(1);
		}
		try {
			hitsforinLinks = searcherForInLinks.search(query, 3 * maxHits); // run
			// the
			// query
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
		if (hitsforinLinks.totalHits == 0) {
			// empty result
		}
	}

	private ArrayList<Integer> parseInLinks(Integer entid) {
		ArrayList<Integer> inlinks = new ArrayList<Integer>();
		String query = buildInLinksQuery(entid);
		try {
			searchidInInlinksIndex(query, 4);
			if (!(hitsforinLinks.scoreDocs.length == 0)) {
				int h = 0;
				for (; h < hitsforinLinks.scoreDocs.length; h++) {
					Document doc = searcherForInLinks
							.doc(hitsforinLinks.scoreDocs[h].doc);
					String links = doc.get("inlinks");
					String[] allLinks = links.split(" ");
					for (int i = 0; i < allLinks.length; i++) {
						inlinks.add(Integer.parseInt(allLinks[i]));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return inlinks;
	}

	private ArrayList<String> parseSynAdjWords(String freq) {
		Matcher matcher = adj_pattern.matcher(freq);
		String word = new String();
		ArrayList<String> wordList = new ArrayList<String>();
		while (matcher.find()) {
			word = matcher.group(1);
			wordList.add(word.trim().toLowerCase());
		}
		return wordList;
	}

	private ArrayList<String> parseSynWords(String freq) {
		Matcher matcher = synopsis_pattern.matcher(freq);
		String word = new String();
		ArrayList<String> wordList = new ArrayList<String>();
		while (matcher.find()) {
			word = matcher.group(1);
			wordList.add(word.trim().toLowerCase());
		}
		return wordList;
	}

	private ArrayList<String> parseFreqWords(String freq) {
		Matcher matcher = freq_pattern.matcher(freq);
		String word = new String();
		ArrayList<String> wordList = new ArrayList<String>();
		while (matcher.find()) {
			word = matcher.group(1);
			wordList.add(word.trim().toLowerCase());
		}
		return wordList;
	}

	private ArrayList<Integer> parseOutlinks(String outlinks) {
		Matcher matcher = outlink_pattern.matcher(outlinks);
		String id = new String();
		ArrayList<Integer> idList = new ArrayList<Integer>();
		while (matcher.find()) {
			id = matcher.group(1);
			idList.add(Integer.parseInt(id));
		}
		return idList;
	}

	private String buildInLinksQuery(Integer id) {
		String entid = QueryParser.escape(id.toString());
		String queryString = "+page_id:(" + "\"" + entid + "\"" + ")";
		return queryString;
	}

	private HashMap<Integer, Integer> parseOutlinks1(String outlinks) {
		Matcher matcher = outlink_pattern.matcher(outlinks);
		String id = new String();
		String count = new String();
		HashMap<Integer, Integer> idCount = new HashMap<Integer, Integer>();
		while (matcher.find()) {
			id = matcher.group(1);
			count = matcher.group(2);
			idCount.put(Integer.parseInt(id), Integer.parseInt(count));
		}
		return idCount;
	}

	// for collective training purpose
	public NodePotentialsSet extractNodesForTraining(
			KeywordsGroundTruth keywordGroundTruth, int numNodes,
			int maxLength, String filename) {

		NodePotentialsSet np_set = new NodePotentialsSet();
		try {
			for (int i = 0; i < keywordGroundTruth.getGroundMentionNames()
					.size(); i++) {
				String key = keywordGroundTruth.getGroundMentionNames().get(i);
				int off = keywordGroundTruth.getGroundOffset().get(i);
				if (off < keywordGroundTruth.getDocumentText().length()) {
					String ent = keywordGroundTruth.getGroundTruth().get(i);
					if (ent == null) {
						continue;
					}
					HashSet<String> node_names = new HashSet<String>();
					NodePotentials redirectnode = new NodePotentials();
					ArrayList<NodePotentials> temp_set = new ArrayList<NodePotentials>();
					String redirectid = null;
					boolean groundAdded = false;
					String redirectquery = buildRedirectQuery(key,
							keywordGroundTruth, i, off + key.length());

					searchStringInRedirectionIndex(redirectquery, 1);

					if (!(hitsforRedirects.scoreDocs.length == 0)) {
						int h = 0;

						for (; h < numNodes
								&& h < hitsforRedirects.scoreDocs.length; h++) {
							Document doc = searcherForRedirects
									.doc(hitsforRedirects.scoreDocs[h].doc);
							String titleText = doc.get("redirect_text");
							if (!titleText.equalsIgnoreCase(key)) {
								continue;
							} else {

								redirectid = doc.get("page_id");
								String queryString = buildpageidQuery(redirectid);
								searchidInIndex(queryString, numNodes);

								if (!(hitsforId.scoreDocs.length == 0)) {
									int j = 0;
									for (; j < numNodes
											&& j < hitsforId.scoreDocs.length; j++) {
										Document doc1 = searcher
												.doc(hitsforId.scoreDocs[j].doc); // get
										// the
										// next
										// document
										String pagetitle = doc1
												.get("page_title"); // get its
										// title
										String pageid = doc1.get("page_id"); // get
										// its
										// id
										String disamb = doc1
												.get("title_disamb");
										if ((pagetitle == null)
												|| pagetitle.equals("")) // use
											// the
											// id
											// if
											// it
											// has
											// no
											// title
											continue;

										if (!((disamb == null) || disamb
												.equals(""))) // check for
											// disambiguation
											// tag
											pagetitle = pagetitle + " ("
													+ disamb + ")";

										if (ent.equalsIgnoreCase(pagetitle)) {
											redirectnode = setNodePotentials(
													doc1, key, off, j,
													hitsforId, true,
													keywordGroundTruth, i);
											redirectnode.redirection = 1;
											groundAdded = true;
											temp_set.add(redirectnode);
										} else {
											redirectnode = setNodePotentials(
													doc1, key, off, j,
													hitsforId, false,
													keywordGroundTruth, i);
											redirectnode.redirection = 1;
											temp_set.add(redirectnode);
										}

									}
								}
							}
						}
					}

					if (!ent.equalsIgnoreCase("NA")) {

						String groundquery = buildGroundEntQuery(ent);
						boolean flag = false;
						searchTitleInIndex(groundquery);

						if (!(hitsforGroundEnt.scoreDocs.length == 0)) {
							for (int j = 0; j < hitsforGroundEnt.scoreDocs.length; j++) {
								if (!flag) {
									Document docEnt = searcher
											.doc(hitsforGroundEnt.scoreDocs[j].doc); // get
									// the
									// next
									// document
									String pagetitle = docEnt.get("page_title"); // get
									// its
									// title
									String id = docEnt.get("page_id"); // get
									// its
									// id

									String disamb = docEnt.get("title_disamb");
									if (!((disamb == null) || disamb.equals(""))) // check
										// for
										// disambiguation
										// tag
										pagetitle = pagetitle + " (" + disamb
												+ ")";
									if (pagetitle.equalsIgnoreCase(ent)) {
										NodePotentials np = setNodePotentials(
												docEnt, key, off, j,
												hitsforGroundEnt, true,
												keywordGroundTruth, i);
										if (!redirectnode.name
												.equalsIgnoreCase(np.name)) {
											if (redirectid != null
													&& redirectid == id) {
												np.redirection = 1;
											}
											node_names.add(ent);
											temp_set.add(np);
											flag = true;
											break;
										}
									}
								}

							}

						}
					}

					String query = buildPhraseSearchQueryforTraining(key,
							keywordGroundTruth, i, off + key.length(), ""); // same
					// as
					// earlier
					// search
					// query

					query = query.toLowerCase().replaceAll("\\sand", "");
					searchStringInIndex(query, numNodes);

					if (!(hits.scoreDocs.length == 0)) {
						int redundancy = 0;
						int j = 0;
						for (; j < numNodes
								&& (j + redundancy) < hits.scoreDocs.length;) {
							Document doc = searcher.doc(hits.scoreDocs[j
									+ redundancy].doc); // get the next document
							String pagetitle = doc.get("page_title"); // get its
							// title
							String id = doc.get("page_id"); // get its id
							String disamb = doc.get("title_disamb");
							if ((pagetitle == null) || pagetitle.equals("")) // use
							// the
							// id
							// if
							// it
							// has
							// no
							// title
							{
								j++;
								continue;
							}

							if (!((disamb == null) || disamb.equals(""))) // check
								// for
								// disambiguation
								// tag
								pagetitle = pagetitle + " (" + disamb + ")";
							NodePotentials np = setNodePotentials(doc, key,
									off, j + redundancy, hits, false,
									keywordGroundTruth, i);
							if (redirectid != null && redirectid == id) {
								np.redirection = 1;
							}
							if (node_names.contains(pagetitle)) {
								redundancy++;
								continue;
							} else {
								node_names.add(pagetitle);
								temp_set.add(np);
							}

							j++;
						}
					}

					if (Config.wikiSense && temp_set != null) {
						setWikiSenseFeature(temp_set, key, off,
								keywordGroundTruth, i);
						setMentionAnchorFeature(temp_set, key, off,
								keywordGroundTruth, i);
						setLogisticScore(temp_set, key, off,
								keywordGroundTruth, i);
					}
					if (temp_set != null) {
						Collections.sort(temp_set,
								new Comparator<NodePotentials>() {
									@Override
									public int compare(NodePotentials o1,
											NodePotentials o2) {
										return Double.compare(
												o1.logistic_score,
												o2.logistic_score);
									}
								});
						boolean grdAdded = false;
						for (int c = 0; c < Config.maxCandidates
								&& c < temp_set.size(); c++) {
							if (temp_set.get(c).label == 1) {
								grdAdded = true;
							}
							np_set.potentials_set.add(temp_set.get(c));
						}
						if (grdAdded == false) {
							for (int c = 0; c < temp_set.size(); c++) {
								if (temp_set.get(c).label == 1) {
									np_set.potentials_set.add(temp_set.get(c));
								}
							}
						}

					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error in extractNodes: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		return np_set;

	}

	public NodePotentialsSet extractNodes(
			KeywordsGroundTruth keywordGroundTruth, int numNodes,
			int maxLength, String filename) {

		NodePotentialsSet np_set = new NodePotentialsSet();

		try {

			ArrayList<String> tokens = new ArrayList<String>();
			String text = keywordGroundTruth.getDocumentText();

			StringTokenizer str = new StringTokenizer(text);

			while (str.hasMoreTokens()) {
				String token = str.nextToken();
				if (token == null || "".equals(token))
					continue;
				// tokens.add(token.replaceAll("[^0-9a-z\\sA-Z/\\-]",""));
				tokens.add(token.replaceAll("_", " "));
			}

			ArrayList<String> keywords = keywordGroundTruth.getMentionNames();

			int curr_offset = 0;
			int s = 0;
			for (int i = 0; i < tokens.size() && s < keywords.size();) {

				if (!keywords.contains(tokens.get(i))) {
					i++;
					continue;
				}
				if (curr_offset == 0) {
					curr_offset = text.indexOf(tokens.get(i));
				} else {
					curr_offset = text.indexOf(tokens.get(i), curr_offset);
				}

				ArrayList<Integer> allOffset = new ArrayList<Integer>();
				ArrayList<String> allWords = new ArrayList<String>();

				allWords.addAll(tokens.subList(i,
						Math.min(tokens.size(), i + maxLength + 1)));
				allOffset.add(curr_offset);
				int k;

				// take n grams of specified length
				for (k = i + 1; k <= Math.min(tokens.size() - 1, i + maxLength); k++) {
					int off = text.indexOf(tokens.get(k), curr_offset);
					allOffset.add(off);
				}

				boolean foundDisamb = false;
				boolean foundRedirect = false;
				boolean flag = false;
				int finalIndex = 0;
				int l;

				// loop for all n-grams to 1 gram and break as we get any match
				for (l = 0; l <= allWords.size() - 1; l++) {

					// if flag is false consider n-l gram

					if (!(flag || foundRedirect || foundDisamb)) {
						String mentiontext = "";
						for (int h = 0; h < allWords.size() - l; h++) {
							if (h != allWords.size() - l - 1)
								mentiontext += allWords.get(h) + " ";
							else
								mentiontext += allWords.get(h);
						}

						ArrayList<NodePotentials> temp_set_combined = new ArrayList<NodePotentials>();
						HashSet<String> combined = new HashSet<String>();
						ArrayList<String> morphs = new ArrayList<String>();
						morphs = MorphologicalAnalyzer.analyze(mentiontext);
						String titleString = "";

						/*
						 * consider morphological variants for n grams string :
						 * note that it is not helpful in case of phrase but if
						 * we consider one token string then this is required.
						 */

						for (int m = 0; m < morphs.size(); m++) {
							if ("".equals(morphs.get(m))
									|| morphs.get(m) == null)
								continue;
							combined.add(morphs.get(m));
						}
						if (combined.size() == 0) {
							continue;
						}

						ArrayList<NodePotentials> temp_set_redirect = new ArrayList<NodePotentials>();
						ArrayList<NodePotentials> temp_set_disamb = new ArrayList<NodePotentials>();
						HashMap<String, String> temp_map_redirect = new HashMap<String, String>();
						HashMap<String, String> temp_map_disamb = new HashMap<String, String>();
						ArrayList<NodePotentials> temp_set = new ArrayList<NodePotentials>();
						HashMap<String, String> temp_map = new HashMap<String, String>();
						HashSet<String> node_names = new HashSet<String>();
						HashSet<String> pageIds = new HashSet<String>();

						// store n-gram string and its morphological variants in
						// HashSet
						for (String key : combined) {
							key = key.replaceAll("[^0-9a-z\\sA-Z/\\-]", "");
							String redirectquery = buildRedirectQuery(key,
									keywordGroundTruth, s, allWords.size() - l); // specific
							// to
							// redirects
							String disambquery = buildDisambQuery(key,
									keywordGroundTruth, s, allWords.size() - l); // specific
							// to
							// disambiguation

							searchStringInRedirectionIndex(redirectquery,
									numNodes); // query index for phrase search
							// query

							if (!(hitsforRedirects.scoreDocs.length == 0)) {
								int h = 0;

								for (; h < numNodes
										&& h < hitsforRedirects.scoreDocs.length; h++) {
									Document doc = searcherForRedirects
											.doc(hitsforRedirects.scoreDocs[h].doc);
									String titleText = doc.get("redirect_text");
									if (!titleText
											.equalsIgnoreCase(mentiontext)) {
										continue;
									} else {

										foundRedirect = true;
										ArrayList<NodePotentials> np = setNodePotentialsforRedirects(
												doc, key, allOffset.get(0), h,
												numNodes, temp_map_redirect,
												keywordGroundTruth, s);

										if (np != null) {
											for (int n = 0; n < np.size(); n++)
												if (!np.get(n).name
														.equalsIgnoreCase("Wiktionary")) {
													temp_set_redirect.add(np
															.get(n));
												}
										}
										String id = doc.get("page_id");
										pageIds.add(id);
									}
								}
							}
							if (Config.useDisambPages) {
								searchStringInDisambIndex(disambquery, numNodes);
								if (!(hitsforDisamb.scoreDocs.length == 0)) {
									int h = 0;
									for (; h < numNodes
											&& h < hitsforDisamb.scoreDocs.length; h++) {
										Document doc = searcherForDisamb
												.doc(hitsforDisamb.scoreDocs[h].doc);
										String titleText = doc
												.get("page_title");
										if (!(titleText
												.equalsIgnoreCase(mentiontext)
												|| mentiontext.replaceAll(
														"[,.;?!]", "")
														.equalsIgnoreCase(
																titleText) || mentiontext
												.replaceAll("[.,;?':!\\-\\*]",
														"").equalsIgnoreCase(
														titleText))) {
											continue;
										} else {
											foundDisamb = true;
											ArrayList<NodePotentials> np = setNodePotentialsforDisamb(
													doc, key, allOffset.get(0),
													h, numNodes,
													temp_map_disamb,
													keywordGroundTruth, s);

											// if(synopsis!=null&&contextWords!=null){
											// np.context_score =
											// (float)calcJaccard(synopsis,contextWords);
											// }

											if (np != null) {
												for (int n = 0; n < np.size(); n++)
													if (!np.get(n).name
															.equalsIgnoreCase("Wiktionary")) {
														temp_set_disamb.add(np
																.get(n));
													}
											}
											String ids = doc.get("id_list"); // get
											// its
											// id
											String[] idArr = ids.split(" ");
											for (String id : idArr) {
												pageIds.add(id);
											}
										}
									}
								}
							} // Config.useDisambPages
							String idList = "";
							for (String pid : pageIds) {
								idList += pid + " ";
							}

							String query = buildPhraseSearchQuery(key,
									keywordGroundTruth, s, allWords.size() - l,
									idList); // same as earlier search query

							String name = mentiontext;

							if (name.charAt(name.length() - 1) == '.'
									|| name.charAt(name.length() - 1) == ','
									|| name.charAt(name.length() - 1) == ';'
									|| name.charAt(name.length() - 1) == ':'
									|| name.charAt(name.length() - 1) == '?'
									|| name.charAt(name.length() - 1) == '!')
								name = name.substring(0, name.length() - 1);

							int length = allOffset.get(finalIndex)
									- allOffset.get(0)
									+ allWords.get(finalIndex).length();

							temp_map.put(name + "_" + allOffset.get(0), query);

							searchStringInIndex(query, numNodes); // query index
							// for
							// phrase
							// search
							// query

							if (!(hits.scoreDocs.length == 0)) {

								int redundancy = 0;
								int j = 0;
								for (; j < numNodes
										&& (j + redundancy) < hits.scoreDocs.length;) {
									// TermFreqVector tfv_title =
									// reader.getTermFreqVector(j+redundancy,
									// "page_title");
									// TermFreqVector tfv_syn =
									// reader.getTermFreqVector(j+redundancy,
									// "synopsis");
									Document doc = searcher
											.doc(hits.scoreDocs[j + redundancy].doc); // get
									// the
									// next
									// document
									String pagetitle = doc.get("page_title"); // get
									// its
									// title
									String id = doc.get("page_id"); // get its
									// id
									String disamb = doc.get("title_disamb");

									if ((pagetitle == null)
											|| pagetitle.equals("")) // use the
									// id if
									// it
									// has
									// no
									// title
									{
										j++;
										continue;
									}

									if (!((disamb == null) || disamb.equals(""))) // check
										// for
										// disambiguation
										// tag
										pagetitle = pagetitle + " (" + disamb
												+ ")";

									// if(tfv_title!=null){
									// String tterms[] = tfv_title.getTerms();
									// int termCount = tterms.length;
									// int freqs[] =
									// tfv_title.getTermFrequencies();
									//
									// for (int t=0; t < termCount; t++) {
									// double idf =
									// reader.numDocs()/reader.docFreq(new
									// Term("page_title", tterms[t]));
									// System.out.println(tterms[t] + " " +
									// freqs[t]*Math.log(idf));
									// }
									// }

									NodePotentials np = setNodePotentials(doc,
											name, allOffset.get(0), j
													+ redundancy, hits, false,
											keywordGroundTruth, s);

									if (node_names.contains(pagetitle)) {
										redundancy++;
										continue;
									} else {
										if (!np.name
												.equalsIgnoreCase("Wikitionary")) {
											node_names.add(pagetitle);
											temp_set.add(np);
										} else {
											redundancy++;
											continue;
										}
									}

									j++;
								}
							}

						} // end morph variant loop

						for (String entity : node_names) {
							if (mentiontext.equalsIgnoreCase(entity)
									|| mentiontext.replaceAll("[,.;?!]", "")
											.equalsIgnoreCase(entity)
									|| mentiontext.replaceAll(
											"[.,;?':!\\-\\*]", "")
											.equalsIgnoreCase(entity)) {
								flag = true;
								break;
							}
						}

						if (allWords.size() - l - 1 == 0) {
							String temp = KeywordsGroundTruth.tagger.tagString(
									allWords.get(0)).toString();
							if ((temp.split("_")[1].trim().equalsIgnoreCase(
									"DT")
									|| temp.split("_")[1].trim()
											.equalsIgnoreCase("PDT") || temp
										.split("_")[1].trim().equalsIgnoreCase(
									"CC"))) {
								flag = false;
								foundRedirect = false;
								foundDisamb = false;
							}
						}

						if (flag || foundDisamb || foundRedirect) {
							finalIndex = allWords.size() - l - 1;
							temp_set_combined.addAll(temp_set);
							np_set.mention_queries.putAll(temp_map);
						}
						if (foundRedirect) {
							finalIndex = allWords.size() - l - 1;
							temp_set_combined.addAll(temp_set_redirect);
							np_set.mention_queries.putAll(temp_map_redirect);
						}
						if (Config.useDisambPages && foundDisamb) {
							// System.out.println("found disamb for "+mentiontext);
							finalIndex = allWords.size() - l - 1;
							temp_set_combined.addAll(temp_set_disamb);
							np_set.mention_queries.putAll(temp_map_disamb);

						}
						if (flag || foundDisamb || foundRedirect) {
							if (Config.wikiSense) {
								setWikiSenseFeature(temp_set_combined,
										mentiontext, allOffset.get(0),
										keywordGroundTruth, s);
								// setMentionAnchorFeature(temp_set_combined,
								// mentiontext, allOffset.get(0),
								// keywordGroundTruth, s);
								setLogisticScore(temp_set_combined,
										mentiontext, allOffset.get(0),
										keywordGroundTruth, s);
							}
							if (temp_set_combined != null) {
								Collections.sort(temp_set_combined,
										new Comparator<NodePotentials>() {
											@Override
											public int compare(
													NodePotentials o1,
													NodePotentials o2) {
												return Double.compare(
														o1.logistic_score,
														o2.logistic_score);
											}
										});
								for (int c = 0; c < Config.maxCandidates
										&& c < temp_set_combined.size(); c++) {
									np_set.potentials_set.add(temp_set_combined
											.get(c));
								}
							}
						}

					} // end outermost if flag

				} // end of n-grams for loop
					// for(int c=0;c<=finalIndex;c++){
					// if(keywords.contains(allWords.get(c))){
					// s++;
					// }
					// }
					// i=i+finalIndex+1;
				i = i + 1;
				s = s + 1;
			} // end of tokens for loop

		} catch (Exception e) {
			System.out.println("Error in extractNodes: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		return np_set;
	}
	
	public class DisambRunnable implements Runnable {
		String disambquery;
		int numNodes;
		String key;
		KeywordsGroundTruth keywordGroundTruth;
		String ment;
		int offset;
		HashMap<String, String> temp_map_disamb;
		int i;
		List<NodePotentials> temp_set;
		HashSet<String> node_names;
		LuceneIndexWrapper obj;
		  public DisambRunnable(String _disambquery,int _numNodes,String _key,KeywordsGroundTruth _keywordGroundTruth,
					String _ment,int _offset,HashMap<String, String> _temp_map_disamb,int _i,List<NodePotentials> _temp_set,HashSet<String> _node_names,LuceneIndexWrapper _obj) {
			  	disambquery = _disambquery;
				numNodes = _numNodes;
				key = _key;
				keywordGroundTruth = _keywordGroundTruth;
				ment = _ment;
				offset = _offset;
				temp_map_disamb = _temp_map_disamb;
				i = _i;
				temp_set = _temp_set;
				node_names = _node_names;
				obj = _obj;
		  }

		  public void run() {
			  if (disambquery != null) {
					boolean foundDisamb = false;
					try {
						foundDisamb = disambHandler(disambquery, numNodes, key, keywordGroundTruth, ment, offset, temp_map_disamb, i,temp_set,node_names);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("foundDisamb : " + foundDisamb);
		    	}
		  }
		}
	
	public boolean disambHandler(String disambquery,int numNodes,String key,KeywordsGroundTruth keywordGroundTruth,
			String ment,int offset,HashMap<String, String> temp_map_disamb,int i,List<NodePotentials> temp_set,HashSet<String> node_names) throws Exception{
//	public Set<NodePotentials> disambHandler(String disambquery,int numNodes,String key,KeywordsGroundTruth keywordGroundTruth,
//			String ment,int offset,HashMap<String, String> temp_map_disamb,int i,boolean foundDisamb) throws Exception{
	
		//HashSet<String> node_names = new HashSet<String>();
		
		//Set<NodePotentials> temp_set = new TreeSet<NodePotentials>(new NameComparator());
		boolean foundDisamb = false;
		
		searchStringInDisambIndex(disambquery, numNodes);
		
		if (!(hitsforDisamb.scoreDocs.length == 0)) {
			int h = 0;
			for (; h < numNodes
					&& h < hitsforDisamb.scoreDocs.length; h++) {

				Document doc = searcherForDisamb
				.doc(hitsforDisamb.scoreDocs[h].doc);
				
				String titleText = doc.get("page_title");
				if (!(titleText.equalsIgnoreCase(key)
						|| key.replaceAll("[,.;?!]", "")
								.equalsIgnoreCase(titleText) || key
						.replaceAll("[.,;?':!\\-\\*]", "")
						.equalsIgnoreCase(titleText))
						|| key.replaceAll("[^0-9a-z\\sA-Z/\\-]", "")
								.equalsIgnoreCase(titleText)) {
					continue;
				} else {
					foundDisamb = true;
					ArrayList<NodePotentials> np = setNodePotentialsforDisamb(
							doc, ment, offset, h, numNodes,
							temp_map_disamb, keywordGroundTruth, i);
					

					if (np != null) {
						for (int n = 0; n < np.size(); n++)
							if (!np.get(n).name.equalsIgnoreCase("Wiktionary")
									&& !node_names.contains(np.get(n).name)) {
								temp_set.add(np.get(n));
								node_names.add(np.get(n).name);
							}
					}
				}
			}
		}
		return foundDisamb;
	}
	
	public class RedirectRunnable implements Runnable {
		String redirectquery;
		int numNodes;String key;
		KeywordsGroundTruth keywordGroundTruth;
		String ment;
		int offset;HashMap<String, String> temp_map_redirect;
		int i;
		List<NodePotentials> temp_set;
		HashSet<String> node_names;
		LuceneIndexWrapper obj;
		
		  public RedirectRunnable(String _redirectquery,int _numNodes,String _key,KeywordsGroundTruth _keywordGroundTruth,
					String _ment,int _offset,HashMap<String, String> _temp_map_redirect,int _i,List<NodePotentials> _temp_set,HashSet<String> _node_names,LuceneIndexWrapper _obj) {
			  	redirectquery = _redirectquery;
				numNodes = _numNodes;
				key = _key;
				keywordGroundTruth = _keywordGroundTruth;
				ment = _ment;
				offset = _offset;
				temp_map_redirect = _temp_map_redirect;
				i = _i;
				temp_set = _temp_set;
				node_names = _node_names;
				obj = _obj;
		  }

		  public void run() {
			  if (redirectquery != null) {
					boolean foundRedirect = false;
					try {
						foundRedirect = obj.redirectHandler(redirectquery,numNodes,key,keywordGroundTruth,ment,offset,temp_map_redirect,i,temp_set,node_names);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("foundRedirect : " + foundRedirect);
		    	}
		  }
		}
	
	public boolean redirectHandler(String redirectquery,int numNodes,String key,KeywordsGroundTruth keywordGroundTruth,
			String ment,int offset,HashMap<String, String> temp_map_redirect,int i,List<NodePotentials> temp_set,HashSet<String> node_names) throws Exception{
//	public Set<NodePotentials> redirectHandler(String redirectquery,int numNodes,String key,KeywordsGroundTruth keywordGroundTruth,
//				String ment,int offset,HashMap<String, String> temp_map_redirect,int i,boolean foundRedirect) throws Exception{

		
	//	HashSet<String> node_names = new HashSet<String>();
		
		//Set<NodePotentials> temp_set = new TreeSet<NodePotentials>(new NameComparator());
		
		searchStringInRedirectionIndex(redirectquery, numNodes);
		
		boolean foundRedirect = false;
		
		if (!(hitsforRedirects.scoreDocs.length == 0)) {
			
			int h = 0;
			
			for (; h < numNodes
					&& h < hitsforRedirects.scoreDocs.length; h++) {
				
//				long dsstartTime = System.currentTimeMillis();

				Document doc = searcherForRedirects
				.doc(hitsforRedirects.scoreDocs[h].doc);
				
//				long dsendTime = System.currentTimeMillis();
				
//				docsearchertime += (dsendTime - dsstartTime);
				
				String titleText = doc.get("redirect_text");
				if (!titleText
						.replaceAll("[^0-9a-z\\sA-Z/\\-]", "")
						.equalsIgnoreCase(key)) {
					continue;
				} else {
					foundRedirect = true;
					System.out.println("redirect text : " + titleText);
					ArrayList<NodePotentials> np = setNodePotentialsforRedirects(
							doc, ment, offset, h, numNodes,
							temp_map_redirect, keywordGroundTruth,
							i);
					

					if (np != null) {
						for (int n = 0; n < np.size(); n++)
							if (!np.get(n).name.equalsIgnoreCase("Wiktionary")
									&& !node_names.contains(np.get(n).name)) {
								temp_set.add(np.get(n));
								node_names.add(np.get(n).name);
							}
					}
				}
			}
		}
		return foundRedirect;
	}
	
	public class SearchIndexRunnable implements Runnable {
		String query;
		int numNodes;String name;
		KeywordsGroundTruth keywordGroundTruth;
		int offset;
		int i;
		List<NodePotentials> temp_set;
		HashSet<String> node_names;
		LuceneIndexWrapper obj;
		
		  public SearchIndexRunnable(String _query,int _numNodes,String _name,KeywordsGroundTruth _keywordGroundTruth,
					int _offset,int _i,List<NodePotentials> _temp_set,HashSet<String> _node_names,LuceneIndexWrapper _obj) {
			  	query = _query;
				numNodes = _numNodes;
				name = _name;
				keywordGroundTruth = _keywordGroundTruth;
				offset = _offset;
				i = _i;
				temp_set = _temp_set;
				node_names = _node_names;
				obj = _obj;
		  }

		  public void run() {
			  if (query != null) {
					try {
						obj.searchIndexHandler(query,numNodes,name, keywordGroundTruth, offset, i,temp_set,node_names);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
		  }
		}
	
	public void searchIndexHandler(String query,int numNodes,String name,KeywordsGroundTruth keywordGroundTruth,
		int offset,int i,List<NodePotentials> temp_set,HashSet<String> node_names) throws Exception{
//	public Set<NodePotentials> searchIndexHandler(String query,int numNodes,String name,KeywordsGroundTruth keywordGroundTruth,
//		int offset,int i) throws Exception{
		
//		HashSet<String> node_names = new HashSet<String>();
		
	//	Set<NodePotentials> temp_set = new TreeSet<NodePotentials>(new NameComparator());
		
		//System.out.println("searching " + query + " in index.");
		searchStringInIndex(query, numNodes);
		
		if (!(hits.scoreDocs.length == 0)) {
			int redundancy = 0;
			int j = 0;
			boolean flag1 = false;
			boolean flag2 = false;
			for (; flag1 && flag2
					&& (j + redundancy) < hits.scoreDocs.length;) {
				if (j >= numNodes)
					flag1 = true;
				
				Document doc = searcher.doc(hits.scoreDocs[j
				       									+ redundancy].doc);
				
				String pagetitle = doc.get("page_title");
				String disamb = doc.get("title_disamb");
				if ((pagetitle == null) || pagetitle.equals("")) {
					j++;
					continue;
				}
				if (!((disamb == null) || disamb.equals("")))
					pagetitle = pagetitle + " (" + disamb + ")";
				
				//System.out.println("searched:" +  pagetitle);

				if (name.equals(pagetitle)) {
					flag2 = true;
				} else if (!flag1) {
					redundancy++;
					continue;
				}

				if (!compatibleNamePageTitle(name, pagetitle)) {
					redundancy++;
					continue;
				}
				
				NodePotentials np = setNodePotentials(doc, name,
						offset, j + redundancy, hits, false,
						keywordGroundTruth, i);
				
				if (node_names.contains(pagetitle)) {
					redundancy++;
					continue;
				} else {
					if (!np.name.equalsIgnoreCase("Wikitionary")
							&& !node_names.contains(pagetitle)) {
						node_names.add(pagetitle);
						temp_set.add(np);
					} else {
						redundancy++;
						continue;
					}
				}
				j++;
			}
		}
		//return temp_set;
	}
	
	public void sensesHandler(LabelSense senses,String ment,KeywordsGroundTruth keywordGroundTruth,
			int offset,int i,List<NodePotentials> temp_set,HashSet<String> node_names) throws Exception{
	
//	public Set<NodePotentials> sensesHandler(LabelSense senses,String ment,KeywordsGroundTruth keywordGroundTruth,
//			int offset,int i) throws Exception{

//		HashSet<String> node_names = new HashSet<String>();

	//	Set<NodePotentials> temp_set = new TreeSet<NodePotentials>(new NameComparator());

		for (int e = 0; e < senses.wikiMinerCandidate.length; e++) {
			String ent = senses.wikiMinerCandidate[e];
			//System.out.println("ent : " + ent);
			float senseprob = (float) senses.wikiMinerProbability[e];

			String wikiMinerquery = buildGroundEntQuery(ent);
			if (wikiMinerquery != null) {
				boolean flag = false;

				searchTitleInIndex(wikiMinerquery);

				if (!(hitsforGroundEnt.scoreDocs.length == 0)) {
					for (int j = 0; j < hitsforGroundEnt.scoreDocs.length; j++) {
						if (!flag) {

							Document docEnt = searcher
							.doc(hitsforGroundEnt.scoreDocs[j].doc);

							String pagetitle = docEnt
							.get("page_title");
							String disamb = docEnt
							.get("title_disamb");
							if (!((disamb == null) || disamb
									.equals("")))
								pagetitle = pagetitle + " ("
								+ disamb + ")";
							//							System.out.println("pagetitle:" +  pagetitle);

							if (pagetitle.equals(ent)
									&& !node_names.contains(ent)) {


								NodePotentials np = setNodePotentials(
										docEnt, ment, offset, j,
										hitsforGroundEnt, false,
										keywordGroundTruth, i);

								np.sense_probability = senseprob;
								node_names.add(ent);
								temp_set.add(np);
								flag = true;
								break;

							} else if (pagetitle.equals(ent) && node_names.contains(ent)) {
								for (int t = 0; t < temp_set.size(); t++) {
									if (temp_set.get(t).name.equalsIgnoreCase(pagetitle) || temp_set.get(t).name.equalsIgnoreCase(ent)) {
										temp_set.get(t).sense_probability = senseprob;
										break;
									}
								}
//								for (NodePotentials np : temp_set) {
//									if (np.name.equalsIgnoreCase(pagetitle) || np.name.equalsIgnoreCase(ent)) {
//										np.sense_probability = senseprob;
//										break;
//									}
//								}
							}
						}

					}

				}
			}
		}
		//return temp_set;
	}


	public NodePotentialsSet extractNodesNewConsolidation(
			KeywordsGroundTruth keywordGroundTruth, int numNodes,
			int maxLength, String filename) {
		NodePotentialsSet np_set = new NodePotentialsSet();
		try {

			ArrayList<String> keywords = keywordGroundTruth.getMentionNames();
			long redirecttotaltime = 0,distotaltime = 0,strtotaltime = 0,
			titletotaltime = 0,setnptotal = 0, sorttotal = 0,totalaftime = 0,docsearchertime = 0;

			for (int i = 0; i < keywords.size(); i++) {
				
				LabelSense senses = keywordGroundTruth.getKeywords().get(i).senses;
				
				String key = keywords.get(i);
				String ment = keywords.get(i);
				
				int offset = keywordGroundTruth.getKeywords().get(i).offset;
				
				HashMap<String, String> temp_map_redirect = new HashMap<String, String>();
				HashMap<String, String> temp_map_disamb = new HashMap<String, String>();
				HashMap<String, String> temp_map = new HashMap<String, String>();
				//List<NodePotentials> temp_set = new ArrayList<NodePotentials>();
				List<NodePotentials> temp_set = new ArrayList<NodePotentials>();
//				Set<NodePotentials> temp_set = new TreeSet<NodePotentials>(new NameComparator());
				Set<NodePotentials> temp_set_all = new TreeSet<NodePotentials>(new Comparator<NodePotentials>() {
					@Override
					public int compare(NodePotentials o1,
							NodePotentials o2) {
						return o1.name.compareTo(o2.name);
					}
				});
//				Set<NodePotentials> temp_set_redirect = new TreeSet<NodePotentials>(new NameComparator());
//				Set<NodePotentials> temp_set_disamb = new TreeSet<NodePotentials>(new NameComparator());
//				Set<NodePotentials> temp_set_index = new TreeSet<NodePotentials>(new NameComparator());
//				Set<NodePotentials> temp_set_senses = new TreeSet<NodePotentials>(new NameComparator());
				List<NodePotentials> temp_set_redirect = new ArrayList<NodePotentials>();
				List<NodePotentials> temp_set_disamb = new ArrayList<NodePotentials>();
				List<NodePotentials> temp_set_index = new ArrayList<NodePotentials>();
				List<NodePotentials> temp_set_senses = new ArrayList<NodePotentials>();

				HashSet<String> node_names = new HashSet<String>();
				HashSet<String> node_names_redirect = new HashSet<String>();
				HashSet<String> node_names_disamb = new HashSet<String>();
				HashSet<String> node_names_index = new HashSet<String>();
				HashSet<String> node_names_senses = new HashSet<String>();

				key = key.replaceAll("[^0-9a-z\\sA-Z/\\-]", "").toLowerCase();

				String redirectquery = buildRedirectQuery(key,
						keywordGroundTruth, i, offset);
				String disambquery = buildDisambQuery(key, keywordGroundTruth,
						i, offset);
				String query = buildPhraseSearchQuery(key, keywordGroundTruth,
						i, offset, "");
				

				String name = ment;

				if (name.charAt(name.length() - 1) == '.'
						|| name.charAt(name.length() - 1) == ','
						|| name.charAt(name.length() - 1) == ';'
						|| name.charAt(name.length() - 1) == ':'
						|| name.charAt(name.length() - 1) == '?'
						|| name.charAt(name.length() - 1) == '!')

					name = name.substring(0, name.length() - 1);

				temp_map.put(name + "_" + offset, query);

				boolean foundRedirect = false;
				boolean foundDisamb = false;
				Thread thread1 = new Thread(new RedirectRunnable(redirectquery,numNodes,key,keywordGroundTruth,ment,offset,temp_map_redirect,i,temp_set_redirect,node_names_redirect, this));
				Thread thread2 = new Thread(new DisambRunnable(disambquery, numNodes, key, keywordGroundTruth, ment, offset, temp_map_disamb, i,temp_set_disamb,node_names_disamb, this));
				Thread thread3 = new Thread(new SearchIndexRunnable(query,numNodes,name, keywordGroundTruth, offset, i,temp_set_index,node_names_index, this));
				
				thread1.start();
				thread2.start();
				thread3.start();
				
				thread1.join();
				thread2.join();
				thread3.join();
				
				foundRedirect = (temp_set_redirect.size() > 0 ? true : false);
				foundDisamb = (temp_set_disamb.size() > 0 ? true : false);
				
//				Thread thread1 = new Thread(new Runnable()
//				{
//				   public void run()
//				   {
//				       // this will be run in a separate thread
//						if (redirectquery != null) 
//							foundRedirect = redirectHandler(redirectquery,numNodes,key,keywordGroundTruth,ment,offset,temp_map_redirect,i,temp_set_redirect,node_names_redirect);
//				   }
//				});
//
//				// start the thread
//
//				thread1.start(); 
	/*			
				if (redirectquery != null) {
					
					foundRedirect = redirectHandler(redirectquery,numNodes,key,keywordGroundTruth,ment,offset,temp_map_redirect,i,temp_set_redirect,node_names_redirect);
					System.out.println("foundRedirect : " + foundRedirect);*/
					//temp_set_redirect = redirectHandler(redirectquery,numNodes,key,keywordGroundTruth,ment,offset,temp_map_redirect,i,foundRedirect);
					/*
					long redstartTime = System.currentTimeMillis();

					searchStringInRedirectionIndex(redirectquery, numNodes);
					
					long redendTime = System.currentTimeMillis();
					
					redirecttotaltime += (redendTime - redstartTime);

					if (!(hitsforRedirects.scoreDocs.length == 0)) {
						
						int h = 0;
						
						for (; h < numNodes
								&& h < hitsforRedirects.scoreDocs.length; h++) {
							
							long dsstartTime = System.currentTimeMillis();

							Document doc = searcherForRedirects
							.doc(hitsforRedirects.scoreDocs[h].doc);
							
							long dsendTime = System.currentTimeMillis();
							
							docsearchertime += (dsendTime - dsstartTime);
							
							String titleText = doc.get("redirect_text");
							if (!titleText
									.replaceAll("[^0-9a-z\\sA-Z/\\-]", "")
									.equalsIgnoreCase(key)) {
								continue;
							} else {
								foundRedirect = true;
								System.out.println("redirect text : " + titleText);
								long setnpstartTime = System.currentTimeMillis();
								ArrayList<NodePotentials> np = setNodePotentialsforRedirects(
										doc, ment, offset, h, numNodes,
										temp_map_redirect, keywordGroundTruth,
										i);
								long setnpendTime = System.currentTimeMillis();
								
								setnptotal += (setnpendTime - setnpstartTime);

								if (np != null) {
									for (int n = 0; n < np.size(); n++)
										if (!np.get(n).name.equalsIgnoreCase("Wiktionary")
												&& !node_names.contains(np.get(n).name)) {
											temp_set.add(np.get(n));
											node_names.add(np.get(n).name);
										}
								}
							}
						}
					}*/
		/*		}
				if (Config.useDisambPages && disambquery != null) {
					
					foundDisamb = disambHandler(disambquery, numNodes, key, keywordGroundTruth, ment, offset, temp_map_disamb, i,temp_set_disamb,node_names_disamb);
					//temp_set_disamb = disambHandler(disambquery, numNodes, key, keywordGroundTruth, ment, offset, temp_map_disamb, i, foundDisamb);
					*//*
					long disstartTime = System.currentTimeMillis();

					searchStringInDisambIndex(disambquery, numNodes);
					
					long disendTime = System.currentTimeMillis();
					
					distotaltime += (disendTime - disstartTime);
					
					if (!(hitsforDisamb.scoreDocs.length == 0)) {
						int h = 0;
						for (; h < numNodes
								&& h < hitsforDisamb.scoreDocs.length; h++) {
							long dsstartTime = System.currentTimeMillis();

							Document doc = searcherForDisamb
							.doc(hitsforDisamb.scoreDocs[h].doc);
							
							long dsendTime = System.currentTimeMillis();
							
							docsearchertime += (dsendTime - dsstartTime);
							
							String titleText = doc.get("page_title");
							if (!(titleText.equalsIgnoreCase(key)
									|| key.replaceAll("[,.;?!]", "")
											.equalsIgnoreCase(titleText) || key
									.replaceAll("[.,;?':!\\-\\*]", "")
									.equalsIgnoreCase(titleText))
									|| key.replaceAll("[^0-9a-z\\sA-Z/\\-]", "")
											.equalsIgnoreCase(titleText)) {
								continue;
							} else {
								foundDisamb = true;
								long setnpstartTime = System.currentTimeMillis();
								ArrayList<NodePotentials> np = setNodePotentialsforDisamb(
										doc, ment, offset, h, numNodes,
										temp_map_disamb, keywordGroundTruth, i);
								long setnpendTime = System.currentTimeMillis();
								
								setnptotal += (setnpendTime - setnpstartTime);

								if (np != null) {
									for (int n = 0; n < np.size(); n++)
										if (!np.get(n).name
												.equalsIgnoreCase("Wiktionary")
												&& !node_names.contains(np
														.get(n).name)) {
											temp_set.add(np.get(n));
											node_names.add(np.get(n).name);
										}
								}
							}
						}
					}*/
			//	} // Config.useDisambPages



/*				if (query != null) {
					
					searchIndexHandler(query,numNodes,name, keywordGroundTruth, offset, i,temp_set_index,node_names_index);*/
					//temp_set_index = searchIndexHandler(query,numNodes,name, keywordGroundTruth, offset, i);
					/*
					//System.out.println("searching " + query + " in index.");
					long strstartTime = System.currentTimeMillis();
					
					

					searchStringInIndex(query, numNodes);
					
					long strendTime = System.currentTimeMillis();
					
					strtotaltime += (strendTime - strstartTime);
					
					
					//System.out.println("hits.scoreDocs.length : " + hits.scoreDocs.length); 

					if (!(hits.scoreDocs.length == 0)) {
						int redundancy = 0;
						int j = 0;
						boolean flag1 = false;
						boolean flag2 = false;
						for (; flag1 && flag2
								&& (j + redundancy) < hits.scoreDocs.length;) {
							if (j >= numNodes)
								flag1 = true;
							
							long dsstartTime = System.currentTimeMillis();

							Document doc = searcher.doc(hits.scoreDocs[j
							       									+ redundancy].doc);
							
							long dsendTime = System.currentTimeMillis();
							
							docsearchertime += (dsendTime - dsstartTime);
							
							String pagetitle = doc.get("page_title");
							String disamb = doc.get("title_disamb");
							if ((pagetitle == null) || pagetitle.equals("")) {
								j++;
								continue;
							}
							if (!((disamb == null) || disamb.equals("")))
								pagetitle = pagetitle + " (" + disamb + ")";
							
							//System.out.println("searched:" +  pagetitle);

							if (name.equals(pagetitle)) {
								flag2 = true;
							} else if (!flag1) {
								redundancy++;
								continue;
							}

							if (!compatibleNamePageTitle(name, pagetitle)) {
								redundancy++;
								continue;
							}
							
							long setnpstartTime = System.currentTimeMillis();
							NodePotentials np = setNodePotentials(doc, name,
									offset, j + redundancy, hits, false,
									keywordGroundTruth, i);
							long setnpendTime = System.currentTimeMillis();
							
							setnptotal += (setnpendTime - setnpstartTime);

							if (node_names.contains(pagetitle)) {
								redundancy++;
								continue;
							} else {
								if (!np.name.equalsIgnoreCase("Wikitionary")
										&& !node_names.contains(pagetitle)) {
									node_names.add(pagetitle);
									temp_set.add(np);
								} else {
									redundancy++;
									continue;
								}
							}
							j++;
						}
					}*/
				//}
				temp_set_all.addAll(temp_set_index);
				temp_set_all.addAll(temp_set_redirect);
				temp_set_all.addAll(temp_set_disamb);
				
				temp_set = new ArrayList<NodePotentials>(temp_set_all);
				node_names.addAll(node_names_redirect);
				node_names.addAll(node_names_disamb);
				node_names.addAll(node_names_index);
				
				if (senses != null) {
					//sensesHandler(senses,ment, keywordGroundTruth, offset, i,temp_set_senses,node_names);
					sensesHandler(senses,ment, keywordGroundTruth, offset, i,temp_set,node_names);
					//temp_set_senses = sensesHandler(senses,ment, keywordGroundTruth, offset, i);
					/*
					for (int e = 0; e < senses.wikiMinerCandidate.length; e++) {
						String ent = senses.wikiMinerCandidate[e];
						//System.out.println("ent : " + ent);
						float senseprob = (float) senses.wikiMinerProbability[e];

						String wikiMinerquery = buildGroundEntQuery(ent);
						if (wikiMinerquery != null) {
							boolean flag = false;

							long titlestartTime = System.currentTimeMillis();

							searchTitleInIndex(wikiMinerquery);
							
							long titleendTime = System.currentTimeMillis();
							
							titletotaltime += (titleendTime - titlestartTime);
							
							//System.out.println("hitsforGroundEnt : " + hitsforGroundEnt.scoreDocs.length); 

							if (!(hitsforGroundEnt.scoreDocs.length == 0)) {
								for (int j = 0; j < hitsforGroundEnt.scoreDocs.length; j++) {
									if (!flag) {
										long dsstartTime = System.currentTimeMillis();

										Document docEnt = searcher
										.doc(hitsforGroundEnt.scoreDocs[j].doc);
										
										long dsendTime = System.currentTimeMillis();
										
										docsearchertime += (dsendTime - dsstartTime);
										
										String pagetitle = docEnt
												.get("page_title");
										String disamb = docEnt
												.get("title_disamb");
										if (!((disamb == null) || disamb
												.equals("")))
											pagetitle = pagetitle + " ("
													+ disamb + ")";
//										System.out.println("pagetitle:" +  pagetitle);
										
										if (pagetitle.equals(ent)
												&& !node_names.contains(ent)) {
											

											long setnpstartTime = System.currentTimeMillis();
											NodePotentials np = setNodePotentials(
													docEnt, ment, offset, j,
													hitsforGroundEnt, false,
													keywordGroundTruth, i);
											long setnpendTime = System.currentTimeMillis();
											
											setnptotal += (setnpendTime - setnpstartTime);
											
											np.sense_probability = senseprob;
											node_names.add(ent);
											temp_set.add(np);
											flag = true;
											break;

										} else if (pagetitle.equals(ent) && node_names.contains(ent)) {
											for (int t = 0; t < temp_set.size(); t++) {
												if (temp_set.get(t).name.equalsIgnoreCase(pagetitle) || temp_set.get(t).name.equalsIgnoreCase(ent)) {
													temp_set.get(t).sense_probability = senseprob;
													break;
												}
											}
//											for (NodePotentials np : temp_set) {
//												if (np.name.equalsIgnoreCase(pagetitle) || np.name.equalsIgnoreCase(ent)) {
//													np.sense_probability = senseprob;
//													break;
//												}
//											}
										}
									}

								}

							}
						}
					}*/
				}
				
//				System.out.println("temp_set_redirect : " + temp_set_redirect.size());
//				System.out.println("temp_set_disamb : " + temp_set_disamb.size());
//				System.out.println("temp_set_index : " + temp_set_index.size());
//				System.out.println("temp_set_senses : " + temp_set_senses.size());
//				
//				temp_set_all.addAll(temp_set_senses);
//				temp_set_all.addAll(temp_set_index);
//				temp_set_all.addAll(temp_set_redirect);
//				temp_set_all.addAll(temp_set_disamb);
//				
//				
//				System.out.println("temp_set_all : " + temp_set_all.size());
				
//				temp_set = new ArrayList<NodePotentials>(temp_set_all);
				
				if (temp_map != null || temp_map.size() != 0)
					np_set.mention_queries.putAll(temp_map);
				if (foundRedirect)
					np_set.mention_queries.putAll(temp_map_redirect);
				if (Config.useDisambPages && foundDisamb)
					np_set.mention_queries.putAll(temp_map_disamb);
				
				long mafstartTime = System.currentTimeMillis();
				
				if (temp_set != null && temp_set.size() != 0) {
					if (Config.wikiSense) {
						// setWikiSenseFeature(temp_set, key, offset,
						// keywordGroundTruth, i);
						setMentionAnchorFeature(temp_set, key, offset,
								keywordGroundTruth, i);
						if (Config.logistic)
							setLogisticScore(temp_set, key, offset,
									keywordGroundTruth, i);
					}
				
				long mafendTime = System.currentTimeMillis();
				
				totalaftime += (mafendTime - mafstartTime);

					long sortstartTime = System.currentTimeMillis();
					Collections.sort(temp_set,
							new Comparator<NodePotentials>() {
								@Override
								public int compare(NodePotentials o1,
										NodePotentials o2) {
									return Double.compare(o1.logistic_score,
											o2.logistic_score);
								}
							});
					long sortendTime = System.currentTimeMillis();
					
					sorttotal += (sortendTime - sortstartTime);
					
					
					List<NodePotentials> final_temp = temp_set.subList(0, Math
							.min(temp_set.size(), (int) Config.maxCandidates));
					np_set.potentials_set.addAll(final_temp);
				}

			}// end of main for loop on keyword mentions
//			System.out.println("Time taken by redirect : " + redirecttotaltime + " milliseconds");
//			System.out.println("Time taken by disamb : " + distotaltime + " milliseconds");
//			System.out.println("Time taken by search index : " + strtotaltime + " milliseconds");
//			System.out.println("Time taken by search title : " + titletotaltime + " milliseconds");
//			System.out.println("Time taken by set np : " + setnptotal + " milliseconds");
//			System.out.println("Time taken by sort: " + sorttotal + " milliseconds");
//			System.out.println("Time taken by mention anchor feature: " + totalaftime + " milliseconds");
//			System.out.println("Time taken by doc searcher: " + docsearchertime + " milliseconds");
		} catch (Exception e) {
			System.out.println("Error in extractNodes: " + e.getMessage());
			e.printStackTrace();

		}
		return np_set;
	}
	
//	public class IndexSearchCallable implements Callable<NodePotentialsSet> {
//		KeywordsGroundTruth keywordGroundTruth;
//		int numNodes;
//		int maxLength;
//		String filename;
//		int i;
//		LuceneIndexWrapper obj;
//
//		public IndexSearchCallable(KeywordsGroundTruth _keywordGroundTruth, int _numNodes,
//				int _maxLength, String _filename,int _i,LuceneIndexWrapper _obj) {
//			keywordGroundTruth = _keywordGroundTruth;
//			numNodes = _numNodes;
//			maxLength = _maxLength;
//			filename = _filename;
//			i = _i;
//			obj = _obj;
//		}
//
//		@Override
//		public NodePotentialsSet call() throws Exception {
//			// TODO Auto-generated method stub
//			return obj.extractNodesNewConsolidation_singleKW(keywordGroundTruth,numNodes, maxLength, filename,i);
//		}
//	}
//	
	public NodePotentialsSet extractNodesNewConsolidation_singleKW(
			KeywordsGroundTruth keywordGroundTruth, int numNodes,
			int maxLength, String filename,int i) {
		NodePotentialsSet np_set = new NodePotentialsSet();
		//System.out.println("calling with i : " + i);
		try {
			

			ArrayList<String> keywords = keywordGroundTruth.getMentionNames();
			long redirecttotaltime = 0,distotaltime = 0,strtotaltime = 0,
			titletotaltime = 0,setnptotal = 0, sorttotal = 0,totalaftime = 0,docsearchertime = 0;

				LabelSense senses = keywordGroundTruth.getKeywords().get(i).senses;
				
				String key = keywords.get(i);
				String ment = keywords.get(i);
				
				int offset = keywordGroundTruth.getKeywords().get(i).offset;
				
				HashMap<String, String> temp_map_redirect = new HashMap<String, String>();
				HashMap<String, String> temp_map_disamb = new HashMap<String, String>();
				HashMap<String, String> temp_map = new HashMap<String, String>();
				List<NodePotentials> temp_set = new ArrayList<NodePotentials>();
				HashSet<String> node_names = new HashSet<String>();
				
				key = key.replaceAll("[^0-9a-z\\sA-Z/\\-]", "").toLowerCase();

				String redirectquery = buildRedirectQuery(key,
						keywordGroundTruth, i, offset);
				String disambquery = buildDisambQuery(key, keywordGroundTruth,
						i, offset);
				String query = buildPhraseSearchQuery(key, keywordGroundTruth,
						i, offset, "");
				

				String name = ment;

				if (name.charAt(name.length() - 1) == '.'
						|| name.charAt(name.length() - 1) == ','
						|| name.charAt(name.length() - 1) == ';'
						|| name.charAt(name.length() - 1) == ':'
						|| name.charAt(name.length() - 1) == '?'
						|| name.charAt(name.length() - 1) == '!')

					name = name.substring(0, name.length() - 1);

				temp_map.put(name + "_" + offset, query);

				boolean foundRedirect = false;
				boolean foundDisamb = false;
			
				if (redirectquery != null) {
					
					long redstartTime = System.currentTimeMillis();
						
						searchStringInRedirectionIndex(redirectquery, numNodes);

						long redendTime = System.currentTimeMillis();

						redirecttotaltime += (redendTime - redstartTime);

						if (!(hitsforRedirects.scoreDocs.length == 0)) {

							int h = 0;
							//System.out.println("hitsforRedirects.scoreDocs.length : " + hitsforRedirects.scoreDocs.length);

							for (; h < numNodes
							&& h < hitsforRedirects.scoreDocs.length; h++) {

								long dsstartTime = System.currentTimeMillis();

								Document doc = searcherForRedirects.doc(hitsforRedirects.scoreDocs[h].doc);

								long dsendTime = System.currentTimeMillis();

								docsearchertime += (dsendTime - dsstartTime);

								String titleText = doc.get("redirect_text");
								if (!titleText
										.replaceAll("[^0-9a-z\\sA-Z/\\-]", "")
										.equalsIgnoreCase(key)) {
									continue;
								} else {
									foundRedirect = true;
									//System.out.println("redirect text : " + titleText);
									long setnpstartTime = System.currentTimeMillis();
									ArrayList<NodePotentials> np = setNodePotentialsforRedirects(
											doc, ment, offset, h, numNodes,
											temp_map_redirect, keywordGroundTruth,
											i);
									long setnpendTime = System.currentTimeMillis();

									setnptotal += (setnpendTime - setnpstartTime);

									if (np != null) {
										for (int n = 0; n < np.size(); n++)
											if (!np.get(n).name.equalsIgnoreCase("Wiktionary")
													&& !node_names.contains(np.get(n).name)) {
												temp_set.add(np.get(n));
												node_names.add(np.get(n).name);
											}
									}
								}
							}
						}
				}
				if (Config.useDisambPages && disambquery != null) {
					
					long disstartTime = System.currentTimeMillis();

						searchStringInDisambIndex(disambquery, numNodes);

						long disendTime = System.currentTimeMillis();

						distotaltime += (disendTime - disstartTime);

						if (!(hitsforDisamb.scoreDocs.length == 0)) {
							int h = 0;
							for (; h < numNodes
							&& h < hitsforDisamb.scoreDocs.length; h++) {
								long dsstartTime = System.currentTimeMillis();

								Document doc = searcherForDisamb.doc(hitsforDisamb.scoreDocs[h].doc);

								long dsendTime = System.currentTimeMillis();

								docsearchertime += (dsendTime - dsstartTime);

								String titleText = doc.get("page_title");
								if (!(titleText.equalsIgnoreCase(key)
										|| key.replaceAll("[,.;?!]", "")
										.equalsIgnoreCase(titleText) || key
										.replaceAll("[.,;?':!\\-\\*]", "")
										.equalsIgnoreCase(titleText))
										|| key.replaceAll("[^0-9a-z\\sA-Z/\\-]", "")
										.equalsIgnoreCase(titleText)) {
									continue;
								} else {
									foundDisamb = true;
									long setnpstartTime = System.currentTimeMillis();
									ArrayList<NodePotentials> np = setNodePotentialsforDisamb(
											doc, ment, offset, h, numNodes,
											temp_map_disamb, keywordGroundTruth, i);
									long setnpendTime = System.currentTimeMillis();

									setnptotal += (setnpendTime - setnpstartTime);

									if (np != null) {
										for (int n = 0; n < np.size(); n++)
											if (!np.get(n).name
													.equalsIgnoreCase("Wiktionary")
													&& !node_names.contains(np
															.get(n).name)) {
												temp_set.add(np.get(n));
												node_names.add(np.get(n).name);
											}
									}
								}
							}
						}
				} // Config.useDisambPages



				if (query != null) {
					long strstartTime = System.currentTimeMillis();
					
						searchStringInIndex(query, numNodes);
						long strendTime = System.currentTimeMillis();

						strtotaltime += (strendTime - strstartTime);

						//System.out.println("hits.scoreDocs.length : " + hits.scoreDocs.length); 

						if (!(hits.scoreDocs.length == 0)) {
							int redundancy = 0;
							int j = 0;
							boolean flag1 = false;
							boolean flag2 = false;
							for (; flag1 && flag2
							&& (j + redundancy) < hits.scoreDocs.length;) {
								if (j >= numNodes)
									flag1 = true;

								long dsstartTime = System.currentTimeMillis();

								Document doc = searcher.doc(hits.scoreDocs[j + redundancy].doc);

								long dsendTime = System.currentTimeMillis();

								docsearchertime += (dsendTime - dsstartTime);

								String pagetitle = doc.get("page_title");
								String disamb = doc.get("title_disamb");
								if ((pagetitle == null) || pagetitle.equals("")) {
									j++;
									continue;
								}
								if (!((disamb == null) || disamb.equals("")))
									pagetitle = pagetitle + " (" + disamb + ")";

								if (name.equals(pagetitle)) {
									flag2 = true;
								} else if (!flag1) {
									redundancy++;
									continue;
								}

								if (!compatibleNamePageTitle(name, pagetitle)) {
									redundancy++;
									continue;
								}

								long setnpstartTime = System.currentTimeMillis();
								NodePotentials np = setNodePotentials(doc, name,
										offset, j + redundancy, hits, false,
										keywordGroundTruth, i);
								long setnpendTime = System.currentTimeMillis();

								setnptotal += (setnpendTime - setnpstartTime);

								if (node_names.contains(pagetitle)) {
									redundancy++;
									continue;
								} else {
									if (!np.name.equalsIgnoreCase("Wikitionary")
											&& !node_names.contains(pagetitle)) {
										node_names.add(pagetitle);
										temp_set.add(np);
									} else {
										redundancy++;
										continue;
									}
								}
								j++;
							}
						}
				}
				if (senses != null) {
					for (int e = 0; e < senses.wikiMinerCandidate.length; e++) {
						String ent = senses.wikiMinerCandidate[e];
						//System.out.println("ent : " + ent);
						float senseprob = (float) senses.wikiMinerProbability[e];

						String wikiMinerquery = buildGroundEntQuery(ent);
						if (wikiMinerquery != null) {
							boolean flag = false;

							long titlestartTime = System.currentTimeMillis();
								searchTitleInIndex(wikiMinerquery);

								long titleendTime = System.currentTimeMillis();

								titletotaltime += (titleendTime - titlestartTime);

								//System.out.println("hitsforGroundEnt : " + hitsforGroundEnt.scoreDocs.length); 

								if (!(hitsforGroundEnt.scoreDocs.length == 0)) {
									for (int j = 0; j < hitsforGroundEnt.scoreDocs.length; j++) {
										if (!flag) {
											long dsstartTime = System.currentTimeMillis();

											Document docEnt = searcher
											.doc(hitsforGroundEnt.scoreDocs[j].doc);

											long dsendTime = System.currentTimeMillis();

											docsearchertime += (dsendTime - dsstartTime);

											String pagetitle = docEnt
											.get("page_title");
											String disamb = docEnt
											.get("title_disamb");
											if (!((disamb == null) || disamb
													.equals("")))
												pagetitle = pagetitle + " ("
												+ disamb + ")";
											//										System.out.println("pagetitle:" +  pagetitle);

											if (pagetitle.equals(ent)
													&& !node_names.contains(ent)) {


												long setnpstartTime = System.currentTimeMillis();
												NodePotentials np = setNodePotentials(
														docEnt, ment, offset, j,
														hitsforGroundEnt, false,
														keywordGroundTruth, i);
												long setnpendTime = System.currentTimeMillis();

												setnptotal += (setnpendTime - setnpstartTime);

												np.sense_probability = senseprob;
												node_names.add(ent);
												temp_set.add(np);
												flag = true;
												break;

											} else if (pagetitle.equals(ent) && node_names.contains(ent)) {
												for (int t = 0; t < temp_set.size(); t++) {
													if (temp_set.get(t).name.equalsIgnoreCase(pagetitle) || temp_set.get(t).name.equalsIgnoreCase(ent)) {
														temp_set.get(t).sense_probability = senseprob;
														break;
													}
												}
											}
										}
									}
								}
						}
					}
				}
				
				if (temp_map != null || temp_map.size() != 0)
					np_set.mention_queries.putAll(temp_map);
				if (foundRedirect)
					np_set.mention_queries.putAll(temp_map_redirect);
				if (Config.useDisambPages && foundDisamb)
					np_set.mention_queries.putAll(temp_map_disamb);
				
				long mafstartTime = System.currentTimeMillis();
				
				if (temp_set != null && temp_set.size() != 0) {
					if (Config.wikiSense) {
						// setWikiSenseFeature(temp_set, key, offset,
						// keywordGroundTruth, i);
						setMentionAnchorFeature(temp_set, key, offset,
								keywordGroundTruth, i);
						if (Config.logistic)
							setLogisticScore(temp_set, key, offset,
									keywordGroundTruth, i);
					}
				
				long mafendTime = System.currentTimeMillis();
				
				totalaftime += (mafendTime - mafstartTime);

					long sortstartTime = System.currentTimeMillis();
					Collections.sort(temp_set,
							new Comparator<NodePotentials>() {
								@Override
								public int compare(NodePotentials o1,
										NodePotentials o2) {
									return Double.compare(o1.logistic_score,
											o2.logistic_score);
								}
							});
					long sortendTime = System.currentTimeMillis();
					
					sorttotal += (sortendTime - sortstartTime);
					
					
					List<NodePotentials> final_temp = temp_set.subList(0, Math
							.min(temp_set.size(), (int) Config.maxCandidates));
					np_set.potentials_set.addAll(final_temp);
				}
		} catch (Exception e) {
			System.out.println("Error in extractNodes: " + e.getMessage());
			e.printStackTrace();

		}
		//System.out.println("mention_queries : " + np_set.mention_queries.size() + " potentials_set : " + np_set.potentials_set.size());
		return np_set;
	}

	public static void normalizePerMention(List<NodePotentials> temp_set) {
		double max[] = new double[Config.NODE_FEATURE_DIM];
		double min[] = new double[Config.NODE_FEATURE_DIM];
		Arrays.fill(max, 0.0d);
		Arrays.fill(min, Double.POSITIVE_INFINITY);
		for (NodePotentials n : temp_set) {
			double[] f = fillNodeFeaturesVector(n);
			for (int i = 0; i < Config.NODE_FEATURE_DIM; i++) {
				if (f[i] > max[i])
					max[i] = f[i];
				if (f[i] < min[i])
					min[i] = f[i];
			}
		}
		for (NodePotentials n : temp_set) {
			double[] f = fillNodeFeaturesVector(n);
			MathHelper.normalizeVector(f, min, max);
			setNodeFeaturesFromVector(n, f);
		}
	}

	public static double[] fillNodeFeaturesVector(NodePotentials np) {
		double[] f = new double[Config.NODE_FEATURE_DIM];
		f[5] = np.sense_probability;
		f[3] = np.page_title_score;
		f[2] = np.redirection;
		f[0] = (double) np.inlink_count;
		f[1] = (double) np.outlink_count;
		f[4] = np.anchor_text_score;
		f[6] = np.anchor_text_cosine;
		f[7] = np.anchor_text_context_cosine;
		f[8] = np.full_text_cosine;
		return f;
	}

	public static void setNodeFeaturesFromVector(NodePotentials np, double[] f) {
		np.sense_probability = f[5];
		np.page_title_score = f[3];
		np.redirection = f[2];
		np.inlink_count = f[0];
		np.outlink_count = f[1];
		np.anchor_text_score = f[4];
		np.anchor_text_cosine = f[6];
		np.anchor_text_context_cosine = f[7];
		np.full_text_cosine = f[8];
	}

	private boolean compatibleNamePageTitle(String name, String pagetitle) {
		boolean isAcronym = false;
		String capitals = "";
		if (name.matches("[A-Z]+")) {
			isAcronym = true;
			capitals = name;
		} else if (name.matches("([a-zA-Z]\\.)+")) {
			isAcronym = true;
			capitals = name.replaceAll("\\.", "");
		}

		if (isAcronym) {
			Matcher m = Pattern.compile("([A-Z])").matcher(pagetitle);
			String page_caps = "";
			while (m.find()) {
				page_caps += m.group(1);
			}
			if (page_caps.contains(capitals)
					&& page_caps.length() <= capitals.length()
							+ Config.pageTitleThreshold) {
				return true;
			} else {
				return false;
			}
		}

		String[] name_tokens = name.split(" ");
		String[] title_tokens = pagetitle.split(" ");

		if (name_tokens.length <= title_tokens.length + 1
				&& title_tokens.length <= name_tokens.length
						+ Config.pageTitleThreshold) {
			return true;
		}

		return false;
	}

	public void setLogisticScore(List<NodePotentials> npList, String mention,
			int offset, KeywordsGroundTruth keywordGroundTruth, int s)
			throws Exception {
		try {
			System.out.println("In setLogisticScore");
			
			//SUNNY:moved it- to load ondemand
			dataset = new Instances(loadeddataset);
			dataset.setClassIndex(dataset.numAttributes() - 1);
			cls = loadedcls;

			for (int i = 0; i < npList.size(); i++) {
				Instance xyz = new Instance(dataset.numAttributes()); // [1]
				xyz.setDataset(dataset);

				xyz.setValue(dataset.attribute(0),
						npList.get(i).sense_probability);
				xyz.setValue(dataset.attribute(1),
						npList.get(i).context_score_frequent);
				xyz.setValue(dataset.attribute(2),
						npList.get(i).context_score_synopsis);
				xyz.setValue(dataset.attribute(3),
						npList.get(i).context_score_vbdj);
				xyz.setValue(dataset.attribute(4),
						npList.get(i).page_title_score);
				xyz.setValue(dataset.attribute(5), npList.get(i).redirection);
				xyz.setValue(dataset.attribute(6), npList.get(i).inlink_count);
				xyz.setValue(dataset.attribute(7), npList.get(i).outlink_count);
				xyz.setValue(dataset.attribute(8),
						npList.get(i).anchor_text_score);

				double[] score_01 = cls.distributionForInstance(xyz);
				npList.get(i).logistic_label = cls.classifyInstance(xyz);
				npList.get(i).logistic_score_0_class = score_01[0];
				npList.get(i).logistic_score = score_01[1];

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setMentionAnchorFeature(List<NodePotentials> npList,
			String mention, int offset, KeywordsGroundTruth keywordGroundTruth,
			int s) throws Exception {
		if (!Config.Server && thesaurus == null) {
			System.out.println("Creating thesaurus obj");
			thesaurus = new Wikisaurus();
		}
		ClientWikisauras obj = new ClientWikisauras();
		ArrayList<KeywordsGroundTruth.Mention> keys = keywordGroundTruth.getKeywords();
		
		String contextWords = keys.get(s).contextAroundMention;

		for (int i = 0; i < npList.size(); i++) {

			String label = null;

			if (Config.Server) {
				label = obj.getAnchor(npList.get(i).name.replaceAll("_", " "));
			} else {
				//System.out.println("setting label.");
				label = thesaurus.getAnchor(npList.get(i).name.replaceAll("_",
						" "));
			}
			if (label != null)
				npList.get(i).anchor_text_score = (float) calcJaccardForAnchor(
						label, contextWords, npList.get(i));
		}

	}

	public void setWikiSenseFeature(ArrayList<NodePotentials> npList,
			String mention, int offset, KeywordsGroundTruth keywordGroundTruth,
			int s) throws Exception {
		String[] wikiMinerCandidate;
		double[] wikiMinerProbability;

		LabelSense senses = null;
		boolean[] senseFound;
		if (!Config.Server && thesaurus == null) {
			thesaurus = new Wikisaurus();
		}
		ClientWikisauras obj = new ClientWikisauras();
		try {
			// System.out.println(mention);
			if (Config.Server)
				senses = obj.getSenses(mention);
			else
				senses = new LabelSense(thesaurus.getSenses(mention));

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (senses != null) {
			// System.out.println( senses.length+" senses found for "+mention);
			senseFound = new boolean[senses.wikiMinerCandidate.length];
			wikiMinerCandidate = new String[senses.wikiMinerCandidate.length];
			wikiMinerProbability = new double[senses.wikiMinerCandidate.length];
			for (int i = 0; i < senses.wikiMinerCandidate.length; i++) {
				senseFound[i] = false;
				wikiMinerCandidate[i] = senses.wikiMinerCandidate[i];
				wikiMinerProbability[i] = senses.wikiMinerProbability[i];
			}
			for (int i = 0; i < npList.size(); i++) {
				NodePotentials np = npList.get(i);
				boolean found = false;
				for (int j = 0; j < wikiMinerCandidate.length; j++) {
					if (np.name.equalsIgnoreCase(wikiMinerCandidate[j])) {
						np.sense_probability = (float) wikiMinerProbability[j];
						senseFound[j] = true;
						found = true;
					}
				}
				if (!found) {
					np.sense_probability = 0f;
				}
			}

			for (int k = 0; k < wikiMinerCandidate.length; k++) {
				if (!senseFound[k]) {
					String query = buildPageTitleSearchQuery(wikiMinerCandidate[k]);
					int numNodes = 4;
					searchStringInIndex(query, numNodes); // query index for
					// phrase search
					// query
					boolean added = false;
					if (!(hits.scoreDocs.length == 0)) {
						int j = 0;
						for (; j < numNodes && j < hits.scoreDocs.length; j++) {
							Document doc = searcher.doc(hits.scoreDocs[j].doc); // get
							// the
							// next
							// document
							String pagetitle = doc.get("page_title"); // get its
							// title
							String id = doc.get("page_id"); // get its id
							String disamb = doc.get("title_disamb");
							if ((pagetitle == null) || pagetitle.equals("")) // use
								// the
								// id
								// if
								// it
								// has
								// no
								// title
								continue;

							if (!((disamb == null) || disamb.equals(""))) // check
								// for
								// disambiguation
								// tag
								pagetitle = pagetitle + " (" + disamb + ")";
							if (pagetitle
									.equalsIgnoreCase(wikiMinerCandidate[k])) {
								NodePotentials np = setNodePotentials(doc,
										mention, offset, j, hits, false,
										keywordGroundTruth, s);
								np.sense_probability = (float) wikiMinerProbability[k];
								added = true;
								// System.out.println("Entity added from wikipedia miner set "+
								// pagetitle);
								break;
							}
						}
					}
				}
			}
		} else {
			// System.out.println("No senses found for  "+mention);
			for (int i = 0; i < npList.size(); i++) {
				npList.get(i).sense_probability = 0f;
			}
		}

	}

	public ArrayList<NodePotentials> setNodePotentialsforDisamb(Document doc,
			String name, int offset, int i, int numNodes,
			HashMap<String, String> temp_map,
			KeywordsGroundTruth keywordsGroundTruth, int firstIndex) {
		ArrayList<NodePotentials> np = new ArrayList<NodePotentials>();

		try {
			String ids = doc.get("id_list"); // get its id
			// System.out.println("ids "+ids);
			String disamb_text = doc.get("page_title");
			String[] idArr = ids.split(" ");
			for (String id : idArr) {
				String queryString = buildpageidQuery(id);
				// System.out.println("addidng features for id "+id);
				temp_map.put(name + "_" + offset, queryString);
				searchidInIndex(queryString, numNodes);
				if (hitsforId.scoreDocs.length == 0) {
					continue;
				}
				int j = 0;
				for (; j < numNodes && j < hitsforId.scoreDocs.length;) {
					Document doc1 = searcher.doc(hitsforId.scoreDocs[j].doc); // get
					// the
					// next
					// document
					String pagetitle = doc1.get("page_title"); // get its title
					String pageid = doc1.get("page_id"); // get its id
					String disamb = doc1.get("title_disamb");
					if ((pagetitle == null) || pagetitle.equals("")) // use the
					// id if
					// it
					// has
					// no
					// title
					{
						j++;
						continue;
					}

					if (!((disamb == null) || disamb.equals(""))) // check for
						// disambiguation
						// tag
						pagetitle = pagetitle + " (" + disamb + ")";

					NodePotentials node = setNodePotentials(doc1, name, offset,
							j, hitsforId, false, keywordsGroundTruth,
							firstIndex);

					if (!np.contains(node)) {
						np.add(node);
					}
					j++;
				}
			}
		} catch (Exception e) {
			System.out
					.println("Error in setNodePotentialsforDisambiguation : ");
			e.printStackTrace();
		}
		return np;
	}

	public ArrayList<NodePotentials> setNodePotentialsforRedirects(
			Document doc, String name, int offset, int i, int numNodes,
			HashMap<String, String> temp_map,
			KeywordsGroundTruth keywordsGroundTruth, int firstIndex) {

		ArrayList<NodePotentials> np = new ArrayList<NodePotentials>();
		try {
			String id = doc.get("page_id"); // get its id
			String redirect_text = doc.get("redirect_text");
			String queryString = buildpageidQuery(id);
			temp_map.put(name + "_" + offset, queryString);
			searchidInIndex(queryString, numNodes);
			//System.out.println("id : " + id);
			//System.out.println("hitsforId.scoreDocs.length : " + hitsforId.scoreDocs.length);
			if (hitsforId.scoreDocs.length == 0) {
				return null;
			}
			int j = 0;
			for (; j < numNodes && j < hitsforId.scoreDocs.length;) {
				
				Document doc1 = searcher.doc(hitsforId.scoreDocs[j].doc);
				String pagetitle = doc1.get("page_title"); // get its title
				String pageid = doc1.get("page_id"); // get its id
				String disamb = doc1.get("title_disamb");
				//System.out.println("pagetitle : " + pagetitle + " disamb : " + disamb + " pageid : " + pageid);
				if ((pagetitle == null) || pagetitle.equals("")) {
					j++;
					continue;
				}
				if (!((disamb == null) || disamb.equals("")))
					pagetitle = pagetitle + " (" + disamb + ")";

				NodePotentials node = setNodePotentials(doc1, name, offset, j,
						hitsforId, false, keywordsGroundTruth, firstIndex);
				node.redirection = 1;
				np.add(node);
				j++;
			}
		} catch (Exception e) {
			System.out.println("Error in setNodePotentialsforRedirects : ");
			e.printStackTrace();
		}
		return np;
	}

	public NodePotentials setNodePotentials(Document doc, String name,
			int offset, int index, TopDocs hits, boolean ground,
			KeywordsGroundTruth keywordsGroundTruth, int firstIndex)
			throws IOException {
		ArrayList<KeywordsGroundTruth.Mention> keys = keywordsGroundTruth
				.getKeywords();

		String contextWords = keys.get(firstIndex).context;

		NodePotentials np = new NodePotentials();
		String pagetitle = doc.get("page_title"); // get its title
		String id = doc.get("page_id"); // get its id
		String disamb = doc.get("title_disamb");
		if ((pagetitle == null) || pagetitle.equals("")) // use the id if it has
			// no title
			pagetitle = id;
		// String synopsis = doc.get("synopsis");
		// String synopsis_vbadj = doc.get("synopsis_vbadj");
		// String frequent = doc.get("frequent");
		if (!((disamb == null) || disamb.equals(""))) // check for
			// disambiguation tag
			pagetitle = pagetitle + " (" + disamb + ")";
		
		System.out.println("Setting np for pagetitle : " + pagetitle + " disamb : " + disamb + " pageid : " + id);
		np.name = pagetitle;
		np.id = Integer.parseInt(id);
		np.mention = name + "_" + offset;// allOffset.get(0);
		//byte[] kwBytes = name.getBytes(Charset.forName("UTF-8"));
		np.interval = Interval.valueOf(offset, offset + name.length() - 1);
		//np.interval = Interval.valueOf(offset, offset + kwBytes.length - 1);
		String inlink_count = doc.get("inlink_count");
		if (inlink_count == null || "".equals(inlink_count)) {
			np.inlink_count = 0d;
		} else {
			np.inlink_count = (double) Integer.parseInt(inlink_count);
		}
		String outlink_count = doc.get("outlink_count");
		if (outlink_count == null || "".equals(outlink_count)) {
			np.outlink_count = 0d;
		} else {
			np.outlink_count = (double) Integer.parseInt(outlink_count);
		}

		// np.context_score = hits.scoreDocs[index].score;
		np.page_title_score = (float) calcJaccard(pagetitle, name,
				"page_title", np);

		np.label = 0; // for now keep it as 0, afterwards check ground truth and
		// set this accordingly
		if (ground)
			np.label = 1;
		String outlinks = null;
		DisambProperties props = DisambProperties.getInstance();
		if (!props.isNodeOnly()) {
			outlinks = doc.get("association_inlink");
			if (outlink_count != null && !"".equals(outlink_count)
					&& !"".equals(outlinks) && outlinks != null) {
				np.outLinks = parseOutlinks(outlinks);
			}
			np.inLinks = parseInLinks(np.id);
		}

		/*
		 * Adding functions for new features anchor text cosine and full text
		 * cosine
		 */

		if (contextWords == null) {
			np.contextTermsForInput2Tf = null;
		} else {
			StringTokenizer st = new StringTokenizer(contextWords.toLowerCase());
			int k = 0;
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token == null)
					continue;
				if (np.contextTermsForInput2Tf.containsKey(token)) {
					np.contextTermsForInput2Tf.put(token,
							np.contextTermsForInput2Tf.get(token) + 1);
				} else {
					np.contextTermsForInput2Tf.put(token, 1d);
				}
			}
		}

		setAnchorTextDetails(pagetitle, np);

		// if (np.anchorTextTerms == null)
		// np.anchor_text_cosine = 0d;
		// else {
		// np.anchor_text_cosine = calcCosine(np.anchorTextTerms,
		// np.anchorTextTermsTf, np.anchorTextTermsIdf,
		// np.contextTermsForInput2TfIdfAnchorText);
		// }
		// System.out.println(np.name + "  " + np.mention + "  "
		// + np.anchor_text_cosine);
		if (!props.isNodeOnly()) {
			String wordsString = doc.get("synopsis");
			String synopsis = new String();
			if (wordsString != null) {
				ArrayList<String> wordsList = parseSynWords(wordsString);
				for (int i = 0; i < wordsList.size(); i++) {
					Matcher matcher = digit_pattern.matcher(wordsList.get(i));
					if (!(wordsList.get(i).equalsIgnoreCase("na")
							|| matcher.find() || wordsList.get(i).contains(
							"\\\\"))) {
						np.bagOfWords_synopsis.add(wordsList.get(i).replaceAll(
								"[^0-9a-z\\sA-Z/\\-]", ""));
						Term newTerm = new Term("synopsis", wordsList.get(i)
								.replaceAll("[^0-9a-z\\sA-Z/\\-]", ""));
						double idf = Math.log10((double) (reader.numDocs() + 1)
								/ (double) (1 + reader.docFreq(newTerm)));
						np.idf_synopsis.add(idf);
						synopsis += wordsList.get(i).replaceAll(
								"[^0-9a-z\\sA-Z/\\-]", "")
								+ " ";
					}
				}
				// if (synopsis != null && contextWords != null) {
				// np.context_score_synopsis = (float) calcJaccard(synopsis,
				// contextWords, "synopsis", np);
				// }

			}

			// String wordsStringAdj = doc.get("synopsis_vbadj");
			// if(wordsString!=null){
			// ArrayList<String> wordsListAdj =
			// parseSynAdjWords(wordsStringAdj);
			// for(int i=0;i<wordsListAdj.size();i++){
			// if(wordsListAdj.get(i)!=" "||wordsListAdj.get(i)!=null){
			// //System.out.println(wordsListAdj.get(i).trim());
			// np.bagOfWords_synopsis_vbadj.add(wordsListAdj.get(i).trim());
			// }
			// }
			// }

			String wordsStringAdj = doc.get("synopsis_vbadj");
			String syn_vadj = new String();
			if (wordsStringAdj != null && wordsStringAdj != "") {
				String[] wordsListAdj = wordsStringAdj.split("\\s+");
				// System.out.println(Arrays.toString(wordsListAdj));
				for (int i = 0; i < wordsListAdj.length; i++) {
					if (wordsListAdj[i] != null) {
						np.bagOfWords_synopsis_vbadj.add(wordsListAdj[i].trim()
								.replaceAll("[^0-9a-z\\sA-Z/\\-]", ""));
						Term newTerm = new Term("synopsis_vbadj",
								wordsListAdj[i].replaceAll(
										"[^0-9a-z\\sA-Z/\\-]", ""));
						double idf = Math.log10((double) (reader.numDocs() + 1)
								/ (double) (1 + reader.docFreq(newTerm)));
						np.idf_synopsis_vbadj.add(idf);
						syn_vadj += wordsListAdj[i].trim().replaceAll(
								"[^0-9a-z\\sA-Z/\\-]", "")
								+ " ";
					}
				}

				// if (syn_vadj != null && contextWords != null) {
				// np.context_score_vbdj = (float) calcJaccard(syn_vadj,
				// contextWords, "synopsis_vbadj", np);
				// }
			}

			String wordsStringFreq = doc.get("frequent");
			String freq = new String();
			if (wordsStringFreq != null) {
				wordsStringFreq = wordsStringFreq
						.replaceAll("[^a-z\\sA-Z]", "");
				StringTokenizer st = new StringTokenizer(wordsStringFreq);
				while (st.hasMoreTokens()) {
					String temp = st.nextToken();
					np.bagOfWords_frequent.add(temp);
					Term newTerm = new Term("synopsis", temp);
					double idf = Math.log10((double) (reader.numDocs() + 1)
							/ (double) (1 + reader.docFreq(newTerm)));
					np.idf_frequent.add(idf);
					freq += temp.trim().replaceAll("[\\s]", "") + " ";
				}

				// if (freq != null && contextWords != null) {
				// np.context_score_frequent = (float) calcJaccard(freq,
				// contextWords, "frequent", np);
				// }
			}
		}

		return np;
	}

	private void setAnchorTextDetails(String pagetitle, NodePotentials np) {
		try {

			for (String st : np.contextTermsForInput2Tf.keySet()) {
				double tf = np.contextTermsForInput2Tf.get(st);
				double docf = readerForAnchor.docFreq(new Term("anchor", st));
				double n = readerForAnchor.numDocs();
				double idf = Math.log10((n + 1) / (docf + 1));
				np.contextTermsForInput2TfIdfAnchorText.put(st, tf * idf);
				double docif = readerForAnchor.docFreq(new Term("text", st));
				double idffull = Math.log10((n + 1) / (docif + 1));
				np.contextTermsForInput2TfIdfFullText.put(st, tf * idffull);
				double docff = readerForAnchor.docFreq(new Term(
						"anchor_context", st));
				double idfcontext = Math.log10((n + 1) / (docff + 1));
				np.contextTermsForInput2TfIdfAnchorTextContext.put(st, tf
						* idfcontext);
			}

			String queryString = buildPageTitleSearchAnchorQuery(pagetitle);
			String anchors = null;
			searchTitleInAnchorIndex(queryString, 1);
			if (hitsforanchor.scoreDocs.length == 0) {
				np.anchorTextTerms = null; // in case of no hit from lucene
			}
			int j = 0;
			for (; j < 1 && j < hitsforanchor.scoreDocs.length; j++) {

				Document doc = searcherForAnchor
						.doc(hitsforanchor.scoreDocs[j].doc);
				anchors = doc.get("anchor");
				int docid = hitsforanchor.scoreDocs[j].doc;
				boolean fa = false, fc = false, ff = false;
				TermFreqVector tfv = readerForAnchor.getTermFreqVector(docid,
						"anchor");
				if (tfv == null) {
					np.anchorTextTerms = null;
					np.anchorTextTermsTf = null;
					np.anchorTextTermsIdf = null;
					fa = true;
				}
				TermFreqVector tfv1 = readerForAnchor.getTermFreqVector(docid,
						"anchor_context");
				if (tfv1 == null) {
					np.anchorTextContextTerms = null;
					np.anchorTextContextTermsTf = null;
					np.anchorTextContextTermsIdf = null;
					fc = true;
				}
				TermFreqVector tfv2 = readerForAnchor.getTermFreqVector(docid,
						"text");
				if (tfv2 == null) {
					np.fullTextTerms = null;
					np.fullTextTermsTf = null;
					np.fullTextTermsIdf = null;
					ff = true;
				}

				if (fa == false) {
					np.anchorTextTerms = tfv.getTerms();
					int termCount = np.anchorTextTerms.length;
					np.anchorTextTermsTf = tfv.getTermFrequencies();
					np.anchorTextTermsIdf = new double[termCount];
					for (int t = 0; t < termCount; t++) {
						double idf = Math.log10((double) (readerForAnchor
								.numDocs() + 1)
								/ (double) (readerForAnchor.docFreq(new Term(
										"anchor", np.anchorTextTerms[t])) + 1));
						np.anchorTextTermsIdf[t] = idf;
					}
				}
				if (fc == false) {
					np.anchorTextContextTerms = tfv1.getTerms();
					int termCount = np.anchorTextContextTerms.length;
					np.anchorTextContextTermsTf = tfv1.getTermFrequencies();
					np.anchorTextContextTermsIdf = new double[termCount];
					for (int t = 0; t < termCount; t++) {
						double idf = Math.log10((double) (readerForAnchor
								.numDocs() + 1)
								/ (double) (readerForAnchor.docFreq(new Term(
										"anchor_context",
										np.anchorTextContextTerms[t])) + 1));
						np.anchorTextContextTermsIdf[t] = idf;
					}
				}

				if (ff == false) {

					np.fullTextTerms = tfv2.getTerms();
					int termCount = np.fullTextTerms.length;
					np.fullTextTermsTf = tfv2.getTermFrequencies();
					np.fullTextTermsIdf = new double[termCount];
					for (int t = 0; t < termCount; t++) {
						double idf = Math.log10((double) (readerForAnchor
								.numDocs() + 1)
								/ (double) (readerForAnchor.docFreq(new Term(
										"text", np.fullTextTerms[t])) + 1));
						np.fullTextTermsIdf[t] = idf;
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// used for searching a mention in synonym field and retrieving entities
	// corresponding to that
	public NodePotentialsSet getfromSynonym(String mention, int offset,
			int numNodes) {
		NodePotentialsSet np_set = new NodePotentialsSet();
		try {
			String title = QueryParser.escape(mention);
			if (title == null || "".equals(title) || " ".equals(title)) {
				// System.out.println("Error: empty title");
				return null;
			}

			String queryString = "+synonym:(" + title.toLowerCase() + ")^4";

			searchStringInIndex(queryString, numNodes);

			np_set.mention_queries.put(mention + "_" + offset, queryString);
			int i = 0;
			int redundancy = 0;
			HashSet<String> node_names = new HashSet<String>();
			if (hits.scoreDocs.length == 0)
				return null;
			for (; i < numNodes && (i + redundancy) < hits.scoreDocs.length;) {
				Document doc = searcher.doc(hits.scoreDocs[i + redundancy].doc); // get
				// the
				// next
				// document
				String pagetitle = doc.get("page_title"); // get its title
				String id = doc.get("page_id"); // get its id
				String disamb = doc.get("title_disamb");
				String redirec = doc.get("synonym");

				String[] parsedRedirec = redirec.split("\\|");

				if ((pagetitle == null) || pagetitle.equals("")) // use the id
				// if it has
				// no title
				{
					i++;
					continue;
				}

				if (!((disamb == null) || disamb.equals(""))) // check for
					// disambiguation
					// tag
					pagetitle = pagetitle + " (" + disamb + ")";

				if (node_names.contains(pagetitle)) {
					redundancy++;
					continue;
				} else {
					node_names.add(pagetitle);
				}
				for (String str : parsedRedirec) {
					if (i + redundancy >= numNodes
							|| i + redundancy >= hits.scoreDocs.length)
						break;
					NodePotentials np = new NodePotentials();
					np.name = pagetitle;
					np.id = Integer.parseInt(id);
					np.mention = str.trim() + "_" + offset;
					// System.out.println("from redirection  "+
					// np.mention.split("_")[0]);
					String inlink_count = doc.get("inlink_count");
					if (inlink_count == null || "".equals(inlink_count)) {
						np.inlink_count = 0;
					} else {
						np.inlink_count = Integer.parseInt(inlink_count);
					}
					String outlink_count = doc.get("outlink_count");
					if (outlink_count == null || "".equals(outlink_count)) {
						np.outlink_count = 0;
					} else {
						np.outlink_count = Integer.parseInt(outlink_count);
					}
					// np.context_score = hits.scoreDocs[i].score;
					np.label = 0; // for now keep it as 0, afterwards check
					// ground truth and set this accordingly

					String outlinks = doc.get("association_inlink");
					if (outlink_count != null && !"".equals(outlink_count)
							&& !"".equals(outlinks) && outlinks != null) {
						np.outLinks = parseOutlinks(outlinks);
					}
					np_set.potentials_set.add(np);
					i++;

				}

			}

		} catch (Exception e) {
			System.out.println("Error in getfromSynonym: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		return np_set;
	}

	// for InlinkIndex
	public HashMap<String, String> getInlinkLists(ArrayList<String> id_list)
			throws Exception {
		HashMap<String, String> result = new HashMap<String, String>();
		String id_string = "";
		for (String id : id_list) {
			id_string += id_list + " ";
		}
		String queryString = "+page_id(" + id_string + ")";
		searchStringInIndex(queryString, id_list.size());
		if (!(hits.scoreDocs.length == 0)) {
			for (int j = 0; j < hits.scoreDocs.length; j++) {
				Document doc = searcher.doc(hits.scoreDocs[j].doc);
				String page_id = doc.get("page_id");
				String inlinks = doc.get("inlinks");
				if ((page_id == null) || page_id.equals(""))
					continue;

				result.put(page_id, inlinks);
			}
		}
		return result;
	}

	public double calcJaccardForAnchor(String w1, String w2, NodePotentials np) {
		w1 = w1.toLowerCase();
		w2 = w2.toLowerCase();
		double common = 0d;
		double diff = 0d;

		HashSet<String> out1 = new HashSet<String>();
		HashSet<String> out2 = new HashSet<String>();
		DecimalFormat df = new DecimalFormat("#.####");
		StringTokenizer str = new StringTokenizer(w1.replaceAll(
				"[^0-9a-z\\sA-Z]", " "));
		PorterStemmer stemmer = new PorterStemmer();
		while (str.hasMoreTokens()) {
			String token = str.nextToken().toLowerCase();
			if (token == null || "".equals(token) || " ".equals(token))
				continue;
			stemmer.setCurrent(token);
			stemmer.stem();
			out1.add(stemmer.getCurrent().toLowerCase()
					.replaceAll("[^0-9a-z\\sA-Z]", ""));
		}
		StringTokenizer str2 = new StringTokenizer(w2.replaceAll(
				"[^0-9a-z\\sA-Z]", " "));
		PorterStemmer stemmer1 = new PorterStemmer();
		while (str2.hasMoreTokens()) {
			String token = str2.nextToken();
			if (token == null || "".equals(token) || " ".equals(token))
				continue;
			stemmer1.setCurrent(token);
			stemmer1.stem();
			out2.add(token.toLowerCase());
			out2.add(stemmer1.getCurrent().toLowerCase());
		}

		for (String id1 : out1) {
			if (out2.contains(id1)) {
				common++;
			} else {
				diff++;
			}
		}

		double num = 2 * common;
		double deno = out1.size() + out2.size();

		np.anchor_text_intersection = (int) common;
		np.anchor_text_union = out1.size() + (int) diff;
		np.anchor_text_numerator = num;
		np.anchor_text_denominator = deno;
		if ((int) deno == 0) {
			return 0;
		} else {
			double val = Double.valueOf(df.format(num / deno));
			return (val);
		}

	}
	
	public double calcJaccard(String w1, String w2, String field,
			NodePotentials np) throws IOException {

		int common = 0;
		int diff = 0;

		ArrayList<String> out1 = new ArrayList<String>();
		ArrayList<String> out2 = new ArrayList<String>();
		DecimalFormat df = new DecimalFormat("#.####");
		StringTokenizer str = new StringTokenizer(w1);
		while (str.hasMoreTokens()) {
			String token = str.nextToken().toLowerCase();
			if (token == null || "".equals(token) || " ".equals(token))
				continue;
			out1.add(token.replaceAll("[^0-9a-z\\sA-Z/\\-]", ""));
		}
		StringTokenizer str2 = new StringTokenizer(w2);
		while (str2.hasMoreTokens()) {
			String token = str2.nextToken().toLowerCase();
			if (token == null || "".equals(token) || " ".equals(token))
				continue;
			out2.add(token.replaceAll("[^0-9a-z\\sA-Z/\\-]", ""));
		}

		double numSquare = 0f;
		double denoSquare = 0f;

		for (String id1 : out1) {
			if (out2.contains(id1)) {
				common++;
				Term newTerm = new Term(field, id1);
				double idf = Math.log10((double) (reader.numDocs() + 1)
						/ (double) (1 + reader.docFreq(newTerm)));
				numSquare += Math.pow(idf, 2);
			} else {
				Term newTerm = new Term(field, id1);
				double idf = Math.log10(reader.numDocs() / 1
						+ reader.docFreq(newTerm));
				denoSquare += Math.pow(idf, 2);
				diff++;
			}
		}

		for (String id2 : out2) {
			Term newTerm = new Term(field, id2);
			double idf = Math.log10((double) (reader.numDocs() + 1)
					/ (double) (1 + reader.docFreq(newTerm)));
			denoSquare += Math.pow(idf, 2);
		}

		// float num = 2*common;
		// float deno = diff+common+out2.size();
		// double num = common;
		// double deno = diff+out2.size();
		double num = Math.pow(numSquare, 0.5);
		double deno = Math.pow(denoSquare, 0.5);

		if (field.equalsIgnoreCase("synopsis")) {
			np.context_score_synopsis_intersection = common;
			np.context_score_synopsis_union = out1.size() + diff;
			np.context_score_synopsis_numerator = num;
			np.context_score_synopsis_denominator = deno;
		} else if (field.equalsIgnoreCase("synopsis_vbadj")) {
			np.context_score_synopsis_vbdj_intersection = common;
			np.context_score_synopsis_vbdj_union = out1.size() + diff;
			np.context_score_synopsis_vbdj_numerator = num;
			np.context_score_synopsis_vbdj_denominator = deno;
		} else if (field.equalsIgnoreCase("frequent")) {
			np.context_score_frequent_intersection = common;
			np.context_score_frequent_union = out1.size() + diff;
			np.context_score_frequent_numerator = num;
			np.context_score_frequent_denominator = deno;
		} else if (field.equalsIgnoreCase("page_title")) {
			np.page_title_score_intersection = common;
			np.page_title_score_union = out1.size() + diff;
			np.page_title_score_numerator = num;
			np.page_title_score_denominator = deno;
		}
		if ((int) deno == 0) {
			return 0;
		} else {
			return (Double.valueOf(df.format(num / deno)));
		}
	}
}