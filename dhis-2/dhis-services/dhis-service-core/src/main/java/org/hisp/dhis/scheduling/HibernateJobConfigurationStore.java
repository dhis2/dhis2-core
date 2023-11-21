/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    super(entityManager, jdbcTemplate, publisher, JobConfiguration.class, aclService, true);
  }

  @Override
  public String getLastRunningId(@Nonnull JobType type) {
    // language=SQL
    String sql =
        """
      select uid from jobconfiguration
      where jobstatus = 'RUNNING' and jobtype = :type
      order by lastexecuted desc limit 1""";
    return getSingleResultOrNull(nativeQuery(sql).setParameter("type", type.name()));
  }

  @Override
  public String getLastCompletedId(@Nonnull JobType type) {
    // language=SQL
    String sql =
        """
      select uid from jobconfiguration
      where jobstatus != 'RUNNING' and jobtype = :type
      order by lastfinished desc limit 1""";
    return getSingleResultOrNull(nativeQuery(sql).setParameter("type", type.name()));
  }

  @Override
  public String getProgress(@Nonnull String jobId) {
    // language=SQL
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
    return getSingleResultOrNull(nativeQuery(sql).setParameter("id", jobId));
  }

  @Override
  public String getErrors(@Nonnull String jobId) {
    // language=SQL
    String sql =
        """
          select
            case jsonb_typeof(progress)
            when 'object' then progress #>> '{errors}'
           end
          from jobconfiguration
          where uid = :id
          """;
    return getSingleResultOrNull(nativeQuery(sql).setParameter("id", jobId));
  }

  @Override
  public Set<String> getAllIds() {
    // language=SQL
    String sql = "select uid from jobconfiguration";
    return getResultSet(nativeQuery(sql), Object::toString);
  }

  @Override
  public Set<String> getAllCancelledIds() {
    // language=SQL
    String sql = "select uid from jobconfiguration where cancel = true";
    return getResultSet(nativeQuery(sql), Object::toString);
  }

  @Override
  public Set<JobType> getRunningTypes() {
    // language=SQL
    String sql = "select distinct jobtype from jobconfiguration where jobstatus = 'RUNNING'";
    return getResultSet(nativeQuery(sql), JobType::valueOf);
  }

  @Override
  public Set<JobType> getCompletedTypes() {
    // language=SQL
    String sql =
        """
      select distinct jobtype from jobconfiguration
      where jobstatus != 'RUNNING'
      and lastfinished > lastexecuted
      and progress is not null
      """;
    return getResultSet(nativeQuery(sql), JobType::valueOf);
  }

  @Override
  public Set<String> getAllQueueNames() {
    // language=SQL
    String sql = "select distinct queuename from jobconfiguration where queuename is not null";
    return getResultSet(nativeQuery(sql), Object::toString);
  }

  @Override
  public List<JobConfiguration> getJobsInQueue(@Nonnull String queue) {
    // language=SQL
    String sql = "select * from jobconfiguration where queuename = :queue order by queueposition;";
    return getSession()
        .createNativeQuery(sql, JobConfiguration.class)
        .setParameter("queue", queue)
        .list();
  }

  @Override
  @CheckForNull
  public JobConfiguration getNextInQueue(@Nonnull String queue, int fromPosition) {
    // language=SQL
    String sql = "select * from jobconfiguration where queuename = :queue and queueposition = :pos";
    List<JobConfiguration> res =
        getSession()
            .createNativeQuery(sql, JobConfiguration.class)
            .setParameter("queue", queue)
            .setParameter("pos", fromPosition + 1)
            .list();
    return res.isEmpty() ? null : res.get(0);
  }

  @Override
  public List<JobConfiguration> getJobConfigurations(JobType type) {
    // language=SQL
    String sql = "select * from jobconfiguration where jobtype = :type";
    return getSession()
        .createNativeQuery(sql, JobConfiguration.class)
        .setParameter("type", type.name())
        .list();
  }

  @Override
  public List<JobConfiguration> getStaleConfigurations(int timeoutSeconds) {
    // language=SQL
    String sql =
        """
        select * from jobconfiguration
        where jobstatus = 'RUNNING'
        and (
          now() > lastalive + :timeout * interval '1 second'
          or (schedulingtype = 'FIXED_DELAY'
            and delay is not null
            and now() > lastexecuted + delay * interval '2 second'
          ))
        """;
    return getSession()
        .createNativeQuery(sql, JobConfiguration.class)
        .setParameter("timeout", timeoutSeconds)
        .list();
  }

  @Override
  public Stream<JobConfiguration> getDueJobConfigurations(boolean includeWaiting) {
    // language=SQL
    String sql =
        """
        select * from jobconfiguration j1
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
    return getSession()
        .createNativeQuery(sql, JobConfiguration.class)
        .setParameter("waiting", includeWaiting)
        .stream();
  }

  @Nonnull
  @Override
  public Stream<String> findJobRunErrors(@Nonnull JobRunErrorsParams params) {
    // language=SQL
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
    List<ErrorCode> codeList = params.getCode();
    List<String> codes =
        codeList == null ? List.of() : codeList.stream().map(ErrorCode::name).toList();
    List<JobType> typeList = params.getType();
    List<String> types =
        typeList == null ? List.of() : typeList.stream().map(JobType::name).toList();
    Date start = params.getFrom();
    Date end = params.getTo();
    UID user = params.getUser();
    UID job = params.getJob();
    return getResultStream(
        nativeQuery(sql)
            .setParameter("skipUid", job == null)
            .setParameter("uid", job == null ? "" : job.getValue())
            .setParameter("skipUser", user == null)
            .setParameter("user", user == null ? "" : user.getValue())
            .setParameter("skipStart", start == null)
            .setParameter("start", start == null ? new Date() : start)
            .setParameter("skipEnd", end == null)
            .setParameter("end", end == null ? new Date() : end)
            .setParameter("skipObjects", errors.isEmpty())
            .setParameter("objects", errors.toArray(String[]::new), StringArrayType.INSTANCE)
            .setParameter("skipCodes", codes.isEmpty())
            .setParameter("codes", codes.toArray(String[]::new), StringArrayType.INSTANCE)
            .setParameter("skipTypes", types.isEmpty())
            .setParameter("types", types.toArray(String[]::new), StringArrayType.INSTANCE),
        Object::toString);
  }

  @Override
  @Transactional(propagation = REQUIRES_NEW)
  public boolean tryExecuteNow(@Nonnull String jobId) {
    // language=SQL
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
    return nativeQuery(sql).setParameter("id", jobId).executeUpdate() > 0;
  }

  @Override
  public boolean tryStart(@Nonnull String jobId) {
    // only flip from SCHEDULED to RUNNING if no other job of same type is RUNNING
    // language=SQL
    String sql =
        """
        update jobconfiguration j1
        set
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
    return nativeQuery(sql).setParameter("id", jobId).executeUpdate() > 0;
  }

  @Override
  public boolean tryCancel(@Nonnull String jobId) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set
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
    return nativeQuery(sql).setParameter("id", jobId).executeUpdate() > 0;
  }

  @Override
  public boolean tryFinish(@Nonnull String jobId, JobStatus status) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set
          lastexecutedstatus = case when cancel = true then 'STOPPED' else :status end,
          lastfinished = now(),
          lastalive = null,
          cancel = false,
          enabled = case
            when queueposition is null and schedulingtype = 'ONCE_ASAP' and cronexpression is null and delay is null then false
            else enabled end,
          jobstatus = case
            when enabled = false
              or (schedulingtype = 'ONCE_ASAP' and cronexpression is null and delay is null) then 'DISABLED'
            else 'SCHEDULED' end,
          schedulingtype = case
            when cronexpression is not null then 'CRON'
            when delay is not null then 'FIXED_DELAY'
            else schedulingtype end
        where uid = :id
        and jobstatus = 'RUNNING'
        """;
    return nativeQuery(sql)
            .setParameter("id", jobId)
            .setParameter("status", status.name())
            .executeUpdate()
        > 0;
  }

  @Override
  public boolean trySkip(@Nonnull String queue) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set
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
    return nativeQuery(sql).setParameter("queue", queue).executeUpdate() > 0;
  }

  @Override
  public void updateProgress(
      @Nonnull String jobId, @CheckForNull String progressJson, @CheckForNull String errorCodes) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set
          lastalive = case when jobstatus = 'RUNNING' then now() else lastalive end,
          errorcodes = :errors,
          progress = cast(:json as jsonb)
        where uid = :id
        """;
    nativeQuery(sql)
        .setParameter("id", jobId)
        .setParameter("json", progressJson)
        .setParameter("errors", errorCodes)
        .executeUpdate();
  }

  @Override
  public int updateDisabledJobs() {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set jobstatus = 'DISABLED'
        where jobstatus = 'SCHEDULED'
        and enabled = false
        """;
    return nativeQuery(sql).executeUpdate();
  }

  @Override
  public int deleteFinishedJobs(int ttlMinutes) {
    // language=SQL
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
    int deletedCount = nativeQuery(sql).setParameter("ttl", max(1, ttlMinutes)).executeUpdate();
    if (deletedCount == 0) return 0;
    // jobs have the same UID as their respective FR
    // so if no job exists with the same UID the FR is not assigned
    sql =
        """
        update fileresource fr
        set isassigned = false
        where domain = 'JOB_DATA'
        and uid not in (select uid from jobconfiguration where schedulingtype = 'ONCE_ASAP')
        """;
    nativeQuery(sql).executeUpdate();
    return deletedCount;
  }

  @Override
  public int rescheduleStaleJobs(int timeoutMinutes) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set
          jobstatus = 'SCHEDULED',
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
        and enabled = true
        and (schedulingtype != 'ONCE_ASAP' or lastfinished is null)
        and now() > lastalive + :timeout * interval '1 minute'
        """;
    return nativeQuery(sql).setParameter("timeout", max(1, timeoutMinutes)).executeUpdate();
  }

  private NativeQuery<?> nativeQuery(String sql) {
    return getSession().createNativeQuery(sql);
  }

  private static String getSingleResultOrNull(NativeQuery<?> query) {
    List<?> res = query.list();
    return res == null || res.isEmpty() ? null : (String) res.get(0);
  }

  @SuppressWarnings("unchecked")
  private static <T> Set<T> getResultSet(Query<?> query, Function<String, T> mapper) {
    Stream<String> stream = (Stream<String>) query.stream();
    return stream.map(mapper).collect(toSet());
  }

  @SuppressWarnings("unchecked")
  private static <T> Stream<T> getResultStream(Query<?> query, Function<String, T> mapper) {
    Stream<String> stream = (Stream<String>) query.stream();
    return stream.map(mapper);
  }
}
