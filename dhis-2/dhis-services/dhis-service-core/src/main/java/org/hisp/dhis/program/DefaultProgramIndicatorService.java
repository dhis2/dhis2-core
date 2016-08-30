package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import org.hisp.dhis.commons.sqlfunc.ConditionalSqlFunction;
import org.hisp.dhis.commons.sqlfunc.DaysBetweenSqlFunction;
import org.hisp.dhis.commons.sqlfunc.OneIfZeroOrPositiveSqlFunction;
import org.hisp.dhis.commons.sqlfunc.SqlFunction;
import org.hisp.dhis.commons.sqlfunc.ZeroIfNegativeSqlFunction;
import org.hisp.dhis.commons.sqlfunc.ZeroPositiveValueCountFunction;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
    private static final Map<String, SqlFunction> SQL_FUNC_MAP = ImmutableMap.<String, SqlFunction>builder().
        put( ZeroIfNegativeSqlFunction.KEY, new ZeroIfNegativeSqlFunction() ).
        put( OneIfZeroOrPositiveSqlFunction.KEY, new OneIfZeroOrPositiveSqlFunction() ).
        put( ZeroPositiveValueCountFunction.KEY, new ZeroPositiveValueCountFunction() ).
        put( DaysBetweenSqlFunction.KEY, new DaysBetweenSqlFunction() ).
        put( ConditionalSqlFunction.KEY, new ConditionalSqlFunction() ).build();

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

    @Autowired
    private I18nManager i18nManager;

    // -------------------------------------------------------------------------
    // ProgramIndicatorService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public int addProgramIndicator( ProgramIndicator programIndicator )
    {
        return programIndicatorStore.save( programIndicator );
    }

    @Override
    @Transactional
    public void updateProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.update( programIndicator );
    }

    @Override
    @Transactional
    public void deleteProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.delete( programIndicator );
    }

    @Override
    @Transactional
    public ProgramIndicator getProgramIndicator( int id )
    {
        return programIndicatorStore.get( id );
    }

    @Override
    @Transactional
    public ProgramIndicator getProgramIndicator( String name )
    {
        return programIndicatorStore.getByName( name );
    }

    @Override
    @Transactional
    public ProgramIndicator getProgramIndicatorByUid( String uid )
    {
        return programIndicatorStore.getByUid( uid );
    }

    @Override
    @Transactional
    public ProgramIndicator getProgramIndicatorByShortName( String shortName )
    {
        return programIndicatorStore.getByShortName( shortName );
    }

    @Override
    @Transactional
    public List<ProgramIndicator> getAllProgramIndicators()
    {
        return programIndicatorStore.getAll();
    }

    @Override
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

                    matcher.appendReplacement( description, programStageName + ProgramIndicator.SEPARATOR_ID + dataelementName );
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

    @Override
    public String getAnalyticsSQl( String expression )
    {
        return getAnalyticsSQl( expression, true );
    }
    
    @Override
    public String getAnalyticsSQl( String expression, boolean ignoreMissingValues )
    {
        if ( expression == null )
        {
            return null;
        }

        expression = TextUtils.removeNewlines( expression );
        
        expression = getSubstitutedVariablesForAnalyticsSql( expression );
        
        expression = getSubstitutedFunctionsAnalyticsSql( expression, false );

        expression = getSubstitutedElementsAnalyticsSql( expression, ignoreMissingValues );
        
        return expression;
    }

    private String getSubstitutedFunctionsAnalyticsSql( String expression, boolean ignoreMissingValues )
    {
        if ( expression == null )
        {
            return null;
        }
        
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = ProgramIndicator.SQL_FUNC_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String func = trim( matcher.group( 1 ) );
            String arguments = trim( matcher.group( 2 ) );
            
            if ( func != null && arguments != null )
            {
                String[] args = arguments.split( ProgramIndicator.ARGS_SPLIT );
                
                for ( int i = 0; i < args.length; i++ )
                {
                    String arg = getSubstitutedElementsAnalyticsSql( trim( args[i] ), false );
                    args[i] = arg;
                }
                
                SqlFunction function = SQL_FUNC_MAP.get( func );
                
                if ( function == null )
                {
                    throw new IllegalStateException( "Function not recognized: " + func );
                }
                
                String result = function.evaluate( args );
    
                matcher.appendReplacement( buffer, result );
            }
        }

        return TextUtils.appendTail( matcher, buffer );
    }

    private String getSubstitutedVariablesForAnalyticsSql( String expression )
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
            
            String sql = getVariableAsSql( var, expression );

            if ( sql != null )
            {
                matcher.appendReplacement( buffer, sql );
            }
        }
        
        return TextUtils.appendTail( matcher, buffer );
    }
    
    private String getSubstitutedElementsAnalyticsSql( String expression, boolean ignoreMissingValues )
    {
        if ( expression == null )
        {
            return null;
        }
        
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = ProgramIndicator.EXPRESSION_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String el1 = matcher.group( 2 );
            String el2 = matcher.group( 3 );
            
            if ( ProgramIndicator.KEY_DATAELEMENT.equals( key ) )
            {
                String de = ignoreMissingValues ? getIgnoreNullSql( statementBuilder.columnQuote( el2 ) ) : statementBuilder.columnQuote( el2 );
                
                matcher.appendReplacement( buffer, de );
            }
            else if ( ProgramIndicator.KEY_ATTRIBUTE.equals( key ) )
            {
                String at = ignoreMissingValues ? getIgnoreNullSql( statementBuilder.columnQuote( el1 ) ) : statementBuilder.columnQuote( el1 );
                
                matcher.appendReplacement( buffer, at );
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

    @Override
    public String getAnyValueExistsClauseAnalyticsSql( String expression )
    {
        Set<String> uids = ProgramIndicator.getDataElementAndAttributeIdentifiers( expression );
                
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

    @Override
    @Transactional
    public String expressionIsValid( String expression )
    {
        String expr = getSubstitutedExpression( expression );

        if ( ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION.equals( expr ) )
        {
            return expr;
        }

        if ( !ExpressionUtils.isValid( expr, null ) )
        {
            return ProgramIndicator.EXPRESSION_NOT_VALID;
        }

        return ProgramIndicator.VALID;
    }

    @Override
    @Transactional
    public String filterIsValid( String filter )
    {
        String expr = getSubstitutedExpression( filter );

        if ( ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION.equals( expr ) )
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
     * Generates an expression where all items are substituted with a sample value
     * in order to maintain a valid expression syntax.
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
                matcher.appendReplacement( expr, String.valueOf( 1 ) );
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
    private String getVariableAsSql( String var, String expression )
    {
        final String dbl = statementBuilder.getDoubleColumnType();
        
        if ( ProgramIndicator.VAR_EXECUTION_DATE.equals( var ) )
        {
            return "executiondate";
        }
        else if ( ProgramIndicator.VAR_DUE_DATE.equals( var ) )
        {
            return "duedate";
        }
        else if ( ProgramIndicator.VAR_ENROLLMENT_DATE.equals( var ) )
        {
            return "enrollmentdate";
        }
        else if ( ProgramIndicator.VAR_INCIDENT_DATE.equals( var ) )
        {
            return "incidentdate";
        }
        else if ( ProgramIndicator.VAR_CURRENT_DATE.equals( var ) )
        {
            return "'" + DateUtils.getLongDateString() + "'";
        }
        else if ( ProgramIndicator.VAR_VALUE_COUNT.equals( var ) )
        {
            String sql = "nullif(cast((";

            for ( String uid : ProgramIndicator.getDataElementAndAttributeIdentifiers( expression ) )
            {
                sql += "case when " + statementBuilder.columnQuote( uid ) + " is not null then 1 else 0 end + ";
            }

            return TextUtils.removeLast( sql, "+" ).trim() + ") as " + dbl + "),0)";
        }
        else if ( ProgramIndicator.VAR_ZERO_POS_VALUE_COUNT.equals( var ) )
        {
            String sql = "nullif(cast((";

            for ( String uid : ProgramIndicator.getDataElementAndAttributeIdentifiers( expression ) )
            {
                sql += "case when " + statementBuilder.columnQuote( uid ) + " >= 0 then 1 else 0 end + ";
            }

            return TextUtils.removeLast( sql, "+" ).trim() + ") as " + dbl + "),0)";
        }
        else if ( ProgramIndicator.VAR_EVENT_COUNT.equals( var ) )
        {
            return "distinct psi";
        }
        else if ( ProgramIndicator.VAR_ENROLLMENT_COUNT.equals( var ) )
        {
            return "distinct pi";
        }
        else if ( ProgramIndicator.VAR_TEI_COUNT.equals( var ) )
        {
            return "distinct tei";
        }

        return null;
    }

    private String getIgnoreNullSql( String column )
    {
        return "coalesce(" + column + ",0)";
    }
}
