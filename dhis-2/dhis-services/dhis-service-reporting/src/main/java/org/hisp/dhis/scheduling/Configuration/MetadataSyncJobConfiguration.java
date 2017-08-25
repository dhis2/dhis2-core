package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.dxf2.metadata.tasks.MetadataSyncJob;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class MetadataSyncJobConfiguration extends JobConfiguration
{
    @Override
    public Runnable getRunnable()
    {
        return new MetadataSyncJob();
    }
}
