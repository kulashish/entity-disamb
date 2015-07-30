package Evaluation;

import java.util.ArrayList;

public class Statistics {

	private int truePos;
	private int falsePos;
	private int trueNeg;
	private int falseNeg;
	private int spotterError;
	private int totalNA;
	private int trueNA;

	void statistics() {
		trueNeg = 0;
		truePos = 0;
		falseNeg = 0;
		falsePos = 0;
		spotterError = 0;
		totalNA = 0;
		trueNA = 0;
	}

	public void incrementTotalNA() {
		totalNA++;
	}

	public void incrementTrueNA() {
		trueNA++;
	}

	public void incrementTruePos() {
		truePos += 1;
	}

	public void incrementTrueNeg() {
		trueNeg += 1;
	}

	public void incrementFalsePos() {
		falsePos += 1;
	}

	public void incrementFalseNeg() {
		falseNeg += 1;
	}

	public void incrementSpotterError() {
		spotterError += 1;
	}

	public void setTruePos(int val) {
		truePos = val;
	}

	public void setTrueNeg(int val) {
		trueNeg = val;
	}

	public void setFalsePos(int val) {
		falsePos = val;
	}

	public void setFalseNeg(int val) {
		falseNeg = val;
	}

	public void setSpotterError(int val) {
		spotterError = val;
	}

	public int getTruePos() {
		return truePos;
	}

	public int getTrueNeg() {
		return trueNeg;
	}

	public int getFalsePos() {
		return falsePos;
	}

	public int getFalseNeg() {
		return falseNeg;
	}

	public int getSpotterError() {
		return spotterError;
	}

	public int getTotalNA() {
		return totalNA;
	}

	public int getTrueNA() {
		return trueNA;
	}

	public double getNAPrecision() {
		double p = 1.0d;
		if (totalNA != 0)
			p = trueNA * 1.0 / totalNA;
		return p;
	}

	public double getRecall() {
		if (truePos == 0 && falseNeg == 0) {
			return 0d;
		}
		double recall = (double) truePos / (double) (truePos + falseNeg);
		return recall;
	}

	public double getPrecision() {

		if (truePos == 0 && falsePos == 0) {
			return 0d;
		}
		double precision = (double) truePos / (double) (truePos + falsePos);
		return precision;
	}

	public double getFmeasure() {

		if (getPrecision() == 0d && getRecall() == 0d) {
			return 0d;
		}

		double fmeasure = 2 * getRecall() * getPrecision()
				/ (getPrecision() + getRecall());
		return fmeasure;
	}

	public static double getMicroFmeasure(ArrayList<Statistics> statList) {
		double microRec = getMicroRecall(statList);
		double microPrec = getMicroPrecision(statList);

		if (microRec == 0d && microPrec == 0d) {
			return 0d;
		}
		double fmeasure = 2 * microRec * microPrec / (microPrec + microRec);
		return fmeasure;
	}

	public static double getMicroPrecision(ArrayList<Statistics> statList) {
		double microPrec;
		double sumofTP = 0d;
		double sumofFP = 0d;
		for (int i = 0; i < statList.size(); i++) {
			sumofFP += statList.get(i).getFalsePos();
			sumofTP += statList.get(i).getTruePos();
		}
		if (sumofFP == 0d && sumofTP == 0d) {
			return 0d;
		}
		microPrec = (sumofTP) / (sumofFP + sumofTP);
		return microPrec;
	}

	public static double getMicroRecall(ArrayList<Statistics> statList) {
		double microRec;
		double sumofTP = 0d;
		double sumofFN = 0d;
		for (int i = 0; i < statList.size(); i++) {
			sumofFN += statList.get(i).getFalseNeg();
			sumofTP += statList.get(i).getTruePos();
		}
		if (sumofFN == 0d && sumofTP == 0d) {
			return 0d;
		}
		microRec = (sumofTP) / (sumofFN + sumofTP);
		return microRec;
	}

	public static double getMacroFmeasure(ArrayList<Statistics> statList) {
		double microRec = getMacroRecall(statList);
		double microPrec = getMacroPrecision(statList);
		if (microPrec == 0d && microRec == 0d) {
			return 0d;
		}
		double fmeasure = 2 * microRec * microPrec / (microPrec + microRec);
		return fmeasure;
	}

	public static int getTotalSpotterError(ArrayList<Statistics> statList) {
		int total = 0;
		for (int i = 0; i < statList.size(); i++) {
			total += statList.get(i).getSpotterError();
		}
		return total;
	}

	public static double getMacroPrecision(ArrayList<Statistics> statList) {
		double macroPrec;
		double sumofPrec = 0d;
		double totalNumber = statList.size();
		for (int i = 0; i < statList.size(); i++) {
			sumofPrec += statList.get(i).getPrecision();
		}
		if (totalNumber == 0) {
			return 0d;
		}
		macroPrec = sumofPrec / totalNumber;
		return macroPrec;
	}

	public static double getMacroRecall(ArrayList<Statistics> statList) {
		double macroRec;
		double sumofRec = 0d;
		double totalNumber = statList.size();
		for (int i = 0; i < statList.size(); i++) {
			sumofRec += statList.get(i).getRecall();
		}
		if (totalNumber == 0) {
			return 0d;
		}
		macroRec = sumofRec / totalNumber;
		return macroRec;
	}

}
