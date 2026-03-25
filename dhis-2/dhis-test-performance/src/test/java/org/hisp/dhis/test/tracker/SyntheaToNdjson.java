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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;

/**
 * CLI tool that generates DHIS2 tracker import payloads directly from Synthea, streaming one person
 * at a time to compressed ndjson files. No intermediate CSV files are written.
 *
 * <p>All UIDs reference metadata in the Sierra Leone demo database. The output is not portable to
 * other DHIS2 instances without adjusting the constants below.
 *
 * <p>Produces three files:
 *
 * <ul>
 *   <li>{@code mnch.ndjson.gz} -- MNCH / PNC tracker TEs from pregnancy encounters
 *   <li>{@code child.ndjson.gz} -- Child Programme tracker TEs from infant well-child visits
 *   <li>{@code anc.ndjson.gz} -- Antenatal care visit events from prenatal encounters
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * mvn -f dhis-2/dhis-test-performance/pom.xml test-compile exec:java \
 *   -Dexec.mainClass=org.hisp.dhis.test.tracker.SyntheaToNdjson \
 *   -Dexec.classpathScope=test \
 *   -Dexec.args="--population 100000 --seed 12345 --output-dir src/test/resources/tracker"
 * }</pre>
 *
 * <p>Use {@code --append} to add to existing files (requires different seed to avoid duplicates).
 */
public class SyntheaToNdjson {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  // Synthea SNOMED codes (pregnancy module)
  private static final String SNOMED_PRENATAL_INITIAL = "424441002";
  private static final String SNOMED_PRENATAL_VISIT = "424619006";
  private static final String SNOMED_OBSTETRIC_ADMISSION = "183460006";
  private static final String SNOMED_POSTNATAL_VISIT = "169762003";
  private static final String SNOMED_CESAREAN = "11466000";
  private static final String SNOMED_INSTRUMENTAL_DELIVERY = "236974004";

  // Synthea LOINC codes (wellness_encounters module)
  private static final String LOINC_BODY_WEIGHT = "29463-7";
  private static final String LOINC_BODY_HEIGHT = "8302-2";
  private static final String LOINC_SYSTOLIC_BP = "8480-6";
  private static final String LOINC_DIASTOLIC_BP = "8462-4";
  private static final String LOINC_HEMOGLOBIN = "718-7";
  private static final String LOINC_SMOKING_STATUS = "72166-2";

  private static final String ENCOUNTER_WELLNESS = "wellness";

  // Sierra Leone demo DB: organisation unit and tracked entity type
  private static final String ORG_UNIT = "DiszpKrYNg8"; // Ngelehun CHC
  private static final String TE_TYPE_PERSON = "nEenWmSyUEp";

  // Sierra Leone demo DB: MNCH / PNC (Adult Woman) program
  private static final String MNCH_PROGRAM = "uy2gU8kT1jF";
  private static final String MNCH_STAGE_ANC_1ST = "eaDHS084uMp";
  private static final String MNCH_STAGE_ANC_FOLLOWUP = "grIfo3oOf4Y";
  private static final String MNCH_STAGE_DELIVERY = "Xgk8Wvl0jHr";
  private static final String MNCH_STAGE_PNC = "oRySG82BKE6";

  // Sierra Leone demo DB: Child Programme
  private static final String CHILD_PROGRAM = "IpHINAT79UW";
  private static final String CHILD_STAGE_BIRTH = "A03MvHHogjR";
  private static final String CHILD_STAGE_POSTNATAL = "ZzYYXq4fJie";

  // Sierra Leone demo DB: Antenatal care visit (event program)
  private static final String ANC_PROGRAM = "lxAQ7Zs9VYR";
  private static final String ANC_STAGE = "dBwrot7S420";

