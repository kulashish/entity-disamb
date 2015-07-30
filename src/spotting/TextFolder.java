package spotting;

import java.io.File;
import java.text.Normalizer;
import java.util.regex.Pattern;

import org.wikipedia.miner.db.WEnvironment;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.util.text.*;

public class TextFolder extends TextProcessor{

	private CaseFolder caseFolder = new CaseFolder() ;
	private PorterStemmer stemmer = new PorterStemmer() ;
	private Cleaner cleaner = new Cleaner() ;
	private Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

	@Override
	public String processText(String text) {
		String normalizedText = Normalizer.normalize(text, Normalizer.Form.NFD); 
		normalizedText = pattern.matcher(normalizedText).replaceAll("") ;
		normalizedText = cleaner.processText(normalizedText) ;
		normalizedText = caseFolder.processText(normalizedText) ;
		normalizedText = stemmer.processText(normalizedText) ;

		return normalizedText;
	}

	public static void main(String args[]) throws Exception {

		TextFolder folder = new TextFolder() ;

		WikipediaConfiguration conf = new WikipediaConfiguration(new File(args[0])) ;
		WEnvironment.prepareTextProcessor(folder, conf, new File("tmp"), true, 1) ;
	}
}