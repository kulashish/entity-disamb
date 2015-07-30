package spotting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import util.DisambProperties;
import util.GraphCreator;
import util.ParseXML;
import util.XMLTagInfo;

public class SpotsUpdater {
	private static final Logger logger = Logger.getLogger("spotting");
	private TrainingData data;
	private String groundXMLFile;

	public SpotsUpdater(TrainingData data, String xmlFile) {
		this.data = data;
		this.groundXMLFile = xmlFile;
	}

	public void update() throws Exception {
		Map<String, ArrayList<XMLTagInfo>> xmlMap = null;
		xmlMap = new ParseXML().parseXML(groundXMLFile);
		ArrayList<XMLTagInfo> mentions = null;
		for (String key : xmlMap.keySet()) {
			mentions = xmlMap.get(key);
			if (mentions.size() > 10) {
				update(mentions);
				break;
			}

		}
	}

	private void update(ArrayList<XMLTagInfo> mentions) {
		String mention = null;
		for (XMLTagInfo tagInfo : mentions) {
			if (null == tagInfo.wikiEntity
					|| tagInfo.wikiEntity.equalsIgnoreCase("na")) {
				// System.out.println("checking for " + tagInfo.mention);
				for (NodePotentials np : data.nodes.potentials_set) {
					mention = np.mention.replaceAll("_[0-9]+", "");
					// System.out.print(mention+", ");
					if (mention.equalsIgnoreCase(tagInfo.mention)) {
						// System.out.println("found NA : " + tagInfo.mention);
						np.blnNA = true;
					}
				}
				// System.out.println();
			}
		}
	}

	private void serializeSpots(String name) throws FileNotFoundException,
			IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				name + ".spots"));
		oos.writeObject(data);
		oos.flush();
		oos.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String spotsFolder = args[0];
		String xmlFolder = args[1];

		TrainingData data = null;
		SpotsUpdater updater = null;
		try {
			DisambProperties.init(args[2]);
			for (File spotsFile : new File(spotsFolder).listFiles()) {
				logger.log(Level.INFO, "loading " + spotsFile.getName());
				data = GraphCreator.getInstance().loadFromFile(spotsFile);
				updater = new SpotsUpdater(data, xmlFolder
						+ spotsFile.getName().replace("spots", "xml"));
				updater.update();
				updater.serializeSpots(spotsFile.getAbsolutePath());
				logger.log(Level.INFO,
						"Completed processing " + spotsFile.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