  // Sierra Leone demo DB: tracked entity attributes
  private static final String ATTR_FIRST_NAME = "w75KJ2mc4zz";
  private static final String ATTR_LAST_NAME = "zDhUuAYrxNC";
  private static final String ATTR_GENDER = "cejWyOfXge6";
  private static final String ATTR_DOB = "iESIqZ0R0R0";
  private static final String ATTR_ADDRESS = "VqEFza8wbwA";
  private static final String ATTR_WEIGHT_KG = "OvY4VVhSDeJ";
  private static final String ATTR_HEIGHT_CM = "lw1SqmMlnfh";

  // Sierra Leone demo DB: data elements on MNCH stages
  private static final String DE_MCH_WEIGHT_G = "UXz7xuGCEhU";
  private static final String DE_MCH_BLOOD_PRESSURE = "KVQpGEjHluk";
  private static final String DE_MCH_HB = "xjTklbpY6oG";
  private static final String DE_MCH_DELIVERY_MODE = "fIy3fOtkbdS";
  private static final String DE_MCH_DELIVERY_DATE = "uxRgo9bGWhX";

  // Sierra Leone demo DB: data elements on Child stages (same UID for weight on Birth and MNCH)
  private static final String DE_BIRTH_WEIGHT_G = "UXz7xuGCEhU";
  private static final String DE_APGAR_SCORE = "a3kGcGDCuk6";
  private static final String DE_OPV_DOSE = "ebaJjqltK5N";
  private static final String DE_INFANT_WEIGHT_G = "GQY2lXrypjO";
  private static final String DE_DPT_DOSE = "pOe0ogW4OWd";
  private static final String DE_MEASLES_DOSE = "FqlgKAG8HOu";

  // Sierra Leone demo DB: data elements on ANC event program
  private static final String DE_SMOKING = "sWoqcoByYmD";
  private static final String DE_SMOKING_COUNSELLING = "Ok9OQpitjQr";
  private static final String DE_HEMOGLOBIN = "vANAXwtLwcT";

  // Well-child visits in the first 24 months are used for the Child Programme.
  // We need at least 2 visits: the first provides birth weight (Birth stage),
  // the second provides postnatal weight (Baby Postnatal stage).
  private static final long INFANT_MAX_AGE_MS = 24L * 30 * 24 * 60 * 60 * 1000;

  /** Vitals snapshot from the most recent wellness encounter before a given time. */
  private record Vitals(
      Double weightKg, Double heightCm, String bloodPressure, Double hemoglobin, Boolean smoker) {

    static final Vitals EMPTY = new Vitals(null, null, null, null, null);

    String weightGrams() {
      return weightKg != null ? String.valueOf((int) Math.round(weightKg * 1000)) : null;
    }

    String hemoglobinStr() {
      return hemoglobin != null ? String.format("%.1f", hemoglobin) : null;
    }
  }

  /** Demographics extracted from a Synthea Person. */
  private record Demographics(
      String firstName, String lastName, String gender, String birthdate, String address) {}

  /** Classified encounters for a single Synthea person. */
  private record ClassifiedEncounters(
      List<Encounter> prenatalInitial,
      List<Encounter> prenatalFollowUp,
      List<Encounter> deliveries,
      List<Encounter> postnatalVisits,
      List<Encounter> infantWellChild) {}

