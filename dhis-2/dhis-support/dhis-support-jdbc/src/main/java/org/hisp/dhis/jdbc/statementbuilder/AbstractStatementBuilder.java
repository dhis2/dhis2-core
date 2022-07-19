/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.jdbc.statementbuilder;

import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_ENROLLMENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_EVENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_INCIDENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_SCHEDULED_DATE;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.util.Assert;

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
            value = value
                .replace( "\\", "\\\\" )
                .replace( QUOTE, QUOTE + QUOTE );
        }

        return quote ? (QUOTE + value + QUOTE) : value;
    }

    @Override
    public String columnQuote( String column )
    {
        String qte = getColumnQuote();

        column = column.replaceAll( qte, (qte + qte) );

        return qte + column + qte;
    }

    @Override
    public String limitRecord( int offset, int limit )
    {
        return " limit " + limit + " offset " + offset;
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
            getCharAt( AZaz_QUOTED, "1 + " + getRandom( AZaz.length() ) ),
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
        return "select count(cu.column_name) from information_schema.key_column_usage cu " +
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
     * The generic implementation, which works in all supported database types,
     * returns a subquery in the following form: <code>
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

        return sb.append( ") " ).append( table ).toString();
    }

    /**
     * Generates a derived table containing literals in two columns: integer and
     * string.
     *
     * The generic implementation, which works in all supported database types,
     * returns a subquery in the following form: <code>
     *     (select i1 as intColumn, 's1' as stringColumn
     *      union select i2, 's2'
     *      union select i3, 's3') table
     * </code>
     *
     * @param longValues (non-empty) Integer values for the derived table
     * @param strValues (same size) String values for the derived table
     * @param table the desired table name alias
     * @param longColumn the desired integer column name
     * @param strColumn the desired string column name
     * @return the derived literal table
     */
    @Override
    public String literalLongStringTable( List<Long> longValues, List<String> strValues, String table,
        String longColumn, String strColumn )
    {
        StringBuilder sb = new StringBuilder();

        String before = "(select ";
        String afterInt = " as " + longColumn + ", '";
        String afterStr = "' as " + strColumn;

        for ( int i = 0; i < longValues.size(); i++ )
        {
            sb.append( before ).append( longValues.get( i ) ).append( afterInt )
                .append( strValues.get( i ) ).append( afterStr );
            before = " union select ";
            afterInt = ", '";
            afterStr = "'";
        }

        return sb.append( ") " ).append( table ).toString();
    }

    /**
     * Generates a derived table containing literals in two columns: integer and
     * integer.
     *
     * @param long1Values (non-empty) 1st integer column values for the table
     * @param long2Values (same size) 2nd integer column values for the table
     * @param table the desired table name alias
     * @param long1Column the desired 1st integer column name
     * @param long2Column the desired 2nd integer column name
     * @return the derived literal table
     *
     *         The generic implementation, which works in all supported database
     *         types, returns a subquery in the following form: <code>
     *     (select i1_1 as int1Column, i2_1 as int2Column
     *      union select i1_2, i2_2
     *      union select i1_3, i2_3) table
     * </code>
     */
    @Override
    public String literalLongLongTable( List<Long> long1Values, List<Long> long2Values, String table,
        String long1Column, String long2Column )
    {
        StringBuilder sb = new StringBuilder();

        String before = "(select ";
        String afterInt1 = " as " + long1Column + ", ";
        String afterInt2 = " as " + long2Column;

        for ( int i = 0; i < long1Values.size(); i++ )
        {
            sb.append( before ).append( long1Values.get( i ) ).append( afterInt1 )
                .append( long2Values.get( i ) ).append( afterInt2 );
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

    @Override
    public String getProgramIndicatorDataValueSelectSql( String programStageUid, String dataElementUid,
        Date reportingStartDate, Date reportingEndDate, ProgramIndicator programIndicator )
    {
        String columnName = this.columnQuote( dataElementUid );
        if ( programIndicator.getAnalyticsType().equals( AnalyticsType.ENROLLMENT ) )
        {
            return getProgramIndicatorEventColumnSql( programStageUid, columnName,
                reportingStartDate, reportingEndDate, programIndicator );
        }
        else
        {
            return columnName;
        }
    }

    @Override
    public String getProgramIndicatorEventColumnSql( String programStageUid, String columnName, Date reportingStartDate,
        Date reportingEndDate, ProgramIndicator programIndicator )
    {
        if ( programIndicator.getAnalyticsType().equals( AnalyticsType.ENROLLMENT ) )
        {
            return getProgramIndicatorEventInEnrollmentSelectSql( columnName, programStageUid,
                reportingStartDate, reportingEndDate, programIndicator );
        }
        else
        {
            return columnName;
        }
    }

    @Override
    public String getProgramIndicatorEventColumnSql( String programStageUid, String stageOffset, String columnName,
        Date reportingStartDate, Date reportingEndDate, ProgramIndicator programIndicator )
    {
        if ( programIndicator.getAnalyticsType().equals( AnalyticsType.ENROLLMENT ) )
        {
            return getProgramIndicatorEventInEnrollmentSelectSql( columnName, stageOffset, programStageUid,
                reportingStartDate, reportingEndDate, programIndicator );
        }
        else
        {
            return columnName;
        }
    }

    private String getProgramIndicatorEventInEnrollmentSelectSql( String columnName, String programStageUid,
        Date reportingStartDate, Date reportingEndDate, ProgramIndicator programIndicator )
    {
        return getProgramIndicatorEventInEnrollmentSelectSql( columnName, "0", programStageUid, reportingStartDate,
            reportingEndDate, programIndicator );
    }

    private String getProgramIndicatorEventInEnrollmentSelectSql( String columnName, String stageOffset,
        String programStageUid, Date reportingStartDate, Date reportingEndDate, ProgramIndicator programIndicator )
    {
        String programStageCondition = "";
        if ( programStageUid != null && programStageUid.length() == 11 )
        {
            programStageCondition = "and ps = '" + programStageUid + "' ";
        }

        String eventTableName = "analytics_event_" + programIndicator.getProgram().getUid();
        return "(select " + columnName + " from " + eventTableName + " where " + eventTableName +
            ".pi = " + ANALYTICS_TBL_ALIAS + ".pi and " + columnName + " is not null "
            + (programIndicator.getEndEventBoundary() != null ? ("and "
                + getBoundaryCondition( programIndicator.getEndEventBoundary(), programIndicator, null,
                    reportingStartDate, reportingEndDate )
                + " ") : "")
            + (programIndicator.getStartEventBoundary() != null ? ("and " +
                getBoundaryCondition( programIndicator.getStartEventBoundary(), programIndicator, null,
                    reportingStartDate, reportingEndDate )
                + " ") : "")
            + programStageCondition + "order by executiondate " + createOrderTypeAndOffset( stageOffset )
            + " limit 1 )";
    }

    private String createOrderTypeAndOffset( String stageOffset )
    {
        int offset = Integer.parseInt( stageOffset );

        if ( offset == 0 )
        {
            return "desc";
        }
        if ( offset < 0 )
        {
            return "desc offset " + (-1 * offset);
        }
        else
        {
            return "asc offset " + (offset - 1);
        }
    }

    private String getBoundaryElementColumnSql( AnalyticsPeriodBoundary boundary, Date reportingStartDate,
        Date reportingEndDate, ProgramIndicator programIndicator )
    {
        String columnSql = null;
        if ( boundary.isDataElementCohortBoundary() )
        {
            Matcher matcher = AnalyticsPeriodBoundary.COHORT_HAVING_DATA_ELEMENT_PATTERN
                .matcher( boundary.getBoundaryTarget() );
            Assert.isTrue( matcher.find(), "Can not parse data element pattern for analyticsPeriodBoundary "
                + boundary.getUid() + " - unknown boundaryTarget: " + boundary.getBoundaryTarget() );
            String programStage = matcher.group( AnalyticsPeriodBoundary.PROGRAM_STAGE_REGEX_GROUP );
            Assert.isTrue( programStage != null, "Can not find programStage for analyticsPeriodBoundary "
                + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
            String dataElement = matcher.group( AnalyticsPeriodBoundary.DATA_ELEMENT_REGEX_GROUP );
            Assert.isTrue( dataElement != null, "Can not find data element for analyticsPeriodBoundary "
                + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
            columnSql = getCastToDate( getProgramIndicatorDataValueSelectSql( programStage, dataElement,
                reportingStartDate, reportingEndDate, programIndicator ) );
        }
        else if ( boundary.isAttributeCohortBoundary() )
        {
            Matcher matcher = AnalyticsPeriodBoundary.COHORT_HAVING_ATTRIBUTE_PATTERN
                .matcher( boundary.getBoundaryTarget() );
            Assert.isTrue( matcher.find(), "Can not parse attribute pattern for analyticsPeriodBoundary "
                + boundary.getUid() + " - unknown boundaryTarget: " + boundary.getBoundaryTarget() );
            String attribute = matcher.group( AnalyticsPeriodBoundary.ATTRIBUTE_REGEX_GROUP );
            Assert.isTrue( attribute != null, "Can not find attribute for analyticsPeriodBoundary " + boundary.getUid()
                + " - boundaryTarget: " + boundary.getBoundaryTarget() );
            columnSql = getCastToDate( this.columnQuote( attribute ) );
        }
        Assert.isTrue( columnSql != null, "Can not determine boundary type for analyticsPeriodBoundary "
            + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
        return columnSql;
    }

    @Override
    public String getBoundaryCondition( AnalyticsPeriodBoundary boundary, ProgramIndicator programIndicator,
        String timeField, Date reportingStartDate, Date reportingEndDate )
    {
        String column = boundary.isEventDateBoundary()
            ? Optional.ofNullable( timeField ).orElse( DB_EVENT_DATE )
            : boundary.isEnrollmentDateBoundary() ? DB_ENROLLMENT_DATE
                : boundary.isIncidentDateBoundary() ? DB_INCIDENT_DATE
                    : boundary.isScheduledDateBoundary() ? DB_SCHEDULED_DATE
                        : this.getBoundaryElementColumnSql( boundary, reportingStartDate, reportingEndDate,
                            programIndicator );

        final SimpleDateFormat format = new SimpleDateFormat();
        format.applyPattern( Period.DEFAULT_DATE_FORMAT );
        return addCompatibilityCondition( column ) + " "
            + (boundary.getAnalyticsPeriodBoundaryType().isEndBoundary() ? "<" : ">=") +
            " cast( '" + format.format( boundary.getBoundaryDate( reportingStartDate, reportingEndDate ) )
            + "' as date )";
    }

    /**
     * This method is needed to keep the logic/code backward compatible.
     * Previously, we didn't consider statuses, as we always based on the
     * "executiondate" only (it means ACTIVE and COMPLETED status).
     *
     * Now, we also need to support SCHEDULE status for events. For this reason
     * this method compares the status. If the column is "duedate", it means we
     * only want SCHEDULE status. In all other cases we assume any other status
     * different from SCHEDULE (which makes it backward compatible).
     *
     * @param column
     * @return
     */
    private String addCompatibilityCondition( final String column )
    {
        if ( !DB_SCHEDULED_DATE.equals( column ) )
        {
            return " psistatus != 'SCHEDULE' and " + column;
        }

        return " psistatus = 'SCHEDULE' and " + column;
    }
}
