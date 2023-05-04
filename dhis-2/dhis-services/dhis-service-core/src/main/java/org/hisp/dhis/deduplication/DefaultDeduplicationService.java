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
package org.hisp.dhis.deduplication;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
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

    private final DeduplicationHelper deduplicationHelper;

    private final CurrentUserService currentUserService;

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
    public boolean exists( PotentialDuplicate potentialDuplicate )
        throws PotentialDuplicateConflictException
    {
        return potentialDuplicateStore.exists( potentialDuplicate );
    }

    @Override
    @Transactional( readOnly = true )
    public List<PotentialDuplicate> getPotentialDuplicates( PotentialDuplicateCriteria criteria )
    {
        return potentialDuplicateStore.getPotentialDuplicates( criteria );
    }

    @Override
    @Transactional( readOnly = true )
    public int countPotentialDuplicates( PotentialDuplicateCriteria criteria )
    {
        return potentialDuplicateStore.getCountPotentialDuplicates( criteria );
    }

    @Override
    @Transactional
    public void updatePotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
        setPotentialDuplicateUserNameInfo( potentialDuplicate );
        potentialDuplicateStore.update( potentialDuplicate );
    }

    @Override
    @Transactional
    public void autoMerge( DeduplicationMergeParams params )
        throws PotentialDuplicateConflictException,
        PotentialDuplicateForbiddenException
    {
        String autoMergeConflicts = getAutoMergeConflictErrors( params.getOriginal(), params.getDuplicate() );

        if ( autoMergeConflicts != null )
        {
            throw new PotentialDuplicateConflictException(
                "PotentialDuplicate can not be merged automatically: " + autoMergeConflicts );
        }

        params.setMergeObject( deduplicationHelper.generateMergeObject( params.getOriginal(), params.getDuplicate() ) );

        merge( params );
    }

    @Override
    @Transactional
    public void manualMerge( DeduplicationMergeParams deduplicationMergeParams )
        throws PotentialDuplicateConflictException,
        PotentialDuplicateForbiddenException
    {
        String invalidReference = deduplicationHelper.getInvalidReferenceErrors( deduplicationMergeParams );
        if ( invalidReference != null )
        {
            throw new PotentialDuplicateConflictException(
                "Merging conflict: " + invalidReference );
        }

        merge( deduplicationMergeParams );
    }

    private String getAutoMergeConflictErrors( TrackedEntityInstance original, TrackedEntityInstance duplicate )
    {
        if ( !original.getTrackedEntityType().equals( duplicate.getTrackedEntityType() ) )
        {
            return "Entities have different Tracked Entity Types.";
        }

        if ( original.isDeleted() || duplicate.isDeleted() )
        {
            return "One or both entities have already been marked as deleted.";
        }

        if ( haveSameEnrollment( original.getEnrollments(), duplicate.getEnrollments() ) )
        {
            return "Both entities enrolled in the same program.";
        }

        Set<TrackedEntityAttributeValue> trackedEntityAttributeValueA = original
            .getTrackedEntityAttributeValues();
        Set<TrackedEntityAttributeValue> trackedEntityAttributeValueB = duplicate
            .getTrackedEntityAttributeValues();

        if ( sameAttributesAreEquals( trackedEntityAttributeValueA, trackedEntityAttributeValueB ) )
        {
            return "Entities have conflicting values for the same attributes.";
        }

        return null;
    }

    private void merge( DeduplicationMergeParams params )
        throws PotentialDuplicateForbiddenException
    {
        TrackedEntityInstance original = params.getOriginal();
        TrackedEntityInstance duplicate = params.getDuplicate();
        MergeObject mergeObject = params.getMergeObject();

        String accessError = deduplicationHelper.getUserAccessErrors( original, duplicate, mergeObject );

        if ( accessError != null )
        {
            throw new PotentialDuplicateForbiddenException(
                "Insufficient access: " + accessError );
        }

        potentialDuplicateStore.moveTrackedEntityAttributeValues( original, duplicate,
            mergeObject.getTrackedEntityAttributes() );
        potentialDuplicateStore.moveRelationships( original, duplicate,
            mergeObject.getRelationships() );
        potentialDuplicateStore.moveEnrollments( original, duplicate,
            mergeObject.getEnrollments() );

        potentialDuplicateStore.removeTrackedEntity( duplicate );
        updateTeiAndPotentialDuplicate( params, original );
        potentialDuplicateStore.auditMerge( params );
    }

    private boolean haveSameEnrollment( Set<Enrollment> originalEnrollments,
        Set<Enrollment> duplicateEnrollments )
    {
        Set<String> originalPrograms = originalEnrollments.stream()
            .filter( e -> !e.isDeleted() )
            .map( e -> e.getProgram().getUid() )
            .collect( Collectors.toSet() );
        Set<String> duplicatePrograms = duplicateEnrollments.stream()
            .filter( e -> !e.isDeleted() )
            .map( e -> e.getProgram().getUid() )
            .collect( Collectors.toSet() );

        originalPrograms.retainAll( duplicatePrograms );

        return !originalPrograms.isEmpty();
    }

    private void updateTeiAndPotentialDuplicate( DeduplicationMergeParams deduplicationMergeParams,
        TrackedEntityInstance original )
    {
        updateOriginalTei( original );
        updatePotentialDuplicateStatus( deduplicationMergeParams.getPotentialDuplicate() );
    }

    private void updatePotentialDuplicateStatus( PotentialDuplicate potentialDuplicate )
    {
        setPotentialDuplicateUserNameInfo( potentialDuplicate );
        potentialDuplicate.setStatus( DeduplicationStatus.MERGED );
        potentialDuplicateStore.update( potentialDuplicate );
    }

    private void updateOriginalTei( TrackedEntityInstance original )
    {
        original.setLastUpdated( new Date() );
        original.setLastUpdatedBy( currentUserService.getCurrentUser() );
        original.setLastUpdatedByUserInfo( UserInfoSnapshot.from( currentUserService.getCurrentUser() ) );
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

    @Override
    @Transactional
    public void addPotentialDuplicate( PotentialDuplicate potentialDuplicate )
        throws PotentialDuplicateConflictException
    {
        if ( potentialDuplicate.getStatus() != DeduplicationStatus.OPEN )
        {
            throw new PotentialDuplicateConflictException(
                String.format(
                    "Invalid status %s, creating potential duplicate is allowed using: %s",
                    potentialDuplicate.getStatus(), DeduplicationStatus.OPEN ) );
        }

        setPotentialDuplicateUserNameInfo( potentialDuplicate );
        potentialDuplicateStore.save( potentialDuplicate );
    }

    private void setPotentialDuplicateUserNameInfo( PotentialDuplicate potentialDuplicate )
    {
        if ( potentialDuplicate.getCreatedByUserName() == null )
        {
            potentialDuplicate.setCreatedByUserName( currentUserService.getCurrentUsername() );
        }

        potentialDuplicate.setLastUpdatedByUserName( currentUserService.getCurrentUsername() );
    }
}
