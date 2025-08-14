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
import static org.hisp.dhis.http.HttpClientAdapter.Accept;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML_ADX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_XML;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests the {@link DataValueSetController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class DataValueSetControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired protected TransactionTemplate transactionTemplate;

  @Test
  void testPostJsonDataValueSet() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Import was successful.",
        POST("/38/dataValueSets/", "{}").content(HttpStatus.OK));
  }

  @Test
  void testPostJsonDataValueSet_Async() {
    JsonWebMessage msg = assertWebMessage(HttpStatus.OK, POST("/dataValueSets?async=true", "{}"));
    assertStartsWith("Initiated DATAVALUE_IMPORT", msg.getMessage());
  }

  @Test
  void testPostAdxDataValueSet() {
    transactionTemplate.execute(
        status -> {
          User user = makeUser("X", List.of("ALL"));
          userService.addUser(user);
          injectSecurityContextUser(user);

          String content =
              POST(
                      "/38/dataValueSets/",
                      Body("<adx xmlns=\"urn:ihe:qrph:adx:2015\"></adx>"),
                      ContentType(CONTENT_TYPE_XML_ADX),
                      Accept(CONTENT_TYPE_XML))
                  .content(APPLICATION_XML.toString());

          assertTrue(content.contains("httpStatusCode=\"200\""));

          return null;
        });
  }

  @Test
  void testPostAdxDataValueSet_Async() {
    String content =
        POST(
                "/dataValueSets?async=true",
                Body("<adx xmlns=\"urn:ihe:qrph:adx:2015\"></adx>"),
                ContentType(CONTENT_TYPE_XML_ADX),
                Accept(CONTENT_TYPE_XML))
            .content(APPLICATION_XML.toString());
    assertTrue(content.contains("httpStatusCode=\"200\""));
    assertTrue(content.contains("Initiated DATAVALUE_IMPORT"));
  }

  @Test
  void testPostDxf2DataValueSet() {
    String content =
        POST(
                "/38/dataValueSets/",
                Body("<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\"></dataValueSet>"),
                ContentType(APPLICATION_XML),
                Accept(CONTENT_TYPE_XML))
            .content(APPLICATION_XML.toString());
    assertTrue(content.contains("httpStatusCode=\"200\""));
  }

  @Test
  void testPostDxf2DataValueSet_Async() {
    String content =
        POST(
                "/dataValueSets?async=true",
                Body("<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\"></dataValueSet>"),
                ContentType(APPLICATION_XML),
                Accept(CONTENT_TYPE_XML))
            .content(APPLICATION_XML.toString());
    assertTrue(content.contains("httpStatusCode=\"200\""));
    assertTrue(content.contains("Initiated DATAVALUE_IMPORT"));
  }

  @Test
  void testPostCsvDataValueSet() {
    String csv =
        "dataelement,period,orgunit,categoryoptioncombo,attributeoptioncombo,value,storedby,lastupdated,comment,followup,deleted";
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Import was successful.",
        POST("/38/dataValueSets/", Body(csv), ContentType("application/csv"))
            .content(HttpStatus.OK));
  }

  @Test
  void testPostCsvDataValueSet_Async() {
    String csv =
        "dataelement,period,orgunit,categoryoptioncombo,attributeoptioncombo,value,storedby,lastupdated,comment,followup,deleted";
    JsonWebMessage msg =
        assertWebMessage(
            HttpStatus.OK,
            POST("/dataValueSets?async=true", Body(csv), ContentType("application/csv")));
    assertStartsWith("Initiated DATAVALUE_IMPORT", msg.getMessage());
  }

  @Test
  void testGetDataValueSetJson() {
    String ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01',"
                    + " 'code':'OU1'}"));
    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));
    JsonWebMessage response =
        GET(
                "/dataValueSets/?inputOrgUnitIdScheme=code&idScheme=name&orgUnit={ou}&period=2022-01&dataSet={ds}",
                "OU1",
                dsId)
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);
    assertEquals(
        String.format("User is not allowed to view org unit: `%s`", ouId), response.getMessage());
  }

  @Test
  @DisplayName("Should return error message when user does not have DATA_READ to DataSet")
  void testGetDataValueSetJsonWithNonAccessibleDataSet() {
    String orgUnitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01',"
                    + " 'code':'OU1'}"));
    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));

    switchToNewUser(
        createAndAddUser(Set.of(), Set.of(manager.get(OrganisationUnit.class, orgUnitId))));
    JsonWebMessage response =
        GET(
                "/dataValueSets/?inputOrgUnitIdScheme=code&idScheme=name&orgUnit={ou}&period=2022-01&dataSet={ds}&async=true",
                "OU1",
                dsId)
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);
    assertEquals(
        String.format("User is not allowed to read data for data set: `%s`", dsId),
        response.getMessage());
  }
}
