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
package org.hisp.dhis.commons.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen
 */
class AddOperationTest {

  private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

  @Test
  void testAddEmptyPath() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"\", \"value\": \"bbb\"}" + "]", JsonPatch.class);
    assertNotNull(patch);
    JsonNode root = jsonMapper.createObjectNode();
    root = patch.apply(root);
    assertEquals("bbb", root.asText());
  }

  @Test
  void testAddSimpleStringPropertyPath() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/aaa\", \"value\": \"bbb\"}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    JsonNode root = jsonMapper.createObjectNode();
    root = patch.apply(root);
    assertTrue(root.has("aaa"));
    assertEquals("bbb", root.get("aaa").asText());
  }

  @Test
  void testAddSimpleNumberPropertyPath() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/aaa\", \"value\": 2}" + "]", JsonPatch.class);
    assertNotNull(patch);
    JsonNode root = jsonMapper.createObjectNode();
    root = patch.apply(root);
    assertTrue(root.has("aaa"));
    assertEquals(2, root.get("aaa").asInt());
  }

  @Test
  void testAddAppendArray() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "["
                + "{\"op\": \"add\", \"path\": \"/arr/-\", \"value\": 1},"
                + "{\"op\": \"add\", \"path\": \"/arr/-\", \"value\": 2},"
                + "{\"op\": \"add\", \"path\": \"/arr/-\", \"value\": 3}"
                + "]",
            JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    ArrayNode arrayNode = jsonMapper.createArrayNode();
    root.set("arr", arrayNode);
    root = (ObjectNode) patch.apply(root);
    assertTrue(root.has("arr"));
    assertEquals(3, arrayNode.size());
    assertEquals(1, arrayNode.get(0).asInt());
    assertEquals(2, arrayNode.get(1).asInt());
    assertEquals(3, arrayNode.get(2).asInt());
  }

  @Test
  void testAddModifyArray() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/arr/0\", \"value\": 1}" + "]", JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    root.set("arr", jsonMapper.createArrayNode());
    root = (ObjectNode) patch.apply(root);
    JsonNode arrayNode = root.get("arr");
    assertNotNull(arrayNode);
    assertEquals(1, arrayNode.get(0).asInt());
  }
}
