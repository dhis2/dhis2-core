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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests that the audit fields {@code created}, {@code lastUpdated}, {@code createdBy} and {@code
 * lastUpdatedBy} can be excluded from every metadata export path via {@code skipAuditFields}.
 *
 * <p>The fixture is a nested metadata graph -- {@code Program -> ProgramStage ->
 * ProgramStageDataElement} -- because {@link ProgramStage#getProgramStageDataElements()} holds
 * {@code EmbeddedObject}s that are serialised inline and carry their own audit fields. Audit fields
 * therefore appear at two different depths, which the tests below assert on separately.
 */
@Transactional
class MetadataExportAuditFieldsTest extends PostgresIntegrationTestBase {

  private static final Set<String> AUDIT_FIELDS =
      Set.of("created", "lastUpdated", "createdBy", "lastUpdatedBy");

  private static final List<String> AUDIT_FIELD_EXCLUSIONS =
      List.of(":owner", "!created", "!lastUpdated", "!createdBy", "!lastUpdatedBy");

  @Autowired private MetadataExportService metadataExportService;

  @Autowired private IdentifiableObjectManager manager;

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
    // the embedded objects are what produce audit fields at a nested depth
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

  private MetadataExportParams params() {
    MetadataExportParams params = new MetadataExportParams();
    params.setClasses(Sets.newHashSet(Program.class, ProgramStage.class, DataElement.class));
    return params;
  }

  private JsonNode export(MetadataExportParams params) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    metadataExportService.getMetadataAsObjectNodeStream(params, out);
    return new ObjectMapper().readTree(out.toByteArray());
  }

  private JsonNode exportWithDependencies(MetadataExportParams params) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    metadataExportService.getMetadataWithDependenciesAsNodeStream(program, params, out);
    return new ObjectMapper().readTree(out.toByteArray());
  }

  // -------------------------------------------------------------------------
  // Baseline: the audit fields are present today, at both depths
  // -------------------------------------------------------------------------

  @Test
  void auditFieldsArePresentAtRootAndNestedDepthByDefault() throws IOException {
    List<String> paths = auditFieldPaths(export(params()));

    assertFalse(paths.isEmpty(), "expected audit fields in a default export");
    assertFalse(
        rootLevel(paths).isEmpty(), () -> "expected root-level audit fields, found " + paths);
    assertFalse(
        nested(paths).isEmpty(),
        () -> "expected nested audit fields on programStageDataElements, found " + paths);
    assertTrue(
        AUDIT_FIELDS.stream().allMatch(f -> paths.stream().anyMatch(p -> p.endsWith("." + f))),
        () -> "expected all four audit fields to occur somewhere, found " + paths);
  }

  @Test
  void auditFieldsArePresentAtRootAndNestedDepthInDependencyExportByDefault() throws IOException {
    List<String> paths = auditFieldPaths(exportWithDependencies(params()));

    assertFalse(paths.isEmpty(), "expected audit fields in a default dependency export");
    assertFalse(
        rootLevel(paths).isEmpty(), () -> "expected root-level audit fields, found " + paths);
    assertFalse(
        nested(paths).isEmpty(),
        () -> "expected nested audit fields on programStageDataElements, found " + paths);
  }

  /**
   * Characterises why a {@code fields} based exclusion is not sufficient: exclusion paths are
   * anchored at the root, so {@code !created} removes {@code programStages[0].created} but leaves
   * {@code programStages[0].programStageDataElements[0].created} untouched.
   */
  @Test
  void rootAnchoredFieldsExclusionLeavesNestedAuditFields() throws IOException {
    MetadataExportParams params = params();
    params.setDefaultFields(new ArrayList<>(AUDIT_FIELD_EXCLUSIONS));

    List<String> paths = auditFieldPaths(export(params));

    assertEquals(
        List.of(), rootLevel(paths), "root-level audit fields should be removed by !field syntax");
    assertFalse(
        nested(paths).isEmpty(),
        "nested audit fields survive a root-anchored exclusion, which is why skipAuditFields "
            + "cannot be implemented via defaultFields alone");
  }

  // -------------------------------------------------------------------------
  // The feature: skipAuditFields removes them everywhere
  // -------------------------------------------------------------------------

  @Test
  void skipAuditFieldsRemovesAuditFieldsFromMetadataExport() throws IOException {
    MetadataExportParams params = params();
    params.setSkipAuditFields(true);

    List<String> paths = auditFieldPaths(export(params));

    assertEquals(List.of(), paths, "no audit field should survive skipAuditFields");
  }

  @Test
  void skipAuditFieldsRemovesAuditFieldsFromDependencyExport() throws IOException {
    MetadataExportParams params = params();
    params.setSkipAuditFields(true);

    List<String> paths = auditFieldPaths(exportWithDependencies(params));

    assertEquals(
        List.of(),
        paths,
        "no audit field should survive skipAuditFields on the dependency export, which hard-codes "
            + "\":owner\" and never consults defaultFields");
  }

  @Test
  void skipAuditFieldsKeepsNonAuditFields() throws IOException {
    MetadataExportParams params = params();
    params.setSkipAuditFields(true);

    JsonNode root = export(params);
    JsonNode programStage = root.get("programStages").get(0);

    assertEquals(program.getUid(), root.get("programs").get(0).get("id").asText());
    assertFalse(programStage.get("name").asText().isEmpty(), "name should still be exported");
    assertEquals(
        2,
        programStage.get("programStageDataElements").size(),
        "embedded objects should still be exported, only their audit fields removed");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /**
   * Collects the dotted path of every audit field occurrence in an export, e.g. {@code
   * programStages[0].created} and {@code
   * programStages[0].programStageDataElements[0].lastUpdatedBy}. Recursion stops at an audit field
   * so a {@code createdBy} user object is reported once rather than once per nested property.
   */
  private static List<String> auditFieldPaths(JsonNode root) {
    List<String> found = new ArrayList<>();
    for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> type = it.next();
      if (!"system".equals(type.getKey())) {
        collectAuditFieldPaths(type.getValue(), type.getKey(), found);
      }
    }
    return found.stream().sorted().toList();
  }

  private static void collectAuditFieldPaths(JsonNode node, String path, List<String> found) {
    if (node.isObject()) {
      for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> field = it.next();
        String childPath = path + "." + field.getKey();
        if (AUDIT_FIELDS.contains(field.getKey())) {
          found.add(childPath);
        } else {
          collectAuditFieldPaths(field.getValue(), childPath, found);
        }
      }
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        collectAuditFieldPaths(node.get(i), path + "[" + i + "]", found);
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
