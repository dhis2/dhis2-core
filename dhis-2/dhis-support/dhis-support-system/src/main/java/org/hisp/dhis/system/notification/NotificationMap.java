package org.hisp.dhis.system.notification;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Henning Håkonsen
 */
public class NotificationMap
{
    private Map<JobType, Map<String, List<Notification>>> notificationsWithType;


    NotificationMap ()
    {
        notificationsWithType = new HashMap<>( );
        Arrays.stream( JobType.values() ).filter( JobType::isExecutable )
            .forEach( jobType -> notificationsWithType.put( jobType, new HashMap<>() ) );
    }

    public Map<JobType, Map<String, List<Notification>>> getNotifications( )
    {
        return notificationsWithType;
    }

    public List<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        return notificationsWithType.get( jobType ).get( jobId );
    }

    public Map<String, List<Notification>> getNotificationsWithType( JobType jobType )
    {
        return notificationsWithType.get( jobType );
    }

    public void add( JobConfiguration jobConfiguration, Notification notification )
    {
        String uid = jobConfiguration.getUid();

        Map<String, List<Notification>> uidNotifications = notificationsWithType.get( jobConfiguration.getJobType() );

        List<Notification> notifications;
        if ( uidNotifications.containsKey( uid ) )
        {
            notifications = uidNotifications.get( uid );
        }
        else
        {
            notifications = new ArrayList<>( );
        }

        notifications.add( notification );

        uidNotifications.put( uid, notifications );

        notificationsWithType.put( jobConfiguration.getJobType(), uidNotifications );

    }

    public void clear( JobConfiguration jobConfiguration )
    {
        notificationsWithType.get( jobConfiguration.getJobType() ).remove( jobConfiguration.getUid() );
    }
}
