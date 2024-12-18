package org.hisp.dhis.analytics.common;

import org.hisp.dhis.common.QueryItem;

public class CTEUtils {

    public static String createFilterName(QueryItem queryItem) {
        return "filter_" + getIdentifier(queryItem).replace('.', '_').toLowerCase();
    }

    public static String createFilterNameByIdentifier(String identifier) {
        return "filter_" + identifier.replace('.', '_').toLowerCase();
    }

    public static String getIdentifier(QueryItem queryItem) {
        String stage = queryItem.hasProgramStage() ? queryItem.getProgramStage().getUid() : "default";
        return stage + "." + queryItem.getItemId();
    }
}
