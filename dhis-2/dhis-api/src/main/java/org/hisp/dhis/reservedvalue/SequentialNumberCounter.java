package org.hisp.dhis.reservedvalue;

public class SequentialNumberCounter
{
    private int id;

    private String ownerUID;

    private String key;

    private int counter;

    public SequentialNumberCounter()
    {

    }

    public SequentialNumberCounter( String ownerUID, String key, int counter )
    {
        this.ownerUID = ownerUID;
        this.key = key;
        this.counter = counter;
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

    public int getCounter()
    {
        return counter;
    }

    public void setCounter( int counter )
    {
        this.counter = counter;
    }
}
