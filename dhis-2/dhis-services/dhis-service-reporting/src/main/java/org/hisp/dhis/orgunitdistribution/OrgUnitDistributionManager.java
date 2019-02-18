package org.hisp.dhis.orgunitdistribution;

import org.hisp.dhis.common.Grid;

public interface OrgUnitDistributionManager
{
    Grid getOrgUnitDistribution( OrgUnitDistributionParams params );
}
