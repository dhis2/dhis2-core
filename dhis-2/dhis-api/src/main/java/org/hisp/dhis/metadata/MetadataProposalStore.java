package org.hisp.dhis.metadata;

public interface MetadataProposalStore
{
    MetadataProposal getByUid( String uid );

    void save( MetadataProposal proposal );

    void update( MetadataProposal proposal );

    void delete( MetadataProposal proposal );
}
