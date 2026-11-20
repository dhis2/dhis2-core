/*
 * Copyright (c) 2004-2026, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deleting a user role or a program removes its {@code program_userroles} rows. The API cannot
 * write these rows ({@code Program.userRoles} is read only), so the tests insert one by SQL, as
 * left by older versions.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class ProgramUserRolesDeletionTest extends PostgresControllerIntegrationTestBase {

  @Autowired private JdbcTemplate jdbcTemplate;

  private String programUid;

  private String userRoleUid;

  private long programId;

  private long userRoleId;

  @BeforeEach
  void setUp() {
    userRoleUid = assertStatus(HttpStatus.CREATED, POST("/userRoles", "{'name':'Program role'}"));
    programUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs",
                "{'name':'Program A','shortName':'Program A','programType':'WITHOUT_REGISTRATION'}"));
    startNewRequestSession();

    programId =
        jdbcTemplate.queryForObject(
            "select programid from program where uid = ?", Long.class, programUid);
    userRoleId =
        jdbcTemplate.queryForObject(
            "select userroleid from userrole where uid = ?", Long.class, userRoleUid);
    jdbcTemplate.update(
        "insert into program_userroles (programid, userroleid) values (?, ?)",
        programId,
        userRoleId);
  }

  @Test
  void shouldRemoveProgramUserRolesWhenDeletingUserRole() {
    assertStatus(HttpStatus.OK, DELETE("/userRoles/" + userRoleUid));
    startNewRequestSession();

    assertEquals(0, countRows("userroleid", userRoleId));
  }

  @Test
  void shouldDeleteProgramWithProgramUserRoles() {
    assertStatus(HttpStatus.OK, DELETE("/programs/" + programUid));
    startNewRequestSession();

    assertEquals(0, countRows("programid", programId));
  }

  private int countRows(String column, long id) {
    return jdbcTemplate.queryForObject(
        "select count(*) from program_userroles where " + column + " = ?", Integer.class, id);
  }
}
