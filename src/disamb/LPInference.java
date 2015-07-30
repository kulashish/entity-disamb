package disamb;

import in.ac.iitb.cse.mrf.util.MathHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryCPLEX;
import spotting.ComputeCategorySimilarity;
import spotting.Config;
import spotting.FeatureExtractor;
import spotting.*;
import spotting.NodePotentials;
import spotting.TrainingData;
import util.DisambProperties;
import util.GraphCreator;
import Evaluation.Entity;
import Evaluation.Evaluator;
import Evaluation.Mention;
import Evaluation.Statistics;

//import net.sf.javailp.SolverFactoryLpSolve;

public class LPInference {
	private static final double MIN_NORM = 0.01;

	public double[][] nodeFeatureWeights;
	public double[][] nodeFeatureValues;
	public String[] entNames;
	public int[] label;
	public int[] ids;
	public HashSet<String> entAdded;
	public double[][] edgeFeatureWeights;
	public double[][] edgeFeatureValues;
	public int[][] edges;
	public HashMap<Integer, ArrayList<Integer>> edgeInfo;
	public TrainingData data;
	public HashMap<String, Double> ent2label;
	public HashMap<String, Integer> entity2Prediction;
	public HashSet<String> predictedTrueEnt;
	public HashMap<String, ArrayList<String>> mention2entNameList;
	public HashMap<Mention, ArrayList<Entity>> mention2entity;
	public HashMap<String, ArrayList<Integer>> mention2entIdList;

	public static ArrayList<Statistics> statListKdd;
	public static ArrayList<Statistics> statListOriginalWithSpotter;
	public static ArrayList<Statistics> statListOriginal;
	public static String dataSetType;

	public double[] Lpw0;
	public double[] Lpw1;
	public double[] Lpw00;
	public double[] Lpw11;

	public int actualNodes;
	public int actualEdges;

	public String nodeFeaturesUsed;
	public String edgeFeaturesUsed;

	public boolean perMentionConstraints;
	public boolean globalConstraints;
	public double upperLimitGlobalEntities;
	public double lowerLimitGlobalEntities;
	public int upperLimitPerMentionEntities;

	public LPInference(boolean gconstraint, boolean lconstraint, double ug,
			double lg, int ul) {
		perMentionConstraints = lconstraint;
		globalConstraints = gconstraint;
		upperLimitGlobalEntities = ug;
		lowerLimitGlobalEntities = lg;
		upperLimitPerMentionEntities = ul;
		Lpw0 = Config.Lpw0;
		Lpw1 = Config.Lpw1;
		Lpw00 = Config.Lpw00;
		Lpw11 = Config.Lpw11;
		System.out
				.println("LPInference1 " + globalConstraints + " "
						+ perMentionConstraints + " "
						+ upperLimitGlobalEntities + " "
						+ lowerLimitGlobalEntities + " "
						+ upperLimitPerMentionEntities);

	}

	public LPInference(double[] w0, double[] w1, double[] w00, double[] w11) {
		perMentionConstraints = Config.perMentionConstraints;
		globalConstraints = Config.globalConstraints;
		upperLimitGlobalEntities = Config.upperLimitGlobalEntities;
		lowerLimitGlobalEntities = Config.lowerLimitGlobalEntities;
		upperLimitPerMentionEntities = Config.upperLimitPerMentionEntities;
		Lpw0 = w0;
		Lpw1 = w1;
		Lpw00 = w00;
		Lpw11 = w11;
		System.out
				.println("LPInference2 " + perMentionConstraints + " "
						+ globalConstraints + " " + upperLimitGlobalEntities
						+ " " + lowerLimitGlobalEntities + " "
						+ upperLimitPerMentionEntities);
	}

	public LPInference() {
		perMentionConstraints = Config.perMentionConstraints;
		globalConstraints = Config.globalConstraints;
		upperLimitGlobalEntities = Config.upperLimitGlobalEntities;
		lowerLimitGlobalEntities = Config.lowerLimitGlobalEntities;
		upperLimitPerMentionEntities = Config.upperLimitPerMentionEntities;
		Lpw0 = Config.Lpw0;
		Lpw1 = Config.Lpw1;
		Lpw00 = Config.Lpw00;
		Lpw11 = Config.Lpw11;
		System.out
				.println("LPInference3 " + perMentionConstraints + " "
						+ globalConstraints + " " + upperLimitGlobalEntities
						+ " " + lowerLimitGlobalEntities + " "
						+ upperLimitPerMentionEntities);
	}

