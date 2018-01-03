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

    NotificationMap()
    {
        notificationsWithType = new HashMap<>();
        Arrays.stream( JobType.values() ).filter( JobType::isExecutable )
            .forEach( jobType -> notificationsWithType.put( jobType, new HashMap<>() ) );
    }

    public List<Notification> getLastNotificationsByJobType( JobType jobType )
    {
        Map<String, List<Notification>> jobTypeNotifications = notificationsWithType.get( jobType );

        String lastUid = "";
        Notification lastNotification = null;
        for (Map.Entry<String, List<Notification>> entry : jobTypeNotifications.entrySet()) {
            String uid = entry.getKey();
            List<Notification> list = entry.getValue();

            if ( lastNotification == null || list.get( list.size() ).getTime().after( lastNotification.getTime() ) )
            {
                lastUid = uid;
                lastNotification = list.get( list.size() );
            }
        }

        if ( lastUid.equals( "" ) )
        {
            return new ArrayList<>( );
        } else
        {
            return jobTypeNotifications.get( lastUid );
        }
    }

    public Map<JobType, Map<String, List<Notification>>> getNotifications( )
    {
        return notificationsWithType;
    }

    public List<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        if ( notificationsWithType.get( jobType ).containsKey( jobId ) )
        {
            return notificationsWithType.get( jobType ).get( jobId );
        }
        else
        {
            return new ArrayList<>( );
        }
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
