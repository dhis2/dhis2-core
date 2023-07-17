package org.hisp.dhis.scheduling;

import java.util.List;
import javax.annotation.Nonnull;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Jan Bernitt
 */
@Repository
public class HibernateJobConfigurationStore
    extends HibernateIdentifiableObjectStore<JobConfiguration> implements JobConfigurationStore {

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
  public List<JobConfigurationTrigger> getAllTriggers() {
    // language=SQL
    String sql =
        """
      select uid, lastexecuted, cronexpression, delay
      from jobconfiguration
      where enabled = true
      and jobstatus = 'SCHEDULED'
      and (queueposition is null or queueposition = 0)""";
    return getSession().createNativeQuery(sql, JobConfigurationTrigger.class).list();
  }

  @Override
  public boolean tryRun(@Nonnull String uid) {
    // language=SQL
    String sql =
        """
      update jobconfiguration
      set jobstatus = 'RUNNING', lastexecuted = now(), lastalive = now(),
      where enabled = true and uid = :id and jobstatus = 'SCHEDULED'
      """;
    return getSession().createNativeQuery(sql).setParameter("id", uid).executeUpdate() > 0;
  }

  @Override
  public boolean tryStop(@Nonnull String uid, JobStatus lastExecutedStatus) {
    // language=SQL
    String sql =
        """
      update jobconfiguration
      set lastexecuted = now(), lastexecutedstatus = :status, lastalive = null,
          jobstatus = case when enabled = true then 'SCHEDULED' else 'DISABLED' end
      where uid = :id and jobstatus = 'RUNNING'
      """;
    return getSession()
            .createNativeQuery(sql)
            .setParameter("id", uid)
            .setParameter("status", lastExecutedStatus)
            .executeUpdate()
        > 0;
  }

  @Override
  public boolean assureRunning(@Nonnull String uid) {
    // language=SQL
    String sql =
        """
      update jobconfiguration
      set lastalive = now()
      where uid = :id and jobstatus = 'RUNNING'
      """;
    return getSession().createNativeQuery(sql).setParameter("id", uid).executeUpdate() > 0;
  }
}
