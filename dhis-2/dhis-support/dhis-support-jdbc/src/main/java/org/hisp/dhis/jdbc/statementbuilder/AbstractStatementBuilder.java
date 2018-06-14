package org.hisp.dhis.jdbc.statementbuilder;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author Lars Helge Overland
 */
public abstract class AbstractStatementBuilder
    implements StatementBuilder
{
    static final String AZaz = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static final String AZaz09 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    static final String AZaz_QUOTED = QUOTE + AZaz + QUOTE;
    static final String AZaz09_QUOTED = QUOTE + AZaz09 + QUOTE;

    @Override
    public String encode( String value )
    {
        return encode( value, true );
    }

    @Override
    public String encode( String value, boolean quote )
    {
        if ( value != null )
        {
            value = value.endsWith( "\\" ) ? value.substring( 0, value.length() - 1 ) : value;
            value = value.replaceAll( QUOTE, QUOTE + QUOTE );
        }

        return quote ? (QUOTE + value + QUOTE) : value;
    }

    @Override
    public String columnQuote( String column )
    {
        String qte = getColumnQuote();
        
        column = column.replaceAll( qte, ( qte + qte ) );
        
        return column != null ? ( qte + column + qte ) : null;
    }

    @Override
    public String limitRecord( int offset, int limit )
    {
        return " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String getAutoIncrementValue()
    {
        return "null";
    }

    @Override
    public String getLongVarBinaryType()
    {
        return "VARBINARY(1000000)";
    }

    @Override
    public String concatenate( String... s )
    {
        return "CONCAT(" + StringUtils.join( s, ", " ) + ")";
    }

    @Override
    public String position( String substring, String string )
    {
        return ("POSITION(" + substring + " in " + string + ")");
    }

    @Override
    public String getUid()
    {
        return concatenate(
            getCharAt( AZaz_QUOTED , "1 + " + getRandom( AZaz.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ) );
    }

    @Override
    public String getNumberOfColumnsInPrimaryKey( String table )
    {
        return
            "select count(cu.column_name) from information_schema.key_column_usage cu " +
                "inner join information_schema.table_constraints tc  " +
                "on cu.constraint_catalog=tc.constraint_catalog " +
                "and cu.constraint_schema=tc.constraint_schema " +
                "and cu.constraint_name=tc.constraint_name " +
                "and cu.table_schema=tc.table_schema " +
                "and cu.table_name=tc.table_name " +
                "where tc.constraint_type='PRIMARY KEY' " +
                "and cu.table_name='" + table + "';";
    }
    
    @Override
    public String getCastToDate( String column )
    {
        return "cast(" + column + " as date)";
    }
    
    @Override
    public String getDaysBetweenDates( String fromColumn, String toColumn )
    {
        return "datediff(" + toColumn + ", " + fromColumn + ")";
    }

    @Override
    public String getDropPrimaryKey( String table )
    {
        return "alter table " + table + " drop primary key;";
    }

    @Override
    public String getAddPrimaryKeyToExistingTable( String table, String column )
    {
        return "alter table " + table + " add column " + column + " integer auto_increment primary key not null;";
    }

    @Override
    public String getDropNotNullConstraint( String table, String column, String type )
    {
        return "alter table " + table + " modify column " + column + " " + type + " null;";
    }

    /**
     * Generates a derived table containing one column of literal strings.
     *
     * The generic implementation, which works in all supported database
     * types, returns a subquery in the following form:
     * <code>
     *     (select 's1' as column
     *      union select 's2'
     *      union select 's3') table
     * </code>
     *
     * @param values (non-empty) String values for the derived table
     * @param table the desired table name alias
     * @param column the desired column name
     * @return the derived literal table
     */
    @Override
    public String literalStringTable( Collection<String> values, String table, String column )
    {
        StringBuilder sb = new StringBuilder();

        String before = "(select '";
        String after = "' as " + column;

        for ( String value : values )
        {
            sb.append( before ).append( value ).append( after );

            before = " union select '";
            after = "'";
        }

        return sb.append(") ").append( table ).toString();
    }

    /**
     * Generates a derived table containing literals in two columns: integer
     * and string.
     *
     * The generic implementation, which works in all supported database
     * types, returns a subquery in the following form:
     * <code>
     *     (select i1 as intColumn, 's1' as stringColumn
     *      union select i2, 's2'
     *      union select i3, 's3') table
     * </code>
     *
     * @param intValues (non-empty) Integer values for the derived table
     * @param strValues (same size) String values for the derived table
     * @param table the desired table name alias
     * @param intColumn the desired integer column name
     * @param strColumn the desired string column name
     * @return the derived literal table
     */
    @Override
    public String literalIntStringTable( List<Integer> intValues,
        List<String> strValues, String table, String intColumn, String strColumn )
    {
        StringBuilder sb = new StringBuilder();

        String before = "(select ";
        String afterInt = " as " + intColumn + ", '";
        String afterStr = "' as " + strColumn;

        for ( int i = 0; i < intValues.size(); i++ )
        {
            sb.append( before ).append( intValues.get( i ) ).append( afterInt )
                .append( strValues.get( i ) ).append( afterStr );
            before = " union select ";
            afterInt = ", '";
            afterStr = "'";
        }

        return sb.append( ") " ).append( table ).toString();
    }

    /**
     * Generates a derived table containing literals in two columns: integer
     * and integer.
     *
     * @param int1Values (non-empty) 1st integer column values for the table
     * @param int2Values (same size) 2nd integer column values for the table
     * @param table the desired table name alias
     * @param int1Column the desired 1st integer column name
     * @param int2Column the desired 2nd integer column name
     * @return the derived literal table
     *
     * The generic implementation, which works in all supported database
     * types, returns a subquery in the following form:
     * <code>
     *     (select i1_1 as int1Column, i2_1 as int2Column
     *      union select i1_2, i2_2
     *      union select i1_3, i2_3) table
     * </code>
     */
    @Override
    public String literalIntIntTable( List<Integer> int1Values,
        List<Integer> int2Values, String table, String int1Column, String int2Column )
    {
        StringBuilder sb = new StringBuilder();

        String before = "(select ";
        String afterInt1 = " as " + int1Column + ", ";
        String afterInt2 = " as " + int2Column;

        for ( int i = 0; i < int1Values.size(); i++ )
        {
            sb.append( before ).append( int1Values.get( i ) ).append( afterInt1 )
                .append( int2Values.get( i ) ).append( afterInt2 );
            before = " union select ";
            afterInt1 = ", ";
            afterInt2 = "";
        }

        return sb.append( ") " ).append( table ).toString();
    }

    @Override
    public boolean supportsPartialIndexes()
    {
        return false;
    }
    
    public String getProgramIndicatorDataValueSelectSql( String programStageUid, String dataElementUid, Date reportingStartDate,
        Date reportingEndDate, ProgramIndicator programIndicator )
    {
        if ( programIndicator.getAnalyticsType().equals( AnalyticsType.ENROLLMENT )  )
        {
            if( programIndicator.hasNonDefaultBoundaries() && programIndicator.hasEventBoundary() )
            {
                String eventTableName = "analytics_event_" + programIndicator.getProgram().getUid();
                String columnName = "\"" + dataElementUid + "\"";
                return "(select " + columnName + " from " + eventTableName + " where " + eventTableName +
                    ".pi = enrollmenttable.pi and " + columnName + " is not null " +
                    ( programIndicator.getEndEventBoundary() != null ? ("and " + 
                    getCohortBoundaryCondition( programIndicator.getEndEventBoundary(), reportingStartDate, reportingEndDate, programIndicator ) + 
                    " ") : "" ) + ( programIndicator.getStartEventBoundary() != null ? ( "and " + 
                        getCohortBoundaryCondition( programIndicator.getStartEventBoundary(), reportingStartDate, reportingEndDate, programIndicator ) +
                    " ") : "" ) + "and ps = '" + programStageUid + "' " + "order by executiondate " + "desc limit 1 )";
            }
            else
            {
                return this.columnQuote( programStageUid + ProgramIndicator.DB_SEPARATOR_ID + dataElementUid );
            }
        }
        else
        {
            return this.columnQuote( dataElementUid );
        }
    }
    
    private String getBoundaryElementColumnSql( AnalyticsPeriodBoundary boundary, Date reportingStartDate, Date reportingEndDate, ProgramIndicator programIndicator )
    {
        String columnSql = null;
        if ( boundary.isEnrollmentHavingEventDateCohortBoundary() )
        {
            Matcher matcher = AnalyticsPeriodBoundary.COHORT_HAVING_PROGRAM_STAGE_PATTERN.matcher( boundary.getBoundaryTarget() );
            Assert.isTrue( matcher.find(), "Can not parse program stage pattern for analyticsPeriodBoundary " + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
            String programStage = matcher.group( AnalyticsPeriodBoundary.PROGRAM_STAGE_REGEX_GROUP );
            Assert.isTrue( programStage != null, "Can not find programStage for analyticsPeriodBoundary " + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
            throw new NotImplementedException();
        }
        if ( boundary.isDataElementCohortBoundary() )
        {
            Matcher matcher = AnalyticsPeriodBoundary.COHORT_HAVING_DATA_ELEMENT_PATTERN.matcher( boundary.getBoundaryTarget() );
            Assert.isTrue( matcher.find(), "Can not parse data element pattern for analyticsPeriodBoundary " + boundary.getUid() + " - unknown boundaryTarget: " + boundary.getBoundaryTarget() );
            String programStage = matcher.group( AnalyticsPeriodBoundary.PROGRAM_STAGE_REGEX_GROUP );
            Assert.isTrue( programStage != null, "Can not find programStage for analyticsPeriodBoundary " + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
            String dataElement = matcher.group( AnalyticsPeriodBoundary.DATA_ELEMENT_REGEX_GROUP );
            Assert.isTrue( dataElement != null, "Can not find data element for analyticsPeriodBoundary " + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
            columnSql =  getProgramIndicatorDataValueSelectSql( programStage, dataElement, reportingStartDate, reportingEndDate, programIndicator );
        }
        if ( boundary.isAttributeCohortBoundary() )
        {
            Matcher matcher = AnalyticsPeriodBoundary.COHORT_HAVING_ATTRIBUTE_PATTERN.matcher( boundary.getBoundaryTarget() );
            Assert.isTrue( matcher.find(), "Can not parse attribute pattern for analyticsPeriodBoundary " + boundary.getUid() + " - unknown boundaryTarget: " + boundary.getBoundaryTarget() );
            String attribute = matcher.group( AnalyticsPeriodBoundary.ATTRIBUTE_REGEX_GROUP );
            Assert.isTrue( attribute != null, "Can not find attribute for analyticsPeriodBoundary " + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
            columnSql =  this.columnQuote( attribute );
        }
        Assert.isTrue( columnSql != null, "Can not determine boundary type for analyticsPeriodBoundary " + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
        return columnSql;
    }
    
    public String getCohortBoundaryCondition( AnalyticsPeriodBoundary boundary, Date reportingStartDate, Date reportingEndDate, ProgramIndicator programIndicator )
    {
        String column = boundary.isEventDateBoundary() ? AnalyticsPeriodBoundary.DB_EVENT_DATE : 
            boundary.isEnrollmentDateBoundary() ? AnalyticsPeriodBoundary.DB_ENROLLMENT_DATE : 
            boundary.isIncidentDateBoundary() ? AnalyticsPeriodBoundary.DB_INCIDENT_DATE : 
            this.getBoundaryElementColumnSql( boundary, reportingStartDate, reportingEndDate, programIndicator );
        
        final SimpleDateFormat format = new SimpleDateFormat();
        format.applyPattern( Period.DEFAULT_DATE_FORMAT );
        return column + " " + ( boundary.getAnalyticsPeriodBoundaryType().isEndBoundary() ? "<" : ">=" ) +
            " cast( '" + format.format( boundary.getBoundaryDate( reportingStartDate, reportingEndDate ) ) + "' as date )";
    }
}
