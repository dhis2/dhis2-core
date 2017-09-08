package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.cronutils.model.CronType.QUARTZ;

/**
 * @author Henning Håkonsen
 */
public class JobObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Autowired
    private JobConfigurationService jobConfigurationService;

    private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        List<ErrorReport> errorReports = new ArrayList<>(  );

        JobConfiguration jobConfiguration = (JobConfiguration) object;

        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser parser = new CronParser( cronDefinition );
        Cron quartzCron = parser.parse( jobConfiguration.getCronExpression() );

        quartzCron.validate();


        CronDescriptor descriptor = CronDescriptor.instance( Locale.ENGLISH);
        String description = descriptor.describe(parser.parse( jobConfiguration.getCronExpression() ));
        System.out.println("Cron in natural language: " + description );

        List<JobConfiguration> jobConfigurations = jobConfigurationService.getAllJobConfigurations();
        Boolean sameJobSameCron = jobConfigurations.stream().anyMatch(( jobConfig -> (jobConfig.getJobType().equals( jobConfiguration.getJobType() ) && jobConfig.getCronExpression().equals( jobConfiguration.getCronExpression() ) ) ) );

        if(sameJobSameCron)
        {
            errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E4013) );
        }

        ErrorReport errorReport = jobConfiguration.getJobParameters().validate(quartzCron.retrieveFieldsAsMap());

        return errorReports;
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( object ) )
        {
            return;
        }

        JobConfiguration jobConfiguration = (JobConfiguration) persistedObject;
        schedulingManager.stopJob( jobConfiguration.getUid() );

        sessionFactory.getCurrentSession().saveOrUpdate( jobConfiguration );
    }

    @Override
    public <T extends IdentifiableObject> void preDelete( T persistedObject, ObjectBundle bundle )
    {
        schedulingManager.stopJob( ((JobConfiguration) persistedObject).getUid() );
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
