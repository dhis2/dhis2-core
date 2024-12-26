/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.test.utils;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.ErrorCodeException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.junit.jupiter.api.function.Executable;

/**
 * @author Jan Bernitt
 */
public final class Assertions {
  private Assertions() {
    throw new UnsupportedOperationException("util");
  }

  /**
   * Asserts that the given collection contains exactly the given items in any order.
   *
   * @param <E> the type.
   * @param expected the expected items.
   * @param actual the actual collection.
   */
  public static <E> void assertContainsOnly(Collection<E> expected, Collection<E> actual) {
    assertContainsOnly(expected, actual, "assertContainsOnly found mismatch");
  }

  /**
   * Asserts that the given collection contains exactly the given items in any order.
   *
   * @param <E> the type.
   * @param expected the expected items.
   * @param actual the actual collection.
   * @param heading the assertAll heading
   */
  public static <E> void assertContainsOnly(
      Collection<E> expected, Collection<E> actual, String heading) {
    assertNotNull(
        actual,
        () -> String.format("Expected collection to contain %s, got null instead", expected));

    List<E> missing = CollectionUtils.difference(expected, actual);
    List<E> extra = CollectionUtils.difference(actual, expected);
    assertAll(
        heading,
        () ->
            assertTrue(
                missing.isEmpty(), () -> String.format("Expected %s to be in %s", missing, actual)),
        () ->
            assertTrue(
                extra.isEmpty(), () -> String.format("Expected %s NOT to be in %s", extra, actual)),
        () ->
            assertEquals(
                expected.size(),
                actual.size(),
                () ->
                    String.format(
                        "Expected %s or actual %s contains unexpected duplicates",
                        expected, actual)));
  }

  /**
   * Asserts that two maps contain all the same entries.
   *
   * @param <K> the map key type.
   * @param <V> the map value type.
   * @param expected the expected map.
   * @param actual the actual map.
   */
  public static <K, V> void assertMapEquals(Map<K, V> expected, Map<K, V> actual) {
    assertContainsOnly(expected.entrySet(), actual.entrySet());
  }

  /**
   * Asserts that execution of the given executable throws an exception of the expected type,
   * returns the exception and that the error code of the exception equals the given error code.
   *
   * @param <K>
   * @param expectedType the expected type.
   * @param errorCode the {@link ErrorCode}.
   * @param executable the {@link Executable}.
   */
  public static <K extends ErrorCodeException> void assertThrowsErrorCode(
      Class<K> expectedType, ErrorCode errorCode, Executable executable) {
    K ex = assertThrows(expectedType, executable);

    assertEquals(errorCode, ex.getErrorCode());
  }

  /**
   * Asserts that the given collection is not null and empty.
   *
   * @param actual the collection.
   */
  public static void assertIsEmpty(Collection<?> actual) {
    assertNotNull(actual);
    assertTrue(actual.isEmpty(), actual.toString());
  }

  /**
   * Asserts that the given collection is not null and empty.
   *
   * @param actual the collection.
   * @param message fails with this message
   */
  public static void assertIsEmpty(Collection<?> actual, String message) {
    assertNotNull(actual, message);
    assertTrue(actual.isEmpty(), message);
  }

  /**
   * Asserts that the given collection is not null and not empty.
   *
   * @param actual the collection.
   */
  public static void assertNotEmpty(Collection<?> actual) {
    assertNotNull(actual);
    assertFalse(actual.isEmpty(), "expected collection not to be empty");
  }

  /**
   * Asserts that the given collection is not null and not empty.
   *
   * @param actual the collection.
   * @param message fails with this message
   */
  public static void assertNotEmpty(Collection<?> actual, String message) {
    assertNotNull(actual, message);
    assertFalse(actual.isEmpty(), message);
  }

  /**
   * Asserts that the given collection contains the expected number of elements.
   *
   * @param actual the collection.
   */
  public static void assertHasSize(int expected, Collection<?> actual) {
    assertHasSize(
        expected,
        actual,
        () ->
            String.format(
                "expected collection to contain %d elements, it has %d instead: '%s'",
                expected, actual.size(), actual));
  }

  /**
   * Asserts that the given collection contains the expected number of elements.
   *
   * @param actual the collection.
   * @param messageSupplier fails with this supplied message
   */
  public static void assertHasSize(
      int expected, Collection<?> actual, Supplier<String> messageSupplier) {
    assert expected > 0 : "use assertIsEmpty";

    assertNotEmpty(actual);
    assertEquals(expected, actual.size(), messageSupplier);
  }

  /**
   * Asserts that the given collection contains the expected number of elements.
   *
   * @param actual the collection.
   * @param message fails with this message
   */
  public static void assertHasSize(int expected, Collection<?> actual, String message) {
    assert expected > 0 : "use assertIsEmpty";

    assertNotEmpty(actual);
    assertEquals(expected, actual.size(), message);
  }

  /**
   * Asserts that the given string starts with the expected prefix.
   *
   * @param expected expected prefix of actual string
   * @param actual actual string which should contain the expected prefix
   */
  public static void assertStartsWith(String expected, String actual) {
    assertNotNull(
        actual,
        () -> String.format("expected string to start with '%s', got null instead", expected));
    assertTrue(
        actual.startsWith(expected),
        () ->
            String.format(
                "expected string to start with '%s', got '%s' instead", expected, actual));
  }

  /**
   * Asserts that the given string is not null, has a non-zero length, and contains non-whitespace
   * characters.
   *
   * @param actual the string.
   */
  public static void assertNotBlank(String actual) {
    assertNotNull(actual);
    assertTrue(!actual.isBlank());
  }

