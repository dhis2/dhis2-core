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
package org.hisp.dhis.test.cache;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance benchmark for the API ETag cache under concurrent METADATA MUTATIONS.
 *
 * <p>The read-only {@link PageLoadSimulation} leaves the invalidation machinery (JDBC DML observer
 * -> version bump -> ETag rotation) idle. This simulation adds rate-controlled writers mutating a
 * dedicated seeded pool of metadata objects (name prefix {@code PERF_}) so that path is exercised
 * and measured.
 *
 * <p><b>Profiles</b> ({@code -Dprofile}):
 *
 * <ul>
 *   <li>{@code writeload} — page-load readers + writers at {@code writeRate} writes/sec. Measures
 *       304 share and latency as a function of write rate. With {@code -DwriteTarget=control} the
 *       writers mutate a type the readers never fetch, asserting per-type isolation (304 share must
 *       NOT degrade).
 *   <li>{@code staleness} — after each mutation, immediately re-GET the affected list with the
 *       pre-mutation ETag and count attempts until the server answers 200. Asserts p99 &lt;= 2
 *       attempts: a mutation must invalidate on the very next conditional request.
 *   <li>{@code writecost} — writers only, unpaced. Run twice with {@code cache.api.etag.enabled}
 *       on/off (see {@code scripts/etag-mutation-bench.sh}) to measure the DML observer's
 *       write-path overhead.
 * </ul>
 *
 * <p>The pool is self-cleaning: leftovers matching {@code name:like:PERF_} are deleted at startup
 * and teardown, so runs are repeatable without DB drift. Writers cycle UPDATE (description toggle),
 * and every 10th iteration CREATE a new object and DELETE an old one, so the observer sees all
 * three DML operations.
 *
 * <p>Run:
 *
 * <pre>{@code
 * mvn gatling:test \
 *   -Dgatling.simulationClass=org.hisp.dhis.test.cache.MetadataMutationSimulation \
 *   -Dinstance=http://localhost:8080 -DapiVersion=43 \
 *   -Dprofile=writeload -DwriteRate=1 -DwriteTarget=hot
 * }</pre>
 *
 * @author Morten Svanaes
 */
public class MetadataMutationSimulation extends Simulation {
  private static final Logger log = LoggerFactory.getLogger(MetadataMutationSimulation.class);

  private static final String POOL_PREFIX = "PERF_";
  private static final Pattern UID_PATTERN =
      Pattern.compile("\"id\"\\s*:\\s*\"([A-Za-z0-9]{11})\"");

  /** Staleness attempt buckets: index 1..10 = resolved on that attempt, 11 = never resolved. */
  private static final int STALENESS_EXHAUSTED_BUCKET = 11;

  private static final AtomicLongArray STALENESS_ATTEMPTS =
      new AtomicLongArray(STALENESS_EXHAUSTED_BUCKET + 1);

  private static final AtomicLong WRITES = new AtomicLong();

  private static final ConcurrentLinkedQueue<String> HOT_POOL = new ConcurrentLinkedQueue<>();
  private static final ConcurrentLinkedQueue<String> CONTROL_POOL = new ConcurrentLinkedQueue<>();

  // -- Configuration via system properties --
  private final String instance = prop("instance", "http://localhost:8080");
  private final String adminUser = prop("adminUser", "admin");
  private final String adminPassword = prop("adminPassword", "district");
  private final String apiVersion = prop("apiVersion", "44");
  private final Profile profile = Profile.fromString(prop("profile", "writeload"));
  private final WriteTarget writeTarget = WriteTarget.fromString(prop("writeTarget", "hot"));
  private final double writeRate = doubleProp("writeRate", 1.0);
  private final int readers = intProp("readers", 10);
  private final int writers = intProp("writers", 2);
  private final int durationSec = intProp("durationSec", 120);
  private final int poolSize = intProp("poolSize", 200);
  private final double assertMin304 =
      doubleProp("assertMin304", writeTarget == WriteTarget.CONTROL ? 0.75 : 0.5);
  private final String dashboardUid = prop("dashboardUid", "nghVC4wtyzi");

  private final String api = "/api/" + apiVersion;
  private final Api rest = new Api(instance, adminUser, adminPassword);

