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
package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateDataValue;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.actions.metadata.ProgramActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

/**
 * Groups e2e tests for "/analytics" aggregate endpoint.
 *
 * <p>This tests program indicator disaggregation using existing SL Demo data from the "Inpatient
 * morbidity and mortality" program. It creates a test Category Combination "Gender + Weight in
 * Kilograms" using the existing category "Gender" and a test category "Weight in Kilograms". It
 * creates a test program indicator that maps values from the event program data elements "Gender"
 * and "Weight in kg" to options in these categories.
 *
 * <p>The test category "Weight in Kilograms" includes options for a number of specific weights, as
 * well as an "Other weight" option for weight values not assigned to an option, and a "Missing
 * weight" option for events that are missing a weight value.
 */
public class AnalyticsPiDisaggregationTest extends AnalyticsApiTest {
  private MetadataActions metadataActions;

  private ProgramActions programActions;

  private RestApiActions actions;

  private static final String GENDER_DE_UID = "oZg33kd9taw"; // From SL Demo

  private static final String WEIGHT_IN_KG_DE_UID = "vV9UWAZohSf"; // From SL Demo

  private static final String GENDER_CAT_UID = "cX5k9anHEHd"; // From SL Demo

  private static final String FEMALE_OPTION_UID = "apsOixVZlf1"; // From SL Demo

  private static final String MALE_OPTION_UID = "jRbMi0aBjYn"; // From SL Demo

  private static final String NGELEHUN_CHC_UID = "DiszpKrYNg8"; // From SL Demo

  /** Inpatient morbidity and mortality program UID from SL Demo */
  private static final String PROGRAM_UID = "eBAyeGv0exc";

  /** Inpatient morbidity and mortality program stage UID from SL Demo */
  private static final String PROGRAM_STAGE_UID = "Zj7UnCAulEk";

  /** The remaining UIDs are randomly-generated for use in this test. */
  private static final String WEIGHT_CAT_UID = "fAe1g4o6ngo";

  private static final String CATCOMBO_UID = "HohH2ii7sah";

  private static final String GENDER_MAPPING_UID = "gIgiL8ow0bi";

  private static final String WEIGHT_MAPPING_UID = "aooDie1Fie4";

  private static final String PROGRAM_IND_UID = "Ham7uPo9cah";

  // * (Doesn't need to be a UID, could be any type of identifier.) */
  private static final String EXPORT_DE_ID = "ExportDeId";

  private static final List<String> GENDERS = List.of("Female", "Male");

  private static final Map<String, String> GENDER_OPTION_UIDS =
      Map.of("Female", FEMALE_OPTION_UID, "Male", MALE_OPTION_UID);

  /** Value of the lowest Weight category option. */
  private static final int LOWEST_WEIGHT = 20;

  /** Value of the highest Weight category option. */
  private static final int HIGHEST_WEIGHT = 60;

  /** Represents the category option for other weights */
  private static final String OTHER = "Other";

  /** Represents the category option for missing weights */
  private static final String MISSING = "Missing";

  private static final List<String> EXTRA_WEIGHT_OPTIONS = List.of(OTHER, MISSING);

  @BeforeAll
  public void setup() {
    metadataActions = new MetadataActions();
    programActions = new ProgramActions();
    actions = new RestApiActions("analytics");

    postMetadata();
    postProgramCategoryMappings();
  }

  /** Posts the new metadata used by this test. */
  private void postMetadata() {
    String json = getMetadataJson();
    ApiResponse response = metadataActions.importMetadata(json);
    response
        .validate()
        .statusCode(200)
        .body("stats.updated", equalTo(response.extract("stats.total")));
  }

  /** Builds a json string for the new metadata. */
  private String getMetadataJson() {
    StringBuilder sb =
        new StringBuilder(
            """
      {
      """);
    addCategory(sb);
    addCategoryCombo(sb);
    addCategoryOptions(sb);
    addCategoryOptionCombos(sb);
    addProgramIndicator(sb);
    sb.append(
        """
      }""");
    return sb.toString();
  }

