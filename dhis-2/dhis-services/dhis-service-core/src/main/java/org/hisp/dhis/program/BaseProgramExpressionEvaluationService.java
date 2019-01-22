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

import org.hisp.dhis.commons.sqlfunc.SqlFunction;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author Zubair Asghar.
 */
public abstract class BaseProgramExpressionEvaluationService implements ProgramExpressionEvaluationService
{
    private static final String SEPARATOR_ID = "\\.";
    private static final String KEY_DATAELEMENT = "#";
    private static final String KEY_ATTRIBUTE = "A";
    private static final String KEY_PROGRAM_VARIABLE = "V";
    private static final String KEY_CONSTANT = "C";

    protected static final String VAR_EVENT_DATE = "event_date";
    protected static final String VAR_EXECUTION_DATE = "execution_date";
    protected static final String VAR_DUE_DATE = "due_date";
    protected static final String VAR_ENROLLMENT_DATE = "enrollment_date";
    protected static final String VAR_INCIDENT_DATE = "incident_date";
    protected static final String VAR_ENROLLMENT_STATUS = "enrollment_status";
    protected static final String VAR_CURRENT_DATE = "current_date";
    protected static final String VAR_VALUE_COUNT = "value_count";
    protected static final String VAR_ZERO_POS_VALUE_COUNT = "zero_pos_value_count";
    protected static final String VAR_EVENT_COUNT = "event_count";
    protected static final String VAR_ENROLLMENT_COUNT = "enrollment_count";
    protected static final String VAR_TEI_COUNT = "tei_count";
    protected static final String VAR_COMPLETED_DATE = "completed_date";
    protected static final String VAR_PROGRAM_STAGE_NAME = "program_stage_name";
    protected static final String VAR_PROGRAM_STAGE_ID = "program_stage_id";
    protected static final String VAR_ANALYTICS_PERIOD_START = "analytics_period_start";
    protected static final String VAR_ANALYTICS_PERIOD_END = "analytics_period_end";

    private static final String EXPRESSION_PREFIX_REGEXP = KEY_DATAELEMENT + "|" + KEY_ATTRIBUTE + "|" + KEY_PROGRAM_VARIABLE + "|" + KEY_CONSTANT;
    private static final String EXPRESSION_REGEXP = "(" + EXPRESSION_PREFIX_REGEXP + ")\\{([\\w\\_]+)" + SEPARATOR_ID + "?(\\w*)\\}";
    private static final String SQL_FUNC_ARG_REGEXP = " *(([\"\\w/\\*\\+\\-\\_\\:%\\.\\<\\>\\= \\#\\{\\}]+)|('[^']*'))";
    private static final String SQL_FUNC_REGEXP = "d2:(?<func>.+?)\\((?<args>" + SQL_FUNC_ARG_REGEXP + "*( *," + SQL_FUNC_ARG_REGEXP + ")* *)\\)";

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile( EXPRESSION_REGEXP );
    private static final Pattern SQL_FUNC_PATTERN = Pattern.compile( SQL_FUNC_REGEXP );

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private I18nManager i18nManager;

    @Override
    public String isValidExpression( String expression )
    {
        String expr = getSubstitutedSQLFunc( getSubstitutedExpression( expression ) );

        if ( ExpressionUtils.INVALID_IDENTIFIERS_IN_EXPRESSION.equals( expr ) || ExpressionUtils.UNKNOWN_VARIABLE.equals( expr ) )
        {
            return expr;
        }

        if ( !ExpressionUtils.isValid( expr, null ) )
        {
            return ExpressionUtils.EXPRESSION_NOT_VALID;
        }

        return ExpressionUtils.VALID;
    }

