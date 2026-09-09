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

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.common.adapter.BaseIdentifiableObject_.CREATED_AND_LAST_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonSelector;
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
    params.setClasses(Set.of(Program.class, ProgramStage.class, DataElement.class));
    return params;
  }

  private String export(MetadataExportParams params) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    metadataExportService.getMetadataAsObjectNodeStream(params, out);
    return out.toString(StandardCharsets.UTF_8);
  }

  private String exportWithDependencies(MetadataExportParams params) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    metadataExportService.getMetadataWithDependenciesAsNodeStream(program, params, out);
    return out.toString(StandardCharsets.UTF_8);
  }

  // -------------------------------------------------------------------------
  // Baseline: the properties are present today, at both depths
  // -------------------------------------------------------------------------

  @Test
  void createdAndLastUpdatedArePresentAtRootAndNestedDepthByDefault() throws IOException {
    String json = export(params());
    JsonObject programStage = objects(json, "programStages").get(0);

    assertTrue(programStage.has("created"), "expected created on the exported object");
    assertTrue(
        programStage.getList("programStageDataElements", JsonObject.class).get(0).has("created"),
        "expected created on the embedded object one level down");
    assertEquals(
        CREATED_AND_LAST_UPDATED,
        present(json),
        "expected all four properties to occur somewhere in a default export");
  }

  @Test
  void createdAndLastUpdatedArePresentInDependencyExportByDefault() throws IOException {
    JsonObject programStage = objects(exportWithDependencies(params()), "programStages").get(0);

    assertTrue(programStage.has("created"), "expected created on the exported object");
    assertTrue(
        programStage.getList("programStageDataElements", JsonObject.class).get(0).has("created"),
        "expected created on the embedded object one level down");
  }

  // -------------------------------------------------------------------------
  // The feature: skipCreatedAndLastUpdated removes them everywhere
  // -------------------------------------------------------------------------

  @Test
  void skipRemovesCreatedAndLastUpdatedFromMetadataExport() throws IOException {
    MetadataExportParams params = params();
    params.setSkipCreatedAndLastUpdated(true);

    assertEquals(
        Set.of(),
        present(export(params)),
        "no created/lastUpdated field should survive skipCreatedAndLastUpdated");
  }

  @Test
  void skipRemovesCreatedAndLastUpdatedFromDependencyExport() throws IOException {
    MetadataExportParams params = params();
    params.setSkipCreatedAndLastUpdated(true);

    assertEquals(
        Set.of(),
        present(exportWithDependencies(params)),
        "the dependency export hard-codes \":owner\" and never consults defaultFields, so the flag "
            + "is the only thing that can remove these");
  }

  @Test
  void skipKeepsOtherFields() throws IOException {
    MetadataExportParams params = params();
    params.setSkipCreatedAndLastUpdated(true);

    String json = export(params);
    JsonObject programStage = objects(json, "programStages").get(0);

    assertEquals(program.getUid(), objects(json, "programs").get(0).getString("id").string());
    assertFalse(programStage.getString("name").string().isEmpty(), "name should still be exported");
    assertEquals(
        2,
        programStage.getList("programStageDataElements", JsonObject.class).size(),
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
    params.setClasses(Set.of(Interpretation.class));
    params.setSkipCreatedAndLastUpdated(true);

    JsonObject exported = objects(export(params), "interpretations").get(0);

    assertFalse(
        exported.has("created"),
        "the interpretation's own created is change metadata and should be removed");

    JsonObject mention = exported.getList("mentions", JsonObject.class).get(0);
    assertTrue(
        mention.has("created"),
        "Mention.created is payload rather than change metadata and must survive the flag");
    assertEquals(mentioned.getUsername(), mention.getString("username").string());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /** The named top-level array of a metadata document, e.g. {@code programStages}. */
  private static JsonList<JsonObject> objects(String json, String type) {
    return JsonMixed.of(json).getList(type, JsonObject.class);
  }

  /** Which of the given property names occur anywhere in the document, at any depth. */
  private static Set<String> present(String json) {
    JsonNode root = JsonNode.of(json);
    return org.hisp.dhis.common.adapter.BaseIdentifiableObject_.CREATED_AND_LAST_UPDATED.stream()
        .filter(name -> root.queryExists(JsonSelector.of("$..." + name)))
        .collect(toSet());
  }
}
