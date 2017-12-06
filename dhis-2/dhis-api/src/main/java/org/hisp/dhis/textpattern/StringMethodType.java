package org.hisp.dhis.textpattern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringMethodType
    implements MethodType
{
    private Pattern patternRegex;

    StringMethodType( Pattern patternRegex )
    {
        this.patternRegex = patternRegex;
    }

    @Override
    public boolean validatePattern( String raw )
    {
        return patternRegex.matcher( raw ).matches();
    }

    @Override
    public boolean validateValue( String pattern, String value )
    {
        return compilePattern( pattern ).matcher( value ).matches();
    }

    @Override
    public String getParam( String raw )
    {
        Matcher m = patternRegex.matcher( raw );

        if ( m.matches() )
        {
            return m.group( 1 );
        }

        return null;
    }

    private Pattern compilePattern( String valueFormat )
    {
        String value = valueFormat;
        value = value.replaceAll( "\\^", "" );
        value = value.replaceAll( "\\$", "" );
        return Pattern.compile( value );
    }
}