    @Override
    public String getExpressionDescription( String expression )
    {
        if ( expression == null )
        {
            return null;
        }

        I18n i18n = i18nManager.getI18n();

        StringBuffer description = new StringBuffer();

        Matcher matcher = EXPRESSION_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String uid = matcher.group( 2 );

            if ( KEY_DATAELEMENT.equals( key ) )
            {
                String de = matcher.group( 3 );

                ProgramStage programStage = programStageService.getProgramStage( uid );
                DataElement dataElement = dataElementService.getDataElement( de );

                if ( programStage != null && dataElement != null )
                {
                    String programStageName = programStage.getDisplayName();
                    String dataElementName = dataElement.getDisplayName();

                    matcher.appendReplacement( description, programStageName + ProgramIndicator.SEPARATOR_ID
                        + dataElementName );
                }
            }
            else if ( KEY_ATTRIBUTE.equals( key ) )
            {
                TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( uid );

                if ( attribute != null )
                {
                    matcher.appendReplacement( description, attribute.getDisplayName() );
                }
            }
            else if ( KEY_CONSTANT.equals( key ) )
            {
                Constant constant = constantService.getConstant( uid );

                if ( constant != null )
                {
                    matcher.appendReplacement( description, constant.getDisplayName() );
                }
            }
            else if ( KEY_PROGRAM_VARIABLE.equals( key ) )
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
    public String isFilterExpressionValid( String filter )
    {
        String expr = getSubstitutedSQLFunc( getSubstitutedExpression( filter ) );

        if ( ExpressionUtils.INVALID_IDENTIFIERS_IN_EXPRESSION.equals( expr )
                || ExpressionUtils.UNKNOWN_VARIABLE.equals( expr ) )
        {
            return expr;
        }

        if ( !ExpressionUtils.isBoolean( expr, null ) )
        {
            return ExpressionUtils.FILTER_NOT_EVALUATING_TO_TRUE_OR_FALSE;
        }

        return ExpressionUtils.VALID;
    }

    protected abstract Map<String, ProgramD2Function> getD2Functions();

    protected abstract Map<String, SqlFunction> getSQLFunctions();

    protected abstract Map<String, String> getSourceVariableMap();

    /**
     * Generates an expression where all items are substituted with a sample
     * value in order to maintain a valid expression syntax.
     *
     * @param expression the expression.
     */
    private String getSubstitutedExpression( String expression )
    {
        StringBuffer expr = new StringBuffer();

        Matcher matcher = EXPRESSION_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String uid = matcher.group( 2 );

            if ( KEY_DATAELEMENT.equals( key ) )
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
                    return ExpressionUtils.INVALID_IDENTIFIERS_IN_EXPRESSION;
                }
            }
            else if ( KEY_ATTRIBUTE.equals( key ) )
            {
                TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( uid );

                if ( attribute != null )
                {
                    String sample = ValidationUtils.getSubstitutionValue( attribute.getValueType() );

                    matcher.appendReplacement( expr, sample );
                }
                else
                {
                    return ExpressionUtils.INVALID_IDENTIFIERS_IN_EXPRESSION;
                }
            }
            else if ( KEY_CONSTANT.equals( key ) )
            {
                Constant constant = constantService.getConstant( uid );

                if ( constant != null )
                {
                    matcher.appendReplacement( expr, String.valueOf( constant.getValue() ) );
                }
                else
                {
                    return ExpressionUtils.INVALID_IDENTIFIERS_IN_EXPRESSION;
                }
            }
            else if ( KEY_PROGRAM_VARIABLE.equals( key ) )
            {
                String sampleValue = getSourceVariableMap().get( uid );

                if ( sampleValue != null )
                {
                    matcher.appendReplacement( expr, sampleValue );
                }
                else
                {
                    return ExpressionUtils.UNKNOWN_VARIABLE;
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

        Matcher matcher = SQL_FUNC_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String d2FunctionName = matcher.group( "func" );
            if ( getSQLFunctions().containsKey( d2FunctionName ) )
            {
                matcher.appendReplacement( expr, getSQLFunctions().get( d2FunctionName ).getSampleValue() );
            }
            else if ( getD2Functions().containsKey( d2FunctionName ) )
            {
                matcher.appendReplacement( expr, getD2Functions().get( d2FunctionName ).getSampleValue() );
            }
        }

        matcher.appendTail( expr );

        return expr.toString();
    }
}
