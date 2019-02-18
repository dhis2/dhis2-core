package org.hisp.dhis.orgunitdistribution;

import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;

public interface OrgUnitDistributionServiceV2
{
    OrgUnitDistributionParams getParams( Set<String> orgUnits, Set<String> orgUnitGroupSets );

    Grid getOrgUnitDistribution( OrgUnitDistributionParams params );

    void validate( OrgUnitDistributionParams params )
        throws IllegalQueryException;
}
