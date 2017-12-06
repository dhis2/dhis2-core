package org.hisp.dhis.textpattern;

import java.util.regex.Pattern;

public class TextMethodType
    extends BaseMethodType
{
    TextMethodType( Pattern patternRegex )
    {
        super( patternRegex );
    }

    @Override
    public boolean validateValue( String pattern, String value )
    {
        return value.equals( pattern );
    }
}
