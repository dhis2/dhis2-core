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
package org.hisp.dhis.scheduling;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.NotFoundException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jan Bernitt
 */
@Service
@RequiredArgsConstructor
public class DefaultJobQueueService implements JobQueueService
{
    private final IdentifiableObjectStore<JobConfiguration> jobConfigurationStore;

    @Override
    @Transactional( readOnly = true )
    public Set<String> getQueueNames()
    {
        return jobConfigurationStore.getAll().stream()
            .map( JobConfiguration::getQueueName )
            .filter( Objects::nonNull )
            .collect( toUnmodifiableSet() );
    }

    @Override
    @Transactional( readOnly = true )
    public List<JobConfiguration> getQueue( String name )
        throws NotFoundException
    {
        List<JobConfiguration> sequence = jobConfigurationStore.getAll().stream()
            .filter( config -> name.equals( config.getQueueName() ) )
            .sorted( comparing( JobConfiguration::getQueuePosition ) )
            .collect( toList() );
        if ( sequence.isEmpty() )
        {
            throw new NotFoundException( ErrorCode.E7020, name );
        }
        return sequence;
    }

    @Override
    @Transactional
    public void createQueue( String name, String cronExpression, List<String> sequence )
        throws NotFoundException,
        ConflictException
    {
        if ( !getQueueJobsByQueueName( name ).isEmpty() )
        {
            throw new ConflictException( ErrorCode.E7021, name );
        }
        Map<String, JobConfiguration> queueJobs = getQueueJobsByIds( sequence );
        validateCronExpression( cronExpression );
        validateQueue( name, queueJobs.values() );
        addSequenceToQueue( name, cronExpression, sequence, queueJobs );
    }

    @Override
    @Transactional
    public void updateQueue( String name, String newCronExpression, List<String> newSequence )
        throws NotFoundException,
        ConflictException
    {
        Map<String, JobConfiguration> oldQueueJobs = getQueueJobsByQueueName( name );
        if ( oldQueueJobs.isEmpty() )
        {
            throw new NotFoundException( ErrorCode.E7020, name );
        }
        Map<String, JobConfiguration> newQueueJobs = getQueueJobsByIds( newSequence );
        validateCronExpression( newCronExpression );
        validateQueue( name, newQueueJobs.values() );
        addSequenceToQueue( name, newCronExpression, newSequence, newQueueJobs );
        oldQueueJobs.entrySet().stream()
            .filter( e -> !newQueueJobs.containsKey( e.getKey() ) )
            .map( Map.Entry::getValue )
            .forEach( this::removeJobFromQueue );
    }

    @Override
    @Transactional
    public void deleteQueue( String name )
        throws NotFoundException
    {
        Collection<JobConfiguration> jobs = getQueueJobsByQueueName( name ).values();
        if ( jobs.isEmpty() )
        {
            throw new NotFoundException( ErrorCode.E7020, name );
        }
        jobs.forEach( this::removeJobFromQueue );
    }

    private void validateQueue( String name, Collection<JobConfiguration> sequence )
        throws ConflictException
    {
        // sequence must be at least 2 entries long
        if ( sequence.size() < 2 )
        {
            throw new ConflictException( ErrorCode.E7024 );
        }
        // job is not already part of another queue
        Optional<JobConfiguration> alreadyInQueue = sequence.stream()
            .filter( config -> !name.equals( config.getQueueName() ) && config.isUsedInQueue() )
            .findFirst();
        if ( alreadyInQueue.isPresent() )
        {
            JobConfiguration config = alreadyInQueue.get();
            throw new ConflictException( ErrorCode.E7022, config.getUid(), config.getQueueName() );
        }
        // job is a system job
        Optional<JobConfiguration> systemJob = sequence.stream().filter( config -> !config.isConfigurable() )
            .findFirst();
        if ( systemJob.isPresent() )
        {
            JobConfiguration config = systemJob.get();
            throw new ConflictException( ErrorCode.E7023, config.getUid() );
        }
    }

    private void validateCronExpression( String cronExpression )
        throws ConflictException
    {
        try
        {
            CronExpression.parse( cronExpression );
        }
        catch ( IllegalArgumentException ex )
        {
            throw new ConflictException( ErrorCode.E7005, ex.getMessage() );
        }
    }

    private void addSequenceToQueue( String name, String cronExpression, List<String> sequence,
        Map<String, JobConfiguration> queueJobs )
        throws NotFoundException
    {
        for ( int pos = 0; pos < sequence.size(); pos++ )
        {
            String jobId = sequence.get( pos );
            addJobToQueue( jobId, queueJobs.get( jobId ), name, pos, cronExpression );
        }
    }

    private void addJobToQueue( String jobId, JobConfiguration config, String name, int position,
        String cronExpression )
        throws NotFoundException
    {
        if ( config == null )
        {
            throw new NotFoundException( JobConfiguration.class, jobId );
        }
        config.setQueueName( name );
        config.setQueuePosition( position );
        config.setCronExpression( position == 0 ? cronExpression : null );
        jobConfigurationStore.update( config );
    }

    private void removeJobFromQueue( JobConfiguration config )
    {
        config.setQueueName( null );
        config.setQueuePosition( null );
        config.setCronExpression( null );
        config.setEnabled( false );
        jobConfigurationStore.update( config );
    }

    private Map<String, JobConfiguration> getQueueJobsByQueueName( String name )
    {
        return jobConfigurationStore.getAll().stream()
            .filter( c -> name.equals( c.getQueueName() ) )
            .collect( toMap( IdentifiableObject::getUid, Function.identity() ) );
    }

    private Map<String, JobConfiguration> getQueueJobsByIds( List<String> sequence )
    {
        return jobConfigurationStore.getByUid( sequence ).stream()
            .collect( toMap( IdentifiableObject::getUid, Function.identity() ) );
    }
}
