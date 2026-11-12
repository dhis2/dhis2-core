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

import static org.awaitility.Awaitility.await;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.event.CollectionMembershipChangedEvent;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.collection.CollectionService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.FeedbackException;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalApplicationListenerAdapter;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checks unloaded owning sets without the controller's global cache eviction masking stale entries.
 * Every operation runs in a fresh persistence context; PostgreSQL enables the second-level cache.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CollectionItemsSecondLevelCacheTest extends PostgresControllerIntegrationTestBase {
  private static final String MEMBERS_ROLE = OrganisationUnitGroup.class.getName() + ".members";
  private static final String GROUPS_ROLE = OrganisationUnit.class.getName() + ".groups";

  @Autowired private IdentifiableObjectManager manager;
  @Autowired private CollectionService collectionService;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private SessionFactory sessionFactory;
  @Autowired private DataSource dataSource;
  @Autowired private SchemaService schemaService;

  @Test
  void mergeUnloadedGroupMembersPreservesCountsAndBothCachedSides() {
    String a = organisationUnit("Delta A");
    String b = organisationUnit("Delta B");
    String c = organisationUnit("Delta C");
    String absent = organisationUnit("Delta absent");
    String group =
        assertStatus(
            HttpStatus.CREATED,
            POST("/organisationUnitGroups/", "{'name':'Delta group','shortName':'Delta'}"));
    transaction(
        () -> {
          OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
          owner.getMembers().add(manager.get(OrganisationUnit.class, a));
          owner.getMembers().add(manager.get(OrganisationUnit.class, b));
          manager.update(owner);
          return null;
        });
    warmGroupAndInverseCaches(group, a, c);

    TypeReport report =
        transaction(
            () -> {
              OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
              assertFalse(Hibernate.isInitialized(owner.getMembers()));
              var lastUpdated = owner.getLastUpdated();
              var lastUpdatedBy = owner.getLastUpdatedBy();
              TypeReport result =
                  merge(
                      owner,
                      "organisationUnits",
                      List.of(b, c, "Unknown0001"),
                      List.of(a, absent, "Unknown0002"));
              assertFalse(Hibernate.isInitialized(owner.getMembers()));
              assertEquals(lastUpdated, owner.getLastUpdated());
              assertEquals(lastUpdatedBy, owner.getLastUpdatedBy());
              assertEquals(Set.of(b, c), uids(owner.getMembers()));
              return result;
            });
    assertEquals(1, report.getStats().updated());
    assertEquals(1, report.getStats().deleted());
    assertEquals(2, report.getStats().ignored());
    // Unknown UIDs are silently skipped, as in the existing collection endpoint.
    assertFalse(report.hasObjectReports());
    assertGroupMembers(group, b, c);
    assertGroupMembers(group, b, c);
    transaction(
        () -> {
          assertFalse(uids(manager.get(OrganisationUnit.class, a).getGroups()).contains(group));
          assertTrue(uids(manager.get(OrganisationUnit.class, c).getGroups()).contains(group));
          return null;
        });
  }

  @Test
  void dataSetSourcesUseTheSameMechanismAndKeepLoadedInverseConsistent() {
    String ou = organisationUnit("Delta dataset OU");
    String ds =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'Delta dataset','shortName':'DeltaDS','periodType':'Monthly'}"));
    transaction(
        () -> {
          manager.get(DataSet.class, ds).getSources().size();
          manager.get(OrganisationUnit.class, ou).getDataSets().size();
          return null;
        });
    transaction(
        () -> {
          DataSet owner = manager.get(DataSet.class, ds);
          OrganisationUnit item = manager.get(OrganisationUnit.class, ou);
          assertTrue(item.getDataSets().isEmpty());
          TypeReport report = merge(owner, "organisationUnits", List.of(ou), List.of());
          assertEquals(1, report.getStats().updated());
          assertFalse(Hibernate.isInitialized(owner.getSources()));
          assertTrue(item.getDataSets().contains(owner));
          return null;
        });
    transaction(
        () -> {
          assertEquals(Set.of(ou), uids(manager.get(DataSet.class, ds).getSources()));
          assertEquals(Set.of(ds), uids(manager.get(OrganisationUnit.class, ou).getDataSets()));
          return null;
        });
  }

  @Test
  void initializedOwnerUsesInMemoryPathAndRollbackDoesNotPublishMembership() {
    String ou = organisationUnit("Delta rollback OU");
    String group =
        assertStatus(
            HttpStatus.CREATED,
            POST("/organisationUnitGroups/", "{'name':'Delta rollback','shortName':'DeltaRB'}"));
    transaction(
        () -> {
          OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
          assertTrue(owner.getMembers().isEmpty());
          assertEquals(
              1, merge(owner, "organisationUnits", List.of(ou), List.of()).getStats().updated());
          assertEquals(Set.of(ou), uids(owner.getMembers()));
          return null;
        });
    // The Redis publisher forwards membership changes once their transaction has committed.
    List<CollectionMembershipChangedEvent> committed = new CopyOnWriteArrayList<>();
    ApplicationListener<ApplicationEvent> afterCommit =
        new TransactionalApplicationListenerAdapter<>(
            event -> {
              if (event instanceof PayloadApplicationEvent<?> published
                  && published.getPayload() instanceof CollectionMembershipChangedEvent change) {
                committed.add(change);
              }
            });
    ApplicationEventMulticaster multicaster =
        webApplicationContext.getBean(
            AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
            ApplicationEventMulticaster.class);
    multicaster.addApplicationListener(afterCommit);
    try {
      new TransactionTemplate(transactionManager)
          .executeWithoutResult(
              status -> {
                entityManager.clear();
                OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
                assertFalse(Hibernate.isInitialized(owner.getMembers()));
                assertEquals(
                    1,
                    merge(owner, "organisationUnits", List.of(), List.of(ou)).getStats().deleted());
                assertFalse(Hibernate.isInitialized(owner.getMembers()));
                status.setRollbackOnly();
              });
      assertEquals(List.of(), committed);
      assertGroupMembers(group, ou);

      // the same deletion, committed, does publish the owner's set and the item's inverse set
      transaction(
          () ->
              merge(
                  manager.get(OrganisationUnitGroup.class, group),
                  "organisationUnits",
                  List.of(),
                  List.of(ou)));
      assertEquals(
          Set.of(MEMBERS_ROLE, GROUPS_ROLE),
          committed.stream()
              .map(CollectionMembershipChangedEvent::role)
              .collect(Collectors.toSet()));
    } finally {
      multicaster.removeApplicationListener(afterCommit);
    }
    assertGroupMembers(group);
  }

  /**
   * No collection is bounded by a {@code PropertyRange} today, so this test bounds the schema
   * property itself. The size is then validated with a count, and a rejected change must not leave
   * behind the rows it has already written.
   */
  @Test
  void boundedUnloadedCollectionIsValidatedByCountAndRejectionRollsBack() {
    String member = organisationUnit("Delta bounded A");
    String rejected = organisationUnit("Delta bounded B");
    String group =
        assertStatus(
            HttpStatus.CREATED,
            POST("/organisationUnitGroups/", "{'name':'Delta bounded','shortName':'DeltaBD'}"));
    transaction(
        () -> {
          OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
          owner.getMembers().add(manager.get(OrganisationUnit.class, member));
          manager.update(owner);
          return null;
        });
    Property members =
        schemaService.getSchema(OrganisationUnitGroup.class).getProperty("organisationUnits");
    Double max = members.getMax();
    members.setMax(1d);
    try {
      new TransactionTemplate(transactionManager)
          .executeWithoutResult(
              status -> {
                entityManager.clear();
                OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
                BadRequestException error =
                    assertThrows(
                        BadRequestException.class,
                        () ->
                            collectionService.mergeCollectionItems(
                                owner, "organisationUnits", items(List.of(rejected), List.of())));
                assertEquals(ErrorCode.E4007, error.getErrorReports().get(0).getErrorCode());
                assertFalse(Hibernate.isInitialized(owner.getMembers()));
                status.setRollbackOnly();
              });
    } finally {
      members.setMax(max);
    }
    assertGroupMembers(group, member);
  }

  /**
   * A concurrent transaction adds the same member first. Its row's foreign key holds a lock on the
   * group's row, so the merge waits on its own lock of that row until the commit. The merge then
   * reads the committed row: it counts the member as ignored, as a later request would, and still
   * applies its deletion instead of violating the join table's key.
   */
  @Test
  void concurrentAdditionIsIgnoredAndTheMergeAppliesItsDeletion() throws Exception {
    String removed = organisationUnit("Delta removed");
    String contested = organisationUnit("Delta contested");
    String group =
        assertStatus(
            HttpStatus.CREATED,
            POST("/organisationUnitGroups/", "{'name':'Delta conflict','shortName':'DeltaCF'}"));
    long groupId =
        transaction(
            () -> {
              OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
              owner.getMembers().add(manager.get(OrganisationUnit.class, removed));
              manager.update(owner);
              return owner.getId();
            });
    long contestedId = transaction(() -> manager.get(OrganisationUnit.class, contested).getId());

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection concurrent = dataSource.getConnection()) {
      concurrent.setAutoCommit(false);
      try (PreparedStatement insert =
          concurrent.prepareStatement(
              "insert into orgunitgroupmembers (orgunitgroupid, organisationunitid) values (?, ?)")) {
        insert.setLong(1, groupId);
        insert.setLong(2, contestedId);
        insert.executeUpdate();
      }
      Future<?> commit =
          executor.submit(
              () -> {
                try {
                  awaitWaitingTransaction(concurrent);
                } finally {
                  concurrent.commit();
                }
                return null;
              });

      TypeReport report =
          transaction(
              () ->
                  merge(
                      manager.get(OrganisationUnitGroup.class, group),
                      "organisationUnits",
                      List.of(contested),
                      List.of(removed)));
      commit.get();
      assertEquals(0, report.getStats().updated());
      assertEquals(1, report.getStats().deleted());
      assertEquals(1, report.getStats().ignored());
    } finally {
      executor.shutdownNow();
    }
    assertGroupMembers(group, contested);
  }

  /** Returns once another connection waits for the transaction of the given connection to end. */
  private static void awaitWaitingTransaction(Connection connection) throws SQLException {
    String sql =
        "select count(*) from pg_locks where not granted and locktype = 'transactionid'"
            + " and transactionid ="
            + " (select backend_xid from pg_stat_activity where pid = pg_backend_pid())";
    try (PreparedStatement blocked = connection.prepareStatement(sql)) {
      await()
          .pollInSameThread()
          .pollInterval(Duration.ofMillis(50))
          .atMost(Duration.ofSeconds(30))
          .until(
              () -> {
                try (ResultSet result = blocked.executeQuery()) {
                  return result.next() && result.getLong(1) > 0;
                }
              });
    }
  }

  private String organisationUnit(String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits/",
            "{'name':'" + name + "','shortName':'" + name + "','openingDate':'2020-01-01'}"));
  }

  private void warmGroupAndInverseCaches(String group, String... items) {
    long id =
        transaction(
            () -> {
              OrganisationUnitGroup owner = manager.get(OrganisationUnitGroup.class, group);
              owner.getMembers().size();
              for (String item : items) {
                manager.get(OrganisationUnit.class, item).getGroups().size();
              }
              return owner.getId();
            });
    assertTrue(
        sessionFactory
            .getCache()
            .containsCollection(OrganisationUnitGroup.class.getName() + ".members", id));
  }

  private void assertGroupMembers(String group, String... expected) {
    transaction(
        () -> {
          assertEquals(
              Set.copyOf(Arrays.asList(expected)),
              uids(manager.get(OrganisationUnitGroup.class, group).getMembers()));
          return null;
        });
  }

  private static Set<String> uids(java.util.Collection<? extends IdentifiableObject> items) {
    return items.stream().map(IdentifiableObject::getUid).collect(Collectors.toSet());
  }

  private TypeReport merge(
      IdentifiableObject owner, String property, List<String> additions, List<String> deletions) {
    try {
      return collectionService.mergeCollectionItems(owner, property, items(additions, deletions));
    } catch (FeedbackException ex) {
      throw new AssertionError(ex);
    }
  }

  private static IdentifiableObjects items(List<String> additions, List<String> deletions) {
    IdentifiableObjects items = new IdentifiableObjects();
    items.setAdditions(
        additions.stream().map(uid -> new BaseIdentifiableObject(uid, "", "")).toList());
    items.setDeletions(
        deletions.stream().map(uid -> new BaseIdentifiableObject(uid, "", "")).toList());
    return items;
  }

  private <T> T transaction(Supplier<T> action) {
    return new TransactionTemplate(transactionManager)
        .execute(
            status -> {
              entityManager.clear();
              T result = action.get();
              entityManager.flush();
              entityManager.clear();
              return result;
            });
  }
}
