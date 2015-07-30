package spotting;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import util.DisambProperties;

import in.ac.iitb.cse.mrf.data.AMNWeights;
import exception.TrainingException;

public class CollectiveTrainingTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			DisambProperties.init(args[0]);
			AMNWeights weights = CollectiveTraining.train(args[1]);
			weights.log(Level.INFO);
		} catch (TrainingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
