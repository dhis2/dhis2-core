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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Explicit, create-only setup for disposable organisation-unit search benchmark databases.
 * Expectations are calculated from the generated tree, never from search responses.
 *
 * @author Morten Svanæs
 */
public final class OrganisationUnitSearchFixture {
  static final ObjectMapper JSON = new ObjectMapper();
  private static final int VERSION = 1;
  private static final int WARDS = 200;
  private static final String COUNTRY = "B0000000001";
  private static final String DATA_SET = "D0000000001";
  private static final String DATA_ELEMENT = "E0000000001";
  private static final String ROLE = "R0000000001";
  private static final String DATA_SET_NAME = "Synthetic OU Search Monthly";
  private static final Properties CONFIG = loadConfig();

  private record Unit(String id, String name, String french, String parent, String path) {
    String displayName(boolean fr) {
      return fr && french != null ? french : name;
    }
  }

  private final int leaves = positiveInt("ouSearch.leafCount", 272000);
  private final int batchSize = positiveInt("ouSearch.batchSize", 1000);
  private final String density = prop("ouSearch.translationDensity", "mixed");
  private final List<Unit> units = new ArrayList<>();
  private final Map<String, List<String>> scopes = new LinkedHashMap<>();
  private final ObjectNode manifest = JSON.createObjectNode();
  private HttpClient http;
  private String baseUrl;

  static String prop(String key, String fallback) {
    return System.getProperty(key, CONFIG.getProperty(key, fallback));
  }

