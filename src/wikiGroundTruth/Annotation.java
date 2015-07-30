package wikiGroundTruth;

import it.unimi.dsi.util.Interval;

import java.util.ArrayList;

import spotting.NodePotentials;

public class Annotation implements Comparable<Annotation> {
	public Interval interval;
	public String mention;
	public boolean isPresentInWiki= false;
	public String wikiEntity=null; //if isPresentWiki = true else null;
	public ArrayList<NodePotentials> candidateEntities = new ArrayList<NodePotentials>();
	
	@Override
	public int compareTo(Annotation o) {
		if (o.interval.left > interval.right) return -1;
		if (o.interval.right < interval.left) return 1;
		return interval.left - o.interval.left;
	}
	
	public  boolean contains(Interval bigger, Interval smaller) {
		return bigger.left <= smaller.left && bigger.right >= smaller.right;
	}	
	
	public boolean disjoint(Interval span1, Interval span2) {
		return span1.right < span2.left || span2.right < span1.left; 
	}
	
	public boolean overlaps(Interval span1, Interval span2) {
		return !disjoint(span1, span2);
	}
	
}
