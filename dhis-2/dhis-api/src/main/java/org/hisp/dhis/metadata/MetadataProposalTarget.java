package org.hisp.dhis.metadata;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * The set of metadata object a {@link MetadataProposal} can be made for.
 *
 * @author Jan Bernitt
 */
public enum MetadataProposalTarget
{
    ORGANISATION_UNIT( OrganisationUnit.class );

    private Class<? extends IdentifiableObject> type;

    MetadataProposalTarget( Class<? extends IdentifiableObject> type )
    {
        this.type = type;
    }

    public Class<? extends IdentifiableObject> getType()
    {
        return type;
    }
}
