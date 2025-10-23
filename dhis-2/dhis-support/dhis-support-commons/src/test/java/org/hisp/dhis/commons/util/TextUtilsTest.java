/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.commons.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.commons.util.TextUtils.removeAnyTrailingSlash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.hisp.dhis.util.MapBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Lars Helge Overland
 */
class TextUtilsTest {

  private static final String STRING = "abcdefghij";

  enum Disease {
    ANTIBIOTIC_RESISTANT_INFECTION,
    MALARIA,
    CHRONIC_WASTING_DISEASE
  }

  @Test
  public void testRemoveNonEssentialChars() {
    String same = "abcdefghijkl-";
    assertEquals(same, TextUtils.removeNonEssentialChars(same));

    assertEquals("abcdefghijkl-", TextUtils.removeNonEssentialChars("abcdefghijkl-øæå"));
    assertEquals("abcdefghijkl-", TextUtils.removeNonEssentialChars("abcdefghijkl-øæå§!"));
    assertEquals(" abcdefghijkl-", TextUtils.removeNonEssentialChars(" abcdefghijkl-øæå§!"));
    assertEquals(
        " abcde fghijkl-", TextUtils.removeNonEssentialChars("/(/%å^{} abcde fghijkl-øæå§!&"));
  }

  @Test
  void testSubString() {
    assertEquals("abcdefghij", TextUtils.subString(STRING, 0, 10));
    assertEquals("cdef", TextUtils.subString(STRING, 2, 4));
    assertEquals("ghij", TextUtils.subString(STRING, 6, 4));
    assertEquals("ghij", TextUtils.subString(STRING, 6, 6));
    assertEquals("", TextUtils.subString(STRING, 11, 3));
    assertEquals("j", TextUtils.subString(STRING, 9, 1));
    assertEquals("", TextUtils.subString(STRING, 4, 0));
  }

  @Test
  void testGetTokens() {
    assertEquals(
        List.of("John", "Doe", "Main", "Road", "25"), TextUtils.getTokens("John Doe Main Road 25"));
    assertEquals(
        List.of("Ted,Johnson", "Upper-Road", "45"),
        TextUtils.getTokens("Ted,Johnson Upper-Road 45"));
  }

  @Test
  void testRemoveLastOr() {
    assertEquals(null, TextUtils.removeLastOr(null));
    assertEquals("", TextUtils.removeLastOr(""));
    assertEquals(
        "or name='tom' or name='john' ", TextUtils.removeLastOr("or name='tom' or name='john' or"));
    assertEquals(
        "or name='tom' or name='john' ",
        TextUtils.removeLastOr("or name='tom' or name='john' or "));
    assertEquals(
        "or name='tom' or name='john' ",
        TextUtils.removeLastOr("or name='tom' or name='john' or  "));
  }

  @Test
  void testRemoveLastAnd() {
    assertEquals(null, TextUtils.removeLastAnd(null));
    assertEquals("", TextUtils.removeLastAnd(""));
    assertEquals(
        "and name='tom' and name='john' ",
        TextUtils.removeLastAnd("and name='tom' and name='john' and"));
    assertEquals(
        "and name='tom' and name='john' ",
        TextUtils.removeLastAnd("and name='tom' and name='john' and "));
    assertEquals(
        "and name='tom' and name='john' ",
        TextUtils.removeLastAnd("and name='tom' and name='john' and  "));
  }

  @Test
  void testRemoveLastComma() {
    String nullValue = null;

    assertEquals(null, TextUtils.removeLastComma(nullValue));
    assertEquals("", TextUtils.removeLastComma(""));
    assertEquals("tom", TextUtils.removeLastComma("tom"));
    assertEquals("tom,john", TextUtils.removeLastComma("tom,john,"));
    assertEquals("tom, john", TextUtils.removeLastComma("tom, john, "));
    assertEquals("tom, john", TextUtils.removeLastComma("tom, john,  "));
  }

