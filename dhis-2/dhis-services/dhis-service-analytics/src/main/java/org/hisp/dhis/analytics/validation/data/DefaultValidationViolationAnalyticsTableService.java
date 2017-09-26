package org.hisp.dhis.analytics.validation.data;

import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.validation.ValidationViolationAnalyticsTableService;

/**
 * @author Henning Håkonsen
 */
public class DefaultValidationViolationAnalyticsTableService
    implements ValidationViolationAnalyticsTableService
{
    private AnalyticsTableManager tableManager;

    public void setTableManager( AnalyticsTableManager tableManager )
    {
        this.tableManager = tableManager;
    }




}
