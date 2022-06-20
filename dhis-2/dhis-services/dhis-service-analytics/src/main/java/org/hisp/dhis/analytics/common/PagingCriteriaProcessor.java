package org.hisp.dhis.analytics.common;

import org.hisp.dhis.common.AnalyticsPagingCriteria;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Processor class for AnalyticsPagingCriteria objects.
 * 
 * @see Processor
 */
@Component
@RequiredArgsConstructor
public class PagingCriteriaProcessor implements Processor<AnalyticsPagingCriteria>
{

    private final SystemSettingManager systemSettingManager;

    // TODO: DHIS2-13384 we would really like to have all
    // criteria/request/params to be
    // immutable, but PagingCriteria is not
    // returning it for now, should be converted to use builders
    public AnalyticsPagingCriteria process( final AnalyticsPagingCriteria pagingCriteria )
    {
        int analyticsMaxPageSize = systemSettingManager.getIntSetting( SettingKey.ANALYTICS_MAX_LIMIT );
        pagingCriteria.definePageSize( analyticsMaxPageSize );
        return pagingCriteria;
    }

}