	public void loadEntityData() throws Exception {

		System.out.println("in load entity data size ::"
				+ data.nodes.potentials_set.size());
		entity2Prediction = new HashMap<String, Integer>();
		mention2entIdList = new HashMap<String, ArrayList<Integer>>();
		mention2entNameList = new HashMap<String, ArrayList<String>>();
		mention2entity = new HashMap<Mention, ArrayList<Entity>>();

		int num_nodes = data.nodes.potentials_set.size();

		int num_node_features = 0;
		if (Config.useInlinkCount)
			num_node_features++;
		if (Config.useOutlinkCount)
			num_node_features++;
		if (Config.useRedirection)
			num_node_features++;
		if (Config.usePageTitleScore)
			num_node_features++;
		if (Config.useAnchorTextScore)
			num_node_features++;
		if (Config.useSenseProbability)
			num_node_features++;
		if (Config.useAnchorCosine)
			num_node_features++;
		if (Config.useAnchorContextCosine)
			num_node_features++;
		if (Config.useFullTextCosine)
			num_node_features++;

		if (Config.intercept)
			num_node_features++;

		nodeFeatureWeights = new double[num_node_features][2];
		entAdded = new HashSet<String>();
		edgeInfo = new HashMap<Integer, ArrayList<Integer>>();
		entNames = new String[num_nodes];
		ids = new int[num_nodes];
		label = new int[num_nodes];
		int currNode = 0;

		if (Config.intercept) {
			nodeFeatureWeights[currNode][0] = Lpw0[0];
			nodeFeatureWeights[currNode][1] = Lpw1[0];
			currNode++;
			nodeFeaturesUsed += "Intercept" + "|";

		}
		if (Config.useInlinkCount) {
			nodeFeatureWeights[currNode][0] = Lpw0[1];
			nodeFeatureWeights[currNode][1] = Lpw1[1];
			currNode++;
			nodeFeaturesUsed += "InlinkCount" + "|";
		}
		if (Config.useOutlinkCount) {
			nodeFeatureWeights[currNode][0] = Lpw0[2];
			nodeFeatureWeights[currNode][1] = Lpw1[2];
			currNode++;
			nodeFeaturesUsed += "OutlinkCount" + "|";
		}
		if (Config.useRedirection) {
			nodeFeatureWeights[currNode][0] = Lpw0[3];
			nodeFeatureWeights[currNode][1] = Lpw1[3];
			currNode++;
			nodeFeaturesUsed += "Redirection" + "|";
		}
		if (Config.usePageTitleScore) {
			nodeFeatureWeights[currNode][0] = Lpw0[4];
			nodeFeatureWeights[currNode][1] = Lpw1[4];
			currNode++;
			nodeFeaturesUsed += "PageTitle" + "|";
		}
		if (Config.useAnchorTextScore) {
			nodeFeatureWeights[currNode][0] = Lpw0[5];
			nodeFeatureWeights[currNode][1] = Lpw1[5];
			currNode++;
			nodeFeaturesUsed += "AnchorJaccard" + "|";
		}
		if (Config.useSenseProbability) {
			nodeFeatureWeights[currNode][0] = Lpw0[6];
			nodeFeatureWeights[currNode][1] = Lpw1[6];
			currNode++;
			nodeFeaturesUsed += "SenseProb" + "|";
		}
		if (Config.useAnchorCosine) {
			nodeFeatureWeights[currNode][0] = Lpw0[7];
			nodeFeatureWeights[currNode][1] = Lpw1[7];
			currNode++;
			nodeFeaturesUsed += "AnchorCosine" + "|";
		}
		if (Config.useAnchorContextCosine) {
			nodeFeatureWeights[currNode][0] = Lpw0[8];
			nodeFeatureWeights[currNode][1] = Lpw1[8];
			currNode++;
			nodeFeaturesUsed += "AnchorContextCosine" + "|";
		}
		if (Config.useFullTextCosine) {
			nodeFeatureWeights[currNode][0] = Lpw0[9];
			nodeFeatureWeights[currNode][1] = Lpw1[9];
			currNode++;
			nodeFeaturesUsed += "FullCosine" + "|";
		}

		nodeFeatureValues = new double[num_node_features][num_nodes];

		int curr = 0;
		for (NodePotentials np : data.nodes.potentials_set) {
			if (np.name == null)
				continue;
			
			Mention key = new Mention();
			String[] m = np.mention.split("_");
			key.name = m[0];
			key.isNA = np.blnNA;
			if (m.length > 0)
				key.offset = Integer.parseInt(m[1]);
			else
				key.offset = np.offset;

			Entity val = new Entity();
			val.name = np.name;
			val.trueLabel = np.label;

			if (mention2entity.containsKey(key)) {
				mention2entity.get(key).add(val);
			} else {
				mention2entity.put(key, new ArrayList<Entity>());
				mention2entity.get(key).add(val);
			}

			if (mention2entNameList.containsKey(np.mention)) {
				if (!mention2entNameList.get(np.mention).contains(np.name)) {
					mention2entNameList.get(np.mention).add(np.name);
				}
			} else {
				mention2entNameList.put(np.mention, new ArrayList<String>());
				mention2entNameList.get(np.mention).add(np.name);
			}

			if (entAdded.contains(np.name))
				continue;

			int curr1 = 0;

			if (Config.intercept) {
				nodeFeatureValues[curr1][curr] = 1.0;
				curr1++;
			}

			if (Config.useInlinkCount) {
				nodeFeatureValues[curr1][curr] = np.inlink_count;
				curr1++;
			}
			if (Config.useOutlinkCount) {
				nodeFeatureValues[curr1][curr] = np.outlink_count;
				curr1++;
			}
			if (Config.useRedirection) {
				nodeFeatureValues[curr1][curr] = np.redirection;
				curr1++;
			}
			if (Config.usePageTitleScore) {
				nodeFeatureValues[curr1][curr] = np.page_title_score;
				curr1++;
			}
			if (Config.useAnchorTextScore) {
				nodeFeatureValues[curr1][curr] = np.anchor_text_score;
				curr1++;
			}
			if (Config.useSenseProbability) {
				nodeFeatureValues[curr1][curr] = np.sense_probability;
				curr1++;
			}
			if (Config.useAnchorCosine) {
				if (np.anchorTextTerms == null)
					np.anchor_text_cosine = 0d;
				else {
					np.anchor_text_cosine = FeatureExtractor.calcCosine(
							np.anchorTextTerms, np.anchorTextTermsTf,
							np.anchorTextTermsIdf,
							np.contextTermsForInput2TfIdfAnchorText);
				}

				nodeFeatureValues[curr1][curr] = np.anchor_text_cosine;
				curr1++;
			}
			if (Config.useAnchorContextCosine) {

				if (np.anchorTextContextTerms == null)
					np.anchor_text_context_cosine = 0d;
				else {
					np.anchor_text_context_cosine = FeatureExtractor
							.calcCosine(
									np.anchorTextContextTerms,
									np.anchorTextContextTermsTf,
									np.anchorTextContextTermsIdf,
									np.contextTermsForInput2TfIdfAnchorTextContext);
				}
				nodeFeatureValues[curr1][curr] = np.anchor_text_context_cosine;
				curr1++;
			}
			if (Config.useFullTextCosine) {

				if (np.fullTextTerms == null)
					np.full_text_cosine = 0d;
				else {
					np.full_text_cosine = FeatureExtractor.calcCosine(
							np.fullTextTerms, np.fullTextTermsTf,
							np.fullTextTermsIdf,
							np.contextTermsForInput2TfIdfFullText);
				}

				nodeFeatureValues[curr1][curr] = np.full_text_cosine;
				curr1++;
			}

			label[curr] = np.label;
			entNames[curr] = np.name;
			entAdded.add(np.name);
			ids[curr] = curr;
			edgeInfo.put(curr, new ArrayList<Integer>());
			curr++;
		}

		actualNodes = curr;

		if(num_nodes > 0)
			normalizeNodeFeatures(num_node_features, num_nodes);

		for (int j = curr; j < ids.length; j++) {
			ids[curr] = -1;
		}

		int num_edge_features = 0;
		if (Config.useCategSim)
			num_edge_features++;
		if (Config.useOutlinkSim)
			num_edge_features++;
		if (Config.useInlinkSim)
			num_edge_features++;
		if (Config.useContextFrequentSim)
			num_edge_features++;
		if (Config.useContextSynopsisSim)
			num_edge_features++;
		if (Config.useContextSynopsisVbdjSim)
			num_edge_features++;
		if (Config.useFullTextCosineSim)
			num_edge_features++;

		edgeFeatureWeights = new double[num_edge_features][2];
		curr = 0;
		if (Config.useCategSim) {
			edgeFeatureWeights[curr][0] = Lpw00[0];
			edgeFeatureWeights[curr][1] = Lpw11[0];
			curr++;
			edgeFeaturesUsed += "CategSim" + "|";
		}
		if (Config.useOutlinkSim) {
			edgeFeatureWeights[curr][0] = Lpw00[1];
			edgeFeatureWeights[curr][1] = Lpw11[1];
			curr++;
			edgeFeaturesUsed += "OutlinkSim" + "|";
		}
		if (Config.useInlinkSim) {
			edgeFeatureWeights[curr][0] = Lpw00[2];
			edgeFeatureWeights[curr][1] = Lpw11[2];
			curr++;
			edgeFeaturesUsed += "InlinkSim" + "|";
		}
		if (Config.useContextFrequentSim) {
			edgeFeatureWeights[curr][0] = Lpw00[3];
			edgeFeatureWeights[curr][1] = Lpw11[3];
			curr++;
			edgeFeaturesUsed += "FrequentSim" + "|";
		}
		if (Config.useContextSynopsisSim) {
			edgeFeatureWeights[curr][0] = Lpw00[4];
			edgeFeatureWeights[curr][1] = Lpw11[4];
			curr++;
			edgeFeaturesUsed += "SynopsisSim" + "|";
		}
		if (Config.useContextSynopsisVbdjSim) {
			edgeFeatureWeights[curr][0] = Lpw00[5];
			edgeFeatureWeights[curr][1] = Lpw11[5];
			curr++;
			edgeFeaturesUsed += "SynopsisVbdjSim" + "|";
		}
		if (Config.useFullTextCosineSim) {
			edgeFeatureWeights[curr][0] = Lpw00[6];
			edgeFeatureWeights[curr][1] = Lpw11[6];
			curr++;
			edgeFeaturesUsed += "FullCosineSim";
		}

		int num_edges = (num_nodes * (num_nodes - 1)) / 2; // complete graph

		edgeFeatureValues = new double[num_edge_features][num_edges];
		edges = new int[num_edges][2];

		curr = 0;
		for (int i = 0; i < data.nodes.potentials_set.size(); i++) {
			for (int j = i + 1; j < data.nodes.potentials_set.size(); j++) {
				NodePotentials np1 = data.nodes.potentials_set.get(i);
				NodePotentials np2 = data.nodes.potentials_set.get(j);
				String n1 = data.nodes.potentials_set.get(i).name;
				String n2 = data.nodes.potentials_set.get(j).name;

				int id1 = getid(n1);
				int id2 = getid(n2);

				if (n1.equalsIgnoreCase(n2) || id1 == id2)
					continue;

				if (edgePresent(id1, id2))
					continue;

				double inlinkScore = FeatureExtractor.calcInlinkSim(
						np1.inLinks, np2.inLinks).similarity;

				// if (inlinkScore <= MIN_NORM) {
				// System.out.println("inlink " + inlinkScore + " ent1 "
				// + np1.name + "  ent2 " + np2.name);
				// continue;
				// }

				// if (Config.filterInlink
				// && inlinkScore < Config.filterInlinkThreshold)
				// continue;

				if (id1 == -1 || id2 == -1) {
					System.out.println("something is wrong id is -1");
					continue;
				}

				edges[curr][0] = id1;
				edges[curr][1] = id2;
				addEdge(id1, id2);
				int curr1 = 0;
				if (Config.useCategSim) {
					edgeFeatureValues[curr1][curr] = ComputeCategorySimilarity
							.computeCategSim(n1.replaceAll(" ", "_"),
									n2.replaceAll(" ", "_"));
					curr1++;
				}
				if (Config.useOutlinkSim) {
					edgeFeatureValues[curr1][curr] = FeatureExtractor
							.calcOutlinkSim(np1.outLinks, np2.outLinks).similarity;
					curr1++;
				}
				if (Config.useInlinkSim) {
					edgeFeatureValues[curr1][curr] = inlinkScore;
					curr1++;
				}
				if (Config.useContextFrequentSim) {
					edgeFeatureValues[curr1][curr] = FeatureExtractor
							.calcContextSim(np1.bagOfWords_frequent,
									np2.bagOfWords_frequent, np1.idf_frequent,
									np2.idf_frequent).similarity;
					curr1++;
				}
				if (Config.useContextSynopsisSim) {
					edgeFeatureValues[curr1][curr] = FeatureExtractor
							.calcContextSim(np1.bagOfWords_synopsis,
									np2.bagOfWords_synopsis, np1.idf_synopsis,
									np2.idf_synopsis).similarity;
					curr1++;
				}
				if (Config.useContextSynopsisVbdjSim) {
					edgeFeatureValues[curr1][curr] = FeatureExtractor
							.calcContextSim(np1.bagOfWords_synopsis_vbadj,
									np2.bagOfWords_synopsis_vbadj,
									np1.idf_synopsis_vbadj,
									np2.idf_synopsis_vbadj).similarity;
					curr1++;
				}
				if (Config.useFullTextCosineSim) {
					edgeFeatureValues[curr1][curr] = FeatureExtractor
							.calcCosine(np1.fullTextTerms, np1.fullTextTermsTf,
									np1.fullTextTermsIdf, np2.fullTextTerms,
									np2.fullTextTermsTf, np2.fullTextTermsIdf);
				}
				curr++;
			}
		}

		actualEdges = curr;

		if((num_edges > 0) && (num_edge_features > 0)){
			normalizeEdgeFeatures(num_edge_features, num_edges);
		}
		if (Config.threshold) {
			if(edgeFeatureValues.length > 0) {
				for (int i = 0; i < edgeFeatureValues[0].length; i++) {
					if (edgeFeatureValues[Config.thresholdEdgeFeature][i] <= Config.thresholdValue) {
						for (int j = 0; j < edgeFeatureValues.length; j++) {
							edgeFeatureValues[j][i] = -1;
						}
					}
				}
			}
		}

		for (String key : mention2entNameList.keySet()) {
			mention2entIdList.put(key, new ArrayList<Integer>());
		}

		for (String key : mention2entNameList.keySet()) {
			for (String ent : mention2entNameList.get(key)) {
				int id = getid(ent);
				if (id != -1) {
					mention2entIdList.get(key).add(id);
				}
			}
		}

	}

