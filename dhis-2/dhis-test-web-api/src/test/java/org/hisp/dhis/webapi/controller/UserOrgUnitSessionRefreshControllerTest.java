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

import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that users are marked as changed after commit when their organisation units change, so
 * that their sessions refresh the organisation units held by the session principal.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserOrgUnitSessionRefreshControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private UserDetailsChangeTracker userDetailsChangeTracker;

  @Autowired
  @Qualifier("userDetailsChangeTaskExecutor")
  private ThreadPoolTaskExecutor userDetailsChangeExecutor;

  @ParameterizedTest
  @ValueSource(
      strings = {"organisationUnits", "dataViewOrganisationUnits", "teiSearchOrganisationUnits"})
  void orgUnitChangeMarksUserAsChangedAfterCommit(String property) {
    User user = createUserWithAuth("ou" + property.toLowerCase());
    OrganisationUnit orgUnit = createOrganisationUnit(property.charAt(0));
    manager.save(orgUnit);

    PATCH(
            "/users/" + user.getUid(),
            "[{'op':'add','path':'/%s','value':[{'id':'%s'}]}]"
                .formatted(property, orgUnit.getUid()))
        .content(OK);
    assertNull(changeToken(user), "the change must only be visible after the commit");
    commit();

    assertNotNull(changeToken(user));
  }

  @Test
  void otherChangesDoNotMarkUserAsChanged() {
    User user = createUserWithAuth("ouunchanged");

    PATCH(
            "/users/" + user.getUid(),
            "[{'op':'replace','path':'/email','value':'unchanged@dhis2.org'}]")
        .content(OK);
    commit();

    assertNull(changeToken(user));
  }

  private String changeToken(User user) {
    return userDetailsChangeTracker.getChangeToken(user.getUsername());
  }

  /** Commits the test transaction and waits for the after-commit marking to have run. */
  private void commit() {
    TestTransaction.flagForCommit();
    TestTransaction.end();
    try {
      userDetailsChangeExecutor.submit(() -> {}).get(10, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new IllegalStateException("User details changes were not processed", ex);
    }
  }
}
