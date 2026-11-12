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
package org.hisp.dhis.dxf2.metadata.collection;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hibernate.Hibernate;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Two requests that change the same item of the same unloaded collection at once must both succeed,
 * like the same requests made one after the other: the second one counts the item as ignored.
 *
 * <p>Deliberately NOT {@code @Transactional}: each request runs in its own committed transaction on
 * its own thread, which is what creates the contention.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CollectionItemsConcurrencyTest extends PostgresIntegrationTestBase {
  private static final String PROPERTY = "organisationUnits";

  @Autowired private CollectionService collectionService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  private String group;
  private String unit;
  private UserDetails admin;

  @BeforeEach
  void setUp() {
    OrganisationUnitGroup owner = createOrganisationUnitGroup('A');
    manager.save(owner);
    group = owner.getUid();
    OrganisationUnit item = createOrganisationUnit('A');
    manager.save(item);
    unit = item.getUid();
    admin = CurrentUserUtil.getCurrentUserDetails();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void concurrentChangeOfTheSameItemIsIgnoredByTheLaterRequest(boolean addition)
      throws Exception {
    if (!addition) {
      assertEquals(1, change(true).getStats().updated());
    }
    CountDownLatch firstChanged = new CountDownLatch(1);
    CountDownLatch firstMayCommit = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<TypeReport> first =
          pool.submit(
              () ->
                  inTransaction(
                      () -> {
                        TypeReport report = change(addition);
                        firstChanged.countDown();
                        assertTrue(firstMayCommit.await(1, TimeUnit.MINUTES));
                        return report;
                      }));
      assertTrue(firstChanged.await(1, TimeUnit.MINUTES), "first request did not change the item");

      Future<TypeReport> second = pool.submit(() -> inTransaction(() -> change(addition)));
      await()
          .atMost(1, TimeUnit.MINUTES)
          .until(() -> waitingForLock() > 0 || second.isDone());
      firstMayCommit.countDown();

      assertEquals(1, changed(first.get(1, TimeUnit.MINUTES), addition));
      TypeReport later = second.get(1, TimeUnit.MINUTES);
      assertEquals(0, changed(later, addition));
      assertEquals(1, later.getStats().ignored());
    } finally {
      firstMayCommit.countDown();
      pool.shutdownNow();
    }
    assertEquals(addition ? 1 : 0, members());
  }

  private <T> T inTransaction(TransactionalWork<T> work) {
    injectSecurityContextNoSettings(admin);
    return transactionTemplate.execute(
        status -> {
          try {
            return work.run();
          } catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        });
  }

  private TypeReport change(boolean addition) throws Exception {
    OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
    List<IdentifiableObject> items = List.of(new BaseIdentifiableObject(unit, "", ""));
    TypeReport report =
        addition
            ? collectionService.addCollectionItems(owner, PROPERTY, items)
            : collectionService.delCollectionItems(owner, PROPERTY, items);
    assertFalse(Hibernate.isInitialized(owner.getMembers()), "members must stay unloaded");
    return report;
  }

  private static int changed(TypeReport report, boolean addition) {
    return addition ? report.getStats().updated() : report.getStats().deleted();
  }

  /** Backends of this database that wait for a lock held by another transaction. */
  private int waitingForLock() {
    Integer waiting =
        jdbcTemplate.queryForObject(
            "select count(*) from pg_stat_activity where datname = current_database()"
                + " and wait_event_type = 'Lock'",
            Integer.class);
    return waiting == null ? 0 : waiting;
  }

  private int members() {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from orgunitgroupmembers m"
                + " join orgunitgroup g on g.orgunitgroupid = m.orgunitgroupid where g.uid = ?",
            Integer.class,
            group);
    return count == null ? 0 : count;
  }

  @FunctionalInterface
  private interface TransactionalWork<T> {
    T run() throws Exception;
  }
}
