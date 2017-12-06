package org.hisp.dhis.textpattern;

public interface MethodType
{
    enum RequiredStatus {
        REQUIRED,
        OPTIONAL,
        NONE
    }

    boolean validatePattern( String raw );

    boolean validateText( String format, String text );

    String getParam( String raw );

    String getValueRegex( String format );

    boolean isRequired();

    boolean isOptional();
}
