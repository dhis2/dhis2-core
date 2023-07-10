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
package org.hisp.dhis.common;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.Test;

/**
 * @author Jan Bernitt
 */
class ImportSummaryJacksonTest {

  private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

  @Test
  void testIterableSerialisedAsJsonArray() {
    ImportSummary summary = new ImportSummary();
    summary.addConflict("foo", "bar");
    summary.addConflict("x", "y");
    JsonNode summaryNode = jsonMapper.valueToTree(summary);
    assertTrue(summaryNode.has("conflicts"));
    JsonNode conflicts = summaryNode.get("conflicts");
    assertTrue(conflicts.isArray());
    assertEquals(2, conflicts.size());
    assertEquals(
        "[{\"object\":\"foo\",\"value\":\"bar\"},{\"object\":\"x\",\"value\":\"y\"}]",
        conflicts.toString());
  }

  @Test
  void testObjectsSerialisedAsIndividualProperties() {
    ImportConflict conflict = createImportConflictForIndex(0);
    JsonNode conflictNode = jsonMapper.valueToTree(conflict);
    assertTrue(conflictNode.isObject());
    assertEquals("value", conflictNode.get("objects").get("key").asText());
  }

  @Test
  void testIndicesSerialiseAsArray() {
    ImportConflict conflict1 = createImportConflictForIndex(5);
    ImportConflict conflict2 = createImportConflictForIndex(7);
    conflict1.mergeWith(conflict2);
    JsonNode conflictNode = jsonMapper.valueToTree(conflict1);
    assertTrue(conflictNode.isObject());
    JsonNode indexesArray = conflictNode.get("indexes");
    assertTrue(indexesArray.isArray());
    assertEquals(2, indexesArray.size());
    assertEquals(5, indexesArray.get(0).asInt());
    assertEquals(7, indexesArray.get(1).asInt());
  }

  /**
   * OBS! When redis is used as store {@link ImportSummary}s are serialised and deserialised again.
   */
  @Test
  void testSummaryCanBeDeserialised() throws JsonProcessingException {
    ImportSummary summary = new ImportSummary();
    summary.addConflict(createImportConflictForIndex(2));
    summary.addConflict(createImportConflictForIndex(4));
    summary.addConflict("old", "school");
    JsonNode summaryNode = jsonMapper.valueToTree(summary);
    assertTrue(summaryNode.isObject());
    ImportSummary deserialised = jsonMapper.treeToValue(summaryNode, ImportSummary.class);
    assertNotNull(deserialised);
    assertEquals(2, deserialised.getConflictCount());
    Iterator<ImportConflict> beforeIter = summary.getConflicts().iterator();
    Iterator<ImportConflict> afterIter = deserialised.getConflicts().iterator();
    for (int i = 0; i < 2; i++) {
      assertEquals(beforeIter.next(), afterIter.next());
    }
  }

  private ImportConflict createImportConflictForIndex(int index) {
    return new ImportConflict(
        singletonMap("key", "value"), "message", ErrorCode.E7600, "property", index);
  }
}
