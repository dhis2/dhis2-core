/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static java.lang.Math.max;
import static java.util.stream.Collectors.toSet;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.hibernate.jsonb.type.JsonJobParametersType;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Jan Bernitt
 */
@Slf4j
@Repository
public class HibernateJobConfigurationStore
    extends HibernateIdentifiableObjectStore<JobConfiguration> implements JobConfigurationStore {

  public HibernateJobConfigurationStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, JobConfiguration.class, aclService, false);
  }

  @Override
  public UID getLastRunningId(@Nonnull JobType type) {
    String sql =
        """
      select uid from jobconfiguration
      where jobstatus = 'RUNNING' and jobtype = :type
      order by lastexecuted desc limit 1""";
    return runReadInStatelessSession(
        q ->
            UID.ofNullable(
                getSingleResultOrNull(q.createNativeQuery(sql).setParameter("type", type.name()))));
  }

  @Override
  public UID getLastCompletedId(@Nonnull JobType type) {
    String sql =
        """
      select uid from jobconfiguration
      where jobstatus != 'RUNNING' and jobtype = :type
      order by lastfinished desc limit 1""";
    return runReadInStatelessSession(
        q ->
            UID.ofNullable(
                getSingleResultOrNull(q.createNativeQuery(sql).setParameter("type", type.name()))));
  }

  @Override
  public String getProgress(@Nonnull UID jobId) {
    String sql =
        """
      select
        case jsonb_typeof(progress)
        when 'object' then progress #>> '{}'
        when 'array' then jsonb_build_object('sequence', progress) #>> '{}'
        end
      from jobconfiguration
      where uid = :id
      """;
    return runReadInStatelessSession(
        q -> getSingleResultOrNull(q.createNativeQuery(sql).setParameter("id", jobId.getValue())));
  }

  @Override
  public String getErrors(@Nonnull UID jobId) {
    String sql =
        """
          select
            case jsonb_typeof(progress)
            when 'object' then progress #>> '{errors}'
            end
          from jobconfiguration
          where uid = :id
          """;
    return runReadInStatelessSession(
        q -> getSingleResultOrNull(q.createNativeQuery(sql).setParameter("id", jobId.getValue())));
  }

  @Override
  public Set<UID> getAllIds() {
    String sql = "select uid from jobconfiguration";
    return runReadInStatelessSession(q -> setOf(q.createNativeQuery(sql), UID::ofNullable));
  }

  @Override
  public Set<UID> getAllCancelledIds() {
    String sql = "select uid from jobconfiguration where cancel = true";
    return runReadInStatelessSession(q -> setOf(q.createNativeQuery(sql), UID::ofNullable));
  }

  @Override
  public Set<JobType> getRunningTypes() {
    String sql = "select distinct jobtype from jobconfiguration where jobstatus = 'RUNNING'";
    return runReadInStatelessSession(q -> setOf(q.createNativeQuery(sql), JobType::valueOf));
  }

  @Override
  public Set<JobType> getCompletedTypes() {
    String sql =
        """
      select distinct jobtype from jobconfiguration
      where jobstatus != 'RUNNING'
      and lastfinished > lastexecuted
      and progress is not null
      """;
    return runReadInStatelessSession(q -> setOf(q.createNativeQuery(sql), JobType::valueOf));
  }

  @Override
  public Set<String> getAllQueueNames() {
    String sql = "select distinct queuename from jobconfiguration where queuename is not null";
    return runReadInStatelessSession(q -> setOf(q.createNativeQuery(sql), Object::toString));
  }

  @Override
  public JobEntry getJobById(UID id) {
    String sql =
        """
      select
        j.uid,
        j.jobtype,
        j.schedulingtype,
        j.name,
        j.jobstatus,
        j.executedby,
        j.cronexpression,
        j.delay,
        j.lastexecuted,
        j.lastfinished,
        j.lastalive,
        j.queuename,
        j.queueposition,
        j.jsonbjobparameters
      from jobconfiguration j
      where uid = :id
      """;
    return runReadInStatelessSession(
        q ->
            toEntry(
                getSingleResultOrNull(q.createNativeQuery(sql).setParameter("id", id.getValue()))));
  }

  @Override
  public List<JobConfiguration> getJobsInQueue(@Nonnull String queue) {
    String sql = "select * from jobconfiguration where queuename = :queue order by queueposition;";
    return nativeSynchronizedTypedQuery(sql).setParameter("queue", queue).list();
  }

  @Override
  @CheckForNull
  public JobEntry getNextInQueue(@Nonnull String queue, int fromPosition) {
    String sql =
        """
      select
        j.uid,
        j.jobtype,
        j.schedulingtype,
        j.name,
        j.jobstatus,
        j.executedby,
        j.cronexpression,
        j.delay,
        j.lastexecuted,
        j.lastfinished,
        j.lastalive,
        j.queuename,
        j.queueposition,
        j.jsonbjobparameters
      from jobconfiguration j
      where queuename = :queue and queueposition = :pos
      """;
    return runReadInStatelessSession(
        q ->
            toEntry(
                getSingleResultOrNull(
                    q.createNativeQuery(sql)
                        .setParameter("queue", queue)
                        .setParameter("pos", fromPosition + 1))));
  }

  @Override
  public List<JobConfiguration> getJobConfigurations(JobType type) {
    String sql = "select * from jobconfiguration where jobtype = :type";
    return nativeSynchronizedTypedQuery(sql).setParameter("type", type.name()).list();
  }

  @Override
  public List<JobEntry> getStaleConfigurations(int timeoutSeconds) {
    String sql =
        """
        select
          j.uid,
          j.jobtype,
          j.schedulingtype,
          j.name,
          j.jobstatus,
          j.executedby,
          j.cronexpression,
          j.delay,
          j.lastexecuted,
          j.lastfinished,
          j.lastalive,
          j.queuename,
          j.queueposition,
          j.jsonbjobparameters
        from jobconfiguration j
        where jobstatus = 'RUNNING'
        and (
          now() > lastalive + :timeout * interval '1 second'
          or (schedulingtype = 'FIXED_DELAY'
            and delay is not null
            and now() > lastexecuted + delay * interval '2 second'
          ))
        """;
    return runReadInStatelessSession(
        q -> entryList(q.createNativeQuery(sql).setParameter("timeout", timeoutSeconds)));
  }

  @Override
  public List<JobEntry> getDueJobConfigurations(boolean includeWaiting) {
    String sql =
        """
        select
          j1.uid,
          j1.jobtype,
          j1.schedulingtype,
          j1.name,
          j1.jobstatus,
          j1.executedby,
          j1.cronexpression,
          j1.delay,
          j1.lastexecuted,
          j1.lastfinished,
          j1.lastalive,
          j1.queuename,
          j1.queueposition,
          j1.jsonbjobparameters
        from jobconfiguration j1
        where enabled = true
        and jobstatus = 'SCHEDULED'
        and (queueposition is null or queueposition = 0 or schedulingtype = 'ONCE_ASAP')
        and (:waiting = true or not exists (
          select 1 from jobconfiguration j2
          where j2.jobtype = j1.jobtype
          and j2.jobconfigurationid != j1.jobconfigurationid
          and j2.jobstatus = 'RUNNING'
        ))
        order by jobtype, created
        """;
    return runReadInStatelessSession(
        q -> entryList(q.createNativeQuery(sql).setParameter("waiting", includeWaiting)));
  }

  @Nonnull
  @Override
  public List<String> findJobRunErrors(@Nonnull JobRunErrorsParams params) {
    String sql =
        """
    select jsonb_build_object(
    'id', c.uid,
    'type', c.jobType,
    'user', c.executedby,
    'created', c.created,
    'executed', c.lastexecuted,
    'finished', c.lastfinished,
    'file', fr.uid,
    'filesize', fr.contentlength,
    'filetype', fr.contenttype,
    'errors', c.progress -> 'errors') #>> '{}'
    from jobconfiguration c left join fileresource fr on c.uid = fr.uid
    where c.errorcodes is not null and c.errorcodes != ''
      and (:skipUid or c.uid = :uid)
      and (:skipUser or c.executedby = :user)
      and (:skipStart or c.lastexecuted >= :start)
      and (:skipEnd or c.lastexecuted <= :end)
      and (:skipObjects or jsonb_exists_any(c.progress -> 'errors', :objects ))
      and (:skipCodes or string_to_array(c.errorcodes, ' ') && :codes)
      and (:skipTypes or c.jobtype = any (:types))
    order by c.lastexecuted desc;
    """;
    List<UID> objectList = params.getObject();
    List<String> errors =
        objectList == null ? List.of() : objectList.stream().map(UID::getValue).toList();
    List<String> codeList = params.getCode();
    List<String> codes = codeList == null ? List.of() : codeList;
    List<JobType> typeList = params.getType();
    List<String> types =
        typeList == null ? List.of() : typeList.stream().map(JobType::name).toList();
    Date start = params.getFrom();
    Date end = params.getTo();
    UID user = params.getUser();
    UID job = params.getJob();
    return runReadInStatelessSession(
        q ->
            jsonList(
                q.createNativeQuery(sql)
                    .setParameter("skipUid", job == null)
                    .setParameter("uid", job == null ? "" : job.getValue())
                    .setParameter("skipUser", user == null)
                    .setParameter("user", user == null ? "" : user.getValue())
                    .setParameter("skipStart", start == null)
                    .setParameter("start", start == null ? new Date() : start)
                    .setParameter("skipEnd", end == null)
                    .setParameter("end", end == null ? new Date() : end)
                    .setParameter("skipObjects", errors.isEmpty())
                    .setParameter(
                        "objects", errors.toArray(String[]::new), StringArrayType.INSTANCE)
                    .setParameter("skipCodes", codes.isEmpty())
                    .setParameter("codes", codes.toArray(String[]::new), StringArrayType.INSTANCE)
                    .setParameter("skipTypes", types.isEmpty())
                    .setParameter("types", types.toArray(String[]::new), StringArrayType.INSTANCE),
                Object::toString));
  }

  /**
   * Note that the transaction boundary has been set here instead of the service to avoid over
   * complicating the "executeNow" service method which needs this change to be completed and
   * visible at the end of this method.
   */
  @Override
  public boolean tryExecuteNow(@Nonnull UID jobId) {
    String sql =
        """
        update jobconfiguration
        set
          schedulingtype = 'ONCE_ASAP',
          cancel = false,
          jobstatus = 'SCHEDULED'
        where uid = :id
        and enabled = true
        and jobstatus != 'RUNNING'
        and (schedulingtype != 'ONCE_ASAP' or lastfinished is null)
        """;
    return runWriteInStatelessSession(
            q -> q.createNativeQuery(sql).setParameter("id", jobId.getValue()).executeUpdate())
        > 0;
  }

  @Override
  public boolean tryStart(@Nonnull UID jobId) {
    // only flip from SCHEDULED to RUNNING if no other job of same type is RUNNING
    String sql =
        """
        update jobconfiguration j1
        set
          lastupdated = now(),
          jobstatus = 'RUNNING',
          lastexecuted = now(),
          lastalive = now(),
          progress = null,
          cancel = false
        where uid = :id
        and jobstatus = 'SCHEDULED'
        and enabled = true
        and not exists (
          select 1 from jobconfiguration j2
          where j2.jobtype = j1.jobtype
          and j2.jobconfigurationid != j1.jobconfigurationid
          and j2.jobstatus = 'RUNNING'
        )
        """;
    return runWriteInStatelessSession(
            q -> q.createNativeQuery(sql).setParameter("id", jobId.getValue()).executeUpdate())
        > 0;
  }

  @Override
  public boolean tryCancel(@Nonnull UID jobId) {
    String sql =
        """
        update jobconfiguration
        set
          lastupdated = now(),
          cancel = case when jobstatus = 'RUNNING' then true else false end,
          enabled = case
            when queueposition is null and schedulingtype = 'ONCE_ASAP' and cronexpression is null and delay is null then false
            else enabled end,
          schedulingtype = case
            when cronexpression is not null then 'CRON'
            when delay is not null then 'FIXED_DELAY'
            else schedulingtype end,
          lastexecutedstatus = case
            when jobstatus = 'RUNNING' then lastexecutedstatus
            else 'STOPPED' end
        where uid = :id
        and (
          /* scenario 1: cancel a running job by marking it */
          jobstatus = 'RUNNING' and cancel = false
          or /* scenario 2: cancel a execute now before it started by reverting back */
          jobstatus = 'SCHEDULED' and schedulingtype = 'ONCE_ASAP'
          )
        """;
    return runWriteInStatelessSession(
            q -> q.createNativeQuery(sql).setParameter("id", jobId.getValue()).executeUpdate())
        > 0;
  }

  @Override
  public boolean tryFinish(@Nonnull UID jobId, JobStatus status) {
    String sql =
        """
        update jobconfiguration
        set
          lastupdated = now(),
          lastexecutedstatus = case when cancel = true then 'STOPPED' else :status end,
          lastfinished = now(),
          lastalive = null,
          cancel = false,
          enabled = case
            when queueposition is null and schedulingtype = 'ONCE_ASAP' and cronexpression is null and delay is null then false
            else enabled end,
          jobstatus = case
            when enabled = false
              or (queueposition is null and schedulingtype = 'ONCE_ASAP' and cronexpression is null and delay is null) then 'DISABLED'
            else 'SCHEDULED' end,
          schedulingtype = case
            when cronexpression is not null then 'CRON'
            when delay is not null then 'FIXED_DELAY'
            else schedulingtype end
        where uid = :id
        and jobstatus = 'RUNNING'
        """;
    return runWriteInStatelessSession(
            q ->
                q.createNativeQuery(sql)
                    .setParameter("id", jobId.getValue())
                    .setParameter("status", status.name())
                    .executeUpdate())
        > 0;
  }

  @Override
  public boolean trySkip(@Nonnull String queue) {
    String sql =
        """
        update jobconfiguration
        set
          lastupdated = now(),
          lastexecutedstatus = 'NOT_STARTED',
          lastexecuted = now(),
          lastfinished = now(),
          lastalive = null,
          progress = null,
          cancel = false
        where queuename = :queue
        and jobstatus = 'SCHEDULED'
        and queueposition > 0
        and (
          lastexecuted is null
          or lastexecuted < (select lastexecuted from jobconfiguration where queuename = :queue and queueposition = 0 limit 1))
        """;
    return runWriteInStatelessSession(
            q -> q.createNativeQuery(sql).setParameter("queue", queue).executeUpdate())
        > 0;
  }

  @Override
  public void updateProgress(
      @Nonnull UID jobId, @CheckForNull String progressJson, @CheckForNull String errorCodes) {
    String sql =
        """
        update jobconfiguration
        set
          lastalive = case when jobstatus = 'RUNNING' then now() else lastalive end,
          errorcodes = :errors,
          progress = cast(:json as jsonb)
        where uid = :id
        """;
    runWriteInStatelessSession(
        q ->
            q.createNativeQuery(sql)
                .setParameter("id", jobId.getValue())
                .setParameter("json", progressJson)
                .setParameter("errors", errorCodes)
                .executeUpdate());
  }

  @Override
  public int updateDisabledJobs() {
    String sql =
        """
        update jobconfiguration
        set
          lastupdated = now(),
          jobstatus = 'DISABLED'
        where jobstatus = 'SCHEDULED'
        and enabled = false
        """;
    return runWriteInStatelessSession(q -> q.createNativeQuery(sql).executeUpdate());
  }

  @Override
  public int deleteFinishedJobs(int ttlMinutes) {
    String sql =
        """
        delete from jobconfiguration
        where schedulingtype = 'ONCE_ASAP'
        and cronexpression is null
        and delay is null
        and queueposition is null
        and lastfinished is not null
        and now() > lastfinished + :ttl * interval '1 minute'
        """;
    int deletedCount =
        runWriteInStatelessSession(
            q ->
                q.createNativeQuery(sql)
                    .setLockOptions(new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(2000))
                    .setParameter("ttl", max(1, ttlMinutes))
                    .executeUpdate());
    if (deletedCount == 0) return 0;
    // jobs have the same UID as their respective FR
    // so if no job exists with the same UID the FR is not assigned
    String sql2 =
        """
        update fileresource fr
        set isassigned = false
        where domain = 'JOB_DATA'
        and isassigned = true
        and uid not in (select uid from jobconfiguration where schedulingtype = 'ONCE_ASAP')
        """;
    runWriteInStatelessSession(
        q ->
            q.createNativeQuery(sql2)
                .setLockOptions(new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(2000))
                .executeUpdate());
    return deletedCount;
  }

  @Override
  public int rescheduleStaleJobs(int timeoutMinutes) {
    String sql =
        """
        update jobconfiguration
        set
          lastupdated = now(),
          jobstatus = case
            when enabled = false
              or (queueposition is null and schedulingtype = 'ONCE_ASAP' and cronexpression is null and delay is null) then 'DISABLED'
            else 'SCHEDULED' end,
          enabled = cronexpression is not null or delay is not null or queueposition is not null,
          cancel = false,
          lastexecutedstatus = 'FAILED',
          lastfinished = now(),
          lastalive = null,
          schedulingtype = case
            when cronexpression is not null then 'CRON'
            when delay is not null then 'FIXED_DELAY'
            when queueposition is not null then 'CRON'
            else schedulingtype end
        where jobstatus = 'RUNNING'
        and now() > lastalive + :timeout * interval '1 minute'
        """;
    return runWriteInStatelessSession(
        q ->
            q.createNativeQuery(sql)
                .setParameter("timeout", max(1, timeoutMinutes))
                .executeUpdate());
  }

  @Override
  public boolean tryRevertNow(@Nonnull UID jobId) {
    String sql =
        """
        update jobconfiguration
        set
          lastupdated = now(),
          jobstatus = case
            when enabled = false
              or (queueposition is null and schedulingtype = 'ONCE_ASAP' and cronexpression is null and delay is null) then 'DISABLED'
            else 'SCHEDULED' end,
          enabled = cronexpression is not null or delay is not null or queueposition is not null,
          cancel = false,
          lastexecutedstatus = 'FAILED',
          lastfinished = now(),
          lastalive = null,
          schedulingtype = case
            when cronexpression is not null then 'CRON'
            when delay is not null then 'FIXED_DELAY'
            when queueposition is not null then 'CRON'
            else schedulingtype end
        where jobstatus = 'RUNNING'
        and uid = :id
        and now() > jobconfiguration.lastalive + interval '1 minute'
      """;
    return runWriteInStatelessSession(
            q -> q.createNativeQuery(sql).setParameter("id", jobId.getValue()).executeUpdate())
        > 0;
  }

  private static String getSingleResultOrNull(NativeQuery<?> query) {
    return (String) query.getResultStream().findFirst().orElse(null);
  }

  @SuppressWarnings("unchecked")
  private static <T> Set<T> setOf(NativeQuery<?> query, Function<String, T> mapper) {
    Stream<String> stream = (Stream<String>) query.stream();
    return stream.map(mapper).collect(toSet());
  }

  @SuppressWarnings("unchecked")
  private static <T> List<T> jsonList(NativeQuery<?> query, Function<String, T> mapper) {
    Stream<String> stream = (Stream<String>) query.stream();
    return stream.map(mapper).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<JobEntry> entryList(NativeQuery<?> query) {
    Stream<Object[]> stream = (Stream<Object[]>) query.stream();
    return stream.map(HibernateJobConfigurationStore::toEntry).toList();
  }

  private int runWriteInStatelessSession(ToIntFunction<StatelessSession> query) {
    Transaction transaction = null;
    try (StatelessSession session = getSession().getSessionFactory().openStatelessSession()) {
      transaction = session.beginTransaction();
      int modifiedRowCount = query.applyAsInt(session);
      transaction.commit();
      return modifiedRowCount;
    } catch (RuntimeException ex) {
      // Handle rollback for self-managed transactions
      if (transaction != null && transaction.isActive()) {
        transaction.rollback();
      }
      log.warn("Job update failed:", ex);
      return 0;
    }
  }

  private <R> R runReadInStatelessSession(Function<StatelessSession, R> query) {
    Transaction transaction = null;
    try (StatelessSession session = getSession().getSessionFactory().openStatelessSession()) {
      transaction = session.beginTransaction();
      R res = query.apply(session);
      transaction.commit();
      return res;
    } catch (RuntimeException ex) {
      // Handle rollback for self-managed transactions
      if (transaction != null && transaction.isActive()) {
        transaction.rollback();
      }
      log.warn("Job update failed:", ex);
      throw ex;
    }
  }

  private static JobEntry toEntry(Object row) {
    if (row == null) return null;
    if (!(row instanceof Object[] columns))
      throw new IllegalArgumentException("Job row must be an Object[]");
    return new JobEntry(
        UID.ofNullable((String) columns[0]),
        JobType.valueOf((String) columns[1]),
        SchedulingType.valueOf((String) columns[2]),
        (String) columns[3],
        JobStatus.valueOf((String) columns[4]),
        UID.ofNullable((String) columns[5]),
        (String) columns[6],
        (Integer) columns[7],
        (Date) columns[8],
        (Date) columns[9],
        (Date) columns[10],
        (String) columns[11],
        (Integer) columns[12],
        JsonJobParametersType.fromJson((String) columns[13]));
  }
}
