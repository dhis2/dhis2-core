/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for aggregate datasets with empty data entry forms. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/datasets/datasets_custom_data_entry_forms_empty.yaml}
 *
 * <p>Tests for programs and program stages with empty data entry forms. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/programs/programs_custom_data_entry_form_empty.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityEmptyCustomDataEntryFormsControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  @Test
  void testEmptyAggregateDataEntryForm() {

    final String check = "datasets_custom_data_entry_forms_empty";
    final String dataSetUID = "CowXAwmulDG";

    String defaultCatCombo = getDefaultCatCombo();
    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataSets",
            """
            {
                "name": "Test",
                "id": "%s",
                "shortName": "Test",
                "periodType": "Monthly",
                "categoryCombo": {"id": "%s"},
                "dataSetElements": [
                    {
                        "dataSet": {"id": "%s"},
                        "dataElement": {"id": "%s"}
                    }
                ]
            }
            """
                .formatted(dataSetUID, defaultCatCombo, dataSetUID, dataElementA)));

    assertStatus(
        HttpStatus.NO_CONTENT,
        POST(
            "/dataSets/" + dataSetUID + "/form",
            """
    {
      "style": "NORMAL",
      "htmlCode": "<h1>Test</h1>"
    }
    """));

    assertHasNoDataIntegrityIssues("dataSets", check, true);

    // The API silently ignores empty strings, so we use a space here instead.
    assertStatus(
        HttpStatus.NO_CONTENT,
        POST(
            "/dataSets/" + dataSetUID + "/form",
            """
    {
      "style": "NORMAL",
      "htmlCode": " "
    }
    """));

    assertHasDataIntegrityIssues("dataSets", check, 100, dataSetUID, "Test", "Test", true);
  }

  @Test
  @Disabled
  void testNoEventForm() {

    final String check = "programs_custom_data_entry_forms_empty";
    final String programUID = "IpHINAT79UW";
    final String programStageUID = "Zj7UnCAulGe";
    String defaultCatCombo = getDefaultCatCombo();

    assertHasNoDataIntegrityIssues("programs", check, false);

    // No form should be OK
    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
               {
               "programStages": [
                    {
                        "id": "%s",
                        "name": "Test",
                        "shortName": "Test",
                        "programStageDataElements": []
                    }
                ],
               "programs": [
                   {
                       "id": "%s",
                       "name": "Test",
                       "shortName": "Test",
                       "programType": "WITHOUT_REGISTRATION",
                       "categoryCombo": {"id": "%s"},
                       "programStages": [{"id": "%s"}]
                       }
                     ]
                }
                """
                .formatted(programStageUID, programUID, defaultCatCombo, programStageUID)));

    assertHasNoDataIntegrityIssues("programs", check, true);
  }

  @Test
  @Disabled
  void testNonEmptyEventFormHasNoDataIntegrityIssues() {
    final String check = "programs_custom_data_entry_forms_empty";
    final String programUID = "IpHINAT79UW";
    final String programStageUID = "Zj7UnCAulGe";
    final String dataEntryFormUID = "V2wox39jSdy";
    String defaultCatCombo = getDefaultCatCombo();

    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
                   {
                   "dataEntryForms": [
                       {
                           "id": "%s",
                           "name": "Test",
                           "htmlCode": "<h1>Test</h1>"
                       }
                   ],
                   "programStages": [
                        {
                            "id": "%s",
                            "name": "Test",
                            "shortName": "Test",
                            "programStageDataElements": [],
                            "dataEntryForm": {"id": "%s"}
                        }
                    ],
                   "programs": [
                       {
                           "id": "%s",
                           "name": "Test",
                           "shortName": "Test",
                           "programType": "WITHOUT_REGISTRATION",
                           "categoryCombo": {"id": "%s"},
                           "programStages": [{"id": "%s"}]
                           }
                         ]
                    }
                    """
                .formatted(
                    dataEntryFormUID,
                    programStageUID,
                    dataEntryFormUID,
                    programUID,
                    defaultCatCombo,
                    programStageUID)));

    assertHasNoDataIntegrityIssues("programs", check, true);
  }

  @Test
  void testEmptyEventFormHasIntegrityIssues() {
    final String check = "programs_custom_data_entry_forms_empty";
    final String programUID = "IpHINAT79UW";
    final String programStageUID = "Zj7UnCAulGe";
    final String dataEntryFormUID = "V2wox39jSdy";
    String defaultCatCombo = getDefaultCatCombo();

    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
               {
               "dataEntryForms": [
                   {
                       "id": "%s",
                       "name": "Test",
                       "htmlCode": ""
                   }
               ],
               "programStages": [
                    {
                        "id": "%s",
                        "name": "Test PS",
                        "shortName": "Test PS",
                        "programStageDataElements": [],
                        "dataEntryForm": {"id": "%s"}
                    }
                ],
               "programs": [
                   {
                       "id": "%s",
                       "name": "Test",
                       "shortName": "Test",
                       "programType": "WITH_REGISTRATION",
                       "categoryCombo": {"id": "%s"},
                       "programStages": [{"id": "%s"}]
                       }
                     ]
                }
                """
                .formatted(
                    dataEntryFormUID,
                    programStageUID,
                    dataEntryFormUID,
                    programUID,
                    defaultCatCombo,
                    programStageUID)));

    assertHasDataIntegrityIssues("programs", check, 100, programUID, "Test", "Test PS", true);
  }

  @Test
  void testNonEmptyTrackerFormHasNoDataIntegrityIssues() {
    final String check = "programs_custom_data_entry_forms_empty";
    final String programUID = "IpHINAT79UW";
    final String dataEntryFormUID = "V2wox39jSdy";
    String defaultCatCombo = getDefaultCatCombo();

    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
            {
                "dataEntryForms": [
                    {
                        "id": "%s",
                        "name": "Test",
                        "htmlCode": "<h1>Test</h1>"
                    }
                ],
                "programs": [
                    {
                        "id": "%s",
                        "name": "Test",
                        "shortName": "Test",
                        "programType": "WITH_REGISTRATION",
                        "categoryCombo": {"id": "%s"},
                        "dataEntryForm": {"id": "%s"}
                    }
                ]
            }
            """
                .formatted(dataEntryFormUID, programUID, defaultCatCombo, dataEntryFormUID)));

    assertHasNoDataIntegrityIssues("programs", check, true);
  }

  @Test
  void testEmptyTrackerFormHasDataIntegrityIssues() {
    final String check = "programs_custom_data_entry_forms_empty";
    final String programUID = "IpHINAT79UW";
    final String dataEntryFormUID = "V2wox39jSdy";
    String defaultCatCombo = getDefaultCatCombo();

    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
            {
                "dataEntryForms": [
                    {
                        "id": "%s",
                        "name": "Test",
                        "htmlCode": ""
                    }
                ],
                "programs": [
                    {
                        "id": "%s",
                        "name": "Test",
                        "shortName": "Test",
                        "programType": "WITH_REGISTRATION",
                        "categoryCombo": {"id": "%s"},
                        "dataEntryForm": {"id": "%s"}
                    }
                ]
            }
            """
                .formatted(dataEntryFormUID, programUID, defaultCatCombo, dataEntryFormUID)));

    assertHasDataIntegrityIssues("programs", check, 100, programUID, "Test", "Enrollment", true);
  }
}