  public static void main(String[] args) throws Exception {
    int population = 1000;
    long seed = 12345L;
    String outputDir = ".";
    boolean append = false;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--population", "-p" -> population = Integer.parseInt(args[++i]);
        case "--seed", "-s" -> seed = Long.parseLong(args[++i]);
        case "--output-dir", "-o" -> outputDir = args[++i];
        case "--append" -> append = true;
        default -> {
          System.err.println("Unknown option: " + args[i]);
          System.err.println(
              "Usage: SyntheaToNdjson [--population N] [--seed N] [--output-dir DIR] [--append]");
          System.exit(1);
        }
      }
    }

    int threads = Runtime.getRuntime().availableProcessors();
    Generator generator = createGenerator(seed, population);

    System.err.printf(
        "Generating %d patients (seed=%d, threads=%d) -> %s%n",
        population, seed, threads, outputDir);

    String mnchPath = outputDir + "/mnch.ndjson.gz";
    String childPath = outputDir + "/child.ndjson.gz";
    String ancPath = outputDir + "/anc.ndjson.gz";

    AtomicInteger mnchCount = new AtomicInteger();
    AtomicInteger childCount = new AtomicInteger();
    AtomicInteger ancCount = new AtomicInteger();
    AtomicInteger processed = new AtomicInteger();

    try (Writer mnchOut = gzipWriter(mnchPath, append);
        Writer childOut = gzipWriter(childPath, append);
        Writer ancOut = gzipWriter(ancPath, append)) {

      // Each task generates one patient (~200ms), builds JSON strings, then briefly locks
      // the output writer to append. Bounded queue with CallerRunsPolicy provides
      // backpressure: when all workers are busy and the queue is full, the main thread
      // runs the next task itself, naturally throttling submission to match processing speed.
      // This keeps memory constant regardless of population size.
      var pool =
          new ThreadPoolExecutor(
              threads,
              threads,
              0L,
              TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<>(threads * 2),
              new ThreadPoolExecutor.CallerRunsPolicy());
      final int pop = population;

      for (int i = 0; i < population; i++) {
        final int index = i;
        final long personSeed = seed + (long) i;
        pool.submit(
            () -> {
              try {
                Person person = generator.generatePerson(index, personSeed);
                Demographics demo = extractDemographics(person);
                ClassifiedEncounters enc = classifyEncounters(person);

                if (!enc.prenatalInitial().isEmpty()) {
                  String json = buildMnchJson(person, demo, enc);
                  writeLine(mnchOut, json);
                  mnchCount.incrementAndGet();
                }

                // Child Programme has two stages (Birth + Baby Postnatal), need at least 2 visits
                if (enc.infantWellChild().size() >= 2) {
                  String json = buildChildJson(demo, enc.infantWellChild());
                  writeLine(childOut, json);
                  childCount.incrementAndGet();
                }

                for (Encounter visit : enc.prenatalInitial()) {
                  writeLine(ancOut, buildAncEventJson(person, visit));
                  ancCount.incrementAndGet();
                }
                for (Encounter visit : enc.prenatalFollowUp()) {
                  writeLine(ancOut, buildAncEventJson(person, visit));
                  ancCount.incrementAndGet();
                }

                int done = processed.incrementAndGet();
                if (done % 10000 == 0 || done == pop) {
                  System.err.printf(
                      "%d/%d patients: MNCH=%d, Child=%d, ANC=%d%n",
                      done, pop, mnchCount.get(), childCount.get(), ancCount.get());
                }
              } catch (Exception e) {
                System.err.printf("Error generating patient %d: %s%n", index, e.getMessage());
              }
            });
      }

      pool.shutdown();
      pool.awaitTermination(24, TimeUnit.HOURS);
    }

    System.err.printf("%nDone:%n");
    System.err.printf("  MNCH:  %s (%d TEs)%n", mnchPath, mnchCount.get());
    System.err.printf("  Child: %s (%d TEs)%n", childPath, childCount.get());
    System.err.printf("  ANC:   %s (%d events)%n", ancPath, ancCount.get());
  }

  private static void writeLine(Writer out, String json) throws IOException {
    synchronized (out) {
      out.write(json);
      out.write('\n');
    }
  }

  private static Generator createGenerator(long seed, int population) {
    Config.set("exporter.fhir.export", "false");
    Config.set("exporter.fhir_stu3.export", "false");
    Config.set("exporter.fhir_dstu2.export", "false");
    Config.set("exporter.ccda.export", "false");
    Config.set("exporter.csv.export", "false");
    Config.set("exporter.cpcds.export", "false");
    Config.set("exporter.cdw.export", "false");
    Config.set("exporter.text.export", "false");
    // Keep full encounter history so all patients have childhood well-child visits.
    // Without this, generatePerson() destructively filters the Person's health record
    // to only the last 10 years (the default), discarding infant data for adults.
    Config.set("exporter.years_of_history", "0");

    // Fixed reference time (2026-01-01) so encounter dates are reproducible regardless of
    // when the tool runs. Without this, Synthea uses System.currentTimeMillis() and the same
    // seed produces different dates on different days.
    long refTime =
        java.time.LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

    GeneratorOptions opts = new GeneratorOptions();
    opts.seed = seed;
    opts.clinicianSeed = seed;
    opts.referenceTime = refTime;
    opts.endTime = refTime;
    opts.population = population;
    opts.state = "Massachusetts";
    opts.overflow = false;

    // Use census-based age distribution (default). This produces the most Child TEs because
    // every patient regardless of age has childhood well-child visits in their history.
    // With ageSpecified=true and maxAge=55, patients aged 0-1 at reference time have too few
    // well-child visits to qualify for the Child Programme (needs at least 2).
    opts.enabledModules =
        List.of(
            "pregnancy",
            "female_reproduction",
            "sexual_activity",
            "contraceptives",
            "contraceptive_maintenance",
            "wellness_encounters",
            "injuries",
            "appendicitis",
            "gallstones",
            "sepsis",
            "stroke",
            "myocardial_infarction",
            "congestive_heart_failure");
    return new Generator(opts);
  }

  private static Demographics extractDemographics(Person person) {
    String firstName = (String) person.attributes.get(Person.FIRST_NAME);
    String lastName = (String) person.attributes.get(Person.LAST_NAME);
    String gender = "M".equals(person.attributes.get(Person.GENDER)) ? "Male" : "Female";
    long birthdate = (long) person.attributes.get(Person.BIRTHDATE);
    String address = (String) person.attributes.get(Person.ADDRESS);
    return new Demographics(firstName, lastName, gender, dateStr(birthdate), address);
  }

  private static ClassifiedEncounters classifyEncounters(Person person) {
    long birthdate = (long) person.attributes.get(Person.BIRTHDATE);
    List<Encounter> prenatalInitial = new ArrayList<>();
    List<Encounter> prenatalFollowUp = new ArrayList<>();
    List<Encounter> deliveries = new ArrayList<>();
    List<Encounter> postnatalVisits = new ArrayList<>();
    List<Encounter> infantWellChild = new ArrayList<>();

    for (Encounter enc : person.record.encounters) {
      String code = primaryCode(enc);
      if (SNOMED_PRENATAL_INITIAL.equals(code)) {
        prenatalInitial.add(enc);
      } else if (SNOMED_PRENATAL_VISIT.equals(code)) {
        prenatalFollowUp.add(enc);
      } else if (SNOMED_OBSTETRIC_ADMISSION.equals(code)) {
        deliveries.add(enc);
      } else if (SNOMED_POSTNATAL_VISIT.equals(code)) {
        postnatalVisits.add(enc);
      }
      if (isWellness(enc) && (enc.start - birthdate) < INFANT_MAX_AGE_MS) {
        infantWellChild.add(enc);
      }
    }
    return new ClassifiedEncounters(
        prenatalInitial, prenatalFollowUp, deliveries, postnatalVisits, infantWellChild);
  }

  private static String buildMnchJson(Person person, Demographics demo, ClassifiedEncounters enc) {
    Vitals teVitals = vitalsAt(person, enc.prenatalInitial().get(0).start);

    StringJoiner attrs = new StringJoiner(",");
    attrs.add(jsonAttr(ATTR_FIRST_NAME, demo.firstName()));
    attrs.add(jsonAttr(ATTR_LAST_NAME, demo.lastName()));
    attrs.add(jsonAttr(ATTR_GENDER, demo.gender()));
    attrs.add(jsonAttr(ATTR_DOB, demo.birthdate()));
    if (demo.address() != null) attrs.add(jsonAttr(ATTR_ADDRESS, demo.address()));
    if (teVitals.weightKg() != null)
      attrs.add(jsonAttr(ATTR_WEIGHT_KG, String.format("%.1f", teVitals.weightKg())));
    if (teVitals.heightCm() != null)
      attrs.add(jsonAttr(ATTR_HEIGHT_CM, String.format("%.1f", teVitals.heightCm())));

    StringJoiner enrollments = new StringJoiner(",");
    for (int p = 0; p < enc.prenatalInitial().size(); p++) {
      Encounter initial = enc.prenatalInitial().get(p);
      long nextStart =
          (p + 1 < enc.prenatalInitial().size())
              ? enc.prenatalInitial().get(p + 1).start
              : Long.MAX_VALUE;

      boolean isLast = (p == enc.prenatalInitial().size() - 1);
      boolean hasDelivery =
          enc.deliveries().stream().anyMatch(d -> d.start >= initial.start && d.start < nextStart);
      String status = (isLast && !hasDelivery) ? "ACTIVE" : "COMPLETED";

      StringJoiner events = new StringJoiner(",");

      // ANC 1st visit
      events.add(buildAncStageEvent(person, MNCH_STAGE_ANC_1ST, initial));

      // ANC follow-up (first in this pregnancy only, stage is non-repeatable)
      for (Encounter fu : enc.prenatalFollowUp()) {
        if (fu.start >= initial.start && fu.start < nextStart) {
          events.add(buildAncStageEvent(person, MNCH_STAGE_ANC_FOLLOWUP, fu));
          break;
        }
      }

      // Delivery (first in this pregnancy only)
      for (Encounter del : enc.deliveries()) {
        if (del.start >= initial.start && del.start < nextStart) {
          events.add(buildDeliveryEvent(del));
          break;
        }
      }

      // PNC visit (first in this pregnancy only)
      for (Encounter pnc : enc.postnatalVisits()) {
        if (pnc.start >= initial.start && pnc.start < nextStart) {
          events.add(buildPncEvent(person, pnc));
          break;
        }
      }

      String enrolledAt = dateStr(initial.start);
      enrollments.add(
          """
          {"program":"%s","orgUnit":"%s","enrolledAt":"%s","occurredAt":"%s",\
          "status":"%s","events":[%s]}\
          """
              .formatted(MNCH_PROGRAM, ORG_UNIT, enrolledAt, enrolledAt, status, events));
    }

    return """
        {"trackedEntityType":"%s","orgUnit":"%s","attributes":[%s],"enrollments":[%s]}\
        """
        .formatted(TE_TYPE_PERSON, ORG_UNIT, attrs, enrollments);
  }

  private static String buildAncStageEvent(Person person, String stageUid, Encounter enc) {
    Vitals v = vitalsAt(person, enc.start);
    StringJoiner dvs = new StringJoiner(",");
    if (v.weightGrams() != null) dvs.add(jsonDv(DE_MCH_WEIGHT_G, v.weightGrams()));
    if (v.bloodPressure() != null) dvs.add(jsonDv(DE_MCH_BLOOD_PRESSURE, v.bloodPressure()));
    if (v.hemoglobinStr() != null) dvs.add(jsonDv(DE_MCH_HB, v.hemoglobinStr()));
    return """
        {"programStage":"%s","orgUnit":"%s","occurredAt":"%s","status":"ACTIVE","dataValues":[%s]}\
        """
        .formatted(stageUid, ORG_UNIT, dateStr(enc.start), dvs);
  }

  private static String buildDeliveryEvent(Encounter enc) {
    String mode = deliveryMode(enc);
    String date = dateStr(enc.start);
    StringJoiner dvs = new StringJoiner(",");
    dvs.add(jsonDv(DE_MCH_DELIVERY_MODE, mode));
    dvs.add(jsonDv(DE_MCH_DELIVERY_DATE, date));
    return """
        {"programStage":"%s","orgUnit":"%s","occurredAt":"%s","status":"ACTIVE","dataValues":[%s]}\
        """
        .formatted(MNCH_STAGE_DELIVERY, ORG_UNIT, date, dvs);
  }

  private static String buildPncEvent(Person person, Encounter enc) {
    Vitals v = vitalsAt(person, enc.start);
    StringJoiner dvs = new StringJoiner(",");
    if (v.weightGrams() != null) dvs.add(jsonDv(DE_MCH_WEIGHT_G, v.weightGrams()));
    return """
        {"programStage":"%s","orgUnit":"%s","occurredAt":"%s","status":"ACTIVE","dataValues":[%s]}\
        """
        .formatted(MNCH_STAGE_PNC, ORG_UNIT, dateStr(enc.start), dvs);
  }

  private static String buildChildJson(Demographics demo, List<Encounter> wellChildVisits) {
    Encounter birthVisit = wellChildVisits.get(0);
    Encounter postnatalVisit = wellChildVisits.get(1);

    Double bw = findNumericObs(birthVisit, LOINC_BODY_WEIGHT);
    Double pw = findNumericObs(postnatalVisit, LOINC_BODY_WEIGHT);
    int birthWeightG = bw != null ? (int) Math.round(bw * 1000) : 3500;
    int postnatalWeightG = pw != null ? (int) Math.round(pw * 1000) : 4500;
    int apgar = 8 + (birthWeightG % 3);

    StringJoiner attrs = new StringJoiner(",");
    attrs.add(jsonAttr(ATTR_FIRST_NAME, demo.firstName()));
    attrs.add(jsonAttr(ATTR_LAST_NAME, demo.lastName()));
    attrs.add(jsonAttr(ATTR_GENDER, demo.gender()));

    StringJoiner birthDvs = new StringJoiner(",");
    birthDvs.add(jsonDv(DE_BIRTH_WEIGHT_G, String.valueOf(birthWeightG)));
    birthDvs.add(jsonDv(DE_APGAR_SCORE, String.valueOf(apgar)));
    birthDvs.add(jsonDv(DE_OPV_DOSE, "0"));

    StringJoiner pnDvs = new StringJoiner(",");
    pnDvs.add(jsonDv(DE_INFANT_WEIGHT_G, String.valueOf(postnatalWeightG)));
    pnDvs.add(jsonDv(DE_DPT_DOSE, "1"));
    pnDvs.add(jsonDv(DE_MEASLES_DOSE, "false"));

    return """
        {"trackedEntityType":"%s","orgUnit":"%s","attributes":[%s],\
        "enrollments":[{"program":"%s","orgUnit":"%s",\
        "enrolledAt":"%s","occurredAt":"%s","status":"ACTIVE",\
        "events":[\
        {"programStage":"%s","orgUnit":"%s","occurredAt":"%s","status":"ACTIVE","dataValues":[%s]},\
        {"programStage":"%s","orgUnit":"%s","occurredAt":"%s","status":"ACTIVE","dataValues":[%s]}\
        ]}]}\
        """
        .formatted(
            TE_TYPE_PERSON,
            ORG_UNIT,
            attrs,
            CHILD_PROGRAM,
            ORG_UNIT,
            demo.birthdate(),
            demo.birthdate(),
            CHILD_STAGE_BIRTH,
            ORG_UNIT,
            dateStr(birthVisit.start),
            birthDvs,
            CHILD_STAGE_POSTNATAL,
            ORG_UNIT,
            dateStr(postnatalVisit.start),
            pnDvs);
  }

  private static String buildAncEventJson(Person person, Encounter enc) {
    Vitals v = vitalsAt(person, enc.start);
    String hb = v.hemoglobinStr() != null ? v.hemoglobinStr() : "14.0";
    String smoking = v.smoker() != null && v.smoker() ? "true" : "false";

    StringJoiner dvs = new StringJoiner(",");
    dvs.add(jsonDv(DE_SMOKING, smoking));
    dvs.add(jsonDv(DE_SMOKING_COUNSELLING, smoking));
    dvs.add(jsonDv(DE_HEMOGLOBIN, hb));

    return """
        {"program":"%s","programStage":"%s","orgUnit":"%s",\
        "occurredAt":"%s","status":"COMPLETED","dataValues":[%s]}\
        """
        .formatted(ANC_PROGRAM, ANC_STAGE, ORG_UNIT, dateStr(enc.start), dvs);
  }

  /** Resolve vitals from the most recent wellness encounter at or before the given time. */
  private static Vitals vitalsAt(Person person, long beforeTime) {
    Double weight = null;
    Double height = null;
    String bp = null;
    Double hb = null;
    Boolean smoker = null;

    for (Encounter enc : person.record.encounters) {
      if (enc.start > beforeTime) break;
      if (!isWellness(enc)) continue;

      Double wt = findNumericObs(enc, LOINC_BODY_WEIGHT);
      if (wt != null) weight = wt;
      Double ht = findNumericObs(enc, LOINC_BODY_HEIGHT);
      if (ht != null) height = ht;
      Double sys = findNumericObs(enc, LOINC_SYSTOLIC_BP);
      Double dia = findNumericObs(enc, LOINC_DIASTOLIC_BP);
      if (sys != null && dia != null) {
        bp = "%d/%d".formatted(sys.intValue(), dia.intValue());
      }
      Double h = findNumericObs(enc, LOINC_HEMOGLOBIN);
      if (h != null) hb = h;
      String smokingValue = findStringObs(enc, LOINC_SMOKING_STATUS);
      if (smokingValue != null) {
        smoker = smokingValue.toLowerCase().contains("smokes");
      }
    }
    return new Vitals(weight, height, bp, hb, smoker);
  }

  private static String deliveryMode(Encounter delivery) {
    if (delivery.procedures != null) {
      for (var proc : delivery.procedures) {
        if (proc.codes != null) {
          for (Code code : proc.codes) {
            if (SNOMED_CESAREAN.equals(code.code)) return "Caesarean";
            if (SNOMED_INSTRUMENTAL_DELIVERY.equals(code.code)) return "Vacuum/foreceps";
          }
        }
      }
    }
    return "Normal (SVD)";
  }

  private static String primaryCode(Encounter enc) {
    return (enc.codes != null && !enc.codes.isEmpty()) ? enc.codes.get(0).code : "";
  }

  private static boolean isWellness(Encounter enc) {
    return ENCOUNTER_WELLNESS.equalsIgnoreCase(enc.type);
  }

  private static Double findNumericObs(Encounter enc, String loincCode) {
    if (enc.observations == null) return null;
    for (Observation obs : enc.observations) {
      if (obs.codes != null
          && obs.codes.stream().anyMatch(c -> loincCode.equals(c.code))
          && obs.value instanceof Number n) {
        return n.doubleValue();
      }
    }
    return null;
  }

  private static String findStringObs(Encounter enc, String loincCode) {
    if (enc.observations == null) return null;
    for (Observation obs : enc.observations) {
      if (obs.codes != null
          && obs.codes.stream().anyMatch(c -> loincCode.equals(c.code))
          && obs.value != null) {
        return obs.value.toString();
      }
    }
    return null;
  }

  private static String dateStr(long epochMillis) {
    return DATE_FMT.format(Instant.ofEpochMilli(epochMillis));
  }

  private static String jsonAttr(String uid, String value) {
    return "{\"attribute\":\"%s\",\"value\":\"%s\"}".formatted(uid, escapeJson(value));
  }

  private static String jsonDv(String uid, String value) {
    return "{\"dataElement\":\"%s\",\"value\":\"%s\"}".formatted(uid, escapeJson(value));
  }

  private static String escapeJson(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static Writer gzipWriter(String path, boolean append) throws IOException {
    FileOutputStream fos = new FileOutputStream(path, append);
    return new BufferedWriter(
        new OutputStreamWriter(new GZIPOutputStream(fos), StandardCharsets.UTF_8));
  }
}