  private void addCategory(StringBuilder sb) {
    sb.append(
        """
              "categories": [
                  {
                      "name": "Weight in Kilograms",
                      "shortName": "Weight in Kilograms",
                      "id": "%s",
                      "dataDimensionType": "DISAGGREGATION",
                      "dataDimension": true,
                      "categoryCombos": [
                          {
                              "id": "%s"
                          }
                      ],
                      "categoryOptions": [
                      """
            .formatted(WEIGHT_CAT_UID, CATCOMBO_UID));
    for (int i = LOWEST_WEIGHT; i <= HIGHEST_WEIGHT; i++) {
      sb.append(
          """
                          {
                              "id": "%s"
                          },
                          """
              .formatted(getWeightUid(i)));
    }
    sb.append(
        """
                          {
                              "id": "%s"
                          },
                          {
                              "id": "%s"
                          }
            ]
        }
    ],
    """
            .formatted(getWeightUid(OTHER), getWeightUid(MISSING)));
  }

  private void addCategoryCombo(StringBuilder sb) {
    sb.append(
        """
            "categoryCombos": [
                {
                    "name": "Gender + Weight in Kilograms",
                    "shortName": "Gender + Weight in Kilograms",
                    "id": "%s",
                    "dataDimensionType": "DISAGGREGATION",
                    "categories": [
                        {
                            "id": "%s"
                        },
                        {
                            "id": "%s"
                        }
                    ]
                }
            ],
        """
            .formatted(CATCOMBO_UID, GENDER_CAT_UID, WEIGHT_CAT_UID));
  }

  private void addCategoryOptions(StringBuilder sb) {
    sb.append(
        """
            "categoryOptions": [
        """);
    for (int i = LOWEST_WEIGHT; i <= HIGHEST_WEIGHT; i++) {
      sb.append(
          """

                {
                    "name": "%d Kg",
                    "shortName": "%d Kg",
                    "dataDimensionType": "DISAGGREGATION",
                    "id": "%s",
                    "sharing": {
                        "public": "rwrw----"
                    },
                    "categories": [
                        {
                            "id": "%s"
                        }
                    ]
                },
                """
              .formatted(i, i, getWeightUid(i), WEIGHT_CAT_UID));
    }
    sb.append(
        """
              {
                  "name": "Other weight",
                  "shortName": "Other weight",
                  "dataDimensionType": "DISAGGREGATION",
                  "id": "%s",
                  "sharing": {
                      "public": "rwrw----"
                  },
                  "categories": [
                      {
                          "id": "%s"
                      }
                  ]
              },
              {
                  "name": "Missing weight",
                  "shortName": "Missing weight",
                  "dataDimensionType": "DISAGGREGATION",
                  "id": "%s",
                  "sharing": {
                      "public": "rwrw----"
                  },
                  "categories": [
                      {
                          "id": "%s"
                      }
                  ]
              }
          ],
          """
            .formatted(getWeightUid(OTHER), WEIGHT_CAT_UID, getWeightUid(MISSING), WEIGHT_CAT_UID));
  }

