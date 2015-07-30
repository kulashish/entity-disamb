package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.tartarus.snowball.ext.PorterStemmer;

import weka.core.Stopwords;

public class TrainDocument {

	public String TrainDir = "/home/kanika/workspace/WikiTraining/data/train/";
	public String GroundTruthDir = "/home/kanika/workspace/WikiTraining/data/ground/";
	private String filename;
	private ArrayList<String> keywords;
	private ArrayList<String> groundTruth;



	public TrainDocument(String file){
		filename = file;
		keywords = new ArrayList<String>();
		groundTruth = new ArrayList<String>();
		try {
			setKeywords(true);
			setGroundTruth();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void setKeywords(boolean stem) throws Exception{
		FileReader f = new FileReader(TrainDir+filename);
		String text = new String();
		BufferedReader br = null;
		try {
			String sCurrentLine;
			br = new BufferedReader(f);
			while ((sCurrentLine = br.readLine()) != null) {
				text+=sCurrentLine;
			}
			f.close();
		}catch (IOException e) {
			e.printStackTrace();
		} 
		PorterStemmer stemmer=new PorterStemmer();

		StringTokenizer str=new StringTokenizer(text);
		while(str.hasMoreTokens()){
			String token=str.nextToken();
			System.out.println("token  "+token);
			if(!Stopwords.isStopword(token)){
				if(stem){
					stemmer.setCurrent(token);
					stemmer.stem();
					keywords.add(stemmer.getCurrent().toLowerCase());
				}
				else{
					keywords.add(token);

				}
			}
		}

	}


	public void setGroundTruth() throws Exception{
		FileReader f = new FileReader(GroundTruthDir+filename);
		BufferedReader br = null;
		try {
			String sCurrentLine;
			br = new BufferedReader(f);
			while ((sCurrentLine = br.readLine()) != null) {
				groundTruth.add(sCurrentLine);
			}
			f.close();
		}catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public ArrayList<String> getKeywords(){
		return keywords;
	}
	
	public ArrayList<String> getGroundTruth(){
		return groundTruth;
	}
	
	public static void main(String[] args) {
		TrainDocument doc1 = new TrainDocument("doc1.txt");
		System.out.println("keywords   "+ doc1.getKeywords());
		System.out.println("ground Truth   "+ doc1.getGroundTruth());
		TrainDocument doc2 = new TrainDocument("doc2.txt");
		System.out.println("keywords   "+ doc2.getKeywords());
		System.out.println("ground Truth   "+ doc2.getGroundTruth());
	}

}
