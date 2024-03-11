package org.hisp.dhis.dataintegrity.jobs;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataintegrity.DataIntegrityService;
import org.hisp.dhis.dataintegrity.FlattenedDataIntegrityReport;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
public class DataIntegrityJob
    extends AbstractJob
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataIntegrityService dataIntegrityService;

    public void setDataIntegrityService( DataIntegrityService dataIntegrityService )
    {
        this.dataIntegrityService = dataIntegrityService;
    }

    private Notifier notifier;

    @Autowired
    public void setNotifier( Notifier notifier )
    {
        this.notifier = notifier;
    }

    @Override
    public JobType getJobType()
    {
        return JobType.DATA_INTEGRITY;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        Timer timer = new SystemTimer().start();

        notifier.notify(
            jobConfiguration, NotificationLevel.INFO,
            "Starting data integrity job", false );

        FlattenedDataIntegrityReport report = dataIntegrityService.getFlattenedDataIntegrityReport();

        timer.stop();

        if ( jobConfiguration != null )
        {
            notifier.notify(
                jobConfiguration, NotificationLevel.INFO,
                "Data integrity checks completed in " + timer.toString() + ".", true )
                .addJobSummary( jobConfiguration, report, FlattenedDataIntegrityReport.class );
        }
    }

}
