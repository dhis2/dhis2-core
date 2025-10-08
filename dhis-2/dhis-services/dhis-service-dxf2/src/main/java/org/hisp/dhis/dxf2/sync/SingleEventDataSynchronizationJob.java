package org.hisp.dhis.dxf2.sync;


import lombok.AllArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dbms.DbmsUtils;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.SingleEventDataSynchronizationJobParameters;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Component
@AllArgsConstructor
public class SingleEventDataSynchronizationJob implements Job {

    private final SingleEventDataSynchronizationService eventSync;

    @Override
    public JobType getJobType() {
        return JobType.SINGLE_EVENT_DATA_SYNC_JOB;
    }

    @Override
    public void execute(JobConfiguration config, JobProgress progress) {
            SingleEventDataSynchronizationJobParameters params =
                    (SingleEventDataSynchronizationJobParameters) config.getJobParameters();
            eventSync.synchronizeData(params.getPageSize(), progress);
    }
}