  /**
   * Asserts that the given string is not null and has a non-zero length.
   *
   * @param actual the string.
   */
  public static void assertNotEmpty(String actual) {
    assertNotNull(actual);
    assertTrue(!actual.isEmpty());
  }

  /**
   * Asserts that the given string is not null and has a non-zero length.
   *
   * @param actual the string.
   * @param message fails with this message
   */
  public static void assertNotEmpty(String actual, String message) {
    assertNotNull(actual, message);
    assertTrue(!actual.isEmpty(), message);
  }

  /**
   * Asserts that the given string is not null and has a non-zero length.
   *
   * @param actual the string.
   * @param messageSupplier fails with this supplied message
   */
  public static void assertNotEmpty(String actual, Supplier<String> messageSupplier) {
    assertNotNull(actual, messageSupplier);
    assertTrue(!actual.isEmpty(), messageSupplier);
  }

  /**
   * Asserts that the given character sequence is NOT contained within the actual string.
   *
   * @param expected expected character sequence not to be contained within the actual string
   * @param actual actual string which should not contain the expected character sequence
   */
  public static void assertNotContains(CharSequence expected, String actual) {
    assertNotEmpty(
        actual,
        () ->
            String.format(
                "expected actual NOT to contain '%s', use assertIsEmpty if that is what you expect",
                expected));
    assertFalse(
        actual.contains(expected),
        () ->
            String.format(
                "expected actual NOT to contain '%s', got '%s' instead", expected, actual));
  }

  /**
   * Asserts that the given character sequence is contained within the actual string.
   *
   * @param expected expected character sequence to be contained within the actual string
   * @param actual actual string which should contain the expected character sequence
   */
  public static void assertContains(CharSequence expected, String actual) {
    assertNotEmpty(actual, () -> String.format("expected actual to contain '%s'", expected));
    assertTrue(
        actual.contains(expected),
        () -> String.format("expected actual to contain '%s', got '%s' instead", expected, actual));
  }

  /**
   * Asserts that the given value is within the range of lower and upper bound (inclusive i.e.
   * [lower, upper]).
   *
   * @param lower lower bound
   * @param upper upper bound
   * @param actual actual value to be checked
   */
  public static void assertWithinRange(long lower, long upper, long actual) {
    assertTrue(
        lower < upper,
        () -> String.format("lower bound %d must be < than the upper bound %d", lower, upper));

    assertAll(() -> assertGreaterOrEqual(lower, actual), () -> assertLessOrEqual(upper, actual));
  }

  /**
   * Asserts that the given value is greater or equal than lower bound.
   *
   * @param lower lower bound
   * @param actual actual value to be checked
   */
  public static void assertGreaterOrEqual(long lower, long actual) {
    assertTrue(
        actual >= lower,
        () -> String.format("Expected actual %d to be >= than lower bound %d", actual, lower));
  }

  /**
   * Asserts that the given value is less or equal than upper bound.
   *
   * @param upper upper bound
   * @param actual actual value to be checked
   */
  public static void assertLessOrEqual(long upper, long actual) {
    assertTrue(
        actual <= upper,
        () -> String.format("Expected actual %d to be <= than upper bound %d", actual, upper));
  }

  /**
   * Compares 2 relative URLs for equality. That means they are functionally equivalent but their
   * parameters might occur in a different order.
   *
   * <p>Example of equivalent URLs:
   *
   * <pre>
   * /context/endpoint?a=b&c=d
   * /context/endpoint?c=d&a=b
   * </pre>
   *
   * @param expected the expected URL with path and optional parameters
   * @param actual the actual URL with path and optional parameters
   */
  public static void assertEquivalentRelativeUrls(String expected, String actual) {
    int paramsStart = expected.indexOf('?');
    if (paramsStart < 0) {
      assertEquals(expected, actual);
    } else {
      Function<String, List<String>> toParameterList =
          url -> {
            String params = url.substring(url.indexOf('?') + 1);
            return stream(params.split("&")).collect(toUnmodifiableList());
          };
      assertStartsWith(expected.substring(0, paramsStart + 1), actual);
      assertContainsOnly(toParameterList.apply(expected), toParameterList.apply(actual));
    }
  }

  /**
   * Asserts that the UID of the given UidObject is equal to the expected UID.
   *
   * @param expected expected UID
   * @param actual actual value to be checked
   */
  public static void assertEqualUids(@Nonnull UID expected, @Nonnull UidObject actual) {
    assertEquals(
        expected.getValue(),
        actual.getUid(),
        () ->
            String.format(
                "expected actual UID to be '%s', got '%s' instead",
                expected.getValue(), actual.getUid()));
  }

  public static void assertErrorReport(
      List<ErrorReport> actualErrors, ErrorCode expectedErrorCode) {
    assertErrorReport(actualErrors, expectedErrorCode, "");
  }

  public static void assertErrorReport(
      List<ErrorReport> actualErrors, ErrorCode expectedErrorCode, String expectedMessage) {
    assertFalse(
        actualErrors.isEmpty(), expectedMessage + " not found as error report list is empty");

    assertTrue(
        actualErrors.stream()
            .anyMatch(
                er ->
                    er.getMessage().contains(expectedMessage)
                        && er.getErrorCode() == expectedErrorCode),
        String.format(
            "Error report with code %s and and message '%s' not found in %s",
            expectedErrorCode, expectedMessage, actualErrors));
  }
}
