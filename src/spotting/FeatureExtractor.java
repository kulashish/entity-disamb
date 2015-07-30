package spotting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.DisambProperties;

public class FeatureExtractor {
	LuceneIndexWrapper luceneIndex = null;

	public FeatureExtractor() {
		DisambProperties props = DisambProperties.getInstance();
		luceneIndex = new LuceneIndexWrapper(props.getCompleteIndex(),
				props.getRedirectIndex(), props.getInlinkIndex(),
				props.getDisambIndex(), props.getAnchorIndex());

	}

	// for collective training purpose
	public TrainingData extractFeaturesForTraining(KeywordsGroundTruth kws,
			int numNodes, int consolidationSize) {

		TrainingData result = new TrainingData();
		result.groundtruth = kws;
		result.nodes = luceneIndex.extractNodesForTraining(kws, numNodes,
				consolidationSize, kws.filename);
		// System.out.println("Total entities retrieved for file "+kws.filename+
		// " are " +c);
		if (result.nodes == null) {
			System.out
					.println("FeatureExtractor.extractFeaturesForTraining()::null");
		}
		int node_set_size = result.nodes.potentials_set.size();
		System.out.println("result size " + node_set_size);
		return result;
	}

	public TrainingData extractFeatures(KeywordsGroundTruth kws, int numNodes,
			int consolidationSize) {
		// System.out.println("in extract features");
		TrainingData result = new TrainingData();

		result.groundtruth = kws;
		result.nodes = luceneIndex.extractNodesNewConsolidation(kws, numNodes,
				consolidationSize, kws.filename);

		HashSet<String> groundtruth = new HashSet<String>();
		groundtruth.addAll(kws.getGroundTruth());
		int c = 0;
		for (NodePotentials np : result.nodes.potentials_set) {
			c++;
			// if node is present in groundtruth then set its label = 1
			if (groundtruth.contains(np.name)) {
				np.label = 1;
			}
		}
		System.out.println("Total entities retrieved for file " + kws.filename
				+ " are " + c);
		int node_set_size = result.nodes.potentials_set.size();

		return result;
		// DEBUG

		/*
		 * if (Config.useCategSim) { result.category_sim = new
		 * EdgePotentialsMatrix(node_set_size);
		 * result.category_sim.potential_type =
		 * EdgePotentialsMatrix.CATEGORY_SIM;
		 * 
		 * for (int i = 0; i < node_set_size; i++) {
		 * result.category_sim.matrix.set(i, i, 1.0); for (int j = i+1; j <
		 * node_set_size; j++) { double categ_sim =
		 * ComputeCategorySimilarity.computeCategSim(
		 * result.nodes.potentials_set.get(i).name,
		 * result.nodes.potentials_set.get(j).name);
		 * //System.out.println(result.nodes.potentials_set.get(i).name + " " +
		 * // result.nodes.potentials_set.get(j).name + ": " + categ_sim);
		 * result.category_sim.matrix.set(i, j, categ_sim);
		 * result.category_sim.matrix.set(j, i, categ_sim); } } }
		 * 
		 * if (Config.useOutlinkSim) { result.outlink_sim = new
		 * EdgePotentialsMatrix(node_set_size);
		 * result.outlink_sim.potential_type = EdgePotentialsMatrix.OUTLINK_SIM;
		 * 
		 * for (int i = 0; i < node_set_size; i++) {
		 * result.outlink_sim.matrix.set(i, i, 1.0); for (int j = i+1; j <
		 * node_set_size; j++) { double outlink_sim = calcOutlinkSim(
		 * result.nodes.potentials_set.get(i).outLinks,
		 * result.nodes.potentials_set.get(j).outLinks);
		 * //System.out.println(result.nodes.potentials_set.get(i).name + " " +
		 * // result.nodes.potentials_set.get(j).name + ": " + outlink_sim);
		 * result.outlink_sim.matrix.set(i, j, outlink_sim);
		 * result.outlink_sim.matrix.set(j, i, outlink_sim); } } }
		 * 
		 * if (Config.useInlinkSim) { result.inlink_sim = new
		 * EdgePotentialsMatrix(node_set_size); result.inlink_sim.potential_type
		 * = EdgePotentialsMatrix.INLINK_SIM;
		 * 
		 * for (int i = 0; i < node_set_size; i++) {
		 * result.inlink_sim.matrix.set(i, i, 1.0); for (int j = i+1; j <
		 * node_set_size; j++) { double inlink_sim = calcInlinkSim(
		 * result.nodes.potentials_set.get(i).inLinks,
		 * result.nodes.potentials_set.get(j).inLinks);
		 * //System.out.println(result.nodes.potentials_set.get(i).name + " " +
		 * // result.nodes.potentials_set.get(j).name + ": " + outlink_sim);
		 * result.inlink_sim.matrix.set(i, j, inlink_sim);
		 * result.inlink_sim.matrix.set(j, i, inlink_sim); } } }
		 * 
		 * if (Config.useContextSim) { result.context_sim = new
		 * EdgePotentialsMatrix(node_set_size);
		 * result.context_sim.potential_type = EdgePotentialsMatrix.CONTEXT_SIM;
		 * 
		 * for (int i = 0; i < node_set_size; i++) {
		 * result.context_sim.matrix.set(i, i, 1.0); for (int j = i+1; j <
		 * node_set_size; j++) { double context_sim = calcContextSim(
		 * result.nodes.potentials_set.get(i).bagOfWords_synopsis,
		 * result.nodes.potentials_set
		 * .get(j).bagOfWords_synopsis,result.nodes.potentials_set
		 * .get(i).idf_synopsis,
		 * result.nodes.potentials_set.get(j).idf_synopsis); //use frequent and
		 * synopsis_vbadj also as features
		 * //System.out.println(result.nodes.potentials_set.get(i).name + " " +
		 * // result.nodes.potentials_set.get(j).name + ": " + outlink_sim);
		 * result.context_sim.matrix.set(i, j, context_sim);
		 * result.context_sim.matrix.set(j, i, context_sim); } } }
		 * 
		 * return result;
		 */
	}

