package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.SchedulingManager;

/**
 * @author Henning Håkonsen
 */
public class JobObjectBundleHook
    extends AbstractObjectBundleHook
{
    private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( object ) )
        {
            return;
        }

        JobConfiguration job = (JobConfiguration) persistedObject;
        schedulingManager.stopJob( job.getKey() );

        sessionFactory.getCurrentSession().saveOrUpdate( job );
    }

    @Override
    public <T extends IdentifiableObject> void preDelete( T persistedObject, ObjectBundle bundle )
    {
        schedulingManager.stopJob( ((JobConfiguration) persistedObject).getKey() );
        sessionFactory.getCurrentSession().delete( persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T persistedObject, ObjectBundle bundle )
    {
        schedulingManager.scheduleJob( (JobConfiguration) persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        schedulingManager.scheduleJob( (JobConfiguration) persistedObject );
    }
}
