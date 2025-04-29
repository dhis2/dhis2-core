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
package org.hisp.dhis.dependsOn.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dependsOn.DependencySetupException;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.config.TestConfiguration;

/**
 * Template implementation of {@link ResourceService} that covers the common HTTP interactions and
 * validation logic. Concrete subclasses supply only the resource‑specific constants.
 */
@Slf4j
public abstract class BaseResourceService implements ResourceService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final RestApiActions client; // /29/indicators OR /29/programIndicators
  private final String jsonCollection; // "indicators" OR "programIndicators"
  private final String label; // "Indicator" OR "ProgramIndicator"

  protected BaseResourceService(String basePath, String jsonCollection, String label) {
    this.client = new RestApiActions(basePath).setBaseUri(TestConfiguration.get().baseUrl());
    this.jsonCollection = jsonCollection;
    this.label = label;
  }

  @Override
  public Optional<String> lookup(String code) throws DependencySetupException {
    LoginActions.addAuthenticationHeader(
        TestConfiguration.get().adminUserUsername(), TestConfiguration.get().adminUserPassword());
    log.debug("Looking up {} by code='{}'", label, code);

    QueryParamsBuilder q =
        new QueryParamsBuilder()
            .add("filter", "identifiable:token:" + code)
            .add("filter", "name:ne:default");

    ApiResponse rsp = client.get(q);
    if (rsp.statusCode() != 200) {
      throw new DependencySetupException(label + " lookup returned HTTP " + rsp.statusCode());
    }

    try {
      JsonNode root = MAPPER.readTree(rsp.getAsString());
      List<String> ids = root.path(jsonCollection).findValuesAsText("id");

      return switch (ids.size()) {
        case 0 -> Optional.empty();
        case 1 -> Optional.of(ids.get(0));
        default ->
            throw new DependencySetupException(
                "Duplicate " + label + "s with code '" + code + "' found: " + ids);
      };
    } catch (IOException io) {
      throw new DependencySetupException("Cannot parse lookup JSON", io);
    }
  }

  @Override
  public String create(JsonNode payload) throws DependencySetupException {
    JsonNode clean = payload.deepCopy();
    if (clean.isObject()) {
      ((com.fasterxml.jackson.databind.node.ObjectNode) clean).remove("type");
    }

    String code = clean.path("code").asText();
    log.debug("Creating {} with code='{}'", label, code);

    ApiResponse rsp = client.post(clean.toString());
    if (rsp.statusCode() != 201) {
      throw new DependencySetupException(label + " create returned HTTP " + rsp.statusCode());
    }

    // check errorReports
    try {
      JsonNode errs = MAPPER.readTree(rsp.getAsString()).path("response").path("errorReports");
      if (errs.isArray() && !errs.isEmpty()) {
        throw new DependencySetupException(label + " create returned errorReports: " + errs);
      }
    } catch (IOException io) {
      throw new DependencySetupException("Cannot parse create JSON", io);
    }

    return lookup(code)
        .orElseThrow(
            () ->
                new DependencySetupException(
                    label + " created but UID not found for code '" + code + "'"));
  }

  @Override
  public void delete(String uid) throws DependencySetupException {
    log.debug("Deleting {} uid={}", label, uid);
    ApiResponse rsp = client.delete("/" + uid);
    int status = rsp.statusCode();

    if (status != 200 && status != 204) {
      throw new DependencySetupException(label + " delete uid=" + uid + " returned HTTP " + status);
    }
  }
}
