package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

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

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cronutils.model.CronType.QUARTZ;
import static org.hisp.dhis.scheduling.DefaultSchedulingManager.CONTINOUS_CRON;
import static org.hisp.dhis.scheduling.DefaultSchedulingManager.HOUR_CRON;
import static org.hisp.dhis.scheduling.JobStatus.DISABLED;

/**
 * @author Henning Håkonsen
 */
public class JobConfigurationObjectBundleHook
    extends AbstractObjectBundleHook
{
    private static final Log log = LogFactory.getLog( JobConfigurationObjectBundleHook.class );

    private static final int SUCCESS = 1;

    @Autowired
    private JobConfigurationService jobConfigurationService;

    private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    private List<ErrorReport> validateCronForJobType( JobConfiguration jobConfiguration )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        // Make list of all jobs for each job type
        Map<JobType, List<JobConfiguration>> jobConfigurationForJobTypes = new HashMap<>();

        jobConfigurationService.getAllJobConfigurations().stream()
            .filter( configuration -> !Objects.equals( configuration.getUid(), jobConfiguration.getUid() ) )
            .forEach( configuration -> {
                List<JobConfiguration> jobConfigurationList = new ArrayList<>();
                List<JobConfiguration> oldList = jobConfigurationForJobTypes.get( configuration.getJobType() );
                if ( oldList != null )
                {
                    jobConfigurationList.addAll( oldList );
                }
                jobConfigurationList.add( configuration );
                jobConfigurationForJobTypes.put( configuration.getJobType(), jobConfigurationList );
            } );

        /*
         *  Validate that there are no other jobs of the same job type which are scheduled with the same cron.
         *
         *  Also check if the job is trying to run continuously while other jobs of the same type is running continuously - this should not be allowed
         */
        List<JobConfiguration> listForJobType = jobConfigurationForJobTypes.get( jobConfiguration.getJobType() );

        if ( listForJobType != null )
        {
            for ( JobConfiguration jobConfig : listForJobType )
            {
                if ( jobConfiguration.isContinuousExecution() )
                {
                    if ( jobConfig.isContinuousExecution() )
                    {
                        errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E7001 ) );
                    }
                }
                else
                {
                    if ( jobConfig.getCronExpression().equals( jobConfiguration.getCronExpression() ) )
                    {
                        errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E7000 ) );
                    }
                }
            }
        }

        return errorReports;
    }

    private List<ErrorReport> validateInternal( JobConfiguration jobConfiguration )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        JobConfiguration persitedJobConfiguration = jobConfigurationService.getJobConfigurationByUid( jobConfiguration.getUid() );
        if ( persitedJobConfiguration != null && !persitedJobConfiguration.isConfigurable() )
        {
            if ( persitedJobConfiguration.compareTo( jobConfiguration ) !=  SUCCESS )
            {
                errorReports
                    .add( new ErrorReport( JobConfiguration.class, ErrorCode.E7003, jobConfiguration.getJobType() ) );
            }
            else
            {
                persitedJobConfiguration.setCronExpression( jobConfiguration.getCronExpression() );
                jobConfiguration = persitedJobConfiguration;
            }
        }

        if ( !jobConfiguration.isContinuousExecution() )
        {
            if ( jobConfiguration.getCronExpression() == null )
            {
                errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E7004 ) );
                return errorReports;
            }

            // Validate the cron expression
            CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor( QUARTZ );
            CronParser parser = new CronParser( cronDefinition );
            Cron quartzCron;
            try
            {
                quartzCron = parser.parse( jobConfiguration.getCronExpression() );
                quartzCron.validate();
            }
            catch ( IllegalArgumentException e )
            {
                errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E7005, e ) );
                return errorReports;
            }
        }

        // Validate cron expression with relation to all other jobs
        errorReports.addAll( validateCronForJobType( jobConfiguration ) );

        // Validate parameters
        ErrorReport parameterValidation =
            jobConfiguration.getJobParameters() != null ? jobConfiguration.getJobParameters().validate() : null;
        if ( parameterValidation != null )
        {
            errorReports.add( parameterValidation );
        }

        Job job = schedulingManager.getJob( jobConfiguration.getJobType() );
        ErrorReport jobValidation = job.validate();
        if ( jobValidation != null )
        {
            errorReports.add( jobValidation );
        }

        return errorReports;
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( object ) )
        {
            return new ArrayList<>();
        }

        JobConfiguration jobConfiguration = (JobConfiguration) object;
        List<ErrorReport> errorReports = new ArrayList<>( validateInternal( jobConfiguration ) );

        if ( errorReports.size() == 0 )
        {
            jobConfiguration.setNextExecutionTime( null );
            if ( jobConfiguration.isContinuousExecution() )
            {
                jobConfiguration.setCronExpression( CONTINOUS_CRON );
            }
            log.info( "Validation of '" + jobConfiguration.getName() + "' succeeded" );
        }
        else
        {
            log.info( "Validation of '" + jobConfiguration.getName() + "' failed." );
            log.info( errorReports );
        }

        return errorReports;
    }

    private boolean setDefaultCronExpressionWhenDisablingContinuousExectution( JobConfiguration newObject, JobConfiguration persistedObject )
    {
        return ( !newObject.isContinuousExecution() && persistedObject.isContinuousExecution() ) && newObject.getCronExpression().equals( CONTINOUS_CRON );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( object ) )
        {
            return;
        }
        JobConfiguration newObject = (JobConfiguration) object;
        JobConfiguration persObject = (JobConfiguration) persistedObject;

        newObject.setLastExecuted( persObject.getLastExecuted() );
        newObject.setLastExecutedStatus( persObject.getLastExecutedStatus() );
        newObject.setLastRuntimeExecution( persObject.getLastRuntimeExecution() );

        if ( setDefaultCronExpressionWhenDisablingContinuousExectution( newObject, persObject ) )
        {
            newObject.setCronExpression( HOUR_CRON );
        }

        schedulingManager.stopJob( (JobConfiguration) persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void preDelete( T persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( persistedObject ) )
        {
            return;
        }

        schedulingManager.stopJob( (JobConfiguration) persistedObject );
        sessionFactory.getCurrentSession().delete( persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( persistedObject ) )
        {
            return;
        }

        JobConfiguration jobConfiguration = (JobConfiguration) persistedObject;

        if ( jobConfiguration.getJobStatus() != DISABLED )
        {
            schedulingManager.scheduleJob( jobConfiguration );
        }
    }

    @Override
    public <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( persistedObject ) )
        {
            return;
        }

        JobConfiguration jobConfiguration = (JobConfiguration) persistedObject;

        if ( jobConfiguration.getJobStatus() != DISABLED )
        {
            schedulingManager.scheduleJob( jobConfiguration );
        }
    }
}