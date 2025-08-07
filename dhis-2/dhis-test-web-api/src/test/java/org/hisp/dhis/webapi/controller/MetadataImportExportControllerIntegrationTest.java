/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClient.Body;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.webapi.json.domain.JsonTypeReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetadataImportExportControllerIntegrationTest extends DhisControllerIntegrationTest {

  @Test
  @DisplayName(
      "Importing an existing CategoryCombo, which has no data values, with an additional Category succeeds")
  void importingCategoryComboNewCategoryNoDataTest() {
    // Given existing metadata (including 1 CategoryCombo with 2 Categories)
    JsonImportSummary initialImport =
        POST("/metadata", Body(metadataImport()))
            .content()
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", initialImport.getStatus());
    assertEquals(13, initialImport.getStats().getCreated());

    // When importing the existing CategoryCombo with an additional Category
    // Then the import should succeed with the expected message & stats
    JsonImportSummary updateImport =
        POST("/metadata", Body(getCatComboWithAdditionalCategory()))
            .content()
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", updateImport.getStatus());
    assertEquals(3, updateImport.getStats().getUpdated());
    assertEquals(1, updateImport.getStats().getCreated());
  }

  @Test
  @DisplayName(
      "Importing an existing CategoryCombo, which has data values, with an additional Category fails")
  void importingCategoryComboNewCategoryWitDataTest() {
    // Given existing metadata (including 1 CategoryCombo with 2 Categories)
    JsonImportSummary initialImport =
        POST("/metadata", Body(metadataImport()))
            .content()
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", initialImport.getStatus());
    assertEquals(13, initialImport.getStats().getCreated());

    User currentUser = getCurrentUser();

    // add org unit to user
    PATCH("/users/" + currentUser.getUid(), Body(updateUserOrgUnit("OrgUnitUid1")))
        .content(HttpStatus.OK);

    // and data value exists with one of the COCs
    POST("/dataValues", Body(getDataValue())).content(HttpStatus.CREATED);

    // When importing the existing CategoryCombo with an additional Category
    // Then the import should fail with the expected message & stats
    JsonImportSummary updateImport =
        POST("/metadata", Body(getCatComboWithAdditionalCategory()))
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("ERROR", updateImport.getStatus());
    assertEquals(0, updateImport.getStats().getUpdated());
    assertEquals(4, updateImport.getStats().getIgnored());

    JsonTypeReport klass =
        updateImport.getTypeReports().stream()
            .filter(
                tr -> tr.getString("klass").string().equals("org.hisp.dhis.category.CategoryCombo"))
            .findFirst()
            .get();

    assertEquals(
        "Update cannot be applied as it would make existing data values inaccessible",
        klass.getObjectReports().get(0).getErrorReports().get(0).getMessage());
  }

  @Test
  @DisplayName(
      "Importing an existing CategoryCombo, which has no data values, with its original Categories succeeds")
  void importingCategoryComboNoChangeWitDataTest() {
    // Given existing metadata (including 1 CategoryCombo with 2 Categories)
    JsonImportSummary initialImport =
        POST("/metadata", Body(metadataImport()))
            .content()
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", initialImport.getStatus());
    assertEquals(13, initialImport.getStats().getCreated());

    User currentUser = getCurrentUser();

    // add org unit to user
    PATCH("/users/" + currentUser.getUid(), Body(updateUserOrgUnit("OrgUnitUid1")))
        .content(HttpStatus.OK);

    // and data value exists with one of the COCs
    POST("/dataValues", Body(getDataValue())).content(HttpStatus.CREATED);

    // When importing the existing CategoryCombo with its original Categories
    // Then the import should succeed with the expected message & stats
    JsonImportSummary updateImport =
        POST("/metadata", Body(metadataImport()))
            .content(HttpStatus.OK)
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", updateImport.getStatus());
    assertEquals(13, updateImport.getStats().getUpdated());
    assertEquals(0, updateImport.getStats().getIgnored());
  }

  private String updateUserOrgUnit(String orgUnit) {
    return String.format(
        "[ { \"op\": \"add\", \"path\": \"/organisationUnits\", \"value\": [ { \"id\": \"%s\" } ] } ]",
        orgUnit);
  }

  private String getDataValue() {
    return "{ "
        + "\"dataElement\": \"DeUid000001\","
        + " \"period\": \"20231101\","
        + " \"orgUnit\": \"OrgUnitUid1\","
        + " \"categoryOptionCombo\": \"CocUid00001\","
        + " \"attributeOptionCombo\": \"HllvX50cXC0\","
        + " \"value\": \"2000\","
        + " \"followup\": false "
        + "}";
  }

  private String metadataImport() {
    return "{"
        + "    \"organisationUnits\": ["
        + "        {"
        + "            \"id\": \"OrgUnitUid1\","
        + "            \"name\": \"test org 1\","
        + "            \"shortName\": \"test org 1\","
        + "            \"openingDate\": \"2023-06-15\""
        + "        }"
        + "    ],"
        + "    \"dataElements\": ["
        + "        {"
        + "            \"id\": \"DeUid000001\","
        + "            \"aggregationType\": \"DEFAULT\","
        + "            \"domainType\": \"AGGREGATE\","
        + "            \"name\": \"test de 1 - central v1\","
        + "            \"shortName\": \"test de 1 - central v1\","
        + "            \"valueType\": \"TEXT\""
        + "        }"
        + "    ],"
        + "    \"categoryOptions\": ["
        + "        {"
        + "            \"id\": \"CatOptUid01\","
        + "            \"name\": \"cat opt 1\","
        + "            \"shortName\": \"cat opt 1\""
        + "        },"
        + "        {"
        + "            \"id\": \"CatOptUid02\","
        + "            \"name\": \"cat opt 2\","
        + "            \"shortName\": \"cat opt 2\""
        + "        },"
        + "        {"
        + "            \"id\": \"CatOptUid03\","
        + "            \"name\": \"cat opt 3\","
        + "            \"shortName\": \"cat opt 3\""
        + "        },"
        + "        {"
        + "            \"id\": \"CatOptUid04\","
        + "            \"name\": \"cat opt 4\","
        + "            \"shortName\": \"cat opt 4\""
        + "        }"
        + "    ],"
        + "    \"categories\": ["
        + "        {"
        + "            \"id\": \"CategoUid01\","
        + "            \"name\": \"cat 1\","
        + "            \"shortName\": \"cat 1\","
        + "            \"dataDimensionType\": \"DISAGGREGATION\","
        + "            \"categoryOptions\": ["
        + "                {"
        + "                    \"id\": \"CatOptUid01\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CatOptUid02\""
        + "                }"
        + "            ]"
        + "        },"
        + "        {"
        + "            \"id\": \"CategoUid02\","
        + "            \"name\": \"cat 2\","
        + "            \"shortName\": \"cat 2\","
        + "            \"dataDimensionType\": \"DISAGGREGATION\","
        + "            \"categoryOptions\": ["
        + "                {"
        + "                    \"id\": \"CatOptUid03\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CatOptUid04\""
        + "                }"
        + "            ]"
        + "        }"
        + "    ],"
        + "    \"categoryCombos\": ["
        + "        {"
        + "            \"id\": \"CatComUid01\","
        + "            \"name\": \"cat combo 1\","
        + "            \"dataDimensionType\": \"DISAGGREGATION\","
        + "            \"categories\": ["
        + "                {"
        + "                    \"id\": \"CategoUid01\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CategoUid02\""
        + "                }"
        + "            ]"
        + "        }"
        + "    ],"
        + "    \"categoryOptionCombos\": ["
        + "        {"
        + "            \"name\": \"cat opt 1, cat opt 3\","
        + "            \"id\": \"CocUid00001\","
        + "            \"categoryCombo\": {"
        + "                \"id\": \"CatComUid01\""
        + "            },"
        + "            \"categoryOptions\": ["
        + "                {"
        + "                    \"id\": \"CatOptUid01\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CatOptUid03\""
        + "                }"
        + "            ]"
        + "        },"
        + "        {"
        + "            \"name\": \"cat opt 1, cat opt 4\","
        + "            \"id\": \"CocUid00002\","
        + "            \"categoryCombo\": {"
        + "                \"id\": \"CatComUid01\""
        + "            },"
        + "            \"categoryOptions\": ["
        + "                {"
        + "                    \"id\": \"CatOptUid01\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CatOptUid04\""
        + "                }"
        + "            ]"
        + "        },"
        + "        {"
        + "            \"name\": \"cat opt 2, cat opt 3\","
        + "            \"id\": \"CocUid00003\","
        + "            \"categoryCombo\": {"
        + "                \"id\": \"CatComUid01\""
        + "            },"
        + "            \"categoryOptions\": ["
        + "                {"
        + "                    \"id\": \"CatOptUid02\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CatOptUid03\""
        + "                }"
        + "            ]"
        + "        },"
        + "        {"
        + "            \"name\": \"cat opt 2, cat opt 4\","
        + "            \"id\": \"CocUid00004\","
        + "            \"categoryCombo\": {"
        + "                \"id\": \"CatComUid01\""
        + "            },"
        + "            \"categoryOptions\": ["
        + "                {"
        + "                    \"id\": \"CatOptUid02\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CatOptUid04\""
        + "                }"
        + "            ]"
        + "        }"
        + "    ]"
        + "}";
  }

  private String getCatComboWithAdditionalCategory() {
    return "{"
        + "    \"categories\": ["
        + "        {"
        + "            \"id\": \"CategoUid01\","
        + "            \"name\": \"cat 1\","
        + "            \"shortName\": \"cat 1\","
        + "            \"dataDimensionType\": \"DISAGGREGATION\","
        + "            \"categoryOptions\": []"
        + "        },"
        + "        {"
        + "            \"id\": \"CategoUid02\","
        + "            \"name\": \"cat 2\","
        + "            \"shortName\": \"cat 2\","
        + "            \"dataDimensionType\": \"DISAGGREGATION\","
        + "            \"categoryOptions\": []"
        + "        },"
        + "        {"
        + "            \"id\": \"CategoUid03\","
        + "            \"name\": \"cat 3\","
        + "            \"shortName\": \"cat 3\","
        + "            \"dataDimensionType\": \"DISAGGREGATION\","
        + "            \"categoryOptions\": []"
        + "        }"
        + "    ],"
        + "    \"categoryCombos\": ["
        + "        {"
        + "            \"id\": \"CatComUid01\","
        + "            \"name\": \"cat combo 1\","
        + "            \"dataDimensionType\": \"DISAGGREGATION\","
        + "            \"categories\": ["
        + "                {"
        + "                    \"id\": \"CategoUid01\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CategoUid02\""
        + "                },"
        + "                {"
        + "                    \"id\": \"CategoUid03\""
        + "                }"
        + "            ]"
        + "        }"
        + "    ]"
        + "}";
  }
}
