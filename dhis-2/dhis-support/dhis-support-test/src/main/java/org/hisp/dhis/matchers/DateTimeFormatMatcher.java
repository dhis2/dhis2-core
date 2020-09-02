package org.hisp.dhis.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * @author Luciano Fiandesio
 */
public class DateTimeFormatMatcher extends TypeSafeMatcher<String>
{
    private final String format;

    public DateTimeFormatMatcher( String format )
    {
        this.format = format;
    }

    public boolean isValidFormat( String format, String value, Locale locale )
    {
        LocalDateTime ldt;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern( format, locale );

        try
        {
            ldt = LocalDateTime.parse( value, formatter );
            String result = ldt.format( formatter );
            return result.equals( value );
        }
        catch ( DateTimeParseException e )
        {
            try
            {
                LocalDate ld = LocalDate.parse( value, formatter );
                String result = ld.format( formatter );
                return result.equals( value );
            }
            catch ( DateTimeParseException exp )
            {
                try
                {
                    LocalTime lt = LocalTime.parse( value, formatter );
                    String result = lt.format( formatter );
                    return result.equals( value );
                }
                catch ( DateTimeParseException e2 )
                {
                    // Debugging purposes
                    // e2.printStackTrace();
                }
            }
        }

        return false;
    }

    @Override
    protected boolean matchesSafely( String value )
    {
        return isValidFormat( this.format, value, Locale.getDefault() );
    }

    @Override
    public void describeTo( Description description )
    {
        description.appendText("Invalid date format. Expected [" + format + "]");
    }

    public static Matcher<String> hasDateTimeFormat( String format )
    {
        return new DateTimeFormatMatcher( format );
    }

}
