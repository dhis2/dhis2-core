package org.hisp.dhis.metadata;

/**
 * What kind of {@link MetadataProposal} is made.
 *
 * @author Jan Bernitt
 */
public enum MetadataProposalType
{
    /**
     * Propose to add a new object
     */
    ADD,

    /**
     * Propose to update an existing object
     */
    UPDATE,

    /**
     * Propose to remove an existing object
     */
    REMOVE
}
