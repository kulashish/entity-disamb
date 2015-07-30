package disamb;

//import com.mathworks.toolbox.javabuilder.MWNumericArray;
//import amn.Amn;
//
//import spotting.*;
//
//import java.util.HashMap;

public class AMNInference {
	// double[][] nodeFeatureWeights;
	// double[][] nodeFeatureValues;
	// double[][] edgeFeatureWeights;
	// double[][] edgeFeatureValues;
	// double[][] edges;
	//    
	// public void loadEntityData(TrainingData data) throws Exception {
	// int num_nodes = data.nodes.potentials_set.size();
	// nodeFeatureWeights = new double[1][2];
	// nodeFeatureWeights[0][0] = 1.0;
	// nodeFeatureWeights[0][1] = 0.0;
	//        
	// nodeFeatureValues = new double[1][num_nodes];
	// int curr = 0;
	// for (NodePotentials np: data.nodes.potentials_set){
	// nodeFeatureValues[0][curr] = np.logistic_score;
	// curr++;
	// }
	//        
	// int num_edge_features = 0;
	// if (Config.useInlinkSim) num_edge_features++;
	// if (Config.useOutlinkSim) num_edge_features++;
	// if (Config.useCategSim) num_edge_features++;
	// if (Config.useContextSim) num_edge_features++;
	//        
	// edgeFeatureWeights = new double[num_edge_features][2];
	// curr = 0;
	// if (Config.useInlinkSim) {
	// edgeFeatureWeights[curr][0] = Config.inlinkWt;
	// edgeFeatureWeights[curr][1] = Config.inlinkWt;
	// curr++;
	// }
	// if (Config.useOutlinkSim) {
	// edgeFeatureWeights[curr][0] = Config.outlinkWt;
	// edgeFeatureWeights[curr][1] = Config.outlinkWt;
	// curr++;
	// }
	// if (Config.useCategSim) {
	// edgeFeatureWeights[curr][0] = Config.categWt;
	// edgeFeatureWeights[curr][1] = Config.categWt;
	// curr++;
	// }
	// if (Config.useContextSim) {
	// edgeFeatureWeights[curr][0] = Config.contextWt;
	// edgeFeatureWeights[curr][1] = Config.contextWt;
	// curr++;
	// }
	//                
	// int num_edges = (num_nodes * (num_nodes-1))/2; //complete graph
	//        
	// edgeFeatureValues = new double[num_edge_features][num_edges];
	// edges = new double[num_edges][2];
	//        
	// curr = 0;
	// for (int i = 0; i < data.nodes.potentials_set.size(); i++){
	// for (int j = i+1; j < data.nodes.potentials_set.size(); j++){
	// edges[curr][0] = i+1;
	// edges[curr][1] = j+1;
	// int curr1 = 0;
	// if (Config.useInlinkSim) {
	// edgeFeatureValues[curr1][curr] = data.inlink_sim.matrix.get(i,j);
	// curr1++;
	// }
	// if (Config.useOutlinkSim) {
	// edgeFeatureValues[curr1][curr] = data.outlink_sim.matrix.get(i,j);
	// curr1++;
	// }
	// if (Config.useCategSim) {
	// edgeFeatureValues[curr1][curr] = data.category_sim.matrix.get(i,j);
	// curr1++;
	// }
	// if (Config.useContextSim) {
	// edgeFeatureValues[curr1][curr] = data.context_sim.matrix.get(i,j);
	// curr1++;
	// }
	// curr++;
	// }
	// }
	//        
	// }
	//    
	// public int[] performInference() throws Exception {
	// Amn amn = new Amn();
	// Object[] val = amn.amn_inference(4, new Object[]{nodeFeatureWeights,
	// nodeFeatureValues, edgeFeatureWeights, edgeFeatureValues, edges});
	//        
	// // Read labels 'y' values.
	// // It is column vector with each entry y_i_k, for all nodes i an all
	// labels k.
	// int numNodes = nodeFeatureValues[0].length;
	// int[] y = new int[numNodes];
	// double[] labels = ((MWNumericArray) val[0]).getDoubleData();
	// for(int i = 0; i < numNodes; i++)
	// {
	// y[i] = (int) Math.round(labels[i*2 + 1]);
	// }
	//        
	// /*System.out.println("Labels:");
	// for(int i = 0; i < numNodes; i++)
	// System.out.println(y[i]);*/
	//        
	// return y;
	// }
	//    
	// public HashMap<String,Double> runAMNInference(TrainingData data){
	// HashMap<String, Double> output = new HashMap<String,Double>();
	// try {
	// loadEntityData(data);
	// int[] y = performInference();
	// for (int i = 0; i < data.nodes.potentials_set.size(); i++){
	// output.put(data.nodes.potentials_set.get(i).name, (double) y[i]);
	// }
	// } catch (Exception e){
	// e.printStackTrace();
	// }
	//        
	// return output;
	// }
	//
	// public static void main(String[] args) throws Exception
	// {
	// AMNInference amn_i = new AMNInference();
	//        
	// // Node weights for 2 labels (0 & 1). For 2 node features (f1 & f2)
	// // 2 x 2 matrix:
	// //
	// // w0_f1 w1_f1
	// // w0_f2 w1_f2
	// //
	// amn_i.nodeFeatureWeights = new double[2][2];
	// amn_i.nodeFeatureWeights[0][0] = 5;
	// amn_i.nodeFeatureWeights[0][1] = 100;
	// amn_i.nodeFeatureWeights[1][0] = 100;
	// amn_i.nodeFeatureWeights[1][1] = 5;
	//        
	// // Node feature values for 3 nodes (n1, n2, n3), for 2 node features (f1,
	// f2)
	// // 2 x 3 matrix:
	//        
	// // n1_f1 n2_f1 n3_f1
	// // n1_f2 n2_f2 n3_f2
	// //
	// amn_i.nodeFeatureValues = new double[2][3];
	// amn_i.nodeFeatureValues[0][0] = 100;
	// amn_i.nodeFeatureValues[0][1] = 80;
	// amn_i.nodeFeatureValues[0][2] = 2;
	// amn_i.nodeFeatureValues[1][0] = 10;
	// amn_i.nodeFeatureValues[1][1] = 5;
	// amn_i.nodeFeatureValues[1][2] = 90;
	//        
	// // Edge weights for 2 labels (00 & 11). For 2 node features (f1 & f2)
	// // 2 x 2 matrix:
	// //
	// // w00_f1 w11_f1
	// // w00_f2 w11_f2
	// //
	// amn_i.edgeFeatureWeights = new double[2][2];
	// amn_i.edgeFeatureWeights[0][0] = 5;
	// amn_i.edgeFeatureWeights[0][1] = 100;
	// amn_i.edgeFeatureWeights[1][0] = 100;
	// amn_i.edgeFeatureWeights[1][1] = 5;
	//        
	// // Edge feature values for 3 edges (e1, e2, e3), for 2 edge features (f1,
	// f2)
	// // 2 x 3 matrix
	// // e1_f1 e2_f1 e3_f1
	// // e1_f2 e2_f2 e3_f2
	// //
	// amn_i.edgeFeatureValues = new double[2][3];
	// amn_i.edgeFeatureValues[0][0] = 10;
	// amn_i.edgeFeatureValues[0][1] = 5;
	// amn_i.edgeFeatureValues[0][2] = 10;
	// amn_i.edgeFeatureValues[1][0] = 10;
	// amn_i.edgeFeatureValues[1][1] = 5;
	// amn_i.edgeFeatureValues[1][2] = 90;
	//        
	// // 3 Edges e1 (n1-n2), e2 (n2-n3), e3 (n1-n3)
	// // 3 x 2 matrix:
	// //
	// // n1 n2
	// // n2 n3
	// // n1 n3
	// //
	// amn_i.edges = new double[3][2];
	// amn_i.edges[0][0] = 1;
	// amn_i.edges[0][1] = 2;
	// amn_i.edges[1][0] = 2;
	// amn_i.edges[1][1] = 3;
	// amn_i.edges[2][0] = 1;
	// amn_i.edges[2][1] = 3;
	//        
	// amn_i.performInference();
	// }
}
