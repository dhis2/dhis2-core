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
package org.hisp.dhis.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Deliberately not transactional: a test transaction would hide the rollback-only bug when an
 * invitation cannot be deleted. IntegrationTestBase binds the session and empties the database
 * after each test. The 2.41 controller test base is transactional, so this regression exercises the
 * service.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class MaintenanceExpiredInvitationsTest extends IntegrationTestBase {

  @Autowired private MaintenanceService maintenanceService;

  @Autowired private DataElementService dataElementService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testRemoveExpiredInvitations_VetoedInvitationDoesNotBlockOthers() {
    String removable = invite("removable");
    String vetoed = invite("vetoed");
    DataElement dataElement = createDataElement('A');
    dataElementService.addDataElement(dataElement);
    jdbcTemplate.update(
        "update dataelement set userid = (select userinfoid from userinfo where uid = ?)"
            + " where uid = ?",
        vetoed,
        dataElement.getUid());
    entityManager.clear();

    assertEquals(1, maintenanceService.removeExpiredInvitations());
    assertUserCount(removable, 0);
    assertUserCount(vetoed, 1);
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, false, 0",
    "false, true, false, 1",
    "true, false, false, 1",
    "true, true, true, 1"
  })
  void testRemoveExpiredInvitations_OnlyRemovesUnusedExpiredInvitations(
      boolean expired, boolean invitation, boolean loggedIn, int expectedSubjectCount) {
    String control = invite("control");
    String subject = invite("subject");
    jdbcTemplate.update(
        "update userinfo set restoreexpiry = now() + (? * interval '2 days'), invitation = ?,"
            + " lastlogin = case when ? then now() - interval '1 day' else null end where uid = ?",
        expired ? -1 : 1,
        invitation,
        loggedIn,
        subject);
    entityManager.clear();

    assertEquals(2 - expectedSubjectCount, maintenanceService.removeExpiredInvitations());
    assertUserCount(control, 0);
    assertUserCount(subject, expectedSubjectCount);
  }

  private String invite(String username) {
    String uid = createAndAddUser(username).getUid();
    jdbcTemplate.update(
        "update userinfo set invitation = true, restoretoken = ?,"
            + " restoreexpiry = now() - interval '2 days', lastlogin = null where uid = ?",
        "invitation-" + uid,
        uid);
    return uid;
  }

  private void assertUserCount(String uid, int expectedCount) {
    assertEquals(
        expectedCount,
        jdbcTemplate.queryForObject(
            "select count(*) from userinfo where uid = ?", Integer.class, uid));
  }
}