	public static EdgeFeatureInfo calcInlinkSim(ArrayList<Integer> out1,
			ArrayList<Integer> out2) {
		EdgeFeatureInfo ef = new EdgeFeatureInfo();
		final int numDoc = 4417278;
		int max = 0;
		int min = 0;
		DecimalFormat df = new DecimalFormat("#.####");
		if (out1.size() > out2.size()) {
			max = out1.size();
			min = out2.size();
		} else {
			min = out1.size();
			max = out2.size();
		}
		// System.out.println("min: " + min + "max: " + max);
		ef.max = Math.log(max);
		ef.min = Math.log(min);
		double common = 0d;
		double diff = 0d;
		HashSet<Integer> out2_set = new HashSet<Integer>(out2);
		for (Integer id1 : out1) {
			if (out2_set.contains(id1)) {
				common++;
			} else {
				diff++;
			}
		}
		// System.out.println("common: " + common);
		ef.intersection = (int) common;
		ef.union = out1.size() + out2.size();

		// double num = common;
		// double deno = diff+out2.size();
		double sim = 0.0;
		double sim1 = 0.0;

		if (min == 0 || max == 0 || common == 0) {
			sim = 0.0;
		}

		else {
			double num = Math.log(max) - Math.log(common);
			// double deno = Math.log(numDoc) - Math.log(min);
			double deno = Math.log(numDoc) - Math.log(min);
			ef.numerator = num;
			ef.denominator = deno;
			if (deno == 0d) {
				sim = 0.0;
			} else {
				// sim = Math.pow(Math.E, -Double.valueOf(df.format(num /
				// deno)));
				sim1 = 1 - Double.valueOf(num / deno);
				if (sim1 < 0d) {
					sim = 0.0;
				} else {
					sim = sim1;
				}
			}

		}
		ef.similarity = sim;
		return ef;

	}

	public static EdgeFeatureInfo calcContextSim(ArrayList<String> out1,
			ArrayList<String> out2, ArrayList<Double> idf1,
			ArrayList<Double> idf2) {
		EdgeFeatureInfo ef = new EdgeFeatureInfo();
		double common = 0d;
		double diff = 0d;
		HashSet<String> out2_set = new HashSet<String>(out2);
		DecimalFormat df = new DecimalFormat("#.####");
		double numSquare = 0f;
		double denoSquare = 0f;

		for (int i = 0; i < out1.size(); i++) {
			String id1 = out1.get(i);
			if (out2_set.contains(id1)) {
				common++;
				numSquare += Math.pow(idf1.get(i), 2);
			} else {
				denoSquare += Math.pow(idf1.get(i), 2);
				diff++;
			}
		}

		for (int i = 0; i < idf2.size(); i++) {
			denoSquare += Math.pow(idf2.get(i), 2);
		}

		// double num = common;
		// double deno = diff+out2.size();

		double num = Math.pow(numSquare, 0.5);
		double deno = Math.pow(denoSquare, 0.5);

		ef.intersection = (int) common;
		ef.union = out1.size() + (int) diff;
		ef.numerator = num;
		ef.denominator = deno;
		double sim = 0.0;
		if ((int) deno == 0) {
			sim = 0.0;
		} else {
			sim = Double.valueOf(df.format(num / deno));

		}
		ef.similarity = sim;
		return ef;
	}

