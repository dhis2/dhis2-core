package org.hisp.dhis.scheduling.hibernate;

import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationStore;

/**
 * @author Henning Håkonsen
 */
public class HibernateJobConfigurationStore
    extends HibernateIdentifiableObjectStore<JobConfiguration>
    implements JobConfigurationStore
{
}
