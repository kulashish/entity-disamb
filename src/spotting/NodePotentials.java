package spotting;

import it.unimi.dsi.util.Interval;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class NodePotentials implements Comparable<NodePotentials>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3044259914192183827L;
	public String name = ""; // Wikipedia name of the node
	public String mention = "";
	public int id; // ID of the wikipedia entity stored in Lucene
	public double context_score_synopsis;
	public double context_score_frequent;
	public double context_score_vbdj;
	public double page_title_score;
	public double anchor_text_score;
	public double redirection;
	public double sense_probability;
	public double inlink_count;
	public double outlink_count;
	public double logistic_label;
	public double logistic_score;
	public double logistic_score_0_class;
	public int context_score_synopsis_intersection;
	public int context_score_synopsis_union;
	public double context_score_synopsis_numerator;
	public double context_score_synopsis_denominator;
	public int context_score_synopsis_vbdj_intersection;
	public int context_score_synopsis_vbdj_union;
	public double context_score_synopsis_vbdj_numerator;
	public double context_score_synopsis_vbdj_denominator;
	public int context_score_frequent_intersection;
	public int context_score_frequent_union;
	public double context_score_frequent_numerator;
	public double context_score_frequent_denominator;
	public int page_title_score_intersection;
	public int page_title_score_union;
	public double page_title_score_numerator;
	public double page_title_score_denominator;
	public int anchor_text_intersection;
	public int anchor_text_union;
	public double anchor_text_numerator;
	public double anchor_text_denominator;

	public double full_text_cosine;
	public double anchor_text_cosine;
	public double anchor_text_context_cosine;

	public int label; // label for the entity 1 or 0 depending upon the ground
	// truth
	public int offset;
	public int length;
	public Interval interval;

	public HashMap<String, Double> contextTermsForInput2Tf;

	public String[] anchorTextTerms;
	public double[] anchorTextTermsIdf;
	public int[] anchorTextTermsTf;
	public HashMap<String, Double> contextTermsForInput2TfIdfAnchorText;

	public String[] anchorTextContextTerms;
	public double[] anchorTextContextTermsIdf;
	public int[] anchorTextContextTermsTf;
	public HashMap<String, Double> contextTermsForInput2TfIdfAnchorTextContext;

	public String[] fullTextTerms;
	public double[] fullTextTermsIdf;
	public int[] fullTextTermsTf;
	public HashMap<String, Double> contextTermsForInput2TfIdfFullText;

	public ArrayList<Integer> inLinks;
	public ArrayList<Integer> outLinks;
	public ArrayList<String> bagOfWords_synopsis;
	public ArrayList<Double> idf_synopsis;
	public ArrayList<String> bagOfWords_synopsis_vbadj;
	public ArrayList<Double> idf_synopsis_vbadj;
	public ArrayList<String> bagOfWords_frequent;
	public ArrayList<Double> idf_frequent;
	public HashMap<Integer, String> category;
	public boolean isPresentInWiki = false;
	public boolean blnNA = false;

	public NodePotentials() {
		contextTermsForInput2Tf = new HashMap<String, Double>();
		contextTermsForInput2TfIdfAnchorText = new HashMap<String, Double>();
		contextTermsForInput2TfIdfAnchorTextContext = new HashMap<String, Double>();
		contextTermsForInput2TfIdfFullText = new HashMap<String, Double>();
		category = new HashMap<Integer, String>();
		inLinks = new ArrayList<Integer>();
		bagOfWords_frequent = new ArrayList<String>();
		bagOfWords_synopsis = new ArrayList<String>();
		bagOfWords_synopsis_vbadj = new ArrayList<String>();
		idf_synopsis = new ArrayList<Double>();
		idf_synopsis_vbadj = new ArrayList<Double>();
		idf_frequent = new ArrayList<Double>();
		outLinks = new ArrayList<Integer>();
		label = 0;
		id = -1;
		redirection = 0;
		anchor_text_score = 0f;
		sense_probability = 0f;
		context_score_frequent = 0f;
		context_score_synopsis = 0f;
		context_score_vbdj = 0f;
		logistic_score = 0d;
		logistic_score_0_class = 0d;
		logistic_label = 0d;
	}

	@Override
	public NodePotentials clone() {

		NodePotentials npClone = new NodePotentials();
		npClone.anchor_text_score = this.anchor_text_score;
		npClone.bagOfWords_frequent = this.bagOfWords_frequent;
		npClone.bagOfWords_synopsis = this.bagOfWords_synopsis;
		npClone.bagOfWords_synopsis_vbadj = this.bagOfWords_synopsis_vbadj;
		npClone.category = this.category;
		npClone.context_score_frequent = this.context_score_frequent;
		npClone.context_score_synopsis = this.context_score_synopsis;
		npClone.context_score_vbdj = this.context_score_vbdj;
		npClone.id = this.id;
		npClone.idf_frequent = this.idf_frequent;
		npClone.idf_synopsis = this.idf_synopsis;
		npClone.idf_synopsis_vbadj = this.idf_synopsis_vbadj;
		npClone.inlink_count = this.inlink_count;
		npClone.inLinks = this.inLinks;
		npClone.interval = this.interval;
		npClone.isPresentInWiki = this.isPresentInWiki;
		npClone.label = this.label;
		npClone.length = this.length;
		npClone.logistic_score = this.logistic_score;
		npClone.logistic_score_0_class = this.logistic_score_0_class;
		npClone.mention = this.mention;
		npClone.name = this.name;
		npClone.offset = this.offset;
		npClone.outlink_count = this.outlink_count;
		npClone.outLinks = this.outLinks;
		npClone.page_title_score = this.page_title_score;
		npClone.redirection = this.redirection;
		npClone.sense_probability = this.sense_probability;
		return npClone;
	}

	@Override
	public String toString() {
		return interval + name;
	}

	@Override
	public int compareTo(NodePotentials o) {
		if (o.interval.left > interval.right)
			return -1;
		if (o.interval.right < interval.left)
			return 1;
		return interval.left - o.interval.left;
	}
	/*
	public class NPNameComparator implements Comparator<NodePotentials> {
		 
	    @Override
	    public int compare(NodePotentials np1, NodePotentials np2) {
	        return np1.name.compareTo(np2.name);
	    }
	}*/
	/*
    static int getNameComparator() {
			@Override
			public int compare(NodePotentials np1, NodePotentials np2) {
				// TODO Auto-generated method stub
				return np1.name.compareTo(np2.name);
			}
    }*/
	/*
    public static Comparator<NodePotentials> NameComparator = new Comparator<NodePotentials>() {
    	 
        @Override
        public int compare(NodePotentials np1, NodePotentials np2) {
            return (int) np1.name.compareTo(np2.name);
        }
    };*/

	public boolean contains(Interval bigger, Interval smaller) {
		return bigger.left <= smaller.left && bigger.right >= smaller.right;
	}

	public boolean disjoint(Interval span1, Interval span2) {
		return (span1.right < span2.left) || (span2.right < span1.left);
	}

	public boolean overlaps(Interval span1, Interval span2) {
		return !disjoint(span1, span2);
	}

	public static int distance(NodePotentials n1, NodePotentials n2) {
		int o1 = n1.offset;
		int o2 = n2.offset;
		return o1 > o2 ? o1 - o2 : o2 - o1;
	}

}
