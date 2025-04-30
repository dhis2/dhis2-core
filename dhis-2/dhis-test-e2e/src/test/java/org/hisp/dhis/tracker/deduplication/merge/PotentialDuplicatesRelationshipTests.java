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
package org.hisp.dhis.tracker.deduplication.merge;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonObject;
import java.util.Arrays;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicatesApiTest;
import org.hisp.dhis.tracker.imports.databuilder.RelationshipDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class PotentialDuplicatesRelationshipTests extends PotentialDuplicatesApiTest {
  @BeforeEach
  public void beforeEach() {
    loginActions.loginAsAdmin();
  }

  @Test
  public void shouldAutoMergeRelationshipsWithNonSuperUser() {
    // arrange
    String teA = createTrackedEntity();
    String teB = createTrackedEntity();
    String teC = createTrackedEntity();

    createUniDirectionalRelationship(teB, teC).validateSuccessfulImport();
    String relationship2 =
        createUniDirectionalRelationship(teA, teC).extractImportedRelationships().get(0);
    String relationship3 =
        createUniDirectionalRelationship(teC, teB).extractImportedRelationships().get(0);
    createUniDirectionalRelationship(teA, teB).validateSuccessfulImport();

    String potentialDuplicate =
        potentialDuplicatesActions.createAndValidatePotentialDuplicate(teA, teB, "OPEN");

    // act
    String username = createUserWithAccessToMerge();
    new LoginActions().loginAsUser(username, Constants.USER_PASSWORD);

    potentialDuplicatesActions
        .autoMergePotentialDuplicate(potentialDuplicate)
        .validate()
        .statusCode(200);

    // assert
    trackerImportExportActions
        .getTrackedEntity(teA + "?fields=*")
        .validate()
        .statusCode(200)
        .body("relationships", hasSize(1))
        .body("relationships.relationship", hasItems(relationship2));

    trackerImportExportActions
        .getTrackedEntity(teC + "?fields=*")
        .validate()
        .statusCode(200)
        .body("relationships", hasSize(1))
        .body("relationships.relationship", hasItems(relationship3));
  }

  @Test
  public void shouldManuallyMergeRelationship() {
    String teA = createTrackedEntity();
    String teB = createTrackedEntity();
    String teC = createTrackedEntity();

    String relationship = createRelationship(teB, teC).extractImportedRelationships().get(0);

    String potentialDuplicate =
        potentialDuplicatesActions.createAndValidatePotentialDuplicate(teA, teB, "OPEN");

    potentialDuplicatesActions
        .manualMergePotentialDuplicate(
            potentialDuplicate,
            new JsonObjectBuilder().addArray("relationships", Arrays.asList(relationship)).build())
        .validate()
        .statusCode(200);

    trackerImportExportActions
        .getTrackedEntity(teA + "?fields=*")
        .validate()
        .statusCode(200)
        .body("relationships", hasSize(1))
        .body("relationships.relationship", hasItems(relationship));
  }

  @Test
  public void shouldRemoveDuplicateRelationshipWhenAutoMerging() {
    String teA = createTrackedEntity();
    String teB = createTrackedEntity();

    String relationship = createRelationship(teA, teB).extractImportedRelationships().get(0);

    String potentialDuplicate =
        potentialDuplicatesActions.createAndValidatePotentialDuplicate(teA, teB, "OPEN");

    potentialDuplicatesActions
        .autoMergePotentialDuplicate(potentialDuplicate)
        .validate()
        .statusCode(200);

    trackerImportExportActions
        .getTrackedEntity(teA + "?fields=*")
        .validate()
        .statusCode(200)
        .body("relationships", hasSize(0));

    trackerImportExportActions.getRelationship(relationship).validateStatus(404);
  }

  @Test
  public void shouldNotMergeManuallyWhenThereAreDuplicateRelationships() {
    String teA = createTrackedEntity();
    String teB = createTrackedEntity();

    String relationship = createRelationship(teA, teB).extractImportedRelationships().get(0);

    String potentialDuplicate =
        potentialDuplicatesActions.createAndValidatePotentialDuplicate(teA, teB, "OPEN");

    potentialDuplicatesActions
        .manualMergePotentialDuplicate(
            potentialDuplicate,
            new JsonObjectBuilder().addArray("relationships", Arrays.asList(relationship)).build())
        .validate()
        .statusCode(409)
        .body("message", containsString("A similar relationship already exists on original"));
  }

  private TrackerApiResponse createRelationship(String teA, String teB) {
    JsonObject payload =
        new RelationshipDataBuilder().buildBidirectionalRelationship(teA, teB).array();

    return trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();
  }

  private TrackerApiResponse createUniDirectionalRelationship(String teA, String teB) {
    JsonObject payload =
        new RelationshipDataBuilder().buildUniDirectionalRelationship(teA, teB).array();

    return trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();
  }
}