  private void addCategoryOptionCombos(StringBuilder sb) {
    sb.append(
        """
            "categoryOptionCombos": [""");
    for (String gender : GENDERS) {
      for (int i = LOWEST_WEIGHT; i <= HIGHEST_WEIGHT; i++) {
        sb.append(
            """

                  {
                      "name": "%s, %s Kg",
                      "shortName": "%s, %s Kg",
                      "dataDimensionType": "DISAGGREGATION",
                      "id": "%s",
                      "categoryCombo": {
                          "id": "%s"
                       },
                      "categoryOptions": [
                          {
                              "id": "%s"
                          },
                          {
                              "id": "%s"
                          }
                      ]
                  },"""
                .formatted(
                    gender,
                    i,
                    gender,
                    i,
                    getCocUid(gender, i),
                    CATCOMBO_UID,
                    GENDER_OPTION_UIDS.get(gender),
                    getWeightUid(i)));
      }
      sb.append(
          """

                {
                    "name": "%s, Other Weight",
                    "shortName": "%s, Other Weight",
                    "dataDimensionType": "DISAGGREGATION",
                    "id": "%s",
                    "categoryCombo": {
                        "id": "%s"
                     },
                    "categoryOptions": [
                        {
                            "id": "%s"
                        },
                        {
                            "id": "%s"
                        }
                    ]
                },"""
              .formatted(
                  gender,
                  gender,
                  getCocUid(gender, OTHER),
                  CATCOMBO_UID,
                  GENDER_OPTION_UIDS.get(gender),
                  getWeightUid(OTHER)));
      sb.append(
          """

                {
                    "name": "%s, Missing Weight",
                    "shortName": "%s, Missing Weight",
                    "dataDimensionType": "DISAGGREGATION",
                    "id": "%s",
                    "categoryCombo": {
                      "id": "%s"
                     },
                    "categoryOptions": [
                        {
                            "id": "%s"
                        },
                        {
                            "id": "%s"
                        }
                    ]
                },"""
              .formatted(
                  gender,
                  gender,
                  getCocUid(gender, MISSING),
                  CATCOMBO_UID,
                  GENDER_OPTION_UIDS.get(gender),
                  getWeightUid(MISSING)));
    }
    sb.setLength(sb.length() - 1); // Remove trailing ','
    sb.append(
        """

            ],
            """);
  }

  private void addProgramIndicator(StringBuilder sb) {
    sb.append(
        """
              "programIndicators": [
                  {
                      "name": "Inpatient morbidity and mortality cases",
                      "shortName": "Inpatient morbidity and mortality cases",
                      "id": "%s",
                      "program": {
                          "id": "%s"
                      },
                      "analyticsType": "EVENT",
                      "categoryCombo": {
                          "id": "%s"
                      },
                      "attributeCombo": {
                          "id": "bjDvmb4bfuf"
                      },
                      "aggregationType": "SUM",
                      "expression": "V{event_count}",
                      "analyticsPeriodBoundaries": [
                          {
                              "id": "EAhNe5jie2a",
                              "boundaryTarget": "EVENT_DATE",
                              "analyticsPeriodBoundaryType": "AFTER_START_OF_REPORTING_PERIOD"
                          },
                          {
                              "id": "uooK4Ch3ip4",
                              "boundaryTarget": "EVENT_DATE",
                              "analyticsPeriodBoundaryType": "BEFORE_END_OF_REPORTING_PERIOD"
                          }
                      ],
                      "aggregateExportDataElement": "%s",
                      "categoryMappingIds": [
                          "%s",
                          "%s"
                      ]
                  }
              ]
            """
            .formatted(
                PROGRAM_IND_UID,
                PROGRAM_UID,
                CATCOMBO_UID,
                EXPORT_DE_ID,
                GENDER_MAPPING_UID,
                WEIGHT_MAPPING_UID));
  }

  private void postProgramCategoryMappings() {
    JsonObject genderMapping = getGenderMapping();
    JsonObject weightMapping = getWeightMapping();
    programActions.addOrReplaceCategoryMappings(PROGRAM_UID, List.of(genderMapping, weightMapping));
  }

  private JsonObject getGenderMapping() {
    StringBuilder sb =
        new StringBuilder(
            """
      {
          "id": "%s",
          "categoryId": "%s",
          "mappingName": "Gender mapping",
          "optionMappings": ["""
                .formatted(GENDER_MAPPING_UID, GENDER_CAT_UID));
    for (String gender : GENDERS) {
      String filter = "#{%s.%s} == '%s'".formatted(PROGRAM_STAGE_UID, GENDER_DE_UID, gender);
      sb.append(
          """

            {
                "optionId": "%s",
                "filter": "%s"
            },"""
              .formatted(GENDER_OPTION_UIDS.get(gender), filter));
    }
    sb.setLength(sb.length() - 1); // Remove trailing ','
    sb.append(
        """

              ]
          }
          """);

    return new Gson().fromJson(sb.toString(), JsonObject.class);
  }

