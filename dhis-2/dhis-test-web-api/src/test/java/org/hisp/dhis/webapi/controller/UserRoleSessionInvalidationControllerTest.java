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

import static org.awaitility.Awaitility.await;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the end-to-end asynchronous invalidation of role member sessions when the authorities of
 * a {@link UserRole} change: the {@code UserRoleBundleHook} publishes an event on commit and {@code
 * UserRoleSessionInvalidationListener} expires the members' sessions after the transaction has
 * committed, off the request thread.
 *
 * <p>Because the invalidation runs {@code AFTER_COMMIT} on a separate thread, the change has to be
 * committed (via {@link TestTransaction}) and the assertion has to poll ({@code await}) rather than
 * read synchronously. {@link TestInstance.Lifecycle#PER_CLASS} makes the framework empty the
 * database after the class, so the committed data does not leak into other tests.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRoleSessionInvalidationControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private SessionRegistry sessionRegistry;

  @Test
  void authorityChangeInvalidatesRoleMemberSessionsAsync() {
    UserDetails adminPrincipal = userService.createUserDetails(getAdminUser());

    UserRole role = createUserRole('B', "ALL");
    userService.addUserRole(role);

    // make the admin a member of the role
    PATCH(
            "/users/" + getAdminUid(),
            "[{'op':'add','path':'/userRoles','value':[{'id':'" + role.getUid() + "'}]}]")
        .content(OK);

    sessionRegistry.registerNewSession("session1", adminPrincipal);
    assertFalse(sessionRegistry.getAllSessions(adminPrincipal, false).isEmpty());

    // Change the role authorities and commit; the after-commit listener then invalidates the
    // member sessions asynchronously.
    TestTransaction.flagForCommit();
    PATCH("/userRoles/" + role.getUid(), "[{'op':'add','path':'/authorities','value':['NONE']}]")
        .content(OK);
    TestTransaction.end();

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> assertTrue(sessionRegistry.getAllSessions(adminPrincipal, false).isEmpty()));
  }
}
