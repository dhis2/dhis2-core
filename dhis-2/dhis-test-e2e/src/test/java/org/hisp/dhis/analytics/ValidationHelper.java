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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.hisp.dhis.test.e2e.dto.ApiResponse;

/**
 * Helper class to assist during the validation/assertion in e2e analytics tests.
 *
 * @author maikel arabori
 */
public class ValidationHelper {
  private ValidationHelper() {}

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
