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
package org.hisp.dhis.analytics;

import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.hisp.dhis.program.AnalyticsType.EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@see OrgUnitField} Tester.
 *
 * @author Jim Grace
 */
class OrgUnitFieldTest {
  private static final OrgUnitField DEFALT = new OrgUnitField(null);

  private static final OrgUnitField ATTRIB = new OrgUnitField("attributeId");

  private static final OrgUnitField REGIST = new OrgUnitField("REGISTRATION");

  private static final OrgUnitField ENROLL = new OrgUnitField("ENROLLMENT");

  private static final OrgUnitField OSTART = new OrgUnitField("OWNER_AT_START");

  private static final OrgUnitField OEND = new OrgUnitField("OWNER_AT_END");

  @Test
  void testIsJoinOrgUnitTables() {
    assertFalse(DEFALT.isJoinOrgUnitTables(EVENT));
    assertTrue(ATTRIB.isJoinOrgUnitTables(EVENT));
    assertTrue(REGIST.isJoinOrgUnitTables(EVENT));
    assertTrue(ENROLL.isJoinOrgUnitTables(EVENT));
    assertTrue(OSTART.isJoinOrgUnitTables(EVENT));
    assertTrue(OEND.isJoinOrgUnitTables(EVENT));

    assertFalse(DEFALT.isJoinOrgUnitTables(ENROLLMENT));
    assertTrue(ATTRIB.isJoinOrgUnitTables(ENROLLMENT));
    assertTrue(REGIST.isJoinOrgUnitTables(ENROLLMENT));
    assertFalse(ENROLL.isJoinOrgUnitTables(ENROLLMENT));
    assertFalse(OSTART.isJoinOrgUnitTables(ENROLLMENT));
    assertFalse(OEND.isJoinOrgUnitTables(ENROLLMENT));
  }

  @Test
  void testGetOrgUnitStructColForEvent() {
    assertEquals("ax.\"abc\"", DEFALT.getOrgUnitStructCol("abc", EVENT, false));
    assertEquals("ax.\"abc\"", DEFALT.getOrgUnitStructCol("abc", EVENT, true));
    assertEquals("ax.\"ou\"", DEFALT.getOrgUnitStructCol("ou", EVENT, false));
    assertEquals("ax.\"ou\"", DEFALT.getOrgUnitStructCol("ou", EVENT, true));

    assertEquals("ous.\"abc\"", ATTRIB.getOrgUnitStructCol("abc", EVENT, false));
    assertEquals("ous.\"abc\"", ATTRIB.getOrgUnitStructCol("abc", EVENT, true));
    assertEquals(
        "ous.\"organisationunituid\" as ou", ATTRIB.getOrgUnitStructCol("ou", EVENT, false));
    assertEquals("ous.\"organisationunituid\"", ATTRIB.getOrgUnitStructCol("ou", EVENT, true));

    assertEquals("ous.\"abc\"", REGIST.getOrgUnitStructCol("abc", EVENT, false));
    assertEquals("ous.\"abc\"", REGIST.getOrgUnitStructCol("abc", EVENT, true));
    assertEquals(
        "ous.\"organisationunituid\" as ou", REGIST.getOrgUnitStructCol("ou", EVENT, false));
    assertEquals("ous.\"organisationunituid\"", REGIST.getOrgUnitStructCol("ou", EVENT, true));

    assertEquals("ous.\"abc\"", ENROLL.getOrgUnitStructCol("abc", EVENT, false));
    assertEquals("ous.\"abc\"", ENROLL.getOrgUnitStructCol("abc", EVENT, true));
    assertEquals(
        "ous.\"organisationunituid\" as ou", ENROLL.getOrgUnitStructCol("ou", EVENT, false));
    assertEquals("ous.\"organisationunituid\"", ENROLL.getOrgUnitStructCol("ou", EVENT, true));

    assertEquals(
        "coalesce(own.\"abc\",ous.\"abc\") as abc",
        OSTART.getOrgUnitStructCol("abc", EVENT, false));
    assertEquals(
        "coalesce(own.\"abc\",ous.\"abc\")", OSTART.getOrgUnitStructCol("abc", EVENT, true));
    assertEquals(
        "coalesce(own.\"ou\",ous.\"organisationunituid\") as ou",
        OSTART.getOrgUnitStructCol("ou", EVENT, false));
    assertEquals(
        "coalesce(own.\"ou\",ous.\"organisationunituid\")",
        OSTART.getOrgUnitStructCol("ou", EVENT, true));

    assertEquals(
        "coalesce(own.\"abc\",ous.\"abc\") as abc", OEND.getOrgUnitStructCol("abc", EVENT, false));
    assertEquals("coalesce(own.\"abc\",ous.\"abc\")", OEND.getOrgUnitStructCol("abc", EVENT, true));
    assertEquals(
        "coalesce(own.\"ou\",ous.\"organisationunituid\") as ou",
        OEND.getOrgUnitStructCol("ou", EVENT, false));
    assertEquals(
        "coalesce(own.\"ou\",ous.\"organisationunituid\")",
        OEND.getOrgUnitStructCol("ou", EVENT, true));
  }