	public int getid(String name) {
		for (int i = 0; i < entNames.length; i++) {
			if (entNames[i] != null && entNames[i].equalsIgnoreCase(name))
				return i;
		}
		return -1;
	}

	public boolean edgePresent(Integer n1, Integer n2) {
		if (edgeInfo.get(n1) == null || edgeInfo.get(n2) == null) {
			return false;
		} else if (edgeInfo.get(n1).contains(n2)
				|| edgeInfo.get(n2).contains(n1))
			return true;
		else
			return false;
	}

	public void addEdge(Integer n1, Integer n2) {
		edgeInfo.get(n1).add(n2);
		edgeInfo.get(n2).add(n1);
	}

	public void normalizeNodeFeatures(int numNodeFeatures, int numNodes) {
		double[][] tempFeatureValues = new double[numNodes][numNodeFeatures];
		tempFeatureValues = transpose(nodeFeatureValues);
		MathHelper.normalizeVectors(tempFeatureValues);
		nodeFeatureValues = transpose(tempFeatureValues);
	}

	public void normalizeEdgeFeatures(int numEdgeFeatures, int numEdges) {
		System.out.println("params numEdgeFeatures : " + numEdgeFeatures + " numEdges : " + numEdges);
		double[][] tempFeatureValues = new double[numEdges][numEdgeFeatures];
		tempFeatureValues = transpose(edgeFeatureValues);
		MathHelper.normalizeVectors(tempFeatureValues);
		edgeFeatureValues = transpose(tempFeatureValues);
	}

