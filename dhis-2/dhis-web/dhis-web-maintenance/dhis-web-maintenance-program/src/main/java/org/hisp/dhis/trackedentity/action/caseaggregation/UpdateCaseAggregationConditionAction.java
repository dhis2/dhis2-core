package org.hisp.dhis.trackedentity.action.caseaggregation;

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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.caseaggregation.CaseAggregationCondition;
import org.hisp.dhis.caseaggregation.CaseAggregationConditionService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * 
 * @version UpdateCaseAggregationConditionAction.java Nov 18, 2010 10:42:42 AM
 */
public class UpdateCaseAggregationConditionAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CaseAggregationConditionService aggregationConditionService;

    private DataElementService dataElementService;

    private DataElementCategoryService dataElementCategoryService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    private String operator;

    private String aggregationCondition;

    private String name;

    private String aggregationDataElementId;

    private Integer deSumId;
    
    private Integer dataSetId;

    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------
    
    public void setAggregationConditionService( CaseAggregationConditionService aggregationConditionService )
    {
        this.aggregationConditionService = aggregationConditionService;
    }

    public Integer getDataSetId()
    {
        return dataSetId;
    }

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    public void setDataElementCategoryService( DataElementCategoryService dataElementCategoryService )
    {
        this.dataElementCategoryService = dataElementCategoryService;
    }

    public void setAggregationDataElementId( String aggregationDataElementId )
    {
        this.aggregationDataElementId = aggregationDataElementId;
    }

    public void setOperator( String operator )
    {
        this.operator = operator;
    }

    public void setAggregationCondition( String aggregationCondition )
    {
        this.aggregationCondition = aggregationCondition;
    }

    public void setDeSumId( Integer deSumId )
    {
        this.deSumId = deSumId;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        String[] ids = aggregationDataElementId.split( "\\." );

        DataElement aggregationDataElement = dataElementService.getDataElement( Integer.parseInt( ids[0] ) );
        DataElementCategoryOptionCombo optionCombo = dataElementCategoryService
            .getDataElementCategoryOptionCombo( Integer.parseInt( ids[1] ) );

        CaseAggregationCondition expression = aggregationConditionService.getCaseAggregationCondition( id );

        expression.setName( StringUtils.trimToNull( name ) );
        expression.setAggregationExpression( aggregationCondition );
        expression.setOperator( operator );
        expression.setAggregationDataElement( aggregationDataElement );
        expression.setOptionCombo( optionCombo );
        
        if ( deSumId != null )
        {
            DataElement deSum = dataElementService.getDataElement( deSumId );
            expression.setDeSum( deSum );
        }
        
        aggregationConditionService.updateCaseAggregationCondition( expression );

        return SUCCESS;
    }
}