  @Test
  void testGetOrgUnitStructColForEnrollment() {
    assertEquals("ax.\"abc\"", DEFALT.getOrgUnitStructCol("abc", ENROLLMENT, false));
    assertEquals("ax.\"abc\"", DEFALT.getOrgUnitStructCol("abc", ENROLLMENT, true));
    assertEquals("ax.\"ou\"", DEFALT.getOrgUnitStructCol("ou", ENROLLMENT, false));
    assertEquals("ax.\"ou\"", DEFALT.getOrgUnitStructCol("ou", ENROLLMENT, true));

    assertEquals("ous.\"abc\"", ATTRIB.getOrgUnitStructCol("abc", ENROLLMENT, false));
    assertEquals("ous.\"abc\"", ATTRIB.getOrgUnitStructCol("abc", ENROLLMENT, true));
    assertEquals(
        "ous.\"organisationunituid\" as ou", ATTRIB.getOrgUnitStructCol("ou", ENROLLMENT, false));
    assertEquals("ous.\"organisationunituid\"", ATTRIB.getOrgUnitStructCol("ou", ENROLLMENT, true));

    assertEquals("ous.\"abc\"", REGIST.getOrgUnitStructCol("abc", ENROLLMENT, false));
    assertEquals("ous.\"abc\"", REGIST.getOrgUnitStructCol("abc", ENROLLMENT, true));
    assertEquals(
        "ous.\"organisationunituid\" as ou", REGIST.getOrgUnitStructCol("ou", ENROLLMENT, false));
    assertEquals("ous.\"organisationunituid\"", REGIST.getOrgUnitStructCol("ou", ENROLLMENT, true));

    assertEquals("ax.\"abc\"", ENROLL.getOrgUnitStructCol("abc", ENROLLMENT, false));
    assertEquals("ax.\"abc\"", ENROLL.getOrgUnitStructCol("abc", ENROLLMENT, true));
    assertEquals("ax.\"ou\"", ENROLL.getOrgUnitStructCol("ou", ENROLLMENT, false));
    assertEquals("ax.\"ou\"", ENROLL.getOrgUnitStructCol("ou", ENROLLMENT, true));

    assertEquals(
        "coalesce(own.\"abc\",ax.\"abc\") as abc",
        OSTART.getOrgUnitStructCol("abc", ENROLLMENT, false));
    assertEquals(
        "coalesce(own.\"abc\",ax.\"abc\")", OSTART.getOrgUnitStructCol("abc", ENROLLMENT, true));
    assertEquals(
        "coalesce(own.\"ou\",ax.\"ou\") as ou",
        OSTART.getOrgUnitStructCol("ou", ENROLLMENT, false));
    assertEquals(
        "coalesce(own.\"ou\",ax.\"ou\")", OSTART.getOrgUnitStructCol("ou", ENROLLMENT, true));

    assertEquals(
        "coalesce(own.\"abc\",ax.\"abc\") as abc",
        OEND.getOrgUnitStructCol("abc", ENROLLMENT, false));
    assertEquals(
        "coalesce(own.\"abc\",ax.\"abc\")", OEND.getOrgUnitStructCol("abc", ENROLLMENT, true));
    assertEquals(
        "coalesce(own.\"ou\",ax.\"ou\") as ou", OEND.getOrgUnitStructCol("ou", ENROLLMENT, false));
    assertEquals(
        "coalesce(own.\"ou\",ax.\"ou\")", OEND.getOrgUnitStructCol("ou", ENROLLMENT, true));
  }

