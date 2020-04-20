package org.hisp.dhis.dxf2.events.event.preprocess;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.validation.WorkContext;

public class ImportOptionsPreProcessor
    implements
    PreProcessor
{

    @Override
    public void process( Event event, WorkContext ctx )
    {
        ImportOptions importOptions = ctx.getImportOptions();

        if ( importOptions.getUser() == null )
        {
            importOptions.setUser( ctx.getServiceDelegator().getCurrentUserService().getCurrentUser() );
        }
    }
}
