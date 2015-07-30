package util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class SerializeGroundTruthXmls {

	public static HashSet<String> pageRemoved = new HashSet<String>();

	public boolean searchTitleInIndex(String phrase) {

		String title = QueryParser.escape(phrase);
		if (title == null || "".equals(title) || " ".equals(title)) {
			System.exit(1);
		}
		String queryString = "+page_title:(" + title.toLowerCase() + ")";

		Version LuceneVersion = Version.LUCENE_31;
		String indexName = "/mnt/bag1/querysystem/indexing/CompleteIndex";
		IndexReader reader;
		IndexSearcher searcher;
		Analyzer analyzer;
		QueryParser qp;
		Query queryGroundEnt;
		TopDocs hitsforGroundEnt = null;
		boolean flag = false;
		try {
			reader = IndexReader.open(FSDirectory.open(new File(indexName)));
			searcher = new IndexSearcher(reader);
			analyzer = new SimpleAnalyzer(LuceneVersion);
			qp = new QueryParser(LuceneVersion, "NE_type", analyzer);
			queryGroundEnt = qp.parse(queryString);
			hitsforGroundEnt = searcher.search(queryGroundEnt, 1000);
			if (hitsforGroundEnt != null
					&& !(hitsforGroundEnt.scoreDocs.length == 0)) {
				for (int j = 0; j < hitsforGroundEnt.scoreDocs.length; j++) {
					if (!flag) {
						Document docEnt = searcher
								.doc(hitsforGroundEnt.scoreDocs[j].doc);
						String pagetitle = docEnt.get("page_title");
						String disamb = docEnt.get("title_disamb");
						if (!((disamb == null) || disamb.equals(""))) // check
							pagetitle = pagetitle + " (" + disamb + ")";
						if (pagetitle.equalsIgnoreCase(phrase)) {
							flag = true;
						}
					}
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		if (hitsforGroundEnt == null || flag == false) {
			return false;
		} else
			return true;

	}

	public static void main(String[] args) {
		// String docfolder =
		// "/home/ashish/wikilearn/wiki_experiment/wiki_docs_updated/";
		String xmlFolder = "/home/kanika/bat-env-0.1/benchmark/datasets/AQUAINT/Problems/";
		String objectFolder = "/mnt/bag1/kanika/AQUAINTGroundFilesEntitiesRemoved/";
		File folder = new File(xmlFolder);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {

				String docName = listOfFiles[i].getName();
				File f = new File(
						"/mnt/bag1/kanika/AQUAINTGroundFilesEntitiesRemoved/"
								+ docName + ".xml.object");
				if (f.exists()) {
					System.out.println("success");
				} else {
					SerializeGroundTruthXmls obj = new SerializeGroundTruthXmls();
					HashMap<String, ArrayList<XMLTagInfo>> fileGroundTruthMapKdd = new HashMap<String, ArrayList<XMLTagInfo>>();
					// ParseXML pm1 = new ParseXML();
					ParseXMLAQ pm1 = new ParseXMLAQ();
					try {
						// fileGroundTruthMapKdd = pm1.parseXML(xmlFolder
						// + docName + ".xml");

						fileGroundTruthMapKdd = pm1.parseXMLAQ(xmlFolder
								+ docName);

						for (String filename : fileGroundTruthMapKdd.keySet()) {
							Iterator<XMLTagInfo> it = fileGroundTruthMapKdd
									.get(filename).iterator();
							while (it.hasNext()) {
								XMLTagInfo obj1 = it.next();
								if (obj1.wikiEntity != null
										&& obj1.wikiEntity != "NA")
									obj1.wikiEntity = obj1.wikiEntity
											.replaceAll("_", " ");
								if (obj1.wikiEntity != null
										&& obj1.wikiEntity != "NA") {
									Boolean entityExists = obj
											.searchTitleInIndex(obj1.wikiEntity);
									if (entityExists == false) {
										System.out.println("removed..  "
												+ obj1.wikiEntity);
										pageRemoved.add(obj1.wikiEntity);
										it.remove();
									}
								}
							}

						}
						Serializer.encode(fileGroundTruthMapKdd, objectFolder
								+ docName + ".xml.object");
						System.out.println("Kdd ground truth serialized.. ");
						@SuppressWarnings("unchecked")
						HashMap<String, ArrayList<XMLTagInfo>> temp = (HashMap<String, ArrayList<XMLTagInfo>>) Serializer
								.decode("/mnt/bag1/kanika/AQUAINTGroundFilesEntitiesRemoved/"
										+ docName + ".xml.object");
						for (String filename : temp.keySet()) {
							System.out.println("docName : " + filename
									+ "  total " + temp.get(filename).size());
							// for (int i1 = 0; i1 < temp.get(filename).size();
							// i1++) {
							// if (pageRemoved
							// .contains(temp.get(filename).get(i1).wikiEntity))
							// System.out
							// .println(temp.get(filename).get(i1).wikiEntity
							// + "  not removed from serialized object");
							// }

						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		// for kdd complete

		// Serializer.encode(fileGroundTruthMapKdd,
		// "/mnt/bag1/kanika/KddGroundTruth.object");

		// HashMap<String, ArrayList<XMLTagInfo>> temp = (HashMap<String,
		// ArrayList<XMLTagInfo>>) Serializer
		// .decode("/mnt/bag1/kanika/KddGroundTruth.object");

	}
}
