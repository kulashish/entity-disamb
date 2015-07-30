package wikiGroundTruth.berkeleyDb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;

import wikiGroundTruth.Document;
import wikiGroundTruth.RecursiveFileDisplay;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentMutableConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class CleanData {
	static Environment env_;
	static EntityStore store_;
	static Transaction txn_;
	static dwlDA dataAccessdwl_;

	private static final String TAG_URL = "url";
	private static final String TAG_ID = "id";
	private static final String TAG_TEXT = "text";
	private static final String TAG_ANNOTATIONS = "annotations";
	private static final String TAG_MENTION = "surface_form";
	private static final String TAG_OFFSET = "offset";
	private static final String TAG_ENTITY = "uri";
	private static int filecount = 0;
	private static int srno = 0;

	public static void main(String args[]) {
		try {
			FillDB();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void FillDB() {
		try {
			System.out
					.println("-------------------------here----------------------");
			env_ = openEnv("/mnt/bag1/kanika/anchorTextBerkeleyDb");
			store_ = openStore(env_, "store1");
			txn_ = env_.beginTransaction(null, null);
			dataAccessdwl_ = new dwlDA(store_);

			EnvironmentMutableConfig en = new EnvironmentMutableConfig();
			Long by = new Long(2147483647);
			en.setCacheSize(by);

			File currentDir = new File(
					"/mnt/bag1/kanika/annotated_wikiextractor/extractedNew");
			RecursiveFileDisplay.displayDirectoryContents(currentDir);
			// System.out.println(RecursiveFileDisplay.filenames.size());
			long start = System.currentTimeMillis();
			for (String filename : RecursiveFileDisplay.filenames) {
				BufferedReader br = new BufferedReader(new FileReader(filename));
				String jsonString;
				while ((jsonString = br.readLine()) != null) {
					JSONArray annotations = null;
					Document doc = new Document();
					JSONObject obj = new JSONObject(jsonString);
					String text = obj.getString(TAG_TEXT);
					StringBuilder s = new StringBuilder();
					int count = 0;
					while (count < text.length()) {
						s.append(text.charAt(count));
						count++;
					}
					doc.docText = s.toString();
					String title = obj.getString(TAG_URL);
					doc.docTitle = title.split("/")[4];
					if (doc.docTitle == null)
						continue;
					filecount++;
					annotations = obj.getJSONArray(TAG_ANNOTATIONS);
					for (int i = 0; i < annotations.length(); i++) {

						JSONObject ann = annotations.getJSONObject(i);
						String mention = ann.getString(TAG_MENTION);
						String ent = ann.getString(TAG_ENTITY);
						int offset = ann.getInt(TAG_OFFSET);
						doc.offset.add(offset);
						doc.entname.add(ent);
						doc.mention.add(mention);
						char[] docText = doc.docText.toCharArray();
						doc.anchorContext.add(getContext(docText, offset,
								mention));

					}

					for (int j = 0; j < doc.offset.size(); j++) {
						srno++;
						AnchorText dw = new AnchorText();
						dw.entity = doc.entname.get(j);
						dw.anchor = doc.mention.get(j);
						dw.srno = srno;
						dw.context = doc.anchorContext.get(j);
						storeInDb(dw);
					}
					if (filecount % 10000 == 0) {

						long elapsedTimeMillis = System.currentTimeMillis()
								- start;
						float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);
						System.out.println("Processed  " + filecount
								+ " documents in " + elapsedTimeMin
								+ "  minutes");
						start = System.currentTimeMillis();
					}

				}
			}
			txn_.commit();
			txn_ = null;
			closeStore(store_);
			closeEnv(env_);
			System.out.println("........DONE.......");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void storeInDb(AnchorText token) throws Exception {
		dataAccessdwl_.pIdx.put(txn_, token);
	}

	public static Environment openEnv(String home) throws DatabaseException,
			FileNotFoundException {
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setAllowCreate(true);
		envConfig.setTransactional(true);

		Environment ret = new Environment(new File(home), envConfig);
		return ret;
	}

	public static void closeEnv(Environment env) {
		try {
			env.close();
		} catch (DatabaseException ex) {
			ex.printStackTrace();
		}
	}

	public static EntityStore openStore(Environment env, String name) {
		try {
			StoreConfig storeConfig = new StoreConfig();
			storeConfig.setAllowCreate(true);
			storeConfig.setTransactional(true);
			EntityStore store = new EntityStore(env, name, storeConfig);
			return store;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void closeStore(EntityStore e) {
		try {
			e.close();
		} catch (DatabaseException ex) {
			ex.printStackTrace();
		}
	}

	public static String getContext(char[] text, int offset, String anchor) {
		String context = new String();
		char[] left = Arrays.copyOfRange(text, Math.max(offset - 1000, 0),
				offset - 1);
		char[] right = Arrays.copyOfRange(text, offset + anchor.length(), Math
				.min(offset + anchor.length() + 1000, text.length));
		String leftStr = new String(left);
		String rightStr = new String(right);
		StringTokenizer st1 = new StringTokenizer(leftStr);
		StringTokenizer st2 = new StringTokenizer(rightStr);
		int lcount = st1.countTokens();
		int l = 0;
		int rcount = st2.countTokens();
		int r = 0;
		while (st1.hasMoreTokens() && l < lcount - 5) {
			st1.nextToken();
			l++;
		}
		while (st1.hasMoreTokens()) {
			context = context + " " + st1.nextToken();
		}
		while (st2.hasMoreTokens() && r < 5) {
			context += " " + st2.nextToken();
		}
		context = context.trim();
		// System.out.println("context  " + context);
		return context;
	}
}
