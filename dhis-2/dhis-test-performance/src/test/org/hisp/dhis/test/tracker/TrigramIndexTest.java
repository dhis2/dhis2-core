/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.test.tracker;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class TrigramIndexTest extends Simulation {
  private final HttpClient client = HttpClient.newHttpClient();
  private String encodedAuth = Base64.getEncoder().encodeToString(("admin:district").getBytes());

  public TrigramIndexTest() {
    String repeat = System.getProperty("repeat", "100");
    String trackerProgram = System.getProperty("trackerProgram", "ur1Edk5Oe2n");

    setUpTrigramIndexJob();

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .maxConnectionsPerHost(100)
            .userAgentHeader("Gatling/Performance Test")
            .warmUp(
                "http://localhost:8080/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching() // to repeat the same request without HTTP cache influence (304)
            .check(status().is(200)); // global check for all requests

    // only one user at a time
    TrigramIndexTest.ScenarioWithRequests trackerScenario =
        trackerProgramScenario(repeat, trackerProgram);

    List<Assertion> allAssertions = new ArrayList<>();
    allAssertions.add(forAll().successfulRequests().percent().gte(100d));
    allAssertions.addAll(
        trackerScenario.requests().stream().map(TrigramIndexTest.Request::assertion).toList());

    setUp(trackerScenario.scenario().injectClosed(constantConcurrentUsers(1).during(1)))
        .protocols(httpProtocolBuilder)
        .assertions(allAssertions);
  }

  private void setUpTrigramIndexJob() {
    setAttributeTrigramIndexable();
    String jobUid = createTrigramJob();
    if (jobUid == null) {
      return;
    }
    executeTrigramJob(jobUid);
    checkJobStatus(jobUid);
  }

  private void setAttributeTrigramIndexable() {
    try {
      String body =
          "{\"trigramIndexable\":true,\"valueType\":\"TEXT\",\"id\":\"w75KJ2mc4zz\",\"minCharactersToSearch\":3,\"name\":\"First name\",\"shortName\":\"First name\",\"aggregationType\":\"NONE\",\"blockedSearchOperators\":[]}";

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(
                  URI.create(
                      "http://localhost:8080/api/trackedEntityAttributes/w75KJ2mc4zz?mergeMode=REPLACE"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Basic " + encodedAuth)
              .PUT(HttpRequest.BodyPublishers.ofString(body))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      System.out.println("Set attribute indexable status: " + response.statusCode());
      System.out.println("Set attribute indexable body: " + response.body());
    } catch (Exception e) {
      throw new RuntimeException("Failed to set attribute as indexable", e);
    }
  }

  private String createTrigramJob() {
    String uid;
    try {
      String body =
          "{\"name\":\"Tracker search optimization\", \"jobType\":\"TRACKER_TRIGRAM_INDEX_MAINTENANCE\", \"cronExpression\":\"0 0 3 ? * *\"}";

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:8080/api/jobConfigurations"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Basic " + encodedAuth)
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      System.out.println("Create trigram job status: " + response.statusCode());
      System.out.println("Create trigram job body: " + response.body());

      if (response.statusCode() != 200 && response.statusCode() != 201) {
        return null;
        // return getTrigramJobId();
      }

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(response.body());

      uid = root.path("response").path("uid").asText();
      System.out.println("Job created, uid: " + uid);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create job", e);
    }

    return uid;
  }

  private String getTrigramJobId() throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/jobConfigurations"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + encodedAuth)
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println("Get trigram job status: " + response.statusCode());
    System.out.println("Get trigram job body: " + response.body());

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(response.body());

    String trigramJobId = null;
    for (JsonNode job : root.path("jobConfigurations")) {
      if ("Tracker search optimization".equals(job.path("displayName").asText())) {
        trigramJobId = job.path("id").asText();
        break; // stop once found
      }
    }

    System.out.println("Tracker trigram index job ID: " + trigramJobId);
    return trigramJobId;
  }

  private void executeTrigramJob(String uid) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:8080/api/jobConfigurations/" + uid + "/execute"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Basic " + encodedAuth)
              .POST(HttpRequest.BodyPublishers.noBody())
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      System.out.println("Execute trigram job status: " + response.statusCode());
      System.out.println("Execute trigram job body: " + response.body());
    } catch (Exception e) {
      throw new RuntimeException("Failed to execute job", e);
    }
  }

  private void checkJobStatus(String uid) {
    try {
      ObjectMapper mapper = new ObjectMapper();

      boolean jobDone = false;

      while (!jobDone) {
        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        "http://localhost:8080/api/scheduling/completed/TRACKER_TRIGRAM_INDEX_MAINTENANCE"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + encodedAuth)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Check job status: " + response.statusCode());
        System.out.println("Check job status body: " + response.body());

        JsonNode root = mapper.readTree(response.body());
        System.out.println("Job status body: " + response.body());

        JsonNode sequence = root.path("sequence");

        if (sequence.isArray() && !sequence.isEmpty()) {
          JsonNode first = sequence.get(0);
          String summary = first.path("summary").asText();
          String status = first.path("status").asText();
          String jobId = first.path("jobId").asText();

          if ("Job completed".equals(summary) && "SUCCESS".equals(status) && jobId.equals(uid)) {
            jobDone = true;
            System.out.println("Job is completed successfully!");
          } else {
            System.out.println("Job not finished yet: summary=" + summary + ", status=" + status);
            Thread.sleep(1000);
          }
        } else {
          System.out.println("No sequence returned yet, waiting...");
          Thread.sleep(1000);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to check job status", e);
    }
  }

  private TrigramIndexTest.ScenarioWithRequests trackerProgramScenario(
      String repeat, String trackerProgram) {
    String searchTEByName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:Ines"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByNameAndLastName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:Ines&filter=zDhUuAYrxNC:like:Bebea"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    TrigramIndexTest.Request searchTeByNameWithLikeOperator =
        new TrigramIndexTest.Request(
            searchTEByName, 200, "Search TE by name with like operator", "Get a list of TEs");
    TrigramIndexTest.Request searchTeByNameAndLastNameWithLikeOperator =
        new TrigramIndexTest.Request(
            searchTEByNameAndLastName,
            200,
            "Search TE by name and last name with like operator",
            "Get a list of TEs");
    ScenarioBuilder scenarioBuilder =
        scenario("Tracker Program")
            .exec(login())
            .repeat(Integer.parseInt(repeat))
            .on(
                group("Get a list of TEs")
                    .on(
                        exec(searchTeByNameWithLikeOperator.action())
                            .exec(searchTeByNameAndLastNameWithLikeOperator.action())));

    return new TrigramIndexTest.ScenarioWithRequests(
        scenarioBuilder,
        List.of(searchTeByNameWithLikeOperator, searchTeByNameAndLastNameWithLikeOperator));
  }

  private HttpRequestActionBuilder login() {
    return http("Login")
        .post("/api/auth/login")
        .header("Content-Type", "application/json")
        .body(StringBody("{\"username\":\"admin\",\"password\":\"district\"}"))
        .check(status().is(200));
  }

  private record Request(String url, int ninetyPercentile, String name, String... groups) {
    HttpRequestActionBuilder action() {
      return http(name).get(url);
    }

    Assertion assertion() {
      String[] allParts = new String[groups.length + 1];
      System.arraycopy(groups, 0, allParts, 0, groups.length);
      allParts[groups.length] = name;
      return details(allParts).responseTime().percentile(90).lte(ninetyPercentile);
    }
  }

  private record ScenarioWithRequests(
      ScenarioBuilder scenario, List<TrigramIndexTest.Request> requests) {}
}
