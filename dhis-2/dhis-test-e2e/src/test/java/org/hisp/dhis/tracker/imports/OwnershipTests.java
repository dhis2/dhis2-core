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
package org.hisp.dhis.tracker.imports;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonObject;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
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

  String teiInSearchScope;

  String teiInCaptureScope;

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

    teiInCaptureScope =
        super.importTeisWithEnrollmentAndEvent(captureOu, protectedProgram, protectedProgramStage)
            .extractImportedTeis()
            .get(0);
    teiInSearchScope =
        super.importTeisWithEnrollmentAndEvent(searchOu, protectedProgram, protectedProgramStage)
            .extractImportedTeis()
            .get(0);
  }

  @BeforeEach
  public void beforeEach() {
    loginActions.loginAsAdmin();
  }

  @Test
  public void shouldUpdateTrackedEntitiesInSearchScopeWhenGlassIsBroken() throws Exception {
    String teiId =
        importTeisWithEnrollmentAndEvent(searchOu, protectedProgram, protectedProgramStage)
            .extractImportedTeis()
            .get(0);

    JsonObject updatePayload =
        trackerImportExportActions
            .getTrackedEntity(teiId + "?fields=*")
            .validateStatus(200)
            .getBodyAsJsonBuilder()
            .wrapIntoArray("trackedEntities");

    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions.overrideOwnership(teiId, protectedProgram, "Change in ownership");

    trackerImportExportActions.postAndGetJobReport(updatePayload).validateSuccessfulImport();
  }

  @Test
  public void shouldNotUpdateTrackedEntitiesOutsideCaptureScopeWhenProgramProtected()
      throws Exception {
    String teiId =
        importTeisWithEnrollmentAndEvent(searchOu, protectedProgram, protectedProgramStage)
            .extractImportedTeis()
            .get(0);

    JsonObject updatePayload =
        trackerImportExportActions
            .getTrackedEntity(teiId + "?fields=*")
            .validateStatus(200)
            .getBodyAsJsonBuilder()
            .wrapIntoArray("trackedEntities");

    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions
        .postAndGetJobReport(updatePayload, new QueryParamsBuilder().addAll("atomicMode=OBJECT"))
        .validateErrorReport()
        .body("errorCode", everyItem(equalTo("E1102")))
        .body("trackerType", hasItems("ENROLLMENT", "EVENT"))
        .body("", hasSize(equalTo(2)));
  }

  @Test
  public void shouldUpdateTrackedEntitiesOutsideCaptureScopeWhenProgramOpen() throws Exception {
    String teiId =
        importTeisWithEnrollmentAndEvent(searchOu, openProgram, openProgramStage)
            .extractImportedTeis()
            .get(0);

    JsonObject updatePayload =
        trackerImportExportActions
            .getTrackedEntity(teiId + "?fields=*")
            .validateStatus(200)
            .getBodyAsJsonBuilder()
            .wrapIntoArray("trackedEntities");

    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions.postAndGetJobReport(updatePayload).validateSuccessfulImport();
  }

  @Test
  public void shouldNotValidateCaptureScopeForTei() {
    JsonObject object =
        trackerImportExportActions
            .getTrackedEntity(teiInSearchScope)
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
            .getTrackedEntity(teiInSearchScope + "?fields=enrollments")
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
        .getTrackedEntity(teiInSearchScope + "?fields=enrollments")
        .validate()
        .statusCode(200)
        .body("enrollments", hasSize(0));

    trackerImportExportActions
        .postAndGetJobReport(
            new EnrollmentDataBuilder()
                .array(protectedProgram, captureOu, teiInSearchScope, "ACTIVE"))
        .validateErrorReport()
        .body("errorCode", hasItems("E1102"));
  }

  @Test
  public void shouldNotValidateOwnershipIfProgramIsOpen() {
    loginActions.loginAsUser(username, userPassword);

    trackerImportExportActions
        .postAndGetJobReport(
            new EnrollmentDataBuilder().array(openProgram, captureOu, teiInSearchScope, "ACTIVE"))
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
