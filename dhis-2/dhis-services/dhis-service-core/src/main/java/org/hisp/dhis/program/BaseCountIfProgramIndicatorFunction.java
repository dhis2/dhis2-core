package org.hisp.dhis.program;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseCountIfProgramIndicatorFunction
    implements ProgramIndicatorFunction
{
    public static final String KEY = "countIf";

    public static final String PROGRAM_STAGE_REGEX_GROUP = "p";
    public static final String DATA_ELEMENT_REGEX_GROUP = "de";
    public static final String COHORT_HAVING_DATA_ELEMENT_REGEX = "(?<" + PROGRAM_STAGE_REGEX_GROUP + ">\\w{11})_(?<"+ DATA_ELEMENT_REGEX_GROUP + ">\\w{11})"; 
    public static final Pattern COHORT_HAVING_DATA_ELEMENT_PATTERN = Pattern.compile( COHORT_HAVING_DATA_ELEMENT_REGEX );

    public String countWhereCondition( ProgramIndicator programIndicator, Date reportingStartDate, Date reportingEndDate, String element, String condition )
    {   
        Matcher matcher = COHORT_HAVING_DATA_ELEMENT_PATTERN.matcher( element );
        
        if ( matcher.find() )
        {
            String ps = matcher.group( PROGRAM_STAGE_REGEX_GROUP );
            String de = matcher.group( DATA_ELEMENT_REGEX_GROUP );
            
            String eventTableName = "analytics_event_" + programIndicator.getProgram().getUid();
            String columnName = "\"" + de + "\"";
            return "(select count(" + columnName + ") from " + eventTableName + " where " + eventTableName +
                ".pi = enrollmenttable.pi and " + columnName + " is not null " +
                " and " + columnName + condition + " " +
                (programIndicator.getEndEventBoundary() != null ? ("and " + 
                programIndicator.getEndEventBoundary().getSqlCondition( reportingStartDate, reportingEndDate ) + 
                " ") : "") + (programIndicator.getStartEventBoundary() != null ? ("and " + 
                programIndicator.getStartEventBoundary().getSqlCondition( reportingStartDate, reportingEndDate ) +
                " ") : "") + "and ps = '" + ps + "')";
        }
        else
        {
            throw new IllegalArgumentException( "No data element found in argument 1:" + element + " in " + BaseCountIfProgramIndicatorFunction.KEY 
                + " for program indciator:" + programIndicator.getUid() );
        }
    }
}
