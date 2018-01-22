package org.hisp.dhis.reservedvalue;

import org.hisp.dhis.textpattern.TextPattern;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ReservedValueService
{
    List<String> reserve( TextPattern textPattern, int numberOfReservations, Map<String, String> values, Date expires )
        throws Exception;

    boolean useReservedValue( TextPattern textPattern, String value );
}
