package org.hisp.dhis.analytics.orgunit;

import java.util.Map;

/**
 * @author Lars Helge Overland
 */
public interface OrgUnitAnalyticsManager
{
    /**
     * Returns a data map with a composite metadata key and an org unit count
     * as value for the given parameters.
     *
     * @param params the {@link OrgUnitQueryParams}.
     */
    Map<String, Integer> getOrgUnitData( OrgUnitQueryParams params );
}
