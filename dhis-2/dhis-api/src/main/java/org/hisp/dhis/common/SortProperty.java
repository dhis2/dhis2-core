package org.hisp.dhis.common;

public enum SortProperty
{
    NAME( "name" ), SHORT_NAME( "shortName");

    private String name;

    SortProperty( String name )
    {
        this.name = name;
    }

    public static SortProperty fromValue( String value )
    {
        for ( SortProperty type : SortProperty.values() )
        {
            if ( type.getName().equalsIgnoreCase( value ) )
            {
                return type;
            }
        }

        return null;
    }

    public String getName()
    {
        return name;
    }


}
