package wikiGroundTruth;

import java.io.FileNotFoundException;

import wikiGroundTruth.berkeleyDb.AnchorText;
import wikiGroundTruth.berkeleyDb.CleanData;
import wikiGroundTruth.berkeleyDb.dwlDA;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

public class readDb {

	public static void main(String[] args) {
		Environment openEnv;
		try {
			openEnv = CleanData
					.openEnv("/mnt/bag1/kanika/anchorTextBerkeleyDb");

			EntityStore openStore = CleanData.openStore(openEnv, "store1");
			dwlDA dwlInd = new dwlDA(openStore);
			PrimaryIndex<Long, AnchorText> pIdx = dwlInd.pIdx;
			EntityCursor<AnchorText> pri_cursor = pIdx.entities();
			for (AnchorText doc : pri_cursor) {
				//
				// System.out.println("srno " + doc.srno + "  doc " + doc.entity
				// + "  " + doc.anchor + "  " + doc.context.toString());

				// System.out.println(doc.docTitle);
			}
			pri_cursor.close();

			// SecondaryIndex<String, Long, AnchorText> si = dwlInd.sIdx;
			//
			// EntityCursor<AnchorText> sec_cursor = si.subIndex("Anarchism")
			// .entities();
			// int pres = 0;
			// for (AnchorText doc : sec_cursor) {
			// System.out
			// .println("-------------------------------------secondary----------------------------------------------------");
			// System.out.println("srno " + doc.srno + "  doc " + doc.entity
			// + "  " + doc.anchor + "  " + doc.context.toString());
			//
			// }
			// sec_cursor.close();
			CleanData.closeStore(openStore);
			CleanData.closeEnv(openEnv);
		} catch (DatabaseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
