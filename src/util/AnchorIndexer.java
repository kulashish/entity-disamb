package util;

import iitb.CSAW.Corpus.Wikipedia.WikiInRarCorpus;
import iitb.CSAW.Corpus.Wikipedia.WikiInRarDocument;
import iitb.CSAW.Utils.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.wikipedia.miner.db.WIterator;
import org.wikipedia.miner.db.struct.DbPage;

import spotting.Wikisaurus;

import com.sleepycat.je.DatabaseException;

import exception.IndexingException;

public class AnchorIndexer {
	private static Logger logger = Logger.getLogger("util.anchorindexer");
	private static final String KEY_FIELD = "page_title";
	private static final String WIKIMINER_CONFIG = "/home/ashish/wikilearn/wikipedia-config.xml";
	private static final String CSAW_PROPS = "/home/kanika/workspace2/CSAW-clean/properties/jointAnnotation.properties";
	private static final String CSAW_LOG = "log";

	private IndexWriter writer;
	private String indexDir;

	private WikiInRarCorpus wc;
	private Config config;

	public AnchorIndexer(String dir) throws IndexingException, IOException,
			ClassNotFoundException, ConfigurationException {
		indexDir = dir;
		// new Wikisaurus(WIKIMINER_CONFIG);
		config = new Config(CSAW_PROPS, CSAW_LOG);
		wc = new WikiInRarCorpus(config);
		// new Wikisaurus();
		try {
			writer = new IndexWriter(FSDirectory.open(new File(indexDir)),
					new IndexWriterConfig(Version.LUCENE_36,
							new WhitespaceAnalyzer(Version.LUCENE_36)));
		} catch (CorruptIndexException e) {
			throw new IndexingException(e);
		} catch (LockObtainFailedException e) {
			throw new IndexingException(e);
		} catch (IOException e) {
			throw new IndexingException(e);
		}
	}

	public void commit() throws IndexingException {
		logger.log(Level.INFO, "Committing index");
		try {
			writer.forceMerge(1);
			writer.commit();
			writer.close();
//			Wikisaurus.closeWikipedia();
		} catch (CorruptIndexException e) {
			throw new IndexingException(e);
		} catch (IOException e) {
			throw new IndexingException(e);
		}
	}

	public void update() throws IndexingException {
		logger.log(Level.INFO, "Index update started");
		try {
			IndexReader reader = IndexReader.open(writer, true);
			int numDocs = reader.numDocs();
			Document thisDoc = null;
			String title = null;
			WikiInRarDocument wrd = (WikiInRarDocument) wc
					.allocateReusableDocument();
			for (int docIndex = 0; docIndex < numDocs; docIndex++) {
				thisDoc = reader.document(docIndex);
				title = thisDoc.get(KEY_FIELD);
				if (null == title)
					continue;
				title = title.replaceAll(" ", "_");
				wc.getDocument(title, wrd);
				ArrayList<String> a = wc.getAnnotations(title, 10, false);
				if (null != a && !a.isEmpty())
					updateIndex(thisDoc, wrd.toText(config),
							(String[]) a.toArray(new String[a.size()]));
				if (docIndex % 100000 == 0)
					logger.log(Level.INFO, "Indexed " + docIndex + " documents");
			}
			logger.log(Level.INFO, "Indexed " + writer.numDocs() + " documents");
		} catch (CorruptIndexException e) {
			throw new IndexingException(e);
		} catch (IOException e) {
			throw new IndexingException(e);
		} catch (DatabaseException e) {
			throw new IndexingException(e);
		} catch (ClassNotFoundException e) {
			throw new IndexingException(e);
		}
	}

	public void index() throws IndexingException {
		logger.log(Level.INFO, "Indexing started");
		// PageIterator iterator = Wikisaurus._wikipedia.getPageIterator();
		WIterator<Integer, DbPage> iterator = Wikisaurus._wikipedia
				.getEnvironment().getDbPage().getIterator();
		DbPage page = null;
		String[] anchors = null;
		String[] anchorContexts = null;
		int count = 0;
		while (iterator.hasNext()) {
			page = iterator.next().getValue();
			anchors[count] = Wikisaurus.getAnchorText(page.getTitle());
			// anchorContexts = Wikisaurus.getAnchorContexts(page.getTitle());
			writeIndex(page, anchors, anchorContexts);
			// if (++count >= 100)
			// break;
			if (++count % 100000 == 0)
				logger.log(Level.INFO, "Indexed " + count + " documents");
		}
		logger.log(Level.INFO, "Indexed " + count + " documents");
	}

	private void updateIndex(Document doc, String[] text, String[] anchorContext)
			throws IndexingException {
		doc.removeField("text");
		doc.removeField("anchor_context");
		doc.add(new Field("text", StringUtil.asString(text), Field.Store.YES,
				Field.Index.ANALYZED, TermVector.YES));
		doc.add(new Field("anchor_context", StringUtil.asString(anchorContext),
				Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		try {
			writer.updateDocument(new Term(KEY_FIELD, doc.get(KEY_FIELD)), doc);
		} catch (CorruptIndexException e) {
			throw new IndexingException(e);
		} catch (IOException e) {
			throw new IndexingException(e);
		}
	}

	private void writeIndex(DbPage page, String[] anchors,
			String[] anchorContexts) throws IndexingException {
		// logger.log(Level.INFO, "Indexing page");
		Document doc = new Document();
		doc.add(new Field("page_title", page.getTitle(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		if (null != anchors)
			doc.add(new Field("anchor", StringUtil.asString(anchors, '|'),
					Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		if (null != anchorContexts)
			doc.add(new Field("context", StringUtil.asString(anchorContexts),
					Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		try {
			writer.addDocument(doc);
		} catch (CorruptIndexException e) {
			throw new IndexingException(e);
		} catch (IOException e) {
			throw new IndexingException(e);
		}
		// logger.log(Level.INFO, "Page indexed");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			logger.log(Level.INFO, "Creating anchor index at " + args[0]);
			AnchorIndexer indexer = new AnchorIndexer(args[0]);
			// indexer.index();
			indexer.update();
			indexer.commit();
			logger.log(Level.INFO, "Indexing complete!!");
		} catch (IndexingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

	}

}
