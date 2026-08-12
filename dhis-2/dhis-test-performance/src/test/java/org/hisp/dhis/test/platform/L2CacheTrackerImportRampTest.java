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

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tracker-import ramp simulation for the L2-cache truth mission (Phase 4 primary workload).
 *
 * <p>Concurrent {@code POST /api/tracker?async=false} of synthetic events against an option-heavy
 * event program (default: Inpatient morbidity and mortality {@code eBAyeGv0exc}, Sierra Leone demo
 * DB), mixed with capture-style metadata reads. Event data values are drawn randomly from the
 * program's real option sets, fetched at simulation start (option-set sizes are logged so every run
 * records how option-heavy the workload actually is). The import path repeatedly query-loads
 * reference metadata (options, option sets, data elements, org units), which under READ_WRITE L2
 * regions takes the region write lock per hydrated row -- the convoy mechanism this workload exists
 * to measure.
 *
 * <p>Sync vs async: the import is measured SYNCHRONOUSLY ({@code async=false}) so request latency
 * attributes the full import transaction cost (validation, preheat, persistence) to the request.
 * Uganda's pipeline uses the async default, but async would only measure job-enqueue latency, which
 * is useless for region-lock attribution. The DB starts fresh per harness run (seeded volume), so
 * within-run growth is part of the workload and identical across matrix cells.
 *
 * <p>Additional properties on top of {@link L2CacheRampSimulation}:
 *
 * <ul>
 *   <li>{@code trackerProgramUid} (default: {@code eBAyeGv0exc}) event program under load
 *   <li>{@code importMode} (default: {@code single}): {@code single} = capture-style one event per
 *       POST; {@code batch} = Uganda-style sync payloads of {@code eventsPerRequest} events
 *   <li>{@code eventsPerRequest} (default: {@code 100}) events per POST in {@code batch} mode
 * </ul>
 *
 * @author Morten Svanæs
 */
public class L2CacheTrackerImportRampTest extends L2CacheRampSimulation {

  private static final String PROGRAM_UID = prop("trackerProgramUid", "eBAyeGv0exc");
  private static final String IMPORT_MODE = prop("importMode", "single");
  private static final int EVENTS_PER_REQUEST = Integer.parseInt(prop("eventsPerRequest", "100"));

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** One data element of the program stage plus the value pool to draw from. */
  private record DataElementSpec(String uid, String valueType, List<String> optionCodes) {}

  private record ProgramSpec(
      String programUid,
      String stageUid,
      List<String> orgUnits,
      List<DataElementSpec> elements,
      List<String> optionSetUids) {}

  private static final ProgramSpec PROGRAM = fetchProgramSpec();

  /** Option set UIDs of the program, fed into capture-style option-set reads. */
  private static final FeederBuilder<Object> OPTION_SET_FEEDER = optionSetFeeder();

  public L2CacheTrackerImportRampTest() {
    install(L2CacheTrackerImportRampTest::workflow);
  }

  private static ChainBuilder workflow(String p) {
    // Capture-style reads: the metadata the Capture app hits while users enter events. The
    // option-set read hammers the Option/OptionSet regions the import path also touches.
    ChainBuilder reads =
        exec(http(p + " me").get("/api/me").check(status().is(200)))
            .feed(OPTION_SET_FEEDER)
            .exec(
                http(p + " optionSet byId")
                    .get("/api/optionSets/#{osUid}")
                    .queryParam("fields", "id,name,valueType,options[id,name,code]")
                    .check(status().is(200)))
            .exec(
                http(p + " events workingList")
                    .get("/api/tracker/events")
                    .queryParam("program", PROGRAM.programUid())
                    .queryParam("orgUnit", ORG_UNIT_UID)
                    .queryParam("orgUnitMode", "DESCENDANTS")
                    .queryParam("order", "occurredAt:desc")
                    .queryParam("pageSize", "25")
                    .check(status().is(200)));

    int eventsPerPost = "batch".equalsIgnoreCase(IMPORT_MODE) ? EVENTS_PER_REQUEST : 1;
    String importName =
        p + (eventsPerPost == 1 ? " tracker import single" : " tracker import batch");
    ChainBuilder importEvents =
        exec(
            http(importName)
                .post("/api/tracker")
                .queryParam("async", "false")
                .header("Content-Type", "application/json")
                .body(StringBody(session -> eventsPayload(eventsPerPost)))
                .check(status().is(200)));

    return reads.exec(importEvents);
  }

  // -------------------------------------------------------------------------
  // Synthetic event payloads
  // -------------------------------------------------------------------------

  private static String eventsPayload(int eventCount) {
    ObjectNode root = MAPPER.createObjectNode();
    ArrayNode events = root.putArray("events");
    for (int i = 0; i < eventCount; i++) {
      events.add(randomEvent());
    }
    return root.toString();
  }