  static String required(String key, String environment) {
    String value = prop(key, System.getenv(environment));
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Supply " + key + " in configFile or " + environment);
    }
    return value;
  }

  static int positiveInt(String key, int fallback) {
    int value = Integer.parseInt(prop(key, Integer.toString(fallback)));
    if (value < 1) throw new IllegalArgumentException(key + " must be positive");
    return value;
  }

  private static Properties loadConfig() {
    Properties properties = new Properties();
    String path = System.getProperty("configFile");
    if (path != null) {
      try (var input = Files.newInputStream(Path.of(path))) {
        properties.load(input);
      } catch (IOException ex) {
        throw new IllegalStateException("Cannot read configFile", ex);
      }
    }
    return properties;
  }

  static String basicAuth(String username, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  private static String uid(char prefix, int index) {
    return String.format(Locale.ROOT, "%c%010d", prefix, index);
  }

  private void add(String id, String name, String french, Unit parent) {
    units.add(
        new Unit(
            id,
            name,
            french,
            parent == null ? null : parent.id(),
            (parent == null ? "" : parent.path()) + "/" + id));
  }

  private void generate() {
    if (leaves < 200) throw new IllegalArgumentException("ouSearch.leafCount must be >= 200");
    if (!Set.of("none", "mixed", "dense").contains(density)) {
      throw new IllegalArgumentException("translationDensity must be none, mixed, or dense");
    }
    add(COUNTRY, "Synthetic Country okp", null, null);
    Unit country = units.get(0);
    add(uid('B', 2), "Synthetic District 0 okp", null, country);
    add(uid('B', 3), "Synthetic District 1 okp", null, country);
    for (int ward = 0; ward < WARDS; ward++) {
      add(
          uid('W', ward),
          "Synthetic Ward " + String.format(Locale.ROOT, "%03d", ward) + (ward == 0 ? " okp" : ""),
          null,
          units.get(1 + ward / 100));
    }
    int rareStride = (leaves + 19) / 20;
    for (int index = 0; index < leaves; index++) {
      String number = String.format(Locale.ROOT, "%010d", index);
      boolean rare = index % rareStride == 0;
      boolean translated = density.equals("dense") || density.equals("mixed") && index % 2 == 0;
      // Contiguous leaf ranges in each ward keep the first browser pages near one tree branch.
      Unit parent = units.get(3 + (int) ((long) index * WARDS / leaves));
      add(
          uid('L', index),
          "Bench Leaf " + number + " Fallback" + (rare ? " Needle okp" : ""),
          translated ? "Bench Traduit " + number + (rare ? " Aiguille okp" : "") : null,
          parent);
    }
    scopes.put("single", List.of(uid('B', 2)));
    scopes.put("multi", List.of(uid('B', 2), uid('B', 3)));
    scopes.put("overlap", List.of(uid('B', 2), uid('W', 0)));
    scopes.put("small", List.of(uid('W', 0)));
    scopes.put("large", List.of(COUNTRY));
    scopes.put("french", List.of(COUNTRY));
    scopes.put("client", List.of(COUNTRY));
    createManifest();
  }

  private boolean inScope(Unit unit, String account) {
    return scopes.get(account).stream()
        .anyMatch(root -> unit.id().equals(root) || unit.path().contains("/" + root + "/"));
  }

  private List<Unit> matches(String account, Predicate<Unit> predicate) {
    return units.stream()
        .filter(unit -> inScope(unit, account))
        .filter(predicate)
        .sorted(Comparator.comparing(Unit::id))
        .toList();
  }

  private static ArrayNode strings(List<String> values) {
    ArrayNode result = JSON.createArrayNode();
    values.forEach(result::add);
    return result;
  }

  private static ObjectNode params(int page) {
    ObjectNode params = JSON.createObjectNode();
    params.put("fields", "id,path,displayName");
    params.put("withinUserSearchHierarchy", "true");
    params.put("paging", "true");
    params.put("pageSize", "50");
    params.put("page", Integer.toString(page));
    params.put("order", "id:asc");
    return params;
  }

  private void addCase(String name, String account, ObjectNode params, Predicate<Unit> predicate) {
    List<Unit> all = matches(account, predicate);
    int page = Integer.parseInt(params.path("page").asText());
    int pageSize = Integer.parseInt(params.path("pageSize").asText());
    int start = Math.min((page - 1) * pageSize, all.size());
    List<Unit> result = all.subList(start, Math.min(start + pageSize, all.size()));
    ObjectNode test = manifest.withArray("cases").addObject();
    test.put("name", name);
    test.put("user", account);
    test.put("endpoint", "/api/organisationUnits");
    test.set("params", params);
    ObjectNode expected = test.putObject("expected");
    expected.set("ids", strings(result.stream().map(Unit::id).toList()));
    expected.set("paths", strings(result.stream().map(Unit::path).toList()));
    expected.set(
        "displayNames",
        strings(result.stream().map(u -> u.displayName(account.equals("french"))).toList()));
    expected.put("total", all.size());
    expected.put("checkTotal", true);
    test.put("url", url("/api/organisationUnits", params));
  }

  private void createManifest() {
    manifest.put("fixtureVersion", VERSION);
    manifest.put("leafCount", leaves);
    manifest.put("wardCount", WARDS);
    manifest.put("translationDensity", density);
    manifest.put("organisationUnitCount", units.size());
    manifest.put("rootId", COUNTRY);
    ObjectNode users = manifest.putObject("users");
    int userIndex = 0;
    for (var entry : scopes.entrySet()) {
      ObjectNode user = users.putObject(entry.getKey());
      user.put("id", uid('U', userIndex++));
      user.put("username", "oubench_" + entry.getKey());
      user.put("dbLocale", entry.getKey().equals("french") ? "fr" : "en");
      user.set("roots", strings(entry.getValue()));
    }
    for (String account : scopes.keySet()) {
      if (account.equals("client")) continue;
      boolean french = account.equals("french");
      Map<String, String> terms = new LinkedHashMap<>();
      terms.put("broad", "Bench");
      terms.put("selective", french && !density.equals("none") ? "Aiguille" : "Needle");
      terms.put("none", "ZZZ_NO_SUCH_OU_918273");
      terms.put("translated", "Traduit");
      terms.put("fallback", "Fallback");
      for (var term : terms.entrySet()) {
        ObjectNode params = params(1);
        params.put("filter", "displayName:ilike:" + term.getValue());
        addCase(
            "display-" + account + "-" + term.getKey(),
            account,
            params,
            u ->
                u.displayName(french)
                    .toLowerCase(Locale.ROOT)
                    .contains(term.getValue().toLowerCase(Locale.ROOT)));
        if (account.equals("large") || french) {
          ObjectNode global = params.deepCopy();
          global.remove("withinUserSearchHierarchy");
          addCase(
              "display-global-" + (french ? "fr-" : "en-") + term.getKey(),
              account,
              global,
              u ->
                  u.displayName(french)
                      .toLowerCase(Locale.ROOT)
                      .contains(term.getValue().toLowerCase(Locale.ROOT)));
        }
      }
      ObjectNode query = params(1);
      query.put("fields", "id,path");
      query.put("pageSize", "15");
      query.put("query", "okp");
      addCase(
          "hierarchy-" + account + (account.equals("small") ? "-list" : "-count"),
          account,
          query,
          u -> u.name().contains("okp"));
      ObjectNode or = params(1);
      or.put("rootJunction", "OR");
      or.set("filter", strings(List.of("displayName:ilike:Needle", "id:eq:" + uid('B', 3))));
      addCase(
          "hierarchy-or-" + account + "-count",
          account,
          or,
          u -> u.displayName(french).contains("Needle") || u.id().equals(uid('B', 3)));
      ObjectNode pageTwo = params(2);
      pageTwo.put("filter", "displayName:ilike:Bench");
      addCase(
          "display-" + account + "-broad-page2",
          account,
          pageTwo,
          u -> u.displayName(french).contains("Bench"));
      ObjectNode roots = params(1);
      roots.put("filter", "id:in:[" + String.join(",", scopes.get(account)) + "]");
      addCase(
          "hierarchy-" + account + "-roots",
          account,
          roots,
          u -> scopes.get(account).contains(u.id()));
    }
    for (int page : List.of(1, 2)) {
      ObjectNode clientQuery = params(page);
      clientQuery.remove("withinUserSearchHierarchy");
      clientQuery.put("fields", "path");
      clientQuery.put("filter", "displayName:ilike:Bench");
      addCase("client-paths-page" + page, "client", clientQuery, u -> u.name().contains("Bench"));
    }
    List<Unit> broad = matches("client", u -> u.name().contains("Bench"));
    List<Unit> replacement = matches("client", u -> u.name().contains("Needle"));
    ObjectNode client = manifest.putObject("client");
    client.put("username", "oubench_client");
    client.put("dataSetId", DATA_SET);
    client.put("dataSetName", DATA_SET_NAME);
    client.put("searchTerm", "Bench");
    client.put("replacementTerm", "Needle");
    client.put("pageSize", 50);
    client.put("total", broad.size());
    client.put("replacementTotal", replacement.size());
    client.set("firstPagePaths", strings(broad.subList(0, 50).stream().map(Unit::path).toList()));
    client.set(
        "secondPagePaths", strings(broad.subList(50, 100).stream().map(Unit::path).toList()));
    client.set("replacementPaths", strings(replacement.stream().map(Unit::path).toList()));
    Set<String> visible = new LinkedHashSet<>();
    broad.subList(0, 100).forEach(u -> visible.addAll(List.of(u.path().substring(1).split("/"))));
    replacement.forEach(u -> visible.addAll(List.of(u.path().substring(1).split("/"))));
    ArrayNode orgUnits = client.putArray("orgUnits");
    units.stream()
        .filter(u -> visible.contains(u.id()))
        .forEach(
            u -> {
              ObjectNode node = orgUnits.addObject();
              node.put("id", u.id());
              node.put("path", u.path());
              node.put("displayName", u.name());
            });
  }

  static String url(String endpoint, JsonNode params) {
    List<String> pairs = new ArrayList<>();
    params
        .fields()
        .forEachRemaining(
            entry -> {
              JsonNode value = entry.getValue();
              if (value.isArray())
                value.forEach(item -> pairs.add(entry.getKey() + "=" + encode(item.asText())));
              else pairs.add(entry.getKey() + "=" + encode(value.asText()));
            });
    return endpoint + "?" + String.join("&", pairs);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private JsonNode request(String method, String path, JsonNode body, String authorization)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofMinutes(10))
            .header("Accept", "application/json");
    if (authorization != null) builder.header("Authorization", authorization);
    if (body != null) builder.header("Content-Type", "application/json");
    builder.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
    HttpResponse<String> response =
        http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    // Never include response bodies: rejected user imports may echo password fields.
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException(
          method
              + " "
              + path
              + " returned HTTP "
              + response.statusCode()
              + "; inspect the disposable server log (request bodies are not logged here)");
    }
    JsonNode result =
        response.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(response.body());
    if (result.has("status") && !Set.of("OK", "SUCCESS").contains(result.path("status").asText())) {
      throw new IOException(method + " " + path + " returned non-success status");
    }
    return result;
  }

  private void metadata(ObjectNode objects) throws IOException, InterruptedException {
    JsonNode report =
        request(
            "POST",
            "/api/metadata?importStrategy=CREATE&atomicMode=ALL"
                + "&async=false&importReportMode=ERRORS",
            objects,
            null);
    if (!report.path("response").path("status").asText().equals("OK")) {
      throw new IOException("Metadata import did not report OK; inspect disposable server log");
    }
  }

  private void seed() throws IOException, InterruptedException {
    if (!prop("ouSearch.setup", "").equals("CREATE_DISPOSABLE_FIXTURE")) {
      throw new IllegalArgumentException(
          "Seeding requires -DouSearch.setup=CREATE_DISPOSABLE_FIXTURE");
    }
    baseUrl = required("baseUrl", "DHIS2_BASE_URL").replaceAll("/+$", "");
    String syntheticPassword = required("ouSearch.password", "OU_SEARCH_PASSWORD");
    http =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    request(
        "GET",
        "/api/me",
        null,
        basicAuth(prop("username", "admin"), required("password", "DHIS2_PASSWORD")));
    JsonNode existing = request("GET", "/api/organisationUnits?fields=id&pageSize=1", null, null);
    if (!existing.path("organisationUnits").isArray()
        || !existing.path("organisationUnits").isEmpty()) {
      throw new IllegalStateException(
          "Setup requires a fresh database with no organisation units; restore an empty DB");
    }
    String browserOrigins = prop("ouSearch.corsOrigins", "");
    if (!browserOrigins.isBlank()) {
      Set<String> origins = new LinkedHashSet<>();
      request("GET", "/api/configuration/corsWhitelist", null, null)
          .forEach(origin -> origins.add(origin.asText()));
      for (String origin : browserOrigins.split(",")) {
        if (!origin.isBlank()) origins.add(origin.trim());
      }
      request("POST", "/api/configuration/corsWhitelist", strings(new ArrayList<>(origins)), null);
    }
    // Separate levels ensure every parent exists before its children are imported.
    importUnits(units.subList(0, 1));
    importUnits(units.subList(1, 3));
    importUnits(units.subList(3, 3 + WARDS));
    importUnits(units.subList(3 + WARDS, units.size()));

    ObjectNode aggregate = JSON.createObjectNode();
    ObjectNode element = aggregate.putArray("dataElements").addObject();
    element.put("id", DATA_ELEMENT);
    element.put("name", "Synthetic OU Search Count");
    element.put("shortName", "Synthetic OU Search Count");
    element.put("domainType", "AGGREGATE");
    element.put("valueType", "INTEGER_ZERO_OR_POSITIVE");
    element.put("aggregationType", "SUM");
    element.putObject("sharing").put("public", "r-------");
    ObjectNode dataSet = aggregate.putArray("dataSets").addObject();
    dataSet.put("id", DATA_SET);
    dataSet.put("name", DATA_SET_NAME);
    dataSet.put("shortName", DATA_SET_NAME);
    dataSet.put("periodType", "Monthly");
    dataSet.put("openFuturePeriods", 12);
    dataSet.put("expiryDays", 0);
    dataSet.putObject("sharing").put("public", "r-rw----");
    ObjectNode assignment = dataSet.putArray("dataSetElements").addObject();
    assignment.putObject("dataElement").put("id", DATA_ELEMENT);
    assignment.putObject("dataSet").put("id", DATA_SET);
    ArrayNode assigned = dataSet.putArray("organisationUnits");
    units.stream()
        .filter(u -> u.id().startsWith("L"))
        .filter(u -> u.id().compareTo(uid('L', 200)) < 0 || u.name().contains("Needle"))
        .forEach(u -> assigned.addObject().put("id", u.id()));
    metadata(aggregate);

    ObjectNode access = JSON.createObjectNode();
    ObjectNode role = access.putArray("userRoles").addObject();
    role.put("id", ROLE);
    role.put("name", "Synthetic OU Search Capture");
    role.set("authorities", strings(List.of("F_DATAVALUE_ADD")));
    role.putArray("dataSets").addObject().put("id", DATA_SET);
    metadata(access);
    ObjectNode accounts = JSON.createObjectNode();
    ArrayNode userArray = accounts.putArray("users");
    manifest
        .path("users")
        .fields()
        .forEachRemaining(
            entry -> {
              JsonNode account = entry.getValue();
              ObjectNode user = userArray.addObject();
              user.put("id", account.path("id").asText());
              user.put("username", account.path("username").asText());
              user.put("firstName", "Synthetic");
              user.put("surname", "OU Search " + entry.getKey());
              user.put("password", syntheticPassword);
              user.putArray("userRoles").addObject().put("id", ROLE);
              for (String field :
                  List.of(
                      "organisationUnits",
                      "dataViewOrganisationUnits",
                      "teiSearchOrganisationUnits")) {
                ArrayNode roots = user.putArray(field);
                account.path("roots").forEach(root -> roots.addObject().put("id", root.asText()));
              }
            });
    metadata(accounts);
    for (JsonNode user : manifest.path("users")) {
      request(
          "POST",
          "/api/userSettings/keyDbLocale?user="
              + encode(user.path("username").asText())
              + "&value="
              + user.path("dbLocale").asText(),
          null,
          null);
    }
  }

  private void importUnits(List<Unit> group) throws IOException, InterruptedException {
    for (int start = 0; start < group.size(); start += batchSize) {
      ObjectNode batch = JSON.createObjectNode();
      ArrayNode objects = batch.putArray("organisationUnits");
      for (Unit unit : group.subList(start, Math.min(start + batchSize, group.size()))) {
        ObjectNode object = objects.addObject();
        object.put("id", unit.id());
        object.put("name", unit.name());
        object.put("shortName", unit.name());
        object.put("openingDate", "2020-01-01");
        if (unit.parent() != null) object.putObject("parent").put("id", unit.parent());
        if (unit.french() != null) {
          ObjectNode translation = object.putArray("translations").addObject();
          translation.put("locale", "fr");
          translation.put("property", "NAME");
          translation.put("value", unit.french());
        }
      }
      metadata(batch);
      System.out.println("Imported OU batch: " + objects.size() + " objects");
    }
  }

  public static void main(String[] args) throws Exception {
    String operation = prop("ouSearch.operation", "generate");
    if (!Set.of("generate", "seed").contains(operation)) {
      throw new IllegalArgumentException("ouSearch.operation must be generate or seed");
    }
    OrganisationUnitSearchFixture fixture = new OrganisationUnitSearchFixture();
    fixture.generate();
    if (operation.equals("seed")) fixture.seed();
    Path output = Path.of(prop("ouSearch.manifest", "target/ou-search-manifest.json"));
    Path parent = output.toAbsolutePath().getParent();
    Files.createDirectories(parent);
    JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), fixture.manifest);
    System.out.println(
        "Fixture v"
            + VERSION
            + " manifest written to "
            + output
            + " ("
            + operation
            + "; "
            + fixture.units.size()
            + " OUs)");
  }
}