	public static EdgeFeatureInfo calcOutlinkSim(ArrayList<Integer> out1,
			ArrayList<Integer> out2) {
		EdgeFeatureInfo ef = new EdgeFeatureInfo();
		final int numDoc = 4417278;
		int max = 0;
		int min = 0;
		DecimalFormat df = new DecimalFormat("#.####");
		if (out1.size() > out2.size()) {
			max = out1.size();
			min = out2.size();
		} else {
			min = out1.size();
			max = out2.size();
		}

		double common = 0d;
		double diff = 0d;
		HashSet<Integer> out2_set = new HashSet<Integer>(out2);
		for (Integer id1 : out1) {
			if (out2_set.contains(id1)) {
				common++;
			} else {
				diff++;
			}
		}
		ef.intersection = (int) common;
		ef.union = out1.size() + out2.size();
		// double num = common;
		// double deno = diff+out2.size();
		double sim = 0.0;
		double sim1 = 0.0;
		if (min == 0 || max == 0 || common == 0) {
			sim = 0.0;
		} else {
			double num = Math.log(max) - Math.log(common);
			// double deno = Math.log(numDoc) - Math.log(min);
			double deno = Math.log(numDoc) - Math.log(min);
			ef.numerator = num;
			ef.denominator = deno;
			if (deno == 0d) {
				sim = 0.0;
			} else {
				// sim = Math.pow(Math.E, -Double.valueOf(df.format(num /
				// deno)));
				sim1 = 1 - Double.valueOf(num / deno);
				if (sim1 < 0d) {
					sim = 0.0;
				} else {
					sim = sim1;
				}
			}

		}
		ef.similarity = sim;
		return ef;
	}

	public static EdgeFeatureInfo calcJaccardSim(ArrayList<Integer> out1,
			ArrayList<Integer> out2) {
		EdgeFeatureInfo ef = new EdgeFeatureInfo();
		DecimalFormat df = new DecimalFormat("#.####");
		double common = 0d;
		double diff = 0d;
		HashSet<Integer> out2_set = new HashSet<Integer>(out2);
		for (Integer id1 : out1) {
			if (out2_set.contains(id1)) {
				common++;
			} else {
				diff++;
			}
		}
		// System.out.println("common: " + common);
		ef.intersection = (int) common;
		ef.union = (int) (diff + out2.size());

		// double num = common;
		// double deno = diff+out2.size();
		double sim = 0.0;

		// if (min == 0 || max == 0 || common == 0) {
		// sim = 0.0;
		// }

		if (out1.size() == 0 && out2.size() == 0) {
			sim = 0.0;
		} else {
			double num = 2 * common;
			double deno = out1.size() + out2.size();
			ef.numerator = num;
			ef.denominator = deno;

			sim = Double.valueOf(df.format(num / deno));

		}
		ef.similarity = sim;
		return ef;

	}

	public static double calcCosine(String[] terms, int[] tf, double[] idf,
			HashMap<String, Double> terms2tfidf) {
		double cosine = 0d;
		if (null != terms && null != terms2tfidf && !terms2tfidf.isEmpty()) {
			double dot = 0d;
			double eucld1 = 0d;
			for (int i = 0; i < terms.length; i++) {
				if (terms2tfidf.containsKey(terms[i])) {
					dot += tf[i] * idf[i] * terms2tfidf.get(terms[i]);
				}
				eucld1 += tf[i] * idf[i] * tf[i] * idf[i];
			}

			eucld1 = Math.sqrt(eucld1);
			double eucld2 = 0d;
			for (String term : terms2tfidf.keySet()) {
				eucld2 += terms2tfidf.get(term) * terms2tfidf.get(term);
			}
			eucld2 = Math.sqrt(eucld2);
			cosine = dot / (eucld1 * eucld2);
		}
		return cosine;
	}

	public static void main(String[] args) {
		ArrayList<Integer> out1 = null;
		ArrayList<Integer> out2 = null;
		try {
			out1 = InlinksReader.read(args[0]);
			out2 = InlinksReader.read(args[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}

		EdgeFeatureInfo info = FeatureExtractor.calcInlinkSim(out1, out2);

		System.out.println("sim: " + info.similarity + ", int : "
				+ info.intersection + ", union: " + info.union + "num: "
				+ info.numerator + "den: " + info.denominator);
	}

	static class InlinksReader {
		public static ArrayList<Integer> read(String file) throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String[] inlinks = reader.readLine().split(" ");
			ArrayList<Integer> inlinksList = new ArrayList<Integer>();
			for (String inlink : inlinks)
				inlinksList.add(Integer.parseInt(inlink));
			reader.close();
			return inlinksList;
		}
	}

	public static double calcCosine(String[] fullTextTerms,
			int[] fullTextTermsTf, double[] fullTextTermsIdf,
			String[] fullTextTerms2, int[] fullTextTermsTf2,
			double[] fullTextTermsIdf2) {
		HashMap<String, Double> terms2tfidf = new HashMap<String, Double>();
		for (int i = 0; null != fullTextTerms2 && i < fullTextTerms2.length; i++)
			terms2tfidf.put(fullTextTerms2[i], fullTextTermsTf2[i]
					* fullTextTermsIdf2[i]);
		return calcCosine(fullTextTerms, fullTextTermsTf, fullTextTermsIdf,
				terms2tfidf);
	}
}
