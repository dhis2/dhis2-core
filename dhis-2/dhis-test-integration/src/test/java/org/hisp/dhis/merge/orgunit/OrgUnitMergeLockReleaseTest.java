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
package org.hisp.dhis.merge.orgunit;

import static org.hisp.dhis.merge.orgunit.OrgUnitMergeServiceTest.isMergeLockFree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies that the merge lock taken by an org unit merge is released when the merge transaction
 * ends. Deliberately not {@code @Transactional}, so that each merge runs in, and commits or rolls
 * back, its own transaction.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class OrgUnitMergeLockReleaseTest extends PostgresIntegrationTestBase {

  @Autowired private OrgUnitMergeService service;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private DataSource dataSource;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  @BeforeEach
  void setUp() {
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    idObjectManager.save(ouC);
  }

  @Test
  @DisplayName("The merge lock is released when the merge transaction commits")
  void testMergeLockReleasedAfterCommit() throws ConflictException, SQLException {
    service.merge(
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build());

    assertTrue(isMergeLockFree(dataSource));
  }

  @Test
  @DisplayName("The merge lock is released when the merge transaction rolls back")
  void testMergeLockReleasedAfterRollback() throws SQLException {
    OrgUnitMergeRequest invalid =
        new OrgUnitMergeRequest.Builder().addSource(ouA).withTarget(ouA).build();

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.merge(invalid));
    // Validation runs after the merge lock is acquired
    assertEquals(ErrorCode.E1502, ex.getErrorCode());

    assertTrue(isMergeLockFree(dataSource));
  }
}
