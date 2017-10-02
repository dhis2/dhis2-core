package org.hisp.dhis.analytics.validation.data;

import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.validation.ValidationResultAnalyticsService;

/**
 * @author Henning Håkonsen
 */
public class DefaultValidationResultAnalyticsService
    implements ValidationResultAnalyticsService
{
    private AnalyticsTableManager tableManager;

    public void setTableManager( AnalyticsTableManager tableManager )
    {
        this.tableManager = tableManager;
    }





}
