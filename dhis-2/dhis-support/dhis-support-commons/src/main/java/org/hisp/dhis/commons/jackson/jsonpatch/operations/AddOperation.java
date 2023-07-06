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
package org.hisp.dhis.commons.jackson.jsonpatch.operations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchValueOperation;

/**
 * Sets a new value, no existing value is required (think of it as a set operation).
 *
 * @author Morten Olav Hansen
 */
public class AddOperation extends JsonPatchValueOperation {
  public static final String END_OF_ARRAY = "-";

  @JsonCreator
  public AddOperation(
      @JsonProperty("path") JsonPointer path, @JsonProperty("value") JsonNode value) {
    super("add", path, value);
  }

  @Override
  public JsonNode apply(JsonNode node) throws JsonPatchException {
    if (path == JsonPointer.empty()) {
      return value;
    }

    final JsonNode parentNode = node.at(path.head());
    final String rawToken = path.last().getMatchingProperty();

    if (parentNode.isMissingNode()) {
      throw new JsonPatchException("Path does not exist: " + path);
    }

    if (!parentNode.isContainerNode()) {
      throw new JsonPatchException("Parent node is not a container, unable to proceed");
    }

    if (parentNode.isObject()) {
      ((ObjectNode) parentNode).set(rawToken, value);
    } else if (parentNode.isArray()) {
      final ArrayNode target = (ArrayNode) node.at(path.head());

      // If the "-" character is used to index the end of the array (see
      // RFC6901),this has the effect of appending the value to the
      // array.
      if (rawToken.equals(END_OF_ARRAY)) {
        target.add(value);
        return node;
      }

      final int index = getArrayIndex(rawToken, target.size());
      target.insert(index, value);
    }

    return node;
  }

  private int getArrayIndex(String rawToken, int size) throws JsonPatchException {
    final int index;

    try {
      index = Integer.parseInt(rawToken);
    } catch (NumberFormatException ignored) {
      throw new JsonPatchException(
          "not an index: " + rawToken + " (expected: a non-negative integer)");
    }

    if (index < 0 || index > size) {
      throw new JsonPatchException(
          "index out of bounds: " + index + " (expected: >= 0 && <= " + size + ')');
    }

    return index;
  }
}
