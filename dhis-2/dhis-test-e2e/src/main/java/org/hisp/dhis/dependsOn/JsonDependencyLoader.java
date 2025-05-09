/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.dependsOn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * Loads a dependency JSON file from the class‑path and converts it into an immutable {@link
 * DependencyFile} instance.
 *
 * <p>The file <b>must</b> contain a {@code "type"} field. Currently supported value is {@code
 * "pi"}. For program‑indicator files the loader also verifies that a non‑blank {@code "code"} field
 * is present (DHIS2 uniqueness requirement).
 *
 * <p>The {@code "type"} attribute is stripped out of the payload before the {@link DependencyFile}
 * is returned.
 */
@UtilityClass
public final class JsonDependencyLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Loads and validates a dependency JSON resource.
   *
   * @param resourcePath class‑path location relative to root (e.g. {@code
   *     "dependencies/pi-example.json"})
   * @return populated {@link DependencyFile}
   * @throws DependencySetupException if the resource is missing, malformed, or fails validation
   *     rules
   */
  public static DependencyFile load(String resourcePath) throws DependencySetupException {
    JsonNode rootNode = readJson(resourcePath);
    String typeRaw = extractType(rootNode, resourcePath);
    DependencyType type = DependencyType.from(typeRaw);

    if (type == DependencyType.PROGRAM_INDICATOR || type == DependencyType.INDICATOR) {
      validate(rootNode, resourcePath);
    }

    // Remove the "type" key before returning to caller
    ((ObjectNode) rootNode).remove("type");

    return new DependencyFile(type, rootNode);
  }

  private static JsonNode readJson(String resourcePath) throws DependencySetupException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try (InputStream in = cl.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new DependencySetupException(
            "Dependency file not found on class‑path: " + resourcePath);
      }
      return MAPPER.readTree(in);
    } catch (IOException io) {
      throw new DependencySetupException(
          "Unable to parse JSON in dependency file: " + resourcePath, io);
    }
  }

  private static String extractType(JsonNode node, String resourcePath)
      throws DependencySetupException {
    JsonNode typeNode = node.get("type");
    if (typeNode == null || typeNode.isNull() || typeNode.asText().isBlank()) {
      throw new DependencySetupException(
          "\"type\" attribute missing or blank in dependency file: " + resourcePath);
    }
    return typeNode.asText().toLowerCase(Locale.ROOT);
  }

  private static void validate(JsonNode node, String resourcePath) throws DependencySetupException {
    JsonNode codeNode = node.get("code");
    if (codeNode == null || StringUtils.isBlank(codeNode.asText())) {
      throw new DependencySetupException(
          "\"code\" attribute is required and must be non‑blank for Resource "
              + "dependency file: "
              + resourcePath);
    }
  }
}
