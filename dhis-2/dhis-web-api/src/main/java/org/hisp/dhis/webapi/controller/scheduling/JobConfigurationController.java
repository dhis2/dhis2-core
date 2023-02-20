/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.scheduling;

import static org.hisp.dhis.scheduling.JobStatus.DISABLED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Map;

import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.JobTypes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple controller for API endpoints
 *
 * @author Henning Håkonsen
 */
@OpenApi.Tags( "system" )
@RestController
@RequestMapping( value = JobConfigurationSchemaDescriptor.API_ENDPOINT )
public class JobConfigurationController
    extends AbstractCrudController<JobConfiguration>
{
    private final JobConfigurationService jobConfigurationService;

    private final SchedulingManager schedulingManager;

    public JobConfigurationController( JobConfigurationService jobConfigurationService,
        SchedulingManager schedulingManager )
    {
        this.jobConfigurationService = jobConfigurationService;
        this.schedulingManager = schedulingManager;
    }

    @GetMapping( value = "/jobTypesExtended", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    public @ResponseBody Map<String, Map<String, Property>> getJobTypesExtended()
    {
        return jobConfigurationService.getJobParametersSchema();
    }

    @GetMapping( value = "/jobTypes", produces = APPLICATION_JSON_VALUE )
    public JobTypes getJobTypeInfo()
    {
        return new JobTypes( jobConfigurationService.getJobTypeInfo() );
    }

    @PostMapping( value = "{uid}/execute", produces = { APPLICATION_JSON_VALUE, "application/javascript" } )
    public ObjectReport executeJobConfiguration( @PathVariable( "uid" ) String uid )
        throws NotFoundException
    {
        JobConfiguration jobConfiguration = jobConfigurationService.getJobConfigurationByUid( uid );

        if ( jobConfiguration == null )
        {
            throw new NotFoundException( JobConfiguration.class, uid );
        }

        ObjectReport objectReport = new ObjectReport( JobConfiguration.class, 0 );

        boolean success = schedulingManager.executeNow( jobConfiguration );

        if ( !success )
        {
            objectReport.addErrorReport( new ErrorReport( JobConfiguration.class,
                new ErrorMessage( ErrorCode.E7006, jobConfiguration.getName() ) ) );
        }

        return objectReport;
    }

    @Override
    protected void preCreateEntity( JobConfiguration jobConfiguration )
        throws ConflictException
    {
        checkConfigurable( jobConfiguration, "Job %s must be configurable but was not." );
    }

    @Override
    protected void preUpdateEntity( JobConfiguration before, JobConfiguration after )
        throws ConflictException
    {
        checkConfigurable( before, "Job %s is a system job that cannot be modified." );
        checkConfigurable( after, "Job %s can not be changed into a system job." );
    }

    @Override
    protected void preDeleteEntity( JobConfiguration jobConfiguration )
        throws ConflictException
    {
        checkConfigurable( jobConfiguration,
            "Job %s is a system job that cannot be deleted." );
    }

    @Override
    protected void preUpdateItems( JobConfiguration jobConfiguration, IdentifiableObjects items )
        throws ConflictException
    {
        checkConfigurable( jobConfiguration,
            "Job %s is a system job that cannot be modified." );
    }

    @Override
    protected void postPatchEntity( JsonPatch patch, JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isEnabled() )
        {
            schedulingManager.stop( jobConfiguration );
        }
        jobConfigurationService.refreshScheduling( jobConfiguration );
        if ( jobConfiguration.getJobStatus() != DISABLED )
        {
            schedulingManager.schedule( jobConfiguration );
        }
    }

    private void checkConfigurable( JobConfiguration configuration, String message )
        throws ConflictException
    {
        if ( !configuration.isConfigurable() )
        {
            String identifier = configuration.getUid();
            if ( identifier == null )
            {
                identifier = configuration.getName();
            }
            throw new ConflictException( String.format( message, identifier ) );
        }
    }

}
