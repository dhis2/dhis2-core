/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.hisp.dhis.helpers.extensions.ConfigurationExtension;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This test class has to run before all tests, because the scenarios are testing errors related to
 * missing analytics tables. For this reason they have to run first (before analytics tables are
 * created), hence @Order(1).
 */
@Order(1)
@ExtendWith(ConfigurationExtension.class)
@Tag("analytics")
public class DataItemsAnalyticsTest {

  private final RestApiActions dataItemsActions = new RestApiActions("/dataItems");

  @BeforeAll
  public static void beforeAll() {
    new LoginActions().loginAsAdmin();
  }

  @Test
  void testDataItemsDoesNotContainOptionSets() {
    // Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("paging=false").add("order=name:asc");

    // When
    ApiResponse response = dataItemsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(equalTo(200))
        .body("dataItems.dimensionItemType", not(hasItem("PROGRAM_DATA_ELEMENT_OPTION")))
        .body("dataItems.dimensionItemType", not(hasItem("PROGRAM_ATTRIBUTE_OPTION")))
        .body("dataItems.dimensionItemType", hasItem("PROGRAM_INDICATOR"))
        .body("dataItems.dimensionItemType", hasItem("DATA_ELEMENT"))
        .body("dataItems.dimensionItemType", not(hasItem("OPTION_SET")));
  }

  @Test
  void testDataItemsContainsOptionSetId() {
    // Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("paging=false").add("order=name:asc");

    // When
    ApiResponse response = dataItemsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(equalTo(200))
        .body("dataItems.optionSetId", hasItem("SokRAajDrRz"));
  }

  @Test
  void testDataItemsFiltersNonExistingOptionSetId() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("paging=false")
            .add("order=name:asc")
            .add("filter=optionSetId:eq:ABCD");

    // When
    ApiResponse response = dataItemsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(equalTo(200))
        .body("dataItems.optionSetId", not(hasItem("SokRAajDrRz")))
        .body("dataItems.optionSetId", not(hasItem("ABCD")));
  }

  @Test
  void testDataItemsFiltersExistingOptionSetId() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("paging=false")
            .add("order=name:asc")
            .add("filter=optionSetId:eq:SokRAajDrRz");

    // When
    ApiResponse response = dataItemsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(equalTo(200))
        .body("dataItems.optionSetId", hasItem("SokRAajDrRz"));
  }

  @Test
  void testDataItems_PROGRAM_DATA_ELEMENT_OPTION() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("page=1")
            .add("paging=false")
            .add("order=name:asc")
            .add("filter=dimensionItemType:in:[PROGRAM_DATA_ELEMENT_OPTION]")
            .add("filter=displayName:ilike:bel");

    // When
    ApiResponse response = dataItemsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(equalTo(200))
        .body("dataItems", is(not(empty())))
        .body("dataItems", hasSize(25))
        .body("dataItems.dimensionItemType", not(hasItem("PROGRAM_ATTRIBUTE_OPTION")))
        .body("dataItems.dimensionItemType", everyItem(is("PROGRAM_DATA_ELEMENT_OPTION")))
        .body("dataItems.optionSetId", (allOf(hasItem("ynHtyLDVeJO"), hasItem("eUZ79clX7y1"))));
  }

  @Test
  void testDataItems_PROGRAM_ATTRIBUTE_OPTION() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("page=1")
            .add("paging=false")
            .add("order=name:asc")
            .add("filter=dimensionItemType:in:[PROGRAM_ATTRIBUTE_OPTION]")
            .add("filter=displayName:ilike:bel");

    // When
    ApiResponse response = dataItemsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(equalTo(200))
        .body("dataItems", is(not(empty())))
        .body("dataItems", hasSize(3))
        .body("dataItems.dimensionItemType", not(hasItem("PROGRAM_DATA_ELEMENT_OPTION")))
        .body("dataItems.dimensionItemType", everyItem(is("PROGRAM_ATTRIBUTE_OPTION")))
        .body("dataItems.optionSetId", (not(hasItem("eUZ79clX7y1"))))
        .body("dataItems.optionSetId", (allOf(hasItem("ynHtyLDVeJO"))))
        .body("dataItems.programId", (allOf(hasItem("qDkgAbB5Jlk"))));
  }

  @Test
  void testDataItems_PROGRAM_ATTRIBUTE_OPTION_and_PROGRAM_DATA_ELEMENT_OPTION() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("page=1")
            .add("paging=false")
            .add("order=name:asc")
            .add(
                "filter=dimensionItemType:in:[PROGRAM_ATTRIBUTE_OPTION,PROGRAM_DATA_ELEMENT_OPTION]")
            .add("filter=displayName:ilike:bel");

    // When
    ApiResponse response = dataItemsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(equalTo(200))
        .body("dataItems", is(not(empty())))
        .body("dataItems", hasSize(28))
        .body("dataItems.dimensionItemType", hasItem("PROGRAM_DATA_ELEMENT_OPTION"))
        .body("dataItems.dimensionItemType", hasItem("PROGRAM_ATTRIBUTE_OPTION"))
        .body("dataItems.optionSetId", (hasItem("eUZ79clX7y1")))
        .body("dataItems.optionSetId", (hasItem("ynHtyLDVeJO")))
        .body("dataItems.programId", (hasItem("qDkgAbB5Jlk")));
  }
}
