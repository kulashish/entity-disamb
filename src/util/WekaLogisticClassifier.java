package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.functions.Logistic;
import weka.core.FastVector;
import weka.core.Instances;

public class WekaLogisticClassifier {


	public static void main(String[] args){

		BufferedReader reader,reader1;
		try {
			reader = new BufferedReader(
					new FileReader("/home/kanika/wikiTraining/nodePotentialLearning/arffFiles/appendAll20.arff"));
			Instances traindata = new Instances(reader);
			reader.close();
			traindata.setClassIndex(traindata.numAttributes() - 1);
			
			reader1 = new BufferedReader(
					new FileReader("/home/kanika/wikiTraining/nodePotentialLearning/arffFiles/KDDTestData_10MaxNodes_5Csize_80Context.arff"));
			
			 Instances testdata = new Instances(reader1);
			 testdata.setClassIndex(traindata.numAttributes() - 1);
			 // train classifier
			 Logistic cls = new Logistic();
			 cls.buildClassifier(traindata);
			 
			 // evaluate classifier and print some statistics
//			 Evaluation eval = new Evaluation(traindata);
//			 eval.evaluateModel(cls, testdata);
//		
//			 System.out.println(eval.toSummaryString());
//			 System.out.println(eval.recall(1)+" "+eval.precision(1));
			 
			 
			 EvaluationUtils eu = new EvaluationUtils();
//			 Classifier classifier = new weka.classifiers.functions.Logistic();
			 FastVector predictions = new FastVector();
			 ThresholdCurve tc = new ThresholdCurve();
			 for (int i = 0; i < 2; i++) { // Do two runs.
				 eu.setSeed(i);
				 predictions.appendElements(eu.getTestPredictions(cls, testdata));
			 }
			  Instances result = tc.getCurve(predictions,1);
			  System.out.println(result);
			 
			 FileWriter f = new FileWriter("/home/kanika/wikiTraining/temp.result");
			 f.write(result.toString());
			 f.close();
			// ThresholdCurve thresholdCurve = new ThresholdCurve();
			
			 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// setting class attribute
		
	}
}
