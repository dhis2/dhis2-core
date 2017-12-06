package org.hisp.dhis.textpattern;

public interface MethodType
{
    boolean validatePattern( String raw );

    boolean validateValue( String pattern, String value );

    String getParam( String raw );
}
