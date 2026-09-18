/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests {@code GET /api/metadata/dependencies}, the multi-object dependency export (DHIS2-21899).
 *
 * <p>Gated to {@code OptionSet}, see {@code MetadataDependencyRootResolver.ENABLED_ROOT_TYPES}. An
 * {@link Option} belongs to exactly one {@link OptionSet}, so a shared {@link Attribute} is the
 * only way two enabled roots can share a dependency, and that is what the fixture builds.
 *
 * @author David Mackessy
 */
@Transactional
class MetadataDependencyExportControllerTest extends H2ControllerIntegrationTestBase {

  private Attribute sharedAttribute;
  private OptionSet optionSetA;
  private OptionSet optionSetB;
  private DataSet gatedDataSet;

  @BeforeEach
  void setUpMetadataGraph() {
    sharedAttribute = createAttribute('A');
    sharedAttribute.setOptionSetAttribute(true);
    manager.save(sharedAttribute);

    optionSetA = optionSet('A');
    optionSetB = optionSet('B');

    // a root type the traversal supports but this endpoint does not yet enable
    gatedDataSet = createDataSet('A', PeriodType.getPeriodTypeByName("Monthly"));
    manager.save(gatedDataSet);
  }

  private OptionSet optionSet(char c) {
    Option option = createOption(c);
    manager.save(option);

    OptionSet optionSet = createOptionSet(c, option);
    optionSet.setValueType(ValueType.TEXT);
    optionSet.addAttributeValue(sharedAttribute.getUid(), "value " + c);
    manager.save(optionSet);

    return optionSet;
  }

  // -------------------------------------------------------------------------
  // Merging and de-duplication
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Several roots come back in one payload")
  void severalRootsReturnOnePayload() {
    JsonMixed json = GET(dependencies(refA(), refB())).content();

    assertTrue(json.has("system"));
    assertEquals(sorted(optionSetA.getUid(), optionSetB.getUid()), ids(json, "optionSets"));
    assertEquals(2, ids(json, "options").size(), "each root contributes its own option");
  }

  @Test
  @DisplayName("A dependency shared by two roots is not duplicated")
  void sharedDependencyIsNotDuplicated() {
    JsonMixed json = GET(dependencies(refA(), refB())).content();

    assertEquals(2, ids(json, "optionSets").size(), "both roots must be present");
    assertEquals(
        List.of(sharedAttribute.getUid()),
        ids(json, "attributes"),
        "the attribute reached from both roots was duplicated");
  }

  @Test
  @DisplayName("The same reference given twice yields one root")
  void repeatedReferenceYieldsOneRoot() {
    JsonMixed json = GET(dependencies(refA(), refA())).content();

    assertEquals(List.of(optionSetA.getUid()), ids(json, "optionSets"));
  }

  @Test
  @DisplayName("A single root gives the same payload as the per-type endpoint")
  void singleRootMatchesLegacyEndpoint() {
    JsonMixed viaNew = GET(dependencies(refA())).content();
    JsonMixed viaLegacy = GET("/optionSets/" + optionSetA.getUid() + "/metadata").content();

    for (String type : List.of("optionSets", "options", "attributes")) {
      assertEquals(ids(viaLegacy, type), ids(viaNew, type), "diverged on '" + type + "'");
    }
  }

  @Test
  @DisplayName("One objects parameter may name several objects of the same type")
  void commaSeparatedIdsInOneParameter() {
    JsonMixed json =
        GET("/metadata/dependencies?objects=optionSet:"
                + optionSetA.getUid()
                + ","
                + optionSetB.getUid())
            .content();

    assertEquals(sorted(optionSetA.getUid(), optionSetB.getUid()), ids(json, "optionSets"));
    assertEquals(
        List.of(sharedAttribute.getUid()),
        ids(json, "attributes"),
        "the shared attribute must still be de-duplicated");
  }

  @Test
  @DisplayName("A comma list gives the same payload as repeating the parameter")
  void commaListMatchesRepeatedParameters() {
    JsonMixed viaCommaList =
        GET("/metadata/dependencies?objects=optionSet:"
                + optionSetA.getUid()
                + ","
                + optionSetB.getUid())
            .content();
    JsonMixed viaRepeated = GET(dependencies(refA(), refB())).content();

    for (String type : List.of("optionSets", "options", "attributes")) {
      assertEquals(ids(viaRepeated, type), ids(viaCommaList, type), "diverged on '" + type + "'");
    }
  }

  @Test
  @DisplayName("The plural type name works as well as the singular")
  void pluralTypeNameIsAccepted() {
    assertEquals(
        List.of(optionSetA.getUid()),
        ids(GET(dependencies("optionSets:" + optionSetA.getUid())).content(), "optionSets"));
  }

  // -------------------------------------------------------------------------
  // The supported-type gate
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A traversal root type that is not yet enabled here is refused")
  void notYetEnabledRootTypeIsRefused() {
    assertErrorCodes(GET(dependencies("dataSet:" + gatedDataSet.getUid())), "E6029");
  }

  @Test
  @DisplayName("Gating this endpoint does not affect the per-type endpoint for that type")
  void gatedTypeStillWorksOnItsOwnEndpoint() {
    assertEquals(
        List.of(gatedDataSet.getUid()),
        ids(GET("/dataSets/" + gatedDataSet.getUid() + "/metadata").content(), "dataSets"));
  }

