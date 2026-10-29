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
package org.hisp.dhis.analytics;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.aggregate.DataValueSetActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.actions.metadata.ProgramActions;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests predictors that get their input data from analytics.
 *
 * <p>Each test makes a predictor that predicts March and April 2022 from the two preceding months,
 * runs it, and checks the values predicted at Ngelehun CHC. So the March prediction sums January
 * and February, and the April prediction sums February and March.
 *
 * <p>The predictor inputs use this existing SL Demo data at Ngelehun CHC:
 *
 * <ul>
 *   <li>Malaria focus investigation events with the attribute Locality (count): January 2, March 2
 *   <li>Malaria case diagnosis data element Weight (sum): January 0, February 122, March 623
 *   <li>Child Programme enrollments: February 3, March 2
 *   <li>Program indicator Inpatient cases (event count): January 5, February 3, March 6
 *   <li>Aggregate data element Stock PHU dispensed BCG: January 21, February 25, March 17, and also
 *       March 23 with the attribute option combo "Improve access to medicines" (Medicines)
 * </ul>
 *
 * <p>The event data has the default attribute option combo. The enrollments have no attribute
 * option combo, so they are used in the predictions for every attribute option combo.
 *
 * <p>The Inpatient morbidity and mortality events are, by Gender, Age and Mode of Discharge:
 *
 * <ul>
 *   <li>January: Male 12 Transferred, Male 12 Transferred, Female 88 Died, Female 75 Discharged,
 *       Female 46 Discharged
 *   <li>February: Female 41 Transferred, Male 55 Transferred, Female 20 Died
 *   <li>March: Male 12 Discharged, Male 12 Died, Male 12 Discharged, Female 51 Discharged, Female 8
 *       Absconded, Female 79 Absconded
 * </ul>
 *
 * <p>The Inpatient program gets category mappings for the existing category Gender, and for the
 * test categories Age (Child under 15, Adult 15 and over) and Outcome (an attribute category: Died,
 * or else Survived). The test program indicators count Inpatient cases disaggregated by these
 * categories (see analytics-prediction-metadata.json). So the cases are:
 *
 * <ul>
 *   <li>By Gender: January Female 3, Male 2; February Female 2, Male 1; March Female 3, Male 3
 *   <li>Adult cases by Gender: January Female 3; February Female 2, Male 1; March Female 2
 *   <li>By Gender and Age: January Female Adult 3, Male Child 2; February Female Adult 2, Male
 *       Adult 1; March Female Child 1, Female Adult 2, Male Child 3
 *   <li>By Gender with Outcome: January Female Died 1, Female Survived 2, Male Survived 2; February
 *       Female Died 1, Female Survived 1, Male Survived 1; March Female Survived 3, Male Died 1,
 *       Male Survived 2
 * </ul>
 *
 * <p>This test also adds data for two test aggregate data elements:
 *
 * <ul>
 *   <li>R by Gender: January Female 1, Male 3; February Female 2, Male 4
 *   <li>T by Gender with attribute Outcome: February Female Died 200, Female Survived 100, Male
 *       Survived 300
 * </ul>
 *
 * @author Jim Grace
 */
class AnalyticsPredictionTest extends AnalyticsApiTest {
  private static final String NGELEHUN_CHC = "DiszpKrYNg8"; // From SL Demo

  private static final String FACILITY_LEVEL = "m9lBJogzE95"; // From SL Demo

  private static final String INPATIENT_PROGRAM = "eBAyeGv0exc"; // From SL Demo

  private static final String INPATIENT_STAGE = "Zj7UnCAulEk"; // From SL Demo

  private static final String GENDER_DE = "oZg33kd9taw"; // From SL Demo

  private static final String AGE_DE = "qrur9Dvnyt5"; // From SL Demo

  private static final String MODE_OF_DISCHARGE_DE = "fWIAEtYVEGk"; // From SL Demo

  // Category combos

  private static final String DEFAULT = "bjDvmb4bfuf"; // From SL Demo

  private static final String GENDER = "dPmavA0qloX"; // From SL Demo

  private static final String GENDER_AND_AGE = "PrdCcGendAg";

