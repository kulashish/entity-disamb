package spotting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComputeCategorySimilarity {

	public static int count = 0;
	public static HashMap<String, Integer> entOffsetIndex = null;
	public static HashMap<String, ArrayList<String>> categEntities = null;
	public static HashMap<String, ArrayList<String>> categH = null;
	public static HashMap<String, Integer> categCounts = null;
	public static Integer maxCategCounts = 1;
	public static HashMap<String, Long> categOffsetIndex = null;
	public static String entCategDump = "dataset/entCateg.ttl";
	public static int maxDepth = 4;
	// public static String basedir =
	// "/home/pararth/Projects/rndproj/EntityDisamb/data/";
	public static String basedir = "/data1/kanika/";
	public static Pattern pattern;
	public static Pattern pattern1;
	public static Pattern pattern2;
	public static Matcher matcher;
	public static Matcher matcher1;

	public ComputeCategorySimilarity() {
		pattern = Pattern.compile("<http.*?>");
		pattern1 = Pattern.compile(".*resource/(.*?)>");
		pattern2 = Pattern.compile(".*Category:(.*?)>");

	}

	public static void buildCategIndex() throws Exception {

		categEntities = new HashMap<String, ArrayList<String>>();

		long prev = -1;

		FileInputStream fstream = new FileInputStream(entCategDump);
		int count = 0;
		int start = 0;
		int count1 = 0;
		int last = 0;
		String ent = "";
		String ent1 = "";
		String categ = null;

		StringBuffer bf = new StringBuffer();

		char ch;

		ArrayList<String> entCateg = new ArrayList<String>();

		while (fstream.available() > 0) {

			if ((count1 % 1000) == 0 && count1 != prev) {
				// System.out.println(count1);

				prev = count1;
			}

			ch = (char) fstream.read();

			count++;

			if (ch != '\n') {
				bf.append(ch);
			} else {

				ent = parseEntities(bf.toString());
				categ = parseCategories(bf.toString());

				if (!categEntities.containsKey(categ)) {
					count1++;
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(ent);
					categEntities.put(categ, temp);
				} else {
					categEntities.get(categ).add(ent);
				}

				bf.delete(0, bf.length());

			}
		}

		writeCategIndex();
	}

	public static void buildEntityIndex() throws Exception {

		entOffsetIndex = new HashMap<String, Integer>();

		StringBuffer entFile = new StringBuffer();
		StringBuffer entFileIndex = new StringBuffer();
		String categ;
		long offset = 0;
		long prev = -1;

		FileInputStream fstream = new FileInputStream(entCategDump);

		int count = 0;

		int count1 = 0;

		String ent = "";
		String ent1 = "";

		StringBuffer bf = new StringBuffer();

		char ch;

		ArrayList<String> entCateg = new ArrayList<String>();

		while (fstream.available() > 0) {

			if ((count1 % 1000) == 0 && count1 != prev) {
				// System.out.println(count1);

				prev = count1;
			}

			ch = (char) fstream.read();

			count++;
			if (ch != '\n') {
				bf.append(ch);
			} else {

				// System.out.println(bf.toString()+" "+start);

				ent = parseEntities(bf.toString());
				categ = parseCategories(bf.toString());

				if (ent != null) {

					if (ent.compareTo(ent1) == 0)
						entCateg.add(categ);
				}

				if (ent != null && ent.compareTo(ent1) != 0) {

					entFile.append(ent1 + "\t" + entCateg.toString() + "\n");
					entFileIndex.append(ent + "\t"
							+ (offset + entFile.length()) + "\n");

					count1++;

					if (count1 == 10000) {
						// write to file
						writeEntities(entFile);
						writeEntitiesIndex(entFileIndex);
						offset += entFile.length();
						entFile.delete(0, entFile.length());
						entFileIndex.delete(0, entFileIndex.length());
						count1 = 0;
						// System.out.println("printed");
					}

					entCateg.clear();
					entCateg.add(categ);
					ent1 = ent;

				}

				bf.delete(0, bf.length());

			}
		}

		writeEntitiesIndex(entFileIndex);
		writeEntities(entFile);

	}

	public static String parseEntities(String str) {

		matcher = pattern.matcher(str);
		String t1 = null;

		while (matcher.find()) {

			String temp = matcher.group();
			matcher1 = pattern1.matcher(temp);
			while (matcher1.find()) {
				t1 = matcher1.group(1);

			}
			break;
		}
		return t1;
	}

	public static String parseCategories(String str) {

		matcher = pattern.matcher(str);
		String t1 = null;

		while (matcher.find()) {

			String temp = matcher.group();
			matcher1 = pattern2.matcher(temp);
			while (matcher1.find()) {
				t1 = matcher1.group(1);
			}
		}
		return t1;
	}

	public static void writeEntitiesIndex(StringBuffer buf) throws Exception {
		FileWriter out = new FileWriter("/dataset/entCategIndex", true);
		out.write(buf.toString());
		out.close();
	}

	public static void writeEntities(StringBuffer buf) throws Exception {
		FileWriter out = new FileWriter("/dataset/entCateg", true);
		out.write(buf.toString());
		out.close();
	}

	public static void writeCategIndex() throws Exception {
		String filename = "categEntities";
		String filename1 = "categEntitiesIndex";

		int count = 0;
		long prev = 0;
		long offset = 0;
		StringBuffer buf = new StringBuffer();
		StringBuffer buf1 = new StringBuffer();

		FileWriter fout = new FileWriter("dataset/" + filename, true);
		FileWriter fout1 = new FileWriter("dataset/" + filename1, true);

		for (String key : categEntities.keySet()) {
			count++;
			offset = prev + buf.length();
			buf1.append(key + "\t" + offset + "\n");
			buf.append(key + "\t" + categEntities.get(key).toString() + "\n");
			if (count % 10000 == 0) {
				fout.write(buf.toString());
				fout1.write(buf1.toString());
				prev += buf.length();

				buf.delete(0, buf.length());
				buf1.delete(0, buf1.length());
			}

		}

		fout.write(buf.toString());
		fout1.write(buf1.toString());

		fout.close();
		// System.out.println(categEntities.keySet().size());
	}

	public static void loadEntityIndex() throws Exception {

		String filename = "entCategIndex";
		FileReader f = new FileReader(basedir + filename);
		entOffsetIndex = new HashMap<String, Integer>();

		BufferedReader br = null;

		try {

			String sCurrentLine;

			br = new BufferedReader(f);

			while ((sCurrentLine = br.readLine()) != null) {
				String arr[] = sCurrentLine.toString().split("\t");
				entOffsetIndex.put(arr[0], Integer.parseInt(arr[1]));

			}

			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void loadCategIndex() throws Exception {

		String filename = "categHierarchyIndex2";
		FileReader f = new FileReader(basedir + filename);

		categOffsetIndex = new HashMap<String, Long>();

		StringBuffer buf = new StringBuffer();

		BufferedReader br = null;

		try {

			String sCurrentLine;

			br = new BufferedReader(f);

			while ((sCurrentLine = br.readLine()) != null) {
				String arr[] = sCurrentLine.toString().split("\t");
				categOffsetIndex.put(arr[0], Long.parseLong(arr[1]));

			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * gives an array list of all the categories that the entity has been tagged
	 * in returns null if entity is not present in the index file, else returns
	 * its category list
	 */

	public static ArrayList<String> entCateg(String entity) throws Exception {

		ArrayList<String> list = new ArrayList<String>();

		if (entOffsetIndex == null) {
			System.out.println("entCateg loading index...");
			loadEntityIndex();
			System.out.println("entCateg index loaded !!!");
		}

		if (!entOffsetIndex.containsKey(entity)) {
			// System.out.println("Error: entOffsetIndex does not contain " +
			// entity);
			return null;
		}

		File file = new File(basedir + "entCateg");

		RandomAccessFile rFile = new RandomAccessFile(file, "r");

		// System.out.println(entOffsetIndex.get(entity));
		rFile.seek(entOffsetIndex.get(entity));
		String temp = rFile.readLine();
		rFile.close();
		String arr[] = temp.split("\t");

		String temp1 = arr[1].substring(1, arr[1].length() - 1);
		String arr1[] = temp1.split(",");

		for (String key : arr1) {
			list.add(key.trim());
		}

		return list;
	}

	/*
	 * gives an array list of all the entities that have been tagged in this
	 * category
	 */

	// public static ArrayList<String> categEntites(String categ)throws
	// Exception{
	// String filename="categEntities";
	// ArrayList<String> list=new ArrayList<String>();
	//
	// long then=System.currentTimeMillis();
	// if(categEntitiesIndex==null){
	// loadCategIndex();
	// }
	// System.out.println("Index loaded");
	// System.out.println(System.currentTimeMillis()-then);
	//
	// then = System.currentTimeMillis();
	//
	// File file=new File("/dataset/"+filename);
	// RandomAccessFile rFile=new RandomAccessFile(file, "r");
	//
	//
	//
	// rFile.seek(categEntitiesIndex.get(categ));
	// String temp=rFile.readLine();
	// String arr[]=temp.split("\t");
	// String temp1=arr[1].substring(1,arr[1].length()-1);
	// String arr1[]=temp1.split(",");
	//
	// for(String key:arr1){
	// list.add(key.trim());
	// }
	// System.out.println(System.currentTimeMillis()-then);
	// return list;
	//
	//
	// }

	public static void rebuildCategIndex() throws Exception {

		String filename = "categEntities";
		FileInputStream fstream = new FileInputStream("dataset/" + filename);
		StringBuffer filewrite = new StringBuffer();

		int count = 0;
		int prev2 = 0;
		int count1 = 0;
		int prev = 0;

		String ent = "";
		String ent1 = "";

		StringBuffer bf = new StringBuffer();

		char ch;

		while (fstream.available() > 0) {

			if ((count1 % 1000) == 0 && count1 != prev) {
				// System.out.println(count1);
				writeBuffer(filewrite, "categEntitiesIndex2");
				filewrite.delete(0, filewrite.length());
				prev = count1;
			}

			ch = (char) fstream.read();

			count++;
			if (ch != '\n') {
				bf.append(ch);
			} else {

				// System.out.println(bf.toString()+" "+start);
				String arr[] = bf.toString().split("\t");
				filewrite.append(arr[0] + "\t" + prev2 + "\n");
				prev2 = count;
				count1++;
				bf.delete(0, bf.length());

			}
		}

	}

	public static void rebuildEntityIndex() throws Exception {

		String filename = "entCateg";
		FileInputStream fstream = new FileInputStream("dataset/" + filename);
		StringBuffer filewrite = new StringBuffer();

		int count = 0;
		int prev2 = 0;
		int count1 = 0;
		int prev = 0;

		String ent = "";
		String ent1 = "";

		StringBuffer bf = new StringBuffer();

		char ch;

		while (fstream.available() > 0) {

			if ((count1 % 1000) == 0 && count1 != prev) {
				// System.out.println(count1);
				writeBuffer(filewrite, "entCategIndex2");
				filewrite.delete(0, filewrite.length());
				prev = count1;
			}

			ch = (char) fstream.read();

			count++;
			if (ch != '\n') {
				bf.append(ch);
			} else {

				// System.out.println(bf.toString()+" "+start);
				String arr[] = bf.toString().split("\t");
				filewrite.append(arr[0] + "\t" + prev2 + "\n");
				prev2 = count;
				count1++;
				bf.delete(0, bf.length());

			}
		}

	}

	public static void writeBuffer(StringBuffer buf, String filename)
			throws Exception {
		FileWriter out = new FileWriter("dataset/" + filename, true);
		out.write(buf.toString());
		out.close();
	}

	public static ArrayList<String> intersection(ArrayList<String> list1,
			ArrayList<String> list2) {

		ArrayList<String> list = new ArrayList<String>();

		for (String str : list1) {
			if (list2.contains(str)) {
				list.add(str);
			}
		}

		return list;
	}

	public static void readFromFile(String filename, int size) throws Exception {

		FileReader f = new FileReader(basedir + filename);
		StringBuffer buf = new StringBuffer();
		BufferedReader br = null;
		double[][] unionArr = new double[size][size];
		double[][] intersectionArr = new double[size][size];
		double[][] score = new double[size][size];
		String sCurrentLine;
		br = new BufferedReader(f);
		int i = 0;
		String arr[] = new String[size];
		while ((sCurrentLine = br.readLine()) != null) {
			arr[i] = sCurrentLine;
			i++;
		}
		f.close();
		for (i = 0; i < size; i++) {
			for (int j = 0; j <= i; j++) {
				HashMap<Integer, ArrayList<String>> categories1 = new HashMap<Integer, ArrayList<String>>();
				HashMap<Integer, ArrayList<String>> categories2 = new HashMap<Integer, ArrayList<String>>();
				categories1 = start(arr[i]);
				categories2 = start(arr[j]);
				double union = catUnion(categories1, categories2);
				double intersection = catIntersection(categories1, categories2);
				unionArr[i][j] = union;
				intersectionArr[i][j] = intersection;
				unionArr[j][i] = union;
				intersectionArr[j][i] = intersection;
				score[i][j] = intersectionArr[i][j] / unionArr[i][j];
				score[j][i] = score[i][j];
			}
		}
		String fname1 = "CategSim.txt";
		write(fname1, score);

	}

	public static void write(String filename, double[][] m) throws IOException {
		FileWriter f0 = new FileWriter(basedir + filename);
		try {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(2);
			nf.setGroupingUsed(false);
			for (int i = 0; i < m.length; i++) {
				for (int j = 0; j < m.length; j++) {
					String formattedvalue = nf.format(m[i][j]);
					f0.write(formattedvalue);
					f0.write(" ");
				}
				f0.write("\n");
			}
		} finally {
			try {
				f0.close();
			} catch (IOException ex) {
				// Log error writing file and bail out.
			}
		}

	}

	public static double computeCategSim(String entity1, String entity2) {
		HashMap<Integer, ArrayList<String>> categories1 = new HashMap<Integer, ArrayList<String>>();
		HashMap<Integer, ArrayList<String>> categories2 = new HashMap<Integer, ArrayList<String>>();

		try {
			categories1 = start(entity1);
			categories2 = start(entity2);
			if (categories1 == null || categories2 == null) {
				return 0;
			} else {
				// System.out.println(entity1);
				// dumpCategTree(categories1);
				// System.out.println(entity2);
				// dumpCategTree(categories2);
				// double union=catUnion(categories1,categories2);
				// double intersection=catIntersection(categories1,categories2);
				// return intersection/union;
				return catCosineSim(categories1, categories2);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			return 0;
		}
	}

	public static double catCosineSim(HashMap<Integer, ArrayList<String>> cat1,
			HashMap<Integer, ArrayList<String>> cat2) {
		HashMap<String, Double> vec1 = new HashMap<String, Double>();
		HashMap<String, Double> vec2 = new HashMap<String, Double>();

		for (int depth : cat1.keySet()) {
			for (String c1 : cat1.get(depth)) {
				if (!vec1.containsKey(c1)) {
					double tf = Math.pow(10, 2 - depth);
					int catCount = 1;
					if (categCounts.containsKey(c1)) {
						catCount = categCounts.get(c1);
					}
					double idf = Math.log((double) maxCategCounts
							/ (double) catCount);
					vec1.put(c1, tf * idf);
				}
			}
		}

		for (int depth : cat2.keySet()) {
			for (String c1 : cat2.get(depth)) {
				if (!vec2.containsKey(c1)) {
					double tf = Math.pow(10, 2 - depth);
					int catCount = 1;
					if (categCounts.containsKey(c1)) {
						catCount = categCounts.get(c1);
					}
					double idf = Math.log((double) maxCategCounts
							/ (double) catCount);
					vec2.put(c1, tf * idf);
				}
			}
		}

		HashSet<String> intersection = new HashSet<String>(vec1.keySet());
		intersection.retainAll(vec2.keySet());
		double cosine = 0.0;
		for (String s : intersection) {
			Double v1 = vec1.get(s);
			Double v2 = vec2.get(s);
			vec1.put(s, 100 * v1);
			vec2.put(s, 100 * v2);
			cosine += vec1.get(s) * vec2.get(s);
		}

		cosine /= (calcVecNorm(vec1) * calcVecNorm(vec2));
		return cosine;
	}

	public static double calcVecNorm(HashMap<String, Double> vec) {
		double norm = 0.0;
		for (String s : vec.keySet()) {
			norm += Math.pow(vec.get(s), 2);
		}
		return Math.sqrt(norm);
	}

	public static double catIntersection(
			HashMap<Integer, ArrayList<String>> categories1,
			HashMap<Integer, ArrayList<String>> categories2) {
		double score = 0d;
		HashMap<String, Double> newHash = new HashMap<String, Double>();
		for (int depth : categories1.keySet()) {
			for (String c1 : categories1.get(depth)) {
				if (!newHash.containsKey(c1)) {
					for (int depth1 : categories2.keySet()) {
						if (categories2.get(depth1).contains(c1)) {
							double categ_score = 1.0;
							/*
							 * if (categCounts.containsKey(c1)){ categ_score =
							 * 1.0/(double)(categCounts.get(c1)); }
							 */
							double tem = (categ_score)
									* (double) (depth + depth1)
									/ (double) (2 * depth * depth1);
							newHash.put(c1, tem);
							break;
						}

					}
				}
			}
		}
		for (String key : newHash.keySet()) {
			// System.out.println("Common Category: "+key+"  Score : "+newHash.get(key));
			score += newHash.get(key);
		}
		return score;
	}

	public static double catUnion(
			HashMap<Integer, ArrayList<String>> categories1,
			HashMap<Integer, ArrayList<String>> categories2) {
		double score = 0d;
		HashMap<Integer, ArrayList<String>> newHash = new HashMap<Integer, ArrayList<String>>();
		for (int depth : categories1.keySet()) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.addAll(categories1.get(depth));
			temp.addAll(categories2.get(depth));
			HashSet hs = new HashSet();
			hs.addAll(temp);
			temp.clear();
			temp.addAll(hs);
			newHash.put(depth, temp);

		}
		// System.out.println("final"+newHash);
		for (int depth : newHash.keySet()) {
			for (int i = depth - 1; i >= 1; i--) {
				for (String c : newHash.get(i)) {
					if (newHash.get(depth).contains(c)) {
						newHash.get(depth).remove(c);
					}
				}
			}

		}
		for (int depth : newHash.keySet()) {
			float size = newHash.get(depth).size();
			score += (double) size / ((double) depth * depth);
			/*
			 * for (String categ: newHash.get(depth)){ if
			 * (categCounts.containsKey(categ)){ score +=
			 * (1.0/(double)categCounts.get(categ))/ ((double)depth*depth); }
			 * else { score += 1.0/((double)depth*depth); } }
			 */
		}

		return score;
	}

	/*
	 * @param entity title
	 * 
	 * @returns map of height to list of categories present at that height
	 * corresponding to the entity given as input
	 */
	public static HashMap<Integer, ArrayList<String>> start(String entity1)
			throws Exception {
		int depth = 1;
		if (entOffsetIndex == null) {
			loadEntityIndex();
		}
		if (categOffsetIndex == null) {
			loadCategIndex();
		}
		if (categCounts == null) {
			readCategCountIndex();
		}
		entity1 = entity1.replace(" ", "_");
		ArrayList<String> cat1 = entCateg(entity1);
		HashMap<Integer, ArrayList<String>> levelwiseCateg = new HashMap<Integer, ArrayList<String>>();
		if (cat1 == null) {
			// System.out.println("No Level 1 category for this entity   "+entity1);
			return null;
		} else {

			levelwiseCateg.put(depth, cat1);
			depth = depth + 1;
			if (depth <= maxDepth)
				collapse(levelwiseCateg, depth);
		}
		return levelwiseCateg;
	}

	public static void collapse(
			HashMap<Integer, ArrayList<String>> levelwiseCateg, int depth)
			throws Exception {
		// System.out.println("depth =  "+depth);
		if (depth <= maxDepth) {
			ArrayList<String> appended = new ArrayList<String>();
			for (String c : levelwiseCateg.get(depth - 1)) {
				ArrayList<String> Categ = catToCateg(c);
				if (Categ != null) {
					for (Iterator<String> it = Categ.iterator(); it.hasNext();) {
						String eachCat = it.next();
						for (int i = 1; i <= depth - 1; i++) {
							for (Iterator<String> it1 = levelwiseCateg.get(i)
									.iterator(); it1.hasNext();) {
								String temp = it1.next();
								if (temp.equals(eachCat)) {
									it.remove();
									break;
								}
							}
						}
					}
					appended.addAll(Categ);
				}
			}
			levelwiseCateg.put(depth, appended);
			collapse(levelwiseCateg, depth + 1);
		}
	}

	/*
	 * given a category this function returns the list of categories that are
	 * reachable from this category i.e its parent categories
	 */
	public static ArrayList<String> catToCateg(String categ) throws Exception {

		ArrayList<String> list = new ArrayList<String>();

		if (categOffsetIndex == null) {
			System.out.println("categ loading index...");
			loadCategIndex();
			System.out.println("categ index loaded !!!");
		}

		if (categOffsetIndex.get(categ) != null) {
			File file = new File(basedir + "categHierarchy");
			RandomAccessFile rFile = new RandomAccessFile(file, "r");
			// System.out.println(categOffsetIndex.get(categ));
			rFile.seek(categOffsetIndex.get(categ));
			String temp = rFile.readLine();
			rFile.close();
			String arr[] = temp.split("\t");

			String temp1 = arr[1].substring(1, arr[1].length() - 1);
			String arr1[] = temp1.split(",");

			for (String key : arr1) {
				list.add(key.trim());
			}
			rFile.close();
			return list;

		}

		else {

			return null;
		}

	}

	public static Integer intersectCount(String entity1, String entity2)
			throws Exception {

		System.out.println("entity1 intersectCount     " + entity1);
		System.out.println("entity2 intersectCount     " + entity2);
		if (entOffsetIndex == null) {

			loadEntityIndex();
		}

		entity1 = entity1.replace(" ", "_");
		entity2 = entity2.replace(" ", "_");
		System.out.println("After Replacing entity1 intersectCount     "
				+ entity1);
		System.out.println("After Replacing entity2 intersectCount     "
				+ entity2);

		ArrayList<String> cat1 = entCateg(entity1);
		ArrayList<String> cat2 = entCateg(entity2);

		if (cat1 == null || cat2 == null) {
			System.out.println("zero score");
			return 0;
		}
		ArrayList<String> common = intersection(cat1, cat2);
		System.out.println("\n  " + entity1 + " " + cat1.size() + "       "
				+ entity2 + "  " + cat2.size() + "       " + "counttt   "
				+ common);
		System.exit(0);
		return common.size();

	}

	public static String firstCapital(String place) {

		int i = 0;
		place = place.toLowerCase();

		StringBuffer abc = new StringBuffer();
		for (i = 0; i < place.length(); i++) {
			if (i == 0) {
				int temp = (int) place.charAt(i) - 32;
				abc.append((char) temp);
			} else if (place.charAt(i) == 32) {
				abc.append(place.charAt(i));
				i = i + 1;
				abc.append((char) (place.charAt(i) - 32));
			} else
				abc.append(place.charAt(i));

		}
		return abc.toString();
	}

	public static void buildCategCountIndex(String filename) throws Exception {
		HashMap<String, Integer> categCount = new HashMap<String, Integer>();

		BufferedReader br = new BufferedReader(
		// new FileReader(
		// "/home/pararth/Projects/rndproj/EntityDisamb/data/categHierarchy"));
				new FileReader("/data1/kanika/categHierarchy"));
		String input;
		while ((input = br.readLine()) != null) {
			String[] inp_split = input.split("\t");
			String categ_name = inp_split[0];
			if (categ_name.equals("[]"))
				continue;
			String[] categ_split = inp_split[1].substring(1,
					inp_split[1].length() - 1).split(", ");
			for (int i = 0; i < categ_split.length; i++) {
				if (categCount.containsKey(categ_split[i])) {
					categCount.put(categ_split[i],
							categCount.get(categ_split[i]) + 1);
				} else {
					categCount.put(categ_split[i], 1);
				}
				// System.out.println(categ_split[i] + " " +
				// categCount.get(categ_split[i]));
			}
		}

		System.out.println("### categHierarchy done ###");

		// br = new BufferedReader(new FileReader(
		// "/home/pararth/Projects/rndproj/EntityDisamb/data/entCateg"));
		br = new BufferedReader(new FileReader("/data1/kanika/entCateg"));

		while ((input = br.readLine()) != null) {
			String[] inp_split = input.split("\t");
			String categ_name = inp_split[0];
			if (categ_name.equals("[]"))
				continue;
			String[] categ_split = inp_split[1].substring(1,
					inp_split[1].length() - 1).split(", ");
			for (int i = 0; i < categ_split.length; i++) {
				if (categCount.containsKey(categ_split[i])) {
					categCount.put(categ_split[i],
							categCount.get(categ_split[i]) + 1);
				} else {
					categCount.put(categ_split[i], 1);
				}
				// System.out.println(categ_split[i]);
			}
		}

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(filename, false)));
			System.out.println("### Printing categCount index ###");
			for (String categ : categCount.keySet()) {
				out.println(categ + ", " + categCount.get(categ));
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error: cannot print to " + filename);
		}
	}

	public static void readCategCountIndex() throws Exception {
		categCounts = new HashMap<String, Integer>();

		// BufferedReader br = new BufferedReader(new FileReader(
		// "/home/pararth/Projects/rndproj/EntityDisamb/data/categCounts"));
		BufferedReader br = new BufferedReader(new FileReader(
				"/data1/kanika/categCounts"));
		String input;
		while ((input = br.readLine()) != null) {
			String[] inp_split = input.split(", ");
			if (inp_split.length != 2)
				continue;
			String categ_name = inp_split[0];
			if (categ_name.equals("[]"))
				continue;
			Integer categ_count = Integer.parseInt(inp_split[1]);
			if (categ_count == null || categ_count == 0)
				continue;
			categCounts.put(categ_name, categ_count);
			if (maxCategCounts < categ_count) {
				maxCategCounts = categ_count;
			}
		}

		// System.out.println("### categCounts done ###");

	}

	public static void dumpCategTree(
			HashMap<Integer, ArrayList<String>> categories) {
		for (Integer depth : categories.keySet()) {
			System.out.print(depth + ": ");
			for (String cat : categories.get(depth)) {
				System.out.print(cat + ":");
				if (categCounts.containsKey(cat)) {
					System.out.print(categCounts.get(cat));
				}
				System.out.print(" ");
			}
			System.out.println();
		}
	}

	public static void main(String[] args) throws Exception {
		// buildCategCountIndex("/home/pararth/Projects/rndproj/EntityDisamb/data/categCounts");

		System.out.println("Computer_science, Programming_language: "
				+ computeCategSim("Computer_science", "Programming_language"));
		System.out.println("Computer_science, Randomized_algorithm: "
				+ computeCategSim("Computer_science", "Randomized_algorithm"));
		System.out.println("Machine_learning, Tom_M._Mitchell: "
				+ computeCategSim("Machine_learning", "Tom_M._Mitchell"));
		System.out
				.println("Machine_learning, Support_vector_machine: "
						+ computeCategSim("Machine_learning",
								"Support_vector_machine"));
		System.out
				.println("Massachusetts_Institute_of_Technology, Harvard_Bridge: "
						+ computeCategSim(
								"Massachusetts_Institute_of_Technology",
								"Harvard_Bridge"));
		System.out
				.println("Massachusetts_Institute_of_Technology, Cambridge,_Massachusetts: "
						+ computeCategSim(
								"Massachusetts_Institute_of_Technology",
								"Cambridge,_Massachusetts"));
		System.out.println("Sachin_Tendulkar, Brian_Lara: "
				+ computeCategSim("Sachin_Tendulkar", "Brian_Lara"));
		System.out.println("Sachin_Tendulkar, Mumbai_Indians: "
				+ computeCategSim("Sachin_Tendulkar", "Mumbai_Indians"));
		System.out.println("Sachin_Tendulkar, Marathi_language: "
				+ computeCategSim("Sachin_Tendulkar", "Marathi_language"));
		System.out.println("Sachin_Tendulkar, Ferrari_360: "
				+ computeCategSim("Sachin_Tendulkar", "Ferrari_360"));
		System.out.println("Sports_car, Lamborghini: "
				+ computeCategSim("Sports_car", "Lamborghini"));
		System.out.println("Sports_car, 2008_Paris_Motor_Show: "
				+ computeCategSim("Sports_car", "2008_Paris_Motor_Show"));
		System.out.println("Sports_car, BMW_X1: "
				+ computeCategSim("Sports_car", "BMW_X1"));
		System.out.println("Philosophy, Metaphysics: "
				+ computeCategSim("Philosophy", "Metaphysics"));
		System.out.println("Philosophy, Socratic_dialogue: "
				+ computeCategSim("Philosophy", "Socratic_dialogue"));
		System.out.println("Philosophy, Upanishads: "
				+ computeCategSim("Philosophy", "Upanishads"));
		System.out.println("Philosophy, Immanuel_Kant: "
				+ computeCategSim("Philosophy", "Immanuel_Kant"));
		System.out.println("Astronomy, Stonehenge: "
				+ computeCategSim("Astronomy", "Stonehenge"));
		System.out.println("Astronomy, Milky_Way: "
				+ computeCategSim("Astronomy", "Milky_Way"));
		System.out.println("Astronomy, Galileo_Galilei: "
				+ computeCategSim("Astronomy", "Galileo_Galilei"));
		System.out.println("Sachin_Tendulkar, Immanuel_Kant: "
				+ computeCategSim("Sachin_Tendulkar", "Immanuel_Kant"));
		System.out.println("Astronomy, Support_vector_machine: "
				+ computeCategSim("Astronomy", "Support_vector_machine"));

		/*
		 * readFromFile("kashmir.txt", 26); HashMap<Integer,ArrayList<String>>
		 * categories1 = new HashMap<Integer,ArrayList<String>>();
		 * HashMap<Integer,ArrayList<String>> categories2 = new
		 * HashMap<Integer,ArrayList<String>>();
		 * 
		 * try { String entity1="Support_vector_machine"; String
		 * entity2="Machine_learning"; categories1=start(entity1);
		 * System.out.println("Category Hierarchy for "+entity1+" is : ");
		 * for(Integer key : categories1.keySet()){
		 * System.out.println("level:  "+
		 * key+"\nCategories:   "+categories1.get(key)); }
		 * categories2=start(entity2);
		 * System.out.println("Category Hierarchy for "+entity2+" is: ");
		 * for(Integer key : categories2.keySet()){
		 * System.out.println("level:  "+
		 * key+"\nCategories:   "+categories2.get(key)); } } catch (Exception e)
		 * { e.printStackTrace(); } // categories1.clear(); //
		 * categories2.clear(); // // ArrayList<String> arr = new
		 * ArrayList<String>(); // arr.add("a"); // arr.add("b"); //
		 * ArrayList<String> arr1 = new ArrayList<String>(); // arr1.add("a");
		 * // arr1.add("bb"); // categories1.put(1,arr); // categories1.put(2,
		 * arr1); // ArrayList<String> arr3 = new ArrayList<String>(); //
		 * arr3.add("c"); // arr3.add("d"); // categories2.put(1, arr3); //
		 * ArrayList<String> arr2 = new ArrayList<String>(); // arr2.add("bb");
		 * // arr2.add("b"); // categories2.put(2, arr2); //
		 * System.out.println(categories1); // System.out.println(categories2);
		 * float union=catUnion(categories1,categories2); float
		 * intersection=catIntersection(categories1,categories2);
		 * System.out.println("score of union = "+union);
		 * System.out.println("score of intersection = "+intersection);
		 */
	}

}
