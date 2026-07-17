package app.roadrescue.util;

import java.security.SecureRandom;

public final class Ids {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

  private Ids() {
  }

  public static String cuid() {
    return "c" + Long.toString(System.currentTimeMillis(), 36) + randomPart(14);
  }

  private static String randomPart(int length) {
    var builder = new StringBuilder(length);
    for (int i = 0; i < length; i += 1) {
      builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
    }
    return builder.toString();
  }
}
