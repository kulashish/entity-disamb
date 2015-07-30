package util;

import java.util.ArrayList;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.impl.file.ParseException;
public class TestJawsVerb {

    public static String getNounForm(String word)
    {
    	ArrayList<String> str = new ArrayList<String>();
    	System.setProperty("wordnet.database.dir", "/home/pararth/Projects/rndproj/svn/WikiTraining/WordNet-2.0/dict/");
        try
        {
            //  Concatenate the command-line arguments
            
        	//  Get the synsets containing the word form
            WordNetDatabase database = WordNetDatabase.getFileInstance();
            
            Synset[] synsets = database.getSynsets(word,SynsetType.VERB);
            //  Display the word forms and definitions for synsets retrieved
            for (int j = 0; j < synsets.length; j++)
            {

                System.out.print(synsets[j].getWordForms()[0]);
            }
            if (synsets.length > 0)
            {
                for (int i = 0; i < synsets.length; i++)
                {
                //    System.out.println("");
                    if(synsets[i].getWordForms()[0].equalsIgnoreCase(word)){
                    //	System.out.print(synsets[i].getWordForms()[0]);
                    	VerbSynset s = (VerbSynset) synsets[i];
                        NounSynset[] nounsenses=s.getUsages();
                        for (int j = 0; j < nounsenses.length; j++)
                        {

                            System.out.print((j > 0 ? ", " : ":") +
                                    nounsenses[j].getWordForms()[0]);
                        	
                            str.add(nounsenses[j].getWordForms()[0]);

                        }
                       
                    }
                    else continue;
                                        
                }
              
            }
            else
            {
            	
              System.err.println("No synsets exist that contain " +
                        "the word form '" +word+ "'");
                
                return null;
            }
        }
        catch(ParseException e){
        	return null;
        }
        if(str.size()>=1){
        	System.out.print(str.get(0)+" ");
        	 return str.get(0);
        }
        	
        else
        	return null;
    }
    public static void main(String args[]){
    	String nounForm = TestJAWS.getNounForm("portability");
    	
    }

}
