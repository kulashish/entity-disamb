package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;

public class StatCalculator {
	private File xmlFolder = null;
	private String outCSV = null;
	private Map<String, ArrayList<XMLTagInfo>> globalXMLMap;

	public StatCalculator(String xmlFolderPath, String out, String singleXMLPath)
			throws Exception {
		this(xmlFolderPath, out);
		globalXMLMap = new ParseXML().parseXML(singleXMLPath);
	}

	public StatCalculator(String xmlFolderPath, String out) {
		if (null != xmlFolderPath)
			xmlFolder = new File(xmlFolderPath);
		outCSV = out;
	}

	public void calculate_single() throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outCSV));
		PerFileMentionStats stats = null;
		ArrayList<XMLTagInfo> mentions = null;
		for (String key : globalXMLMap.keySet()) {
			mentions = globalXMLMap.get(key);
			if (mentions.size() < 10)
				continue;
			System.out.println(key + " : " + mentions.size());
			stats = new PerFileMentionStats(key, mentions);
			writer.append(stats.asCSV());
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}

	public void calculate() throws Exception {
		Map<String, ArrayList<XMLTagInfo>> xmlMap = null;
		BufferedWriter writer = new BufferedWriter(new FileWriter(outCSV));
		String csv = null;
		PerFileMentionStats stats = null;
		ArrayList<XMLTagInfo> mentions = null;
		for (File f : xmlFolder.listFiles()) {
			xmlMap = new ParseXML().parseXML(f.getAbsolutePath());
			for (String key : xmlMap.keySet()) {
				mentions = xmlMap.get(key);
				if (mentions.size() < 10)
					continue;
				stats = new PerFileMentionStats(key, mentions);
				csv = stats.asCSV();
				writer.append(csv);
				int limit = stats.maxOffset;
				System.out.println(key + " - " + limit);
				if (key.indexOf("KDD_") != -1)
					key = key.substring(4);
				stats = new PerFileMentionStats(key, globalXMLMap.get(key),
						limit);
				System.out.println(key + " : " + globalXMLMap.get(key).size());
				writer.append(',').append(stats.asCSV());
				writer.newLine();
			}
		}
		writer.flush();
		writer.close();
	}

	class PerFileMentionStats {

		public PerFileMentionStats(String filename,
				ArrayList<XMLTagInfo> tagInfoList) {
			this(filename, tagInfoList, -1);
		}

		public PerFileMentionStats(String filename,
				ArrayList<XMLTagInfo> tagInfoList, int limit) {
			int lastOffset = -1;
			int lastLength = -1;
			file = filename;
			System.out.println("Number of mentions: " + tagInfoList.size());
			for (XMLTagInfo info : tagInfoList) {
				info.cleanMention();
				if (limit != -1 && info.offset > limit)
					break;
				System.out.println(info.offset + " : " + lastOffset + " : "
						+ (info.offset != lastOffset));
				if (info.offset != lastOffset) {
					System.out.print(info.offset+",");
					numMentions++;
					// numWordsinMention+=info.mention.split(" ").length;
					lastOffset = info.offset;
					lastLength = info.length;
					if (info.offset > maxOffset)
						maxOffset = info.offset;
					if (null == info.wikiEntity
							|| info.wikiEntity.equalsIgnoreCase("na"))
						na++;
				} else {
					if (lastLength == info.length)
						multiple++;
					else
						overlaps++;
				}
			}
		}

		public String asCSV() {
			StringBuilder builder = new StringBuilder(file);
			builder.append(',').append(numMentions).append(',').append(na)
					.append(',').append(multiple).append(',').append(overlaps);
			return builder.toString();
		}

		private int numMentions = 0;
		private int na = 0;
		private int multiple = 0;
		private int overlaps = 0;
		private int numWordsinMention = 0;
		private String file;
		private int maxOffset = -1;

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// args[0] = XML folder, args[1] = out CSV file
		try {
			StatCalculator calc = args.length == 3 ? new StatCalculator(
					args[0], args[1], args[2]) : new StatCalculator(null,
					args[1], args[0]);
			if (args.length == 3)
				calc.calculate();
			else
				calc.calculate_single();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
