package me.kenfinnigan.lambda.util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

public final class TokenUtil {
  public static String generateCustomerId() {
    return "c_" + UUID.randomUUID().toString().substring(0, 13);
  }

  public static String generateDeviceToken(String customerId, String deviceId) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
      md.update(customerId.getBytes());
      md.update(deviceId.getBytes());

      ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
      buffer.putLong(System.currentTimeMillis());
      md.update(buffer.array());

      return bytesToHex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      String[] parts = { customerId, deviceId, System.currentTimeMillis() + "" };
      return Arrays.hashCode(parts) + "";
    }
  }

  private static String bytesToHex(byte[] hash) {
    StringBuffer hexString = new StringBuffer(2 * hash.length);

    for (int i = 0; i < hash.length; i++) {
      hexString.append(Integer.toHexString(0xFF & hash[i]));
    }

    return hexString.toString();
  }

  private TokenUtil() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
