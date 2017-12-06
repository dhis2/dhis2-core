package org.hisp.dhis.textpattern;

import java.util.regex.Pattern;

public class GeneratedMethodType
    extends BaseMethodType
{

    GeneratedMethodType( Pattern patternRegex )
    {
        super( patternRegex );
    }

    @Override
    public boolean validateValue( String pattern, String value )
    {
        pattern = pattern.replaceAll( "#", "[0-9]" );
        pattern = pattern.replaceAll( "X", "[A-Z]" );
        pattern = pattern.replaceAll( "x", "[a-z]" );

        return Pattern.compile( pattern ).matcher( value ).matches();
    }
}
