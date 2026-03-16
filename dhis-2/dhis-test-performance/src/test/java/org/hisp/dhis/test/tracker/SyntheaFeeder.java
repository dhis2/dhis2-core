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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.RandomNumberGenerator;
import org.mitre.synthea.modules.LifecycleModule;
import org.mitre.synthea.world.agents.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bounded-queue feeder that generates person demographics from Synthea. A single producer thread
 * uses Synthea's {@link Generator#randomDemographics(RandomNumberGenerator)} for realistic
 * demographics (gender, birthdate, race) and {@link LifecycleModule#birth(Person, long)} for name
 * generation, then pushes records into a {@link LinkedBlockingQueue}. Gatling workers drain the
 * queue concurrently.
 *
 * <p>This bypasses Synthea's full life simulation ({@code updatePerson}), which steps through a
 * person's entire life in weekly increments. That loop dominates generation time (~24ms/person) and
 * is unnecessary since we only need demographics and a plausible date. By calling {@code
 * randomDemographics} + {@code LifecycleModule.birth} directly, generation drops to ~0.1ms/person.
 *
 * <p>Determinism is guaranteed: a single producer thread with a fixed seed produces the same person
 * sequence across runs.
 */
class SyntheaFeeder implements Iterator<Map<String, Object>> {

  private static final Logger logger = LoggerFactory.getLogger(SyntheaFeeder.class);
  private static final int QUEUE_DEPTH = 500;
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  private final LinkedBlockingQueue<Map<String, Object>> queue =
      new LinkedBlockingQueue<>(QUEUE_DEPTH);
  private final AtomicBoolean exhausted = new AtomicBoolean(false);
  private volatile Throwable producerError;

  SyntheaFeeder(long seed, int population) {
    logger.info(
        "Starting Synthea feeder: seed={}, population={}, queueDepth={}",
        seed,
        population,
        QUEUE_DEPTH);

    Thread producer =
        new Thread(
            () -> {
              try {
                // Disable all exporters to avoid loading FHIR/freemarker classes.
                Config.set("exporter.fhir.export", "false");
                Config.set("exporter.fhir_stu3.export", "false");
                Config.set("exporter.fhir_dstu2.export", "false");
                Config.set("exporter.ccda.export", "false");
                Config.set("exporter.csv.export", "false");
                Config.set("exporter.cpcds.export", "false");
                Config.set("exporter.cdw.export", "false");
                Config.set("exporter.text.export", "false");

                GeneratorOptions opts = new GeneratorOptions();
                opts.seed = seed;
                opts.clinicianSeed = seed;
                opts.population = population;
                opts.state = "Massachusetts";
                opts.overflow = false;
                // Match nothing so no clinical modules are loaded into the Generator.
                opts.enabledModules = List.of("__none__");

                Generator generator = new Generator(opts);
                logger.info("Synthea Generator initialized, generating {} persons", population);

                for (int i = 0; i < population; i++) {
                  long personSeed = seed + (long) i;

                  // Create person with demographics only (no life simulation).
                  // randomDemographics picks gender, birthdate, race, city from census data.
                  // LifecycleModule.birth assigns first/last name based on demographics.
                  Person person = new Person(personSeed);
                  person.populationSeed = seed;
                  Map<String, Object> demo = generator.randomDemographics(person);
                  person.attributes.putAll(demo);
                  LifecycleModule.birth(person, (long) demo.get(Person.BIRTHDATE));

                  String firstName = (String) person.attributes.get(Person.FIRST_NAME);
                  String lastName = (String) person.attributes.get(Person.LAST_NAME);
                  String gender = (String) person.attributes.get(Person.GENDER);
                  long birthdate = (long) person.attributes.get(Person.BIRTHDATE);
                  long now = generator.stop;

                  // Derive an encounter date between birthdate and now using the person's PRNG.
                  // This spreads dates across each person's lifetime rather than clustering at
                  // birth.
                  long range = now - birthdate;
                  long encounterMs = birthdate + (long) (person.rand() * range);
                  String birthdateStr = DATE_FMT.format(Instant.ofEpochMilli(birthdate));
                  String encounterDate = DATE_FMT.format(Instant.ofEpochMilli(encounterMs));

                  Map<String, Object> record =
                      Map.of(
                          "firstName", firstName,
                          "lastName", lastName,
                          "gender", "M".equals(gender) ? "Male" : "Female",
                          "birthdate", birthdateStr,
                          "date", encounterDate);

                  queue.put(record);

                  if ((i + 1) % 10_000 == 0) {
                    logger.debug("Synthea feeder produced {}/{} persons", i + 1, population);
                  }
                }

                logger.info("Synthea feeder finished producing {} persons", population);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Synthea producer interrupted");
              } catch (Exception e) {
                producerError = e;
                logger.error("Synthea producer failed", e);
              } finally {
                exhausted.set(true);
              }
            },
            "synthea-producer");
    producer.setDaemon(true);
    producer.start();
  }

  @Override
  public boolean hasNext() {
    return !exhausted.get() || !queue.isEmpty();
  }

  @Override
  public Map<String, Object> next() {
    try {
      Map<String, Object> record = queue.poll(10, TimeUnit.SECONDS);
      if (record == null) {
        if (producerError != null) {
          throw new RuntimeException("Synthea producer failed", producerError);
        }
        throw new RuntimeException("Synthea feeder timed out waiting for next record");
      }
      return record;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Feeder interrupted", e);
    }
  }

  /**
   * Dumps generated person records as CSV to stdout for debugging.
   *
   * <p>Usage: {@code mvn -f dhis-2/dhis-test-performance/pom.xml test-compile exec:java
   * -Dexec.mainClass=org.hisp.dhis.test.tracker.SyntheaFeeder -Dexec.classpathScope=test
   * -Dexec.args="12345 20"}
   */
  public static void main(String[] args) {
    long seed = args.length > 0 ? Long.parseLong(args[0]) : 12345L;
    int population = args.length > 1 ? Integer.parseInt(args[1]) : 10;

    SyntheaFeeder feeder = new SyntheaFeeder(seed, population);

    System.out.println("firstName,lastName,gender,birthdate,date");
    for (int i = 0; i < population; i++) {
      Map<String, Object> record = feeder.next();
      System.out.printf(
          "%s,%s,%s,%s,%s%n",
          record.get("firstName"),
          record.get("lastName"),
          record.get("gender"),
          record.get("birthdate"),
          record.get("date"));
    }
    System.err.printf("Generated %d records with seed=%d%n", population, seed);
  }
}
