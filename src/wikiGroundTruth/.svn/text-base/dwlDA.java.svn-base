//package wikiGroundTruth.berkeleyDb;
//
//import com.sleepycat.je.DatabaseException;
//import com.sleepycat.persist.EntityStore;
//import com.sleepycat.persist.PrimaryIndex;
//
//public class dwlDA {
//	public PrimaryIndex<Integer, AnchorText> pIdx;
//
//	// public SecondaryIndex<Integer, ArrayList<Integer>, docWordList> sIdx;
//	public dwlDA(EntityStore store) throws DatabaseException {
//		// Primary key for Vendor classes
//		pIdx = store.getPrimaryIndex(Integer.class, AnchorText.class);
//		// sIdx = store.getSecondaryIndex(pIdx, ArrayList<Integer>().class,
//		// "classid");
//	}
//}

package wikiGroundTruth.berkeleyDb;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

public class dwlDA {
	public PrimaryIndex<Long, AnchorText> pIdx;
	public SecondaryIndex<String, Long, AnchorText> sIdx;

	public dwlDA(EntityStore store) throws DatabaseException {
		// Primary key for Vendor classes
		pIdx = store.getPrimaryIndex(Long.class, AnchorText.class);
		sIdx = store.getSecondaryIndex(pIdx, String.class, "entity");

	}
}
