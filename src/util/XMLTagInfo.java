package util;

import it.unimi.dsi.util.Interval;

import java.io.Serializable;
import java.util.ArrayList;

public class XMLTagInfo implements Comparable<XMLTagInfo>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 330869254371860340L;
	public String wikiEntity;
	public int offset;
	public int length;
	public String mention;
	public Interval interval;

	@Override
	public int compareTo(XMLTagInfo o) {
		if (o.interval.left > interval.right)
			return -1;
		if (o.interval.right < interval.left)
			return 1;
		return interval.left - o.interval.left;
	}

	public void cleanMention() {
		if (null != mention) {
			mention = mention.replaceAll("_[0-9]+", "");
			length = mention.length();
		}
	}

	public static boolean contains(ArrayList<XMLTagInfo> tagListSorted,
			XMLTagInfo tag) {
		boolean found = false;
		for (XMLTagInfo info : tagListSorted) {
			if (tag.compareTo(info) >= 1)
				break;
			if (tag.compareTo(info) == 0 && tag.wikiEntity != null
					&& tag.wikiEntity.equalsIgnoreCase(info.wikiEntity)) {
				found = true;
				break;
			}
		}
		return found;
	}

	public static void main(String args[]) {
		XMLTagInfo info = new XMLTagInfo();
		info.mention = "Greek_31";
		info.cleanMention();
		System.out.println(info.mention);
	}
}
