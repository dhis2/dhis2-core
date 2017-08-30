package org.hisp.dhis.scheduling;

import org.hisp.dhis.common.GenericNameableObjectStore;
import org.hisp.dhis.scheduling.Configuration.JobConfiguration;

/**
 * @author Henning Håkonsen
 */
public interface JobConfigurationStore
    extends GenericNameableObjectStore<JobConfiguration>
{
    String ID = JobConfigurationStore.class.getName();
}
