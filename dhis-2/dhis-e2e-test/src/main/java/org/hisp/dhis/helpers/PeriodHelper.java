package org.hisp.dhis.helpers;

import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Helper class to assist with period and relative period manipulation.
 * 
 * @author maikel arabori
 */
public class PeriodHelper
{
    public enum Period
    {
        LAST_YEAR( "Last year" ),
        THIS_YEAR( "This year" ),
        LAST_6_MONTHS( "Last 6 months" ),
        LAST_12_MONTHS( "Last 12 months" ),
        LAST_5_YEARS( "Last 5 years" ),
        WEEKS_THIS_YEAR( "Weeks this year" ),
        TODAY( "Today" );

        private final String label;

        Period( String label )
        {
            this.label = label;
        }

        public String label()
        {
            return label;
        }
    }

    private PeriodHelper()
    {
        throw new UnsupportedOperationException( "helper" );
    }

    public static String getRelativePeriodDate( Period relativePeriod )
    {
        switch ( relativePeriod )
        {
        case LAST_YEAR:
            return now().plusYears( 1 ).format( BASIC_ISO_DATE );
        case LAST_5_YEARS:
            return now().plusYears( 5 ).format( BASIC_ISO_DATE );
        case LAST_12_MONTHS:
            return now().plusMonths( 12 ).format( BASIC_ISO_DATE );
        case LAST_6_MONTHS:
            return now().plusMonths( 6 ).format( BASIC_ISO_DATE );
        default:
            return EMPTY;
        }
    }
}