  // Categories and category options

  private static final String GENDER_CATEGORY = "cX5k9anHEHd"; // From SL Demo

  private static final String FEMALE_OPTION = "apsOixVZlf1"; // From SL Demo

  private static final String MALE_OPTION = "jRbMi0aBjYn"; // From SL Demo

  private static final String AGE_CATEGORY = "PrdCatAge01";

  private static final String CHILD_OPTION = "PrdCoChild1";

  private static final String ADULT_OPTION = "PrdCoAdult1";

  private static final String OUTCOME_CATEGORY = "PrdCatOutcm";

  private static final String DIED_OPTION = "PrdCoDied01";

  private static final String SURVIVED_OPTION = "PrdCoSurvd1";

  // Category option combos

  private static final String DEFAULT_COC = "HllvX50cXC0"; // From SL Demo

  private static final String FEMALE = "qk6n4eMAdtK"; // From SL Demo

  private static final String MALE = "KQ50BVoUrd6"; // From SL Demo

  private static final String DIED = "PrdAocDied1";

  private static final String SURVIVED = "PrdAocSurvd";

  private static final String MEDICINES = "sSeEjeQ0Rgt"; // From SL Demo

  /** Labels for the predicted periods and option combos. */
  private static final Map<String, String> LABELS =
      Map.ofEntries(
          Map.entry("202203", "Mar"),
          Map.entry("202204", "Apr"),
          Map.entry(FEMALE, "Female"),
          Map.entry(MALE, "Male"),
          Map.entry("PrdCocFemCh", "Female Child"),
          Map.entry("PrdCocFemAd", "Female Adult"),
          Map.entry("PrdCocMalCh", "Male Child"),
          Map.entry("PrdCocMalAd", "Male Adult"),
          Map.entry(DIED, "Died"),
          Map.entry(SURVIVED, "Survived"),
          Map.entry(MEDICINES, "Medicines"));

  // Category mappings in the Inpatient program

  /** The same mapping as in AnalyticsPiDisaggregationTest, which also maps Gender. */
  private static final String GENDER_MAPPING = "gIgiL8ow0bi";

  private static final String AGE_MAPPING = "PrdMapAge01";

  private static final String OUTCOME_MAPPING = "PrdMapOutcm";

  // Predictor input items

  private static final String LOCALITY = "A{M3xtLkYBlKI.KrCahWFMYYz}"; // From SL Demo

  private static final String MALARIA_WEIGHT = "D{qDkgAbB5Jlk.JINgGHgqzSN}"; // From SL Demo

  private static final String INPATIENT_CASES = "I{luLGbE2WKGP}"; // From SL Demo

  private static final String CHILD_ENROLLMENTS = "I{PrdPiEnroll}";

  private static final String CASES_BY_GENDER = "I{PrdPiCasSex}";

  private static final String ADULT_CASES_BY_GENDER = "I{PrdPiAdltSx}";

  private static final String CASES_BY_GENDER_AND_AGE = "I{PrdPiSexAge}";

  private static final String CASES_BY_GENDER_WITH_OUTCOME = "I{PrdPiSexOut}";

  private static final String CASES_WITH_OUTCOME = "I{PrdPiOutcom}";

  private static final String BCG_DISPENSED = "#{p1MDHOT6ENy}"; // From SL Demo

  private static final String R_BY_GENDER = "#{PrdDeRbyGen}";

  private static final String T_BY_GENDER_WITH_OUTCOME = "#{PrdDeTbyGen}";

  private RestApiActions predictorActions;

  private DataValueSetActions dataValueSetActions;