  @Test
  void testGetOrgUnitGroupSetCol() {
    assertEquals("ax.\"uidlevel1\"", DEFALT.getOrgUnitLevelCol(1, EVENT));
    assertEquals("ax.\"uidlevel2\"", DEFALT.getOrgUnitLevelCol(2, EVENT));
    assertEquals("ax.\"uidlevel3\"", DEFALT.getOrgUnitLevelCol(3, EVENT));

    assertEquals("ous.\"uidlevel1\"", ATTRIB.getOrgUnitLevelCol(1, EVENT));
    assertEquals("ous.\"uidlevel1\"", REGIST.getOrgUnitLevelCol(1, EVENT));
    assertEquals("ous.\"uidlevel1\"", ENROLL.getOrgUnitLevelCol(1, EVENT));
    assertEquals(
        "coalesce(own.\"uidlevel1\",ous.\"uidlevel1\")", OSTART.getOrgUnitLevelCol(1, EVENT));
    assertEquals(
        "coalesce(own.\"uidlevel1\",ous.\"uidlevel1\")", OEND.getOrgUnitLevelCol(1, EVENT));

    assertEquals("ax.\"uidlevel1\"", DEFALT.getOrgUnitLevelCol(1, ENROLLMENT));
    assertEquals("ous.\"uidlevel1\"", ATTRIB.getOrgUnitLevelCol(1, ENROLLMENT));
    assertEquals("ous.\"uidlevel1\"", REGIST.getOrgUnitLevelCol(1, ENROLLMENT));
    assertEquals("ax.\"uidlevel1\"", ENROLL.getOrgUnitLevelCol(1, ENROLLMENT));
    assertEquals(
        "coalesce(own.\"uidlevel1\",ax.\"uidlevel1\")", OSTART.getOrgUnitLevelCol(1, ENROLLMENT));
    assertEquals(
        "coalesce(own.\"uidlevel1\",ax.\"uidlevel1\")", OEND.getOrgUnitLevelCol(1, ENROLLMENT));
  }

  @Test
  void testGetOrgUnitJoinCol() {
    assertEquals("ax.\"ou\"", DEFALT.getOrgUnitJoinCol(EVENT));
    assertEquals("ax.\"attributeId\"", ATTRIB.getOrgUnitJoinCol(EVENT));
    assertEquals("ax.\"registrationou\"", REGIST.getOrgUnitJoinCol(EVENT));
    assertEquals("ax.\"enrollmentou\"", ENROLL.getOrgUnitJoinCol(EVENT));
    assertEquals("ax.\"enrollmentou\"", OSTART.getOrgUnitJoinCol(EVENT));
    assertEquals("ax.\"enrollmentou\"", OEND.getOrgUnitJoinCol(EVENT));

    assertEquals("ax.\"ou\"", DEFALT.getOrgUnitJoinCol(ENROLLMENT));
    assertEquals("ax.\"attributeId\"", ATTRIB.getOrgUnitJoinCol(ENROLLMENT));
    assertEquals("ax.\"registrationou\"", REGIST.getOrgUnitJoinCol(ENROLLMENT));
    assertEquals("ax.\"ou\"", ENROLL.getOrgUnitJoinCol(ENROLLMENT));
    assertEquals("ax.\"ou\"", OSTART.getOrgUnitJoinCol(ENROLLMENT));
    assertEquals("ax.\"ou\"", OEND.getOrgUnitJoinCol(ENROLLMENT));
  }

  @Test
  void testGetOrgUnitWhereCol() {
    assertEquals("ax.\"ou\"", DEFALT.getOrgUnitWhereCol(EVENT));
    assertEquals("ax.\"attributeId\"", ATTRIB.getOrgUnitWhereCol(EVENT));
    assertEquals("ax.\"registrationou\"", REGIST.getOrgUnitWhereCol(EVENT));
    assertEquals("ax.\"enrollmentou\"", ENROLL.getOrgUnitWhereCol(EVENT));
    assertEquals(
        "coalesce(own.\"enrollmentou\",ax.\"enrollmentou\")", OSTART.getOrgUnitWhereCol(EVENT));
    assertEquals(
        "coalesce(own.\"enrollmentou\",ax.\"enrollmentou\")", OEND.getOrgUnitWhereCol(EVENT));

    assertEquals("ax.\"ou\"", DEFALT.getOrgUnitWhereCol(ENROLLMENT));
    assertEquals("ax.\"attributeId\"", ATTRIB.getOrgUnitWhereCol(ENROLLMENT));
    assertEquals("ax.\"registrationou\"", REGIST.getOrgUnitWhereCol(ENROLLMENT));
    assertEquals("ax.\"ou\"", ENROLL.getOrgUnitWhereCol(ENROLLMENT));
    assertEquals("coalesce(own.\"ou\",ax.\"ou\")", OSTART.getOrgUnitWhereCol(ENROLLMENT));
    assertEquals("coalesce(own.\"ou\",ax.\"ou\")", OEND.getOrgUnitWhereCol(ENROLLMENT));
  }
}
