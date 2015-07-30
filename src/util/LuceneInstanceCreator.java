package util;

import spotting.LuceneIndexWrapper;

public class LuceneInstanceCreator {
	
	static LuceneIndexWrapper luceneIndex = null;
	
	public static void init(){
		
		if(luceneIndex == null){
			DisambProperties props = DisambProperties.getInstance();
			luceneIndex = new LuceneIndexWrapper(
					props.getCompleteIndex(), props.getRedirectIndex(),
					props.getInlinkIndex(), props.getDisambIndex(),
					props.getAnchorIndex());
			System.out.println("LuceneIndexWrapper Initialized.");
		}
	}
	
	public static LuceneIndexWrapper getInstance(){
		
		System.out.println("In LuceneInstanceCreator getInstance.");
		if(luceneIndex == null){
			DisambProperties props = DisambProperties.getInstance();
			luceneIndex = new LuceneIndexWrapper(
					props.getCompleteIndex(), props.getRedirectIndex(),
					props.getInlinkIndex(), props.getDisambIndex(),
					props.getAnchorIndex());
			System.out.println("New LuceneIndexWrapper Created.");
		}
		
		return luceneIndex;
	}

}
