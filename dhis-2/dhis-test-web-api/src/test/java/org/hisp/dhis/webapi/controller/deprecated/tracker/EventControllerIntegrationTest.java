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
package org.hisp.dhis.webapi.controller.deprecated.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Viet Nguyen
 */
class EventControllerIntegrationTest extends DhisControllerIntegrationTest {
  @BeforeEach
  public void setUp() {
    OrganisationUnit organisationUnit = createOrganisationUnit("a");
    organisationUnit.setUid("ZiMBqH865GV");
    manager.save(organisationUnit);
    Program program = createProgram('A');
    program.getOrganisationUnits().add(organisationUnit);
    program.setUid("q04UBOqq3rp");
    manager.save(program);
    ProgramStage programStage = createProgramStage('A', program);
    programStage.setUid("pSllsjpfLH2");
    program.getProgramStages().add(programStage);
    manager.save(programStage);
  }

  @Test
  void testQueryCsv() {
    HttpResponse res =
        GET(
            "/events/query.csv?format=csv&orgUnit=ZiMBqH865GV&program=q04UBOqq3rp&programStage=pSllsjpfLH2");
    assertEquals(HttpStatus.OK, res.status());
    assertEquals(ContextUtils.CONTENT_TYPE_TEXT_CSV, res.header("Content-Type"));
  }

  @Test
  void testGetCsvZip() {
    HttpResponse res =
        GET(
            "/events.csv.zip?attachment=events.csv.zip&orgUnit=ZiMBqH865GV&program=q04UBOqq3rp&programStage=pSllsjpfLH2");
    assertEquals(HttpStatus.OK, res.status());
    assertEquals("application/csv+zip", res.header("Content-Type"));
    assertEquals("attachment; filename=events.csv.zip", res.header("Content-Disposition"));
  }

  @Test
  void testGetXml() {
    HttpResponse res =
        GET(
            "/events.xml?attachment=events.xml&orgUnit=ZiMBqH865GV&program=q04UBOqq3rp&programStage=pSllsjpfLH2");
    assertEquals(HttpStatus.OK, res.status());
    assertEquals("application/xml", res.header("Content-Type"));
    assertEquals("attachment; filename=events.xml", res.header("Content-Disposition"));
  }

  @Test
  void testGetXmlZip() {
    HttpResponse res =
        GET(
            "/events.xml.zip?attachment=events.xml.zip&orgUnit=ZiMBqH865GV&program=q04UBOqq3rp&programStage=pSllsjpfLH2");
    assertEquals(HttpStatus.OK, res.status());
    assertEquals("application/xml+zip", res.header("Content-Type"));
    assertEquals("attachment; filename=events.xml.zip", res.header("Content-Disposition"));
  }

  @Test
  void testGetJsonZip() {
    HttpResponse res =
        GET(
            "/events.json.zip?attachment=events.json.zip&orgUnit=ZiMBqH865GV&program=q04UBOqq3rp&programStage=pSllsjpfLH2");
    assertEquals(HttpStatus.OK, res.status());
    assertEquals("application/json+zip", res.header("Content-Type"));
    assertEquals("attachment; filename=events.json.zip", res.header("Content-Disposition"));
  }

  @Test
  void testSkipPaging() {
    JsonObject res = GET("/events.json?ouMode=ALL&skipPaging=true").content(HttpStatus.OK);
    assertFalse(res.get("pager").exists());

    res = GET("/events.json?ouMode=ALL&skipPaging=false").content(HttpStatus.OK);
    assertTrue(res.get("pager").exists());
  }
}