	public double[][] transpose(double[][] mat) {
		double[][] transMat = new double[mat[0].length][mat.length];

		for (int i = 0; i < mat[0].length; i++)
			for (int j = 0; j < mat.length; j++) {
				transMat[i][j] = 0;
			}
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++) {
				transMat[j][i] = mat[i][j];
			}
		return transMat;
	}

	public Result inference() {

		// SolverFactory factory = new SolverFactoryLpSolve(); // use lp_solve
		SolverFactory factory = new SolverFactoryCPLEX();
		Map<Object, Object> params = factory.getParameters();

		factory.setParameter(Solver.VERBOSE, 0);
		factory.setParameter(Solver.TIMEOUT, 1000); // set timeout to 100
		// seconds

		Problem problem = new Problem();
		Linear linear = new Linear();

		Linear linearGlobal1 = new Linear();
		// Linear linearGlobal0 = new Linear();

		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == -1)
				continue;
			double coefficient0 = getNodePotential(i, 0);
			double coefficient1 = getNodePotential(i, 1);
			linear.add(coefficient0, "n" + i + "_0");
			linear.add(coefficient1, "n" + i + "_1");

			problem.setVarLowerBound("n" + i + "_0", 0);
			problem.setVarLowerBound("n" + i + "_1", 0);

			Linear linearConstraint = new Linear();
			linearConstraint.add(1, "n" + i + "_0");
			linearConstraint.add(1, "n" + i + "_1");

			if (Config.clampingInLPInference && label[i] == 1) {
				Linear constraint = new Linear();
				constraint.add(1, "n" + i + "_1");
				problem.add(constraint, "=", 1);
				// System.out.println(entNames[i]);
			}

			problem.add(linearConstraint, "=", 1);

			problem.setVarType("n" + i + "_0", Integer.class);
			problem.setVarType("n" + i + "_1", Integer.class);

			linearGlobal1.add(1, "n" + i + "_1");

		}

		if (globalConstraints) {
			System.out.println("in global " + upperLimitGlobalEntities + " "
					+ lowerLimitGlobalEntities + " "
					+ mention2entIdList.keySet().size());
			problem.add(linearGlobal1, "<=", (int) mention2entIdList.keySet()
					.size() * upperLimitGlobalEntities);
			problem.add(linearGlobal1, ">=", (int) mention2entIdList.keySet()
					.size() * lowerLimitGlobalEntities);
		}

		if (perMentionConstraints) {
			System.out
					.println("in per mention " + upperLimitPerMentionEntities);
			for (String key : mention2entIdList.keySet()) {
				Linear linearLocal = new Linear();
				for (int j = 0; j < mention2entIdList.get(key).size(); j++) {
					linearLocal.add(1, "n" + mention2entIdList.get(key).get(j)
							+ "_1");
					// System.out.print("n" + mention2entIdList.get(key).get(j)
					// + "_1");
				}
				// System.out.println();
				problem.add(linearLocal, "<=",
						(int) upperLimitPerMentionEntities);
			}
		}

		// for (int i = 0; i < entNames.length; i++) {
		// System.out.println(entNames[i] + " ---> " + getid(entNames[i]));
		// }
		// System.out.println(getid("Food energy"));
		// System.out.println(getid("Calorie"));

		for (int i = 0; i < actualEdges; i++) {
			double coefficient0 = getEdgePotential(i, 0);
			double coefficient1 = getEdgePotential(i, 1);
			if (coefficient0 <= 0d && coefficient1 <= 0d)
				continue;

			linear.add(coefficient0, "e" + edges[i][0] + edges[i][1] + "_0");
			linear.add(coefficient1, "e" + edges[i][0] + edges[i][1] + "_1");

			problem.setVarLowerBound("e" + edges[i][0] + edges[i][1] + "_0", 0);
			problem.setVarLowerBound("e" + edges[i][0] + edges[i][1] + "_1", 0);

			Linear constraint0 = new Linear();
			constraint0.add(1, "e" + edges[i][0] + edges[i][1] + "_0");
			constraint0.add(-1, "n" + edges[i][0] + "_0");

			Linear constraint1 = new Linear();
			constraint1.add(1, "e" + edges[i][0] + edges[i][1] + "_1");
			constraint1.add(-1, "n" + edges[i][0] + "_1");

			problem.add(constraint0, "<=", 0);
			problem.add(constraint1, "<=", 0);

			constraint0 = new Linear();
			constraint0.add(1, "e" + edges[i][0] + edges[i][1] + "_0");
			constraint0.add(-1, "n" + edges[i][1] + "_0");

			constraint1 = new Linear();
			constraint1.add(1, "e" + edges[i][0] + edges[i][1] + "_1");
			constraint1.add(-1, "n" + edges[i][1] + "_1");

			problem.add(constraint0, "<=", 0);
			problem.add(constraint1, "<=", 0);

			problem.setVarType("e" + edges[i][0] + edges[i][1] + "_1",
					Integer.class);
			problem.setVarType("e" + edges[i][0] + edges[i][1] + "_0",
					Integer.class);

		}

		System.out.println("nodes " + actualNodes + "edges " + actualEdges
				+ "linear obj " + linear.size());

		problem.setObjective(linear, OptType.MAX);

		// System.out
		// .println("problem objective " + problem.getObjective().size());

		Solver solver = factory.get();

		// System.out.println("solver  " + solver.toString());

		Result result = solver.solve(problem);

		// System.out.println("result " + result);

		return result;

	}

	public double[] getNodePotentialArray(int index, int classlabel) {
		double[] pot = new double[nodeFeatureWeights.length];
		for (int i = 0; i < nodeFeatureValues.length; i++) {
			pot[i] = 0.0;
		}
		for (int i = 0; i < nodeFeatureValues.length; i++) {
			pot[i] = nodeFeatureValues[i][index]
					* nodeFeatureWeights[i][classlabel];
		}
		return pot;
	}

	public double[] getEdgePotentialArray(int index, int classlabel) {
		double[] pot = new double[edgeFeatureValues.length];
		for (int i = 0; i < edgeFeatureValues.length; i++) {
			pot[i] = 0.0;
		}
		for (int i = 0; i < edgeFeatureValues.length; i++) {
			pot[i] = edgeFeatureValues[i][index]
					* edgeFeatureWeights[i][classlabel];
		}
		return pot;
	}

	public double getNodePotential(int index, int classlabel) {
		double pot = 0.0;
		double features[] = new double[nodeFeatureValues.length];
		double weights[] = new double[nodeFeatureValues.length];
		for (int i = 0; i < nodeFeatureValues.length; i++) {
			features[i] = nodeFeatureValues[i][index];
			weights[i] = nodeFeatureWeights[i][classlabel];
			// pot += nodeFeatureValues[i][index]
			// * nodeFeatureWeights[i][classlabel];
		}
		pot = MathHelper.dotProduct(features, weights);
		return pot;
	}

	public double getEdgePotential(int index, int classlabel) {
		double pot = 0.0;
		double features[] = new double[edgeFeatureValues.length];
		double weights[] = new double[edgeFeatureWeights.length];
		for (int i = 0; i < edgeFeatureValues.length; i++) {
			features[i] = edgeFeatureValues[i][index];
			weights[i] = edgeFeatureWeights[i][classlabel];
			// pot += edgeFeatureValues[i][index]
			// * edgeFeatureWeights[i][classlabel];
		}
		pot = MathHelper.dotProduct(features, weights);
		return pot;
	}

	public HashMap<String, Double> createEntityLabelMap(Result res) {
		HashMap<String, Double> ent2label = new HashMap<String, Double>();
		predictedTrueEnt = new HashSet<String>();
		int c = 0;
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == -1)
				break;
			int r1 = res.get("n" + i + "_1").intValue();
			int r2 = res.getPrimalValue("n" + i + "_1").intValue();
			if (r1 == 1 || r2 == 1) {
				predictedTrueEnt.add(entNames[i]);
				c++;
			}
		}
		// System.out.println("Total labels as 1 :: " + c);

		for (String key : predictedTrueEnt) {
			ent2label.put(key, 1.0);
		}
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == -1)
				break;
			else {
				if (entNames[i] != null && !ent2label.containsKey(entNames[i])) {
					ent2label.put(entNames[i], 0.0);
				}
			}
		}

		return ent2label;
	}

	/*
	 * @param training data for a document (after running SpotterNew)
	 * 
	 * returns HashMap of entity to its label {0,1}
	 */

	public HashMap<String, Double> runLPInference(TrainingData traindata) {
		data = traindata;
		try {
			loadEntityData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Result res = inference();
		ent2label = createEntityLabelMap(res);
		return ent2label;
	}

	public void writetoCSVFeatures(String csvFile) {
		try {
			String CSV_SEPARATOR = "|";

			FileWriter f1 = new FileWriter(csvFile, true);
			for (int i = 0; i < actualNodes; i++) {
				StringBuffer oneLine = new StringBuffer();

				oneLine.append(entNames[i]);
				oneLine.append(CSV_SEPARATOR);
				for (int j = 0; j < nodeFeatureValues.length; j++) {
					oneLine.append(nodeFeatureValues[j][i]);
					oneLine.append(CSV_SEPARATOR);
				}
				oneLine.append(label[i]);

				f1.write(oneLine.toString());
				f1.write("\n");

			}
			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public void writetoCSVEdgeFeatures(String csvFile) {
		String CSV_SEPARATOR = "|";
		try {
			FileWriter f1 = new FileWriter(csvFile, true);

			for (int i = 0; i < actualEdges; i++) {
				StringBuffer oneLine = new StringBuffer();
				double coefficient0 = getEdgePotential(i, 0);
				double coefficient1 = getEdgePotential(i, 1);
				oneLine.append(coefficient0);
				oneLine.append(CSV_SEPARATOR);
				oneLine.append(coefficient1);
				oneLine.append(CSV_SEPARATOR);
				oneLine.append(edges[i][0]);
				oneLine.append(CSV_SEPARATOR);
				oneLine.append(edges[i][1]);
				oneLine.append(CSV_SEPARATOR);
				oneLine.append(entNames[edges[i][0]]);
				oneLine.append(CSV_SEPARATOR);
				oneLine.append(entNames[edges[i][1]]);
				oneLine.append(CSV_SEPARATOR);

				f1.write(oneLine.toString());
				f1.write("\n");

			}

			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public void writetoCSV(String csvFile, String mention, NodePotentials np,
			double predictedLabel, int id) {
		try {
			String CSV_SEPARATOR = "|";

			FileWriter f1 = new FileWriter(csvFile, true);

			StringBuffer oneLine = new StringBuffer();

			String ment = mention.split("_")[0];
			String off = mention.split("_")[1];

			oneLine.append(off);
			oneLine.append(CSV_SEPARATOR);

			oneLine.append(Integer.parseInt(off) + ment.length() - 1);
			oneLine.append(CSV_SEPARATOR);

			oneLine.append(ment);
			oneLine.append(CSV_SEPARATOR);

			oneLine.append(np.name);
			oneLine.append(CSV_SEPARATOR);

			oneLine.append(predictedLabel);
			oneLine.append(CSV_SEPARATOR);

			oneLine.append(np.label);
			oneLine.append(CSV_SEPARATOR);

			// double[] nodeArray0 = getNodePotentialArray(id, 0);
			// double[] nodeArray1 = getNodePotentialArray(id, 1);

			oneLine.append(getNodePotential(id, 0));
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(getNodePotential(id, 1));
			oneLine.append(CSV_SEPARATOR);

			// double[] edgeArray0 = getEdgePotentialArray(id, 0);
			// double[] edgeArray1 = getEdgePotentialArray(id, 1);

			// oneLine.append(entNames[edges[id][0]]);
			// oneLine.append(CSV_SEPARATOR);
			//
			// oneLine.append(entNames[edges[id][1]]);
			// oneLine.append(CSV_SEPARATOR);

			// for (int i = 0; i < nodeArray0.length; i++) {
			// oneLine.append(nodeArray0[i]);
			// oneLine.append(CSV_SEPARATOR);
			// }

			// for (int i = 0; i < edgeArray0.length; i++) {
			// oneLine.append(edgeArray0[i]);
			// oneLine.append(CSV_SEPARATOR);
			// }

			// for (int i = 0; i < nodeArray1.length; i++) {
			// oneLine.append(nodeArray1[i]);
			// oneLine.append(CSV_SEPARATOR);
			// }

			// for (int i = 0; i < edgeArray1.length; i++) {
			// oneLine.append(edgeArray1[i]);
			// oneLine.append(CSV_SEPARATOR);
			// }
			//

			f1.write(oneLine.toString());
			f1.write("\n");

			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public void writetoCSVHeaders(String csvFile, String header) {
		try {
			StringBuffer oneLine = new StringBuffer();
			FileWriter f1 = new FileWriter(csvFile, true);
			oneLine.append(header);

			f1.write(oneLine.toString());
			f1.write("\n");
			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public static void writeHeadertoAccuracyFile(String filename, int run) {
		try {
			StringBuffer oneLine = new StringBuffer();
			FileWriter f1 = new FileWriter(filename, true);
			oneLine.append("\n");
			oneLine.append("-------------------------------  " + run
					+ "   ---------------------------------");
			oneLine.append("\n");
			oneLine.append("\n");
			f1.write(oneLine.toString());
			f1.write("\n");
			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

	}

	public static void writetoAccuracyFile(String filename, String docName,
			Statistics kdd, Statistics original, Statistics withSpotter) {
		try {
			StringBuffer oneLine = new StringBuffer();
			FileWriter f1 = new FileWriter(filename, true);
			String CSV_Separator = "|";
			oneLine.append(docName);
			oneLine.append(CSV_Separator);
			oneLine.append(kdd.getPrecision());
			oneLine.append(CSV_Separator);
			oneLine.append(kdd.getRecall());
			oneLine.append(CSV_Separator);
			oneLine.append(kdd.getFmeasure());
			oneLine.append(CSV_Separator);
			oneLine.append(original.getPrecision());
			oneLine.append(CSV_Separator);
			oneLine.append(original.getRecall());
			oneLine.append(CSV_Separator);
			oneLine.append(original.getFmeasure());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getPrecision());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getRecall());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getFmeasure());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getSpotterError());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getTruePos());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getFalsePos());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getFalseNeg());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getTotalNA());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getTrueNA());
			oneLine.append(CSV_Separator);
			oneLine.append(withSpotter.getNAPrecision());
			oneLine.append("\n");

			f1.write(oneLine.toString());
			f1.write("\n");
			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public static void writeGlobalAccuracy(String filename,
			ArrayList<Statistics> kdd, ArrayList<Statistics> original,
			ArrayList<Statistics> withSpotter) {
		try {
			StringBuffer oneLine = new StringBuffer();
			FileWriter f1 = new FileWriter(filename, true);
			String CSV_Separator = "|";
			oneLine.append("MacroAveraged");
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroPrecision(kdd));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroRecall(kdd));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroFmeasure(kdd));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroPrecision(original));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroRecall(original));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroFmeasure(original));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroPrecision(withSpotter));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroRecall(withSpotter));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMacroFmeasure(withSpotter));
			oneLine.append("\n");

			f1.write(oneLine.toString());
			f1.write("\n");

			oneLine = new StringBuffer();
			oneLine.append("MicroAveraged");
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroPrecision(kdd));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroRecall(kdd));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroFmeasure(kdd));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroPrecision(original));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroRecall(original));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroFmeasure(original));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroPrecision(withSpotter));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroRecall(withSpotter));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getMicroFmeasure(withSpotter));
			oneLine.append(CSV_Separator);
			oneLine.append(Statistics.getTotalSpotterError(withSpotter));
			oneLine.append("\n");

			f1.write(oneLine.toString());
			f1.write("\n");

			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public static void writetoglobalFile(String filename, int run, double w00,
			double w11, double recall, double precision) {
		try {
			StringBuffer oneLine = new StringBuffer();
			FileWriter f1 = new FileWriter(filename, true);

			String CSV_SEPARATOR = "|";

			oneLine.append(run);
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(w00);
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(w11);
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(precision);
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(recall);
			oneLine.append(CSV_SEPARATOR);

			f1.write(oneLine.toString());
			f1.write("\n");
			f1.flush();
			f1.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	/*
	 * @param path to documents, disamb.properties file, precision recall file,
	 * path to detailed result folder.
	 */

	// public static void manualTuning(File trainFolder, String accuracyFile,
	// double startValuew00, double endValuew11, double stepSize) {
	// int globalTruePos = 0;
	// int globalFalsePos = 0;
	// int globalTrueNeg = 0;
	// int globalFalseNeg = 0;
	//
	// double[] Lpw0 = { 1.022067514036876, 1.0664030896901386,
	// 1.020562236165359, 1.0420524492630134, 1.0736306821021004,
	// 0.9493857763525069, 1.0490879542696572, 1.0194497368207978,
	// 1.0324927349426463 };
	// double[] Lpw1 = { 1.043041671426192, 0.9987060957729293,
	// 1.0445469492977069, 1.0230567362000564, 0.9914785033609638,
	// 1.1157234091105617, 1.0160212311934111, 1.0456594486422701,
	// 1.0326164505204183 };
	//
	// double[] Lpw00 = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
	// double[] Lpw11 = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
	//
	// double w00 = startValuew00;
	// double w11 = endValuew11;
	//
	// int run = 0;
	//
	// while (w11 > w00) {
	//
	// run += 1;
	//
	// Lpw00[2] = w00;
	// Lpw11[2] = w11;
	//
	// writeHeadertoAccuracyFile(accuracyFile, run);
	// int c = 0;
	// for (File trainFile : trainFolder.listFiles()) {
	// String docName = trainFile.getName();
	// if (trainFile.isFile()) {
	// long start = System.currentTimeMillis();
	// GraphCreator creator = GraphCreator.getInstance();
	//
	// LPInference lp = new LPInference(Lpw0, Lpw1, Lpw00, Lpw11);
	//
	// try {
	// lp.data = creator.loadFromFile(trainFile);
	// } catch (Exception e1) {
	// e1.printStackTrace();
	// }
	//
	// System.out.println("Creating node and edge features for "
	// + docName);
	//
	// // CollectiveTraining ct1 = new
	// // CollectiveTraining(trainPath,
	// // "/mnt/bag1/kanika/wikipediaGroundtruth.xml",
	// // "/home/kanika/wikiTraining/annotationXmls/" + docName
	// // + ".xml", docName);
	//
	// String file = "/mnt/bag1/kanika/temp/" + docName + ".csv";
	// String file1 = "/mnt/bag1/kanika/temp/Label" + docName
	// + ".csv";
	// // ct1.spotterNew();
	//
	// long elapsedTimeMillis = System.currentTimeMillis() - start;
	// float elapsedTimeMin1 = elapsedTimeMillis / (60 * 1000F);
	// System.out.println("Spotter took " + elapsedTimeMin1
	// + " time");
	//
	// // LPInference lp = new LPInference();
	// // lp.data = ct1.getData();
	//
	// float elapsedTimeMin2 = 0;
	//
	// try {
	// lp.loadEntityData();
	// elapsedTimeMillis = System.currentTimeMillis() - start;
	// elapsedTimeMin2 = elapsedTimeMillis / (60 * 1000F);
	// System.out
	// .println("Loading Features took "
	// + (elapsedTimeMin2 - elapsedTimeMin1)
	// + " time");
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// Result res = lp.inference();
	//
	// System.out.println("result " + res);
	// if (res != null) {
	// lp.ent2label = lp.createEntityLabelMap(res);
	//
	// elapsedTimeMillis = System.currentTimeMillis() - start;
	// float elapsedTimeMin3 = elapsedTimeMillis
	// / (60 * 1000F);
	// System.out
	// .println("ILP Inference took "
	// + (elapsedTimeMin3 - elapsedTimeMin2)
	// + " time");
	//
	// int truePos = 0;
	// int falseNeg = 0;
	// int falsePos = 0;
	// int trueNeg = 0;
	//
	// for (NodePotentials np : lp.data.nodes.potentials_set) {
	// if (np.label == 1) {
	// if (lp.predictedTrueEnt.contains(np.name)) {
	// truePos++;
	// } else {
	// falseNeg++;
	// }
	// } else {
	// if (lp.predictedTrueEnt.contains(np.name)) {
	// falsePos++;
	//
	// } else {
	// trueNeg++;
	// }
	// }
	// }
	//
	// String header = "entity|" + lp.nodeFeaturesUsed
	// + "|Intercept|label";
	// // String header = "mention|entity|predicted|true|"
	// // + lp.nodeFeaturesUsed + lp.edgeFeaturesUsed
	// // + lp.nodeFeaturesUsed + lp.edgeFeaturesUsed;
	// //
	// lp.writetoCSVHeaders(file, header);
	// // lp.writetoCSVEdgeFeatures(args[3] + docName
	// // + "EdgeFeatures" + ".csv");
	//
	// lp.writetoCSVFeatures(file);
	//
	// for (String mention : lp.mention2entNameList.keySet()) {
	// for (String entity : lp.mention2entNameList
	// .get(mention)) {
	//
	// for (NodePotentials np : lp.data.nodes.potentials_set) {
	// if (np.name.equals(entity)) {
	// int id = lp.getid(entity);
	// lp.writetoCSV(file1, mention, np,
	// lp.ent2label.get(entity), id);
	// break;
	// }
	//
	// }
	// }
	//
	// }
	// System.out.println("TruePos :: " + truePos
	// + "\nFalsePos :: " + falsePos + "\nTrueNeg :: "
	// + trueNeg + "\nFalseNeg :: " + falseNeg);
	//
	// float recall = (float) truePos
	// / (float) (truePos + falseNeg);
	// float precision = (float) truePos
	// / (float) (truePos + falsePos);
	//
	// writetoAccuracyFile(accuracyFile, docName, recall,
	// precision);
	//
	// globalTruePos += truePos;
	// globalFalseNeg += falseNeg;
	// globalTrueNeg += trueNeg;
	// globalFalsePos += falsePos;
	// }
	// System.out
	// .println("-----------------------------------------Completed file "
	// + (++c)
	// + " Doc Name ::"
	// + docName
	// + "------------------------------------------------------");
	//
	// }
	//
	// }
	// float globalrecall = (float) globalTruePos
	// / (float) (globalTruePos + globalFalseNeg);
	// float globalprecision = (float) globalTruePos
	// / (float) (globalTruePos + globalFalsePos);
	// writetoAccuracyFile(accuracyFile, "global", globalrecall,
	// globalprecision);
	//
	// System.out.println("Global True Pos ::" + globalTruePos
	// + "\nGlobal False Pos :: " + globalFalsePos
	// + "\nGlobal True Neg :: " + globalTrueNeg
	// + "\nGlobal False Neg :: " + globalFalseNeg
	// + "\nPrecision :: " + globalprecision + "\nRecall :: "
	// + globalrecall);
	//
	// writetoglobalFile(accuracyFile + "_Analysis.csv", run, w00, w11,
	// globalprecision, globalrecall);
	//
	// w00 += stepSize;
	// w11 -= stepSize;
	//
	// }
	// }

	public static void inference(File trainFolder, String file2, boolean g,
			boolean l, double gu, double gl, int lu) {

		String accuracyFile = file2 + ".csv";

		// int globalTruePos = 0;
		// int globalFalsePos = 0;
		// int globalTrueNeg = 0;
		// int globalFalseNeg = 0;
		DisambProperties props = DisambProperties.getInstance();
		int c = 0;
		for (File trainFile : trainFolder.listFiles()) {
			String docName = trainFile.getName();
			if (trainFile.isFile()) {
				long start = System.currentTimeMillis();
				GraphCreator creator = GraphCreator.getInstance();

				LPInference lp = new LPInference(g, l, gu, gl, lu);

				try {
				lp.data = creator.loadFromFile(trainFile);
				} catch (Exception e1) {
					e1.printStackTrace();
				}

				System.out.println("Creating node and edge features for "
						+ docName);

			//	 CollectiveTraining ct1 = new CollectiveTraining("/home/kanika/bat-env-0.1/benchmark/datasets/AQUAINT/RawTexts/",
			//	 "","/home/kanika/bat-env-0.1/benchmark/datasets/AQUAINT/Problems/"+docName, docName, dataSetType);

				String file = file2 + "/" + docName + "_features" + ".csv";
				String file1 = file2 + "/" + docName + "_potentials" + ".csv";
				String file3 = file2 + "/" + docName + "_edgefeatures" + ".csv";

			//	 ct1.spotterWikiMiner();
			//	 lp.data = ct1.getData();

				long elapsedTimeMillis = System.currentTimeMillis() - start;
				float elapsedTimeMin1 = elapsedTimeMillis / (60 * 1000F);
				System.out.println("Spotter took " + elapsedTimeMin1 + " time");

				// LPInference lp = new LPInference();
				 //lp.data = ct1.getData();

				float elapsedTimeMin2 = 0;

				try {
					lp.loadEntityData();
					elapsedTimeMillis = System.currentTimeMillis() - start;
					elapsedTimeMin2 = elapsedTimeMillis / (60 * 1000F);
					System.out.println("Loading Features took "
							+ (elapsedTimeMin2 - elapsedTimeMin1) + " time");
				} catch (Exception e) {
					e.printStackTrace();
				}

				Result res = lp.inference();
				if (res == null) {
					System.out
							.println("-----------------Result :: null-------------");
				}
				// System.out.println("result " + res);
				if (res != null) {
					lp.ent2label = lp.createEntityLabelMap(res);

					elapsedTimeMillis = System.currentTimeMillis() - start;
					float elapsedTimeMin3 = elapsedTimeMillis / (60 * 1000F);
					System.out.println("ILP Inference took "
							+ (elapsedTimeMin3 - elapsedTimeMin2) + " time");

					// for (String mention : lp.mention2entNameList.keySet()) {
					// System.out.print(mention + " ---> ");
					// for (int i = 0; i < lp.mention2entNameList.get(mention)
					// .size(); i++) {
					// System.out.print(lp.mention2entNameList
					// .get(mention).get(i) + " || ");
					// }
					// System.out.println();
					// }
					// for (String ent : lp.predictedTrueEnt)
					// System.out.println(ent);

					// int truePos = 0;
					// int falseNeg = 0;
					// int falsePos = 0;
					// int trueNeg = 0;

					// for (NodePotentials np : lp.data.nodes.potentials_set) {
					// if (np.label == 1) {
					// if (lp.predictedTrueEnt.contains(np.name)) {
					// truePos++;
					// } else {
					// falseNeg++;
					// }
					// } else {
					// if (lp.predictedTrueEnt.contains(np.name)) {
					// falsePos++;
					//
					// } else {
					// trueNeg++;
					// }
					// }
					// }

					String header = "mention|entity|predicted|true|"
							+ lp.nodeFeaturesUsed + lp.edgeFeaturesUsed
							+ lp.nodeFeaturesUsed + lp.edgeFeaturesUsed;

					// String header = "entity|" + lp.nodeFeaturesUsed
					// + "|Intercept|label";

					// lp.writetoCSVHeaders(file, header);

					lp.writetoCSVEdgeFeatures(file3);

					// lp.writetoCSVHeaders(file, header);
					// lp.writetoCSVEdgeFeatures(args[3] + docName
					// + "EdgeFeatures" + ".csv");

					lp.writetoCSVFeatures(file);

					for (String mention : lp.mention2entNameList.keySet()) {
						for (String entity : lp.mention2entNameList
								.get(mention)) {

							for (NodePotentials np : lp.data.nodes.potentials_set) {
								if (np.name.equals(entity)) {
									int id = lp.getid(entity);
									lp.writetoCSV(file1, mention, np,
											lp.ent2label.get(entity), id);
									break;
								}

							}
						}

					}

					for (Mention key : lp.mention2entity.keySet()) {
						// System.out.println("Mention ---> " + key.name);
						// System.out.print("Entities :: ");
						Iterator<Entity> it = lp.mention2entity.get(key)
								.iterator();
						while (it.hasNext()) {
							Entity obj = it.next();
							if (lp.predictedTrueEnt.contains(obj.name)) {
								obj.predictedLabel = 1;
							} else {
								obj.predictedLabel = 0;
							}
							// System.out.println(obj.name + " " +
							// obj.trueLabel);
						}
					}

					System.out
							.println("no.of spots-----------------> "
									+ lp.mention2entity.size()
									+ "predicted label 1 entities --------------------> "
									+ lp.predictedTrueEnt.size());
					// for (String ent : lp.predictedTrueEnt)
					// System.out.println(ent);

					String documentName = null;
					String documentName2 = null;

				
					 if (dataSetType.equals("kddo")) {
					 String[] str = docName.split("\\.");
					 if (str.length == 3)
					 documentName = str[0] + "." + str[1];
					 else
					 documentName = str[0];
					 }
					
					 else if(dataSetType.equals("kddc")) {
					 String[] str = docName.split("\\.");
					 if (str.length == 3)
					 documentName = str[0] + "." + str[1];
					 else
					 documentName = str[0];
					 documentName2 = "KDD_" + documentName;
					 }
					
					 else if (dataSetType.equals("wikic")) {
					 String[] str = docName.split("\\.");
					 documentName = str[0];
					 }

					else{
						documentName=docName;
					}

					System.out.println("document name given to Evaluator "
							+ documentName);

					Statistics statskdd = new Statistics();
					Statistics statsOriginal = new Statistics();
					Statistics statsWithSpotter = new Statistics();

					if (dataSetType.equals("kddo")) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						statskdd = evaluator
								.KddEvaluator(DisambProperties.getInstance()
										.getkddOriginalGroundObjectFile());
						statListKdd.add(statskdd);
					} else if (dataSetType.equals("kddc")) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName2);
						statskdd = evaluator.KddEvaluator(DisambProperties
								.getInstance().getkddCuratedObjectFolder()
								+ documentName + ".xml.object");
						statListKdd.add(statskdd);
					} else if (dataSetType.equals("wikic")) {
						System.out.println(DisambProperties.getInstance()
								.getwikiCuratedObjectFolder()
								+ documentName
								+ ".xml.object");
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						statskdd = evaluator.KddEvaluator(DisambProperties
								.getInstance().getwikiCuratedObjectFolder()
								+ documentName + ".xml.object");
						statListKdd.add(statskdd);
					} else if (dataSetType.equals(props.getMSNBCdataset())) {
						System.out.println(DisambProperties.getInstance()
								.getmsnbcObjectFolder()
								+ documentName
								+ ".xml.object");
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						statskdd = evaluator.KddEvaluator(DisambProperties
								.getInstance().getmsnbcObjectFolder()
								+ documentName + ".xml.object");
						statListKdd.add(statskdd);
					} else if (dataSetType.equals(props.getAQUAINTdataset())) {
						System.out.println(DisambProperties.getInstance()
								.getaquaintObjectFolder()
								+ documentName
								+ ".xml.object");
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						statskdd = evaluator.KddEvaluator(DisambProperties
								.getInstance().getaquaintObjectFolder()
								+ documentName + ".xml.object");
						statListKdd.add(statskdd);
					}

					if (dataSetType.equals("kddo")) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);

						statsOriginal = evaluator
								.DisambiguationEvaluator(DisambProperties
										.getInstance()
										.getkddOriginalGroundObjectFile());
						statListOriginal.add(statsOriginal);
					} else if (dataSetType.equals("kddc")) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName2);

						statsOriginal = evaluator
								.DisambiguationEvaluator(DisambProperties
										.getInstance()
										.getkddCuratedObjectFolder()
										+ documentName + ".xml.object");
						statListOriginal.add(statsOriginal);
					} else if (dataSetType.equals("wikic")) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						System.out.println(DisambProperties.getInstance()
								.getwikiCuratedObjectFolder()
								+ documentName
								+ ".xml.object");
						statsOriginal = evaluator
								.DisambiguationEvaluator(DisambProperties
										.getInstance()
										.getwikiCuratedObjectFolder()
										+ documentName + ".xml.object");
						statListOriginal.add(statsOriginal);
					} else if (dataSetType.equals(props.getMSNBCdataset())) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						System.out.println(DisambProperties.getInstance()
								.getmsnbcObjectFolder()
								+ documentName
								+ ".xml.object");
						statsOriginal = evaluator
								.DisambiguationEvaluator(DisambProperties
										.getInstance().getmsnbcObjectFolder()
										+ documentName + ".xml.object");
						statListOriginal.add(statsOriginal);
					} else if (dataSetType.equals(props.getAQUAINTdataset())) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						System.out.println(DisambProperties.getInstance()
								.getaquaintObjectFolder()
								+ documentName
								+ ".xml.object");
						statsOriginal = evaluator
								.DisambiguationEvaluator(DisambProperties
										.getInstance().getaquaintObjectFolder()
										+ documentName + ".xml.object");
						statListOriginal.add(statsOriginal);
					}

					if (dataSetType.equals("kddo")) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);

						statsWithSpotter = evaluator
								.CompleteEvaluator(DisambProperties
										.getInstance()
										.getkddOriginalGroundObjectFile());
						statListOriginalWithSpotter.add(statsWithSpotter);
					} else if (dataSetType.equals("kddc")) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName2);

						statsWithSpotter = evaluator
								.CompleteEvaluator(DisambProperties
										.getInstance()
										.getkddCuratedObjectFolder()
										+ documentName + ".xml.object");
						statListOriginalWithSpotter.add(statsWithSpotter);
					} else if (dataSetType.equals("wikic")) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						System.out.println(DisambProperties.getInstance()
								.getwikiCuratedObjectFolder()
								+ documentName
								+ ".xml.object");
						statsWithSpotter = evaluator
								.CompleteEvaluator(DisambProperties
										.getInstance()
										.getwikiCuratedObjectFolder()
										+ documentName + ".xml.object");
						statListOriginalWithSpotter.add(statsWithSpotter);
					} else if (dataSetType.equals(props.getMSNBCdataset())) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						System.out.println(DisambProperties.getInstance()
								.getmsnbcObjectFolder()
								+ documentName
								+ ".xml.object");
						statsWithSpotter = evaluator
								.CompleteEvaluator(DisambProperties
										.getInstance().getmsnbcObjectFolder()
										+ documentName + ".xml.object");
						statListOriginalWithSpotter.add(statsWithSpotter);
					} else if (dataSetType.equals(props.getAQUAINTdataset())) {
						Evaluator evaluator = new Evaluator(lp.mention2entity,
								documentName);
						System.out.println(DisambProperties.getInstance()
								.getaquaintObjectFolder()
								+ documentName
								+ ".xml.object");
						statsWithSpotter = evaluator
								.CompleteEvaluator(DisambProperties
										.getInstance().getaquaintObjectFolder()
										+ documentName + ".xml.object");
						statListOriginalWithSpotter.add(statsWithSpotter);
					}
					// System.out.println(statsOriginal.getFalsePos() + " "
					// + statsOriginal.getTruePos() + " "
					// + statsOriginal.getPrecision());
					writetoAccuracyFile(accuracyFile, documentName, statskdd,
							statsOriginal, statsWithSpotter);

					// System.out.println("TruePos :: " + truePos
					// + "\nFalsePos :: " + falsePos + "\nTrueNeg :: "
					// + trueNeg + "\nFalseNeg :: " + falseNeg);
					//
					// float recall = (float) truePos
					// / (float) (truePos + falseNeg);
					// float precision = (float) truePos
					// / (float) (truePos + falsePos);
					//
					// writetoAccuracyFile(accuracyFile, docName, recall,
					// precision);
					//
					// globalTruePos += truePos;
					// globalFalseNeg += falseNeg;
					// globalTrueNeg += trueNeg;
					// globalFalsePos += falsePos;
				}
				System.out
						.println("-----------------------------------------Completed file "
								+ (++c)
								+ " Doc Name ::"
								+ docName
								+ "------------------------------------------------------");

			}

		}
		// float globalrecall = (float) globalTruePos
		// / (float) (globalTruePos + globalFalseNeg);
		// float globalprecision = (float) globalTruePos
		// / (float) (globalTruePos + globalFalsePos);
		// writetoAccuracyFile(accuracyFile, "global", globalrecall,
		// globalprecision);
		//
		// System.out.println("Global True Pos ::" + globalTruePos
		// + "\nGlobal False Pos :: " + globalFalsePos
		// + "\nGlobal True Neg :: " + globalTrueNeg
		// + "\nGlobal False Neg :: " + globalFalseNeg + "\nPrecision :: "
		// + globalprecision + "\nRecall :: " + globalrecall);

		writeGlobalAccuracy(accuracyFile, statListKdd, statListOriginal,
				statListOriginalWithSpotter);
	}

	public static void main(String args[]) {
		statListKdd = new ArrayList<Statistics>();
		statListOriginalWithSpotter = new ArrayList<Statistics>();
		statListOriginal = new ArrayList<Statistics>();

		String propsFile = args[1];
		try {
			DisambProperties.init(propsFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String trainPath = args[0];
		File trainFolder = new File(trainPath);

		String accuracyFile = args[2];

		dataSetType = args[3];

		// if (args.length == 6) {
		// double startValuew00 = Double.parseDouble(args[4]);
		// double endValuew11 = Double.parseDouble(args[5]);
		// double stepSize = Double.parseDouble(args[6]);
		// manualTuning(trainFolder, accuracyFile, startValuew00, endValuew11,
		// stepSize);
		//
		// } else

		if (args.length == 9) {
			boolean gc = Boolean.parseBoolean(args[4]);
			boolean lc = Boolean.parseBoolean(args[5]);
			double gu = Double.parseDouble(args[6]);
			double gl = Double.parseDouble(args[7]);
			int lu = Integer.parseInt(args[8]);
			inference(trainFolder, accuracyFile, gc, lc, gu, gl, lu);
		} else {
			inference(trainFolder, accuracyFile, Config.globalConstraints,
					Config.perMentionConstraints,
					Config.upperLimitGlobalEntities,
					Config.lowerLimitGlobalEntities,
					Config.upperLimitPerMentionEntities);
		}
	}
}
