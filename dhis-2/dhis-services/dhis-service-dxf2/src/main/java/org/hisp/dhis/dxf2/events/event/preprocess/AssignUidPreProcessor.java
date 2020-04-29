package org.hisp.dhis.dxf2.events.event.preprocess;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.UidGenerator;
import org.hisp.dhis.dxf2.events.event.context.WorkContext;

/**
 * This pre-processor assigns a UID to an Event and to the Event's note The UID
 * is assigned only if it's not null
 *
 */
public class AssignUidPreProcessor
    implements
    PreProcessor
{
    private final static UidGenerator uidGen = new UidGenerator();

    @Override
    public void process( Event event, WorkContext ctx )
    {
        uidGen.assignUidToEvent( event );
    }
}
