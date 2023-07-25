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

import static java.util.stream.Collectors.toSet;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * @author Jan Bernitt
 */
@Repository
public class HibernateJobConfigurationStore
    extends HibernateIdentifiableObjectStore<JobConfiguration> implements JobConfigurationStore {

  private static final RowMapper<JobConfiguration> TRIGGER_ROW_MAPPER =
      (rs, index) -> {
        JobConfiguration res = new JobConfiguration();
        res.setName(rs.getString(1));
        res.setJobType(JobType.valueOf(rs.getString(2)));
        res.setUid(rs.getString(3));
        res.setSchedulingType(SchedulingType.valueOf(rs.getString(4)));
        Timestamp lastUpdated = rs.getTimestamp(5);
        res.setLastExecuted(lastUpdated == null ? null : new Date(lastUpdated.getTime()));
        res.setCronExpression(rs.getString(6));
        res.setDelay(rs.getObject(7, Integer.class));
        return res;
      };

  public HibernateJobConfigurationStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        JobConfiguration.class,
        currentUserService,
        aclService,
        true);
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
  public String getCompletedProgress(@Nonnull String jobId) {
    // language=SQL
    String sql =
        """
      select progress #>> '{}' from jobconfiguration
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
          extract('epoch' from lastalive - now()) > :timeout
          or (schedulingtype = 'FIXED_DELAY'
            and delay is not null
            and extract('epoch' from lastexecuted - now()) > 2 * delay)
          )
        """;
    return getSession()
        .createNativeQuery(sql, JobConfiguration.class)
        .setParameter("timeout", timeoutSeconds * 1000L)
        .list();
  }

  @Override
  public List<JobConfiguration> getAllTriggers() {
    // language=SQL
    String sql =
        """
        select name, jobtype, uid, schedulingtype, lastexecuted, cronexpression, delay
        from jobconfiguration j1
        where enabled = true
        and jobstatus = 'SCHEDULED'
        and (queueposition is null or queueposition = 0)
        """;
    return jdbcTemplate.query(sql, TRIGGER_ROW_MAPPER);
  }

  @Override
  public boolean tryScheduleToRunOutOfOrder(@Nonnull String jobId) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set
          schedulingtype = 'ONCE_ASAP'
        where enabled = true
        and uid = :id
        and jobstatus = 'SCHEDULED'
        and schedulingtype != 'ONCE_ASAP'
        """;
    return nativeQuery(sql).setParameter("id", jobId).executeUpdate() > 0;
  }

  @Override
  public boolean tryRun(@Nonnull String jobId) {
    // only flip from SCHEDULED to RUNNING if no other job of same type is RUNNING
    // language=SQL
    String sql =
        """
        update jobconfiguration j1
        set
          jobstatus = 'RUNNING',
          lastexecuted = now(),
          lastalive = now()
        where enabled = true
        and uid = :id
        and jobstatus = 'SCHEDULED'
        and enabled = true
        and not exists (
          select 1 from jobconfiguration j2 where j2.jobtype = j1.jobtype and j2.jobstatus = 'RUNNING'
        )
        """;
    return nativeQuery(sql).setParameter("id", jobId).executeUpdate() > 0;
  }

  @Override
  public boolean tryStop(@Nonnull String jobId, JobStatus lastExecutedStatus) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set
          lastexecutedstatus = :status,
          lastfinished = now(),
          lastalive = null,
          enabled = case
            when schedulingtype = 'ONCE_ASAP' and cronexpression is null and delay is null then false
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
            .setParameter("status", lastExecutedStatus.name())
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
          lastalive = null
        where queuename = :queue
        and jobstatus = 'SCHEDULED'
        and queueposition > 0
        and lastexecuted < (select lastexecuted from jobconfiguration where queuename = :queue and queueposition = 0 limit 1)
        """;
    return nativeQuery(sql).setParameter("queue", queue).executeUpdate() > 0;
  }

  @Override
  public boolean assureRunning(@Nonnull String jobId) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set lastalive = now()
        where uid = :id and jobstatus = 'RUNNING'
        """;
    return nativeQuery(sql).setParameter("id", jobId).executeUpdate() > 0;
  }

  @Override
  public void attachProgress(@Nonnull String jobId, @CheckForNull String progressJson) {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set progress = to_jsonb(:json)
        where uid = :id
        """;
    nativeQuery(sql).setParameter("id", jobId).setParameter("json", progressJson).executeUpdate();
  }

  @Override
  public int updateDisabledJobs() {
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set jobstatus = 'DISABLED'
        where schedulingtype = 'SCHEDULED'
        and enabled = false
        """;
    return nativeQuery(sql).executeUpdate();
  }

  @Override
  public int deleteFinishedJobs(int ttlSeconds) {
    // language=SQL
    String sql =
        """
        delete from jobconfiguration
        where schedulingtype = 'ONCE_ASAP'
        and enabled = false
        and cronexpression is null
        and delay is null
        and lastfinished is not null
        and lastfinished + :ttl * interval '1 second' > now()
        """;
    return nativeQuery(sql).setParameter("ttl", ttlSeconds).executeUpdate();
  }

  @Override
  public int rescheduleStaleJobs(int timeoutMinutes) {
    if (timeoutMinutes < 1)
      throw new IllegalArgumentException(
          "Timeout must be 1 minute or longer but was: " + timeoutMinutes);
    // language=SQL
    String sql =
        """
        update jobconfiguration
        set
          jobstatus = 'SCHEDULED',
          lastexecutedstatus = 'FAILED',
          lastfinished = now(),
          lastalive = null,
          progress = null,
          schedulingtype = case
            when cronexpression is not null then 'CRON'
            when delay is not null then 'FIXED_DELAY'
            else schedulingtype end
        where jobstatus = 'RUNNING'
        and enabled = true
        and lastalive > lastexecuted
        and extract('epoch' from lastalive - now()) > :timeout
        """;
    return nativeQuery(sql).setParameter("timeout", timeoutMinutes * 60_000L).executeUpdate();
  }

  private NativeQuery<?> nativeQuery(String sql) {
    return getSession().createNativeQuery(sql);
  }

  private static String getSingleResultOrNull(NativeQuery<?> query) {
    List<?> res = query.list();
    return res == null || res.isEmpty() ? null : (String) res.get(0);
  }

  @SuppressWarnings("unchecked")
  private static <T> Set<T> getResultSet(NativeQuery<?> query, Function<String, T> mapper) {
    Stream<String> stream = (Stream<String>) query.stream();
    return stream.map(mapper).collect(toSet());
  }
}