  private static ObjectNode randomEvent() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    ObjectNode event = MAPPER.createObjectNode();
    event.put("program", PROGRAM.programUid());
    event.put("programStage", PROGRAM.stageUid());
    event.put("orgUnit", PROGRAM.orgUnits().get(random.nextInt(PROGRAM.orgUnits().size())));
    event.put("status", "ACTIVE");
    event.put("occurredAt", LocalDate.now().minusDays(random.nextInt(90)).toString());
    ArrayNode dataValues = event.putArray("dataValues");
    for (DataElementSpec spec : PROGRAM.elements()) {
      String value = randomValue(spec, random);
      if (value == null) {
        continue;
      }
      ObjectNode dataValue = dataValues.addObject();
      dataValue.put("dataElement", spec.uid());
      dataValue.put("value", value);
    }
    return event;
  }

  private static String randomValue(DataElementSpec spec, ThreadLocalRandom random) {
    if (!spec.optionCodes().isEmpty()) {
      return spec.optionCodes().get(random.nextInt(spec.optionCodes().size()));
    }
    return switch (spec.valueType()) {
      case "INTEGER", "INTEGER_POSITIVE", "INTEGER_ZERO_OR_POSITIVE" ->
          String.valueOf(random.nextInt(1, 99));
      case "INTEGER_NEGATIVE" -> String.valueOf(-random.nextInt(1, 99));
      case "NUMBER", "PERCENTAGE" -> String.valueOf(random.nextInt(1, 99)) + ".5";
      case "UNIT_INTERVAL" -> "0." + random.nextInt(1, 9);
      case "BOOLEAN" -> String.valueOf(random.nextBoolean());
      case "TRUE_ONLY" -> "true";
      case "DATE" -> LocalDate.now().minusDays(random.nextInt(365)).toString();
      case "DATETIME" -> LocalDate.now().minusDays(random.nextInt(365)) + "T10:00:00.000";
      case "TIME" -> "10:30";
      case "PHONE_NUMBER" -> "+4712345678";
      case "EMAIL" -> "l2-cache-truth@example.com";
      case "TEXT", "LONG_TEXT" -> "l2 perf " + random.nextInt(1_000_000);
      // FILE_RESOURCE, IMAGE, COORDINATE, ORGANISATION_UNIT, USERNAME... are skipped
      default -> null;
    };
  }

  // -------------------------------------------------------------------------
  // Program metadata fetched once at simulation start
  // -------------------------------------------------------------------------

  private static ProgramSpec fetchProgramSpec() {
    String url =
        BASE_URL
            + "/api/programs/"
            + PROGRAM_UID
            + ".json?fields=id,name,programType,categoryCombo[id,name],organisationUnits[id],"
            + "programStages[id,name,programStageDataElements[dataElement[id,name,valueType,"
            + "optionSet[id,name,options[code]]]]]";
    JsonNode program = getJson(url);

    if (!"WITHOUT_REGISTRATION".equals(program.path("programType").asText())) {
      throw new IllegalStateException(
          "Program " + PROGRAM_UID + " is not an event program: " + program.path("programType"));
    }
    JsonNode stages = program.path("programStages");
    if (stages.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly one program stage on " + PROGRAM_UID + ", found " + stages.size());
    }
    JsonNode stage = stages.get(0);

    List<String> orgUnits = new ArrayList<>();
    for (JsonNode orgUnit : program.path("organisationUnits")) {
      orgUnits.add(orgUnit.path("id").asText());
    }
    if (orgUnits.isEmpty()) {
      throw new IllegalStateException("Program " + PROGRAM_UID + " has no org units assigned");
    }

    List<DataElementSpec> elements = new ArrayList<>();
    List<String> optionSetUids = new ArrayList<>();
    int totalOptions = 0;
    for (JsonNode psde : stage.path("programStageDataElements")) {
      JsonNode dataElement = psde.path("dataElement");
      List<String> optionCodes = new ArrayList<>();
      JsonNode optionSet = dataElement.path("optionSet");
      if (!optionSet.isMissingNode()) {
        for (JsonNode option : optionSet.path("options")) {
          optionCodes.add(option.path("code").asText());
        }
        if (!optionCodes.isEmpty()) {
          if (!optionSetUids.contains(optionSet.path("id").asText())) {
            optionSetUids.add(optionSet.path("id").asText());
          }
          totalOptions += optionCodes.size();
          System.out.println(
              "[L2CacheTrackerImportRampTest] optionSet "
                  + optionSet.path("name").asText()
                  + " ("
                  + optionSet.path("id").asText()
                  + ") size="
                  + optionCodes.size()
                  + " on dataElement "
                  + dataElement.path("name").asText());
        }
      }
      elements.add(
          new DataElementSpec(
              dataElement.path("id").asText(),
              dataElement.path("valueType").asText(),
              List.copyOf(optionCodes)));
    }
    if (elements.isEmpty()) {
      throw new IllegalStateException("Program stage of " + PROGRAM_UID + " has no data elements");
    }

    System.out.println(
        "[L2CacheTrackerImportRampTest] program "
            + program.path("name").asText()
            + " ("
            + PROGRAM_UID
            + "): stage "
            + stage.path("id").asText()
            + ", "
            + elements.size()
            + " data elements, "
            + optionSetUids.size()
            + " distinct option sets with "
            + totalOptions
            + " options total, "
            + orgUnits.size()
            + " org units, categoryCombo="
            + program.path("categoryCombo").path("name").asText()
            + ", importMode="
            + IMPORT_MODE
            + (("batch".equalsIgnoreCase(IMPORT_MODE))
                ? " (" + EVENTS_PER_REQUEST + " events/request)"
                : ""));

    return new ProgramSpec(
        program.path("id").asText(),
        stage.path("id").asText(),
        List.copyOf(orgUnits),
        List.copyOf(elements),
        List.copyOf(optionSetUids));
  }

  private static FeederBuilder<Object> optionSetFeeder() {
    if (PROGRAM.optionSetUids().isEmpty()) {
      throw new IllegalStateException(
          "Program " + PROGRAM_UID + " has no option sets; the option-heavy workload needs them");
    }
    List<Map<String, Object>> records = new ArrayList<>();
    for (String osUid : PROGRAM.optionSetUids()) {
      records.add(Map.of("osUid", osUid));
    }
    return listFeeder(records).random();
  }

  private static JsonNode getJson(String url) {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Authorization", "Basic " + BASIC_AUTH)
              .GET()
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException(url + " failed with HTTP " + response.statusCode());
      }
      return MAPPER.readTree(response.body());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch " + url, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while fetching " + url, e);
    }
  }
}