  private JsonObject getWeightMapping() {
    StringBuilder sb =
        new StringBuilder(
            """
      {
          "id": "%s",
          "categoryId": "%s",
          "mappingName": "Weight mapping",
          "optionMappings": [
          """
                .formatted(WEIGHT_MAPPING_UID, WEIGHT_CAT_UID));
    for (int i = LOWEST_WEIGHT; i <= HIGHEST_WEIGHT; i++) {
      String filter = "#{%s.%s} == %d".formatted(PROGRAM_STAGE_UID, WEIGHT_IN_KG_DE_UID, i);
      sb.append(
          """
                 {
                      "optionId": "%s",
                      "filter": "%s"
                 },
                  """
              .formatted(getWeightUid(i), filter));
    }

    String otherFilter =
        "#{%s.%s} < %d or #{%s.%s} > %d"
            .formatted(
                PROGRAM_STAGE_UID,
                WEIGHT_IN_KG_DE_UID,
                LOWEST_WEIGHT,
                PROGRAM_STAGE_UID,
                WEIGHT_IN_KG_DE_UID,
                HIGHEST_WEIGHT);
    String missingFilter = "isNull(#{%s.%s})".formatted(PROGRAM_STAGE_UID, WEIGHT_IN_KG_DE_UID);
    sb.append(
        """
                 {
                      "optionId": "%s",
                      "filter": "%s"
                 },
                 {
                      "optionId": "%s",
                      "filter": "%s"
                 }
              ]
          }
              """
            .formatted(getWeightUid(OTHER), otherFilter, getWeightUid(MISSING), missingFilter));

    return new Gson().fromJson(sb.toString(), JsonObject.class);
  }

  /** Get the ith Weight Category Option UID */
  private String getWeightUid(int i) {
    return String.format("weight%05d", i);
  }

  /** Get the Category Option UID for "Other" or "Missing" weight */
  private String getWeightUid(String s) {
    Assert.isTrue(EXTRA_WEIGHT_OPTIONS.contains(s), "Unexpected weight: '" + s + "'");
    return String.format("weight%.5s", s);
  }

  /** Get the Category Option Combo UID for Gender + ith Weight */
  private String getCocUid(String gender, int i) {
    Assert.isTrue(GENDERS.contains(gender), "Unexpected gender: '" + gender + "'");
    return String.format("coc%.4s%04d", gender, i);
  }

  /** Get the Category Option Combo UID for Gender + "Other" or "Missing" weight */
  private String getCocUid(String gender, String s) {
    Assert.isTrue(GENDERS.contains(gender), "Unexpected gender: '" + gender + "'");
    Assert.isTrue(EXTRA_WEIGHT_OPTIONS.contains(s), "Unexpected weight: '" + s + "'");
    return String.format("coc%.4s%.4s", gender, s);
  }

