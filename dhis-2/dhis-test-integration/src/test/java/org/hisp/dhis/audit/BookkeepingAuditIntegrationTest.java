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
package org.hisp.dhis.audit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.config.PostgresDhisConfigurationProvider;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Updates that only touch bookkeeping properties (a login, lastUpdated) are not audited, real
 * changes are.
 *
 * <p>Audits are written asynchronously after the commit, so absence is asserted against a marker:
 * each test changes the first name before and after the step under test and waits for the audit of
 * that change. The step under test is audited only if the number of UPDATE audits grows by more
 * than one between the two markers.
 *
 * @author Morten (Morty) <netroms@gmail.com>
 */
@ActiveProfiles(profiles = {"test-audit"})
@ContextConfiguration(classes = {BookkeepingAuditIntegrationTest.DhisConfig.class})
class BookkeepingAuditIntegrationTest extends PostgresIntegrationTestBase {

  static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      Properties override = new Properties();
      override.put("system.audit.enabled", "true");
      override.put("audit.database", "true");
      override.put("audit.metadata", "CREATE;UPDATE");
      PostgresDhisConfigurationProvider postgresDhisConfigurationProvider =
          new PostgresDhisConfigurationProvider(null);
      postgresDhisConfigurationProvider.addProperties(override);
      return postgresDhisConfigurationProvider;
    }
  }

  private static final int TIMEOUT = 10;

  @Autowired private AuditService auditService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  private User user;

  @BeforeEach
  void setUp() {
    user = createAndAddUser("bookkeeper");
  }

  @Test
  void testLoginIsNotAuditedAndDoesNotChangeLastUpdated() {
    int updateAudits = changeFirstNameAndAwaitAudit("marker-before");
    Map<String, Object> before = lastUpdatedAndLastLogin();
    assertNull(before.get("lastlogin"));

    userService.setLastLogin(user.getUsername());

    Map<String, Object> after = lastUpdatedAndLastLogin();
    assertNotNull(after.get("lastlogin"));
    assertEquals(before.get("lastupdated"), after.get("lastupdated"));
    assertEquals(updateAudits + 1, changeFirstNameAndAwaitAudit("marker-after"));
  }

  static Stream<Named<Consumer<User>>> bookkeepingOnlyChanges() {
    return Stream.of(
        Named.of("only lastUpdated", u -> {}),
        Named.of("lastLogin", u -> u.setLastLogin(new Date())),
        Named.of("lastCheckedInterpretations", u -> u.setLastCheckedInterpretations(new Date())));
  }

  @ParameterizedTest
  @MethodSource("bookkeepingOnlyChanges")
  void testUpdateOfOnlyBookkeepingPropertiesIsNotAudited(Consumer<User> change) {
    int updateAudits = changeFirstNameAndAwaitAudit("marker-before");

    updateUser(change);

    assertEquals(updateAudits + 1, changeFirstNameAndAwaitAudit("marker-after"));
  }

  @Test
  void testCollectionOnlyChangeIsAudited() {
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);
    int updateAudits = changeFirstNameAndAwaitAudit("marker-before");

    updateUser(u -> u.addOrganisationUnit(orgUnit));

    await()
        .atMost(TIMEOUT, TimeUnit.SECONDS)
        .until(() -> hasUpdateAuditContaining(orgUnit.getUid()));
    assertEquals(updateAudits + 1, auditService.countAudits(updateAuditsOfUser()));
  }

  private void updateUser(Consumer<User> change) {
    transactionTemplate.execute(
        status -> {
          User persisted = userService.getUser(user.getUid());
          change.accept(persisted);
          userService.updateUser(persisted);
          return null;
        });
  }

  /** Makes a real change and returns the number of UPDATE audits once it has been audited. */
  private int changeFirstNameAndAwaitAudit(String firstName) {
    updateUser(u -> u.setFirstName(firstName));
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> hasUpdateAuditContaining(firstName));
    return auditService.countAudits(updateAuditsOfUser());
  }

  private boolean hasUpdateAuditContaining(String value) {
    return auditService.getAudits(updateAuditsOfUser()).stream()
        .anyMatch(audit -> audit.getData().contains(value));
  }

  private AuditQuery updateAuditsOfUser() {
    return AuditQuery.builder()
        .uid(Set.of(user.getUid()))
        .auditType(Set.of(AuditType.UPDATE))
        .build();
  }

  private Map<String, Object> lastUpdatedAndLastLogin() {
    return jdbcTemplate.queryForMap(
        "select lastupdated, lastlogin from userinfo where uid = ?", user.getUid());
  }
}
