package org.hisp.dhis.program;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseCountIfProgramIndicatorFunction
    implements ProgramIndicatorFunction
{
    public static final String KEY = "countIf";

    public static final String PROGRAM_STAGE_REGEX_GROUP = "p";
    public static final String DATA_ELEMENT_REGEX_GROUP = "de";
    public static final String COHORT_HAVING_DATA_ELEMENT_REGEX = "#\\{(?<" + PROGRAM_STAGE_REGEX_GROUP + ">\\w{11}).(?<"+ DATA_ELEMENT_REGEX_GROUP + ">\\w{11})\\}"; 
    public static final Pattern COHORT_HAVING_DATA_ELEMENT_PATTERN = Pattern.compile( COHORT_HAVING_DATA_ELEMENT_REGEX );
    public static final String CONDITION_STRING = "[=<>!].*";
    public static final Pattern CONDITION_PATTERN = Pattern.compile( CONDITION_STRING );

    public String countWhereCondition( ProgramIndicator programIndicator, Date reportingStartDate, Date reportingEndDate, List<String> elementAndConditions )
    {   
        String sqlCondition = "";
        String programStage = null;
        for ( String elementAndCondition : elementAndConditions )
        {
            Matcher dataElementMatcher = COHORT_HAVING_DATA_ELEMENT_PATTERN.matcher( elementAndCondition );
            if ( dataElementMatcher.find() )
            {
                String ps = dataElementMatcher.group( PROGRAM_STAGE_REGEX_GROUP );
                if( programStage == null )
                {
                    programStage = ps;
                }
                else if ( !programStage.equals( ps ) )
                {
                    throw new IllegalArgumentException( "One " + BaseCountIfProgramIndicatorFunction.KEY +
                     " can only target one program stage. Error in program indciator:" + programIndicator.getUid() );
                }

                Matcher conditionMatcher = CONDITION_PATTERN.matcher( elementAndCondition ); 
                if ( conditionMatcher.find() )
                {
                    String de = dataElementMatcher.group( DATA_ELEMENT_REGEX_GROUP );
                    String con = conditionMatcher.group();
                
                    String columnName = "\"" + de + "\"";
                    if ( sqlCondition.length() > 0 )
                    {
                        sqlCondition += " and";
                    }
                    sqlCondition += " "  + columnName + " is not null " +
                        " and " + columnName + con + " " ;
                }
            }
            else
            {
                throw new IllegalArgumentException( "No data element found in argument 1:" + elementAndCondition + " in " + BaseCountIfProgramIndicatorFunction.KEY 
                    + " for program indciator:" + programIndicator.getUid() );
            }
        }

        if ( sqlCondition.length() > 0 )
        {
            String eventTableName = "analytics_event_" + programIndicator.getProgram().getUid();

            return "(select count(*) from " + eventTableName + " where " + eventTableName +
                ".pi = enrollmenttable.pi and " +
                sqlCondition +
                //+ columnName + " is not null " +
                //" and " + columnName + condition + " " +
                (programIndicator.getEndEventBoundary() != null ? ("and " + 
                programIndicator.getEndEventBoundary().getSqlCondition( reportingStartDate, reportingEndDate ) + 
                " ") : "") + (programIndicator.getStartEventBoundary() != null ? ("and " + 
                programIndicator.getStartEventBoundary().getSqlCondition( reportingStartDate, reportingEndDate ) +
                " ") : "") + "and ps = '" + programStage + "')";
        }
        else
        {
            throw new IllegalArgumentException( "No valid conditions found in " + BaseCountIfProgramIndicatorFunction.KEY 
                + " for program indciator:" + programIndicator.getUid() );
        }
    }
}
