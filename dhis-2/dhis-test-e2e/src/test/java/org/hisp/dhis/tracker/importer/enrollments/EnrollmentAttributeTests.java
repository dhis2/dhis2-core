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
package org.hisp.dhis.tracker.importer.enrollments;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.*;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.function.Function;
import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.metadata.TrackedEntityAttributeActions;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.tracker.importer.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EnrollmentAttributeTests extends TrackerNtiApiTest {
  TrackedEntityAttributeActions teaAttributeActions;

  ProgramActions programActions;

  String optionSetId = "ZGkmoWb77MW";

  String programId;

  String attributeId;

  String optionSetAttributeId;

  String numberAttributeId;

  String uniqueAttributeId;

  @BeforeEach
  public void beforeEach() {
    teaAttributeActions = new TrackedEntityAttributeActions();
    programActions = new ProgramActions();

    loginActions.loginAsAdmin();

    programId =
        programActions
            .createTrackerProgram(Constants.TRACKED_ENTITY_TYPE, Constants.ORG_UNIT_IDS)
            .extractUid();

    setupAttributes();
  }

  @Test
  public void shouldRequireMandatoryAttributeValue() throws Exception {
    String tea = teaAttributeActions.create("TEXT");
    programActions.addAttribute(programId, tea, true);

    String tei = importTei();

    JsonObject payload =
        new EnrollmentDataBuilder().setTei(tei).array(programId, Constants.ORG_UNIT_IDS[1]);

    trackerActions
        .postAndGetJobReport(payload)
        .validateErrorReport()
        .body("message", hasItem(Matchers.stringContainsInOrder(tea, "is mandatory")))
        .body("errorCode", hasItem("E1018"))
        .body("trackerType", hasItem("ENROLLMENT"));
  }

  @Test
  public void shouldUpdateAttributeValue() throws Exception {
    String tei = importTei();

    JsonObject payload =
        new EnrollmentDataBuilder()
            .setId(new IdGenerator().generateUniqueId())
            .setTei(tei)
            .addAttribute(numberAttributeId, "5")
            .array(programId, Constants.ORG_UNIT_IDS[1]);

    trackerActions.postAndGetJobReport(payload).validateSuccessfulImport();

    payload =
        new JsonObjectBuilder(payload)
            .addPropertyByJsonPath("enrollments[0].attributes[0].value", "9")
            .build();

    trackerActions.postAndGetJobReport(payload).validateSuccessfulImport();

    trackerActions
        .getTrackedEntity(tei + "?program=" + programId)
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("attributes", hasSize(greaterThanOrEqualTo(1)))
        .body("attributes.attribute", hasItem(numberAttributeId))
        .body("attributes.value", hasItem("9"));
  }

  @Test
  public void shouldAddAttributeValue() throws Exception {
    String tei = importTei();

    JsonObject payload =
        new EnrollmentDataBuilder()
            .setTei(tei)
            .addAttribute(optionSetAttributeId, "TA_YES")
            .array(programId, Constants.ORG_UNIT_IDS[1]);

    trackerActions
        .postAndGetJobReport(payload, new QueryParamsBuilder().add("async=false"))
        .validateSuccessfulImport();

    trackerActions
        .getTrackedEntity(tei + "?program=" + programId)
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("attributes", hasSize(greaterThanOrEqualTo(1)))
        .body("attributes.attribute", hasItem(optionSetAttributeId))
        .body("attributes.value", hasItem("TA_YES"));
  }

  @Test
  public void shouldRemoveAttributeValue() throws Exception {
    String tei = importTei();

    JsonObject payload =
        new EnrollmentDataBuilder()
            .setId(new IdGenerator().generateUniqueId())
            .setTei(tei)
            .addAttribute(optionSetAttributeId, "TA_YES")
            .array(programId, Constants.ORG_UNIT_IDS[1]);

    trackerActions.postAndGetJobReport(payload).validateSuccessfulImport();

    payload =
        new JsonObjectBuilder(payload)
            .addPropertyByJsonPath("enrollments[0].attributes[0].value", null)
            .build();

    trackerActions.postAndGetJobReport(payload).validateSuccessfulImport();

    trackerActions
        .getTrackedEntity(tei + "?program=" + programId)
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("attributes", hasSize(greaterThanOrEqualTo(1)))
        .body("attributes.attribute", not(hasItem(optionSetAttributeId)))
        .body("attributes.value", not(hasItem("TA_YES")));
  }

  @Test
  public void shouldRejectTetAttributes() throws Exception {
    String tetAttribute = "dIVt4l5vIOa";

    String tei = importTei();

    JsonObject payload =
        new EnrollmentDataBuilder()
            .setId(new IdGenerator().generateUniqueId())
            .setTei(tei)
            .addAttribute(tetAttribute, "NOT_A_VALUE")
            .array(programId, Constants.ORG_UNIT_IDS[1]);

    trackerActions
        .postAndGetJobReport(payload)
        .validateErrorReport()
        .body("", hasSize(1))
        .body("errorCode", hasItem("E1019"))
        .body("message", hasItem(containsStringIgnoringCase("Only program attributes")));
  }

  @Test
  public void shouldValidateUniqueness() throws Exception {
    String tei = importTei();
    String value = DataGenerator.randomString();

    JsonObject payload =
        new EnrollmentDataBuilder()
            .setId(new IdGenerator().generateUniqueId())
            .setTei(tei)
            .addAttribute(uniqueAttributeId, value)
            .array(programId, Constants.ORG_UNIT_IDS[1]);

    trackerActions.postAndGetJobReport(payload).validateSuccessfulImport();

    payload =
        new EnrollmentDataBuilder()
            .setId(new IdGenerator().generateUniqueId())
            .setTei(importTei())
            .addAttribute(uniqueAttributeId, value)
            .array(programId, Constants.ORG_UNIT_IDS[1]);

    trackerActions.postAndGetJobReport(payload).validateErrorReport();
  }

  @Test
  public void shouldValidateUniquenessWithinThePayload() throws Exception {
    String tei = importTei();
    String teiB = importTei();
    String value = DataGenerator.randomString();

    Function<String, JsonObject> singleEnrollment =
        (teiId) -> {
          return new EnrollmentDataBuilder()
              .setId(new IdGenerator().generateUniqueId())
              .setTei(teiId)
              .addAttribute(uniqueAttributeId, value)
              .setProgram(programId)
              .setOu(Constants.ORG_UNIT_IDS[1])
              .single();
        };

    JsonObject payload =
        new JsonObjectBuilder()
            .addOrAppendToArray(
                "enrollments", singleEnrollment.apply(tei), singleEnrollment.apply(teiB))
            .build();

    trackerActions.postAndGetJobReport(payload).validateErrorReport();
  }

  private void setupAttributes() {
    attributeId = teaAttributeActions.create("TEXT");
    optionSetAttributeId = teaAttributeActions.createOptionSetAttribute(optionSetId);
    numberAttributeId = teaAttributeActions.create("NUMBER");
    uniqueAttributeId = teaAttributeActions.create("TEXT", true);

    Arrays.asList(attributeId, optionSetAttributeId, numberAttributeId, uniqueAttributeId)
        .forEach(
            att -> {
              programActions.addAttribute(programId, att, false);
            });
  }
}
