package util;

import in.ac.iitb.cse.mrf.data.MRFGraph;
import in.ac.iitb.cse.mrf.data.WikiEdge;
import in.ac.iitb.cse.mrf.data.WikiNode;
import in.ac.iitb.cse.mrf.util.LearningProperties;
import in.ac.iitb.cse.mrf.util.WikiEdgeProvider;
import in.ac.iitb.cse.mrf.util.WikiNodeProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import org.jgrapht.WeightedGraph;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.xml.sax.SAXException;

import spotting.ComputeCategorySimilarity;
import spotting.Config;
import spotting.EdgeFeatureInfo;
import spotting.FeatureExtractor;
import spotting.NodePotentials;
import spotting.TrainingData;

public class GraphCreator {

	private static GraphCreator creator;
	private WeightedGraph<WikiNode, WikiEdge> mrfgraph;
	private long nodeCreationTime = 0l;
	private long edgeCreationTime = 0l;

	public static void main(String[] args) {
		String spotsFolder = args[0];
		String graphFolder = args[1];
		TrainingData data = null;
		MRFGraph g = null;
		GraphCreator gc = GraphCreator.getInstance();
		String filename = null;
		try {
			DisambProperties.init(args[2]);
			new LearningProperties(args[3]);
			for (File f : new File(spotsFolder).listFiles()) {
				filename = f.getName().indexOf('.') != -1 ? f.getName()
						.substring(0, f.getName().indexOf('.')) : f.getName();
				if (exists(graphFolder + filename))
					continue;
				data = gc.loadFromFile(f);
				g = new MRFGraph(gc.createGraph(data));
				g.normalizeFeatures(LearningProperties.getNodeDim(),
						LearningProperties.getEdgeDim());

				gc.serializeGraph(g.getGraph(), graphFolder + filename);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean exists(String name) {
		File file = new File(name + ".graph");
		return file.exists();
	}

	public TrainingData loadFromFile(File file) throws FileNotFoundException,
			IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		TrainingData data = (TrainingData) ois.readObject();
		ois.close();
		return data;
	}

	private void serializeGraph(WeightedGraph<WikiNode, WikiEdge> graph,
			String name) throws FileNotFoundException, IOException,
			TransformerConfigurationException, SAXException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				name + ".graph"));
		oos.writeObject(graph);
		oos.flush();
		oos.close();

