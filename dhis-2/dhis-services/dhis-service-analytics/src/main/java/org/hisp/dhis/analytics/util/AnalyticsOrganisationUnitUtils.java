package org.hisp.dhis.analytics.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS_USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS_USER_ORGUNIT_GRANDCHILDREN;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS_USER_ORG_UNIT;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNITS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnalyticsOrganisationUnitUtils {
    public static Collection<Map<String, Object>> getUserOrganisationUnitsUidList(User currentUser, List<OrganisationUnit> organisationUnits) {
        List<Map<String, Object>> userOrganisations = new ArrayList<>();
        Map<String, Object> userOrganisationUnits = AnalyticsOrganisationUnitUtils
                .getUserOrganisationUnitUidList(ITEMS_USER_ORG_UNIT, currentUser, organisationUnits);
        if(userOrganisationUnits != null){
            userOrganisations.add(userOrganisationUnits);
        }

        Map<String, Object> userChildrenOrganisationUnits = AnalyticsOrganisationUnitUtils
                .getUserOrganisationUnitUidList(ITEMS_USER_ORGUNIT_CHILDREN, currentUser, organisationUnits);
        if(userChildrenOrganisationUnits != null){
            userOrganisations.add(userChildrenOrganisationUnits);
        }

        Map<String, Object> userGrandChildrenOrganisationUnits = AnalyticsOrganisationUnitUtils
                .getUserOrganisationUnitUidList(ITEMS_USER_ORGUNIT_GRANDCHILDREN, currentUser, organisationUnits);
        if(userGrandChildrenOrganisationUnits != null){
            userOrganisations.add(userGrandChildrenOrganisationUnits);
        }

        return userOrganisations;
    }
    private static Map<String, Object> getUserOrganisationUnitUidList(AnalyticsMetaDataKey analyticsMetaDataKey,
                                                               User currentUser, List<OrganisationUnit> organisationUnits) {
        List<String> paramOrgUidList = organisationUnits.stream().map(UidObject::getUid).toList();

        List<String> userOrgUnitList = new ArrayList<>();

        switch (analyticsMetaDataKey) {
            case ITEMS_USER_ORG_UNIT -> userOrgUnitList = currentUser.getOrganisationUnits().stream()
                    .map(BaseIdentifiableObject::getUid).toList();
            case ITEMS_USER_ORGUNIT_CHILDREN -> userOrgUnitList = currentUser.getOrganisationUnits().stream()
                    .flatMap(ou -> ou.getChildren().stream())
                    .map(BaseIdentifiableObject::getUid).toList();
            case ITEMS_USER_ORGUNIT_GRANDCHILDREN -> userOrgUnitList = currentUser.getOrganisationUnits()
                    .stream()
                    .flatMap(ou -> ou.getGrandChildren().stream())
                    .map(BaseIdentifiableObject::getUid).toList();
        }

        Collection<String> intersection = CollectionUtils.intersection(paramOrgUidList, userOrgUnitList);

        return intersection.isEmpty() ? null : Map.of(analyticsMetaDataKey.getKey(), Map.of(ORG_UNITS.getKey(), intersection));
    }
}
