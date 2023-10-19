package org.hisp.dhis.analytics.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS_USER_ORG_UNIT;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNITS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnalyticsOrganisationUnitUtils {
    public static Map<String, Object> getUserOrganisationUnits(EventQueryParams eventQueryParams) {
        return Map.of(ITEMS_USER_ORG_UNIT.getKey(), Map.of(ORG_UNITS.getKey(),
                eventQueryParams.getOrganisationUnits().stream().map(UidObject::getUid).toList()));
    }

    public static Map<String, Object> getUserOrganisationUnits(DataQueryParams dataQueryParams) {
        return Map.of(ITEMS_USER_ORG_UNIT.getKey(), Map.of(ORG_UNITS.getKey(),
                dataQueryParams.getOrganisationUnits().stream().map(UidObject::getUid).toList()));
    }
}
