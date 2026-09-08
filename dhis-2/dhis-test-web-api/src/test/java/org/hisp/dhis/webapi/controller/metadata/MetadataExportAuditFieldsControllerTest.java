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
package org.hisp.dhis.webapi.controller.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@code skipCreatedAndLastUpdated} request parameter on the metadata export endpoints.
 *
 * <p>Complements {@code MetadataExportAuditFieldsTest}, which covers the same behaviour at service
 * level. These tests go over HTTP so they also cover the parameter parsing in {@code
 * getParamsFromMap} and the message converter that streams the response, which the service tests
 * bypass.
 *
 * <p>The fixture is a nested metadata graph -- {@code Program -> ProgramStage ->
 * ProgramStageDataElement} -- so that audit fields appear both on the exported objects and on
 * embedded objects one level below them.
 */
@Transactional
class MetadataExportAuditFieldsControllerTest extends H2ControllerIntegrationTestBase {

  private static final Set<String> AUDIT_FIELDS =
      Set.of("created", "lastUpdated", "createdBy", "lastUpdatedBy");

  private static final Set<String> SHARING_FIELDS =
      Set.of("user", "publicAccess", "userGroupAccesses", "userAccesses", "sharing");

  /** Selects only the types the fixture creates, to keep the exports small. */
  private static final String TYPES = "programs=true&programStages=true&dataElements=true";

  private Program program;

  @BeforeEach
  void setUpMetadataGraph() {
    User user = makeUser("A");
    manager.save(user);

    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    manager.save(dataElementA);
    manager.save(dataElementB);

    program = createProgram('A');
    program.setCreatedBy(user);
    program.setLastUpdatedBy(user);
    manager.save(program);

    ProgramStage programStage =
        createProgramStage('A', Sets.newHashSet(dataElementA, dataElementB));
    programStage.setProgram(program);
    programStage.setCreatedBy(user);
    programStage.setLastUpdatedBy(user);
    programStage
        .getProgramStageDataElements()
        .forEach(
            psde -> {
              psde.setCreatedBy(user);
              psde.setLastUpdatedBy(user);
            });
    manager.save(programStage);

    program.getProgramStages().add(programStage);
    manager.update(program);
  }

  // -------------------------------------------------------------------------
  // GET /api/metadata
  // -------------------------------------------------------------------------

  @Test
  void metadataExportContainsAuditFieldsByDefault() throws IOException {
    List<String> paths = auditFieldPaths(getMetadata("/metadata?" + TYPES));

    assertFalse(rootLevel(paths).isEmpty(), () -> "expected root-level audit fields, got " + paths);
    assertFalse(nested(paths).isEmpty(), () -> "expected nested audit fields, got " + paths);
  }

