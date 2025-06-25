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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the OU specific special Gist API parameters.
 *
 * @author Jan Bernitt
 */
class GistOrgUnitsControllerTest extends AbstractGistControllerTest {

  private String ou1, ou2, ou3a, ou3b, ou4a, ou4b;

  @BeforeEach
  void setUp() {
    ou1 = addOrganisationUnit("nameL1");
    ou2 = addOrganisationUnit("nameL2", ou1);
    ou3a = addOrganisationUnit("nameL3a", ou2);
    ou3b = addOrganisationUnit("nameL3b", ou2);
    ou4a = addOrganisationUnit("nameL4a", ou3a);
    ou4b = addOrganisationUnit("nameL4b", ou3b);
  }

  @Test
  void testOrgUnitsOffline() {
    assertEquals(
        List.of("nameL1", "nameL2", "nameL3a", "nameL4a", "nameL3b", "nameL4b"),
        toOrganisationUnitNames(GET("/api/organisationUnits/gist?orgUnitsOffline=true").content()));
    assertEquals(
        List.of("nameL1", "nameL2", "nameL3a", "nameL3b"),
        toOrganisationUnitNames(
            GET("/api/organisationUnits/gist?orgUnitsOffline=true&filter=level:le:3").content()));
  }

  @Test
  void testOrgUnitsTree() {
    assertEquals(
        List.of("nameL1", "nameL2", "nameL3a", "nameL4a"),
        toOrganisationUnitNames(
            GET(
                    "/api/organisationUnits/gist?fields=displayName&orgUnitsTree=true&filter=id:eq:{id}",
                    ou4a)
                .content()));
    assertEquals(
        List.of("nameL1", "nameL2", "nameL3b"),
        toOrganisationUnitNames(
            GET(
                    "/api/organisationUnits/gist?fields=displayName&orgUnitsTree=true&filter=id:eq:{id}",
                    ou3b)
                .content()));
  }
}
