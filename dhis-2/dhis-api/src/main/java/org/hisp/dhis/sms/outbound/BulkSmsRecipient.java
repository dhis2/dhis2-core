package org.hisp.dhis.sms.outbound;

public class BulkSmsRecipient
{
    private String type = "INTERNATIONAL";
    private String address;

    public BulkSmsRecipient()
    {
    }

    public BulkSmsRecipient( String address )
    {
        this.address = address;
    }

    public String getType()
    {
        return type;
    }

    public String getAddress()
    {
        return address;
    }
}
