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
package org.hisp.dhis.tracker.imports;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonObject;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.actions.metadata.ProgramActions;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.test.e2e.utils.DataGenerator;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EnrollmentDataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OwnershipTests extends TrackerApiTest {
  String userPassword = Constants.USER_PASSWORD;

  String captureOu = "DiszpKrYNg8"; // level 4

  String searchOu = "YuQRtpLP10I"; // level 3

  String username;

  String teInSearchScope;

  String teInCaptureScope;

  private ProgramActions programActions;

  private UserActions userActions;

  private String protectedProgram;

  private String protectedProgramStage;

  private String openProgram;

  private String openProgramStage;

  @BeforeAll
  public void beforeAll() throws Exception {
    userActions = new UserActions();
    programActions = new ProgramActions();

    loginActions.loginAsSuperUser();
    username = createUserWithAccessToOu();

    protectedProgram =
        programActions.createProgramWithAccessLevel("PROTECTED", captureOu, searchOu);
    protectedProgramStage =
        programActions
            .get(protectedProgram, new QueryParamsBuilder().add("fields=programStages"))
            .validateStatus(200)
            .extractString("programStages.id[0]");

    openProgram = programActions.createProgramWithAccessLevel("OPEN", captureOu, searchOu);
    openProgramStage =
        programActions
            .get(openProgram, new QueryParamsBuilder().add("fields=programStages"))
            .validateStatus(200)
            .extractString("programStages.id[0]");

    teInCaptureScope =
        super.importTrackedEntitiesWithEnrollmentAndEvent(
                captureOu, protectedProgram, protectedProgramStage)
            .extractImportedTrackedEntities()
            .get(0);
    teInSearchScope =
        super.importTrackedEntitiesWithEnrollmentAndEvent(
                searchOu, protectedProgram, protectedProgramStage)
            .extractImportedTrackedEntities()
            .get(0);
  }

  @BeforeEach
  public void beforeEach() {
    loginActions.loginAsAdmin();
  }

  @Test
  public void shouldUpdateTrackedEntitiesInSearchScopeWhenGlassIsBroken() throws Exception {
    String teId =
        importTrackedEntitiesWithEnrollmentAndEvent(
                searchOu, protectedProgram, protectedProgramStage)
            .extractImportedTrackedEntities()
            .get(0);

    JsonObject updatePayload =
        trackerImportExportActions
            .getTrackedEntity(teId + "?fields=*,!enrollments")
            .validateStatus(200)
            .getBodyAsJsonBuilder()
            .wrapIntoArray("trackedEntities");

    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions.overrideOwnership(teId, protectedProgram, "Change in ownership");

    trackerImportExportActions.postAndGetJobReport(updatePayload).validateSuccessfulImport();
  }

  @Test
  public void shouldNotUpdateTrackedEntitiesOutsideCaptureScopeWhenProgramProtected()
      throws Exception {
    String teId =
        importTrackedEntitiesWithEnrollmentAndEvent(
                searchOu, protectedProgram, protectedProgramStage)
            .extractImportedTrackedEntities()
            .get(0);

    JsonObject updatePayload =
        trackerImportExportActions
            .getTrackedEntity(teId + "?fields=*")
            .validateStatus(200)
            .getBodyAsJsonBuilder()
            .wrapIntoArray("trackedEntities");

    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions
        .postAndGetJobReport(updatePayload, new QueryParamsBuilder().addAll("atomicMode=OBJECT"))
        .validateErrorReport()
        .body("errorCode", hasItems("E1102", "E1000"))
        .body("trackerType", hasItems("ENROLLMENT", "EVENT"))
        .body("", hasSize(equalTo(3)));
  }

  @Test
  public void shouldUpdateTrackedEntitiesOutsideCaptureScopeWhenProgramOpen() throws Exception {
    String teId =
        importTrackedEntitiesWithEnrollmentAndEvent(searchOu, openProgram, openProgramStage)
            .extractImportedTrackedEntities()
            .get(0);

    JsonObject updatePayload =
        trackerImportExportActions
            .getTrackedEntity(teId + "?fields=*,!enrollments")
            .validateStatus(200)
            .getBodyAsJsonBuilder()
            .wrapIntoArray("trackedEntities");

    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions.postAndGetJobReport(updatePayload).validateSuccessfulImport();
  }

  @Test
  public void shouldNotValidateCaptureScopeForTrackedEntity() {
    JsonObject object =
        trackerImportExportActions
            .getTrackedEntity(teInSearchScope)
            .validateStatus(200)
            .getBodyAsJsonBuilder()
            .wrapIntoArray("trackedEntities");

    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions.postAndGetJobReport(object).validateSuccessfulImport();
  }

  @ValueSource(strings = {"CREATE_AND_UPDATE", "UPDATE", "DELETE"})
  @ParameterizedTest
  public void shouldValidateEnrollmentOwnership(String importStrategy) {
    JsonObject enrollment =
        trackerImportExportActions
            .getTrackedEntity(teInSearchScope + "?fields=enrollments")
            .validateStatus(200)
            .getBody();

    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions
        .postAndGetJobReport(
            enrollment, new QueryParamsBuilder().add("importStrategy=" + importStrategy))
        .validateErrorReport()
        .body("errorCode", hasItems("E1102"));
  }

  @Test
  public void shouldNotEnrollWhenOwnershipOutsideCaptureOu() {
    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions
        .getTrackedEntity(teInSearchScope + "?fields=enrollments")
        .validate()
        .statusCode(200)
        .body("enrollments", hasSize(0));

    trackerImportExportActions
        .postAndGetJobReport(
            new EnrollmentDataBuilder()
                .array(protectedProgram, captureOu, teInSearchScope, "ACTIVE"))
        .validateErrorReport()
        .body("errorCode", hasItems("E1102"));
  }

  @Test
  public void shouldNotValidateOwnershipIfProgramIsOpen() {
    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions
        .postAndGetJobReport(
            new EnrollmentDataBuilder().array(openProgram, captureOu, teInSearchScope, "ACTIVE"))
        .validateSuccessfulImport();
  }

  private String createUserWithAccessToOu() {
    String username = DataGenerator.randomString().toLowerCase();
    String userid = userActions.addUser(username, Constants.USER_PASSWORD);

    userActions.grantUserCaptureAccessToOrgUnit(userid, captureOu);
    userActions.grantUserSearchAccessToOrgUnit(userid, searchOu);
    userActions.addUserToUserGroup(userid, Constants.USER_GROUP_ID);

    return username;
  }
}
