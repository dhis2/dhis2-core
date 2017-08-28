package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataset.DataInputPeriod;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by henninghakonsen on 28/08/2017.
 * Project: dhis-2.
 */
public class JobObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Autowired
    private SchedulingManager schedulingManager;

    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !DataInputPeriod.class.isInstance( object ) )
        {
            return;
        }


    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !DataInputPeriod.class.isInstance( object ) )
        {
            return;
        }


    }

    private void setJob( IdentifiableObject object )
    {
        Job job = (Job) object;
        job.setStatus( null );
        job.setKey( "" );
        job.setNextExecutionTime();

        schedulingManager.scheduleJob( job );
    }
}
