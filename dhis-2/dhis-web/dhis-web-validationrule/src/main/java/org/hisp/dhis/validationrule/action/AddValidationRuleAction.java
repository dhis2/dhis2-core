package org.hisp.dhis.validationrule.action;

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

import com.opensymphony.xwork2.Action;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.Importance;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;

import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING;
import static org.hisp.dhis.expression.MissingValueStrategy.safeValueOf;

/**
 * Adds a new validation rule to the database.
 *
 * @author Margrethe Store
 * @author Lars Helge Overland
 * @version $Id: AddValidationRuleAction.java 3868 2007-11-08 15:11:12Z larshelg $
 */
public class AddValidationRuleAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ValidationRuleService validationRuleService;

    public void setValidationRuleService( ValidationRuleService validationRuleService )
    {
        this.validationRuleService = validationRuleService;
    }

    private ExpressionService expressionService;

    public void setExpressionService( ExpressionService expressionService )
    {
        this.expressionService = expressionService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private String instruction;

    public void setInstruction( String instruction )
    {
        this.instruction = instruction;
    }

    private String importance;

    public void setImportance( String importance )
    {
        this.importance = importance;
    }

    private String operator;

    public void setOperator( String operator )
    {
        this.operator = operator;
    }

    private String leftSideExpression;

    public void setLeftSideExpression( String leftSideExpression )
    {
        this.leftSideExpression = leftSideExpression;
    }

    private String leftSideDescription;

    public void setLeftSideDescription( String leftSideDescription )
    {
        this.leftSideDescription = leftSideDescription;
    }

    private String leftSideMissingValueStrategy;

    public void setLeftSideMissingValueStrategy( String leftSideMissingValueStrategy )
    {
        this.leftSideMissingValueStrategy = leftSideMissingValueStrategy;
    }

    private String rightSideExpression;

    public void setRightSideExpression( String rightSideExpression )
    {
        this.rightSideExpression = rightSideExpression;
    }

    private String rightSideDescription;

    public void setRightSideDescription( String rightSideDescription )
    {
        this.rightSideDescription = rightSideDescription;
    }

    private String skipTestExpression;

    public void setSkipTestExpression( String skipTestExpression )
    {
        this.skipTestExpression = skipTestExpression;
    }

    private String skipTestDescription;

    public void setSkipTestDescription( String skipTestDescription )
    {
        this.skipTestDescription = skipTestDescription;
    }

    private String rightSideMissingValueStrategy;

    public void setRightSideMissingValueStrategy( String rightSideMissingValueStrategy )
    {
        this.rightSideMissingValueStrategy = rightSideMissingValueStrategy;
    }

    private String periodTypeName;

    public void setPeriodTypeName( String periodTypeName )
    {
        this.periodTypeName = periodTypeName;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        Expression leftSide = new Expression();

        leftSide.setExpression( leftSideExpression );
        leftSide.setDescription( leftSideDescription );
        leftSide.setMissingValueStrategy( safeValueOf( leftSideMissingValueStrategy, SKIP_IF_ANY_VALUE_MISSING ) );
        leftSide.setDataElementsInExpression( expressionService.getDataElementsInExpression( leftSideExpression ) );
        leftSide.setSampleElementsInExpression( expressionService.getSampleElementsInExpression( leftSideExpression ) );

        Expression rightSide = new Expression();

        rightSide.setExpression( rightSideExpression );
        rightSide.setDescription( rightSideDescription );
        rightSide.setMissingValueStrategy( safeValueOf( rightSideMissingValueStrategy, SKIP_IF_ANY_VALUE_MISSING ) );
        rightSide.setDataElementsInExpression( expressionService.getDataElementsInExpression( rightSideExpression ) );
        rightSide.setSampleElementsInExpression( expressionService.getSampleElementsInExpression( rightSideExpression ) );

        Expression skipTest = null;

        if ( StringUtils.trimToNull( skipTestExpression ) != null )
        {
            skipTest = new Expression();
            skipTest.setExpression( skipTestExpression );
            skipTest.setDescription( skipTestDescription );

            skipTest.setDataElementsInExpression( expressionService.getDataElementsInExpression( skipTestExpression ) );
            skipTest.setSampleElementsInExpression( expressionService.getSampleElementsInExpression( skipTestExpression ) );
        }

        ValidationRule validationRule = new ValidationRule();

        validationRule.setName( StringUtils.trimToNull( name ) );
        validationRule.setDescription( StringUtils.trimToNull( description ) );
        validationRule.setInstruction( StringUtils.trimToNull( instruction ) );
        validationRule.setImportance( Importance.valueOf( StringUtils.trimToNull( importance ) ) );
        validationRule.setOperator( Operator.valueOf( operator ) );
        validationRule.setLeftSide( leftSide );
        validationRule.setRightSide( rightSide );

        PeriodType periodType = periodService.getPeriodTypeByName( periodTypeName );
        validationRule.setPeriodType( periodType );

        validationRuleService.saveValidationRule( validationRule );

        return SUCCESS;
    }
}
