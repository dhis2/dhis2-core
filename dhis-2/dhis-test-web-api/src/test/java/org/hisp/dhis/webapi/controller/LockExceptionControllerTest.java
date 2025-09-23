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

import static java.lang.String.format;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link LockExceptionController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class LockExceptionControllerTest extends PostgresControllerIntegrationTestBase {

  private String ouId;

  private String dsId;

  @BeforeEach
  void setUp() {
    dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/", "{'name':'My data set', 'shortName':'MDS', 'periodType':'Monthly'}"));
    ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    assertStatus(HttpStatus.OK, POST("/organisationUnits/{ou}/dataSets/{ds}", ouId, dsId));
  }

  @Test
  void testAddLockException() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "LockException created successfully.",
        POST("/lockExceptions/?ou={ou}&pe=2021-01&ds={ds}", ouId, dsId)
            .content(HttpStatus.CREATED));
  }

  @Test
  void testAddLockException_UnauthorizedUser() {
    switchToNewUser("guest", "F_DATASET_PUBLIC_ADD");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "You can only add a lock exceptions to your data capture organisation units.",
        POST("/lockExceptions/?ou={ou}&pe=2021-01&ds={ds}", ouId, dsId)
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  void testAddLockException_DataSetNotLinked() {
    String dsId2 =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set2', 'shortName':'MDS2', 'periodType':'Monthly'}"));
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        format(
            "None of the target organisation unit(s) %s is linked to the specified data set: %s",
            ouId, dsId2),
        POST("/lockExceptions/?ou={ou}&pe=2021-01&ds={ds}", ouId, dsId2)
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testAddLockException_NoOrgUnit() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "OrganisationUnit ID is invalid.",
        POST("/lockExceptions/?ou=&pe=2021-01&ds=" + dsId).content(HttpStatus.CONFLICT));
  }

  @Test
  void testAddLockException_IllegalOrgUnit() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Can't find OrganisationUnit with id =xyz",
        POST("/lockExceptions/?ou=xyz&pe=2021-01&ds=" + dsId).content(HttpStatus.CONFLICT));
  }
}
