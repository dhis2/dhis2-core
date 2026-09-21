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
package org.hisp.dhis.association;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class AbstractOrganisationUnitAssociationsQueryBuilderTest {

  @BeforeEach
  void setUp() {
    CurrentUserUtil.injectUserInSecurityContext(
        UserDetails.empty().uid("tester12345").username("tester").isSuper(false).build());
  }

  @AfterEach
  void tearDown() {
    CurrentUserUtil.clearSecurityContext();
  }

  @Test
  void buildSqlQueryUsesPatharrayContainmentNotPathLike() {
    DataSetOrganisationUnitAssociationsQueryBuilder builder =
        new DataSetOrganisationUnitAssociationsQueryBuilder();
    User nonSuperUser = new User();

    String sql =
        builder.buildSqlQuery(
            Set.of("ds123456789"), Set.of("/AoCaBHBeXTs/qLiKWoddwFu/uid1234567"), nonSuperUser);

    assertTrue(
        sql.contains("ou.patharray @> ARRAY['AoCaBHBeXTs','qLiKWoddwFu','uid1234567']::varchar[]"),
        () -> "expected patharray containment condition in: " + sql);
    assertFalse(sql.contains("path like"), () -> "should not use path like anymore: " + sql);
  }
}
