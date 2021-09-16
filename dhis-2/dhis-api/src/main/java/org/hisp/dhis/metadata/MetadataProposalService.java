package org.hisp.dhis.metadata;

public interface MetadataProposalService
{
    MetadataProposal getByUid( String uid );

    MetadataProposal propose( MetadataProposalParams proposal );

    void accept( MetadataProposal proposal );

    void comment( MetadataProposal proposal, String comment );

    void reject( MetadataProposal proposal );
}
