package org.hisp.dhis.analytics.table.scheduling;

import java.util.Date;

import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.springframework.stereotype.Component;

@Component( "continuousAnalyticsTableJob" )
public class ContinuousAnlyticsTableJob
    extends AbstractJob
{
    private final AnalyticsTableGenerator analyticsTableGenerator;

    public ContinuousAnlyticsTableJob( AnalyticsTableGenerator analyticsTableGenerator )
    {
        this.analyticsTableGenerator = analyticsTableGenerator;
    }

    @Override
    public JobType getJobType()
    {
        return JobType.CONTINUOUS_ANALYTICS_TABLE;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        AnalyticsJobParameters parameters = (AnalyticsJobParameters) jobConfiguration.getJobParameters();

        Date lastSuccessfulUpdate = analyticsTableGenerator.getLastSuccessfulAnalyticsTableUpdate();

        // TODO
    }
}
