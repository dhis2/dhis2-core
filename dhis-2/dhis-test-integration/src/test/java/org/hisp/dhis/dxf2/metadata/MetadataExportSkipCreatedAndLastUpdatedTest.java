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

import static org.hisp.dhis.common.adapter.BaseIdentifiableObject_.CREATED_AND_LAST_UPDATED;
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
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests that {@code created}, {@code lastUpdated}, {@code createdBy} and {@code lastUpdatedBy} can
 * be excluded from every metadata export path via {@code skipCreatedAndLastUpdated}.
 *
 * <p>The fixture is a nested metadata graph -- {@code Program -> ProgramStage ->
 * ProgramStageDataElement} -- because {@link ProgramStage#getProgramStageDataElements()} holds
 * {@code EmbeddedObject}s that are serialised inline and carry their own copies of them. They
 * therefore appear at two different depths, which the tests below assert on separately.
 */
@Transactional
class MetadataExportSkipCreatedAndLastUpdatedTest extends PostgresIntegrationTestBase {

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
    // the embedded objects are what produce these properties at a nested depth
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
  // Baseline: the properties are present today, at both depths
  // -------------------------------------------------------------------------

  @Test
  void createdAndLastUpdatedArePresentAtRootAndNestedDepthByDefault() throws IOException {
    List<String> paths = createdAndLastUpdatedPaths(export(params()));

    assertFalse(paths.isEmpty(), "expected created/lastUpdated in a default export");
    assertFalse(
        rootLevel(paths).isEmpty(),
        () -> "expected root-level created/lastUpdated, found " + paths);
    assertFalse(
        nested(paths).isEmpty(),
        () -> "expected nested created/lastUpdated on programStageDataElements, found " + paths);
    assertTrue(
        CREATED_AND_LAST_UPDATED.stream()
            .allMatch(f -> paths.stream().anyMatch(p -> p.endsWith("." + f))),
        () -> "expected all four properties to occur somewhere, found " + paths);
  }

  @Test
  void createdAndLastUpdatedArePresentInDependencyExportByDefault() throws IOException {
    List<String> paths = createdAndLastUpdatedPaths(exportWithDependencies(params()));

    assertFalse(paths.isEmpty(), "expected created/lastUpdated in a default dependency export");
    assertFalse(
        rootLevel(paths).isEmpty(),
        () -> "expected root-level created/lastUpdated, found " + paths);
    assertFalse(
        nested(paths).isEmpty(),
        () -> "expected nested created/lastUpdated on programStageDataElements, found " + paths);
  }

  // -------------------------------------------------------------------------
  // The feature: skipCreatedAndLastUpdated removes them everywhere
  // -------------------------------------------------------------------------

  @Test
  void skipRemovesCreatedAndLastUpdatedFromMetadataExport() throws IOException {
    MetadataExportParams params = params();
    params.setSkipCreatedAndLastUpdated(true);

    List<String> paths = createdAndLastUpdatedPaths(export(params));

    assertEquals(
        List.of(), paths, "no created/lastUpdated field should survive skipCreatedAndLastUpdated");
  }

  @Test
  void skipRemovesCreatedAndLastUpdatedFromDependencyExport() throws IOException {
    MetadataExportParams params = params();
    params.setSkipCreatedAndLastUpdated(true);

    List<String> paths = createdAndLastUpdatedPaths(exportWithDependencies(params));

    assertEquals(
        List.of(),
        paths,
        "no created/lastUpdated field should survive skipCreatedAndLastUpdated on the dependency export, which hard-codes "
            + "\":owner\" and never consults defaultFields");
  }

  @Test
  void skipKeepsOtherFields() throws IOException {
    MetadataExportParams params = params();
    params.setSkipCreatedAndLastUpdated(true);

    JsonNode root = export(params);
    JsonNode programStage = root.get("programStages").get(0);

    assertEquals(program.getUid(), root.get("programs").get(0).get("id").asText());
    assertFalse(programStage.get("name").asText().isEmpty(), "name should still be exported");
    assertEquals(
        2,
        programStage.get("programStageDataElements").size(),
        "embedded objects should still be exported, only created/lastUpdated removed");
  }

  /**
   * {@code Mention} is not an {@link org.hisp.dhis.common.IdentifiableObject} and its {@code
   * created} holds the time of the mention itself. A property that merely shares a name with one of
   * the four must therefore survive the flag.
   */
  @Test
  void skipCreatedAndLastUpdatedKeepsSameNamedPropertyOnNonIdentifiableObject() throws IOException {
    User mentioned = makeUser("B");
    manager.save(mentioned);

    Visualization visualization = createVisualization('A');
    manager.save(visualization, false);

    Interpretation interpretation =
        new Interpretation(visualization, null, "see @" + mentioned.getUsername());
    interpretation.setMentionsFromUsers(Set.of(mentioned));
    manager.save(interpretation, false);

    MetadataExportParams params = new MetadataExportParams();
    params.setClasses(Sets.newHashSet(Interpretation.class));
    params.setSkipCreatedAndLastUpdated(true);

    JsonNode exported = export(params).get("interpretations").get(0);

    assertFalse(
        exported.has("created"),
        "the interpretation's own created is change metadata and should be removed");

    JsonNode mention = exported.get("mentions").get(0);
    assertTrue(
        mention.has("created"),
        "Mention.created is payload rather than change metadata and must survive the flag");
    assertEquals(mentioned.getUsername(), mention.get("username").asText());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /**
   * Collects the dotted path of every occurrence of the four properties in an export, e.g. {@code
   * programStages[0].created} and {@code
   * programStages[0].programStageDataElements[0].lastUpdatedBy}. Recursion stops at a match so a
   * {@code createdBy} user object is reported once rather than once per nested property.
   */
  private static List<String> createdAndLastUpdatedPaths(JsonNode root) {
    List<String> found = new ArrayList<>();
    for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> type = it.next();
      if (!"system".equals(type.getKey())) {
        collectCreatedAndLastUpdatedPaths(type.getValue(), type.getKey(), found);
      }
    }
    return found.stream().sorted().toList();
  }

  private static void collectCreatedAndLastUpdatedPaths(
      JsonNode node, String path, List<String> found) {
    if (node.isObject()) {
      for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> field = it.next();
        String childPath = path + "." + field.getKey();
        if (CREATED_AND_LAST_UPDATED.contains(field.getKey())) {
          found.add(childPath);
        } else {
          collectCreatedAndLastUpdatedPaths(field.getValue(), childPath, found);
        }
      }
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        collectCreatedAndLastUpdatedPaths(node.get(i), path + "[" + i + "]", found);
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
