package spotting;

public class Config {

	public static int k = 150;

	public static boolean Server = false;
	public static boolean logistic = false;
	public static boolean wikiSense = true;
	public static Integer maxCandidates = 8;
	public static boolean useDisambPages = true;

	public static Integer pageTitleThreshold = 3;

	public static boolean normalizePerMention = false;

	public static boolean useContextScore = false;
	public static int contextSize = 100;

	public static boolean useLogisticScore = false;

	public static boolean clampingInLPInference = false;
	public static boolean perMentionConstraints = false;
	public static boolean globalConstraints = false;
	public static double upperLimitGlobalEntities = 2;
	public static double lowerLimitGlobalEntities = 1;
	public static int upperLimitPerMentionEntities = 2;

	public static boolean useEdgePotentials = true;
	public static boolean useLuceneSpotter = true;

	// Edge Features
	public static boolean useCategSim = false;

	public static boolean useOutlinkSim = false;
	public static boolean useInlinkSim = true;
	public static boolean useContextFrequentSim = false;
	public static boolean useContextSynopsisSim = false;
	public static boolean useContextSynopsisVbdjSim = false;
	public static boolean useFullTextCosineSim = false;

	public static boolean useContextSim = false;
	// Node Features
	public static boolean useSenseProbability = true;
	public static boolean useAnchorContextCosine = true;
	public static boolean useAnchorCosine = true;
	public static boolean useFullTextCosine = true;
	public static boolean usePageTitleScore = true;
	public static boolean useRedirection = true;
	public static boolean useInlinkCount = true;
	public static boolean useOutlinkCount = true;
	public static boolean useAnchorTextScore = true;

	public static boolean intercept = true;

	public static boolean threshold = false;
	public static int thresholdEdgeFeature = 1;
	public static double thresholdValue = 0.01;

	public static boolean useManualWeights = false;

	public static double nodePotWt = 1.0;
	public static double inlinkWt = 1.0;
	public static double outlinkWt = 1.0;
	public static double categWt = 1.0;
	public static double contextWt = 1.0;

	public static boolean useDissocPotential = false;
	public static double dissocEdgePotWt = 1.0;

	public static boolean chunkInputText = false;
	public static Integer chunkSize = 1000; // num of characters.

	public static double edgeWtThreshold = 0.0;

	public static boolean useManualAMNWeights = false;
	public static String amn_w0 = "1.3,3.5,3.1,1.5,0.3,2.9,8.6";
	public static String amn_w1 = "4.5,2.3,2.7,4.3,5.5,2.9,8.0";
	public static String amn_w00 = "0.0,0.0,0.0,0.7,0.0,0.0";
	public static String amn_w11 = "0.1,0.1,0.3,0.2,0.05,0.06";

	//I8
	public static double[] Lpw0 = {10.827397316393123,4.230824651274196,5.298194216627693,4.152944662388054,5.04960947631643,4.57294670895014,6.43317219243585,4.903994517446219,4.341914612184825,4.913584130373956};
	public static double[] Lpw1 = {7.738746605434796,5.783180257200886,4.715810691847387,6.352299522860143,4.964395432158648,5.441058199524936,9.12957333169317,5.110010391028866,5.672090296290254,5.100420778101133};
	
	public static double[] Lpw00 = { 0.0,0.0,0.002622432023138,0.0, 0.0, 0.0, 0.0 };
	public static double[] Lpw11 = { 0.0,0.0,0.139301821243452,0.0, 0.0, 0.0, 0.0 };
	
	//IOF3
//	public static double[] Lpw0 = {9.52276262764174,2.1243061946946393,3.1649043559191985,4.799721454896912,3.4396456986060193,3.1149269662672285,8.310825546324606,3.6279568951562706,1.9821474274613224,2.7410851137109664};
//	public static double[] Lpw1 = {5.555378496619315,4.619380876965156,3.57878271574059,8.026736924903268,3.3427379355767903,3.6287601053925695,13.442482240141818,3.1157301765035337,4.761539644198472,4.002601957948826};
//	
//	public static double[] Lpw00 = { 0.0,0.0,0.0,0.09233770449999329, 0.0, 0.0, 0.0 };
//	public static double[] Lpw11 = { 0.0,0.0,0.1742788290677928,0.0, 0.0, 0.0, 0.0 };

	//sunny:values changed.
	//public static boolean filterInlink = false;
	//public static double filterInlinkThreshold = 0.4d;
	
	public static boolean filterInlink = false;
	public static double filterInlinkThreshold = 0.01d;
	
	public static boolean filterChunk = false;
	public static int filterChunkThreshold = 300;

	public static final int NODE_FEATURE_DIM = 10;
	public static final int EDGE_FEATURE_DIM = 7;

	public Config() {
	}
}
