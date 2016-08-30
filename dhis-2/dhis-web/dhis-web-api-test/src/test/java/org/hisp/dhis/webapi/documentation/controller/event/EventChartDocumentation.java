package org.hisp.dhis.webapi.documentation.controller.event;

import org.apache.http.HttpStatus;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.webapi.documentation.controller.AbstractWebApiTest;

/**
 * @author Viet Nguyen <viet@dhis.org>
 */
public class EventChartDocumentation
    extends AbstractWebApiTest<EventChart>
{
    @Override
    protected void setStatues()
    {
        createdStatus = HttpStatus.SC_CREATED;
    }
}

