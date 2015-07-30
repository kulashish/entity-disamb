package util;

import in.ac.iitb.cse.mrf.util.LearningProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import spotting.CollectiveTraining;

public class GraphUpdater {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new LearningProperties(args[0]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String trainTextPath = LearningProperties.getTrainingTextPath();
		String trainGraphPath = LearningProperties.getTrainingPath();
		String trainxmlPath = LearningProperties.getTrainingXMLPath();
		String trainxmlwikiPath = LearningProperties.getTrainingXMLWikiPath();

		for (File gFile : new File(trainGraphPath).listFiles()) {
			CollectiveTraining.graph(trainTextPath, new File(trainTextPath
					+ gFile.getName()), trainGraphPath, trainxmlPath,
					trainxmlwikiPath);
		}

	}

}
