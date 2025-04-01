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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.security.AuthoritiesController}.
 *
 * @author Jan Bernitt
 */
@Transactional
class AuthoritiesControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetAuthorities() {
    JsonArray systemAuthorities = GET("/authorities").content().getArray("systemAuthorities");
    assertTrue(systemAuthorities.size() > 10);
    List<String> listAuthNames =
        systemAuthorities.asList(JsonObject.class).stream()
            .map(o -> o.getString("name").string().toLowerCase())
            .toList();

    assertTrue(ListUtils.isSorted(listAuthNames, String::compareToIgnoreCase));
  }

  @Test
  void testGetAllAuthorities() {
    JsonArray systemAuthorities = GET("/authorities").content().getArray("systemAuthorities");
    assertTrue(systemAuthorities.size() > 10);

    // Get all authority ids/names
    List<String> listIds =
        systemAuthorities.asList(JsonObject.class).stream()
            .map(o -> o.getString("id").string())
            .toList();

    // Authorities from AppManager.getApps()
    assertTrue(listIds.contains("M_androidsettingsapp"));

    // Authorities from AppManager.BUNDLED_APPS
    // TODO
    // List<String> moduleAuths = listIds.stream().filter(id ->
    // id.startsWith("M_dhis-web-")).toList();
    // assertTrue(moduleAuths.size() > 4);

    // Authorities from schemaService
    assertTrue(listIds.contains("F_USER_ADD"));

    // System authorities fom Authorities enum
    assertTrue(listIds.contains("ALL"));
  }

  @Test
  void testNoDuplicateAuthorities() {
    JsonArray systemAuthorities = GET("/authorities").content().getArray("systemAuthorities");
    List<String> listIds =
        systemAuthorities.asList(JsonObject.class).stream()
            .map(o -> o.getString("id").string())
            .toList();
    Set<String> uniqueIds = new HashSet<>(listIds);
    assertEquals(
        uniqueIds.size(),
        listIds.size(),
        "Found duplicate authorities in response: List size="
            + listIds.size()
            + ", Unique size="
            + uniqueIds.size());
  }
}
