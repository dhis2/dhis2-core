package org.hisp.dhis.orgunitdistribution.impl;

import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionManager;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionParams;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultOrgUnitDistributionServiceV2
    implements OrgUnitDistributionServiceV2
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private OrgUnitDistributionManager distributionManager;

    @Override
    public OrgUnitDistributionParams getParams( Set<String> orgUnits, Set<String> orgUnitGroupSets )
    {
        return new OrgUnitDistributionParams()
            .setOrgUnits( idObjectManager.getObjects( OrganisationUnit.class, IdentifiableProperty.UID, orgUnits ) )
            .setOrgUnitGroupSets( idObjectManager.getObjects( OrganisationUnitGroupSet.class, IdentifiableProperty.UID, orgUnitGroupSets ) );
    }

    @Override
    public Grid getOrgUnitDistribution( OrgUnitDistributionParams params )
    {
        validate( params );

        return distributionManager.getOrgUnitDistribution( params );
    }

    @Override
    public void validate( OrgUnitDistributionParams params )
    {
        if ( params == null )
        {
            throw new IllegalQueryException( "Query cannot be null" );
        }

        if ( params.getOrgUnits().isEmpty() )
        {
            throw new IllegalQueryException( "At least one org unit must be specified" );
        }

        if ( params.getOrgUnitGroupSets().isEmpty() )
        {
            throw new IllegalQueryException( "At least one org unit group set must be specified" );
        }
    }
}
