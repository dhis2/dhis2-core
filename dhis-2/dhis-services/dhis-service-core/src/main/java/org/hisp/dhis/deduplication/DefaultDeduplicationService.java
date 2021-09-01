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

import java.util.Date;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service( "org.hisp.dhis.deduplication.DeduplicationService" )
@RequiredArgsConstructor
public class DefaultDeduplicationService
    implements DeduplicationService
{
    private final PotentialDuplicateStore potentialDuplicateStore;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private final DeduplicationHelper deduplicationHelper;

    private final CurrentUserService currentUserService;

    private final AuditManager auditManager;

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
    @Transactional
    public void autoMerge( DeduplicationMergeParams params )
    {
        if ( !isAutoMergeable( params.getOriginal(), params.getDuplicate() ) )
            throw new PotentialDuplicateConflictException( "PotentialDuplicate can not be merged automatically." );

        params.setMergeObject( deduplicationHelper.generateMergeObject( params.getOriginal(), params.getDuplicate() ) );

        merge( params );
    }

    @Override
    @Transactional
    public void manualMerge( DeduplicationMergeParams deduplicationMergeParams )
    {
        if ( deduplicationHelper.hasInvalidReference( deduplicationMergeParams ) )
        {
            throw new PotentialDuplicateConflictException(
                "The submitted payload contains invalid references and cannot be merged." );
        }

        merge( deduplicationMergeParams );
    }

    private boolean isAutoMergeable( TrackedEntityInstance original, TrackedEntityInstance duplicate )
    {
        if ( enrolledSameProgram( original, duplicate ) )
        {
            return false;
        }

        if ( !original.getTrackedEntityType().equals( duplicate.getTrackedEntityType() ) )
        {
            return false;
        }

        if ( original.isDeleted() || duplicate.isDeleted() )
        {
            return false;
        }

        Set<TrackedEntityAttributeValue> trackedEntityAttributeValueA = original
            .getTrackedEntityAttributeValues();
        Set<TrackedEntityAttributeValue> trackedEntityAttributeValueB = duplicate
            .getTrackedEntityAttributeValues();

        return !sameAttributesAreEquals( trackedEntityAttributeValueA, trackedEntityAttributeValueB );
    }

    private void merge( DeduplicationMergeParams params )
    {
        TrackedEntityInstance original = params.getOriginal();
        TrackedEntityInstance duplicate = params.getDuplicate();
        MergeObject mergeObject = params.getMergeObject();

        if ( !deduplicationHelper.hasUserAccess( original, duplicate, mergeObject ) )
            throw new PotentialDuplicateForbiddenException(
                "You have insufficient access to merge the PotentialDuplicate." );

        potentialDuplicateStore.moveTrackedEntityAttributeValues( original, duplicate,
            mergeObject.getTrackedEntityAttributes() );
        potentialDuplicateStore.moveRelationships( original, duplicate,
            mergeObject.getRelationships() );
        potentialDuplicateStore.moveEnrollments( original, duplicate,
            mergeObject.getEnrollments() );

        potentialDuplicateStore.removeTrackedEntity( duplicate );
        updateTeiAndPotentialDuplicate( params, original );
    }

    private void updateTeiAndPotentialDuplicate( DeduplicationMergeParams deduplicationMergeParams,
        TrackedEntityInstance original )
    {
        updateOriginalTei( original );
        updatePotentialDuplicateStatus( deduplicationMergeParams.getPotentialDuplicate() );
    }

    private void updatePotentialDuplicateStatus( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicate.setStatus( DeduplicationStatus.MERGED );
        potentialDuplicateStore.update( potentialDuplicate );
    }

    private void updateOriginalTei( TrackedEntityInstance original )
    {
        original.setLastUpdated( new Date() );
        original.setLastUpdatedBy( currentUserService.getCurrentUser() );
        trackedEntityInstanceService.updateTrackedEntityInstance( original );
    }

    private boolean sameAttributesAreEquals( Set<TrackedEntityAttributeValue> trackedEntityAttributeValueA,
        Set<TrackedEntityAttributeValue> trackedEntityAttributeValueB )
    {
        if ( trackedEntityAttributeValueA.isEmpty() || trackedEntityAttributeValueB.isEmpty() )
        {
            return false;
        }

        for ( TrackedEntityAttributeValue teavA : trackedEntityAttributeValueA )
        {
            for ( TrackedEntityAttributeValue teavB : trackedEntityAttributeValueB )
            {
                if ( teavA.getAttribute().equals( teavB.getAttribute() )
                    && !teavA.getValue().equals( teavB.getValue() ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean enrolledSameProgram( TrackedEntityInstance trackedEntityInstanceA,
        TrackedEntityInstance trackedEntityInstanceB )
    {
        if ( !trackedEntityInstanceA.getProgramInstances().isEmpty()
            && !trackedEntityInstanceB.getProgramInstances().isEmpty() )
        {
            for ( ProgramInstance programInstanceA : trackedEntityInstanceA.getProgramInstances() )
            {
                for ( ProgramInstance programInstanceB : trackedEntityInstanceB.getProgramInstances() )
                {
                    if ( !programInstanceA.isDeleted()
                        && programInstanceA.getProgram().equals( programInstanceB.getProgram() ) )
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void addPotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicateStore.save( potentialDuplicate );
    }

}
