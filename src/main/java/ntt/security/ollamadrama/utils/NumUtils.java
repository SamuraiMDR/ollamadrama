package ntt.security.ollamadrama.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;

public class NumUtils {
	
	private static final Random RANDOM = new Random();
    
    public static int randomNumWithinRangeAsInt(final int min, final int max) {
        if (max == min) {
            return min;
        }
        if (max < min) {
            return min; // default to max value
        }
        int diff = max - min;
        int randValue = RANDOM.nextInt(diff + 1) + min;
        return randValue;
    }

}
