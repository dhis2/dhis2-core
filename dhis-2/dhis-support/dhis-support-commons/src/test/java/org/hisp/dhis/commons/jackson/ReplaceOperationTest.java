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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen
 */
class ReplaceOperationTest {

  private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

  @Test
  void testBasicPropertyReplacement() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/aaa\", \"value\": \"bbb\"}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    root.set("aaa", TextNode.valueOf("aaa"));
    assertTrue(root.has("aaa"));
    assertEquals("aaa", root.get("aaa").asText());
    root = (ObjectNode) patch.apply(root);
    assertTrue(root.has("aaa"));
    assertEquals("bbb", root.get("aaa").asText());
  }

  @Test
  void testBasicReplaceNotExistProperty() throws JsonProcessingException {

    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"replace\", \"path\": \"/notExist\", \"value\": \"bbb\"}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    root.set("aaa", TextNode.valueOf("aaa"));
    assertTrue(root.has("aaa"));
    assertEquals("aaa", root.get("aaa").asText());
    assertThrows(JsonPatchException.class, () -> patch.apply(root));
  }

  @Test
  void testBasicTextToArray() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/aaa\", \"value\": [1, 2, 3, 4, 5]}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    root.set("aaa", TextNode.valueOf("aaa"));
    assertTrue(root.has("aaa"));
    assertEquals("aaa", root.get("aaa").asText());
    root = (ObjectNode) patch.apply(root);
    assertTrue(root.has("aaa"));
    JsonNode testNode = root.get("aaa");
    assertTrue(testNode.isArray());
    assertEquals(5, testNode.size());
  }

  @Test
  void testBasicTextToObject() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/aaa\", \"value\": {\"a\": 123}}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    root.set("aaa", TextNode.valueOf("aaa"));
    assertTrue(root.has("aaa"));
    assertEquals("aaa", root.get("aaa").asText());
    root = (ObjectNode) patch.apply(root);
    assertTrue(root.has("aaa"));
    JsonNode testNode = root.get("aaa");
    assertTrue(testNode.isObject());
    assertTrue(testNode.has("a"));
    assertEquals(123, testNode.get("a").asInt());
  }
}
