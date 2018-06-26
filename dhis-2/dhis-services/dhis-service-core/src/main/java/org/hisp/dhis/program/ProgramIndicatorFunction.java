package org.hisp.dhis.program;

import java.util.Date;

public interface ProgramIndicatorFunction
{   
    String evaluate( ProgramIndicator programIndicator, Date reportingStartDate, Date reportingEndDate, String... args );
}
