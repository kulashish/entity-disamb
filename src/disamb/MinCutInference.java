package disamb;

import in.ac.iitb.cse.mrf.data.AMNWeights;
import in.ac.iitb.cse.mrf.data.AugmentedGraph;
import in.ac.iitb.cse.mrf.data.MRFGraph;
import in.ac.iitb.cse.mrf.data.WikiEdge;
import in.ac.iitb.cse.mrf.data.WikiNode;
import in.ac.iitb.cse.mrf.infer.GraphMincutInference;
import in.ac.iitb.cse.mrf.util.LearningProperties;

import java.util.HashMap;
import java.util.HashSet;

import org.jgrapht.WeightedGraph;

import spotting.ComputeCategorySimilarity;
import spotting.Config;
import spotting.FeatureExtractor;
import spotting.NodePotentials;
import spotting.TrainingData;
import util.GraphCreator;

public class MinCutInference {
	LearningProperties props = null;

	private static double[] w0 = { 10.997973011507273, 10.609446007271977,
			10.434273087006043, 9.238648681775834, 8.851912882403644,
			6.745006156169512, 8.718288158897881, 11.60723181091319,
			5.627939359453897 };
	private static double[] w1 = { 8.503722356728426, 8.892249360963723,
			9.067422281229662, 10.263046686459868, 10.649782485832068,
			12.756689212066192, 10.78340720933782, 7.89446355732252,
			13.873756008781807 };
	private static double[] w00 = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
	private static double[] w11 = { 0.1300379015353103, 0.25488853986043647,
			0.2809926604040576, 0.09141654123467748, 0.0833069295331551,
			0.06518107323263571 };

	WeightedGraph<WikiNode, WikiEdge> orig_graph = null;
	// WeightedGraph<WikiNode, WikiEdge> mrf_graph = null;
	MRFGraph mrfObj = null;
	AMNWeights weights = null;

	private double[] parseDoubleArray(String inp) {
		String[] arr = inp.split(",");
		double[] result = new double[arr.length];
		for (int i = 0; i < result.length; i++) {
			try {
				result[i] = Double.parseDouble(arr[i]);
			} catch (Exception e) {
				e.printStackTrace();
				result[i] = 1.0;
			}
		}
		return result;
	}

	public void loadEntityData(TrainingData data) {

		orig_graph = GraphCreator.getInstance().createGraph(data);
		HashSet<String> clampedEntities = new HashSet<String>();
		for (NodePotentials np: data.nodes.potentials_set){
		    if (np.label == 1){
		        clampedEntities.add(np.name);
		    }
		}
		for (WikiNode node: orig_graph.vertexSet()){
		    if (clampedEntities.contains(node.getLabel())){
		        node.setPredcut(true);
		    }
		}
		mrfObj = new MRFGraph(orig_graph);
		// MRFFeatures mrf_features = new MRFFeatures(orig_graph,
		// NODE_FEATURE_DIM, EDGE_FEATURE_DIM);
		// mrf_features.normalize();
		if (!Config.normalizePerMention) {
			mrfObj.normalizeFeatures(Config.NODE_FEATURE_DIM, Config.EDGE_FEATURE_DIM);
		}
		mrfObj.makeSparse();
		// mrf_graph = mrf_features.getMrfGraph();
	}

	private double[] createEdge(NodePotentials np1, NodePotentials np2) {
		double[] f = new double[Config.EDGE_FEATURE_DIM];
		double inlinkScore = FeatureExtractor.calcInlinkSim(np1.inLinks,
				np2.inLinks).similarity;
		// Filter based on inlink similarity score
		if (Config.filterInlink && inlinkScore < Config.filterInlinkThreshold)
			f = null;
		else {
			f[0] = ComputeCategorySimilarity.computeCategSim(np1.name
					.replaceAll(" ", "_"), np2.name.replaceAll(" ", "_"));
			f[1] = FeatureExtractor.calcOutlinkSim(np1.outLinks, np2.outLinks).similarity;
			f[2] = inlinkScore;
			f[3] = FeatureExtractor
					.calcContextSim(np1.bagOfWords_frequent,
							np2.bagOfWords_frequent, np1.idf_frequent,
							np2.idf_frequent).similarity;
			f[4] = FeatureExtractor
					.calcContextSim(np1.bagOfWords_synopsis,
							np2.bagOfWords_synopsis, np1.idf_synopsis,
							np2.idf_synopsis).similarity;
			f[5] = FeatureExtractor.calcContextSim(
					np1.bagOfWords_synopsis_vbadj,
					np2.bagOfWords_synopsis_vbadj, np1.idf_synopsis_vbadj,
					np2.idf_synopsis_vbadj).similarity;
		}
		return f;
	}

	private WikiNode createNode(NodePotentials np) {
		WikiNode node = null;
		// check if there is already a node with the same label
		for (WikiNode n : orig_graph.vertexSet())
			if (n.getLabel().equalsIgnoreCase(np.name)) {
				node = n;
				break;
			}
		// if not then create a new node
		if (null == node) {
			node = new WikiNode(np.name);
			double[] f = null;
			node.setIncut(np.label == 1);
			f = new double[Config.NODE_FEATURE_DIM];
			f[0] = np.context_score_frequent;
			f[1] = np.context_score_synopsis;
			f[2] = np.context_score_vbdj;
			f[3] = Math.log(1.0 + (double) np.inlink_count);
			f[4] = Math.log(1.0 + (double) np.outlink_count);
			f[5] = np.redirection;
			f[6] = np.page_title_score;
			f[7] = np.anchor_text_score;
			f[8] = np.sense_probability;
			node.setfVector(f);
		}
		return node;
	}

	public HashMap<String, Double> runInference() {
		// mrfObj = new MRFGraph(mrf_graph);
		try {
			if (props == null)
				props = new LearningProperties(
						"/home/pararth/Projects/rndproj/EntityDisamb/wikimrf/learning.properties");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (Config.useManualAMNWeights) {
			w0 = parseDoubleArray(Config.amn_w0);
			w1 = parseDoubleArray(Config.amn_w1);
			w00 = parseDoubleArray(Config.amn_w00);
			w11 = parseDoubleArray(Config.amn_w11);
		}

		weights = new AMNWeights(w0, w1, w00, w11);
		AugmentedGraph augGraph = new AugmentedGraph(mrfObj, weights);
		GraphMincutInference inference = new GraphMincutInference(augGraph);
		inference.computeMincut();
		HashMap<String, Double> result = new HashMap<String, Double>();
		for (WikiNode n : augGraph.getNodes()) {
			// System.out.println("Predicted class for " + n.getLabel() + " : "
			// + n.isPredcut());
			if (n.isPredcut()) {
				result.put(n.getLabel(), 1.0);
			} else {
				result.put(n.getLabel(), 0.0);
			}
		}
		return result;

	}

}
