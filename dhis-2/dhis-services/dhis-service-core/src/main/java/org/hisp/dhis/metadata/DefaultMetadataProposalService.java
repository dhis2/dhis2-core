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
package org.hisp.dhis.metadata;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class DefaultMetadataProposalService implements MetadataProposalService
{

    private final MetadataProposalStore store;

    private final CurrentUserService currentUserService;

    private final IdentifiableObjectManager objectManager;

    @Override
    @Transactional( readOnly = true )
    public MetadataProposal getByUid( String uid )
    {
        return store.getByUid( uid );
    }

    @Override
    @Transactional
    public MetadataProposal propose( MetadataProposalParams params )
    {
        MetadataProposal proposal = MetadataProposal.builder()
            .createdBy( currentUserService.getCurrentUser() )
            .type( params.getType() )
            .target( params.getTarget() )
            .targetUid( params.getTargetUid() )
            .comment( params.getComment() )
            .change( params.getChange() )
            .build();
        store.save( proposal );
        return proposal;
    }

    @Override
    @Transactional
    public void accept( MetadataProposal proposal )
    {
        switch ( proposal.getType() )
        {
        case ADD:
            acceptAdd( proposal );
            return;
        case REMOVE:
            acceptRemove( proposal );
            return;
        case UPDATE:
            acceptUpdate( proposal );
            return;
        }
        store.delete( proposal );
    }

    @Override
    @Transactional
    public void comment( MetadataProposal proposal, String comment )
    {
        proposal.setComment( comment );
        store.update( proposal );
    }

    @Override
    @Transactional
    public void reject( MetadataProposal proposal )
    {
        store.delete( proposal );
    }

    private void acceptAdd( MetadataProposal proposal )
    {

    }

    private void acceptUpdate( MetadataProposal proposal )
    {

    }

    private void acceptRemove( MetadataProposal proposal )
    {
        IdentifiableObject deleted = objectManager.get( proposal.getTargetUid() );
        objectManager.delete( deleted );
    }
}
