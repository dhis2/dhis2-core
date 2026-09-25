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

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Deliberately not transactional: a test transaction would hide the rollback-only bug when an
 * invitation cannot be deleted. SpringIntegrationTestExtension empties the database after each
 * non-transactional test, so no manual cleanup is needed.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class MaintenanceExpiredInvitationsControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testRemoveExpiredInvitations_VetoedInvitationDoesNotBlockOthers() {
    String role = createRole();
    String removable = invite("removable", role);
    String vetoed = invite("vetoed", role);
    expire(removable);
    expire(vetoed);
    String categoryCombo =
        jdbcTemplate.queryForObject(
            "select uid from categorycombo where name = 'default'", String.class);
    String dataElement =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{'name':'Owned element','shortName':'Owned element','valueType':'INTEGER',"
                    + "'aggregationType':'SUM','domainType':'AGGREGATE','categoryCombo':{'id':'"
                    + categoryCombo
                    + "'}}"));
    jdbcTemplate.update(
        "update dataelement set userid = (select userinfoid from userinfo where uid = ?)"
            + " where uid = ?",
        vetoed,
        dataElement);
    entityManager.clear();

    assertStatus(HttpStatus.OK, POST("/maintenance?expiredInvitationsClear=true"));
    assertStatus(HttpStatus.NOT_FOUND, GET("/users/" + removable));
    assertStatus(HttpStatus.OK, GET("/users/" + vetoed));
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, false, NOT_FOUND",
    "false, true, false, OK",
    "true, false, false, OK",
    "true, true, true, OK"
  })
  void testRemoveExpiredInvitations_OnlyRemovesUnusedExpiredInvitations(
      boolean expired, boolean invitation, boolean loggedIn, HttpStatus expectedStatus) {
    String role = createRole();
    String control = invite("control", role);
    String subject = invite("subject", role);
    expire(control);
    jdbcTemplate.update(
        "update userinfo set restoreexpiry = now() + (? * interval '2 days'), invitation = ?,"
            + " lastlogin = case when ? then now() - interval '1 day' else null end where uid = ?",
        expired ? -1 : 1,
        invitation,
        loggedIn,
        subject);
    entityManager.clear();

    assertStatus(HttpStatus.OK, POST("/maintenance?expiredInvitationsClear=true"));
    assertStatus(HttpStatus.NOT_FOUND, GET("/users/" + control));
    assertStatus(expectedStatus, GET("/users/" + subject));
  }

  private String createRole() {
    return assertStatus(
        HttpStatus.CREATED, POST("/userRoles", "{'name':'Invitee role','authorities':[]}"));
  }

  private String invite(String username, String role) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/users/invite",
            "{'surname':'Invitee','firstName':'Test','email':'"
                + username
                + "@example.com','username':'"
                + username
                + "','userRoles':[{'id':'"
                + role
                + "'}]}"));
  }

  private void expire(String uid) {
    jdbcTemplate.update(
        "update userinfo set restoreexpiry = now() - interval '2 days' where uid = ?", uid);
  }
}
