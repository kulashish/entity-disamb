package spotting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.wikipedia.miner.model.Label;

import spotting.KeywordsGroundTruth.Mention;
import util.TestJAWS;
import util.WikiToFreebaseIDMap;
import weka.core.Stopwords;
import wikiGroundTruth.server.ClientWikisauras;
import wikiGroundTruth.server.LabelSense;

public class DatasetSpotter {
	
	String Text;
	KeywordsGroundTruth kw = null;
	int maxLength;
	
	DatasetSpotter(String query,int _maxLength){
		Text = query;
		kw = new KeywordsGroundTruth(Text);
		maxLength = _maxLength;
	}
	
	KeywordsGroundTruth getKWGroundTruth(){
		return kw;
	}
	
	void extractKeywords(){
		kw.tagged_document = kw.tagger.tagString(Text);
		ArrayList<String> tokens = new ArrayList<String>();
		
		System.out.println("tagged document : " + kw.tagged_document);

		StringTokenizer str = new StringTokenizer(kw.tagged_document);

		while (str.hasMoreTokens()) {
			String token = str.nextToken();
			if (token == null || "".equals(token) || " ".equals(token))
				continue;
			if (!Stopwords.isStopword(token.split("_")[0])
					|| kw.noun_tags.contains(token.split("_")[1])
					|| kw.adj_tags.contains(token.split("_")[1])
					|| kw.extra_tags.contains(token.split("_")[1]))
						tokens.add(token);
			//System.out.println("token : " + token);
			if (!Stopwords.isStopword(token.split("_")[0])){
//				System.out.println("token added.");
				tokens.add(token);
			}
		}

		String prev_tag = null; // if previous token was a noun then add n-gram
		// noun clause

		int curr_offset = 0,currbyte = 0;
		for (int i = 0; i < tokens.size(); i++) {
			// System.out.print(" "+tokens.get(i));
			if (tokens.get(i) == null)
				continue;
			Matcher matcher = kw.pattern.matcher(tokens.get(i));
			matcher.find();
			String word = matcher.group(1).replaceAll("\\p{C}", " ");//.replace(".","");
			String tag = matcher.group(2);
			
			//System.out.println("word: " + word + " tag: " + tag);

			if (word == null || "".equals(word)) {
				prev_tag = null;
				continue;
			}
			String token = word.replaceAll("[^0-9a-z\\sA-Z/\\-]", "");
			if ("".equals(token) || "/".equals(token)) {
				prev_tag = null;
				continue;
			}
			if (!(kw.noun_tags.contains(tag) || kw.adj_tags.contains(tag) || kw.extra_tags
					.contains(tag))) {
				prev_tag = null;
				continue;
			}
			KeywordsGroundTruth.Mention mention = kw.new Mention();

			if (tag.equals("JJ")) {
				String temp = TestJAWS.getNounForm(token);
				if (temp != null && !"".equals(temp)) {
					mention.key = temp;
					prev_tag = null;
				} else {
					mention.key = token;
					prev_tag = null;

				}
			} else {
				mention.key = token;
			}

			mention.name = word;
			mention.length = word.length();
			curr_offset = kw.document.indexOf(word, curr_offset);
			
			mention.offset = curr_offset;
			mention.context = kw.getContext(curr_offset, mention.length,
					kw.contextSize);
			mention.contextAroundMention = kw.getContext(curr_offset,
					mention.length, 10);
			kw.keywords.add(mention);
		}
//		LuceneIndexWrapper luceneIndex = new LuceneIndexWrapper(
//				props.getCompleteIndex(), props.getRedirectIndex(),
//				props.getInlinkIndex(), props.getDisambIndex(),
//				props.getAnchorIndex());

		ArrayList<Mention> mentions = new ArrayList<Mention>();
		mentions.addAll(kw.keywords);
		//System.out.println("consolidating mentions size:" + mentions.size());
		kw.keywords = new ArrayList<Mention>();
		Integer[] token_type = new Integer[mentions.size()];
		for (int i = 0; i < token_type.length; i++)
			token_type[i] = 0;

		curr_offset = 0;
		String curr_mention = "";
		for (int i = 0; i < mentions.size(); i++) {
			if (token_type[i] != 0) {
				//i++;
				continue;
			}
			curr_offset = mentions.get(i).offset;
			curr_mention = mentions.get(i).name;
			//System.out.println("offset: " + curr_offset + " curr_mention: " + curr_mention + " context: " + mentions.get(i).context);

			String[] allWords = new String[maxLength];
			Integer[] allOffset = new Integer[maxLength];
			String currWord = curr_mention;
			Integer currWordEnd = curr_offset + curr_mention.length();// + 1;

			allWords[0] = currWord;
			allOffset[0] = curr_offset;
			int k = 1;
			for (; k < maxLength; k++) {
				currWordEnd = Text.indexOf(" ", currWordEnd + 1);
				if (currWordEnd == -1)
					currWordEnd = Text.length();
				if (curr_offset < 0 || curr_offset >= Text.length()) {
					k--;
					break;
				}
				currWord = Text.substring(curr_offset, currWordEnd);
				allWords[k] = currWord;
				allOffset[k] = currWordEnd;
				if (currWordEnd >= Text.length())
					break;
			}
			if (k == maxLength)
				k--;

			for (; k >= 0; k--) {
				LabelSense senses = null;
				if (Stopwords.isStopword(allWords[0])){
					continue;
				}
				
				String words = allWords[k].replaceAll("\\p{C}", "").replaceAll("[^0-9a-z\\sA-Z/\\-]", "");

				Vector<String> sensewmc = new Vector<String>();
				Vector<Double> sensewmp = new Vector<Double>();
				for (Entry<String, Vector<String>> e : WikiToFreebaseIDMap.getInstance().mentionEntityMap.entrySet()) {

					if (e.getKey().equalsIgnoreCase((words))) {
						System.out.println("found in entity.tsv : " + e.getValue().size());

						for (int count = 0;count < e.getValue().size();++count) {
							sensewmc.add(e.getValue().get(count));
							sensewmp.add(new Double(0.7));
						}
					}
				}

				if(sensewmc.size() > 0){
					senses = new LabelSense();

					senses.wikiMinerCandidate = new String[sensewmc.size()];
					senses.wikiMinerProbability = new double[sensewmc.size()];
				}

				for(int scount=0;scount<sensewmc.size();++scount){
					senses.wikiMinerCandidate[scount] = sensewmc.elementAt(scount);
					senses.wikiMinerProbability[scount] = sensewmp.elementAt(scount);
				}

				if (senses != null) {
					Mention new_mention = kw.new Mention();
					new_mention.name = words;
					new_mention.length = new_mention.name.length();
					new_mention.offset = curr_offset;
					new_mention.context = kw.getContext(curr_offset,
							new_mention.length, kw.contextSize);
					new_mention.contextAroundMention = kw.getContext(curr_offset,new_mention.length, 10);
					if (k == 0)
						new_mention.key = mentions.get(i).key;
					new_mention.senses = senses;

					System.out.println("entity.tsv candidate for : " +  new_mention.name);
					for(int ic=0;ic<senses.wikiMinerCandidate.length;++ic){
						System.out.println("\t" + senses.wikiMinerCandidate[ic] + "  " + senses.wikiMinerProbability[ic]);
					}

					kw.keywords.add(new_mention);
					//System.out.println("new_mention offset + length : " + new_mention.offset + " " + new_mention.length);
					if (!kw.isArticleToken(curr_mention)) {
						for (int j = i; j < mentions.size()
						&& mentions.get(j).offset < (new_mention.offset + new_mention.length); j++)
							token_type[j] = 1;
					} else {
						token_type[i] = 2;
					}
					break;
				}
			}

//			if (token_type[i] == 0 && !kw.isArticleToken(curr_mention) && kw.isValidToken(curr_mention)) {
//				kw.keywords.add(mentions.get(i));
//			}
		}
	}
}
