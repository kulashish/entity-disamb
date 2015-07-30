package spotting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NodePotentialsSet implements Serializable{

	public String[] node_names; // not used
	public List<NodePotentials> potentials_set;
	public HashMap<String,String> mention_queries;
	
	public NodePotentialsSet() {
		potentials_set = new ArrayList<NodePotentials>();
		mention_queries = new HashMap<String, String>();
	}
}
