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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the Configuration controller endpoints.
 *
 * @author Generated Test
 */
@Transactional
class ConfigurationControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  @DisplayName("Configuration object should have infrastructuralIndicators property")
  void testConfigurationHasInfrastructuralIndicatorsProperty() {
    // Get the configuration object
    JsonObject configuration = GET("/configuration").content(HttpStatus.OK).as(JsonObject.class);

    assertNotNull(configuration);

    // Verify that infrastructuralIndicators property exists (should be null initially)
    assertFalse(configuration.get("infrastructuralIndicators").exists());
  }

  @Test
  @DisplayName("GET /configuration should return complete configuration object")
  void testGetConfiguration() {
    // Get the configuration object
    JsonObject configuration = GET("/configuration").content(HttpStatus.OK).as(JsonObject.class);

    assertNotNull(configuration);

    // Verify key properties are present in the configuration object
    configuration.has("systemId");
    configuration.has("feedbackRecipients");
    configuration.has("systemUpdateNotificationRecipients");
    configuration.has("offlineOrganisationUnitLevel");
    configuration.has("infrastructuralIndicators");
    configuration.has("infrastructuralDataElements");
    configuration.has("infrastructuralPeriodType");
    configuration.has("selfRegistrationRole");
    configuration.has("selfRegistrationOrgUnit");
    configuration.has("facilityOrgUnitGroupSet");
    configuration.has("facilityOrgUnitLevel");
    configuration.has("corsWhitelist");
  }

  @Test
  @DisplayName("GET /configuration/infrastructuralIndicators should return correct payload")
  void testGetInfrastructuralIndicators() {
    // Test GET endpoint for infrastructuralIndicators when not set (should return null/empty)
    JsonObject response = GET("/configuration/infrastructuralIndicators").content(HttpStatus.OK).as(JsonObject.class);

    // When no infrastructural indicators are configured, the response should be empty or null
    assertEquals(0, response.size());
  }

  @Test
  @DisplayName("GET /configuration/infrastructuralIndicators should return indicator group when set")
  void testGetInfrastructuralIndicatorsWhenSet() {
    // Create an IndicatorType first (required for indicators)
    String indicatorTypeId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorTypes/",
                """
            {
                'name': 'Infrastructure Indicator Type',
                'factor': 1
            }
            """));

    // Create an Indicator
    String indicatorId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators/",
                """
            {
                'name': 'Infrastructure Indicator',
                'shortName': 'INFR_IND',
                'indicatorType': {
                    'id': '%s'
                },
                'numerator': '1',
                'denominator': '1'
            }
            """
                    .formatted(indicatorTypeId)));

    // Create an IndicatorGroup for infrastructural indicators
    String indicatorGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups/",
                """
            {
                'name': 'Infrastructural Indicators',
                'shortName': 'INFR_IND_GRP',
                'description': 'Group for infrastructural indicators',
                'indicators': [
                    {'id': '%s'}
                ]
            }
            """
                    .formatted(indicatorId)));

    // Set the infrastructural indicators configuration
    assertStatus(
        HttpStatus.NO_CONTENT,
        POST("/configuration/infrastructuralIndicators", "\"" + indicatorGroupId + "\""));

    // Now test the GET endpoint
    JsonObject indicatorGroup = GET("/configuration/infrastructuralIndicators").content(HttpStatus.OK).as(JsonObject.class);

    assertNotNull(indicatorGroup);
    assertEquals("Infrastructural Indicators", indicatorGroup.getString("name").string());
    assertEquals("INFR_IND_GRP", indicatorGroup.getString("shortName").string());
    assertEquals("Group for infrastructural indicators", indicatorGroup.getString("description").string());
    assertEquals(indicatorGroupId, indicatorGroup.getString("id").string());

    // Verify the configuration object also reflects this change
    JsonObject configuration = GET("/configuration").content(HttpStatus.OK).as(JsonObject.class);
    JsonObject configInfraIndicators = configuration.getObject("infrastructuralIndicators");
    assertNotNull(configInfraIndicators);
    assertEquals(indicatorGroupId, configInfraIndicators.getString("id").string());
    assertEquals("Infrastructural Indicators", configInfraIndicators.getString("name").string());
  }

  @Test
  @DisplayName("POST /configuration/infrastructuralIndicators should set the infrastructural indicators")
  void testSetInfrastructuralIndicators() {
    // Create an IndicatorGroup
    String indicatorGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups/",
                """
            {
                'name': 'Test Infrastructure Group',
                'shortName': 'TIG'
            }
            """));

    // Set the infrastructural indicators
    assertStatus(
        HttpStatus.NO_CONTENT,
        POST("/configuration/infrastructuralIndicators", "\"" + indicatorGroupId + "\""));

    // Verify it was set correctly
    JsonObject indicatorGroup = GET("/configuration/infrastructuralIndicators").content(HttpStatus.OK).as(JsonObject.class);
    assertEquals(indicatorGroupId, indicatorGroup.getString("id").string());
    assertEquals("Test Infrastructure Group", indicatorGroup.getString("name").string());
  }
}