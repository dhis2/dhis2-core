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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a value of a unique tracked entity attribute to every imported tracked entity, to benchmark
 * the import uniqueness check against values populated by {@code
 * scripts/populate-unique-attribute.sh}.
 *
 * <p>Values are sequential numbers ({@code "1"}, {@code "2"}, ...), matching the populated ones.
 * The next value is kept in the dataStore key {@value #DATASTORE_PATH}, so consecutive runs against
 * the same database (e.g. warmup and measured runs) never send a value twice. Sending a value
 * already imported into the same org unit would fail the import with E1064.
 */
final class UniqueAttributeValues {
  private static final Logger logger = LoggerFactory.getLogger(UniqueAttributeValues.class);

  private static final String DATASTORE_PATH = "/api/dataStore/perf-tracker/uniqueAttributeValues";

  private static final String ATTRIBUTES = "\"attributes\":[";

  private final String attribute;
  private final String instance;
  private final String authorization;
  private final HttpClient client = HttpClient.newHttpClient();
  private final long first;
  private final long populatedValues;
  private final AtomicLong next;

  private UniqueAttributeValues(
      String attribute, String instance, String authorization, long first, long populatedValues) {
    this.attribute = attribute;
    this.instance = instance;
    this.authorization = authorization;
    this.first = first;
    this.populatedValues = populatedValues;
    this.next = new AtomicLong(first);
  }

  /**
   * Reads where the previous run stopped from the dataStore key written by {@code
   * scripts/populate-unique-attribute.sh}, failing if the database was not populated.
   */
  static UniqueAttributeValues load(
      String attribute, String instance, String username, String password)
      throws IOException, InterruptedException {
    String authorization =
        "Basic "
            + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(instance + DATASTORE_PATH))
                    .header("Authorization", authorization)
                    .header("Accept", "application/json")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 404) {
      // without it the attribute usually does not exist either, failing every import with E1006
      throw new IllegalStateException(
          "-DuniqueAttribute="
              + attribute
              + " is set but the database was not populated ("
              + DATASTORE_PATH
              + " not found). Run scripts/populate-unique-attribute.sh first, e.g. by setting"
              + " POPULATE_SCRIPT=scripts/populate-unique-attribute.sh for run-simulation.sh.");
    }
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "Failed to read " + DATASTORE_PATH + ": HTTP " + response.statusCode());
    }
    UniqueAttributeValues values =
        new UniqueAttributeValues(
            attribute,
            instance,
            authorization,
            number(response.body(), "next"),
            number(response.body(), "populatedValues"));

    logger.info(
        "Sending values of unique attribute {} starting at {} ({} populated values)",
        attribute,
        values.first,
        values.populatedValues);
    return values;
  }

  /** Returns the tracked entity JSON with a new value of the attribute added to its attributes. */
  String addTo(String trackedEntity) {
    String value =
        "{\"attribute\":\"%s\",\"value\":\"%d\"}".formatted(attribute, next.getAndIncrement());
    int start = trackedEntity.indexOf(ATTRIBUTES);
    if (start < 0) {
      int end = trackedEntity.lastIndexOf('}');
      return trackedEntity.substring(0, end) + ",\"attributes\":[" + value + "]}";
    }
    int insertAt = start + ATTRIBUTES.length();
    String separator = trackedEntity.charAt(insertAt) == ']' ? "" : ",";
    return trackedEntity.substring(0, insertAt)
        + value
        + separator
        + trackedEntity.substring(insertAt);
  }

  /** Stores where this run stopped, so the next run continues from there. */
  void save() throws IOException, InterruptedException {
    long nextValue = next.get();
    String body = "{\"next\": %d, \"populatedValues\": %d}".formatted(nextValue, populatedValues);
    HttpResponse<String> response =
        client.send(
            HttpRequest.newBuilder(URI.create(instance + DATASTORE_PATH))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new IllegalStateException(
          "Failed to write "
              + DATASTORE_PATH
              + ": HTTP "
              + response.statusCode()
              + " "
              + response.body());
    }

    logger.info("Sent values {} to {} of unique attribute {}", first, nextValue - 1, attribute);
    if (nextValue - 1 > populatedValues) {
      logger.warn(
          "Sent values up to {} but only {} values are populated: values above it collide with no"
              + " stored value. Populate more values per org unit to keep the benchmark comparable.",
          nextValue - 1,
          populatedValues);
    }
  }

  private static long number(String json, String name) {
    Matcher matcher = Pattern.compile("\"" + name + "\"\\s*:\\s*(\\d+)").matcher(json);
    if (!matcher.find()) {
      throw new IllegalStateException("Missing " + name + " in " + DATASTORE_PATH + ": " + json);
    }
    return Long.parseLong(matcher.group(1));
  }
}