  @Test
  void queryAllWeightsTotal() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:%s".formatted(PROGRAM_IND_UID))
            .add("filter=pe:2021,ou:%s".formatted(NGELEHUN_CHC_UID))
            .add("skipMeta=true");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of(PROGRAM_IND_UID, "117.0"));
  }

  @Test
  void queryAllWeightsByGender() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=%s,dx:%s".formatted(GENDER_CAT_UID, PROGRAM_IND_UID))
            .add("filter=pe:2021,ou:%s".formatted(NGELEHUN_CHC_UID))
            .add("skipMeta=true");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(2)))
        .body("height", equalTo(2))
        .body("width", equalTo(3));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, GENDER_CAT_UID, "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "51.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "66.0"));
  }

  @Test
  void queryAllWeightsByGenderAndWeight() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=%s,%s,dx:%s".formatted(GENDER_CAT_UID, WEIGHT_CAT_UID, PROGRAM_IND_UID))
            .add("filter=pe:2021,ou:%s".formatted(NGELEHUN_CHC_UID))
            .add("skipMeta=true");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(48)))
        .body("height", equalTo(48))
        .body("width", equalTo(4));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, GENDER_CAT_UID, "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        WEIGHT_CAT_UID,
        "Weight in Kilograms",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00021", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00022", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00024", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00026", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00030", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00031", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00032", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00033", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00035", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00037", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00043", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00044", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00047", "3.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00051", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00052", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00054", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00055", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00056", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00057", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00059", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weightOther", "22.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weightMissi", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00020", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00021", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00022", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00025", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00026", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00027", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00028", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00030", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00031", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00032", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00034", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00036", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00039", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00040", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00042", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00043", "2.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00045", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00050", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00054", "3.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00055", "3.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00056", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00057", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00058", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00059", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weightOther", "22.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weightMissi", "11.0"));
  }

  @Test
  void queryFilteredWeightsTotal() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:%s".formatted(PROGRAM_IND_UID))
            .add(
                "filter=pe:2021,ou:%s,%s:weight00055;weightOther"
                    .formatted(NGELEHUN_CHC_UID, WEIGHT_CAT_UID))
            .add("skipMeta=true");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of(PROGRAM_IND_UID, "48.0"));
  }

  @Test
  void queryFilteredWeightsByGender() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=%s,dx:%s".formatted(GENDER_CAT_UID, PROGRAM_IND_UID))
            .add(
                "filter=pe:2021,ou:%s,%s:weight00055;weightOther"
                    .formatted(NGELEHUN_CHC_UID, WEIGHT_CAT_UID))
            .add("skipMeta=true");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(2)))
        .body("height", equalTo(2))
        .body("width", equalTo(3));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, GENDER_CAT_UID, "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "23.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "25.0"));
  }

  @Test
  void queryFilteredWeightsByGenderAndWeight() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "dimension=%s,%s:weight00055;weightOther,dx:%s"
                    .formatted(GENDER_CAT_UID, WEIGHT_CAT_UID, PROGRAM_IND_UID))
            .add("filter=pe:2021,ou:%s".formatted(NGELEHUN_CHC_UID))
            .add("skipMeta=true");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(4)))
        .body("height", equalTo(4))
        .body("width", equalTo(4));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, GENDER_CAT_UID, "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        WEIGHT_CAT_UID,
        "Weight in Kilograms",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weight00055", "1.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, FEMALE_OPTION_UID, "weightOther", "22.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weight00055", "3.0"));
    validateRow(response, List.of(PROGRAM_IND_UID, MALE_OPTION_UID, "weightOther", "22.0"));
  }

  @Test
  void queryDataSetFilteredWeightsByGenderAndWeight() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2021,ou:%s,dx:%s".formatted(NGELEHUN_CHC_UID, PROGRAM_IND_UID))
            .add("filter=%s:weight00055;weightOther".formatted(WEIGHT_CAT_UID))
            .add("skipMeta=true");

    // When
    ApiResponse response = actions.get("/dataValueSet.json", params);

    // Then
    response.validate().statusCode(200).body("dataValues", hasSize(equalTo(4)));

    // Assert data values.
    validateDataValue(response, EXPORT_DE_ID, "2021", NGELEHUN_CHC_UID, "cocFema0055", "1.0");
    validateDataValue(response, EXPORT_DE_ID, "2021", NGELEHUN_CHC_UID, "cocFemaOthe", "22.0");
    validateDataValue(response, EXPORT_DE_ID, "2021", NGELEHUN_CHC_UID, "cocMale0055", "3.0");
    validateDataValue(response, EXPORT_DE_ID, "2021", NGELEHUN_CHC_UID, "cocMaleOthe", "22.0");
  }
}
