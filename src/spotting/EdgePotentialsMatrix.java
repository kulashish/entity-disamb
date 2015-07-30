package spotting;

import java.io.Serializable;

import org.ejml.data.DenseMatrix64F;

public class EdgePotentialsMatrix implements Serializable{

	// Edge potential types
	public static int CATEGORY_SIM = 0;
	public static int CONTEXT_SIM = 1;
	public static int OUTLINK_SIM = 2;
	public static int INLINK_SIM = 3;
	
	public int potential_type;  // The type of edge potential of this matrix (from the 4 above)
	
	public int size;  // size of similarity matrix
	public DenseMatrix64F matrix;
	
	public EdgePotentialsMatrix(int numNodes) {
		matrix = new DenseMatrix64F(numNodes, numNodes);
		size = numNodes;
	}
	
}

