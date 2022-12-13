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

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobQueueService;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;
import org.hisp.dhis.webapi.openapi.SchemaGenerators.UID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API for scheduler list and named queues (sequences).
 *
 * @author Jan Bernitt
 */
@OpenApi.Tags( "system" )
@RestController
@RequestMapping( value = "/scheduler" )
@AllArgsConstructor
public class JobSchedulerController
{
    private final JobConfigurationService jobConfigurationService;

    private final JobQueueService jobQueueService;

    @Value
    static class SchedulerEntry
    {
        String name;

        String type;

        String cronExpression;

        Date nextExecutionTime;

        JobStatus status;

        boolean enabled;

        boolean configurable;

        List<SchedulerEntryJob> sequence;

        static SchedulerEntry of( JobConfiguration config )
        {
            return new SchedulerEntry(
                config.getName(),
                config.getJobType().name(),
                config.getCronExpression(),
                config.getNextExecutionTime(),
                config.getJobStatus(),
                config.isEnabled(),
                config.getJobType().isConfigurable(),
                List.of( SchedulerEntryJob.of( config ) ) );
        }

        static SchedulerEntry of( List<JobConfiguration> queue )
        {
            JobConfiguration trigger = queue.get( 0 );
            if ( trigger.getQueueName() == null )
            {
                return of( trigger );
            }
            return new SchedulerEntry(
                trigger.getQueueName(),
                "Sequence",
                trigger.getCronExpression(),
                trigger.getNextExecutionTime(),
                trigger.getJobStatus(),
                trigger.isEnabled(),
                true,
                queue.stream()
                    .sorted( comparing( JobConfiguration::getQueuePosition ) )
                    .map( SchedulerEntryJob::of )
                    .collect( toList() ) );
        }
    }

    @Value
    @OpenApi.Property
    static class SchedulerEntryJob
    {
        @OpenApi.Property( { UID.class, JobConfiguration.class } )
        String id;

        String name;

        JobType type;

        String cronExpression;

        Date nextExecutionTime;

        JobStatus status;

        static SchedulerEntryJob of( JobConfiguration config )
        {
            return new SchedulerEntryJob(
                config.getUid(),
                config.getName(),
                config.getJobType(),
                config.getCronExpression(),
                config.getNextExecutionTime(),
                config.getJobStatus() );
        }
    }

    @GetMapping
    public List<SchedulerEntry> getSchedulerEntries()
    {
        Map<String, List<JobConfiguration>> configsByQueueNameOrUid = jobConfigurationService.getAllJobConfigurations()
            .stream().collect( groupingBy( JobConfiguration::getQueueIdentifier ) );
        return configsByQueueNameOrUid.values().stream()
            .map( SchedulerEntry::of )
            .sorted( comparing( SchedulerEntry::getNextExecutionTime ) )
            .collect( toList() );
    }

    @GetMapping( "/queueable" )
    public List<SchedulerEntry> getQueueableJobs( @RequestParam( required = false ) String name )
    {
        Predicate<JobConfiguration> nameFilter = name == null || name.isEmpty()
            ? config -> true
            : config -> !name.equals( config.getQueueName() );
        return jobConfigurationService.getAllJobConfigurations().stream()
            .filter( JobConfiguration::isConfigurable )
            .filter( not( JobConfiguration::isLeaderOnlyJob ) )
            .filter( config -> config.getSchedulingType() != SchedulingType.FIXED_DELAY )
            .filter( config -> config.getQueueName() == null )
            .filter( nameFilter )
            .map( SchedulerEntry::of )
            .collect( toList() );
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class SchedulerQueue
    {
        String name;

        String cronExpression;

        @OpenApi.Property( { UID[].class, JobConfiguration.class } )
        List<String> sequence;
    }

    @GetMapping( "/queue/" )
    public Set<String> getQueueNames()
    {
        return jobConfigurationService.getAllJobConfigurations().stream()
            .filter( config -> config.getQueueName() != null )
            .map( JobConfiguration::getQueueName )
            .collect( toUnmodifiableSet() );
    }

    @GetMapping( "/queue/{name}" )
    public SchedulerQueue getQueue( @PathVariable String name )
        throws WebMessageException
    {
        List<JobConfiguration> sequence = getQueueSequence( name );
        JobConfiguration trigger = sequence.get( 0 );
        return new SchedulerQueue( trigger.getQueueName(), trigger.getCronExpression(),
            sequence.stream().map( IdentifiableObject::getUid ).collect( toList() ) );
    }

    @PostMapping( "/queue/{name}" )
    @ResponseStatus( HttpStatus.CREATED )
    public void createQueue( @PathVariable String name, @RequestBody SchedulerQueue queue )
    {
        jobQueueService.createQueue( name, queue.getCronExpression(), queue.getSequence() );
    }

    @PutMapping( "/queue/{name}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateQueue( @PathVariable String name, @RequestBody SchedulerQueue queue )
    {
        jobQueueService.updateQueue( name, queue.getCronExpression(), queue.getSequence() );
    }

    @DeleteMapping( "/queue/{name}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteQueue( @PathVariable String name )
    {
        jobQueueService.deleteQueue( name );
    }

    private List<JobConfiguration> getQueueSequence( String name )
        throws WebMessageException
    {
        List<JobConfiguration> sequence = jobConfigurationService.getAllJobConfigurations().stream()
            .filter( config -> name.equals( config.getQueueName() ) )
            .sorted( comparing( JobConfiguration::getQueuePosition ) )
            .collect( toList() );
        if ( sequence.isEmpty() )
        {
            throw new WebMessageException( notFound( SchedulerQueue.class, name ) );
        }
        return sequence;
    }
}
