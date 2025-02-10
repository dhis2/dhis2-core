package org.hisp.dhis.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StringUtilsTest {

  @ParameterizedTest
  @MethodSource("stringReplaceParams")
  @DisplayName("String char replacement should have expected outcome")
  void stringCharsReplaceTest(
      String original, String matchingChars, String replaceWith, String expectedString) {
    assertEquals(
        expectedString, StringUtils.replaceAllRecursively(original, matchingChars, replaceWith));
  }

  private static Stream<Arguments> stringReplaceParams() {
    return Stream.of(
        Arguments.of("test/this/string", "//", "/", "test/this/string"),
        Arguments.of("test//this/string", "//", "/", "test/this/string"),
        Arguments.of("test//this//string", "//", "/", "test/this/string"),
        Arguments.of("/test//this//string", "//", "/", "/test/this/string"),
        Arguments.of("//test/this/string//", "//", "/", "/test/this/string/"),
        Arguments.of("///////test/////this/string////", "//", "/", "/test/this/string/"),
        Arguments.of("////test//this////string", "//", "/", "/test/this/string"),
        Arguments.of("test/this/string///////", "//", "/", "test/this/string/"));
  }
}