  @BeforeAll
  void setup() {
    predictorActions = new RestApiActions("predictors");
    dataValueSetActions = new DataValueSetActions();

    new MetadataActions()
        .importMetadata(new File("src/test/resources/metadata/analytics-prediction-metadata.json"))
        .validate()
        .body("status", equalTo("OK"));

    new ProgramActions()
        .addOrReplaceCategoryMappings(
            INPATIENT_PROGRAM,
            List.of(
                categoryMapping(
                    GENDER_MAPPING,
                    GENDER_CATEGORY,
                    "Gender mapping",
                    Map.of(
                        FEMALE_OPTION,
                        "#{%s.%s} == 'Female'".formatted(INPATIENT_STAGE, GENDER_DE),
                        MALE_OPTION,
                        "#{%s.%s} == 'Male'".formatted(INPATIENT_STAGE, GENDER_DE))),
                categoryMapping(
                    AGE_MAPPING,
                    AGE_CATEGORY,
                    "Prediction test Age mapping",
                    Map.of(
                        CHILD_OPTION,
                        "#{%s.%s} < 15".formatted(INPATIENT_STAGE, AGE_DE),
                        ADULT_OPTION,
                        "#{%s.%s} >= 15".formatted(INPATIENT_STAGE, AGE_DE))),
                categoryMapping(
                    OUTCOME_MAPPING,
                    OUTCOME_CATEGORY,
                    "Prediction test Outcome mapping",
                    Map.of(
                        DIED_OPTION,
                        "#{%s.%s} == 'MODDIED'".formatted(INPATIENT_STAGE, MODE_OF_DISCHARGE_DE),
                        SURVIVED_OPTION,
                        "#{%s.%s} != 'MODDIED'"
                            .formatted(INPATIENT_STAGE, MODE_OF_DISCHARGE_DE)))));

    postDataValues(
        "PrdDsDeflt1",
        dataValue("PrdDeRbyGen", "202201", FEMALE, DEFAULT_COC, 1),
        dataValue("PrdDeRbyGen", "202201", MALE, DEFAULT_COC, 3),
        dataValue("PrdDeRbyGen", "202202", FEMALE, DEFAULT_COC, 2),
        dataValue("PrdDeRbyGen", "202202", MALE, DEFAULT_COC, 4));

    postDataValues(
        "PrdDsOutcom",
        dataValue("PrdDeTbyGen", "202202", FEMALE, DIED, 200),
        dataValue("PrdDeTbyGen", "202202", FEMALE, SURVIVED, 100),
        dataValue("PrdDeTbyGen", "202202", MALE, SURVIVED, 300));
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  /** Tests predicting from a program tracked entity attribute (event count). */
  @Test
  void testPredictProgramTrackedEntityAttribute() {
    assertPredictions("A", DEFAULT, "sum( " + LOCALITY + " )", "Mar 2", "Apr 2");
  }

  /** Tests predicting from a program data element. */
  @Test
  void testPredictProgramDataElement() {
    assertPredictions("D", DEFAULT, "sum( " + MALARIA_WEIGHT + " )", "Mar 122", "Apr 745");
  }

  /** Tests predicting from an event program indicator and an enrollment program indicator. */
  @Test
  void testPredictProgramIndicators() {
    assertPredictions(
        "I",
        DEFAULT,
        "sum( " + INPATIENT_CASES + " + " + CHILD_ENROLLMENTS + " )",
        "Mar 11",
        "Apr 14");
  }

  /**
   * Tests predicting from both a program tracked entity attribute and an aggregate data element. In
   * April there is also a prediction for Medicines, from the aggregate data element alone.
   */
  @Test
  void testPredictEventAndDataElement() {
    assertPredictions(
        "AE",
        DEFAULT,
        "sum( " + LOCALITY + " ) + sum( " + BCG_DISPENSED + " )",
        "Mar 48",
        "Apr 44",
        "Apr Medicines 23");
  }

  /**
   * Tests predicting from both a program data element and an aggregate data element. In April there
   * is also a prediction for Medicines, from the aggregate data element alone.
   */
  @Test
  void testPredictProgramDataElementAndDataElement() {
    assertPredictions(
        "DE",
        DEFAULT,
        "sum( " + MALARIA_WEIGHT + " ) + sum( " + BCG_DISPENSED + " )",
        "Mar 168",
        "Apr 787",
        "Apr Medicines 23");
  }

  /**
   * Tests predicting from both program indicators and an aggregate data element. The enrollments
   * are also used in the predictions for Medicines: March 3 (enrollments), April 5 + 23 = 28.
   */
  @Test
  void testPredictProgramIndicatorsAndDataElement() {
    assertPredictions(
        "IE",
        DEFAULT,
        "sum( " + INPATIENT_CASES + " + " + CHILD_ENROLLMENTS + " ) + sum( " + BCG_DISPENSED + " )",
        "Mar 57",
        "Apr 56",
        "Mar Medicines 3",
        "Apr Medicines 28");
  }

  /**
   * Tests predicting from a program tracked entity attribute, a program data element, program
   * indicators and an aggregate data element. The enrollments are also used in the predictions for
   * Medicines: March 3 (enrollments), April 5 + 23 = 28.
   */
  @Test
  void testPredictAllEventItemsAndDataElement() {
    assertPredictions(
        "ADIE",
        DEFAULT,
        "sum( "
            + LOCALITY
            + " ) + sum( "
            + MALARIA_WEIGHT
            + " ) + sum( "
            + INPATIENT_CASES
            + " + "
            + CHILD_ENROLLMENTS
            + " ) + sum( "
            + BCG_DISPENSED
            + " )",
        "Mar 181",
        "Apr 803",
        "Mar Medicines 3",
        "Apr Medicines 28");
  }

  /** Tests predicting by Gender from a program indicator disaggregated by Gender. */
  @Test
  void testPredictDisaggregatedProgramIndicator() {
    assertPredictions(
        "Cases",
        GENDER,
        "sum( " + CASES_BY_GENDER + " )",
        "Mar Female 5",
        "Mar Male 3",
        "Apr Female 5",
        "Apr Male 4");
  }

  /**
   * Tests predicting by Gender from two program indicators disaggregated by Gender, an aggregate
   * data element disaggregated by Gender, and an aggregate data element that is not disaggregated.
   *
   * <p>( cases + R ) * adult cases + BCG dispensed:
   *
   * <ul>
   *   <li>March Female: ( 5 + 3 ) * 5 + 46 = 86
   *   <li>March Male: ( 3 + 7 ) * 1 + 46 = 56
   *   <li>April Female: ( 5 + 2 ) * 4 + 42 = 70
   *   <li>April Male: ( 4 + 4 ) * 1 + 42 = 50
   * </ul>
   *
   * <p>In April there are also predictions for Medicines, from BCG dispensed alone (23).
   */
  @Test
  void testPredictDisaggregatedProgramIndicatorsAndDataElements() {
    assertPredictions(
        "Combo",
        GENDER,
        "( sum( "
            + CASES_BY_GENDER
            + " ) + sum( "
            + R_BY_GENDER
            + " ) ) * sum( "
            + ADULT_CASES_BY_GENDER
            + " ) + sum( "
            + BCG_DISPENSED
            + " )",
        "Mar Female 86",
        "Mar Male 56",
        "Apr Female 70",
        "Apr Male 50",
        "Apr Female Medicines 23",
        "Apr Male Medicines 23");
  }

  /**
   * Tests predicting by Gender from a program indicator that is not disaggregated, so the whole
   * value is predicted for each Gender.
   */
  @Test
  void testPredictDisaggregatedFromNonDisaggregatedProgramIndicator() {
    assertPredictions(
        "NonDis",
        GENDER,
        "sum( " + INPATIENT_CASES + " )",
        "Mar Female 8",
        "Mar Male 8",
        "Apr Female 9",
        "Apr Male 9");
  }

  /**
   * Tests predicting by Gender and Age from a program indicator disaggregated by Gender and Age.
   * There is no data for Female Child in January or February, so there is no prediction for Female
   * Child in March.
   */
  @Test
  void testPredictTwoCategoryProgramIndicator() {
    assertPredictions(
        "SexAge",
        GENDER_AND_AGE,
        "sum( " + CASES_BY_GENDER_AND_AGE + " )",
        "Mar Female Adult 5",
        "Mar Male Child 2",
        "Mar Male Adult 1",
        "Apr Female Child 1",
        "Apr Female Adult 4",
        "Apr Male Child 3",
        "Apr Male Adult 1");
  }

  /**
   * Tests predicting by Gender from a program indicator disaggregated by Gender and Age, so the
   * ages are summed for each Gender.
   */
  @Test
  void testPredictOneCategoryFromTwoCategoryProgramIndicator() {
    assertPredictions(
        "SxFrSA",
        GENDER,
        "sum( " + CASES_BY_GENDER_AND_AGE + " )",
        "Mar Female 5",
        "Mar Male 3",
        "Apr Female 5",
        "Apr Male 4");
  }

  /**
   * Tests predicting by Gender and Outcome (attribute) from a program indicator disaggregated by
   * Gender with attribute Outcome, plus an aggregate data element disaggregated by Gender with
   * attribute Outcome. There is no data for Male Died in January or February, so there is no
   * prediction for Male Died in March.
   */
  @Test
  void testPredictCategoryAndAttributeProgramIndicatorAndDataElement() {
    assertPredictions(
        "Outcom",
        GENDER,
        "sum( " + CASES_BY_GENDER_WITH_OUTCOME + " ) + sum( " + T_BY_GENDER_WITH_OUTCOME + " )",
        "Mar Female Died 202",
        "Mar Female Survived 103",
        "Mar Male Survived 303",
        "Apr Female Died 201",
        "Apr Female Survived 104",
        "Apr Male Died 1",
        "Apr Male Survived 303");
  }

  /**
   * Tests predicting by Gender from a program indicator disaggregated by Gender without an
   * attribute combo, plus an aggregate data element disaggregated by Gender with attribute Outcome.
   * The program indicator's values are predicted with the default attribute option combo (the
   * attribute option combo of its events), and are not added into the predictions for each Outcome.
   */
  @Test
  void testPredictCategoryProgramIndicatorAndAttributeDataElement() {
    assertPredictions(
        "CasOut",
        GENDER,
        "sum( " + CASES_BY_GENDER + " ) + sum( " + T_BY_GENDER_WITH_OUTCOME + " )",
        "Mar Female 5",
        "Mar Male 3",
        "Mar Female Died 200",
        "Mar Female Survived 100",
        "Mar Male Survived 300",
        "Apr Female 5",
        "Apr Male 4",
        "Apr Female Died 200",
        "Apr Female Survived 100",
        "Apr Male Survived 300");
  }

  /**
   * Tests predicting to an output data element without disaggregation from a program indicator
   * disaggregated by Gender with attribute Outcome. The Genders are summed, and the predictions are
   * made for each Outcome.
   */
  @Test
  void testPredictNotDisaggregatedFromAttributeProgramIndicator() {
    assertPredictions(
        "NotDis",
        DEFAULT,
        "sum( " + CASES_BY_GENDER_WITH_OUTCOME + " )",
        "Mar Died 2",
        "Mar Survived 6",
        "Apr Died 2",
        "Apr Survived 7");
  }

  /**
   * Tests predicting by Gender from a program indicator with attribute Outcome but no category
   * combo. The whole value for each Outcome is predicted for each Gender.
   */
  @Test
  void testPredictDisaggregatedFromAttributeOnlyProgramIndicator() {
    assertPredictions(
        "OutOnl",
        GENDER,
        "sum( " + CASES_WITH_OUTCOME + " )",
        "Mar Female Died 2",
        "Mar Female Survived 6",
        "Mar Male Died 2",
        "Mar Male Survived 6",
        "Apr Female Died 2",
        "Apr Female Survived 7",
        "Apr Male Died 2",
        "Apr Male Survived 7");
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Makes and runs a predictor with its own output data element, and checks the values it predicts
   * at Ngelehun CHC.
   *
   * <p>If the output category combo is the default, the predictor's output category option combo is
   * the default. Otherwise, the predictor has no output category option combo, so it makes a
   * prediction for each category option combo of the output data element.
   *
   * @param code a code (up to 6 characters) that makes the predictor's UIDs unique
   * @param outputCategoryCombo the output data element's category combo
   * @param expression the predictor generator expression
   * @param expected each expected value as "period [category option combo] [attribute option combo]
   *     value", with default option combos left out, for example "Mar Female Died 2"
   */
  private void assertPredictions(
      String code, String outputCategoryCombo, String expression, String... expected) {
    String outputUid = uid("PrdOu", code);
    String predictorUid = uid("PrdPr", code);

    new MetadataActions()
        .importMetadata(
            """
            {
              "dataElements": [{
                "id": "%s",
                "name": "Prediction test %s output",
                "shortName": "Prediction test %s output",
                "valueType": "INTEGER",
                "aggregationType": "SUM",
                "domainType": "AGGREGATE",
                "categoryCombo": { "id": "%s" }
              }],
              "predictors": [{
                "id": "%s",
                "name": "Prediction test %s",
                "shortName": "Prediction test %s",
                "output": { "id": "%s" },
                %s
                "generator": { "expression": "%s" },
                "periodType": "Monthly",
                "organisationUnitLevels": [{ "id": "%s" }],
                "organisationUnitDescendants": "DESCENDANTS",
                "sequentialSampleCount": 2,
                "annualSampleCount": 0
              }]
            }
            """
                .formatted(
                    outputUid,
                    code,
                    code,
                    outputCategoryCombo,
                    predictorUid,
                    code,
                    code,
                    outputUid,
                    DEFAULT.equals(outputCategoryCombo)
                        ? "\"outputCombo\": { \"id\": \"%s\" },".formatted(DEFAULT_COC)
                        : "",
                    expression,
                    FACILITY_LEVEL))
        .validate()
        .body("status", equalTo("OK"));

    predictorActions
        .post(
            predictorUid + "/run",
            new JsonObject(),
            new QueryParamsBuilder().add("startDate=2022-03-01").add("endDate=2022-04-30"))
        .validateStatus(200);

    List<Map<String, String>> dataValues =
        dataValueSetActions
            .get(
                new QueryParamsBuilder()
                    .add("dataElement", outputUid)
                    .add("orgUnit", NGELEHUN_CHC)
                    .add("startDate", "2022-03-01")
                    .add("endDate", "2022-04-30"))
            .validateStatus(200)
            .extractList("dataValues");

    List<String> actual = new ArrayList<>();
    if (dataValues != null) {
      for (Map<String, String> dv : dataValues) {
        actual.add(
            String.join(
                " ",
                Stream.of(
                        LABELS.get(dv.get("period")),
                        LABELS.get(dv.get("categoryOptionCombo")),
                        LABELS.get(dv.get("attributeOptionCombo")),
                        dv.get("value"))
                    .filter(s -> s != null)
                    .toList()));
      }
    }

    assertEquals(sorted(List.of(expected)), sorted(actual));
  }

  /** Makes a UID from a prefix and a code, padded with zeros to 11 characters. */
  private static String uid(String prefix, String code) {
    return (prefix + code + "0".repeat(11)).substring(0, 11);
  }

  /** Returns a sorted copy of a list of strings. */
  private static List<String> sorted(List<String> list) {
    return list.stream().sorted().toList();
  }

  /** Makes a program category mapping from each category option UID to its filter. */
  private static JsonObject categoryMapping(
      String id, String categoryId, String mappingName, Map<String, String> optionFilters) {
    List<Map<String, String>> optionMappings =
        optionFilters.entrySet().stream()
            .map(e -> Map.of("optionId", e.getKey(), "filter", e.getValue()))
            .toList();

    return new Gson()
        .toJsonTree(
            Map.of(
                "id",
                id,
                "categoryId",
                categoryId,
                "mappingName",
                mappingName,
                "optionMappings",
                optionMappings))
        .getAsJsonObject();
  }

  /** Posts data values for a data set. */
  private void postDataValues(String dataSet, String... dataValues) {
    dataValueSetActions
        .post(
            """
            { "dataSet": "%s", "dataValues": [ %s ] }"""
                .formatted(dataSet, String.join(", ", dataValues)))
        .validate()
        .statusCode(200)
        .body("response.importCount.ignored", equalTo(0));
  }

  /** Makes a data value at Ngelehun CHC, as JSON. */
  private static String dataValue(
      String dataElement, String period, String coc, String aoc, int value) {
    return """
        { "dataElement": "%s", "period": "%s", "orgUnit": "%s", \
        "categoryOptionCombo": "%s", "attributeOptionCombo": "%s", "value": "%d" }"""
        .formatted(dataElement, period, NGELEHUN_CHC, coc, aoc, value);
  }
}
