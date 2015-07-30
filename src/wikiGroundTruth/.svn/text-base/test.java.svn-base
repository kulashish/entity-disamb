package wikiGroundTruth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
public static void main(String[] args){
	Pattern pat=Pattern.compile("\\[\\[(.*?)\\]\\]");
    String temp="[[abcd|defg]] asdasd hakjs hdkjlashdlkjashla [[car|car1]]";
    Matcher m1=pat.matcher(temp);
    while(m1.find()){
            System.out.println(m1.group(0));
    }
}
}
