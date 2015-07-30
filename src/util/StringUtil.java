package util;

public class StringUtil {
	public static String asString(String[] arr, char delim) {
		StringBuilder builder = new StringBuilder();
		for (String s : arr)
			builder.append(s).append(delim).append(' ');
		return builder.toString();
	}

	public static String asString(String[] arr) {
		return asString(arr, ' ');
	}

}
