package org.hisp.dhis.textpattern;

import java.util.regex.Pattern;

public class TextMethodType
    extends BaseMethodType
{
    TextMethodType( Pattern pattern )
    {
        super( pattern );
    }

    @Override
    public boolean validateText( String format, String text )
    {
        System.out.println(format + " :: " + text);
        return text.equals( format );
    }

    @Override
    public String getValueRegex( String format )
    {
        return ".*?";
    }
}
