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
package org.hisp.dhis.analytics.event.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

public class EventAggregateTest extends AnalyticsApiTest {
  private final AnalyticsEventActions analyticsEventActions = new AnalyticsEventActions();

  @Test
  void testMetadataInfoForOptionSetForAggregate() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,C0aLZo75dgJ.B6TnnFMgmCk,C0aLZo75dgJ.Z1rLc1rVHK8,C0aLZo75dgJ.CklPZdOd6H1")
            .add("filter=C0aLZo75dgJ.vTKipVM0GsX,C0aLZo75dgJ.h5FuguPFF2j,C0aLZo75dgJ.aW66s2QSosT")
            .add("stage=C0aLZo75dgJ")
            .add("displayProperty=NAME")
            .add("outputType=ENROLLMENT")
            .add("totalPages=false");

    // When
    ApiResponse response = analyticsEventActions.aggregate().get("qDkgAbB5Jlk", JSON, JSON, params);
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(6)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("rows", hasSize(equalTo(0)))
        .body("metaData.items", hasKey("CklPZdOd6H1"))
        .body("metaData.items", hasKey("AZK4rjJCss5"))
        .body("metaData.items", hasKey("UrUdMteQzlT"));

    validateHeader(
        response,
        0,
        "Z1rLc1rVHK8",
        "Date of birth (mal) is estimated",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response, 1, "CklPZdOd6H1", "Sex", "TEXT", "java.lang.String", false, true, "hiQ3QFheQ3O");
    validateHeader(
        response,
        2,
        "B6TnnFMgmCk",
        "Age (years)",
        "INTEGER_ZERO_OR_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(response, 3, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 5, "value", "Value", "NUMBER", "java.lang.Double", false, false);
  }
}
