package spotting;

import java.io.File;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.model.Article.Label;
import org.wikipedia.miner.util.WikipediaConfiguration;

  public class WikipediaDefiner {

	public static void main(String args[]) throws Exception {
		
	    WikipediaConfiguration conf = new WikipediaConfiguration(new File("/home/kanika/workspace/wikipedia-miner-1.2.0/wikipedia-config.xml")) ;
			
	    Wikipedia wikipedia = new Wikipedia(conf, false) ;
	    
	    Article article = wikipedia.getArticleByTitle("Sachin Tendulkar") ;
	    
	    System.out.println(article.getSentenceMarkup(0)) ;
	    Label[] label = article.getLabels();
	    System.out.println("labels ");
	    for(int i=0;i<label.length;i++){
	    	System.out.println(label[i].getText());
	    }
	    
	    
	    System.out.println(article.getId());
	    Article article1 = wikipedia.getArticleByTitle("India") ;
	    
	    System.out.println(article1.getSentenceMarkup(0)) ;
	    Label[] label1 = article1.getLabels();
	    System.out.println("labels ");
	    for(int i=0;i<label1.length;i++){
	    	System.out.println(label1[i].getText());
	    }
	    System.out.println(article1.getId());
	    wikipedia.close() ;
	}
  }