/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.test.platform;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static org.hisp.dhis.test.platform.OrganisationUnitSearchFixture.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only, exact-result benchmark for translated filtering and hierarchy scope queries. Setup is
 * exclusively the separate {@link OrganisationUnitSearchFixture} CLI.
 *
 * @author Morten Svanæs
 */
public class OrganisationUnitSearchPerformanceTest extends Simulation {
  public OrganisationUnitSearchPerformanceTest() throws IOException {
    JsonNode manifest =
        JSON.readTree(
            Path.of(prop("ouSearch.manifest", "target/ou-search-manifest.json")).toFile());
    if (manifest.path("fixtureVersion").asInt() != 1
        || !manifest.path("cases").isArray()
        || manifest.path("cases").isEmpty()) {
      throw new IllegalArgumentException(
          "Expected organisation-unit search fixtureVersion=1 manifest with cases");
    }
    int concurrency = positiveInt("concurrency", 1);
    int iterations = positiveInt("iterations", 3);
    String mode = prop("mode", "sequential");
    if (!Set.of("sequential", "parallel").contains(mode)) {
      throw new IllegalArgumentException("mode must be sequential or parallel");
    }
    String selected = prop("ouSearch.cases", "all");
    Set<String> requested = new LinkedHashSet<>(Arrays.asList(selected.split(",")));
    Set<String> found = new LinkedHashSet<>();
    List<PopulationBuilder> populations = new ArrayList<>();
    List<Assertion> assertions = new ArrayList<>();
    String password = required("ouSearch.password", "OU_SEARCH_PASSWORD");
    String p95 = prop("ouSearch.p95Ms", "");
    if (!p95.isBlank() && Integer.parseInt(p95) <= 0) {
      throw new IllegalArgumentException("ouSearch.p95Ms must be positive when supplied");
    }
    for (JsonNode test : manifest.path("cases")) {
      String name = test.path("name").asText();
      if (!selected.equals("all") && !requested.contains(name)) continue;
      if (!found.add(name)) throw new IllegalArgumentException("Duplicate case name: " + name);
      JsonNode expected = test.path("expected");
      if (!expected.path("ids").isArray()
          || !expected.path("paths").isArray()
          || !expected.path("displayNames").isArray()
          || !expected.path("total").isIntegralNumber()
          || !expected.path("checkTotal").isBoolean()) {
        throw new IllegalArgumentException("Invalid expectation for " + name);
      }
      String username =
          manifest.path("users").path(test.path("user").asText()).path("username").asText();
      if (username.isBlank())
        throw new IllegalArgumentException("Missing manifest username for " + name);
      boolean checkTotal = expected.path("checkTotal").asBoolean();
      Set<String> fields = Set.of(test.path("params").path("fields").asText().split(","));
      ObjectNode signature = JSON.createObjectNode();
      if (fields.contains("id")) signature.set("ids", expected.path("ids"));
      if (fields.contains("path")) signature.set("paths", expected.path("paths"));
      if (fields.contains("displayName"))
        signature.set("displayNames", expected.path("displayNames"));
      if (checkTotal) signature.set("total", expected.path("total"));
      var authenticate =
          exec(flushCookieJar())
              .exec(
                  http("Authenticate - " + name)
                      .get("/api/me?fields=id,username")
                      .header("Authorization", basicAuth(username, password))
                      .check(status().is(200), jsonPath("$.username").is(username)));
      var query =
          http(name)
              .get(url(test.path("endpoint").asText(), test.path("params")))
              .check(
                  status().is(200),
                  bodyString()
                      .transform(body -> responseSignature(body, fields, checkTotal))
                      .is(signature.toString()));
      // One cookie-authenticated session per virtual user; no Basic header on timed searches.
      // Check failures remain normal Gatling KOs, including known baseline semantic defects.
      populations.add(
          scenario(name)
              .exec(authenticate)
              .exitHereIfFailed()
              .repeat(iterations)
              .on(exec(query))
              .injectOpen(atOnceUsers(concurrency)));
      assertions.add(details(name).successfulRequests().percent().is(100D));
      if (!p95.isBlank())
        assertions.add(details(name).responseTime().percentile(95).lt(Integer.parseInt(p95)));
    }
    if (!selected.equals("all") && !found.equals(requested)) {
      requested.removeAll(found);
      throw new IllegalArgumentException("Unknown ouSearch.cases: " + requested);
    }
    if (populations.isEmpty()) throw new IllegalArgumentException("No selected cases");
    // Also fail failed authentication rather than silently publishing an empty measurement.
    assertions.add(global().successfulRequests().percent().is(100D));
    var protocol =
        http.baseUrl(required("baseUrl", "DHIS2_BASE_URL"))
            .acceptHeader("application/json")
            .disableCaching();
    var simulation =
        mode.equals("parallel")
            ? setUp(populations.toArray(PopulationBuilder[]::new))
            : setUp(sequential(populations, 0));
    simulation.protocols(protocol).assertions(assertions.toArray(Assertion[]::new));
  }

  private static PopulationBuilder sequential(List<PopulationBuilder> populations, int index) {
    PopulationBuilder current = populations.get(index);
    return index + 1 == populations.size()
        ? current
        : current.andThen(sequential(populations, index + 1));
  }

  private static String responseSignature(String body, Set<String> fields, boolean checkTotal) {
    try {
      JsonNode response = JSON.readTree(body);
      JsonNode units = response.path("organisationUnits");
      if (!units.isArray()) return "Missing organisationUnits array";
      ObjectNode result = JSON.createObjectNode();
      for (String field : List.of("id", "path", "displayName")) {
        if (!fields.contains(field)) continue;
        String key =
            switch (field) {
              case "id" -> "ids";
              case "path" -> "paths";
              default -> "displayNames";
            };
        var values = result.putArray(key);
        for (JsonNode unit : units) {
          if (!unit.path(field).isTextual()) return "Missing organisation-unit " + field;
          values.add(unit.path(field).asText());
        }
      }
      if (checkTotal) {
        JsonNode total = response.path("pager").path("total");
        if (!total.isIntegralNumber()) return "Missing pager.total";
        result.set("total", total);
      }
      return result.toString();
    } catch (IOException ex) {
      return "Invalid JSON response";
    }
  }
}