  @Test
  void testRemoveLastCommaStringBuilder() {
    StringBuilder nullValue = null;

    assertEquals(null, TextUtils.removeLastComma(nullValue));
    assertEquals("", TextUtils.removeLastComma(new StringBuilder()).toString());
    assertEquals("tom", TextUtils.removeLastComma(new StringBuilder("tom")).toString());
    assertEquals("tom,john", TextUtils.removeLastComma(new StringBuilder("tom,john,")).toString());
    assertEquals(
        "tom, john", TextUtils.removeLastComma(new StringBuilder("tom, john, ")).toString());
    assertEquals(
        "tom, john", TextUtils.removeLastComma(new StringBuilder("tom, john,  ")).toString());
  }

  @Test
  void testJoinReplaceNull() {
    assertEquals(
        "green-red-blue", TextUtils.join(Arrays.asList("green", "red", "blue"), "-", "[n/a]"));
    assertEquals(
        "green-[n/a]-blue", TextUtils.join(Arrays.asList("green", null, "blue"), "-", "[n/a]"));
    assertEquals(
        "green-red-[n/a]", TextUtils.join(Arrays.asList("green", "red", null), "-", "[n/a]"));
    assertEquals(
        "greenred[n/a]", TextUtils.join(Arrays.asList("green", "red", null), null, "[n/a]"));
    assertEquals("greenred", TextUtils.join(Arrays.asList("green", "red", null), null, null));
  }

  @Test
  void testGetPrettyClassName() {
    assertEquals("Array List", TextUtils.getPrettyClassName(ArrayList.class));
    assertEquals(
        "Abstract Sequential List", TextUtils.getPrettyClassName(AbstractSequentialList.class));
  }

  @Test
  void testGetPrettyEnumName() {
    assertEquals(
        "Antibiotic resistant infection",
        TextUtils.getPrettyEnumName(Disease.ANTIBIOTIC_RESISTANT_INFECTION));
    assertEquals(
        "Chronic wasting disease", TextUtils.getPrettyEnumName(Disease.CHRONIC_WASTING_DISEASE));
    assertEquals("Malaria", TextUtils.getPrettyEnumName(Disease.MALARIA));
  }

  @Test
  void testGetPrettyPropertyName() {
    assertEquals(
        "Tracker program page size", TextUtils.getPrettyPropertyName("trackerProgramPageSize"));
    assertEquals("Data values page size", TextUtils.getPrettyPropertyName("dataValuesPageSize"));
    assertEquals("Relative start", TextUtils.getPrettyPropertyName("relativeStart"));
  }

  @Test
  void testSplitSafe() {
    assertEquals("green", TextUtils.splitSafe("red-green-blue", "-", 1));
    assertEquals("green", TextUtils.splitSafe("red.green.blue", "\\.", 1));
    assertEquals("red", TextUtils.splitSafe("red-green-blue", "-", 0));
    assertEquals("blue", TextUtils.splitSafe("red-green-blue", "-", 2));
    assertNull(TextUtils.splitSafe("red-green-blue", "-", 3));
    assertNull(TextUtils.splitSafe("red-green-blue", "-", -2));
    assertNull(TextUtils.splitSafe("red-green-blue-", "-", 3));
  }

  @Test
  void testReplace() {
    assertEquals(
        "Welcome John Doe",
        TextUtils.replace(
            "Welcome ${first_name} ${last_name}",
            Map.of("first_name", "John", "last_name", "Doe")));
  }

  @Test
  void testReplaceMultiple() {
    assertEquals(
        "Hey John, my name is John",
        TextUtils.replace("Hey ${name}, my name is ${name}", Map.of("name", "John")));
  }

