package org.hisp.dhis.textpattern;

import java.util.regex.Pattern;

public class TextMethodType
    extends BaseMethodType
{
    TextMethodType( Pattern pattern, RequiredStatus requiredStatus )
    {
        super( pattern, requiredStatus );
    }

    @Override
    public boolean validateText( String format, String text )
    {
        return text.equals( format );
    }

    @Override
    public String getValueRegex( String format )
    {
        return ".*?";
    }
}
