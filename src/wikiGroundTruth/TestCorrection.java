package wikiGroundTruth;

import java.util.ArrayList;
import java.util.HashMap;

import spotting.NodePotentials;

public class TestCorrection {

	public static void main(String[] args) {
		HashMap<String, HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>> OBJ = (HashMap<String, HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>>>) Serializer
				.decode("/home/kanika/wikiTraining/Correction");

		System.out.println(OBJ.keySet());
		HashMap<ArrayList<NodePotentials>, ArrayList<NodePotentials>> changes = OBJ
				.get("Activision");

		System.out.println(changes.keySet());
		for (ArrayList<NodePotentials> npKey : changes.keySet()) {
			ArrayList<NodePotentials> newList = changes.get(npKey);
			System.out
					.println("-----------------------------------Original-----------------------------------");
			for (int i = 0; i < npKey.size(); i++) {
				System.out.println(npKey.get(i).mention + " "
						+ npKey.get(i).name);
			}
			System.out
					.println("-----------------------------------Changed--------------------------------------");
			for (int i = 0; i < newList.size(); i++) {
				System.out.println(newList.get(i).mention + " "
						+ newList.get(i).name);
			}
		}

	}

}
