package org.hisp.dhis.deduplication;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class DefaultDeduplicationService
    implements DeduplicationService
{

    private final PotentialDuplicateStore potentialDuplicateStore;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    public DefaultDeduplicationService( PotentialDuplicateStore potentialDuplicateStore,
        TrackedEntityInstanceService trackedEntityInstanceService )
    {
        this.potentialDuplicateStore = potentialDuplicateStore;
        this.trackedEntityInstanceService = trackedEntityInstanceService;
    }

    @Override
    public long addPotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
            potentialDuplicateStore.save( potentialDuplicate );
            return potentialDuplicate.getId();
    }

    @Override
    public PotentialDuplicate getPotentialDuplicateById( long id )
    {
        return potentialDuplicateStore.get( id );
    }

    @Override
    public PotentialDuplicate getPotentialDuplicateByUid( String uid )
    {
        return potentialDuplicateStore.getByUid( uid );
    }

    @Override
    public List<PotentialDuplicate> getAllPotentialDuplicates()
    {
        return potentialDuplicateStore.getAll();
    }

    @Override
    public void markPotentialDuplicateInvalid( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicate.setStatus( DeduplicationStatus.INVALID );
        potentialDuplicateStore.update( potentialDuplicate );
    }

    @Override
    public int countPotentialDuplciates( PotentialDuplicateQuery query )
    {

        return potentialDuplicateStore.getCountByQuery( query );
    }

    @Override
    public boolean exists( PotentialDuplicate potentialDuplicate )
    {
        return potentialDuplicateStore.exists( potentialDuplicate );
    }

    @Override
    public List<PotentialDuplicate> getAllPotentialDuplicates( PotentialDuplicateQuery query )
    {
        return potentialDuplicateStore.getAllByQuery( query );
    }

    private void validatePotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {

        // Validate that teiA is present and a valid uid of an existing TEI
        if ( potentialDuplicate.getTeiA() == null )
        {
            throw new
        }

        if ( !CodeGenerator.isValidUid( potentialDuplicate.getTeiA() ) )
        {
            throw new WebMessageException(
                conflict( "'" + potentialDuplicate.getTeiA() + "' is not valid value for property 'teiA'" ) );
        }

        TrackedEntityInstance teiA = trackedEntityInstanceService
            .getTrackedEntityInstance( potentialDuplicate.getTeiA() );

        if ( teiA == null )
        {
            throw new WebMessageException(
                notFound( "No tracked entity instance found with id '" + potentialDuplicate.getTeiA() + "'." ) );
        }

        if ( !trackerAccessManager.canRead( currentUserService.getCurrentUser(), teiA ).isEmpty() )
        {
            throw new WebMessageException(
                forbidden( "You don't have read access to '" + potentialDuplicate.getTeiA() + "'." ) );
        }

        // Validate that teiB is a valid uid of an existing TEI if present
        if ( potentialDuplicate.getTeiB() != null )
        {
            if ( !CodeGenerator.isValidUid( potentialDuplicate.getTeiB() ) )
            {
                throw new WebMessageException(
                    conflict( "'" + potentialDuplicate.getTeiA() + "' is not valid value for property 'teiB'" ) );
            }

            TrackedEntityInstance teiB = trackedEntityInstanceService
                .getTrackedEntityInstance( potentialDuplicate.getTeiB() );

            if ( teiB == null )
            {
                throw new WebMessageException(
                    notFound( "No tracked entity instance found with id '" + potentialDuplicate.getTeiB() + "'." ) );
            }

            if ( !trackerAccessManager.canRead( currentUserService.getCurrentUser(), teiB ).isEmpty() )
            {
                throw new WebMessageException(
                    forbidden( "You don't have read access to '" + potentialDuplicate.getTeiB() + "'." ) );
            }
        }

        if ( deduplicationService.exists( potentialDuplicate ) )
        {
            {
                throw new WebMessageException(
                    conflict( "'" + potentialDuplicate.getTeiA() + "' " +
                        (potentialDuplicate.getTeiB() != null ? "and '" + potentialDuplicate.getTeiB() + "' " : "") +
                        "is already marked as a potential duplicate" ) );
            }
        }
    }
}
