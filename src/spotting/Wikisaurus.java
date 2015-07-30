package spotting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import org.wikipedia.miner.db.WEnvironment;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.model.Page;
import org.wikipedia.miner.model.Page.PageType;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.PageIterator;
import org.wikipedia.miner.util.WikipediaConfiguration;

import util.DisambProperties;

public class Wikisaurus {

	static WikipediaConfiguration conf;
	public static Wikipedia _wikipedia = null;
	//public static TextFolder _textfolder = null;
	static DisambProperties props = DisambProperties.getInstance();
	static String wikiMinerConfigFile;

	public Wikisaurus() {

		this(props.getwikiMinerConfigFile());
	}

	public Wikisaurus(String wikiminerConfig) {
		try {
			wikiMinerConfigFile = wikiminerConfig;
			conf = new WikipediaConfiguration(new File(wikiminerConfig));
//			conf.setLabelComparisonModel(new File(
//						"/home/kanika/workspace/wikipedia-miner-1.2.0/models/compare/labelCompare_en_In.model"));

//			if (_textfolder == null) {
//				System.out.println("setting textfolder..");
//				_textfolder = new TextFolder() ;
//				
//				WEnvironment.prepareTextProcessor(_textfolder, conf, new File("tmp"), false, 1) ;
//			}
//
//			conf.setDefaultTextProcessor(_textfolder) ;
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if (_wikipedia == null) {
			_wikipedia = new Wikipedia(conf, false);
		}
	}

	public Label.Sense[] run(String term) throws Exception {
		Label.Sense[] senses = getSenses(term);
		/*
		 * if (senses == null) { System.out.println("I have no idea what '" +
		 * term + "' is") ; } else { if (senses.length == 0) {
		 * System.out.println("m here in legth 0"); displaySense(senses[0]) ; }
		 * else { System.out.println("'" + term +
		 * "' could mean several things:") ; for (int i=0 ; i<senses.length ;
		 * i++) { System.out.println(" - [" + (i+1) + "] " +
		 * senses[i].getTitle()+"  "+senses[i].getPriorProbability()) ; } } }
		 */
		return senses;
	}

	public Label.Sense[] getSenses(String term) throws Exception {
		Label label = _wikipedia.getLabel(term);
		if (!label.exists()) {
			return null;
		} else {
			return label.getSenses();
		}
	}

	public static Label.Sense[] getAllSenses(String term) throws Exception {
		Label label = _wikipedia.getLabel(term);
		if (!label.exists()) {
			return null;
		} else {
			return label.getSenses();
		}
	}

	public String getAnchor(String page_title) {
		Article article = _wikipedia.getArticleByTitle(page_title);
		if (article == null) {
			// System.out.println("no article for "+page_title);
			return null;
		}
		Article.Label[] label = article.getLabels();
		String labelString = "";
		// System.out.println("labels ");
		if (label == null) {
			return null;
		}
		for (int i = 0; i < label.length; i++) {
			// System.out.println(label[i].getText());
			labelString += label[i].getText() + " ";
		}

		return labelString;
	}

	public static String getAnchorText(String page_title) {

		Article article = _wikipedia.getArticleByTitle(page_title);
		if (article == null) {
			// System.out.println("no article for "+page_title);
			return null;
		}
		Article.Label[] label = article.getLabels();
		String labelString = "";
		// System.out.println("labels ");
		if (label == null) {
			return null;
		}
		for (int i = 0; i < label.length; i++) {
			// System.out.println(label[i].getText());
			labelString += label[i].getText() + " ";
		}
		return labelString;
	}

	public static Article[] getInLinks(String page_title) {
		Article article = _wikipedia.getArticleByTitle(page_title);
		if (article == null) {
			return null;
		}
		return (article.getLinksIn());
	}

	protected void displaySense(Label.Sense sense) throws Exception {
		System.out.println("==" + sense.getTitle() + "==");
	}

	public static void closeWikipedia() {
		if (null != _wikipedia)
			_wikipedia.close();
	}

	public static void main(String args[]) throws Exception {

		Wikisaurus thesaurus = new Wikisaurus(
				"/home/kanika/workspace/wikipedia-miner-1.2.0/wikipedia-config.xml");
		String mention;
		String title = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		PageType pt = PageType.valueOf("disambiguation");
		PageIterator itr = _wikipedia.getPageIterator(pt);
		while (itr.hasNext()) {
			Page p = itr.next();
			title += p.getTitle();
			title += "\n";

		}
		File file = new File("/mnt/bag1/kanika/disambPagesList.txt");
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(title);
		bw.close();

		System.out.println("Done");
		System.out.println(title);
		System.out.println("Done");
		// while (true) {
		// try {
		// mention = br.readLine();
		// if (mention.equals("q"))
		// break;
		// Label.Sense[] sens = thesaurus.run(mention);
		// for (int i = 0; i < sens.length; i++) {
		// System.out.println(sens[i].getTitle() + "  "
		// + sens[i].getPriorProbability());
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// break;
		// }
		// }

		/*
		 * keeps on taking title as input till user enters "q"
		 */
		// while (true) {
		// try {
		//
		// // provide the title for the entity for which you want anchor
		// // text and sentence list
		// title = br.readLine();
		// System.out.println("Entity Title entered :: " + title);
		// if (title.equals("q"))
		// break;
		// Article target = _wikipedia.getArticleByTitle(title);
		//
		// // String array label will have all the anchor text for the
		// // particular entity title
		// // String[] label = getAnchorText(title);
		// //
		// // // printing the anchor texts
		// // if (label == null)
		// // System.out.println("anchor text for " + title
		// // + " returned null");
		// // else {
		// // for (int i = 0; i < label.length; i++)
		// // System.out.println("anchor text :: " + label[i]);
		// // }
		// //
		// // storing all inlinks for a particular entity
		// Article[] inlinks = getInLinks(title);
		// if (inlinks == null)
		// System.out.println("for pagetitle " + title
		// + " null inlinks returned");
		// else {
		// // iterating over all inlink pages and storing the sentences
		// // for the target entity reference
		// for (int i = 0; i < inlinks.length; i++) {
		// Integer[] sentenceIndex = inlinks[i]
		// .getSentenceIndexesMentioning(target);
		// for (int j = 0; j < sentenceIndex.length; j++) {
		// String sentence = inlinks[i]
		// .getSentenceMarkup(sentenceIndex[j]);
		// System.out.println(" sentence ::: " + sentence);
		// }
		// }
		// }
		// } catch (Exception e) {
		//
		// }
		// }
		thesaurus._wikipedia.close();
	}
}
