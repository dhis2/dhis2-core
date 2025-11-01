/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.test.utils.CsvUtils.getRowCountFromCsv;
import static org.hisp.dhis.test.utils.CsvUtils.getRowFromCsv;
import static org.hisp.dhis.test.utils.CsvUtils.getValueFromCsv;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonUser;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the generic operations offered by the {@code AbstractFullReadOnlyController} using specific
 * endpoints.
 *
 * @author David Mackessy
 */
@Transactional
class AbstractFullReadOnlyControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private DataElementService dataElementService;

  @Test
  void testGetObjectList_QueryUsers() {
    // this just simulates the normal setup with a system super-user
    User user = switchToNewUser("system", "ALL");
    // make sure "system" does not occur in any other property that might be searched by query=
    user.setName("x");
    user.setFirstName("y");
    user.setSurname("z");
    user.setCode("xyz");
    userService.updateUser(user);

    JsonList<JsonUser> users =
        GET("/users?fields=id,name,username&query=system")
            .content()
            .getList("users", JsonUser.class);
    assertEquals(1, users.size());
    assertEquals("system", users.get(0).getUsername());
  }

  @Test
  void testGetObjectListCsv() {
    createDataElements(36);

    String response = GET("/dataElements.csv").content("text/csv");

    assertNotNull(response);
    String firstRow = getRowFromCsv(0, response);
    String uid = getValueFromCsv(0, 1, response);
    String displayName = getValueFromCsv(1, 2, response);

    assertEquals("id,displayName", firstRow);
    // confirms valid UID if created with no exception
    CodeGenerator.isValidUid(uid);
    assertEquals("DataElement1", displayName);
  }

  @Test
  void testGetCsvSkipHeader() {
    createDataElements(36);
    String response = GET("/dataElements.csv?skipHeader=true").content("text/csv");

    assertNotNull(response);
    String firstRow = getRowFromCsv(0, response);
    assertNotNull(firstRow);
    assertNotEquals("id,displayName", firstRow);
    assertTrue(firstRow.contains("DataElement0"));
  }

  @Test
  void testGetCsvWithFields() {
    createDataElements(36);
    String response = GET("/dataElements.csv?fields=id,name,domainType").content("text/csv");

    assertNotNull(response);
    String firstRow = getRowFromCsv(0, response);
    String secondRow = getRowFromCsv(1, response);
    assertEquals("id,name,domainType", firstRow);
    assertTrue(secondRow.contains("DataElement0"));
    assertTrue(secondRow.contains("AGGREGATE"));
  }

  @Test
  void testGetCsvWithPageSize() {
    createDataElements(36);
    String response = GET("/dataElements.csv?pageSize=10&skipHeader=true").content("text/csv");

    assertNotNull(response);
    int rowCount = getRowCountFromCsv(response);
    assertEquals(10, rowCount);
  }

  private void createDataElements(int count) {
    for (int i = 0; i < count; ++i) {
      DataElement dataElement = createDataElement(Character.forDigit(i, 36));
      dataElementService.addDataElement(dataElement);
    }
  }
}
