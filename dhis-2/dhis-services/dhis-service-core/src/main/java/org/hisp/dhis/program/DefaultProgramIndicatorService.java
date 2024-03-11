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

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.sqlfunc.ConditionalSqlFunction;
import org.hisp.dhis.commons.sqlfunc.OneIfZeroOrPositiveSqlFunction;
import org.hisp.dhis.commons.sqlfunc.OperatorLogicalNot;
import org.hisp.dhis.commons.sqlfunc.RelationshipCountSqlFunction;
import org.hisp.dhis.commons.sqlfunc.SqlFunction;
import org.hisp.dhis.commons.sqlfunc.ZeroIfNegativeSqlFunction;
import org.hisp.dhis.commons.sqlfunc.ZeroPositiveValueCountFunction;
import org.hisp.dhis.commons.sqlfunc.HasValueSqlFunction;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * @author Chau Thu Tran
 */
public class DefaultProgramIndicatorService
    implements ProgramIndicatorService
{

    private static final Map<String, SqlFunction> SQL_FUNC_MAP = ImmutableMap.<String, SqlFunction> builder()
        .put( ZeroIfNegativeSqlFunction.KEY, new ZeroIfNegativeSqlFunction() )
        .put( OneIfZeroOrPositiveSqlFunction.KEY, new OneIfZeroOrPositiveSqlFunction() )
        .put( ZeroPositiveValueCountFunction.KEY, new ZeroPositiveValueCountFunction() )
        .put( ConditionalSqlFunction.KEY, new ConditionalSqlFunction() )
        .put( HasValueSqlFunction.KEY, new HasValueSqlFunction() )
        .put( OperatorLogicalNot.KEY, new OperatorLogicalNot() )
        .put( RelationshipCountSqlFunction.KEY, new RelationshipCountSqlFunction() ).build();
    
    private static final Map<String, ProgramIndicatorFunction> PI_FUNC_MAP = ImmutableMap.<String, ProgramIndicatorFunction> builder()
        .put( CountIfValueProgramIndicatorFunction.KEY, new CountIfValueProgramIndicatorFunction() )
        .put( CountProgramIndicatorFunction.KEY, new CountProgramIndicatorFunction() )
        .put( CountIfConditionProgramIndicatorFunction.KEY, new CountIfConditionProgramIndicatorFunction() )
        .put( DaysBetweenProgramIndicatorFunction.KEY, new DaysBetweenProgramIndicatorFunction() )
        .put( WeeksBetweenProgramIndicatorFunction.KEY, new WeeksBetweenProgramIndicatorFunction() )
        .put( MonthsBetweenProgramIndicatorFunction.KEY, new MonthsBetweenProgramIndicatorFunction() )
        .put( YearsBetweenProgramIndicatorFunction.KEY, new YearsBetweenProgramIndicatorFunction() )
        .put( MinutesBetweenProgramIndicatorFunction.KEY, new MinutesBetweenProgramIndicatorFunction() ).build();

    private static final Map<String, String> VARIABLE_SAMPLE_VALUE_MAP = ImmutableMap.<String, String> builder()
        .put( ProgramIndicator.VAR_COMPLETED_DATE, "'2017-07-08'" )
        .put( ProgramIndicator.VAR_CURRENT_DATE, "'2017-07-08'" )
        .put( ProgramIndicator.VAR_DUE_DATE, "'2017-07-08'" )
        .put( ProgramIndicator.VAR_ENROLLMENT_COUNT, "1" )
        .put( ProgramIndicator.VAR_ENROLLMENT_DATE, "'2017-07-08'" )
        .put( ProgramIndicator.VAR_ENROLLMENT_STATUS, "'COMPLETED'" )
        .put( ProgramIndicator.VAR_EVENT_COUNT, "1" )
        .put( ProgramIndicator.VAR_EVENT_DATE, "'2017-07-08'" )
        .put( ProgramIndicator.VAR_EXECUTION_DATE, "'2017-07-08'" )
        .put( ProgramIndicator.VAR_INCIDENT_DATE, "'2017-07-08'" )
        .put( ProgramIndicator.VAR_ANALYTICS_PERIOD_START, "'2017-07-01'" )
        .put( ProgramIndicator.VAR_PROGRAM_STAGE_ID, "'WZbXY0S00lP'" )
        .put( ProgramIndicator.VAR_PROGRAM_STAGE_NAME, "'First antenatal care visit'" )
        .put( ProgramIndicator.VAR_TEI_COUNT, "1" )
        .put( ProgramIndicator.VAR_VALUE_COUNT, "1" )
        .put( ProgramIndicator.VAR_ZERO_POS_VALUE_COUNT, "1" )
        .put( ProgramIndicator.VAR_ANALYTICS_PERIOD_END, "'2017-07-07'" ).build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramIndicatorStore programIndicatorStore;

    public void setProgramIndicatorStore( ProgramIndicatorStore programIndicatorStore )
    {
        this.programIndicatorStore = programIndicatorStore;
    }

    private ProgramStageService programStageService;

    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private TrackedEntityAttributeService attributeService;

    public void setAttributeService( TrackedEntityAttributeService attributeService )
    {
        this.attributeService = attributeService;
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

    @Autowired
    private I18nManager i18nManager;

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
        if ( expression == null )
        {
            return null;
        }

        I18n i18n = i18nManager.getI18n();

        StringBuffer description = new StringBuffer();

        Matcher matcher = ProgramIndicator.EXPRESSION_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String uid = matcher.group( 2 );

            if ( ProgramIndicator.KEY_DATAELEMENT.equals( key ) )
            {
                String de = matcher.group( 3 );

                ProgramStage programStage = programStageService.getProgramStage( uid );
                DataElement dataElement = dataElementService.getDataElement( de );

                if ( programStage != null && dataElement != null )
                {
                    String programStageName = programStage.getDisplayName();
                    String dataelementName = dataElement.getDisplayName();

                    matcher.appendReplacement( description, programStageName + ProgramIndicator.SEPARATOR_ID
                        + dataelementName );
                }
            }
            else if ( ProgramIndicator.KEY_ATTRIBUTE.equals( key ) )
            {
                TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( uid );

                if ( attribute != null )
                {
                    matcher.appendReplacement( description, attribute.getDisplayName() );
                }
            }
            else if ( ProgramIndicator.KEY_CONSTANT.equals( key ) )
            {
                Constant constant = constantService.getConstant( uid );

                if ( constant != null )
                {
                    matcher.appendReplacement( description, constant.getDisplayName() );
                }
            }
            else if ( ProgramIndicator.KEY_PROGRAM_VARIABLE.equals( key ) )
            {
                String varName = i18n.getString( uid );

                if ( varName != null )
                {
                    matcher.appendReplacement( description, varName );
                }
            }
        }

        matcher.appendTail( description );

        return description.toString();
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
        
        sqlExpression = getSubstitutedFunctionsAnalyticsSql( sqlExpression, false, programIndicator, startDate, endDate );

        sqlExpression = getSubstitutedVariablesForAnalyticsSql( sqlExpression, programIndicator, startDate, endDate, expression );

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
            String logicalNot = trim( matcher.group( 1 ) );

            if ( func != null && arguments != null )
            {
                String result = "";
                
                String[] args = arguments.split( ProgramIndicator.ARGS_SPLIT );

                ProgramIndicatorFunction piFunction = PI_FUNC_MAP.get( func );
                
                if ( piFunction != null )
                {
                    result = piFunction.evaluate( programIndicator, statementBuilder, reportingStartDate, reportingEndDate, args );
                }
                else
                {
                    SqlFunction sqlFunction = SQL_FUNC_MAP.get( func );
    
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

            if ( logicalNot != null )
            {
                SqlFunction function = SQL_FUNC_MAP.get( logicalNot );

                if ( function == null )
                {
                    throw new IllegalStateException( "Character not recognized: " + logicalNot );
                }

                String result = function.evaluate( null );

                buffer.insert( 0, result );
            }
        }

        return TextUtils.appendTail( matcher, buffer );
    }

    private String getSubstitutedVariablesForAnalyticsSql( String expression, ProgramIndicator programIndicator,
        Date startDate, Date endDate, String originalExpression )
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

            String sql = getVariableAsSql( var, expression, programIndicator.getAnalyticsType(), startDate, endDate, programIndicator, originalExpression );

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
                ValueType valueType = ValueType.TEXT;

                if ( ProgramIndicator.KEY_DATAELEMENT.equals( key ) )
                {
                    columnName = statementBuilder.getProgramIndicatorDataValueSelectSql( el1, el2, startDate, endDate, programIndicator );

                    DataElement dataElement = dataElementService.getDataElement( el2 );
                    Assert.assertNotNull( dataElement );
                    valueType = dataElement.getValueType();
                }
                else
                // ProgramIndicator.KEY_ATTRIBUTE
                {
                    columnName = statementBuilder.columnQuote( el1 );
                    TrackedEntityAttribute trackedEntityAttribute = attributeService.getTrackedEntityAttribute( el1 );
                    Assert.assertNotNull( trackedEntityAttribute );
                    valueType = trackedEntityAttribute.getValueType();
                }

                if ( equalsZero )
                {
                    columnName = getIgnoreNullSql( columnName, ValueType.NUMBER ) + " == 0 ";
                }
                else if ( equalsEmpty )
                {
                    columnName = getIgnoreNullSql( columnName, ValueType.TEXT ) + " == '' ";
                }
                else if ( ignoreMissingValues )
                {
                    columnName = getIgnoreNullSql( columnName, valueType );
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
        String expr = getSubstitutedSQLFunc( getSubstitutedExpression( expression ) );

        if ( ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION.equals( expr )
            || ProgramIndicator.UNKNOWN_VARIABLE.equals( expr ) )
        {
            return expr;
        }

        if ( !ExpressionUtils.isValid( expr, null ) )
        {
            return ProgramIndicator.EXPRESSION_NOT_VALID;
        }

        return ProgramIndicator.VALID;
    }

    @Transactional
    public String filterIsValid( String filter )
    {
        String expr = getSubstitutedSQLFunc( getSubstitutedExpression( filter ) );

        if ( ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION.equals( expr )
            || ProgramIndicator.UNKNOWN_VARIABLE.equals( expr ) )
        {
            return expr;
        }

        if ( !ExpressionUtils.isBoolean( expr, null ) )
        {
            return ProgramIndicator.FILTER_NOT_EVALUATING_TO_TRUE_OR_FALSE;
        }

        return ProgramIndicator.VALID;
    }

    /**
     * Generates an expression where all items are substituted with a sample
     * value in order to maintain a valid expression syntax.
     *
     * @param expression the expression.
     */
    private String getSubstitutedExpression( String expression )
    {
        StringBuffer expr = new StringBuffer();

        Matcher matcher = ProgramIndicator.EXPRESSION_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String uid = matcher.group( 2 );

            if ( ProgramIndicator.KEY_DATAELEMENT.equals( key ) )
            {
                String de = matcher.group( 3 );

                ProgramStage programStage = programStageService.getProgramStage( uid );
                DataElement dataElement = dataElementService.getDataElement( de );

                if ( programStage != null && dataElement != null )
                {
                    String sample = ValidationUtils.getSubstitutionValue( dataElement.getValueType() );

                    matcher.appendReplacement( expr, sample );
                }
                else
                {
                    return ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION;
                }
            }
            else if ( ProgramIndicator.KEY_ATTRIBUTE.equals( key ) )
            {
                TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( uid );

                if ( attribute != null )
                {
                    String sample = ValidationUtils.getSubstitutionValue( attribute.getValueType() );

                    matcher.appendReplacement( expr, sample );
                }
                else
                {
                    return ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION;
                }
            }
            else if ( ProgramIndicator.KEY_CONSTANT.equals( key ) )
            {
                Constant constant = constantService.getConstant( uid );

                if ( constant != null )
                {
                    matcher.appendReplacement( expr, String.valueOf( constant.getValue() ) );
                }
                else
                {
                    return ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION;
                }
            }
            else if ( ProgramIndicator.KEY_PROGRAM_VARIABLE.equals( key ) )
            {
                String sampleValue = VARIABLE_SAMPLE_VALUE_MAP.get( uid );

                if ( sampleValue != null )
                {
                    matcher.appendReplacement( expr, sampleValue );
                }
                else
                {
                    return ProgramIndicator.UNKNOWN_VARIABLE;
                }
            }
        }

        matcher.appendTail( expr );
        
        return expr.toString();
    }
    
    /**
     * Generates an expression where all d2:functions are substituted with a sample
     * value in order to maintain a valid expression syntax.
     *
     * @param expression the expression.
     */
    private String getSubstitutedSQLFunc( String expression )
    {
        StringBuffer expr = new StringBuffer();

        Matcher matcher = ProgramIndicator.SQL_FUNC_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String d2FunctionName = matcher.group( "func" );
            if ( SQL_FUNC_MAP.containsKey( d2FunctionName ) )
            {
                matcher.appendReplacement( expr, SQL_FUNC_MAP.get( d2FunctionName ).getSampleValue() );
            }
            else if ( PI_FUNC_MAP.containsKey( d2FunctionName ) )
            {
                matcher.appendReplacement( expr, PI_FUNC_MAP.get( d2FunctionName ).getSampleValue() );
            }
        }

        matcher.appendTail( expr );

        return expr.toString();
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
        Date endDate, ProgramIndicator indicator, String originalExpression )
    {
        final String dbl = statementBuilder.getDoubleColumnType();

        String variableColumnName = ProgramIndicator.getVariableColumnName( var );

        if ( ProgramIndicator.VAR_CURRENT_DATE.equals( var ) )
        {
            return "'" + DateUtils.getLongDateString() + "'";
        }
        else if ( ProgramIndicator.VAR_EVENT_DATE.equals( var ) )
        {
            return statementBuilder.getProgramIndicatorEventColumnSql( null, variableColumnName, startDate, endDate, indicator );
        }
        else if ( ProgramIndicator.VAR_VALUE_COUNT.equals( var ) )
        {
            String sql = "nullif(cast((";

            for ( String uid : ProgramIndicator.getDataElementAndAttributeIdentifiers( originalExpression, analyticsType ) )
            {
                sql += "case when " + statementBuilder.columnQuote( uid ) + " is not null then 1 else 0 end + ";
            }

            return TextUtils.removeLast( sql, "+" ).trim() + ") as " + dbl + "),0)";
        }
        else if ( ProgramIndicator.VAR_ZERO_POS_VALUE_COUNT.equals( var ) )
        {
            String sql = "nullif(cast((";

            for ( String uid : ProgramIndicator.getDataElementAndAttributeIdentifiers( originalExpression, analyticsType ) )
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

    private String getIgnoreNullSql( String column, ValueType valueType )
    {
        if ( valueType.equals( ValueType.DATE ) )
        {
            return "coalesce(" + column + "::date,'0001-01-01')";
        }
        else if ( valueType.equals( ValueType.TEXT ) )
        {
            return "coalesce(" + column + ",'')";
        }
        else
        {
            return "coalesce(" + column + "::numeric,0)";
        }
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
