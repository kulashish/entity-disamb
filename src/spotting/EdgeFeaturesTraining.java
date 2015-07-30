package spotting;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EdgeFeaturesTraining {

	static String trainDir = "data/KDDTrainingData/";
	static String groundFilename = "data/KddGroundTruth.xml"; // add filename here!
	static String poscsvFile = "data/posEdgePotentialMilneSimFinal.csv";
	static final String CSV_SEPARATOR = ",";
	static int maxNodes=25; // hardcoded max number of entities to be extracted from lucene
	static int consolidationSize = 5;
	static int csize=20;
	
	
	public static void main(String[] args) {
		ArrayList<TrainingData> trainDataList = new ArrayList<TrainingData>();		
		try {

			ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
			List<KeywordsGroundTruth> kws = kw_extractor.extractDirectorywithGroundTruth(trainDir, groundFilename,csize);
			FeatureExtractor ft_extractor = new FeatureExtractor();
			for (int k=0;k<kws.size();k++) {
				KeywordsGroundTruth kw = kws.get(k);
				long start = System.currentTimeMillis();
				TrainingData temp = ft_extractor.extractFeatures(kw, maxNodes,consolidationSize);
				trainDataList.add(temp);
				long elapsedTimeMillis = System.currentTimeMillis()-start;
				float elapsedTimeMin = elapsedTimeMillis/(60*1000F);
				System.out.println("For file "+kw.filename+" process completed in  "+elapsedTimeMin +" mins");		
				System.out.println("finished "+(k+1)+" files");
			}
			HashMap<Integer,ArrayList<NodePotentials>> posDocList = new HashMap<Integer,ArrayList<NodePotentials>>();
			HashMap<Integer,ArrayList<NodePotentials>> negDocList = new HashMap<Integer,ArrayList<NodePotentials>>();
			
			for(int i=0;i<trainDataList.size();i++){
				ArrayList<NodePotentials> postemp = new ArrayList<NodePotentials>();
				ArrayList<NodePotentials> negtemp = new ArrayList<NodePotentials>();	
				for(int l=0;l<trainDataList.get(i).nodes.potentials_set.size();l++){
					//System.out.println(trainDataList.get(i).nodes.potentials_set.get(l).name+"   "+trainDataList.get(i).nodes.potentials_set.get(l).inLinks.size()+"   "+trainDataList.get(i).nodes.potentials_set.get(l).bagOfWords.size()+"   "+trainDataList.get(i).nodes.potentials_set.get(l).outLinks.size());
					//System.out.println(trainDataList.get(i).nodes.potentials_set.get(l).name+"  "+trainDataList.get(i).nodes.potentials_set.get(l).label);
					if(trainDataList.get(i).nodes.potentials_set.get(l).label==1){
						postemp.add(trainDataList.get(i).nodes.potentials_set.get(l));
					}
					else{
						negtemp.add(trainDataList.get(i).nodes.potentials_set.get(l));
					}
				}
				posDocList.put(i,postemp);
				negDocList.put(i, negtemp);
			}
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(poscsvFile), "UTF-8"));
			
			for(int i=0;i<trainDataList.size();i++){
				System.out.println("started writing "+i+" file");
				for(int j=0;j<posDocList.get(i).size();j++){
					int num1 = (int)(Math.random()*10);
					if(num1 > 4){
					for(int k=j+1;k<posDocList.get(i).size();k++){
						int num = (int)(Math.random()*10);
						if(num > 5){
						StringBuffer oneLine = new StringBuffer();
						oneLine.append(posDocList.get(i).get(j).name);
	                    oneLine.append(CSV_SEPARATOR);
	                    oneLine.append(posDocList.get(i).get(k).name);
	                    oneLine.append(CSV_SEPARATOR);
	                    oneLine.append(posDocList.get(i).get(j).label);
	                    oneLine.append(CSV_SEPARATOR);
	                    oneLine.append(posDocList.get(i).get(k).label);
	                    oneLine.append(CSV_SEPARATOR);
	                    
	                    oneLine.append(ComputeCategorySimilarity.computeCategSim(
	                    		posDocList.get(i).get(j).name.replaceAll(" ", "_"), 
	                    		posDocList.get(i).get(k).name.replaceAll(" ", "_")));
	                    oneLine.append(CSV_SEPARATOR);
	                    
	                    
	                    oneLine.append(FeatureExtractor.calcOutlinkSim(
	                    		posDocList.get(i).get(j).outLinks, 
	                    		posDocList.get(i).get(k).outLinks));
	                    oneLine.append(CSV_SEPARATOR);
	                    
	                    oneLine.append(FeatureExtractor.calcInlinkSim(
	                    		posDocList.get(i).get(j).inLinks, 
	                    		posDocList.get(i).get(k).inLinks));
	                    oneLine.append(CSV_SEPARATOR);
	                    
	                    
	                    oneLine.append(FeatureExtractor.calcContextSim(
	                    		posDocList.get(i).get(j).bagOfWords_synopsis, 
	                    		posDocList.get(i).get(k).bagOfWords_synopsis,
	                    		posDocList.get(i).get(j).idf_synopsis,
	                    		posDocList.get(i).get(k).idf_synopsis));
	                 
						bw.write(oneLine.toString());
	                    bw.newLine();
						}
					}
				}
				System.out.println("finished writing "+i+" file");
			}
			}
			bw.flush();
            bw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
