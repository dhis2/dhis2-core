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
package org.hisp.dhis.analytics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import org.hisp.dhis.test.e2e.dto.ApiResponse;

/**
 * Helper class to assist during the validation/assertion in e2e analytics tests.
 *
 * @author maikel arabori
 */
public class ValidationHelper {
  private ValidationHelper() {}

  /**
   * Finds a header map by its 'name' property within the extracted list of headers.
   *
   * @param actualHeaders List of headers extracted from the response (e.g., using
   *     response.extract("headers")).
   * @param headerName The exact 'name' of the header to find.
   * @return The Map representing the found header.
   * @throws AssertionError if the header with the given name is not found.
   */
  public static Map<String, Object> getHeaderByName(
      List<Map<String, Object>> actualHeaders, String headerName) {
    return actualHeaders.stream()
        .filter(h -> headerName.equals(h.get("name")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Header with name '" + headerName + "' not found."));
  }

  /**
   * Finds the index of a header by its 'name' property within the extracted list of headers.
   *
   * @param actualHeaders List of headers extracted from the response.
   * @param headerName The exact 'name' of the header to find.
   * @return The 0-based index of the header.
   * @throws AssertionError if the header with the given name is not found.
   */
  public static int getHeaderIndexByName(
      List<Map<String, Object>> actualHeaders, String headerName) {
    OptionalInt indexOpt =
        IntStream.range(0, actualHeaders.size())
            .filter(i -> headerName.equals(actualHeaders.get(i).get("name")))
            .findFirst();

    return indexOpt.orElseThrow(
        () ->
            new AssertionError(
                "Header with name '" + headerName + "' not found, cannot determine index."));
  }

  /**
   * Validates the overall structure (header count, width) based on whether PostGIS columns are
   * expected.
   *
   * @param response The ApiResponse object.
   * @param expectPostgis True if PostGIS-specific headers (geometry, etc.) are expected, false
   *     otherwise.
   * @param expectedRowCount The expected number of rows (height).
   * @param expectedHeaderCountWithPostgis The expected header count/width when PostGIS is enabled.
   * @param expectedHeaderCountWithoutPostgis The expected header count/width when PostGIS is
   *     disabled.
   */
  public static void validateResponseStructure(
      ApiResponse response,
      boolean expectPostgis,
      int expectedRowCount,
      int expectedHeaderCountWithPostgis,
      int expectedHeaderCountWithoutPostgis) {
    int expectedSize =
        expectPostgis ? expectedHeaderCountWithPostgis : expectedHeaderCountWithoutPostgis;

    List<String> currentHeaders = (List) response.extract("headers");
    if (expectedSize != currentHeaders.size()) {
      fail(
          "Expected "
              + expectedSize
              + " headers, but got "
              + currentHeaders.size()
              + ". Headers: "
              + currentHeaders);
    }

    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(expectedRowCount))
        .body("height", equalTo(expectedRowCount))
        .body("width", equalTo(expectedSize))
        .body("headerWidth", equalTo(expectedSize));
  }

  /**
   * Asserts whether a header with the given name exists or does not exist in the response.
   *
   * @param actualHeaders List of headers extracted from the response.
   * @param headerName The name of the header to check.
   * @param shouldExist True if the header is expected to exist, false if it's expected to be
   *     absent.
   */
  public static void validateHeaderExistence(
      List<Map<String, Object>> actualHeaders, String headerName, boolean shouldExist) {
    boolean actuallyExists = actualHeaders.stream().anyMatch(h -> headerName.equals(h.get("name")));

    assertThat(
        "Expectation failed for header '" + headerName + "' existence.",
        actuallyExists,
        is(shouldExist));
  }

  /**
   * Validates the common properties of a specific header identified by its name.
   *
   * @param response The ApiResponse object.
   * @param actualHeaders List of headers extracted from the response.
   * @param headerName The exact 'name' of the header to validate.
   * @param expectedColumn The expected 'column' property value.
   * @param expectedValueType The expected 'valueType' property value.
   * @param expectedType The expected 'type' property value.
   * @param expectedHidden The expected 'hidden' property value.
   * @param expectedMeta The expected 'meta' property value.
   */
  public static void validateHeaderPropertiesByName(
      ApiResponse response,
      List<Map<String, Object>> actualHeaders,
      String headerName,
      String expectedColumn,
      String expectedValueType,
      String expectedType,
      boolean expectedHidden,
      boolean expectedMeta) {

    // Find the header first to ensure it exists before using index
    Map<String, Object> header = getHeaderByName(actualHeaders, headerName);
    int headerIndex = getHeaderIndexByName(actualHeaders, headerName);

    response
        .validate()
        .body("headers[" + headerIndex + "].name", equalTo(headerName))
        .body("headers[" + headerIndex + "].column", equalTo(expectedColumn))
        .body("headers[" + headerIndex + "].valueType", equalTo(expectedValueType))
        .body("headers[" + headerIndex + "].type", equalTo(expectedType))
        .body("headers[" + headerIndex + "].hidden", is(expectedHidden))
        .body("headers[" + headerIndex + "].meta", is(expectedMeta));
  }

  /**
   * Validates a specific cell value in the response rows, identifying the column by header name.
   *
   * @param response The ApiResponse object.
   * @param actualHeaders List of headers extracted from the response.
   * @param rowIndex The 0-based index of the row to validate.
   * @param headerName The name of the header defining the column to validate.
   * @param expectedValue The expected string value for the cell. Use "" for empty strings. Use null
   *     check separately if null is expected.
   */
  public static void validateRowValueByName(
      ApiResponse response,
      List<Map<String, Object>> actualHeaders,
      int rowIndex,
      String headerName,
      String expectedValue) {
    int colIndex = getHeaderIndexByName(actualHeaders, headerName);
    String jsonPath = String.format("rows[%d][%d]", rowIndex, colIndex);

    // Note: RestAssured might return numbers as BigDecimal/Integer.
    // Casting or using `equalTo(expectedValue)` might require adjustments
    // if the expected value is numeric. For simplicity, assuming string comparison works
    // for most cases or expectedValue is already formatted as a string.
    response.validate().body(jsonPath, equalTo(expectedValue));
  }

  /**
   * Validate/assert all attributes of the given header (represented by the index), matching each
   * argument with its respective header attribute value.
   *
   * @param response
   * @param headerIndex of the header
   * @param name
   * @param column
   * @param valueType
   * @param type
   * @param hidden
   * @param meta
   */
  public static void validateHeader(
      ApiResponse response,
      int headerIndex,
      String name,
      String column,
      String valueType,
      String type,
      boolean hidden,
      boolean meta) {
    response
        .validate()
        .body("headers[" + headerIndex + "].name", equalTo(name))
        .body("headers[" + headerIndex + "].column", equalTo(column))
        .body("headers[" + headerIndex + "].valueType", equalTo(valueType))
        .body("headers[" + headerIndex + "].type", equalTo(type))
        .body("headers[" + headerIndex + "].hidden", is(hidden))
        .body("headers[" + headerIndex + "].meta", is(meta));
  }

  /**
   * Validate/assert all attributes of the given header (represented by the index), matching each
   * argument with its respective header attribute value.
   *
   * @param response
   * @param headerIndex of the header
   * @param name
   * @param column
   * @param valueType
   * @param type
   * @param hidden
   * @param meta
   * @param optionSet
   */
  public static void validateHeader(
      ApiResponse response,
      int headerIndex,
      String name,
      String column,
      String valueType,
      String type,
      boolean hidden,
      boolean meta,
      String optionSet) {
    response
        .validate()
        .body("headers[" + headerIndex + "].name", equalTo(name))
        .body("headers[" + headerIndex + "].column", equalTo(column))
        .body("headers[" + headerIndex + "].valueType", equalTo(valueType))
        .body("headers[" + headerIndex + "].type", equalTo(type))
        .body("headers[" + headerIndex + "].hidden", is(hidden))
        .body("headers[" + headerIndex + "].meta", is(meta))
        .body("headers[" + headerIndex + "].optionSet", is(optionSet));
  }

  /**
   * Validate/assert all attributes of the given header (represented by the index), matching each
   * argument with its respective header attribute value.
   *
   * @param response
   * @param headerIndex
   * @param name
   * @param column
   * @param valueType
   * @param type
   * @param hidden
   * @param meta
   * @param programStage
   * @param repeatableStageParams
   * @param stageOffset
   */
  public static void validateHeader(
      ApiResponse response,
      int headerIndex,
      String name,
      String column,
      String valueType,
      String type,
      boolean hidden,
      boolean meta,
      String programStage,
      String repeatableStageParams,
      int stageOffset) {
    response
        .validate()
        .body("headers[" + headerIndex + "].name", equalTo(name))
        .body("headers[" + headerIndex + "].column", equalTo(column))
        .body("headers[" + headerIndex + "].valueType", equalTo(valueType))
        .body("headers[" + headerIndex + "].type", equalTo(type))
        .body("headers[" + headerIndex + "].hidden", is(hidden))
        .body("headers[" + headerIndex + "].programStage", equalTo(programStage))
        .body("headers[" + headerIndex + "].repeatableStageParams", equalTo(repeatableStageParams))
        .body("headers[" + headerIndex + "].stageOffset", equalTo(stageOffset));
  }

  /**
   * Validate/assert all attributes of the given rowContext (represented by the row and column
   * index), matching each argument with its respective repeatableStageValueStatus value.
   *
   * @param response
   * @param rowIndex
   * @param colIndex
   * @param valueStatus
   */
  public static void validateRowContext(
      ApiResponse response, int rowIndex, int colIndex, String valueStatus) {
    response
        .validate()
        .body("rowContext." + rowIndex + "." + colIndex + ".valueStatus", equalTo(valueStatus));
  }

  /**
   * Validate/assert that all values of the given row are present in the given response.
   *
   * @param response
   * @param rowIndex
   * @param expectedValues
   */
  public static void validateRow(ApiResponse response, int rowIndex, List<String> expectedValues) {
    response.validate().body("rows[" + rowIndex + "]", equalTo(expectedValues));
  }

  /**
   * Validate/assert that all values of the given row are present in the given response.
   *
   * @param response
   * @param expectedValues
   */
  public static void validateRow(ApiResponse response, List<String> expectedValues) {
    response.validate().body("rows", hasItems(expectedValues));
  }

  /**
   * Validate/assert that a response contains a dataValue with the given properties.
   *
   * @param response The ApiResponse object.
   * @param dataElement The dataElement of the dataValue.
   * @param period The period of the dataValue.
   * @param orgUnit The orgUnit of the dataValue.
   * @param categoryOptionCombo The categoryOptionCombo of the dataValue.
   * @param value The value of the dataValue.
   */
  public static void validateDataValue(
      ApiResponse response,
      String dataElement,
      String period,
      String orgUnit,
      String categoryOptionCombo,
      String value) {
    response
        .validate()
        .body(
            "dataValues",
            hasItem(
                allOf(
                    hasEntry("dataElement", dataElement),
                    hasEntry("period", period),
                    hasEntry("orgUnit", orgUnit),
                    hasEntry("value", value),
                    hasEntry("categoryOptionCombo", categoryOptionCombo))));
  }
}
