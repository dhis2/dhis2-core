package org.hisp.dhis.dataset.notification;

import org.hisp.dhis.notification.NotificationRecipient;

/**
 * Created by zubair on 26.06.17.
 */
public enum DataSetNotificationRecipients implements NotificationRecipient
{
    ORGANISATION_UNIT_CONTACT( true ),
    USERS_AT_ORGANISATION_UNIT( false ),
    USER_GROUP( false );

    private boolean external;

    DataSetNotificationRecipients( boolean external )
    {
        this.external = external;
    }

    @Override
    public boolean isExternalRecipient()
    {
        return external;
    }
}
