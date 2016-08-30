package org.hisp.dhis.webapi.documentation.controller.event;

import org.apache.http.HttpStatus;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.webapi.documentation.controller.AbstractWebApiTest;

/**
 * @author Viet Nguyen <viet@dhis.org>
 */
public class EventReportDocumentation
    extends AbstractWebApiTest<EventReport>
{
    @Override
    protected void setStatues()
    {
        createdStatus = HttpStatus.SC_CREATED;
    }
}

