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

import static org.hisp.dhis.common.adapter.BaseIdentifiableObject_.CREATED_AND_LAST_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
  void metadataExportContainsCreatedAndLastUpdatedByDefault() throws IOException {
    JsonNode programStage = getMetadata("/metadata?" + TYPES).get("programStages").get(0);

    assertTrue(programStage.has("created"), "expected created on the exported object");
    assertTrue(
        programStage.get("programStageDataElements").get(0).has("created"),
        "expected created on the embedded object one level down");
  }

  @Test
  void metadataExportOmitsCreatedAndLastUpdatedWhenFlagIsTrue() throws IOException {
    assertEquals(
        Map.of(),
        remaining(getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true")),
        "no created/lastUpdated field should survive skipCreatedAndLastUpdated=true");
  }

  @Test
  void metadataExportKeepsCreatedAndLastUpdatedWhenFlagIsFalse() throws IOException {
    assertEquals(
        CREATED_AND_LAST_UPDATED,
        remaining(getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=false")).keySet(),
        "skipCreatedAndLastUpdated=false should behave like the default");
  }

  // -------------------------------------------------------------------------
  // GET /api/programs/{uid}/metadata -- the dependency export, which hard-codes
  // ":owner" and never consults defaultFields
  // -------------------------------------------------------------------------

  @Test
  void dependencyExportContainsCreatedAndLastUpdatedByDefault() throws IOException {
    JsonNode programStage =
        getMetadata("/programs/" + program.getUid() + "/metadata").get("programStages").get(0);

    assertTrue(programStage.has("created"), "expected created on the exported object");
    assertTrue(
        programStage.get("programStageDataElements").get(0).has("created"),
        "expected created on the embedded object one level down");
  }

  @Test
  void dependencyExportOmitsCreatedAndLastUpdatedWhenFlagIsTrue() throws IOException {
    assertEquals(
        Map.of(),
        remaining(
            getMetadata(
                "/programs/" + program.getUid() + "/metadata?skipCreatedAndLastUpdated=true")),
        "no created/lastUpdated field should survive skipCreatedAndLastUpdated=true");
  }

  // -------------------------------------------------------------------------
  // Interaction with skipSharing, and no over-reach
  // -------------------------------------------------------------------------

  @Test
  void skipCreatedAndLastUpdatedCombinesWithSkipSharing() throws IOException {
    JsonNode root =
        getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true&skipSharing=true");

    assertEquals(Map.of(), remaining(root), "created/lastUpdated should be removed at every depth");
    assertFalse(
        root.get("dataElements").get(0).has("sharing"),
        "skipSharing should still remove root-level sharing fields");
  }

  @Test
  void skipKeepsOtherFields() throws IOException {
    JsonNode root = getMetadata("/metadata?" + TYPES + "&skipCreatedAndLastUpdated=true");

    JsonNode exportedProgram = root.get("programs").get(0);
    assertEquals(program.getUid(), exportedProgram.get("id").asText());
    assertFalse(exportedProgram.get("name").asText().isEmpty(), "name should still be exported");

    JsonNode programStage = root.get("programStages").get(0);
    assertEquals(
        2,
        programStage.get("programStageDataElements").size(),
        "embedded objects should still be exported, only created/lastUpdated removed");
    assertTrue(
        programStage.get("programStageDataElements").get(0).has("id"),
        "embedded objects should keep their other fields");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private JsonNode getMetadata(String url) throws IOException {
    return new ObjectMapper().readTree(GET(url).content().toString());
  }

  /**
   * The properties still present anywhere in a response, each mapped to the ids of the objects
   * declaring them. {@link JsonNode#findParents(String)} descends to any depth and does not look
   * inside a match, so embedded objects are covered and a {@code createdBy} user object is reported
   * once.
   */
  private static Map<String, List<String>> remaining(JsonNode root) {
    Map<String, List<String>> found = new TreeMap<>();
    for (String name : CREATED_AND_LAST_UPDATED) {
      List<String> owners =
          root.findParents(name).stream().map(o -> o.path("id").asText("?")).toList();
      if (!owners.isEmpty()) {
        found.put(name, owners);
      }
    }
    return found;
  }
}
