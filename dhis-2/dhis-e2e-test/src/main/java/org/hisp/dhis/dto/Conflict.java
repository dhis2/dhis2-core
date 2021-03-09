package org.hisp.dhis.dto;




/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class Conflict
{
    private String object;

    private String value;

    public String getObject()
    {
        return object;
    }

    public void setObject( String object )
    {
        this.object = object;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }
}
