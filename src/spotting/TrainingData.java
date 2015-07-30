package spotting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import util.MorphologicalAnalyzer;

public class TrainingData implements Serializable {

	public KeywordsGroundTruth groundtruth; // data extracted from the training
											// files
	public NodePotentialsSet nodes; // node names and potentials
	public EdgePotentialsMatrix category_sim;
	public EdgePotentialsMatrix context_sim;
	public EdgePotentialsMatrix outlink_sim;
	public EdgePotentialsMatrix inlink_sim;

	public TrainingData() {
		nodes = new NodePotentialsSet();
	}

	public void printContentsToLog(String filename) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(filename, true)));
			out.println("### Printing TrainingData Dump ###");
			out.println("### Nodes ###");

			DecimalFormat df = new DecimalFormat("##0.000");

			for (int i = 0; i < nodes.potentials_set.size(); i++) {
				NodePotentials np = nodes.potentials_set.get(i);
				out.print(i + " " + np.name + " " + np.mention + " "
						+ df.format(np.logistic_score));
				out.println(" " + df.format(np.inlink_count) + " "
						+ df.format(np.outlink_count) + " "
						+ df.format(np.context_score_frequent));
			}

			if (inlink_sim != null && inlink_sim.matrix != null) {
				out.println("### InlinkSim Matrix ###");
				for (int i = 0; i < inlink_sim.size; i++) {
					for (int j = 0; j < inlink_sim.size; j++) {
						out.print(df.format(inlink_sim.matrix.get(i, j)) + " ");
					}
					out.println();
				}
			}

			if (outlink_sim != null && outlink_sim.matrix != null) {
				out.println("### OutlinkSim Matrix ###");
				for (int i = 0; i < outlink_sim.size; i++) {
					for (int j = 0; j < outlink_sim.size; j++) {
						out.print(df.format(outlink_sim.matrix.get(i, j)) + " ");
					}
					out.println();
				}
			}

			if (category_sim != null && category_sim.matrix != null) {
				out.println("### CategorySim Matrix ###");
				for (int i = 0; i < category_sim.size; i++) {
					for (int j = 0; j < category_sim.size; j++) {
						out.print(df.format(category_sim.matrix.get(i, j))
								+ " ");
					}
					out.println();
				}
			}

			if (context_sim != null && context_sim.matrix != null) {
				out.println("### ContextSim Matrix ###");
				for (int i = 0; i < context_sim.size; i++) {
					for (int j = 0; j < context_sim.size; j++) {
						out.print(df.format(context_sim.matrix.get(i, j)) + " ");
					}
					out.println();
				}
			}

			out.println("### End of TrainingData Dump ###");
			out.close();
		} catch (IOException e) {
			System.out.println("Error: cannot print to " + filename);
		}

	}

	public TrainingData removeNoise(FeatureExtractor fe) {

		int maxLength = 4;
		TrainingData td = new TrainingData();

		ArrayList<String> tokens = new ArrayList<String>();
		String text = this.groundtruth.getDocumentText();
		StringTokenizer str = new StringTokenizer(text);
		while (str.hasMoreTokens()) {
			String token = str.nextToken();
			if (token == null || "".equals(token) || " ".equals(token))
				continue;
			tokens.add(token.replaceAll("[^a-z\\sA-Z/\\-]", ""));
		}
		ArrayList<String> mentionList = this.groundtruth.getMentionNames();
		int curr_offset = 0;

		for (int i = 0; i < tokens.size();) {

			// System.out.println(" i = "+i);

			if (!mentionList.contains(tokens.get(i))) {
				i++;
				continue;
			}

			curr_offset = text.indexOf(tokens.get(i), curr_offset);

			ArrayList<Integer> allOffset = new ArrayList<Integer>();
			ArrayList<String> allWords = new ArrayList<String>();
			allWords.addAll(tokens.subList(i,
					Math.min(tokens.size(), i + maxLength + 1)));

			// for(int i1=0;i1<allWords.size();i1++){
			// allWords.set(i,allWords.get(i).toLowerCase());
			// }

			allOffset.add(curr_offset);
			int k;

			for (k = i + 1; k <= Math.min(tokens.size() - 1, i + maxLength); k++) {
				allOffset.add(text.indexOf(tokens.get(k), curr_offset));
			}

			// int max_offset =
			// allOffset.get(k-i-1)+allWords.get(k-i-1).length();

			ArrayList<NodePotentials> union = new ArrayList<NodePotentials>();

			for (int j = 0; j < this.nodes.potentials_set.size(); j++) {
				String[] arr = this.nodes.potentials_set.get(j).mention
						.split("_");
				if (allWords.contains(arr[0])) {
					union.add(this.nodes.potentials_set.get(j));
				}
			}
			// System.out.println(union.size());
			ArrayList<NodePotentials> union_copy = new ArrayList<NodePotentials>();
			union_copy.addAll(union);
			int l;
			boolean flag = false;
			int finalIndex = 0;
			for (l = 0; l <= allWords.size() - 1; l++) {
				if (!flag) {
					String mentiontext = "";
					for (int h = 0; h < allWords.size() - l; h++) {
						if (h != allWords.size() - l - 1)
							mentiontext += allWords.get(h) + " ";
						else
							mentiontext += allWords.get(h);
					}
					// File redirec = new File ("data/wikipedia_redirect.ser");
					// WikipediaRedirect wr =
					// IOUtil.loadWikipediaRedirect(redirec);
					// wr.get("owner");
					// if()
					// if(fe.luceneIndex.getfromSynonym(mentiontext,allOffset.get(0),10)!=null){
					// union_copy.addAll(fe.luceneIndex.getfromSynonym(mentiontext,allOffset.get(0),10).potentials_set);
					// union.addAll(fe.luceneIndex.getfromSynonym(mentiontext,allOffset.get(0),10).potentials_set);
					// }

					for (Iterator<NodePotentials> itr = union.iterator(); itr
							.hasNext();) {
						NodePotentials np = itr.next();
						String wikiName = np.name.replaceAll(
								"[^a-z\\sA-Z/\\-]", "").toLowerCase();
						if (wikiName
								.equalsIgnoreCase(mentiontext.toLowerCase())
								|| np.mention.split("_")[0]
										.equalsIgnoreCase(mentiontext)) {
							finalIndex = allWords.size() - l - 1;
							flag = true;
							break;
						} else {
							for (String morphs : MorphologicalAnalyzer
									.analyze(mentiontext)) {
								if ((wikiName.equalsIgnoreCase(morphs))) {
									finalIndex = allWords.size() - l - 1;
									flag = true;
									break;
								}
							}
							if (flag)
								break;
						}
					}
				}
			}

			i = i + finalIndex + 1;
			String mentiontext = "";
			if (finalIndex > 0) {
				for (int h = 0; h < finalIndex + 1; h++) {
					if (h != finalIndex)
						mentiontext += allWords.get(h).toLowerCase() + " ";
					else
						mentiontext += allWords.get(h).toLowerCase();
				}

				for (Iterator<NodePotentials> itr = union_copy.iterator(); itr
						.hasNext();) {
					NodePotentials np = itr.next();
					String wikiName = np.name
							.replaceAll("[^a-z\\sA-Z/\\-]", "").toLowerCase();
					if (!(wikiName.indexOf(mentiontext.toLowerCase()) != -1 || np.mention
							.split("_")[0].equalsIgnoreCase(mentiontext))) {
						int delete = 1;
						for (String morphs : MorphologicalAnalyzer
								.analyze(mentiontext)) {
							if ((wikiName.equalsIgnoreCase(morphs))) {
								delete = 0;
							}
						}
						if (delete == 1)
							itr.remove();

					}

				}

				for (Iterator<NodePotentials> itr = union_copy.iterator(); itr
						.hasNext();) {
					NodePotentials np = itr.next();
					np.offset = allOffset.get(0);
					np.length = allOffset.get(finalIndex) - allOffset.get(0)
							+ allWords.get(finalIndex).length();
					for (int l2 = 0; l2 <= finalIndex; l2++) {
						if (l2 == 0)
							np.mention = allWords.get(l2);
						else
							np.mention += " " + allWords.get(l2);
					}
					np.mention += "_" + np.offset;
				}

				for (int g = 0; g < union_copy.size(); g++) {
					NodePotentials np = union_copy.get(g);
					int f = 0;
					for (Iterator<NodePotentials> itr = td.nodes.potentials_set
							.iterator(); itr.hasNext();) {
						NodePotentials tdnode = itr.next();
						if (tdnode.name.equalsIgnoreCase(np.name)
								&& tdnode.mention.equalsIgnoreCase(np.mention)) {
							f = 1;
						}
					}
					if (f == 0) {
						// System.out.println(np.mention +" "+np.name);
						td.nodes.potentials_set.add(union_copy.get(g));
					}
				}

				// System.out.println(union_copy.size());

			} else {

				// td.nodes.potentials_set.addAll(fe.luceneIndex.getfromSynonym(allWords.get(0),20,allOffset.get(0)).potentials_set);
				for (int j = 0; j < this.nodes.potentials_set.size(); j++) {
					String[] arr = this.nodes.potentials_set.get(j).mention
							.split("_");
					if (arr.length > 1) {
						if (arr[0].replaceAll("[^a-z\\sA-Z/\\-]", "")
								.equalsIgnoreCase(allWords.get(0))
								&& Integer.parseInt(arr[1]) == allOffset.get(0)) {
							// System.out.println("nodes being added "+arr[0]+" "+arr[1]);
							td.nodes.potentials_set
									.add(this.nodes.potentials_set.get(j));
						}
					}

				}

				// if(fe.luceneIndex.getfromSynonym(mentiontext,allOffset.get(0),10)!=null){
				// for(NodePotentials n
				// :fe.luceneIndex.getfromSynonym(allWords.get(0),allOffset.get(0),10).potentials_set){
				// if(n.mention.split("_")[0].equalsIgnoreCase(allWords.get(0))){
				// n.mention=allWords.get(0)+"_"+allOffset.get(0);
				// td.nodes.potentials_set.add(n);
				// }
				// }
				// }
			}

		}

		return td;
	}


}
