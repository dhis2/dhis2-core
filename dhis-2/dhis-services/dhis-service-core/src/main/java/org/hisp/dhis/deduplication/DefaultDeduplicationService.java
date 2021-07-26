/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.deduplication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service( "org.hisp.dhis.deduplication.DeduplicationService" )
@RequiredArgsConstructor
public class DefaultDeduplicationService
    implements DeduplicationService
{
    private final PotentialDuplicateStore potentialDuplicateStore;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    @Override
    @Transactional( readOnly = true )
    public PotentialDuplicate getPotentialDuplicateById( long id )
    {
        return potentialDuplicateStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public PotentialDuplicate getPotentialDuplicateByUid( String uid )
    {
        return potentialDuplicateStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<PotentialDuplicate> getAllPotentialDuplicates()
    {
        return potentialDuplicateStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public boolean exists( PotentialDuplicate potentialDuplicate )
    {
        return potentialDuplicateStore.exists( potentialDuplicate );
    }

    @Override
    @Transactional( readOnly = true )
    public List<PotentialDuplicate> getAllPotentialDuplicatesBy( PotentialDuplicateQuery query )
    {
        return potentialDuplicateStore.getAllByQuery( query );
    }

    @Override
    @Transactional( readOnly = true )
    public List<PotentialDuplicate> getPotentialDuplicateByTei( String tei, DeduplicationStatus status )
    {
        return potentialDuplicateStore.getAllByTei( tei, status );
    }

    @Override
    @Transactional( readOnly = true )
    public int countPotentialDuplicates( PotentialDuplicateQuery query )
    {

        return potentialDuplicateStore.getCountByQuery( query );
    }

    @Override
    @Transactional
    public void updatePotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicateStore.update( potentialDuplicate );
    }

    @Override
    public boolean isAutoMergeable( PotentialDuplicate potentialDuplicate )
    {
        TrackedEntityInstance trackedEntityInstanceA = Optional.ofNullable( trackedEntityInstanceService
            .getTrackedEntityInstance( potentialDuplicate.getTeiA() ) )
            .orElseThrow( () -> new PotentialDuplicateException(
                "No tracked entity instance found with id '" + potentialDuplicate.getTeiA() + "'." ) );

        TrackedEntityInstance trackedEntityInstanceB = Optional.ofNullable( trackedEntityInstanceService
            .getTrackedEntityInstance( potentialDuplicate.getTeiB() ) )
            .orElseThrow( () -> new PotentialDuplicateException(
                "No tracked entity instance found with id '" + potentialDuplicate.getTeiB() + "'." ) );

        if ( !trackedEntityInstanceA.getProgramInstances().isEmpty()
            && !trackedEntityInstanceB.getProgramInstances().isEmpty() )
        {
            for ( ProgramInstance programInstanceA : trackedEntityInstanceA.getProgramInstances() )
            {
                for ( ProgramInstance programInstanceB : trackedEntityInstanceA.getProgramInstances() )
                {
                    if ( programInstanceA.getProgram().equals( programInstanceB.getProgram() ) )
                        return false;
                }
            }
        }

        if ( !trackedEntityInstanceA.getTrackedEntityType().equals( trackedEntityInstanceB.getTrackedEntityType() ) )
        {
            return false;
        }

        if ( trackedEntityInstanceA.isDeleted() || trackedEntityInstanceB.isDeleted() )
        {
            return false;
        }

        Set<TrackedEntityAttributeValue> trackedEntityAttributeValueA = trackedEntityInstanceA
            .getTrackedEntityAttributeValues();
        Set<TrackedEntityAttributeValue> trackedEntityAttributeValueB = trackedEntityInstanceB
            .getTrackedEntityAttributeValues();

        if ( trackedEntityAttributeValueA.isEmpty() || trackedEntityAttributeValueB.isEmpty() )
        {
            return true;
        }

        for ( TrackedEntityAttributeValue teavA : trackedEntityAttributeValueA )
        {
            for ( TrackedEntityAttributeValue teavB : trackedEntityAttributeValueB )
            {
                if ( teavA.getAttribute().equals( teavB.getAttribute() )
                    && !teavA.getValue().equals( teavB.getValue() ) )
                {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    @Transactional
    public void addPotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicateStore.save( potentialDuplicate );
    }
}
