package org.hisp.dhis.sms.listener;

import org.hisp.dhis.smscompression.models.Metadata;

import java.util.Date;

public class Metadata2 {
    public static class ID {
        String id;
    }

    public Date lastSyncDate;
    public ID[] users;
    public ID[] trackedEntityTypes;
    public ID[] trackedEntityAttributes;
    public ID[] programs;
    public ID[] organisationUnits;
}
