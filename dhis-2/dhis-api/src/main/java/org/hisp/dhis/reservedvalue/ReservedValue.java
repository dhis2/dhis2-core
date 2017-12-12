package org.hisp.dhis.reservedvalue;

import java.util.Date;

public class ReservedValue
{
    private int id;

    private String ownerUID;

    private String key;

    private String value;

    private Date expires;

    public ReservedValue( String ownerUID, String key, String value, Date expires )
    {
        this.ownerUID = ownerUID;
        this.key = key;
        this.value = value;
        this.expires = expires;
    }

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public String getOwnerUID()
    {
        return ownerUID;
    }

    public void setOwnerUID( String ownerUID )
    {
        this.ownerUID = ownerUID;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public Date getExpires()
    {
        return expires;
    }

    public void setExpires( Date expires )
    {
        this.expires = expires;
    }
}
