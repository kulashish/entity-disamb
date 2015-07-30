package wikiGroundTruth.server;

import java.io.Serializable;

import org.wikipedia.miner.model.Label;

@SuppressWarnings("serial")
public class LabelSense implements Serializable {
	public String[] wikiMinerCandidate;
	public double[] wikiMinerProbability;
	
	public LabelSense() {}

	public LabelSense(Label.Sense[] label) {

		wikiMinerCandidate = new String[label.length];
		wikiMinerProbability = new double[label.length];
		// System.out.println("in label sense  " + label.length + "  ");
		for (int i = 0; i < label.length; i++) {
			wikiMinerCandidate[i] = label[i].getTitle();
			wikiMinerProbability[i] = label[i].getPriorProbability();
		}

	}

}
