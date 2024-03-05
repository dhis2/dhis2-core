/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.tei;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowContext;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsTeiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/trackedEntities/query" endpoint. */
public class TeiQuery2AutoTest extends AnalyticsApiTest {
  private AnalyticsTeiActions actions = new AnalyticsTeiActions();

  @Test
  public void queryTrackedentityquerywithrowcontext5() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "headers=IpHINAT79UW.A03MvHHogjR[1].bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR[2].bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR[0].bx6fsa0t90x,created")
            .add("lastUpdated=LAST_5_YEARS")
            .add("pageSize=3")
            .add(
                "dimension=IpHINAT79UW.A03MvHHogjR[1].bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR[2].bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR[0].bx6fsa0t90x")
            .add("desc=created")
            .add("relativePeriodDate=2016-01-08");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":3,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"name\":\"MCH BCG dose, Child Programme, Birth\"},\"bx6fsa0t90x\":{\"name\":\"MCH BCG dose\"},\"2015\":{\"name\":\"2015\"},\"pe\":{\"name\":\"Period\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"2014\":{\"name\":\"2014\"},\"2013\":{\"name\":\"2013\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2012\":{\"name\":\"2012\"},\"2011\":{\"name\":\"2011\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"2011\",\"2012\",\"2013\",\"2014\",\"2015\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "IpHINAT79UW.A03MvHHogjR[1].bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth (1)",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        1,
        "IpHINAT79UW.A03MvHHogjR[2].bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth (2)",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        2,
        "IpHINAT79UW.A03MvHHogjR[0].bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
            response,
            3,
            "created",
            "Created",
            "DATETIME",
            "java.time.LocalDateTime",
            false,
            true);

    // Assert row context
    validateRowContext(response, 0, 0, "ND");
    validateRowContext(response, 0, 1, "ND");
    validateRowContext(response, 0, 2, "ND");
    validateRowContext(response, 1, 1, "ND");
    validateRowContext(response, 2, 1, "ND");

    // Assert rows.
    validateRow(response, 0, List.of("", "", "", "2015-10-14 14:18:23.02"));
    validateRow(response, 1, List.of("1", "", "1", "2015-08-07 15:47:29.301"));
    validateRow(response, 2, List.of("0", "", "0", "2015-08-07 15:47:29.3"));
  }
}
