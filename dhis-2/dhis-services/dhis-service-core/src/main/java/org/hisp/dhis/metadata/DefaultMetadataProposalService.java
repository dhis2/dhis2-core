package org.hisp.dhis.metadata;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;

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