  // -------------------------------------------------------------------------
  // Lean payload flags
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("skipSharing and skipCreatedAndLastUpdated apply to the merged payload")
  void leanPayloadFlagsApply() {
    String body =
        GET(dependencies(refA(), refB()) + "&skipSharing=true&skipCreatedAndLastUpdated=true")
            .content()
            .toJson();

    assertFalse(body.contains("\"sharing\""), "sharing survived skipSharing");
    assertFalse(body.contains("\"created\""), "created survived skipCreatedAndLastUpdated");
    assertFalse(body.contains("\"lastUpdated\""), "lastUpdated survived skipCreatedAndLastUpdated");
  }

  // -------------------------------------------------------------------------
  // Download and compression
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("download=true returns the payload as an attachment")
  void downloadReturnsAttachment() {
    HttpResponse res = GET(dependencies(refA()) + "&download=true");

    assertEquals(HttpStatus.OK, res.status());
    assertEquals("attachment; filename=metadata.json", res.header("Content-Disposition"));
    assertEquals("application/json", res.header("Content-Type"));
  }

  @Test
  @DisplayName("The .json.zip suffix returns a zipped attachment")
  void zipSuffixReturnsZippedAttachment() {
    HttpResponse res = GET("/metadata/dependencies.json.zip?objects=" + refA() + "&download=true");

    assertEquals(HttpStatus.OK, res.status());
    assertEquals("attachment; filename=metadata.json.zip", res.header("Content-Disposition"));
    assertEquals("application/json+zip", res.header("Content-Type"));
    assertEquals("binary", res.header("Content-Transfer-Encoding"));
  }

  // -------------------------------------------------------------------------
  // Fail fast on bad references
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A malformed reference is rejected")
  void malformedReferenceIsRejected() {
    assertErrorCodes(GET(dependencies("optionSet")), "E6024");
  }

  @Test
  @DisplayName("An unknown type is rejected")
  void unknownTypeIsRejected() {
    assertErrorCodes(GET(dependencies("optionSetz:" + optionSetA.getUid())), "E6002");
  }

  @Test
  @DisplayName("A type that is not a dependency export root at all is rejected")
  void unsupportedRootTypeIsRejected() {
    assertErrorCodes(GET(dependencies("dataElement:" + optionSetA.getUid())), "E6026");
  }

  @Test
  @DisplayName("An unknown id is rejected")
  void unknownIdIsRejected() {
    assertErrorCodes(GET(dependencies("optionSet:aaaaaaaaaaa")), "E1113");
  }

  @Test
  @DisplayName("No objects parameter at all is rejected")
  void missingObjectParameterIsRejected() {
    assertErrorCodes(GET("/metadata/dependencies"), "E6028");
  }

  @Test
  @DisplayName("Every bad reference is reported in one response, not just the first")
  void allBadReferencesAreReportedAtOnce() {
    assertErrorCodes(
        GET(
            dependencies(
                "optionSet",
                "optionSetz:" + optionSetA.getUid(),
                "dataElement:" + optionSetA.getUid(),
                "dataSet:" + gatedDataSet.getUid(),
                "optionSet:aaaaaaaaaaa")),
        "E6024",
        "E6002",
        "E6026",
        "E6029",
        "E1113");
  }

  @Test
  @DisplayName("A request with any bad reference exports nothing at all")
  void aBadReferenceFailsTheWholeRequest() {
    JsonMixed json =
        GET(dependencies(refA(), "optionSet:aaaaaaaaaaa")).content(HttpStatus.CONFLICT);

    assertFalse(json.has("optionSets"), "a failed request must not return a partial payload");
  }

  @Test
  @DisplayName("A bad reference is still reported as JSON under a compressed suffix")
  void errorUnderCompressedSuffixIsStillReported() {
    assertErrorCodes(GET("/metadata/dependencies.json.zip?objects=optionSet:aaaaaaaaaaa"), "E1113");
  }

  // -------------------------------------------------------------------------
  // Authority
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A user without F_METADATA_EXPORT cannot use the endpoint")
  void requiresMetadataExportAuthority() {
    switchToNewUser("noExportAuthority");

    assertEquals(
        "Unfiltered access to metadata export requires super user or 'F_METADATA_EXPORT' authority.",
        GET(dependencies(refA())).content(HttpStatus.CONFLICT).getString("message").string());
  }

  @Test
  @DisplayName("A class selection parameter cannot be used to skip the authority check")
  void classesParameterDoesNotBypassAuthorityGate() {
    switchToNewUser("noExportAuthority");

    // validate() only enforces the authority when no classes are selected, so a stray
    // `optionSets=true` would otherwise populate `classes` and slip past the check
    assertEquals(HttpStatus.CONFLICT, GET(dependencies(refA()) + "&optionSets=true").status());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private String refA() {
    return "optionSet:" + optionSetA.getUid();
  }

  private String refB() {
    return "optionSet:" + optionSetB.getUid();
  }

  private static String dependencies(String... objects) {
    return "/metadata/dependencies?"
        + String.join("&", Arrays.stream(objects).map(o -> "objects=" + o).toList());
  }

  private static List<String> sorted(String... uids) {
    return Arrays.stream(uids).sorted().toList();
  }

  /** The ids of the named top-level array, sorted so comparisons are order independent. */
  private static List<String> ids(JsonMixed json, String type) {
    JsonArray array = json.getArray(type);
    return !array.exists()
        ? List.of()
        : array.asList(JsonObject.class).stream()
            .map(o -> o.getString("id").string())
            .sorted()
            .toList();
  }

  private static void assertErrorCodes(HttpResponse response, String... expected) {
    JsonMixed json = response.content(HttpStatus.CONFLICT);

    assertEquals(
        List.of(expected),
        json.getObject("response").getArray("errorReports").asList(JsonObject.class).stream()
            .map(report -> report.getString("errorCode").string())
            .toList(),
        "every bad reference should be reported, in request order");
  }
}
