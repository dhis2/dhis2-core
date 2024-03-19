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
package org.hisp.dhis.analytics.dataitems;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.dataitem.DataItemActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test cases related to GET "dataItems" endpoint. The tests and assertions are based on the file
 * "setup/metadata.json" => "programIndicators", "dataElements".
 *
 * <p>The test cases using default pagination will imply "paging=true", which is the default when
 * "paging" is omitted.
 *
 * @author maikel arabori
 */
@Tag("category:analytics")
public class DataItemQueryTests extends ApiTest {
  private static final int OK = 200;

  private static final int CONFLICT = 409;

  private DataItemActions dataItemActions;

  @BeforeAll
  public void before() {
    dataItemActions = new DataItemActions();
    login();
  }

  @Test
  public void testGetAllDataItemsUsingDefaultPagination() {
    // When
    ApiResponse response = dataItemActions.get();

    // Then
    response.validate().statusCode(is(OK));
    response.validate().body("pager", isA(Object.class));
    response.validate().body("dataItems", is(not(empty())));
    response
        .validate()
        .body(
            "dataItems.dimensionItemType",
            (anyOf(hasItem("PROGRAM_INDICATOR"), hasItem("DATA_ELEMENT"))));
  }

  @Test
  public void testGetAllDataItemsWithoutPagination() {
    // Given
    String noPagination = "?paging=false";

    // When
    ApiResponse response = dataItemActions.get(noPagination);

    // Then
    response.validate().statusCode(is(OK));
    response.validate().body("pager", is(nullValue()));
    response.validate().body("dataItems", is(not(empty())));
    response.validate().body("dataItems.dimensionItemType", hasItem("PROGRAM_INDICATOR"));
    response.validate().body("dataItems.dimensionItemType", hasItem("DATA_ELEMENT"));
  }

  @Test
  public void testGetAllDataItemsUsingDefaultPaginationOrderedByCode() {
    // When
    ApiResponse response = dataItemActions.get("?order=name:asc");

    // Then
    response.validate().statusCode(is(OK));
    response.validate().body("pager", isA(Object.class));
    response.validate().body("dataItems", is(not(empty())));
    response.validate().body("dataItems.code", hasItem("AAAAAAA-1234"));
  }

  @Test
  public void testFilterByDimensionTypeUsingDefaultPagination() {
    // Given
    String theDimensionType = "PROGRAM_INDICATOR";
    String theUrlParams = "?filter=dimensionItemType:in:[%s]";

    // When
    ApiResponse response = dataItemActions.get(format(theUrlParams, theDimensionType));

    // Then
    response.validate().statusCode(is(OK));
    response.validate().body("pager", isA(Object.class));
    response.validate().body("dataItems", is(not(empty())));
    response.validate().body("dataItems.dimensionItemType", everyItem(is(theDimensionType)));
  }

  @Test
  public void testFilterUsingInvalidDimensionTypeUsingDefaultPagination() {
    // Given
    String anyInvalidDimensionType = "INVALID_TYPE";
    String theUrlParams = "?filter=dimensionItemType:in:[%s]";

    // When
    ApiResponse response = dataItemActions.get(format(theUrlParams, anyInvalidDimensionType));

    // Then
    response.validate().statusCode(is(CONFLICT));
    response.validate().body("pager", is(nullValue()));
    response.validate().body("httpStatus", is("Conflict"));
    response.validate().body("httpStatusCode", is(CONFLICT));
    response.validate().body("status", is("ERROR"));
    response.validate().body("errorCode", is("E2016"));
    response
        .validate()
        .body(
            "message",
            containsString(
                "Unable to parse element `"
                    + anyInvalidDimensionType
                    + "` on filter `dimensionItemType`, the available values are:"));
  }

  @Test
  public void testWhenDataIsNotFoundUsingDefaultPagination() {
    // Given
    String theDimensionType = "PROGRAM_INDICATOR";
    String aNonExistingName = "non-existing-Name";
    String aValidFilteringAttribute = "name";
    String theUrlParams =
        "?filter=dimensionItemType:in:[%s]&filter=" + aValidFilteringAttribute + ":ilike:%s";

    // When
    ApiResponse response =
        dataItemActions.get(format(theUrlParams, theDimensionType, aNonExistingName));

    // Then
    response.validate().statusCode(is(OK));
    response.validate().body("dataItems", is(empty()));
  }

  @Test
  public void testFilterByProgramUsingNonexistentAttributeAndDefaultPagination() {
    // Given
    String theDimensionType = "PROGRAM_INDICATOR";
    String theProgramId = Constants.EVENT_PROGRAM_ID;
    String aNonExistingAttr = "nonExistingAttr";
    String theUrlParams =
        "?filter=dimensionItemType:in:[%s]&filter=" + aNonExistingAttr + ":eq:%s&order=code:asc";

    // When
    ApiResponse response =
        dataItemActions.get(format(theUrlParams, theDimensionType, theProgramId));

    // Then
    response.validate().statusCode(is(CONFLICT));
    response.validate().body("pager", is(nullValue()));
    response.validate().body("httpStatus", is("Conflict"));
    response.validate().body("httpStatusCode", is(CONFLICT));
    response.validate().body("status", is("ERROR"));
    response.validate().body("errorCode", is("E2034"));
    response
        .validate()
        .body("message", containsString("Filter not supported: `" + aNonExistingAttr + "`"));
  }

  @Test
  public void testWhenFilteringByNonExistingNameWithoutPagination() {
    // Given
    String theDimensionType = "PROGRAM_INDICATOR";
    String aNonExistingName = "non-existing-name";
    String theUrlParams = "?filter=dimensionItemType:in:[%s]&filter=name:ilike:%s&paging=false";

    // When
    ApiResponse response =
        dataItemActions.get(format(theUrlParams, theDimensionType, aNonExistingName));

    // Then
    response.validate().statusCode(is(OK));
    response.validate().body("pager", is(nullValue()));
    response.validate().body("dataItems", is(empty()));
  }

  private void login() {
    new LoginActions().loginAsSuperUser();
  }
}
