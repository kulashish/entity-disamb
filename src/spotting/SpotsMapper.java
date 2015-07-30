package spotting;

import in.ac.iitb.cse.mrf.data.WikiEdge;
import in.ac.iitb.cse.mrf.data.WikiNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapht.WeightedGraph;

import util.DisambProperties;
import util.GraphCreator;
import Evaluation.Entity;
import Evaluation.Mention;

public class SpotsMapper {
	private static final Logger logger = Logger.getLogger("spotting");
	private TrainingData data;
	private WeightedGraph<WikiNode, WikiEdge> graph;
	private Map<Mention, List<Entity>> dataMap;

	public SpotsMapper(TrainingData data,
			WeightedGraph<WikiNode, WikiEdge> graph) {
		this.data = data;
		dataMap = new HashMap<Mention, List<Entity>>();
		this.graph = graph;
	}

	private void update() {
		String mentionName = null;
		List<Entity> entities = null;
		Entity entity = null;
		Mention mention = null;
		for (NodePotentials np : data.nodes.potentials_set) {
//			System.out.println(np.interval.left);
			mention = new Mention();
			mentionName = np.mention.replaceAll("_[0-9]+", "");
			mention.name = mentionName;
			mention.offset = np.interval.left;
			mention.isNA = np.blnNA;
			entity = new Entity();
			entity.name = np.name;
			entity.trueLabel = np.label;
			updatePred(entity);
			entities = dataMap.get(mention);
			if (null == entities) {
				entities = new ArrayList<Entity>();
				dataMap.put(mention, entities);
			}
			entities.add(entity);
		}
	}

	private void updatePred(Entity entity) {
		for (WikiNode n : graph.vertexSet())
			if (entity.name.equalsIgnoreCase(n.getLabel()))
				entity.predictedLabel = n.isPredcut() ? 1 : 0;
	}

	private void serializeSpots(String name) throws FileNotFoundException,
			IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				name));
		oos.writeObject(dataMap);
		oos.flush();
		oos.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 String spotsFolder = args[0];
//		String spotsFolder = "/Users/ashish/wiki/orig_vs_cur_analysis/spot_test/";
		 String graphFolder = args[1];
//		String graphFolder = "/Users/ashish/wiki/orig_vs_cur_analysis/outgraph_test/";
		 String targetFolder = args[2];
//		String targetFolder = "/Users/ashish/wiki/orig_vs_cur_analysis/maps_out/";
		TrainingData data = null;
		SpotsMapper mapper = null;
		try {
			 DisambProperties.init(args[3]);
//			DisambProperties.init("/Users/ashish/wiki/orig_vs_cur_analysis/disamb.properties");
			for (File spotsFile : new File(spotsFolder).listFiles()) {
				logger.log(Level.INFO, "loading " + spotsFile.getName());
				data = GraphCreator.getInstance().loadFromFile(spotsFile);
				String name = spotsFile.getName().substring(0,
						spotsFile.getName().indexOf('.'));
				ObjectInputStream ois = null;
				WeightedGraph<WikiNode, WikiEdge> graph = null;
				try {
					ois = new ObjectInputStream(new FileInputStream(graphFolder
							+ name + ".graph.outgraph"));
					graph = (WeightedGraph<WikiNode, WikiEdge>) ois
							.readObject();
				} catch (FileNotFoundException e) {
					logger.log(Level.INFO, e.getMessage());
				} catch (IOException e) {
					logger.log(Level.INFO, e.getMessage());
				} catch (ClassNotFoundException e) {
					logger.log(Level.INFO, e.getMessage());
				}
				if (null != graph) {
					mapper = new SpotsMapper(data, graph);
					mapper.update();

					mapper.serializeSpots(targetFolder + name + ".map");
					logger.log(Level.INFO,
							"Completed processing " + spotsFile.getName());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
