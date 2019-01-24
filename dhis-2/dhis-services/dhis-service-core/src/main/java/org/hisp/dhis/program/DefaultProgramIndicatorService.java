package org.hisp.dhis.program;

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


import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.commons.sqlfunc.SqlFunction;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.system.util.DateUtils;

import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * @author Chau Thu Tran
 */
public class DefaultProgramIndicatorService
    implements ProgramIndicatorService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramIndicatorStore programIndicatorStore;

    public void setProgramIndicatorStore( ProgramIndicatorStore programIndicatorStore )
    {
        this.programIndicatorStore = programIndicatorStore;
    }

    private ConstantService constantService;

    public void setConstantService( ConstantService constantService )
    {
        this.constantService = constantService;
    }

    private StatementBuilder statementBuilder;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    private IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore;

    public void setProgramIndicatorGroupStore(
        IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore )
    {
        this.programIndicatorGroupStore = programIndicatorGroupStore;
    }

    private ProgramIndicatorExpressionEvaluationService expressionEvaluationService;

    public void setExpressionEvaluationService( ProgramIndicatorExpressionEvaluationService expressionEvaluationService )
    {
        this.expressionEvaluationService = expressionEvaluationService;
    }

    // -------------------------------------------------------------------------
    // ProgramIndicatorService implementation
    // -------------------------------------------------------------------------

    @Transactional
    public int addProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.save( programIndicator );
        return programIndicator.getId();
    }

    @Transactional
    public void updateProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.update( programIndicator );
    }

    @Transactional
    public void deleteProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.delete( programIndicator );
    }

    @Transactional
    public ProgramIndicator getProgramIndicator( int id )
    {
        return programIndicatorStore.get( id );
    }

    @Transactional
    public ProgramIndicator getProgramIndicator( String name )
    {
        return programIndicatorStore.getByName( name );
    }

    @Transactional
    public ProgramIndicator getProgramIndicatorByUid( String uid )
    {
        return programIndicatorStore.getByUid( uid );
    }

    @Transactional
    public List<ProgramIndicator> getAllProgramIndicators()
    {
        return programIndicatorStore.getAll();
    }

    @Transactional
    public String getExpressionDescription( String expression )
    {
        return  expressionEvaluationService.getExpressionDescription( expression );
    }

    public String getAnalyticsSQl( String expression, ProgramIndicator programIndicator, Date startDate, Date endDate )
    {
        return getAnalyticsSQl( expression, programIndicator, true, startDate, endDate );
    }

    public String getAnalyticsSQl( String expression, ProgramIndicator programIndicator, boolean ignoreMissingValues,
        Date startDate, Date endDate )
    {
        if ( expression == null )
        {
            return null;
        }

        String sqlExpression = TextUtils.removeNewlines( expression );

        sqlExpression = getSubstitutedVariablesForAnalyticsSql( sqlExpression, programIndicator, startDate, endDate );

        sqlExpression = getSubstitutedFunctionsAnalyticsSql( sqlExpression, false, programIndicator, startDate, endDate );

        sqlExpression = getSubstitutedElementsAnalyticsSql( sqlExpression, ignoreMissingValues, programIndicator, startDate,
            endDate );

        return sqlExpression;
    }

    private String getSubstitutedFunctionsAnalyticsSql( String expression, boolean ignoreMissingValues,
        ProgramIndicator programIndicator, Date reportingStartDate, Date reportingEndDate )
    {
        if ( expression == null )
        {
            return null;
        }

        StringBuffer buffer = new StringBuffer();

        Matcher matcher = ProgramIndicator.SQL_FUNC_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String func = trim( matcher.group( "func" ) );
            String arguments = trim( matcher.group( "args" ) );

            if ( func != null && arguments != null )
            {
                String result = "";
                
                String[] args = arguments.split( ProgramIndicator.ARGS_SPLIT );

                ProgramD2Function piFunction = expressionEvaluationService.getD2Functions().get( func );
                
                if ( piFunction != null )
                {
                    result = piFunction.evaluate( programIndicator, statementBuilder, reportingStartDate, reportingEndDate, args );
                }
                else
                {
                    SqlFunction sqlFunction = expressionEvaluationService.getSQLFunctions().get( func );
    
                    if ( sqlFunction != null )
                    {
                        for ( int i = 0; i < args.length; i++ )
                        {
                            String arg = getSubstitutedElementsAnalyticsSql( trim( args[i] ), false, programIndicator,
                                reportingStartDate, reportingEndDate );
                            args[i] = arg;
                        }
                        
                        result = sqlFunction.evaluate( args );
                    }
                    else
                    {
                        throw new IllegalStateException( "Function not recognized: " + func );
                    }
                }

                matcher.appendReplacement( buffer, result );
            }
        }

        return TextUtils.appendTail( matcher, buffer );
    }

    private String getSubstitutedVariablesForAnalyticsSql( String expression, ProgramIndicator programIndicator,
        Date startDate, Date endDate )
    {
        if ( expression == null )
        {
            return null;
        }

        StringBuffer buffer = new StringBuffer();

        Matcher matcher = ProgramIndicator.VARIABLE_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String var = matcher.group( 1 );

            String sql = getVariableAsSql( var, expression, programIndicator.getAnalyticsType(), startDate, endDate );

            if ( sql != null )
            {
                matcher.appendReplacement( buffer, sql );
            }
        }

        return TextUtils.appendTail( matcher, buffer );
    }

    private String getSubstitutedElementsAnalyticsSql( String expression, boolean ignoreMissingValues,
        ProgramIndicator programIndicator, Date startDate, Date endDate )
    {
        if ( expression == null )
        {
            return null;
        }

        StringBuffer buffer = new StringBuffer();

        Matcher matcher = ProgramIndicator.EXPRESSION_EQUALSZEROOREMPTY_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String el1 = matcher.group( 2 );
            String el2 = matcher.group( 3 );
            boolean equalsZero = matcher.group( 4 ) != null && matcher.group( 4 ).matches( ProgramIndicator.EQUALSZERO );
            boolean equalsEmpty = matcher.group( 4 ) != null
                && matcher.group( 4 ).matches( ProgramIndicator.EQUALSEMPTY );

            if ( ProgramIndicator.KEY_DATAELEMENT.equals( key ) || ProgramIndicator.KEY_ATTRIBUTE.equals( key ) )
            {
                String columnName;

                if ( ProgramIndicator.KEY_DATAELEMENT.equals( key ) )
                {
                    columnName = statementBuilder.getProgramIndicatorDataValueSelectSql( el1, el2, startDate, endDate, programIndicator );
                }
                else
                // ProgramIndicator.KEY_ATTRIBUTE
                {
                    columnName = statementBuilder.columnQuote( el1 );
                }

                if ( equalsZero )
                {
                    columnName = getNumericIgnoreNullSql( columnName ) + " == 0 ";
                }
                else if ( equalsEmpty )
                {
                    columnName = getTextIgnoreNullSql( columnName ) + " == '' ";
                }
                else if ( ignoreMissingValues )
                {
                    columnName = getNumericIgnoreNullSql( columnName );
                }

                matcher.appendReplacement( buffer, columnName );
            }
            else if ( ProgramIndicator.KEY_CONSTANT.equals( key ) )
            {
                Constant constant = constantService.getConstant( el1 );

                if ( constant != null )
                {
                    matcher.appendReplacement( buffer, String.valueOf( constant.getValue() ) );
                }
            }
        }

        return TextUtils.appendTail( matcher, buffer );
    }

    public String getAnyValueExistsClauseAnalyticsSql( String expression, AnalyticsType analyticsType )
    {
        Set<String> uids = ProgramIndicator.getDataElementAndAttributeIdentifiers( expression, analyticsType );

        if ( uids.isEmpty() )
        {
            return null;
        }

        String sql = StringUtils.EMPTY;

        for ( String uid : uids )
        {
            sql += statementBuilder.columnQuote( uid ) + " is not null or ";
        }

        return TextUtils.removeLastOr( sql ).trim();
    }

    @Transactional
    public String expressionIsValid( String expression )
    {
        return expressionEvaluationService.isExpressionValid( expression );
    }

    @Transactional
    public String filterIsValid( String filter )
    {
        return expressionEvaluationService.isFilterExpressionValid( filter );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Creates a SQL select clause from the given program indicator variable
     * based on the given expression. Wraps the count variables with
     * <code>nullif</code> to avoid potential division by zero.
     *
     * @param var the program indicator variable.
     * @param expression the program indicator expression.
     * @return a SQL select clause.
     */
    private String getVariableAsSql( String var, String expression, AnalyticsType analyticsType, Date startDate,
        Date endDate )
    {
        final String dbl = statementBuilder.getDoubleColumnType();

        String variableColumnName = ProgramIndicator.getVariableColumnName( var );

        if ( ProgramIndicator.VAR_CURRENT_DATE.equals( var ) )
        {
            return "'" + DateUtils.getLongDateString() + "'";
        }
        else if ( ProgramIndicator.VAR_VALUE_COUNT.equals( var ) )
        {
            String sql = "nullif(cast((";

            for ( String uid : ProgramIndicator.getDataElementAndAttributeIdentifiers( expression, analyticsType ) )
            {
                sql += "case when " + statementBuilder.columnQuote( uid ) + " is not null then 1 else 0 end + ";
            }

            return TextUtils.removeLast( sql, "+" ).trim() + ") as " + dbl + "),0)";
        }
        else if ( ProgramIndicator.VAR_ZERO_POS_VALUE_COUNT.equals( var ) )
        {
            String sql = "nullif(cast((";

            for ( String uid : ProgramIndicator.getDataElementAndAttributeIdentifiers( expression, analyticsType ) )
            {
                sql += "case when " + statementBuilder.columnQuote( uid ) + " >= 0 then 1 else 0 end + ";
            }

            return TextUtils.removeLast( sql, "+" ).trim() + ") as " + dbl + "),0)";
        }
        else if ( ProgramIndicator.VAR_EVENT_COUNT.equals( var ) || ProgramIndicator.VAR_ENROLLMENT_COUNT.equals( var )
            || ProgramIndicator.VAR_TEI_COUNT.equals( var ) )
        {
            return "distinct " + variableColumnName;
        }
        else if ( ProgramIndicator.VAR_PROGRAM_STAGE_NAME.equals( var ) )
        {
            if ( AnalyticsType.EVENT == analyticsType )
            {
                return "(select name from programstage where uid = ps)";
            }
            else
            {
                return "''";
            }
        }
        else if ( ProgramIndicator.VAR_PROGRAM_STAGE_ID.equals( var ) )
        {
            if ( AnalyticsType.EVENT == analyticsType )
            {
                return variableColumnName;
            }
            else
            {
                return "''";
            }
        }
        else if ( ProgramIndicator.VAR_ANALYTICS_PERIOD_START.equals( var ) )
        {
            return "'" + DateUtils.getSqlDateString( startDate ) + "'";
        }
        else if ( ProgramIndicator.VAR_ANALYTICS_PERIOD_END.equals( var ) )
        {
            return "'" + DateUtils.getSqlDateString( endDate ) + "'";
        }

        return variableColumnName;
    }

    private String getNumericIgnoreNullSql( String column )
    {
        return "coalesce(" + column + "::numeric,0)";
    }

    private String getTextIgnoreNullSql( String column )
    {
        return "coalesce(" + column + ",'')";
    }

    // -------------------------------------------------------------------------
    // ProgramIndicatorGroup
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public int addProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.save( programIndicatorGroup );
        return programIndicatorGroup.getId();
    }

    @Override
    @Transactional
    public void updateProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.update( programIndicatorGroup );
    }

    @Override
    @Transactional
    public void deleteProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.delete( programIndicatorGroup );
    }

    @Override
    public ProgramIndicatorGroup getProgramIndicatorGroup( int id )
    {
        return programIndicatorGroupStore.get( id );
    }

    @Override
    public ProgramIndicatorGroup getProgramIndicatorGroup( String uid )
    {
        return programIndicatorGroupStore.getByUid( uid );
    }

    @Override
    public List<ProgramIndicatorGroup> getAllProgramIndicatorGroups()
    {
        return programIndicatorGroupStore.getAll();
    }
}
