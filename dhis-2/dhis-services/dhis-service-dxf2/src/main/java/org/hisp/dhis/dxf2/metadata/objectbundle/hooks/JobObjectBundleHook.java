package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/**
 * Created by henninghakonsen on 28/08/2017.
 * Project: dhis-2.
 */
public class JobObjectBundleHook
    extends AbstractObjectBundleHook
{
    /*private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !Job.class.isInstance( object ) )
        {
            return;
        }

        Job job = handleJob( object );
        sessionFactory.getCurrentSession().save( job );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !Job.class.isInstance( object ) )
        {
            return;
        }

        Job job = handleJob( object );
        sessionFactory.getCurrentSession().saveOrUpdate( job );
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T persistedObject, ObjectBundle bundle )
    {
        schedulingManager.scheduleJob( (Job) persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        Job job = (Job) persistedObject;

        schedulingManager.stopJob( job.getKey() );
        schedulingManager.scheduleJob( job );
    }

    private Job handleJob( IdentifiableObject object )
    {
        Job job = (Job) object;
        job.setStatus( null );
        job.setKey( "" );
        job.setNextExecutionTime();

        return job;
    }*/
}
