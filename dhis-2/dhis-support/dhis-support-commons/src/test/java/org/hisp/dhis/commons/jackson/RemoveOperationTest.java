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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen
 */
class RemoveOperationTest {

  private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

  @Disabled("for now we will allow 'removal' of invalid path keys")
  @Test
  void testRemoveInvalidKeyShouldThrowException() throws JsonProcessingException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/aaa\"}" + "]", JsonPatch.class);
    assertNotNull(patch);
    JsonNode root = jsonMapper.createObjectNode();
    assertFalse(root.has("aaa"));
    assertThrows(JsonPatchException.class, () -> patch.apply(root));
  }

  @Test
  void testRemoveProperty() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/aaa\"}" + "]", JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    root.set("aaa", TextNode.valueOf("bbb"));
    assertTrue(root.has("aaa"));
    root = (ObjectNode) patch.apply(root);
    assertFalse(root.has("aaa"));
  }

  @Test
  void testRemovePropertyFromMap() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/props/id\"}" + "]", JsonPatch.class);
    assertNotNull(patch);
    Map<String, String> map = new HashMap<>();
    map.put("id", "123");
    ObjectNode root = jsonMapper.createObjectNode();
    root.set("props", jsonMapper.valueToTree(map));
    assertTrue(root.has("props"));
    assertTrue(root.get("props").has("id"));
    root = (ObjectNode) patch.apply(root);
    assertTrue(root.has("props"));
    assertFalse(root.get("props").has("id"));
  }

  @Test
  void testRemovePropertyArrayLastIndex() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/aaa/2\"}" + "]", JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    ArrayNode arrayNode = jsonMapper.createArrayNode();
    arrayNode.add(10);
    arrayNode.add(20);
    arrayNode.add(30);
    root.set("aaa", arrayNode);
    assertTrue(root.has("aaa"));
    assertEquals(3, arrayNode.size());
    root = (ObjectNode) patch.apply(root);
    arrayNode = (ArrayNode) root.get("aaa");
    assertNotNull(arrayNode);
    assertEquals(2, arrayNode.size());
    assertEquals(10, arrayNode.get(0).asInt());
    assertEquals(20, arrayNode.get(1).asInt());
  }

  @Test
  void testRemovePropertyArray2ndIndex() throws JsonProcessingException, JsonPatchException {
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/aaa/1\"}" + "]", JsonPatch.class);
    assertNotNull(patch);
    ObjectNode root = jsonMapper.createObjectNode();
    ArrayNode arrayNode = jsonMapper.createArrayNode();
    arrayNode.add(10);
    arrayNode.add(20);
    arrayNode.add(30);
    root.set("aaa", arrayNode);
    assertTrue(root.has("aaa"));
    assertEquals(3, arrayNode.size());
    root = (ObjectNode) patch.apply(root);
    arrayNode = (ArrayNode) root.get("aaa");
    assertNotNull(arrayNode);
    assertEquals(2, arrayNode.size());
    assertEquals(10, arrayNode.get(0).asInt());
    assertEquals(30, arrayNode.get(1).asInt());
  }
}
