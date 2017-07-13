package org.hisp.dhis.dataset.notifications;

import org.hisp.dhis.notification.NotificationRecipient;

/**
 * Created by zubair on 26.06.17.
 */
public enum DataSetNotificationRecipient implements NotificationRecipient
{
    ORGANISATION_UNIT_CONTACT( true ),
    USER_GROUP( false );

    private boolean external;

    DataSetNotificationRecipient(boolean external )
    {
        this.external = external;
    }

    @Override
    public boolean isExternalRecipient()
    {
        return external;
    }
}