  @Test
  void metadataExportOmitsAuditFieldsWhenSkipCreatedAndLastUpdated() throws IOException {
    List<String> paths =
        auditFieldPaths(getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true"));

    assertEquals(List.of(), paths, "no audit field should survive skipCreatedAndLastUpdated=true");
  }

  @Test
  void metadataExportKeepsAuditFieldsWhenSkipCreatedAndLastUpdatedIsFalse() throws IOException {
    List<String> paths =
        auditFieldPaths(getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=false"));

    assertFalse(paths.isEmpty(), "skipCreatedAndLastUpdated=false should behave like the default");
  }

  // -------------------------------------------------------------------------
  // GET /api/programs/{uid}/metadata -- the dependency export, which hard-codes
  // ":owner" and never consults defaultFields
  // -------------------------------------------------------------------------

  @Test
  void dependencyExportContainsAuditFieldsByDefault() throws IOException {
    List<String> paths =
        auditFieldPaths(getMetadata("/programs/" + program.getUid() + "/metadata"));

    assertFalse(rootLevel(paths).isEmpty(), () -> "expected root-level audit fields, got " + paths);
    assertFalse(nested(paths).isEmpty(), () -> "expected nested audit fields, got " + paths);
  }

  @Test
  void dependencyExportOmitsAuditFieldsWhenSkipCreatedAndLastUpdated() throws IOException {
    List<String> paths =
        auditFieldPaths(
            getMetadata(
                "/programs/" + program.getUid() + "/metadata?skipCreatedAndLastUpdated=true"));

    assertEquals(List.of(), paths, "no audit field should survive skipCreatedAndLastUpdated=true");
  }

  // -------------------------------------------------------------------------
  // GET /api/dataElements -- an individual metadata controller, served by
  // AbstractFullReadOnlyController rather than the metadata export service
  // -------------------------------------------------------------------------

  /**
   * {@code skipCreatedAndLastUpdated} is scoped to the metadata export endpoints. The individual
   * metadata controllers are served by {@code AbstractFullReadOnlyController}, which builds its
   * {@code FieldFilterParams} without either skip flag ({@code skipSharing} is not supported there
   * either), so the parameter has no effect on them. Characterised here so the boundary of the
   * feature is explicit rather than discovered.
   */
  @Test
  void individualControllerGetIgnoresSkipCreatedAndLastUpdated() throws IOException {
    assertFalse(
        auditFieldPaths(getMetadata("/dataElements?fields=:owner")).isEmpty(),
        "expected audit fields on GET /dataElements");

    assertFalse(
        auditFieldPaths(getMetadata("/dataElements?fields=:owner&skipCreatedAndLastUpdated=true"))
            .isEmpty(),
        "GET /dataElements does not support skipCreatedAndLastUpdated; only the metadata export endpoints do");
  }

  // -------------------------------------------------------------------------
  // Interaction with skipSharing, and no over-reach
  // -------------------------------------------------------------------------

  @Test
  void skipCreatedAndLastUpdatedCombinesWithSkipSharing() throws IOException {
    JsonNode root =
        getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true&skipSharing=true");

    assertEquals(List.of(), auditFieldPaths(root), "audit fields should be removed at every depth");
    assertEquals(
        List.of(),
        rootLevel(fieldPaths(root, SHARING_FIELDS)),
        "skipSharing should still remove root-level sharing fields");
  }

  /**
   * {@code skipSharing} matches on the full path, so unlike {@code skipCreatedAndLastUpdated} it
   * only removes sharing fields at the root of an exported object and leaves those on embedded
   * objects in place. Characterised here to make the difference between the two flags explicit --
   * changing {@code skipSharing} is out of scope for {@code skipCreatedAndLastUpdated}.
   */
  @Test
  void skipSharingLeavesNestedSharingFields() throws IOException {
    List<String> paths =
        fieldPaths(getMetadata("/metadata?" + TYPES + "&skipSharing=true"), SHARING_FIELDS);

    assertEquals(List.of(), rootLevel(paths), "root-level sharing fields should be removed");
    assertFalse(
        nested(paths).isEmpty(), () -> "nested sharing fields survive skipSharing, got " + paths);
  }

  @Test
  void skipCreatedAndLastUpdatedKeepsNonAuditFields() throws IOException {
    JsonNode root = getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true");

    JsonNode exportedProgram = root.get("programs").get(0);
    assertEquals(program.getUid(), exportedProgram.get("id").asText());
    assertFalse(exportedProgram.get("name").asText().isEmpty(), "name should still be exported");

    JsonNode programStage = root.get("programStages").get(0);
    assertEquals(
        2,
        programStage.get("programStageDataElements").size(),
        "embedded objects should still be exported, only their audit fields removed");
    assertTrue(
        programStage.get("programStageDataElements").get(0).has("id"),
        "embedded objects should keep their non-audit fields");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private JsonNode getMetadata(String url) throws IOException {
    return new ObjectMapper().readTree(GET(url).content().toString());
  }

  private static List<String> auditFieldPaths(JsonNode root) {
    return fieldPaths(root, AUDIT_FIELDS);
  }

  /**
   * Collects the dotted path of every occurrence of {@code names} in an export, e.g. {@code
   * programStages[0].created} and {@code
   * programStages[0].programStageDataElements[0].lastUpdatedBy}. Recursion stops at a match so a
   * {@code createdBy} user object is reported once rather than once per nested property.
   */
  private static List<String> fieldPaths(JsonNode root, Set<String> names) {
    List<String> found = new ArrayList<>();
    for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> type = it.next();
      if (!"system".equals(type.getKey())) {
        collectFieldPaths(type.getValue(), type.getKey(), names, found);
      }
    }
    return found.stream().sorted().toList();
  }

  private static void collectFieldPaths(
      JsonNode node, String path, Set<String> names, List<String> found) {
    if (node.isObject()) {
      for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> field = it.next();
        String childPath = path + "." + field.getKey();
        if (names.contains(field.getKey())) {
          found.add(childPath);
        } else {
          collectFieldPaths(field.getValue(), childPath, names, found);
        }
      }
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        collectFieldPaths(node.get(i), path + "[" + i + "]", names, found);
      }
    }
  }

  /** e.g. {@code programStages[0].created} -- one property below the exported type. */
  private static List<String> rootLevel(List<String> paths) {
    return paths.stream().filter(p -> p.chars().filter(c -> c == '.').count() == 1).toList();
  }

  /** e.g. {@code programStages[0].programStageDataElements[0].created}. */
  private static List<String> nested(List<String> paths) {
    return paths.stream().filter(p -> p.chars().filter(c -> c == '.').count() > 1).toList();
  }
}