  @Test
  void testReplaceWithNull() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            TextUtils.replace(
                "Welcome ${first_name} ${last_name}",
                new MapBuilder<String, String>()
                    .put("first_name", "John")
                    .put("last_name", null)
                    .build()));
  }

  @Test
  void testReplaceVarargs() {
    assertEquals("Welcome John", TextUtils.replace("Welcome ${first_name}", "first_name", "John"));
  }

  @Test
  void testGetOptions() {
    assertEquals(List.of("uidA", "uidB"), TextUtils.getOptions("uidA;uidB"));
    assertEquals(List.of("uidA"), TextUtils.getOptions("uidA"));
    assertEquals(List.of(), TextUtils.getOptions(null));
  }

  @Test
  void testGetCommaDelimitedString() {
    assertThat(
        TextUtils.getCommaDelimitedString(List.of("1", "2", "3", "4", "5")), is("1, 2, 3, 4, 5"));
    assertThat(TextUtils.getCommaDelimitedString(List.of("1")), is("1"));
    assertThat(TextUtils.getCommaDelimitedString(null), is(""));
  }

  @Test
  void testToLinesUnix() {
    String string = "one,two,three\naa,bb,cc";
    List<String> lines = TextUtils.toLines(string);
    assertEquals(2, lines.size());
    assertEquals("one,two,three", lines.get(0));
    assertEquals("aa,bb,cc", lines.get(1));
  }

  @Test
  void testToLinesWindows() {
    String string = "one,two,three\r\naa,bb,cc";
    List<String> lines = TextUtils.toLines(string);
    assertEquals(2, lines.size());
    assertEquals("one,two,three", lines.get(0));
    assertEquals("aa,bb,cc", lines.get(1));
  }

  @Test
  void testRemoveTrailingSlash() {
    String strWithSlash = "/path/";
    String slashRemoved = removeAnyTrailingSlash(strWithSlash);
    assertEquals("/path", slashRemoved);
  }

  @Test
  void testRemoveNoTrailingSlash() {
    String strWithSlash = "/path";
    String slashRemoved = removeAnyTrailingSlash(strWithSlash);
    assertEquals("/path", slashRemoved);
  }

  @Test
  void testFormat() {
    assertEquals(
        "Found 2 items of type text", TextUtils.format("Found {} items of type {}", 2, "text"));
  }

  @Test
  void testEmptyIfFalse() {
    assertEquals("", TextUtils.emptyIfFalse("foo", false));
    assertEquals("foo", TextUtils.emptyIfFalse("foo", true));
  }

  @Test
  void testEmptyIfTrue() {
    assertEquals("", TextUtils.emptyIfTrue("foo", true));
    assertEquals("foo", TextUtils.emptyIfTrue("foo", false));
  }

  @Test
  void testGetVariableNames() {
    assertEquals(
        Set.of("animal", "target"),
        TextUtils.getVariableNames("The ${animal} jumped over the ${target}."));
  }

  @Test
  void testGetVariableNamesWithNullInput() {
    assertEquals(Set.of(), TextUtils.getVariableNames(null));
  }

  @Test
  void testSanitize() {
    Pattern pattern = Pattern.compile("[a-zA-Z\\s_]");

    assertEquals(
        "The algorithm decided to eat a vegetable",
        TextUtils.sanitize(pattern, "The algorithm decided to eat a vegetable", '_'));

    assertEquals(
        "The algor_thm deci_ed to e_t a veg_tab_e",
        TextUtils.sanitize(pattern, "The algor!thm deci&ed to e/t a veg#tab?e", '_'));
    assertEquals(
        "The algor_thm decided to _eat_ a _vegetable_",
        TextUtils.sanitize(pattern, "#The algor!!##thm decided to **eat** a *vegetable*", '_'));
    assertEquals(
        "The_ algorithm _decided_ to _eat_ a vegetable",
        TextUtils.sanitize(pattern, "(The) algorithm (decided) to (eat) a vegetable", '_'));
  }

  @ParameterizedTest
  @MethodSource("urlFormatParams")
  @DisplayName("URL formats are valid and cleaned")
  void urlFormatsTest(String baseUrl, String path, String expected) {
    String cleanValidUrl = TextUtils.cleanUrlPathOnly(baseUrl, path);
    assertEquals(expected, cleanValidUrl);
  }

  private static Stream<Arguments> urlFormatParams() {
    return Stream.of(
        Arguments.of(
            "http://dhis2.org/", "//path//to/resource/", "http://dhis2.org/path/to/resource/"),
        Arguments.of(
            "https://dhis2.org", "path//to///resource", "https://dhis2.org/path/to/resource"),
        Arguments.of(
            "https://dhis2.org/", "path/to/resource", "https://dhis2.org/path/to/resource"),
        Arguments.of(
            "https://dhis2.org",
            "////path//to///resource//",
            "https://dhis2.org/path/to/resource/"));
  }
}