		// Also export it to a text file in GraphML format
		VertexNameProvider<WikiNode> vertexProvider = new WikiNodeProvider();
		EdgeNameProvider<WikiEdge> edgeProvider = new WikiEdgeProvider();
		new GraphMLExporter<WikiNode, WikiEdge>(vertexProvider, vertexProvider,
				edgeProvider, edgeProvider).export(new FileWriter(name
				+ ".graphml"), graph);
	}

	public static GraphCreator getInstance() {
		if (null == creator)
			creator = new GraphCreator();
		return creator;
	}

	public void updateGraphEdges(WeightedGraph<WikiNode, WikiEdge> g,
			TrainingData trainData, List<Integer> edgeFeatureIndices) {
		NodePotentials np1 = null;
		NodePotentials np2 = null;
		for (WikiEdge e : g.edgeSet()) {
			np1 = findNodePotential(trainData, g.getEdgeSource(e));
			np2 = findNodePotential(trainData, g.getEdgeTarget(e));
			updateEdgeFeatures(e, np1, np2, edgeFeatureIndices);
		}
	}

	private void updateEdgeFeatures(WikiEdge e, NodePotentials np1,
			NodePotentials np2, List<Integer> edgeFeatureIndices) {
		double[] f = e.getfVector();
		for (int index : edgeFeatureIndices) {
			switch (index) {
			case 3:
				f[index] = FeatureExtractor.calcContextSim(
						np1.bagOfWords_frequent, np2.bagOfWords_frequent,
						np1.idf_frequent, np2.idf_frequent).similarity;
			case 4:
				f[index] = FeatureExtractor.calcContextSim(
						np1.bagOfWords_synopsis, np2.bagOfWords_synopsis,
						np1.idf_synopsis, np2.idf_synopsis).similarity;
			case 5:
				f[index] = FeatureExtractor.calcContextSim(
						np1.bagOfWords_synopsis_vbadj,
						np2.bagOfWords_synopsis_vbadj, np1.idf_synopsis_vbadj,
						np2.idf_synopsis_vbadj).similarity;
			}
		}
	}

	private NodePotentials findNodePotential(TrainingData trainData,
			WikiNode node) {
		NodePotentials foundNP = null;
		for (NodePotentials np : trainData.nodes.potentials_set)
			if (np.name.equalsIgnoreCase(node.getLabel())) {
				foundNP = np;
				break;
			}
		return foundNP;
	}

	public WeightedGraph<WikiNode, WikiEdge> createGraph(TrainingData trainData) {
		mrfgraph = new SimpleWeightedGraph<WikiNode, WikiEdge>(WikiEdge.class);
		nodeCreationTime = 0l;
		edgeCreationTime = 0l;
		NodePotentials np1 = null;
		NodePotentials np2 = null;
		WikiNode source = null;
		WikiNode target = null;
		WikiEdge edge = null;
		// int edgeCounter = 0;
		long start = 0l;
		long end = 0l;
		for (int j = 0; j < trainData.nodes.potentials_set.size(); j++) {
			np1 = trainData.nodes.potentials_set.get(j);
			start = System.currentTimeMillis();
			source = createNode(np1);
			source.incrNumOccur();
			end = System.currentTimeMillis();
			nodeCreationTime += (end - start);
			mrfgraph.addVertex(source);
			if (LearningProperties.getEdgeDim() > 0)
				for (int k = j + 1; k < trainData.nodes.potentials_set.size(); k++) {

					np2 = trainData.nodes.potentials_set.get(k);
					start = System.currentTimeMillis();
					target = createNode(np2);
					end = System.currentTimeMillis();
					nodeCreationTime += (end - start);
					mrfgraph.addVertex(target);
					if (source.equals(target))
						continue; // for self loop case
					// System.out.println("Adding edge " + edgeCounter++);

					// Filter to check if the nodes are within a chunk
					if (Config.filterChunk
							&& NodePotentials.distance(np1, np2) > Config.filterChunkThreshold)
						continue;
					start = System.currentTimeMillis();
					edge = mrfgraph.addEdge(source, target);
					end = System.currentTimeMillis();
					if (edge != null) { // if edge already exists
						edgeCreationTime += (end - start);
						double[] ef = createEdge(np1, np2);
						if (null == ef)
							mrfgraph.removeEdge(edge);
						else
							edge.setfVector(ef);
					}
				}
		}
		System.out.println("Graph has " + mrfgraph.vertexSet().size()
				+ " nodes and " + mrfgraph.edgeSet().size() + " edges");
		System.out.println("Node creation time - total : "
				+ (nodeCreationTime / 60000.0) + "Average : "
				+ (nodeCreationTime / (60000.0 * mrfgraph.vertexSet().size())));
		System.out.println("Edge creation time - total : "
				+ (edgeCreationTime / 60000.0) + "Average :"
				+ (edgeCreationTime / (60000.0 * mrfgraph.edgeSet().size())));
		return mrfgraph;
	}

	/*
	 * computes edge features between two nodes. If the filterInlink is true,
	 * then the edge features are constructed only if the inlink similarity
	 * score exceeds the filter threshold.
	 */
	public double[] createEdge(NodePotentials np1, NodePotentials np2) {
		double[] f = LearningProperties.getEdgeDim() > 0 ? new double[LearningProperties
				.getEdgeDim()] : null;
		if (null != f) {
			int index = 0;
			// Filter based on inlink similarity score
			// if (Config.filterInlink
			// && inlinkScore < Config.filterInlinkThreshold)
			// f = null;
			// else {
			if (LearningProperties.isCategory())
				f[index++] = ComputeCategorySimilarity.computeCategSim(
						np1.name.replaceAll(" ", "_"),
						np2.name.replaceAll(" ", "_"));
			if (LearningProperties.isOutlink()) {
				EdgeFeatureInfo ef1 = FeatureExtractor.calcOutlinkSim(
						np1.outLinks, np2.outLinks);
				f[index++] = ef1.similarity;
			}
			if (LearningProperties.isInlink()) {
				EdgeFeatureInfo ef = FeatureExtractor.calcInlinkSim(
						np1.inLinks, np2.inLinks);
				f[index++] = ef.similarity;
			}
			if (LearningProperties.isFrequent()) {
				EdgeFeatureInfo ef3 = FeatureExtractor.calcContextSim(
						np1.bagOfWords_frequent, np2.bagOfWords_frequent,
						np1.idf_frequent, np2.idf_frequent);
				f[index++] = ef3.similarity;
			}
			if (LearningProperties.isSynopsis()) {
				EdgeFeatureInfo ef4 = FeatureExtractor.calcContextSim(
						np1.bagOfWords_synopsis, np2.bagOfWords_synopsis,
						np1.idf_synopsis, np2.idf_synopsis);
				f[index++] = ef4.similarity;
			}
			if (LearningProperties.isSynvbadj()) {
				EdgeFeatureInfo ef5 = FeatureExtractor.calcContextSim(
						np1.bagOfWords_synopsis_vbadj,
						np2.bagOfWords_synopsis_vbadj, np1.idf_synopsis_vbadj,
						np2.idf_synopsis_vbadj);
				f[index++] = ef5.similarity;
			}
			if (LearningProperties.isFulltext())
				f[index++] = FeatureExtractor.calcCosine(np1.fullTextTerms,
						np1.fullTextTermsTf, np1.fullTextTermsIdf,
						np2.fullTextTerms, np2.fullTextTermsTf,
						np2.fullTextTermsIdf);
			// }
		}
		return f;
	}

	public WikiNode createNode(NodePotentials np) {
		WikiNode node = null;
		// check if there is already a node with the same label
		if (null != mrfgraph)
			for (WikiNode n : mrfgraph.vertexSet())
				if (n.getLabel().equalsIgnoreCase(np.name)) {
					node = n;
					break;
				}
		int index = 0;
		double[] f = null;
		// if not then create a new node
		if (null == node) {
			node = new WikiNode(np.name);
			node.setIncut(np.label == 1);
			node.setNodeInfo(node.new AdditionalNodeInfo(
					np.contextTermsForInput2TfIdfAnchorText,
					np.contextTermsForInput2TfIdfAnchorTextContext,
					np.contextTermsForInput2TfIdfFullText));
			f = new double[LearningProperties.getNodeDim()];
			node.setfVector(f);
			// if (LearningProperties.isNodeIntercept())
			f[index++] = 1; // Intercept
			if (LearningProperties.isNodeInlink()) 
				f[index++] = np.inlink_count;
			if (LearningProperties.isNodeOutlink())
				f[index++] = np.outlink_count;
			if (LearningProperties.isNodeRedirect())
				f[index++] = np.redirection;
			if (LearningProperties.isNodePagetitle())
				f[index++] = np.page_title_score;
			if (LearningProperties.isNodeAnchor())
				f[index++] = np.anchor_text_score;
			if (LearningProperties.isNodeSense())
				f[index++] = np.sense_probability;
			// } else {
			// index = 6;
			// node.getNodeInfo().unionAll(
			// np.contextTermsForInput2TfIdfAnchorText,
			// np.contextTermsForInput2TfIdfAnchorTextContext,
			// np.contextTermsForInput2TfIdfFullText);
			// f = node.getfVector();
			// }
			if (LearningProperties.isNodeAnchorcosine())
				f[index++] = FeatureExtractor.calcCosine(np.anchorTextTerms,
						np.anchorTextTermsTf, np.anchorTextTermsIdf,
						np.contextTermsForInput2TfIdfAnchorText);
			if (LearningProperties.isNodeAnchortextcosine())
				f[index++] = FeatureExtractor.calcCosine(
						np.anchorTextContextTerms, np.anchorTextContextTermsTf,
						np.anchorTextContextTermsIdf,
						np.contextTermsForInput2TfIdfAnchorTextContext);
			if (LearningProperties.isNodeFulltext())
				f[index++] = FeatureExtractor.calcCosine(np.fullTextTerms,
						np.fullTextTermsTf, np.fullTextTermsIdf,
						np.contextTermsForInput2TfIdfFullText);
		}
		return node;
	}

	private GraphCreator() {

	}

}
