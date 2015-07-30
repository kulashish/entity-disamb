package wikiGroundTruth;

import java.io.Serializable;
import java.util.ArrayList;

public class Document implements Serializable {
	public String docText;
	public String docTitle;
	public ArrayList<Integer> offset = new ArrayList<Integer>();
	public ArrayList<String> mention = new ArrayList<String>();
	public ArrayList<String> entname = new ArrayList<String>();
	public ArrayList<Integer> entId = new ArrayList<Integer>();
	public ArrayList<String> anchorContext = new ArrayList<String>();
	public ArrayList<WikiMention> entity = new ArrayList<WikiMention>();

}
