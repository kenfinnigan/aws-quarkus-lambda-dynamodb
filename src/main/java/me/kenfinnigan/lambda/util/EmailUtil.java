package me.kenfinnigan.lambda.util;

public final class EmailUtil {
  // OWASP Email Validation Regex - https://owasp.org/www-community/OWASP_Validation_Regex_Repository
  private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

  public static boolean isValidEmail(String email) {
    return email.matches(EMAIL_REGEX);
  }

  private EmailUtil() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
