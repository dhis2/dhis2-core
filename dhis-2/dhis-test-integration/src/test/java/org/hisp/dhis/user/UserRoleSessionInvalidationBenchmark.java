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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.hooks.UserRoleBundleHook;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Measures what an authority change on a user role with a large membership costs, in JDBC
 * statements and in wall time inside the updating transaction — the two numbers from the Uganda
 * trace (UGANDA-22).
 *
 * <p>Not a test; it asserts only that the run did the work it claims to have done. It is skipped
 * unless {@code -Ddhis.benchmark=true} is passed, because building the membership takes minutes.
 *
 * <pre>
 * mvn -o -pl dhis-test-integration test -Dtest=UserRoleSessionInvalidationBenchmark \
 *     -Ddhis.benchmark=true -Ddhis.benchmark.members=10000
 * </pre>
 *
 * <p>The measured call is {@link UserRoleBundleHook#postUpdate}, the entry point of the fan-out, so
 * the same source measures the code before and after the fix.
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@EnabledIfSystemProperty(named = "dhis.benchmark", matches = "true")
@Timeout(value = 30, unit = TimeUnit.MINUTES)
class UserRoleSessionInvalidationBenchmark extends PostgresIntegrationTestBase {

  /** Uganda's role has 23,867 members. */
  private static final int MEMBERS = Integer.getInteger("dhis.benchmark.members", 10_000);

  /** Members given a live session, so expiry does real work for a realistic fraction. */
  private static final int SESSIONS = 100;

  @Autowired
  @Qualifier("org.hisp.dhis.user.UserStore")
  private UserStore userStore;

  @Autowired private UserRoleStore userRoleStore;
  @Autowired private SessionRegistry sessionRegistry;

  /**
   * The three org unit lookups in {@code DefaultUserService.createUserDetails} go through {@code
   * JdbcTemplate}, so Hibernate's statement counter never sees them — that is the whole gap between
   * the 2.02 statements per member Hibernate reports and the 5.04 the production trace measured.
   * Spying the service counts them exactly: each call is one {@code queryForList}, one statement.
   */
  @MockitoSpyBean private OrganisationUnitService orgUnitService;

  @BeforeAll
  static void announce() {
    System.out.printf("%n=== UGANDA-22 benchmark, %d members ===%n", MEMBERS);
  }

  @Test
  void measureAuthorityChangeOnALargeRole() {
    UserRole role = createUserRole('Z');
    role.getAuthorities().add("F_USER_ADD");
    userRoleStore.save(role);

    List<String> usernames = new ArrayList<>(MEMBERS);
    long setupStart = System.nanoTime();
    for (int i = 0; i < MEMBERS; i++) {
      String username = "benchuser" + i;
      User user = new User();
      user.setAutoFields();
      user.setUsername(username);
      user.setFirstName("Bench");
      user.setSurname("User" + i);
      user.setPassword("$2a$10$AAAAAAAAAAAAAAAAAAAAAOxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      user.getUserRoles().add(role);
      userStore.save(user, false);
      usernames.add(username);
      if (i % 500 == 499) {
        entityManager.flush();
        entityManager.clear();
        role = userRoleStore.getByUid(role.getUid());
      }
    }
    entityManager.flush();
    entityManager.clear();
    System.out.printf(
        "setup: %d members in %.1f s%n", MEMBERS, (System.nanoTime() - setupStart) / 1e9);

    for (int i = 0; i < SESSIONS; i++) {
      sessionRegistry.registerNewSession("bench-session-" + i, principalFor(usernames.get(i)));
    }

    // Re-read the role so its members collection is uninitialised, as it is in a real import.
    UserRole reloaded = userRoleStore.getByUid(role.getUid());

    ObjectBundle bundle = mock(ObjectBundle.class);
    when(bundle.getExtras(reloaded, UserRoleBundleHook.INVALIDATE_SESSION_KEY))
        .thenReturn(Boolean.TRUE);

    Statistics stats =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();
    clearInvocations(orgUnitService);

    UserRoleBundleHook hook = new UserRoleBundleHook(userService);

    long start = System.nanoTime();
    hook.postUpdate(reloaded, bundle);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    long hibernateStatements = stats.getPrepareStatementCount();
    long orgUnitStatements =
        mockingDetails(orgUnitService).getInvocations().stream()
            .filter(i -> i.getMethod().getName().endsWith("OrganisationUnitsUidsByUser"))
            .count();
    long statements = hibernateStatements + orgUnitStatements;
    long entities = stats.getEntityLoadCount();

    System.out.printf(
        "%n=== RESULT: members=%d statements=%d (hibernate=%d orgUnitJdbc=%d) "
            + "entitiesLoaded=%d elapsedMs=%d statementsPerMember=%.3f ===%n",
        MEMBERS,
        statements,
        hibernateStatements,
        orgUnitStatements,
        entities,
        elapsedMs,
        statements / (double) MEMBERS);

    // The run is only meaningful if the sessions really were expired.
    for (int i = 0; i < SESSIONS; i++) {
      assertEquals(
          true,
          sessionRegistry.getSessionInformation("bench-session-" + i).isExpired(),
          "session " + i + " should have been expired");
    }
  }

  /** The principal a real login registers. */
  private UserDetails principalFor(String username) {
    User user = new User();
    user.setAutoFields();
    user.setUsername(username);
    return UserDetails.createUserDetails(user, true, true, null, null, null);
  }
}
