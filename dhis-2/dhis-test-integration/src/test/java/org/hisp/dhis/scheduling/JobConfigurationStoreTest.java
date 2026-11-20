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
package org.hisp.dhis.scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.TextType;
import org.hisp.dhis.hibernate.jsonb.type.JsonJobParametersType;
import org.hisp.dhis.scheduling.parameters.MockJobParameters;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class JobConfigurationStoreTest extends PostgresIntegrationTestBase {

  private static final String CRON = "0 0 3 ? * *";

  @Autowired private JobConfigurationStore store;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void shouldMapJobConfigurationToItsTableAndColumns() {
    AbstractEntityPersister persister = persister();

    assertEquals("jobconfiguration", persister.getTableName());
    assertArrayEquals(new String[] {"jobconfigurationid"}, persister.getIdentifierColumnNames());
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());

    Map<String, String> columns =
        Map.ofEntries(
            Map.entry("uid", "uid"),
            Map.entry("code", "code"),
            Map.entry("name", "name"),
            Map.entry("created", "created"),
            Map.entry("lastUpdated", "lastupdated"),
            Map.entry("lastUpdatedBy", "lastupdatedby"),
            Map.entry("queueName", "queuename"),
            Map.entry("queuePosition", "queueposition"),
            Map.entry("cronExpression", "cronexpression"),
            Map.entry("delay", "delay"),
            Map.entry("jobType", "jobtype"),
            Map.entry("schedulingType", "schedulingtype"),
            Map.entry("jobStatus", "jobstatus"),
            Map.entry("lastExecutedStatus", "lastexecutedstatus"),
            Map.entry("executedBy", "executedby"),
            Map.entry("lastExecuted", "lastexecuted"),
            Map.entry("lastFinished", "lastfinished"),
            Map.entry("lastAlive", "lastalive"),
            Map.entry("enabled", "enabled"),
            Map.entry("jobParameters", "jsonbjobparameters"),
            Map.entry("errorCodes", "errorcodes"));
    columns.forEach(
        (property, column) ->
            assertEquals(
                column,
                persister.getPropertyColumnNames(property)[0].toLowerCase(),
                property + " column"));
  }

  @Test
  void shouldMapJobConfigurationNullabilityTypesLazinessAndCache() {
    AbstractEntityPersister persister = persister();

    for (String property :
        List.of(
            "uid",
            "name",
            "created",
            "lastUpdated",
            "jobType",
            "schedulingType",
            "jobStatus",
            "lastExecutedStatus",
            "enabled")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property :
        List.of(
            "code",
            "lastUpdatedBy",
            "queueName",
            "queuePosition",
            "cronExpression",
            "delay",
            "executedBy",
            "lastExecuted",
            "lastFinished",
            "lastAlive",
            "jobParameters",
            "errorCodes")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
    for (String property :
        List.of("jobType", "schedulingType", "jobStatus", "lastExecutedStatus")) {
      CustomType type = assertInstanceOf(CustomType.class, persister.getPropertyType(property));
      assertFalse(
          assertInstanceOf(EnumType.class, type.getUserType()).isOrdinal(),
          property + " is mapped ordinal");
    }
    assertInstanceOf(TextType.class, persister.getPropertyType("errorCodes"));
    assertInstanceOf(
        JsonJobParametersType.class,
        ((CustomType) persister.getPropertyType("jobParameters")).getUserType());
    assertFalse(((ManyToOneType) persister.getPropertyType("lastUpdatedBy")).isEager(null));
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  void shouldMapColumnsThatHibernateMustNotInsertOrUpdate() {
    for (String property : List.of("lastExecuted", "lastFinished", "lastAlive", "errorCodes")) {
      assertFalse(isInsertable(property), property + " is insertable");
      assertFalse(isUpdateable(property), property + " is updateable");
    }
    for (String property : List.of("created", "jobType", "jobStatus", "lastExecutedStatus")) {
      assertTrue(isInsertable(property), property + " is not insertable");
      assertFalse(isUpdateable(property), property + " is updateable");
    }
    for (String property : List.of("name", "schedulingType", "cronExpression", "enabled")) {
      assertTrue(isUpdateable(property), property + " is not updateable");
    }
  }

  @Test
  void shouldWriteAndReadJobConfigurationColumns() {
    User admin = getAdminUser();
    JobConfiguration job = new JobConfiguration("Job A", JobType.MOCK, admin.getUid());
    job.setCode("JOB_A");
    job.setSchedulingType(SchedulingType.CRON);
    job.setCronExpression(CRON);
    job.setDelay(30);
    job.setQueueName("queue A");
    job.setQueuePosition(2);
    job.setJobStatus(JobStatus.SCHEDULED);
    job.setLastExecutedStatus(JobStatus.COMPLETED);
    job.setEnabled(false);
    job.setLastUpdatedBy(admin);
    job.setJobParameters(new MockJobParameters("message A"));
    store.save(job);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, code, name, created, lastupdated, jobtype, schedulingtype, cronexpression, delay, queuename,"
                + " queueposition, jobstatus, lastexecutedstatus, enabled, executedby,"
                + " lastupdatedby, jsonbjobparameters::text as jobparameters"
                + " from jobconfiguration where jobconfigurationid = ?",
            job.getId());
    assertEquals(job.getUid(), row.get("uid"));
    assertEquals("JOB_A", row.get("code"));
    assertEquals("Job A", row.get("name"));
    assertEquals(job.getCreated().getTime(), ((Timestamp) row.get("created")).getTime());
    assertEquals(job.getLastUpdated().getTime(), ((Timestamp) row.get("lastupdated")).getTime());
    assertEquals("MOCK", row.get("jobtype"));
    assertEquals("CRON", row.get("schedulingtype"));
    assertEquals(CRON, row.get("cronexpression"));
    assertEquals(30, row.get("delay"));
    assertEquals("queue A", row.get("queuename"));
    assertEquals(2, row.get("queueposition"));
    assertEquals("SCHEDULED", row.get("jobstatus"));
    assertEquals("COMPLETED", row.get("lastexecutedstatus"));
    assertFalse((Boolean) row.get("enabled"));
    assertEquals(admin.getUid(), row.get("executedby"));
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());
    assertTrue(
        ((String) row.get("jobparameters")).contains("\"message\": \"message A\""),
        (String) row.get("jobparameters"));

    JobConfiguration loaded = reload(job);
    assertEquals("JOB_A", loaded.getCode());
    assertEquals("Job A", loaded.getName());
    assertEquals(job.getCreated().getTime(), loaded.getCreated().getTime());
    assertEquals(job.getLastUpdated().getTime(), loaded.getLastUpdated().getTime());
    assertEquals(JobType.MOCK, loaded.getJobType());
    assertEquals(SchedulingType.CRON, loaded.getSchedulingType());
    assertEquals(CRON, loaded.getCronExpression());
    assertEquals(30, loaded.getDelay());
    assertEquals("queue A", loaded.getQueueName());
    assertEquals(2, loaded.getQueuePosition());
    assertEquals(JobStatus.SCHEDULED, loaded.getJobStatus());
    assertEquals(JobStatus.COMPLETED, loaded.getLastExecutedStatus());
    assertFalse(loaded.isEnabled());
    assertEquals(admin.getUid(), loaded.getExecutedBy());
    assertEquals(admin.getUid(), loaded.getLastUpdatedBy().getUid());
    assertEquals(
        "message A",
        assertInstanceOf(MockJobParameters.class, loaded.getJobParameters()).getMessage());
  }

  @Test
  void shouldWriteSchedulingTypeAndJobStatusDerivedByTheirGetters() {
    JobConfiguration cron = new JobConfiguration("Job cron", JobType.MOCK);
    cron.setCronExpression(CRON);
    JobConfiguration delay = new JobConfiguration("Job delay", JobType.MOCK);
    delay.setDelay(60);
    JobConfiguration once = new JobConfiguration("Job once", JobType.MOCK);
    JobConfiguration disabledButEnabled = new JobConfiguration("Job enabled", JobType.MOCK);
    disabledButEnabled.setCronExpression(CRON);
    disabledButEnabled.setJobStatus(JobStatus.DISABLED);
    store.save(cron);
    store.save(delay);
    store.save(once);
    store.save(disabledButEnabled);
    entityManager.flush();

    assertEquals(List.of("CRON", "SCHEDULED"), schedulingTypeAndJobStatus(cron));
    assertEquals(List.of("FIXED_DELAY", "SCHEDULED"), schedulingTypeAndJobStatus(delay));
    assertEquals(List.of("ONCE_ASAP", "NOT_STARTED"), schedulingTypeAndJobStatus(once));
    assertEquals(List.of("CRON", "SCHEDULED"), schedulingTypeAndJobStatus(disabledButEnabled));
  }

  @Test
  void shouldKeepJobTypeAndStatusColumnsOnUpdate() {
    JobConfiguration job = new JobConfiguration("Job B", JobType.MOCK);
    job.setSchedulingType(SchedulingType.CRON);
    job.setCronExpression(CRON);
    store.save(job);
    entityManager.flush();

    JobConfiguration loaded = reload(job);
    loaded.setJobType(JobType.DATA_INTEGRITY);
    loaded.setJobStatus(JobStatus.RUNNING);
    loaded.setLastExecutedStatus(JobStatus.FAILED);
    loaded.setSchedulingType(SchedulingType.FIXED_DELAY);
    loaded.setDelay(10);
    loaded.setCreated(new Date());
    store.update(loaded);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select created, jobtype, jobstatus, lastexecutedstatus, schedulingtype, delay"
                + " from jobconfiguration where jobconfigurationid = ?",
            job.getId());
    assertEquals(job.getCreated().getTime(), ((Timestamp) row.get("created")).getTime());
    assertEquals("MOCK", row.get("jobtype"));
    assertEquals("SCHEDULED", row.get("jobstatus"));
    assertEquals("NOT_STARTED", row.get("lastexecutedstatus"));
    assertEquals("FIXED_DELAY", row.get("schedulingtype"));
    assertEquals(10, row.get("delay"));
  }

  @Test
  void shouldReadButNeverWriteColumnsMaintainedBySql() {
    JobConfiguration job = new JobConfiguration("Job C", JobType.MOCK);
    job.setCronExpression(CRON);
    job.setLastExecuted(new Date());
    job.setLastFinished(new Date());
    job.setLastAlive(new Date());
    job.setErrorCodes("E1000");
    store.save(job);
    entityManager.flush();
    Map<String, Object> inserted =
        jdbcTemplate.queryForMap(
            "select lastexecuted, lastfinished, lastalive, errorcodes from jobconfiguration"
                + " where jobconfigurationid = ?",
            job.getId());
    inserted.forEach((column, value) -> assertNull(value, column + " was inserted"));

    Timestamp executed = Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
    Timestamp finished = Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 5, 6));
    Timestamp alive = Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 6, 7));
    jdbcTemplate.update(
        "update jobconfiguration set lastexecuted = ?, lastfinished = ?, lastalive = ?,"
            + " errorcodes = 'E1100 E1105' where jobconfigurationid = ?",
        executed,
        finished,
        alive,
        job.getId());

    JobConfiguration loaded = reload(job);
    assertEquals(executed.getTime(), loaded.getLastExecuted().getTime());
    assertEquals(finished.getTime(), loaded.getLastFinished().getTime());
    assertEquals(alive.getTime(), loaded.getLastAlive().getTime());
    assertEquals("E1100 E1105", loaded.getErrorCodes());

    loaded.setLastExecuted(new Date());
    loaded.setLastFinished(null);
    loaded.setLastAlive(new Date());
    loaded.setErrorCodes(null);
    loaded.setName("Job C renamed");
    store.update(loaded);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select name, lastexecuted, lastfinished, lastalive, errorcodes from jobconfiguration"
                + " where jobconfigurationid = ?",
            job.getId());
    assertEquals("Job C renamed", row.get("name"));
    assertEquals(executed, row.get("lastexecuted"));
    assertEquals(finished, row.get("lastfinished"));
    assertEquals(alive, row.get("lastalive"));
    assertEquals("E1100 E1105", row.get("errorcodes"));
  }

  @Test
  void shouldLoadJobsInQueueAsEntitiesThroughNativeQuery() {
    JobConfiguration first = new JobConfiguration("Job queue 0", JobType.MOCK);
    first.setQueueName("queue B");
    first.setQueuePosition(0);
    first.setCronExpression(CRON);
    JobConfiguration second = new JobConfiguration("Job queue 1", JobType.DATA_INTEGRITY);
    second.setQueueName("queue B");
    second.setQueuePosition(1);
    store.save(second);
    store.save(first);
    entityManager.flush();
    clearSession();

    List<JobConfiguration> queue = store.getJobsInQueue("queue B");

    assertEquals(
        List.of(first.getUid(), second.getUid()),
        queue.stream().map(JobConfiguration::getUid).toList());
    assertEquals(SchedulingType.CRON, queue.get(0).getSchedulingType());
    assertEquals(JobType.DATA_INTEGRITY, queue.get(1).getJobType());
    assertEquals(
        List.of(second.getUid()),
        store.getJobConfigurations(JobType.DATA_INTEGRITY).stream()
            .map(JobConfiguration::getUid)
            .toList());
  }

  @Test
  void shouldEqualUninitializedProxyOfSameJob() {
    JobConfiguration job = new JobConfiguration("Job D", JobType.MOCK);
    job.setCode("JOB_D");
    store.save(job);
    entityManager.flush();
    clearSession();

    JobConfiguration proxy = entityManager.getReference(JobConfiguration.class, job.getId());

    assertFalse(Hibernate.isInitialized(proxy));
    assertEquals(proxy, job);
    assertEquals(job, proxy);
    assertEquals(job.hashCode(), proxy.hashCode());
  }

  @Test
  void shouldLoadNullJobParametersAsNull() {
    JobConfiguration job = new JobConfiguration("Job E", JobType.DATA_INTEGRITY);
    store.save(job);
    entityManager.flush();

    assertNull(
        jdbcTemplate.queryForObject(
            "select jsonbjobparameters::text from jobconfiguration where jobconfigurationid = ?",
            String.class,
            job.getId()));
    assertNull(reload(job).getJobParameters());
  }

  private List<String> schedulingTypeAndJobStatus(JobConfiguration job) {
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select schedulingtype, jobstatus from jobconfiguration where jobconfigurationid = ?",
            job.getId());
    return List.of((String) row.get("schedulingtype"), (String) row.get("jobstatus"));
  }

  private JobConfiguration reload(JobConfiguration job) {
    clearSession();
    entityManagerFactory.getCache().evict(JobConfiguration.class);
    return store.getByUid(job.getUid());
  }

  private boolean isNullable(String property) {
    AbstractEntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private boolean isInsertable(String property) {
    AbstractEntityPersister persister = persister();
    return persister
        .getPropertyInsertability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private boolean isUpdateable(String property) {
    AbstractEntityPersister persister = persister();
    return persister
        .getPropertyUpdateability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private AbstractEntityPersister persister() {
    return (AbstractEntityPersister)
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(JobConfiguration.class);
  }
}
