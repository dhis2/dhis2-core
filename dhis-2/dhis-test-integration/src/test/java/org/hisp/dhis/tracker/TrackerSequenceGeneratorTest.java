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
package org.hisp.dhis.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Date;
import java.util.function.LongSupplier;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.deduplication.DeduplicationService;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicate;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.tracker.export.singleevent.SingleEventChangeLog;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventChangeLog;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entities written both through Hibernate and through JDBC (relying on the column default) must
 * take their ids from the same dedicated sequence, otherwise the two writers hand out overlapping
 * primary keys.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackerSequenceGeneratorTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private DeduplicationService deduplicationService;

  @Autowired private JdbcTemplate jdbcTemplate;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
  }

  @BeforeEach
  void resetSecurityContext() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldNotReuseIdOfJdbcInsertedChangeLogWhenPersistingChangeLogThroughHibernate() {
    moveToSameValue("hibernate_sequence", "trackedentitychangelog_sequence");
    TrackedEntity trackedEntity = get(TrackedEntity.class, "QS6w44flWAf");
    TrackedEntityAttribute attribute = get(TrackedEntityAttribute.class, "integerAttr");
    Long jdbcId =
        jdbcTemplate.queryForObject(
            """
            insert into trackedentitychangelog (trackedentityid, trackedentityattributeid,
              previousvalue, currentvalue, changelogtype, created, createdby)
            values (?, ?, null, '1', 'CREATE', now(), 'admin')
            returning trackedentitychangelogid
            """,
            Long.class,
            trackedEntity.getId(),
            attribute.getId());

    TrackedEntityChangeLog changeLog =
        new TrackedEntityChangeLog(
            trackedEntity, attribute, "1", "2", ChangeLogType.UPDATE, new Date(), "admin");
    entityManager.persist(changeLog);
    entityManager.flush();

    assertNotEquals(jdbcId, changeLog.getId());
  }

  @Test
  void shouldTakeTrackedEntityChangeLogIdFromItsSequence() {
    assertIdTakenFrom(
        "trackedentitychangelog_sequence",
        () ->
            persist(
                    new TrackedEntityChangeLog(
                        get(TrackedEntity.class, "QS6w44flWAf"),
                        get(TrackedEntityAttribute.class, "integerAttr"),
                        "1",
                        "2",
                        ChangeLogType.UPDATE,
                        new Date(),
                        "admin"))
                .getId());
  }

  @Test
  void shouldTakeTrackerEventChangeLogIdFromItsSequence() {
    assertIdTakenFrom(
        "trackereventchangelog_sequence",
        () ->
            persist(
                    new TrackerEventChangeLog(
                        get(TrackerEvent.class, "pTzf9KYMk72"),
                        get(DataElement.class, "DATAEL00001"),
                        null,
                        "1",
                        "2",
                        ChangeLogType.UPDATE,
                        new Date(),
                        "admin"))
                .getId());
  }

  @Test
  void shouldTakeSingleEventChangeLogIdFromItsSequence() {
    assertIdTakenFrom(
        "singleeventchangelog_sequence",
        () ->
            persist(
                    new SingleEventChangeLog(
                        get(SingleEvent.class, "OTmjvJDn0Fu"),
                        get(DataElement.class, "DATAEL00001"),
                        null,
                        "1",
                        "2",
                        ChangeLogType.UPDATE,
                        new Date(),
                        "admin"))
                .getId());
  }

  @Test
  void shouldTakePotentialDuplicateIdFromItsSequence() {
    assertIdTakenFrom(
        "potentialduplicatesequence",
        () -> {
          PotentialDuplicate potentialDuplicate =
              new PotentialDuplicate(UID.of("QS6w44flWAf"), UID.of("dUE514NMOlo"));
          try {
            deduplicationService.addPotentialDuplicate(potentialDuplicate);
          } catch (PotentialDuplicateConflictException e) {
            throw new IllegalStateException(e);
          }
          entityManager.flush();
          return potentialDuplicate.getId();
        });
  }

  /**
   * Moves both sequences apart, so an id taken from the wrong one cannot match the expected
   * sequence by coincidence. Sequences are not transactional, so only ever move them forward.
   */
  private void assertIdTakenFrom(String sequence, LongSupplier persist) {
    long value = moveToSameValue("hibernate_sequence", sequence);
    jdbcTemplate.execute("select setval('hibernate_sequence', " + (value + 1000) + ")");

    long id = persist.getAsLong();

    assertEquals(value + 1, id);
  }

  /** Moves both sequences forward to the higher of their two values and returns that value. */
  private long moveToSameValue(String sequence1, String sequence2) {
    Long value =
        jdbcTemplate.queryForObject(
            "select greatest((select last_value from %s), (select last_value from %s))"
                .formatted(sequence1, sequence2),
            Long.class);
    assertNotNull(value);
    jdbcTemplate.execute("select setval('%s', %d)".formatted(sequence1, value));
    jdbcTemplate.execute("select setval('%s', %d)".formatted(sequence2, value));
    return value;
  }

  private <T> T persist(T entity) {
    entityManager.persist(entity);
    entityManager.flush();
    return entity;
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T object = manager.get(type, uid);
    assertNotNull(object);
    return object;
  }
}