  public MetadataMutationSimulation() {
    CacheScenarios.resetCounters();
    WRITES.set(0);
    for (int i = 0; i < STALENESS_ATTEMPTS.length(); i++) {
      STALENESS_ATTEMPTS.set(i, 0);
    }

    log.info(
        "Metadata Mutation Benchmark: profile={}, writeTarget={}, writeRate={}/s, readers={}, "
            + "writers={}, durationSec={}, poolSize={}, instance={}",
        profile,
        writeTarget,
        writeRate,
        readers,
        writers,
        durationSec,
        poolSize,
        instance);

    HttpProtocolBuilder readerProtocol =
        http.baseUrl(instance)
            .acceptHeader("application/json")
            .header("X-Requested-With", "XMLHttpRequest");

    // Writers and the staleness prober manage conditional headers themselves; Gatling's own
    // HTTP cache would otherwise attach If-None-Match automatically and mask the measurement.
    HttpProtocolBuilder writerProtocol =
        http.baseUrl(instance)
            .acceptHeader("application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .disableCaching();

    CacheScenarios scenarios =
        new CacheScenarios(api, adminUser, adminPassword, dashboardUid, true);

    List<PopulationBuilder> populations = new ArrayList<>();
    List<Assertion> assertions = new ArrayList<>();
    assertions.add(forAll().successfulRequests().percent().gte(99.0));

    switch (profile) {
      case WRITELOAD -> {
        // The page-load mix does not fetch the mutated ("hot") type, so append list reads of
        // it to each cycle. With writeTarget=hot these are what mutations invalidate; with
        // writeTarget=control they prove isolation (their 304s must survive constant writes).
        ChainBuilder hotReads =
            exec(
                http("dataElements (hot list)")
                    .get(api + "/dataElements?fields=id,displayName&pageSize=50")
                    .check(scenarios.cacheableStatus()),
                http("dataElements (hot page 2)")
                    .get(api + "/dataElements?fields=id,displayName&pageSize=50&page=2")
                    .check(scenarios.cacheableStatus()));
        ScenarioBuilder readerScenario =
            scenario("Readers (page loads)")
                .exec(scenarios.login())
                .during(Duration.ofSeconds(durationSec))
                .on(scenarios.appCycle(true).exec(hotReads));
        populations.add(
            readerScenario
                .injectClosed(
                    rampConcurrentUsers(0).to(readers).during(Duration.ofSeconds(10)),
                    constantConcurrentUsers(readers).during(Duration.ofSeconds(durationSec)))
                .protocols(readerProtocol));
        if (writeRate > 0 && writers > 0) {
          populations.add(writerPopulation(scenarios, writerProtocol, pacedWriterCycle()));
        }
      }
      case STALENESS -> populations.add(stalenessPopulation(scenarios, writerProtocol));
      case WRITECOST -> populations.add(writerPopulation(scenarios, writerProtocol, writerCycle()));
      default -> throw new IllegalArgumentException("Unknown profile: " + profile);
    }

    setUp(populations).assertions(assertions);
  }

  // -- Writer scenario --

  private PopulationBuilder writerPopulation(
      CacheScenarios scenarios, HttpProtocolBuilder protocol, ChainBuilder cycle) {
    ScenarioBuilder writerScenario =
        scenario("Writers (" + writeTarget + ")")
            .exec(scenarios.login())
            .exec(session -> session.set("wi", 0L))
            .during(Duration.ofSeconds(durationSec))
            .on(cycle);
    return writerScenario
        .injectClosed(constantConcurrentUsers(writers).during(Duration.ofSeconds(durationSec)))
        .protocols(protocol);
  }

  /** One writer iteration at the aggregate target rate (pace is per virtual user). */
  private ChainBuilder pacedWriterCycle() {
    long perWriterMillis = Math.max(50L, (long) (1000.0 * writers / writeRate));
    return pace(Duration.ofMillis(perWriterMillis)).exec(writerCycle());
  }

  /** UPDATE every iteration; every 10th iteration additionally CREATE + DELETE. */
  private ChainBuilder writerCycle() {
    ConcurrentLinkedQueue<String> pool = pool();
    String endpoint = writeTarget.endpoint;
    return exec(session -> {
          String uid = pool.poll();
          if (uid == null) {
            throw new IllegalStateException("Mutation pool is empty - seeding failed?");
          }
          pool.offer(uid);
          long i = session.getLong("wi") + 1;
          WRITES.incrementAndGet();
          return session.set("uid", uid).set("wi", i);
        })
        .exec(
            http("mutate/update")
                .patch(api + "/" + endpoint + "/#{uid}")
                .header("Content-Type", "application/json-patch+json")
                .body(
                    StringBody(
                        session ->
                            "[{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"perf "
                                + System.nanoTime()
                                + "\"}]"))
                // 404 tolerated: another writer may have deleted the polled object in the
                // create/delete leg between our poll/offer and the PATCH landing.
                .check(status().in(200, 404)))
        .doIf(session -> session.getLong("wi") % 10 == 0)
        .then(
            exec(http("mutate/create")
                    .post(api + "/" + endpoint)
                    .header("Content-Type", "application/json")
                    .body(StringBody(session -> createPayload()))
                    .check(status().in(200, 201))
                    .check(jsonPath("$.response.uid").optional().saveAs("newUid")))
                .exec(
                    session -> {
                      String newUid = session.getString("newUid");
                      if (newUid != null) {
                        pool.offer(newUid);
                      }
                      String victim = pool.poll();
                      return victim == null
                          ? session.markAsFailed()
                          : session.set("victimUid", victim);
                    })
                .exec(
                    http("mutate/delete")
                        .delete(api + "/" + endpoint + "/#{victimUid}")
                        .check(status().in(200, 404))));
  }

  // -- Staleness scenario --

  private PopulationBuilder stalenessPopulation(
      CacheScenarios scenarios, HttpProtocolBuilder protocol) {
    long paceMillis = Math.max(500L, (long) (1000.0 * Math.max(1, writers) / writeRate));
    String listUrl = api + "/dataElements?fields=id&pageSize=50";

    ChainBuilder probeCycle =
        pace(Duration.ofMillis(paceMillis))
            .exec(session -> session.set("wi", session.getLong("wi") + 1).remove("etag"))
            .exec(
                http("probe/baseline")
                    .get(listUrl)
                    .check(status().in(200, 304))
                    .check(header("ETag").optional().saveAs("etag")))
            .doIf(session -> session.contains("etag"))
            .then(
                exec(session -> {
                      String uid = HOT_POOL.poll();
                      if (uid == null) {
                        throw new IllegalStateException("Mutation pool is empty");
                      }
                      HOT_POOL.offer(uid);
                      WRITES.incrementAndGet();
                      return session.set("uid", uid).set("attempts", 0).set("probeStatus", 304);
                    })
                    .exec(
                        http("probe/mutate")
                            .patch(api + "/dataElements/#{uid}")
                            .header("Content-Type", "application/json-patch+json")
                            .body(
                                StringBody(
                                    session ->
                                        "[{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"staleness "
                                            + System.nanoTime()
                                            + "\"}]"))
                            .check(status().is(200)))
                    .asLongAs(
                        session ->
                            session.getInt("probeStatus") == 304 && session.getInt("attempts") < 10)
                    .on(
                        exec(session -> session.set("attempts", session.getInt("attempts") + 1))
                            .exec(
                                http("probe/conditional-get")
                                    .get(listUrl)
                                    .header("If-None-Match", "#{etag}")
                                    .check(status().in(200, 304))
                                    .check(status().saveAs("probeStatus")))
                            .pause(Duration.ofMillis(200)))
                    .exec(
                        session -> {
                          int bucket =
                              session.getInt("probeStatus") == 304
                                  ? STALENESS_EXHAUSTED_BUCKET
                                  : session.getInt("attempts");
                          STALENESS_ATTEMPTS.incrementAndGet(bucket);
                          return session;
                        }));

    ScenarioBuilder prober =
        scenario("Staleness probers")
            .exec(scenarios.login())
            .exec(session -> session.set("wi", 0L))
            .during(Duration.ofSeconds(durationSec))
            .on(probeCycle);
    return prober
        .injectClosed(
            constantConcurrentUsers(Math.max(1, writers)).during(Duration.ofSeconds(durationSec)))
        .protocols(protocol);
  }

  // -- Lifecycle: seeding and teardown --

  @Override
  public void before() {
    log.info("Seeding mutation pools (poolSize={} per type, prefix={})", poolSize, POOL_PREFIX);
    deletePerfObjects("dataElements");
    deletePerfObjects("constants");

    String categoryCombo = rest.firstUid("/api/categoryCombos?filter=name:eq:default&fields=id");
    if (categoryCombo == null) {
      throw new IllegalStateException("Could not resolve default categoryCombo UID");
    }

    StringBuilder dataElements = new StringBuilder();
    StringBuilder constants = new StringBuilder();
    for (int i = 0; i < poolSize; i++) {
      String suffix = i + "_" + Integer.toHexString(ThreadLocalRandom.current().nextInt());
      if (i > 0) {
        dataElements.append(',');
        constants.append(',');
      }
      dataElements
          .append("{\"name\":\"")
          .append(POOL_PREFIX)
          .append("de_")
          .append(suffix)
          .append("\",\"shortName\":\"")
          .append(POOL_PREFIX)
          .append("de_")
          .append(suffix)
          .append("\",\"valueType\":\"INTEGER\",\"domainType\":\"AGGREGATE\",")
          .append("\"aggregationType\":\"SUM\",\"categoryCombo\":{\"id\":\"")
          .append(categoryCombo)
          .append("\"}}");
      constants
          .append("{\"name\":\"")
          .append(POOL_PREFIX)
          .append("c_")
          .append(suffix)
          .append("\",\"shortName\":\"")
          .append(POOL_PREFIX)
          .append("c_")
          .append(suffix)
          .append("\",\"value\":1}");
    }
    rest.postExpectOk(
        "/api/metadata?async=false",
        "{\"dataElements\":[" + dataElements + "]}",
        "seed dataElements");
    rest.postExpectOk(
        "/api/metadata?async=false", "{\"constants\":[" + constants + "]}", "seed constants");

    HOT_POOL.clear();
    HOT_POOL.addAll(fetchPerfUids("dataElements"));
    CONTROL_POOL.clear();
    CONTROL_POOL.addAll(fetchPerfUids("constants"));
    log.info("Seeded pools: hot={} control={}", HOT_POOL.size(), CONTROL_POOL.size());
    if (HOT_POOL.size() < poolSize / 2 || CONTROL_POOL.size() < poolSize / 2) {
      throw new IllegalStateException(
          "Seeding failed: hot=" + HOT_POOL.size() + " control=" + CONTROL_POOL.size());
    }
  }

  @Override
  public void after() {
    log.info("Cleaning up mutation pools ({}*)", POOL_PREFIX);
    deletePerfObjects("dataElements");
    deletePerfObjects("constants");

    long total = CacheScenarios.cacheableResponses();
    long n304 = CacheScenarios.notModifiedResponses();
    double share = total == 0 ? 0.0 : (double) n304 / (double) total;
    log.info(
        "mutation-bench profile={} writeTarget={} writeRate={} writes={} cacheableResponses={} "
            + "notModified={} share={}%",
        profile,
        writeTarget,
        writeRate,
        WRITES.get(),
        total,
        n304,
        String.format("%.1f", share * 100.0));

    switch (profile) {
      case WRITELOAD -> {
        if (total < 100) {
          throw new AssertionError(
              "writeload: too few cacheable responses to judge 304 share (n=" + total + ")");
        }
        if (share < assertMin304) {
          throw new AssertionError(
              String.format(
                  "writeload(writeTarget=%s, writeRate=%s): 304 share %.1f%% (n=%d/%d) is below "
                      + "minimum %.0f%% — invalidation may be over-broad or cache broken",
                  writeTarget, writeRate, share * 100.0, n304, total, assertMin304 * 100.0));
        }
      }
      case STALENESS -> assertStaleness();
      case WRITECOST -> {
        // No in-sim assertion: the wrapper script compares an on/off pair of runs.
      }
    }
  }

  private void assertStaleness() {
    long total = 0;
    StringBuilder dist = new StringBuilder();
    for (int i = 1; i <= STALENESS_EXHAUSTED_BUCKET; i++) {
      long n = STALENESS_ATTEMPTS.get(i);
      total += n;
      if (n > 0) {
        dist.append(i == STALENESS_EXHAUSTED_BUCKET ? " >10" : " " + i).append("=").append(n);
      }
    }
    log.info("staleness attempts distribution (attempt=count):{} total={}", dist, total);
    if (total < 10) {
      throw new AssertionError("staleness: too few probes to judge (n=" + total + ")");
    }
    long p99Rank = (long) Math.ceil(total * 0.99);
    long cumulative = 0;
    int p99 = STALENESS_EXHAUSTED_BUCKET;
    for (int i = 1; i <= STALENESS_EXHAUSTED_BUCKET; i++) {
      cumulative += STALENESS_ATTEMPTS.get(i);
      if (cumulative >= p99Rank) {
        p99 = i;
        break;
      }
    }
    log.info("staleness p99 attempts={}", p99);
    if (p99 > 2) {
      throw new AssertionError(
          "staleness: p99 attempts-until-200 is "
              + p99
              + " (>2) — mutations are not invalidating ETags promptly");
    }
  }

  // -- Helpers --

  private ConcurrentLinkedQueue<String> pool() {
    return writeTarget == WriteTarget.CONTROL ? CONTROL_POOL : HOT_POOL;
  }

  private String createPayload() {
    String suffix =
        "n_"
            + Long.toHexString(System.nanoTime())
            + Integer.toHexString(ThreadLocalRandom.current().nextInt(0xFFF));
    if (writeTarget == WriteTarget.CONTROL) {
      return "{\"name\":\""
          + POOL_PREFIX
          + "c_"
          + suffix
          + "\",\"shortName\":\""
          + POOL_PREFIX
          + "c_"
          + suffix
          + "\",\"value\":1}";
    }
    return "{\"name\":\""
        + POOL_PREFIX
        + "de_"
        + suffix
        + "\",\"shortName\":\""
        + POOL_PREFIX
        + "de_"
        + suffix
        + "\",\"valueType\":\"INTEGER\",\"domainType\":\"AGGREGATE\","
        + "\"aggregationType\":\"SUM\",\"categoryCombo\":{\"id\":\""
        + seededCategoryCombo()
        + "\"}}";
  }

  private volatile String categoryComboUid;

  private String seededCategoryCombo() {
    String uid = categoryComboUid;
    if (uid == null) {
      uid = rest.firstUid("/api/categoryCombos?filter=name:eq:default&fields=id");
      categoryComboUid = uid;
    }
    return uid;
  }

  private List<String> fetchPerfUids(String endpoint) {
    return rest.uids(
        "/api/" + endpoint + "?filter=name:like:" + POOL_PREFIX + "&fields=id&paging=false");
  }

  private void deletePerfObjects(String endpoint) {
    List<String> uids = fetchPerfUids(endpoint);
    if (uids.isEmpty()) {
      return;
    }
    StringBuilder body = new StringBuilder("{\"").append(endpoint).append("\":[");
    for (int i = 0; i < uids.size(); i++) {
      if (i > 0) {
        body.append(',');
      }
      body.append("{\"id\":\"").append(uids.get(i)).append("\"}");
    }
    body.append("]}");
    rest.postExpectOk(
        "/api/metadata?importStrategy=DELETE&atomicMode=NONE&async=false",
        body.toString(),
        "delete " + uids.size() + " " + endpoint);
    log.info("Deleted {} leftover {} with prefix {}", uids.size(), endpoint, POOL_PREFIX);
  }

  private static String prop(String key, String defaultValue) {
    return System.getProperty(key, defaultValue);
  }

  private static int intProp(String key, int defaultValue) {
    return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
  }

  private static double doubleProp(String key, double defaultValue) {
    return Double.parseDouble(System.getProperty(key, String.valueOf(defaultValue)));
  }

  private enum Profile {
    WRITELOAD,
    STALENESS,
    WRITECOST;

    static Profile fromString(String s) {
      try {
        return valueOf(s.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Unknown profile: " + s + ". Valid: writeload, staleness, writecost");
      }
    }
  }

  private enum WriteTarget {
    HOT("dataElements"),
    CONTROL("constants");

    final String endpoint;

    WriteTarget(String endpoint) {
      this.endpoint = endpoint;
    }

    static WriteTarget fromString(String s) {
      return switch (s.toLowerCase()) {
        case "hot" -> HOT;
        case "control" -> CONTROL;
        default ->
            throw new IllegalArgumentException(
                "Unknown writeTarget: " + s + ". Valid: hot, control");
      };
    }
  }

  /** Minimal blocking REST helper for seeding/teardown (runs on the Gatling main thread). */
  private static final class Api {
    private final HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final String authHeader;

    Api(String baseUrl, String user, String password) {
      this.baseUrl = baseUrl;
      this.authHeader =
          "Basic "
              + Base64.getEncoder()
                  .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    String firstUid(String path) {
      List<String> uids = uids(path);
      return uids.isEmpty() ? null : uids.get(0);
    }

    List<String> uids(String path) {
      String body = send("GET", path, null);
      List<String> uids = new ArrayList<>();
      Matcher m = UID_PATTERN.matcher(body);
      while (m.find()) {
        uids.add(m.group(1));
      }
      return uids;
    }

    void postExpectOk(String path, String body, String what) {
      String response = send("POST", path, body);
      if (response.contains("\"status\":\"ERROR\"")) {
        throw new IllegalStateException(what + " failed: " + truncate(response));
      }
    }

    private String send(String method, String path, String body) {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + path))
              .timeout(Duration.ofMinutes(2))
              .header("Authorization", authHeader);
      if (body != null) {
        builder.header("Content-Type", "application/json");
        builder.method(method, HttpRequest.BodyPublishers.ofString(body));
      } else {
        builder.method(method, HttpRequest.BodyPublishers.noBody());
      }
      try {
        HttpResponse<String> response =
            client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
          throw new IllegalStateException(
              method
                  + " "
                  + path
                  + " -> HTTP "
                  + response.statusCode()
                  + ": "
                  + truncate(response.body()));
        }
        return response.body();
      } catch (IOException e) {
        throw new IllegalStateException(method + " " + path + " failed", e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(method + " " + path + " interrupted", e);
      }
    }

    private static String truncate(String s) {
      return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
  }
}
