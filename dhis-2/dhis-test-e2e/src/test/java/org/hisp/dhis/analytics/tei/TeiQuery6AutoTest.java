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
public class TeiQuery6AutoTest extends AnalyticsApiTest {
  private AnalyticsTeiActions actions = new AnalyticsTeiActions();

  @Test
  public void singleOrgUnitLongitudeAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=RG7uGl4w5Jq")
            .add("headers=ouname,RG7uGl4w5Jq,lw1SqmMlnfh")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:jNb63DIHuwU,RG7uGl4w5Jq,lw1SqmMlnfh:GT:180")
            .add("relativePeriodDate=2024-06-13");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(9)))
        .body("height", equalTo(9))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"jNb63DIHuwU\":{\"uid\":\"jNb63DIHuwU\",\"code\":\"OU_573\",\"name\":\"Baoma Station CHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"jNb63DIHuwU\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "RG7uGl4w5Jq", "Longitude", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "lw1SqmMlnfh", "Height in cm", "NUMBER", "java.lang.Double", false, true);

    // Assert rows.
    validateRow(response, 0, List.of("Baoma Station CHP", "18.375857", "183.0"));
    validateRow(response, 1, List.of("Baoma Station CHP", "18.418267", "189.0"));
    validateRow(response, 2, List.of("Baoma Station CHP", "23.223017", "185.0"));
    validateRow(response, 3, List.of("Baoma Station CHP", "24.660945", "187.0"));
    validateRow(response, 4, List.of("Baoma Station CHP", "24.802831", "186.0"));
    validateRow(response, 5, List.of("Baoma Station CHP", "28.013589", "188.0"));
    validateRow(response, 6, List.of("Baoma Station CHP", "28.114683", "185.0"));
    validateRow(response, 7, List.of("Baoma Station CHP", "28.117988", "184.0"));
    validateRow(response, 8, List.of("Baoma Station CHP", "28.684907", "183.0"));
  }

  @Test
  public void singleOrgUnitInBooleanFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:tEgxbwwrwUd,WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h:IN:NV;1")
            .add("relativePeriodDate=2024-06-13");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"QWVRukwa83h\":{\"uid\":\"QWVRukwa83h\",\"code\":\"EP1_TRE_CCC\",\"name\":\"WHOMCH Contraceptive counselling provided\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h\":{\"uid\":\"QWVRukwa83h\",\"code\":\"EP1_TRE_CCC\",\"name\":\"WHOMCH Contraceptive counselling provided\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"tEgxbwwrwUd\":{\"uid\":\"tEgxbwwrwUd\",\"code\":\"OU_193232\",\"name\":\"Kayongoro MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"tEgxbwwrwUd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"QWVRukwa83h\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h",
        "WHOMCH Contraceptive counselling provided, WHO RMNCH Tracker, Postpartum care visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Kayongoro MCHP", "1"));
  }

  @Test
  public void singleOrgUnitProgramStatusFilterMultipleInBoolean() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h")
            .add(
                "headers=ouname,WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h,WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI,WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo,WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I,WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw,WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ,WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH,WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME,WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9,WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS,WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq,WSGAb5XwJ3Y.programstatus")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:tEgxbwwrwUd,WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h:IN:NV;0;1,WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI,WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo:IN:0,WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I:IN:1;NV,WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw,WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ,WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH:IN:0;1,WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME:IN:1,WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9,WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS,WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq")
            .add("programStatus=WSGAb5XwJ3Y.COMPLETED,WSGAb5XwJ3Y.ACTIVE")
            .add("relativePeriodDate=2024-06-13");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(15)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(15))
        .body("headerWidth", equalTo(15));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"FIHEeJwfhZH\":{\"uid\":\"FIHEeJwfhZH\",\"code\":\"EC_CLI_NEA\",\"name\":\"WHOMCH Maternal near-miss\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h\":{\"uid\":\"QWVRukwa83h\",\"code\":\"EP1_TRE_CCC\",\"name\":\"WHOMCH Contraceptive counselling provided\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo\":{\"uid\":\"DCUDZxqOxUo\",\"code\":\"EA9_TRE_BRE2\",\"name\":\"WHOMCH ECV performed\",\"description\":\"Persistent conversion from breech to cephalic presentation after 1 week.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46\":{\"uid\":\"csl3yq5UC46\",\"code\":\"LAB_WBC\",\"name\":\"WHOMCH White blood cell count\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"DecmCMPDPdS\":{\"uid\":\"DecmCMPDPdS\",\"code\":\"EA_EDD_ULS\",\"name\":\"WHOMCH Ultrasound estimate of due date\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I\":{\"uid\":\"w7enwqzx90I\",\"code\":\"EA8_CLI_ECL1\",\"name\":\"WHOMCH Eclamptic convulsions\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"QFX1FLWBwtq\":{\"uid\":\"QFX1FLWBwtq\",\"code\":\"MMD_ALL\",\"name\":\"WHOMCH Allergies (drugs and/or severe food allergies)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw\":{\"uid\":\"hib4oz2sOLw\",\"code\":\"EC_CLI_PPH\",\"name\":\"WHOMCH Estimated blood loss (ml)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"tEgxbwwrwUd\":{\"uid\":\"tEgxbwwrwUd\",\"code\":\"OU_193232\",\"name\":\"Kayongoro MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"w7enwqzx90I\":{\"uid\":\"w7enwqzx90I\",\"code\":\"EA8_CLI_ECL1\",\"name\":\"WHOMCH Eclamptic convulsions\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"Itl05OEupgQ\":{\"uid\":\"Itl05OEupgQ\",\"code\":\"LAB_HIV\",\"name\":\"WHOMCH HIV rapid test\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq\":{\"uid\":\"QFX1FLWBwtq\",\"code\":\"MMD_ALL\",\"name\":\"WHOMCH Allergies (drugs and/or severe food allergies)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"PFDfvmGpsR3\":{\"uid\":\"PFDfvmGpsR3\",\"name\":\"Care at birth\",\"description\":\"Intrapartum care / Childbirth / Labour and delivery\"},\"WZbXY0S00lP\":{\"uid\":\"WZbXY0S00lP\",\"name\":\"First antenatal care visit\",\"description\":\"First antenatal care visit\"},\"BmaBjPQX8ME\":{\"uid\":\"BmaBjPQX8ME\",\"code\":\"EC_TRE_PPH\",\"name\":\"WHOMCH Oxytocin given\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME\":{\"uid\":\"BmaBjPQX8ME\",\"code\":\"EC_TRE_PPH\",\"name\":\"WHOMCH Oxytocin given\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"m3XQrgadVK9\":{\"uid\":\"m3XQrgadVK9\",\"code\":\"EC4_TRE_HEM\",\"name\":\"WHOMCH Uterotonics given\",\"description\":\"If person was treated with uterotonics\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9\":{\"uid\":\"m3XQrgadVK9\",\"code\":\"EC4_TRE_HEM\",\"name\":\"WHOMCH Uterotonics given\",\"description\":\"If person was treated with uterotonics\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"DCUDZxqOxUo\":{\"uid\":\"DCUDZxqOxUo\",\"code\":\"EA9_TRE_BRE2\",\"name\":\"WHOMCH ECV performed\",\"description\":\"Persistent conversion from breech to cephalic presentation after 1 week.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46\":{\"uid\":\"csl3yq5UC46\",\"code\":\"LAB_WBC\",\"name\":\"WHOMCH White blood cell count\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS\":{\"uid\":\"DecmCMPDPdS\",\"code\":\"EA_EDD_ULS\",\"name\":\"WHOMCH Ultrasound estimate of due date\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"edqlbukwRfQ\":{\"uid\":\"edqlbukwRfQ\",\"name\":\"Second antenatal care visit\",\"description\":\"Antenatal care visit\"},\"hib4oz2sOLw\":{\"uid\":\"hib4oz2sOLw\",\"code\":\"EC_CLI_PPH\",\"name\":\"WHOMCH Estimated blood loss (ml)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"yTDoF5b1OhI\":{\"uid\":\"yTDoF5b1OhI\",\"code\":\"EA9_TRE_BRE3\",\"name\":\"WHOMCH ECV conversion remaining\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI\":{\"uid\":\"yTDoF5b1OhI\",\"code\":\"EA9_TRE_BRE3\",\"name\":\"WHOMCH ECV conversion remaining\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH\":{\"uid\":\"FIHEeJwfhZH\",\"code\":\"EC_CLI_NEA\",\"name\":\"WHOMCH Maternal near-miss\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"QWVRukwa83h\":{\"uid\":\"QWVRukwa83h\",\"code\":\"EP1_TRE_CCC\",\"name\":\"WHOMCH Contraceptive counselling provided\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"csl3yq5UC46\":{\"uid\":\"csl3yq5UC46\",\"code\":\"LAB_WBC\",\"name\":\"WHOMCH White blood cell count\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ\":{\"uid\":\"Itl05OEupgQ\",\"code\":\"LAB_HIV\",\"name\":\"WHOMCH HIV rapid test\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"FIHEeJwfhZH\":[],\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DecmCMPDPdS\":[],\"QFX1FLWBwtq\":[\"l8S7SjnQ58G\",\"rexqxNDqUKg\"],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"w7enwqzx90I\":[],\"kyIzQsj96BD\":[],\"Itl05OEupgQ\":[\"R7O031O2brW\",\"wrD98kfvt90\",\"hXmq4IJwz1k\"],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"BmaBjPQX8ME\":[],\"m3XQrgadVK9\":[],\"DCUDZxqOxUo\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"tEgxbwwrwUd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"hib4oz2sOLw\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"yTDoF5b1OhI\":[\"GxdJJFuMQ5i\",\"j72KsGIAe8h\",\"VqvXhoA5NgX\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"QWVRukwa83h\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"csl3yq5UC46\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h",
        "WHOMCH Contraceptive counselling provided, WHO RMNCH Tracker, Postpartum care visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        2,
        "WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI",
        "WHOMCH ECV conversion remaining, WHO RMNCH Tracker, Second antenatal care visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        3,
        "WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo",
        "WHOMCH ECV performed, WHO RMNCH Tracker, Second antenatal care visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        4,
        "WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I",
        "WHOMCH Eclamptic convulsions, WHO RMNCH Tracker, Second antenatal care visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        5,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw",
        "WHOMCH Estimated blood loss (ml), WHO RMNCH Tracker, Care at birth",
        "INTEGER_ZERO_OR_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response,
        6,
        "WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ",
        "WHOMCH HIV rapid test, WHO RMNCH Tracker, First antenatal care visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        7,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH",
        "WHOMCH Maternal near-miss, WHO RMNCH Tracker, Care at birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        8,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME",
        "WHOMCH Oxytocin given, WHO RMNCH Tracker, Care at birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        9,
        "WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46",
        "WHOMCH White blood cell count, WHO RMNCH Tracker, Postpartum care visit",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        10,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46",
        "WHOMCH White blood cell count, WHO RMNCH Tracker, Care at birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        11,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9",
        "WHOMCH Uterotonics given, WHO RMNCH Tracker, Care at birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        12,
        "WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS",
        "WHOMCH Ultrasound estimate of due date, WHO RMNCH Tracker, First antenatal care visit",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeader(
        response,
        13,
        "WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq",
        "WHOMCH Allergies (drugs and/or severe food allergies), WHO RMNCH Tracker, First antenatal care visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        14,
        "WSGAb5XwJ3Y.programstatus",
        "Program Status, WHO RMNCH Tracker",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Kayongoro MCHP", "0", "", "0", "1", "", "", "1", "1", "", "", "0", "", "", "ACTIVE"));
  }

  @Test
  public void multiOrgUnitProgramStatusFilterMultipleInBoolean() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=oucode")
            .add(
                "headers=ouname,oucode,WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h,WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI,WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo,WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I,WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw,WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ,WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH,WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME,WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9,WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS,WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq,WSGAb5XwJ3Y.programstatus,ur1Edk5Oe2n.ZkbAXlQUYJG.HmkXnHJxcD1,ur1Edk5Oe2n.ZkbAXlQUYJG.U5ubm6PPYrM,ur1Edk5Oe2n.ZkbAXlQUYJG.vAzDOljIN1o,ur1Edk5Oe2n.jdRD35YwbRH.yLIPuJHRgey,ur1Edk5Oe2n.EPEcjy3FWmI.lJTx9EZ1dk1,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO,IpHINAT79UW.ZzYYXq4fJie.HLmTEmupdX0,IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,uy2gU8kT1jF.Xgk8Wvl0jHr.Rbv6wcblbxe,uy2gU8kT1jF.eaDHS084uMp.utliJZmDeeC,uy2gU8kT1jF.eaDHS084uMp.pB5sL7Ts4fb,uy2gU8kT1jF.oRySG82BKE6.EzMxXuVww2z,uy2gU8kT1jF.grIfo3oOf4Y.gAbD3uDVHHh,uy2gU8kT1jF.eaDHS084uMp.OuJ6sgPyAbC,uy2gU8kT1jF.oRySG82BKE6.UXz7xuGCEhU,uy2gU8kT1jF.grIfo3oOf4Y.g9eOBujte1U,uy2gU8kT1jF.eaDHS084uMp.NALlPhMmMTQ,uy2gU8kT1jF.incidentdate,fDd25txQckK.lST1OZ5BDJ2.qpQinIDQ6Uy,fDd25txQckK.lST1OZ5BDJ2.fQMBEt42CSl,fDd25txQckK.lST1OZ5BDJ2.Mnkodq2wzlV,created")
            .add("created=LAST_10_YEARS")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=5")
            .add("page=1")
            .add(
                "dimension=ou:tEgxbwwrwUd;ObV5AR1NECl;Uwcj0mz78BV;nDwbwJZQUYU;OjTS752GbZE;mt47bcb0Rcj;ZxuSbAmsLCn;ImspTQPwCqd,WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h:IN:NV;0;1,WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI,WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo:IN:0,WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I,WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw,WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ,WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH,WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME:IN:1,WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9,WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS,WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq,ur1Edk5Oe2n.ZkbAXlQUYJG.HmkXnHJxcD1,ur1Edk5Oe2n.ZkbAXlQUYJG.U5ubm6PPYrM,ur1Edk5Oe2n.ZkbAXlQUYJG.vAzDOljIN1o,ur1Edk5Oe2n.jdRD35YwbRH.yLIPuJHRgey,ur1Edk5Oe2n.EPEcjy3FWmI.lJTx9EZ1dk1,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO,IpHINAT79UW.ZzYYXq4fJie.HLmTEmupdX0,IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,uy2gU8kT1jF.Xgk8Wvl0jHr.Rbv6wcblbxe,uy2gU8kT1jF.eaDHS084uMp.utliJZmDeeC,uy2gU8kT1jF.eaDHS084uMp.pB5sL7Ts4fb,uy2gU8kT1jF.oRySG82BKE6.EzMxXuVww2z,uy2gU8kT1jF.grIfo3oOf4Y.gAbD3uDVHHh,uy2gU8kT1jF.eaDHS084uMp.OuJ6sgPyAbC,uy2gU8kT1jF.oRySG82BKE6.UXz7xuGCEhU,uy2gU8kT1jF.grIfo3oOf4Y.g9eOBujte1U,uy2gU8kT1jF.eaDHS084uMp.NALlPhMmMTQ,fDd25txQckK.lST1OZ5BDJ2.qpQinIDQ6Uy,fDd25txQckK.lST1OZ5BDJ2.fQMBEt42CSl,fDd25txQckK.lST1OZ5BDJ2.Mnkodq2wzlV")
            .add("programStatus=WSGAb5XwJ3Y.COMPLETED,WSGAb5XwJ3Y.ACTIVE")
            .add("relativePeriodDate=2024-06-13");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(40)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(40))
        .body("headerWidth", equalTo(40));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":false},\"items\":{\"GQY2lXrypjO\":{\"uid\":\"GQY2lXrypjO\",\"code\":\"DE_2006099\",\"name\":\"MCH Infant Weight  (g)\",\"description\":\"Infant weight in grams\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46\":{\"uid\":\"csl3yq5UC46\",\"code\":\"LAB_WBC\",\"name\":\"WHOMCH White blood cell count\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I\":{\"uid\":\"w7enwqzx90I\",\"code\":\"EA8_CLI_ECL1\",\"name\":\"WHOMCH Eclamptic convulsions\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ObV5AR1NECl\":{\"uid\":\"ObV5AR1NECl\",\"code\":\"OU_193234\",\"name\":\"Karina MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"uy2gU8kT1jF.oRySG82BKE6.EzMxXuVww2z\":{\"uid\":\"EzMxXuVww2z\",\"code\":\"DE_2008144\",\"name\":\"MCH Iron/Folic\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"fDd25txQckK.lST1OZ5BDJ2.fQMBEt42CSl\":{\"uid\":\"fQMBEt42CSl\",\"code\":\"DE_217131\",\"name\":\"PFS End-of-training assessment - CAC MVA\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"PFDfvmGpsR3\":{\"uid\":\"PFDfvmGpsR3\",\"name\":\"Care at birth\",\"description\":\"Intrapartum care / Childbirth / Labour and delivery\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"BmaBjPQX8ME\":{\"uid\":\"BmaBjPQX8ME\",\"code\":\"EC_TRE_PPH\",\"name\":\"WHOMCH Oxytocin given\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9\":{\"uid\":\"m3XQrgadVK9\",\"code\":\"EC4_TRE_HEM\",\"name\":\"WHOMCH Uterotonics given\",\"description\":\"If person was treated with uterotonics\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"Xgk8Wvl0jHr\":{\"uid\":\"Xgk8Wvl0jHr\",\"name\":\"Delivery\",\"description\":\"Delivery phase\"},\"WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46\":{\"uid\":\"csl3yq5UC46\",\"code\":\"LAB_WBC\",\"name\":\"WHOMCH White blood cell count\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"UXz7xuGCEhU\":{\"uid\":\"UXz7xuGCEhU\",\"code\":\"DE_2005736\",\"name\":\"MCH Weight (g)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"hib4oz2sOLw\":{\"uid\":\"hib4oz2sOLw\",\"code\":\"EC_CLI_PPH\",\"name\":\"WHOMCH Estimated blood loss (ml)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"grIfo3oOf4Y\":{\"uid\":\"grIfo3oOf4Y\",\"name\":\"ANC Visit (2-4+)\",\"description\":\"ANC visits 2 to 4+\"},\"uy2gU8kT1jF.eaDHS084uMp.utliJZmDeeC\":{\"uid\":\"utliJZmDeeC\",\"code\":\"DE_2008125\",\"name\":\"MCH MUAC\",\"description\":\"Mid Upper Arm Circumference\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"fQMBEt42CSl\":{\"uid\":\"fQMBEt42CSl\",\"code\":\"DE_217131\",\"name\":\"PFS End-of-training assessment - CAC MVA\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ\":{\"uid\":\"Itl05OEupgQ\",\"code\":\"LAB_HIV\",\"name\":\"WHOMCH HIV rapid test\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n.ZkbAXlQUYJG.HmkXnHJxcD1\":{\"uid\":\"HmkXnHJxcD1\",\"code\":\"DE_1150454\",\"name\":\"TB Case Definition\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"FIHEeJwfhZH\":{\"uid\":\"FIHEeJwfhZH\",\"code\":\"EC_CLI_NEA\",\"name\":\"WHOMCH Maternal near-miss\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h\":{\"uid\":\"QWVRukwa83h\",\"code\":\"EP1_TRE_CCC\",\"name\":\"WHOMCH Contraceptive counselling provided\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"yLIPuJHRgey\":{\"uid\":\"yLIPuJHRgey\",\"code\":\"DE_859997\",\"name\":\"TB smear microscopy number of specimen\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"Uwcj0mz78BV\":{\"uid\":\"Uwcj0mz78BV\",\"code\":\"OU_193237\",\"name\":\"Manjoro MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw\":{\"uid\":\"hib4oz2sOLw\",\"code\":\"EC_CLI_PPH\",\"name\":\"WHOMCH Estimated blood loss (ml)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"tEgxbwwrwUd\":{\"uid\":\"tEgxbwwrwUd\",\"code\":\"OU_193232\",\"name\":\"Kayongoro MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"uy2gU8kT1jF.incidentdate\":{\"name\":\"LMP Date\",\"dimensionType\":\"PERIOD\"},\"gAbD3uDVHHh\":{\"uid\":\"gAbD3uDVHHh\",\"code\":\"DE_2005743\",\"name\":\"MCH Tetatus\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"Itl05OEupgQ\":{\"uid\":\"Itl05OEupgQ\",\"code\":\"LAB_HIV\",\"name\":\"WHOMCH HIV rapid test\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.ZzYYXq4fJie.HLmTEmupdX0\":{\"uid\":\"HLmTEmupdX0\",\"code\":\"DE_2006106\",\"name\":\"MCH Vit A\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"fDd25txQckK.lST1OZ5BDJ2.qpQinIDQ6Uy\":{\"uid\":\"qpQinIDQ6Uy\",\"code\":\"DE_217125\",\"name\":\"PFS Date of training (end of training)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"uy2gU8kT1jF.eaDHS084uMp.OuJ6sgPyAbC\":{\"uid\":\"OuJ6sgPyAbC\",\"code\":\"DE_2008126\",\"name\":\"MCH Visit Comment\",\"description\":\"Free text comment used to put additional information for a visit.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"mt47bcb0Rcj\":{\"uid\":\"mt47bcb0Rcj\",\"code\":\"OU_193231\",\"name\":\"Kamabai CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"DCUDZxqOxUo\":{\"uid\":\"DCUDZxqOxUo\",\"code\":\"EA9_TRE_BRE2\",\"name\":\"WHOMCH ECV performed\",\"description\":\"Persistent conversion from breech to cephalic presentation after 1 week.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"uy2gU8kT1jF.oRySG82BKE6.UXz7xuGCEhU\":{\"uid\":\"UXz7xuGCEhU\",\"code\":\"DE_2005736\",\"name\":\"MCH Weight (g)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"uy2gU8kT1jF.eaDHS084uMp.pB5sL7Ts4fb\":{\"uid\":\"pB5sL7Ts4fb\",\"code\":\"DE_2008154\",\"name\":\"MCH Mabendazole\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"uy2gU8kT1jF.grIfo3oOf4Y.gAbD3uDVHHh\":{\"uid\":\"gAbD3uDVHHh\",\"code\":\"DE_2005743\",\"name\":\"MCH Tetatus\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"pB5sL7Ts4fb\":{\"uid\":\"pB5sL7Ts4fb\",\"code\":\"DE_2008154\",\"name\":\"MCH Mabendazole\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"fDd25txQckK.lST1OZ5BDJ2.Mnkodq2wzlV\":{\"uid\":\"Mnkodq2wzlV\",\"code\":\"DE_217212\",\"name\":\"PFS Visit number\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"eaDHS084uMp\":{\"uid\":\"eaDHS084uMp\",\"name\":\"ANC 1st visit\",\"description\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH / PNC (Adult Woman)\"},\"csl3yq5UC46\":{\"uid\":\"csl3yq5UC46\",\"code\":\"LAB_WBC\",\"name\":\"WHOMCH White blood cell count\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"uy2gU8kT1jF.grIfo3oOf4Y.g9eOBujte1U\":{\"uid\":\"g9eOBujte1U\",\"code\":\"DE_2005735\",\"name\":\"MCH ANC Visit\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO\":{\"uid\":\"GQY2lXrypjO\",\"code\":\"DE_2006099\",\"name\":\"MCH Infant Weight  (g)\",\"description\":\"Infant weight in grams\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ZxuSbAmsLCn\":{\"uid\":\"ZxuSbAmsLCn\",\"code\":\"OU_193235\",\"name\":\"Kamasikie MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"QFX1FLWBwtq\":{\"uid\":\"QFX1FLWBwtq\",\"code\":\"MMD_ALL\",\"name\":\"WHOMCH Allergies (drugs and/or severe food allergies)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"w7enwqzx90I\":{\"uid\":\"w7enwqzx90I\",\"code\":\"EA8_CLI_ECL1\",\"name\":\"WHOMCH Eclamptic convulsions\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq\":{\"uid\":\"QFX1FLWBwtq\",\"code\":\"MMD_ALL\",\"name\":\"WHOMCH Allergies (drugs and/or severe food allergies)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"lST1OZ5BDJ2\":{\"uid\":\"lST1OZ5BDJ2\",\"name\":\"Provider Follow-up and Support Tool\",\"description\":\"Provider Follow-up and Support Tool\"},\"vAzDOljIN1o\":{\"uid\":\"vAzDOljIN1o\",\"code\":\"TB Previous use of second- line drugs\",\"name\":\"TB Previous use of second-line drugs\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"WZbXY0S00lP\":{\"uid\":\"WZbXY0S00lP\",\"name\":\"First antenatal care visit\",\"description\":\"First antenatal care visit\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"uid\":\"UXz7xuGCEhU\",\"code\":\"DE_2005736\",\"name\":\"MCH Weight (g)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"Mnkodq2wzlV\":{\"uid\":\"Mnkodq2wzlV\",\"code\":\"DE_217212\",\"name\":\"PFS Visit number\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"OuJ6sgPyAbC\":{\"uid\":\"OuJ6sgPyAbC\",\"code\":\"DE_2008126\",\"name\":\"MCH Visit Comment\",\"description\":\"Free text comment used to put additional information for a visit.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"oRySG82BKE6\":{\"uid\":\"oRySG82BKE6\",\"name\":\"PNC Visit\",\"description\":\"Post Natal Care Visit\"},\"WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI\":{\"uid\":\"yTDoF5b1OhI\",\"code\":\"EA9_TRE_BRE3\",\"name\":\"WHOMCH ECV conversion remaining\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"QWVRukwa83h\":{\"uid\":\"QWVRukwa83h\",\"code\":\"EP1_TRE_CCC\",\"name\":\"WHOMCH Contraceptive counselling provided\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n.jdRD35YwbRH.yLIPuJHRgey\":{\"uid\":\"yLIPuJHRgey\",\"code\":\"DE_859997\",\"name\":\"TB smear microscopy number of specimen\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"HmkXnHJxcD1\":{\"uid\":\"HmkXnHJxcD1\",\"code\":\"DE_1150454\",\"name\":\"TB Case Definition\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"OjTS752GbZE\":{\"uid\":\"OjTS752GbZE\",\"code\":\"OU_193229\",\"name\":\"Kagbankona MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"uy2gU8kT1jF.Xgk8Wvl0jHr.Rbv6wcblbxe\":{\"uid\":\"Rbv6wcblbxe\",\"code\":\"DE_2008177\",\"name\":\"MCH Condition of mother on discharge\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"NALlPhMmMTQ\":{\"uid\":\"NALlPhMmMTQ\",\"code\":\"DE_2005739\",\"name\":\"MCH ARVs\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo\":{\"uid\":\"DCUDZxqOxUo\",\"code\":\"EA9_TRE_BRE2\",\"name\":\"WHOMCH ECV performed\",\"description\":\"Persistent conversion from breech to cephalic presentation after 1 week.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"DecmCMPDPdS\":{\"uid\":\"DecmCMPDPdS\",\"code\":\"EA_EDD_ULS\",\"name\":\"WHOMCH Ultrasound estimate of due date\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"uy2gU8kT1jF.eaDHS084uMp.NALlPhMmMTQ\":{\"uid\":\"NALlPhMmMTQ\",\"code\":\"DE_2005739\",\"name\":\"MCH ARVs\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"HLmTEmupdX0\":{\"uid\":\"HLmTEmupdX0\",\"code\":\"DE_2006106\",\"name\":\"MCH Vit A\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME\":{\"uid\":\"BmaBjPQX8ME\",\"code\":\"EC_TRE_PPH\",\"name\":\"WHOMCH Oxytocin given\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"m3XQrgadVK9\":{\"uid\":\"m3XQrgadVK9\",\"code\":\"EC4_TRE_HEM\",\"name\":\"WHOMCH Uterotonics given\",\"description\":\"If person was treated with uterotonics\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS\":{\"uid\":\"DecmCMPDPdS\",\"code\":\"EA_EDD_ULS\",\"name\":\"WHOMCH Ultrasound estimate of due date\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"Rbv6wcblbxe\":{\"uid\":\"Rbv6wcblbxe\",\"code\":\"DE_2008177\",\"name\":\"MCH Condition of mother on discharge\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"qpQinIDQ6Uy\":{\"uid\":\"qpQinIDQ6Uy\",\"code\":\"DE_217125\",\"name\":\"PFS Date of training (end of training)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"EzMxXuVww2z\":{\"uid\":\"EzMxXuVww2z\",\"code\":\"DE_2008144\",\"name\":\"MCH Iron/Folic\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"edqlbukwRfQ\":{\"uid\":\"edqlbukwRfQ\",\"name\":\"Second antenatal care visit\",\"description\":\"Antenatal care visit\"},\"2023\":{\"uid\":\"2023\",\"code\":\"2023\",\"name\":\"2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-01-01T00:00:00.000\",\"endDate\":\"2023-12-31T00:00:00.000\"},\"utliJZmDeeC\":{\"uid\":\"utliJZmDeeC\",\"code\":\"DE_2008125\",\"name\":\"MCH MUAC\",\"description\":\"Mid Upper Arm Circumference\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"name\":\"2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"U5ubm6PPYrM\":{\"uid\":\"U5ubm6PPYrM\",\"code\":\"DE_860610\",\"name\":\"TB HIV testing done\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n.ZkbAXlQUYJG.vAzDOljIN1o\":{\"uid\":\"vAzDOljIN1o\",\"code\":\"TB Previous use of second- line drugs\",\"name\":\"TB Previous use of second-line drugs\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"2020\":{\"uid\":\"2020\",\"code\":\"2020\",\"name\":\"2020\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2020-01-01T00:00:00.000\",\"endDate\":\"2020-12-31T00:00:00.000\"},\"yTDoF5b1OhI\":{\"uid\":\"yTDoF5b1OhI\",\"code\":\"EA9_TRE_BRE3\",\"name\":\"WHOMCH ECV conversion remaining\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"2019\":{\"uid\":\"2019\",\"code\":\"2019\",\"name\":\"2019\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2019-01-01T00:00:00.000\",\"endDate\":\"2019-12-31T00:00:00.000\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH\":{\"uid\":\"FIHEeJwfhZH\",\"code\":\"EC_CLI_NEA\",\"name\":\"WHOMCH Maternal near-miss\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"2018\":{\"uid\":\"2018\",\"code\":\"2018\",\"name\":\"2018\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-01-01T00:00:00.000\",\"endDate\":\"2018-12-31T00:00:00.000\"},\"2017\":{\"uid\":\"2017\",\"code\":\"2017\",\"name\":\"2017\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-01T00:00:00.000\",\"endDate\":\"2017-12-31T00:00:00.000\"},\"2016\":{\"uid\":\"2016\",\"code\":\"2016\",\"name\":\"2016\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2016-01-01T00:00:00.000\",\"endDate\":\"2016-12-31T00:00:00.000\"},\"2015\":{\"uid\":\"2015\",\"code\":\"2015\",\"name\":\"2015\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2015-01-01T00:00:00.000\",\"endDate\":\"2015-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"nDwbwJZQUYU\":{\"uid\":\"nDwbwJZQUYU\",\"code\":\"OU_193233\",\"name\":\"Kanikay MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2014\":{\"uid\":\"2014\",\"code\":\"2014\",\"name\":\"2014\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2014-01-01T00:00:00.000\",\"endDate\":\"2014-12-31T00:00:00.000\"},\"g9eOBujte1U\":{\"uid\":\"g9eOBujte1U\",\"code\":\"DE_2005735\",\"name\":\"MCH ANC Visit\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n.EPEcjy3FWmI.lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n.ZkbAXlQUYJG.U5ubm6PPYrM\":{\"uid\":\"U5ubm6PPYrM\",\"code\":\"DE_860610\",\"name\":\"TB HIV testing done\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"GQY2lXrypjO\":[],\"QFX1FLWBwtq\":[\"l8S7SjnQ58G\",\"rexqxNDqUKg\"],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"w7enwqzx90I\":[],\"vAzDOljIN1o\":[],\"RG7uGl4w5Jq\":[],\"BmaBjPQX8ME\":[],\"Mnkodq2wzlV\":[],\"GUOBQt5K2WI\":[],\"UXz7xuGCEhU\":[],\"VqEFza8wbwA\":[],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"OuJ6sgPyAbC\":[],\"FO4sWYJ64LQ\":[],\"hib4oz2sOLw\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"QWVRukwa83h\":[],\"VHfUeXpawmE\":[],\"HmkXnHJxcD1\":[\"GXGM2yao0rH\",\"aAhiLBpMgEz\",\"GdEx2pyQfiT\",\"xOVen5ksQGn\"],\"ZcBPrXKahq2\":[],\"fQMBEt42CSl\":[],\"H9IlTX2X6SL\":[],\"FIHEeJwfhZH\":[],\"NALlPhMmMTQ\":[\"NXyMwAwxNap\",\"OZH6GLUufaX\",\"fpfMGr05G23\",\"snKkbSbKQFi\",\"QAr1LjJB7hV\",\"J8tdCrlmoyp\",\"e3Y43oVooNx\",\"ehhkhM0cmbA\",\"bswStRDzLny\",\"wGQbXCz6qgd\",\"bopJ9PaLnAZ\",\"ARN7cNTxlRA\",\"OP2n2kZ3eWw\"],\"zDhUuAYrxNC\":[],\"yLIPuJHRgey\":[],\"DecmCMPDPdS\":[],\"Qo571yj6Zcn\":[],\"kyIzQsj96BD\":[],\"gAbD3uDVHHh\":[\"M1D2AXtFXut\",\"PSucEiz4ZfN\",\"J7oIsZPyWQQ\",\"FaSW1H8HwH1\",\"JJ5WSuvMqec\",\"IMAmgn3qNzW\"],\"Itl05OEupgQ\":[\"R7O031O2brW\",\"wrD98kfvt90\",\"hXmq4IJwz1k\"],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"HLmTEmupdX0\":[],\"m3XQrgadVK9\":[],\"DCUDZxqOxUo\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"Rbv6wcblbxe\":[\"qSUp06RvAKJ\",\"weqPUCYpyVl\",\"yCfC0m5w9HD\"],\"lJTx9EZ1dk1\":[],\"lZGmxYbs97q\":[],\"qpQinIDQ6Uy\":[],\"EzMxXuVww2z\":[\"CG2YAW9O0sD\",\"geM27ZJWF2L\",\"AztAiEXmPMq\"],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"tEgxbwwrwUd\",\"ObV5AR1NECl\",\"Uwcj0mz78BV\",\"nDwbwJZQUYU\",\"OjTS752GbZE\",\"mt47bcb0Rcj\",\"ZxuSbAmsLCn\",\"ImspTQPwCqd\"],\"utliJZmDeeC\":[\"zEf3eIl9IaO\",\"FLSVupnfTtJ\",\"GzGLgdEseTL\"],\"U5ubm6PPYrM\":[],\"pB5sL7Ts4fb\":[\"AAgDQGRuOHB\",\"kzKxayEMqZB\",\"T6ZEg0zYzbL\"],\"yTDoF5b1OhI\":[\"GxdJJFuMQ5i\",\"j72KsGIAe8h\",\"VqvXhoA5NgX\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"2014\",\"2015\",\"2016\",\"2017\",\"2018\",\"2019\",\"2020\",\"2021\",\"2022\",\"2023\"],\"csl3yq5UC46\":[],\"g9eOBujte1U\":[\"o2mLqUo5jL4\",\"Zaz5TmIXRaw\",\"xcDhxsAhFRr\",\"fZjnhU6pX5S\",\"HspSSygqtr3\",\"nToKhKY4g74\"],\"AuPLng5hLbE\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h",
        "WHOMCH Contraceptive counselling provided, WHO RMNCH Tracker, Postpartum care visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        3,
        "WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI",
        "WHOMCH ECV conversion remaining, WHO RMNCH Tracker, Second antenatal care visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        4,
        "WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo",
        "WHOMCH ECV performed, WHO RMNCH Tracker, Second antenatal care visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        5,
        "WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I",
        "WHOMCH Eclamptic convulsions, WHO RMNCH Tracker, Second antenatal care visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        6,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw",
        "WHOMCH Estimated blood loss (ml), WHO RMNCH Tracker, Care at birth",
        "INTEGER_ZERO_OR_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response,
        7,
        "WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ",
        "WHOMCH HIV rapid test, WHO RMNCH Tracker, First antenatal care visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        8,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH",
        "WHOMCH Maternal near-miss, WHO RMNCH Tracker, Care at birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        9,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME",
        "WHOMCH Oxytocin given, WHO RMNCH Tracker, Care at birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        10,
        "WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46",
        "WHOMCH White blood cell count, WHO RMNCH Tracker, Postpartum care visit",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        11,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46",
        "WHOMCH White blood cell count, WHO RMNCH Tracker, Care at birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        12,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9",
        "WHOMCH Uterotonics given, WHO RMNCH Tracker, Care at birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        13,
        "WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS",
        "WHOMCH Ultrasound estimate of due date, WHO RMNCH Tracker, First antenatal care visit",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeader(
        response,
        14,
        "WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq",
        "WHOMCH Allergies (drugs and/or severe food allergies), WHO RMNCH Tracker, First antenatal care visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        15,
        "WSGAb5XwJ3Y.programstatus",
        "Program Status, WHO RMNCH Tracker",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        16,
        "ur1Edk5Oe2n.ZkbAXlQUYJG.HmkXnHJxcD1",
        "TB Case Definition, TB program, TB visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        17,
        "ur1Edk5Oe2n.ZkbAXlQUYJG.U5ubm6PPYrM",
        "TB HIV testing done, TB program, TB visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        18,
        "ur1Edk5Oe2n.ZkbAXlQUYJG.vAzDOljIN1o",
        "TB Previous use of second-line drugs, TB program, TB visit",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        19,
        "ur1Edk5Oe2n.jdRD35YwbRH.yLIPuJHRgey",
        "TB smear microscopy number of specimen, TB program, Sputum smear microscopy test",
        "INTEGER_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response,
        20,
        "ur1Edk5Oe2n.EPEcjy3FWmI.lJTx9EZ1dk1",
        "Tb lab Glucose, TB program, Lab monitoring",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        21,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        22,
        "IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        23,
        "IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO",
        "MCH Infant Weight  (g), Child Programme, Baby Postnatal",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        24,
        "IpHINAT79UW.ZzYYXq4fJie.HLmTEmupdX0",
        "MCH Vit A, Child Programme, Baby Postnatal",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        25,
        "IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g), Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        26,
        "uy2gU8kT1jF.Xgk8Wvl0jHr.Rbv6wcblbxe",
        "MCH Condition of mother on discharge, MNCH / PNC (Adult Woman), Delivery",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        27,
        "uy2gU8kT1jF.eaDHS084uMp.utliJZmDeeC",
        "MCH MUAC, MNCH / PNC (Adult Woman), ANC 1st visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        28,
        "uy2gU8kT1jF.eaDHS084uMp.pB5sL7Ts4fb",
        "MCH Mabendazole, MNCH / PNC (Adult Woman), ANC 1st visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        29,
        "uy2gU8kT1jF.oRySG82BKE6.EzMxXuVww2z",
        "MCH Iron/Folic, MNCH / PNC (Adult Woman), PNC Visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        30,
        "uy2gU8kT1jF.grIfo3oOf4Y.gAbD3uDVHHh",
        "MCH Tetatus, MNCH / PNC (Adult Woman), ANC Visit (2-4+)",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        31,
        "uy2gU8kT1jF.eaDHS084uMp.OuJ6sgPyAbC",
        "MCH Visit Comment, MNCH / PNC (Adult Woman), ANC 1st visit",
        "LONG_TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        32,
        "uy2gU8kT1jF.oRySG82BKE6.UXz7xuGCEhU",
        "MCH Weight (g), MNCH / PNC (Adult Woman), PNC Visit",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        33,
        "uy2gU8kT1jF.grIfo3oOf4Y.g9eOBujte1U",
        "MCH ANC Visit, MNCH / PNC (Adult Woman), ANC Visit (2-4+)",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        34,
        "uy2gU8kT1jF.eaDHS084uMp.NALlPhMmMTQ",
        "MCH ARVs, MNCH / PNC (Adult Woman), ANC 1st visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        35,
        "uy2gU8kT1jF.incidentdate",
        "LMP Date, MNCH / PNC (Adult Woman)",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        36,
        "fDd25txQckK.lST1OZ5BDJ2.qpQinIDQ6Uy",
        "PFS Date of training (end of training), Provider Follow-up and Support Tool, Provider Follow-up and Support Tool",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeader(
        response,
        37,
        "fDd25txQckK.lST1OZ5BDJ2.fQMBEt42CSl",
        "PFS End-of-training assessment - CAC MVA, Provider Follow-up and Support Tool, Provider Follow-up and Support Tool",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        38,
        "fDd25txQckK.lST1OZ5BDJ2.Mnkodq2wzlV",
        "PFS Visit number, Provider Follow-up and Support Tool, Provider Follow-up and Support Tool",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 39, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Harvest Time MCHP",
            "OU_1023",
            "1",
            "",
            "0",
            "1",
            "",
            "",
            "1",
            "1",
            "",
            "",
            "0",
            "",
            "",
            "ACTIVE",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:32.782"));
    validateRow(
        response,
        1,
        List.of(
            "Njala CHC",
            "OU_1038",
            "0",
            "",
            "0",
            "1",
            "",
            "",
            "1",
            "1",
            "",
            "",
            "0",
            "",
            "",
            "ACTIVE",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:33.922"));
    validateRow(
        response,
        2,
        List.of(
            "Feiba CHP",
            "OU_1054",
            "1",
            "",
            "0",
            "0",
            "",
            "",
            "1",
            "1",
            "",
            "",
            "1",
            "",
            "",
            "ACTIVE",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:32.913"));
    validateRow(
        response,
        3,
        List.of(
            "Ngieyehun MCHP",
            "OU_1065",
            "1",
            "",
            "0",
            "0",
            "",
            "",
            "1",
            "1",
            "",
            "",
            "1",
            "",
            "",
            "ACTIVE",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:34.146"));
    validateRow(
        response,
        4,
        List.of(
            "Upper Saama MCHP",
            "OU_1069",
            "1",
            "",
            "0",
            "0",
            "",
            "",
            "1",
            "1",
            "",
            "",
            "0",
            "",
            "",
            "ACTIVE",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.624"));
  }
}
