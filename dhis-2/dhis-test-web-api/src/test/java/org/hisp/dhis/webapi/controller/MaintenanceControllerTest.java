/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MaintenanceController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class MaintenanceControllerTest extends DhisControllerConvenienceTest {

  @Test
  void testPruneDataByOrganisationUnit() {
    String ougId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Data was pruned successfully",
        POST("/maintenance/dataPruning/organisationUnits/" + ougId).content());
  }

  @Test
  void testPruneDataByOrganisationUnit_DoesNotExist() {
    String ouId = CodeGenerator.generateUid();
    assertEquals(
        "Organisation unit does not exist: " + ouId,
        POST("/maintenance/dataPruning/organisationUnits/" + ouId)
            .error(HttpStatus.CONFLICT)
            .getMessage());
  }

  @Test
  void testPruneDataByOrganisationUnit_MissingAuthority() {
    switchToNewUser("guest");
    assertEquals(
        "Access is denied",
        POST("/maintenance/dataPruning/organisationUnits/xzy")
            .error(HttpStatus.FORBIDDEN)
            .getMessage());
  }

  @Test
  void testPruneDataByDataElement() {
    String ccId =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0)
            .getString("id")
            .string();
    String deId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements/",
                "{'name':'My data element', 'shortName':'DE1', 'code':'DE1', 'valueType':'INTEGER', "
                    + "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', "
                    + "'categoryCombo': {'id': '"
                    + ccId
                    + "'}}"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Data was pruned successfully",
        POST("/maintenance/dataPruning/dataElements/" + deId).content());
  }

  @Test
  void testPruneDataByDataElement_DoesNotExist() {
    String deId = CodeGenerator.generateUid();
    assertEquals(
        "Data element does not exist: " + deId,
        POST("/maintenance/dataPruning/dataElements/" + deId)
            .error(HttpStatus.CONFLICT)
            .getMessage());
  }

  @Test
  void testPruneDataByDataElement_MissingAuthority() {
    switchToNewUser("guest");
    assertEquals(
        "Access is denied",
        POST("/maintenance/dataPruning/dataElements/xzy").error(HttpStatus.FORBIDDEN).getMessage());
  }

  @Test
  void testAppReload() {
    assertWebMessage("OK", 200, "OK", "Apps reloaded", GET("/maintenance/appReload").content());
  }

  @Test
  void testAppReload_MissingAuthority() {
    switchToNewUser("guest");
    assertEquals(
        "Access is denied", GET("/maintenance/appReload").error(HttpStatus.FORBIDDEN).getMessage());
  }

  @Test
  void testUpdateCategoryOptionCombos() {
    String ccId =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0)
            .getString("id")
            .string();
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Import was successful.",
        POST("/maintenance/categoryOptionComboUpdate/categoryCombo/" + ccId).content());
  }

  @Test
  void testUpdateCategoryOptionCombos_DoesNotExist() {
    assertEquals(
        "CategoryCombo does not exist: xyz",
        POST("/maintenance/categoryOptionComboUpdate/categoryCombo/xyz")
            .error(HttpStatus.CONFLICT)
            .getMessage());
  }

  @Test
  void testUpdateCategoryOptionCombos_MissingAuthority() {
    switchToNewUser("guest");
    assertEquals(
        "Access is denied",
        POST("/maintenance/categoryOptionComboUpdate/categoryCombo/xyz")
            .error(HttpStatus.FORBIDDEN)
            .getMessage());
  }
}
