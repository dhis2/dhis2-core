package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.SchedulingManager;

import java.util.List;

import static com.cronutils.model.CronType.QUARTZ;

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
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        JobConfiguration jobConfiguration = (JobConfiguration) object;

        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser parser = new CronParser( cronDefinition );
        Cron quartzCron = parser.parse( jobConfiguration.getCronExpression() );

        quartzCron.validate();

        return super.validate( object, bundle );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( object ) )
        {
            return;
        }

        JobConfiguration jobConfiguration = (JobConfiguration) persistedObject;
        schedulingManager.stopJob( jobConfiguration.getKey() );

        sessionFactory.getCurrentSession().saveOrUpdate( jobConfiguration );
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
