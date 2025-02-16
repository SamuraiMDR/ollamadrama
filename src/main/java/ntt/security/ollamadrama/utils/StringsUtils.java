package ntt.security.ollamadrama.utils;

public class StringsUtils {

	public static String cutAndPadStringToN (String s, Integer n) {
		final Integer wantedLength = n;
		if (s.length() > wantedLength) {
			s = s.substring(0, Math.min(s.length(), wantedLength));
		} else if (s.length() == wantedLength) {
			// we are done
		} else {
			while (s.length() < wantedLength) {
				s = s + " ";
			}
		}
		return s;
	}
	
    public static String getLastNCharacters(String input, int n) {
        if (input == null || n <= 0) {
            return ""; // Return an empty string for invalid input or non-positive n
        }

        int length = input.length();
        if (n >= length) {
            return input; // Return the whole string if n is greater than or equal to its length
        }

        return input.substring(length - n); // Extract and return the last n characters
    }

}
