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

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.common.adapter.BaseIdentifiableObject_.CREATED_AND_LAST_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.Set;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonSelector;
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
 * <p>Complements {@code MetadataExportSkipCreatedAndLastUpdatedTest}, which covers the same
 * behaviour at service level. These tests go over HTTP so they also cover the parameter parsing in
 * {@code getParamsFromMap} and the message converter that streams the response, which the service
 * tests bypass.
 *
 * <p>The fixture is a nested metadata graph -- {@code Program -> ProgramStage ->
 * ProgramStageDataElement} -- so that created/lastUpdated appear both on the exported objects and
 * on embedded objects one level below them.
 */
@Transactional
class MetadataExportSkipCreatedAndLastUpdatedControllerTest
    extends H2ControllerIntegrationTestBase {

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
  void metadataExportContainsCreatedAndLastUpdatedByDefault() {
    JsonObject programStage = objects(getMetadata("/metadata?" + TYPES), "programStages").get(0);

    assertTrue(programStage.has("created"), "expected created on the exported object");
    assertTrue(
        programStage.getList("programStageDataElements", JsonObject.class).get(0).has("created"),
        "expected created on the embedded object one level down");
  }

  @Test
  void metadataExportOmitsCreatedAndLastUpdatedWhenFlagIsTrue() {
    assertEquals(
        Set.of(),
        present(
            getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true"),
            CREATED_AND_LAST_UPDATED),
        "no created/lastUpdated field should survive skipCreatedAndLastUpdated=true");
  }

  @Test
  void metadataExportKeepsCreatedAndLastUpdatedWhenFlagIsFalse() {
    assertEquals(
        CREATED_AND_LAST_UPDATED,
        present(
            getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=false"),
            CREATED_AND_LAST_UPDATED),
        "skipCreatedAndLastUpdated=false should behave like the default");
  }

  // -------------------------------------------------------------------------
  // GET /api/programs/{uid}/metadata -- the dependency export, which hard-codes
  // ":owner" and never consults defaultFields
  // -------------------------------------------------------------------------

  @Test
  void dependencyExportContainsCreatedAndLastUpdatedByDefault() {
    JsonObject programStage =
        objects(getMetadata("/programs/" + program.getUid() + "/metadata"), "programStages").get(0);

    assertTrue(programStage.has("created"), "expected created on the exported object");
    assertTrue(
        programStage.getList("programStageDataElements", JsonObject.class).get(0).has("created"),
        "expected created on the embedded object one level down");
  }

  @Test
  void dependencyExportOmitsCreatedAndLastUpdatedWhenFlagIsTrue() {
    assertEquals(
        Set.of(),
        present(
            getMetadata(
                "/programs/" + program.getUid() + "/metadata?skipCreatedAndLastUpdated=true"),
            CREATED_AND_LAST_UPDATED),
        "no created/lastUpdated field should survive skipCreatedAndLastUpdated=true");
  }

  // -------------------------------------------------------------------------
  // Interaction with skipSharing, and no over-reach
  // -------------------------------------------------------------------------

  @Test
  void skipCreatedAndLastUpdatedCombinesWithSkipSharing() {
    JsonMixed json =
        getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true&skipSharing=true");

    assertEquals(
        Set.of(),
        present(json, CREATED_AND_LAST_UPDATED),
        "created/lastUpdated should be removed at every depth");
    assertFalse(
        objects(json, "dataElements").get(0).has("sharing"),
        "skipSharing should still remove root-level sharing fields");
  }

  @Test
  void skipKeepsOtherFields() {
    JsonMixed root = getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true");

    JsonObject exportedProgram = objects(root, "programs").get(0);
    assertEquals(program.getUid(), exportedProgram.getString("id").string());
    assertFalse(
        exportedProgram.getString("name").string().isEmpty(), "name should still be exported");

    JsonObject programStage = objects(root, "programStages").get(0);
    assertEquals(
        2,
        programStage.getList("programStageDataElements", JsonObject.class).size(),
        "embedded objects should still be exported, only created/lastUpdated removed");
    assertTrue(
        programStage.getList("programStageDataElements", JsonObject.class).get(0).has("id"),
        "embedded objects should keep their other fields");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private JsonMixed getMetadata(String url) {
    return GET(url).content();
  }

  /** The named top-level array of a metadata document, e.g. {@code programStages}. */
  private static JsonList<JsonObject> objects(JsonMixed json, String type) {
    return json.getList(type, JsonObject.class);
  }

  /** Which of the given property names occur anywhere in the document, at any depth. */
  private static Set<String> present(JsonMixed json, Set<String> propertyNames) {
    JsonNode root = json.node();
    return propertyNames.stream()
        .filter(name -> root.queryExists(JsonSelector.of("$..." + name)))
        .collect(toSet());
  }
}
