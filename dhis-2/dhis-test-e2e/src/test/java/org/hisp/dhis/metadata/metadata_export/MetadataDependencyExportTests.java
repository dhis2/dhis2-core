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
package org.hisp.dhis.metadata.metadata_export;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.IdGenerator;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for {@code GET /api/metadata/dependencies}, the multi-object dependency export
 * (DHIS2-21899).
 *
 * <p>The endpoint is gated to {@code OptionSet} to begin with, because the dependency traversal has
 * known N+1 query problems for the other root types. The fixture is therefore two option sets that
 * share one custom attribute -- an option belongs to exactly one option set, so a shared attribute
 * is the only way two enabled roots can share a dependency -- plus a data set used to prove the
 * gate refuses a type the traversal otherwise supports.
 *
 * <p>What only an end-to-end test can show is that the merged payload actually imports; see {@link
 * #exportedPayloadShouldBeImportable()}.
 */
public class MetadataDependencyExportTests extends ApiTest {

  private MetadataActions metadataActions;
  private RestApiActions optionSetActions;
  private LoginActions loginActions;
  private UserActions userActions;

  private final IdGenerator idGenerator = new IdGenerator();

  private String attributeId;
  private String optionSetAId;
  private String optionSetBId;
  private String optionAId;
  private String optionBId;
  private String gatedDataSetId;

  private String userWithoutAccessUsername;

  @BeforeAll
  public void beforeAll() {
    metadataActions = new MetadataActions();
    optionSetActions = new RestApiActions("/optionSets");
    loginActions = new LoginActions();
    userActions = new UserActions();

    loginActions.loginAsSuperUser();

    attributeId = idGenerator.generateUniqueId();
    optionSetAId = idGenerator.generateUniqueId();
    optionSetBId = idGenerator.generateUniqueId();
    optionAId = idGenerator.generateUniqueId();
    optionBId = idGenerator.generateUniqueId();
    gatedDataSetId = idGenerator.generateUniqueId();

    metadataActions.importAndValidateMetadata(new Gson().fromJson(fixture(), JsonObject.class));

    userWithoutAccessUsername =
        ("MetadataDependencyExportUser" + DataGenerator.randomString()).toLowerCase();
    userActions.addUser(userWithoutAccessUsername, Constants.USER_PASSWORD);
  }

  @BeforeEach
  public void beforeEach() {
    loginActions.loginAsSuperUser();
  }

  // -------------------------------------------------------------------------
  // Merging and de-duplication
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Several roots are returned in one payload")
  public void shouldExportSeveralRootsInOnePayload() {
    ApiResponse response = dependencies(refA(), refB());

    response
        .validate()
        .statusCode(200)
        .body("optionSets.id", hasItem(optionSetAId))
        .body("optionSets.id", hasItem(optionSetBId));

    assertNoDuplicateIds(response);
  }

  @Test
  @DisplayName("A dependency shared by two roots appears once")
  public void shouldNotDuplicateSharedDependency() {
    ApiResponse response = dependencies(refA(), refB());

    response
        .validate()
        .statusCode(200)
        .body(occurrencesOf("attributes", attributeId), equalTo(1))
        .body("optionSets.size()", equalTo(2));

    assertNoDuplicateIds(response);
  }

  @Test
  @DisplayName("The same reference given twice yields one root")
  public void shouldCollapseRepeatedReference() {
    dependencies(refA(), refA())
        .validate()
        .statusCode(200)
        .body(occurrencesOf("optionSets", optionSetAId), equalTo(1));
  }

  @Test
  @DisplayName("A single root gives the same payload as the per-type endpoint")
  public void shouldMatchPerTypeEndpointForASingleRoot() {
    JsonObject viaNewEndpoint = dependencies(refA()).getBody();
    JsonObject viaPerTypeEndpoint =
        optionSetActions.get("/" + optionSetAId + "/metadata").getBody();

    for (String type : List.of("optionSets", "options", "attributes")) {
      assertEquals(
          idsOf(viaPerTypeEndpoint, type),
          idsOf(viaNewEndpoint, type),
          "the multi-object endpoint diverged from /optionSets/{id}/metadata on '" + type + "'");
    }
  }

  @Test
  @DisplayName("The plural type name is accepted as well as the singular")
  public void shouldAcceptPluralTypeName() {
    dependencies("optionSets:" + optionSetAId)
        .validate()
        .statusCode(200)
        .body("optionSets.id", hasItem(optionSetAId));
  }

  // -------------------------------------------------------------------------
  // The supported-type gate
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A root type that is not yet enabled here is refused")
  public void shouldRefuseRootTypeThatIsNotYetEnabled() {
    dependencies("dataSet:" + gatedDataSetId)
        .validate()
        .statusCode(409)
        .body("response.errorReports[0].errorCode", equalTo("E6029"))
        .body("response.errorReports[0].message", containsString("optionSet"));
  }

  @Test
  @DisplayName("Gating this endpoint does not affect that type's own metadata endpoint")
  public void gatedTypeShouldStillWorkOnItsOwnEndpoint() {
    new RestApiActions("/dataSets")
        .get("/" + gatedDataSetId + "/metadata")
        .validate()
        .statusCode(200)
        .body("dataSets.id", hasItem(gatedDataSetId));
  }

  // -------------------------------------------------------------------------
  // The merged payload is a usable import file
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A merged payload can be imported again")
  public void exportedPayloadShouldBeImportable() {
    JsonObject exported = dependencies(refA(), refB()).getBody();

    metadataActions
        .importMetadata(exported, "importMode=VALIDATE")
        .validate()
        .statusCode(200)
        .body("response.status", equalTo("OK"))
        .body("response.stats.ignored", equalTo(0));
  }

  // -------------------------------------------------------------------------
  // Download and compression
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("download=true returns the payload as an attachment")
  public void shouldDownloadAsAttachment() {
    metadataActions
        .get("/dependencies?object=" + refA() + "&download=true")
        .validate()
        .statusCode(200)
        .header("Content-Disposition", equalTo("attachment; filename=metadata.json"));
  }

  @Test
  @DisplayName("The .json.zip suffix returns a zipped attachment")
  public void shouldDownloadAsZip() {
    metadataActions
        .get("/dependencies.json.zip?object=" + refA() + "&download=true")
        .validate()
        .statusCode(200)
        .header("Content-Disposition", equalTo("attachment; filename=metadata.json.zip"))
        .header("Content-Type", containsString("application/json+zip"));
  }

  // -------------------------------------------------------------------------
  // Fail fast on bad references
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Every bad reference is reported in one response")
  public void shouldReportAllInvalidReferencesAtOnce() {
    dependencies(
            "optionSet",
            "optionSetz:" + optionSetAId,
            "dataElement:" + optionSetAId,
            "dataSet:" + gatedDataSetId,
            "optionSet:aaaaaaaaaaa")
        .validate()
        .statusCode(409)
        .body(
            "response.errorReports.errorCode",
            equalTo(List.of("E6024", "E6002", "E6026", "E6029", "E6025")));
  }

  @Test
  @DisplayName("A type that is not a dependency export root at all is rejected")
  public void shouldRejectTypeThatIsNotADependencyRoot() {
    dependencies("dataElement:" + optionSetAId)
        .validate()
        .statusCode(409)
        .body("response.errorReports[0].errorCode", equalTo("E6026"));
  }

  @Test
  @DisplayName("A request with any bad reference exports nothing")
  public void shouldNotReturnAPartialPayload() {
    dependencies(refA(), "optionSet:aaaaaaaaaaa")
        .validate()
        .statusCode(409)
        .body("optionSets", equalTo(null));
  }

  @Test
  @DisplayName("Omitting the object parameter is rejected")
  public void shouldRejectMissingObjectParameter() {
    metadataActions
        .get("/dependencies")
        .validate()
        .statusCode(409)
        .body("response.errorReports[0].errorCode", equalTo("E6028"));
  }

  // -------------------------------------------------------------------------
  // Authority
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A user without F_METADATA_EXPORT cannot use the endpoint")
  public void shouldRequireMetadataExportAuthority() {
    loginActions.loginAsUser(userWithoutAccessUsername, Constants.USER_PASSWORD);

    dependencies(refA())
        .validate()
        .statusCode(409)
        .body(
            "message",
            equalTo(
                "Unfiltered access to metadata export requires super user or 'F_METADATA_EXPORT' authority."));
  }

  @Test
  @DisplayName("A class selection parameter cannot be used to skip the authority check")
  public void classSelectionShouldNotBypassAuthorityCheck() {
    loginActions.loginAsUser(userWithoutAccessUsername, Constants.USER_PASSWORD);

    metadataActions
        .get("/dependencies?object=" + refA() + "&optionSets=true")
        .validate()
        .statusCode(409);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private String refA() {
    return "optionSet:" + optionSetAId;
  }

  private String refB() {
    return "optionSet:" + optionSetBId;
  }

  /**
   * {@code QueryParamsBuilder} collapses repeated keys, so the repeated {@code object} parameters
   * are built by hand.
   */
  private ApiResponse dependencies(String... objects) {
    String query =
        List.of(objects).stream().map(o -> "object=" + o).collect(Collectors.joining("&"));
    return metadataActions.get("/dependencies?" + query);
  }

  /** A GPath expression counting how many entries of the given type carry the given id. */
  private static String occurrencesOf(String type, String id) {
    return String.format("%s.findAll { it.id == '%s' }.size()", type, id);
  }

  private static List<String> idsOf(JsonObject payload, String type) {
    if (!payload.has(type)) {
      return List.of();
    }

    List<String> ids = new ArrayList<>();
    payload.getAsJsonArray(type).forEach(e -> ids.add(e.getAsJsonObject().get("id").getAsString()));
    ids.sort(String::compareTo);
    return ids;
  }

  /** No array in a merged payload may carry the same id twice. */
  private static void assertNoDuplicateIds(ApiResponse response) {
    JsonObject payload = response.getBody();

    for (String type : payload.keySet()) {
      JsonElement value = payload.get(type);

      if (!value.isJsonArray()) {
        continue;
      }

      List<String> ids = new ArrayList<>();
      JsonArray array = value.getAsJsonArray();
      array.forEach(
          element -> {
            JsonObject object = element.getAsJsonObject();
            if (object.has("id")) {
              ids.add(object.get("id").getAsString());
            }
          });

      assertEquals(
          new HashSet<>(ids).size(), ids.size(), "duplicate id in array '" + type + "': " + ids);
    }
  }

  private String fixture() {
    return """
        {
          "attributes": [
            {
              "id": "%2$s",
              "name": "Dep export attribute %1$s",
              "shortName": "Dep export attr %1$s",
              "valueType": "TEXT",
              "optionSetAttribute": true
            }
          ],
          "options": [
            { "id": "%3$s", "name": "Dep export option A %1$s", "code": "DEP_EXP_A_%1$s" },
            { "id": "%4$s", "name": "Dep export option B %1$s", "code": "DEP_EXP_B_%1$s" }
          ],
          "optionSets": [
            {
              "id": "%5$s",
              "name": "Dep export option set A %1$s",
              "valueType": "TEXT",
              "options": [ { "id": "%3$s" } ],
              "attributeValues": [ { "attribute": { "id": "%2$s" }, "value": "a" } ]
            },
            {
              "id": "%6$s",
              "name": "Dep export option set B %1$s",
              "valueType": "TEXT",
              "options": [ { "id": "%4$s" } ],
              "attributeValues": [ { "attribute": { "id": "%2$s" }, "value": "b" } ]
            }
          ],
          "dataSets": [
            {
              "id": "%7$s",
              "name": "Dep export gated data set %1$s",
              "shortName": "Dep export gated DS %1$s",
              "periodType": "Monthly"
            }
          ]
        }
        """
        .formatted(
            DataGenerator.randomString(),
            attributeId,
            optionAId,
            optionBId,
            optionSetAId,
            optionSetBId,
            gatedDataSetId);
  }
}
