package Evaluation;

import java.io.Serializable;

public class Entity implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 222L;
	public String name;
	public int trueLabel;
	public int predictedLabel;
	public double[] nodeFeatures;
}
