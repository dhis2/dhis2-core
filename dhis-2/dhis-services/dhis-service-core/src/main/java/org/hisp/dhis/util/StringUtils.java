package org.hisp.dhis.util;

import javax.annotation.Nonnull;

public class StringUtils {

  private StringUtils() {}

  /**
   * Replace all matchingChars with replaceWith chars recursively. See {@link
   * StringUtilsTest#stringCharsReplaceTest} for examples.
   *
   * @param string string to operate on
   * @param matchingChars matching chars to be replaced
   * @param replaceWith chars to replace matching chars
   * @return potentially-updated string
   */
  public static String replaceAllRecursively(
      @Nonnull String string, @Nonnull String matchingChars, @Nonnull String replaceWith) {
    if (!string.contains(matchingChars)) return string;
    return replaceAllRecursively(
        string.replace(matchingChars, replaceWith), matchingChars, replaceWith);
  }
}
