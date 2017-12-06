package org.hisp.dhis.textpattern;

import java.util.regex.Pattern;

public class GeneratedMethodType
    extends BaseMethodType
{

    GeneratedMethodType( Pattern pattern, RequiredStatus requiredStatus )
    {
        super( pattern, requiredStatus );
    }

    @Override
    public String getValueRegex( String format )
    {
        format = format.replaceAll( "#", "[0-9]" );
        format = format.replaceAll( "X", "[A-Z]" );
        format = format.replaceAll( "x", "[a-z]" );
        return format;
    }
}
