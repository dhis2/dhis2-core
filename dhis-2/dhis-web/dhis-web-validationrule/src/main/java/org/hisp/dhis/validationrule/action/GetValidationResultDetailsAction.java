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

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class GetValidationResultDetailsAction
    implements Action
{
    private static final String NULL_REPLACEMENT = "-";
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    private ValidationRuleService validationRuleService;

    public void setValidationRuleService( ValidationRuleService validationRuleService )
    {
        this.validationRuleService = validationRuleService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private ExpressionService expressionService;

    public void setExpressionService( ExpressionService expressionService )
    {
        this.expressionService = expressionService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer validationRuleId;

    public void setValidationRuleId( Integer validationRuleId )
    {
        this.validationRuleId = validationRuleId;
    }

    private Integer sourceId;

    public void setSourceId( Integer sourceId )
    {
        this.sourceId = sourceId;
    }

    private Integer periodId;

    public void setPeriodId( Integer periodId )
    {
        this.periodId = periodId;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private ValidationRule validationRule;

    public ValidationRule getValidationRule()
    {
        return validationRule;
    }
    
    private Map<String, String> leftSideMap = new HashMap<>();

    public Map<String, String> getLeftSideMap()
    {
        return leftSideMap;
    }

    private Map<String, String> rightSideMap = new HashMap<>();

    public Map<String, String> getRightSideMap()
    {
        return rightSideMap;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        validationRule = validationRuleService.getValidationRule( validationRuleId );

        Period period = periodService.getPeriod( periodId );

        OrganisationUnit source = organisationUnitService.getOrganisationUnit( sourceId );

        for ( DataElementOperand operand : expressionService.getOperandsInExpression( validationRule.getLeftSide().getExpression() ) )
        {
            DataElement dataElement = dataElementService.getDataElement( operand.getDataElementId() );
            DataElementCategoryOptionCombo categoryOptionCombo = categoryService
                .getDataElementCategoryOptionCombo( operand.getOptionComboId() );

            DataValue dataValue = dataValueService.getDataValue( dataElement, period, source, categoryOptionCombo );
            
            String value = dataValue != null ? dataValue.getValue() : NULL_REPLACEMENT;
            
            leftSideMap.put( DataElementOperand.getPrettyName( dataElement, categoryOptionCombo ), value );
        }

        for ( DataElementOperand operand : expressionService.getOperandsInExpression( validationRule.getRightSide().getExpression() ) )
        {
            DataElement dataElement = dataElementService.getDataElement( operand.getDataElementId() );
            DataElementCategoryOptionCombo categoryOptionCombo = categoryService
                .getDataElementCategoryOptionCombo( operand.getOptionComboId() );

            DataValue dataValue = dataValueService.getDataValue( dataElement, period, source, categoryOptionCombo );

            String value = dataValue != null ? dataValue.getValue() : NULL_REPLACEMENT;
            
            rightSideMap.put( DataElementOperand.getPrettyName( dataElement, categoryOptionCombo ), value );
        }

        return SUCCESS;
    }
}
