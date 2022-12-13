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

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.IllegalQueryException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 *
 * @author Jan Bernitt
 */
@Service
@AllArgsConstructor
public class DefaultJobQueueService implements JobQueueService
{
    private final IdentifiableObjectStore<JobConfiguration> jobConfigurationStore;

    @Override
    @Transactional
    public void createQueue( String name, String cronExpression, List<String> sequence )
    {
        //TODO name not already used
        Map<String, JobConfiguration> queueJobs = getQueueJobConfigurationsById( sequence );
        validateQueue( queueJobs.values() );
        for ( int pos = 0; pos < sequence.size(); pos++ )
        {
            addToQueue( queueJobs.get( sequence.get( pos ) ), name, pos, cronExpression );
        }
    }

    @Override
    @Transactional
    public void updateQueue( String name, String newCronExpression, List<String> newSequence )
    {
        Map<String, JobConfiguration> oldQueueJobs = getQueueJobConfigurationsById( name );
        //TODO check is not empty
        Map<String, JobConfiguration> newQueueJobs = getQueueJobConfigurationsById( newSequence );
        validateQueue( newQueueJobs.values() );
        for ( int pos = 0; pos < newSequence.size(); pos++ )
        {
            addToQueue( newQueueJobs.get( newSequence.get( pos ) ), name, pos, newCronExpression );
        }
        oldQueueJobs.entrySet().stream()
            .filter( e -> !newQueueJobs.containsKey( e.getKey() ) )
            .map( Map.Entry::getValue )
            .forEach( this::removeFromQueue );
    }

    @Override
    @Transactional
    public void deleteQueue( String name )
    {
        getQueueJobConfigurationsById( name ).values().forEach( this::removeFromQueue );
    }

    private void validateQueue( Collection<JobConfiguration> queue )
    {

    }

    private void addToQueue( JobConfiguration config, String name, int position, String cronExpression )
    {
        if ( config == null )
        {
            throw new IllegalQueryException( "No job with ID: " );
        }
        config.setQueueName( name );
        config.setQueuePosition( position );
        config.setCronExpression( position == 0 ? cronExpression : null );
        jobConfigurationStore.update( config );
    }

    private void removeFromQueue( JobConfiguration config )
    {
        config.setQueueName( null );
        config.setQueuePosition( null );
        config.setCronExpression( null );
        config.setEnabled( false );
        jobConfigurationStore.update( config );
    }

    private Map<String, JobConfiguration> getQueueJobConfigurationsById( String name )
    {
        return jobConfigurationStore.getAll().stream()
            .filter( c -> name.equals( c.getQueueName() ) )
            .collect( toMap( IdentifiableObject::getUid, Function.identity() ) );
    }

    private Map<String, JobConfiguration> getQueueJobConfigurationsById( List<String> sequence )
    {
        return jobConfigurationStore.getByUid( sequence ).stream()
            .collect( toMap( IdentifiableObject::getUid, Function.identity() ) );
    }
}
