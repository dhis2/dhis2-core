package org.hisp.dhis.caseaggregation;

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

import java.util.Collection;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Chau Thu Tran
 */
public class CaseAggregationConditionDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CaseAggregationConditionService aggregationConditionService;

    public void setAggregationConditionService( CaseAggregationConditionService aggregationConditionService )
    {
        this.aggregationConditionService = aggregationConditionService;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    protected String getClassName()
    {
        return CaseAggregationCondition.class.getSimpleName();
    }

    @Override
    public String allowDeleteDataElement( DataElement dataElement )
    {
        Collection<CaseAggregationCondition> conditions = aggregationConditionService
            .getCaseAggregationCondition( dataElement );

        if ( conditions != null && conditions.size() > 0 )
        {
            return ERROR;
        }

        conditions = aggregationConditionService.getAllCaseAggregationCondition();

        for ( CaseAggregationCondition condition : conditions )
        {
            Collection<DataElement> dataElements = aggregationConditionService.getDataElementsInCondition( condition
                .getAggregationExpression() );

            if ( dataElements != null && dataElements.contains( dataElement ) )
            {
                return ERROR;
            }
        }

        return null;
    }

    @Override
    public String allowDeleteDataElementCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        Collection<CaseAggregationCondition> conditions = aggregationConditionService.getAllCaseAggregationCondition();

        for ( CaseAggregationCondition condition : conditions )
        {
            if ( categoryCombo.getOptionCombos().contains( condition.getOptionCombo() ) )
            {
                return ERROR;
            }
        }

        return null;
    }

    @Override
    public String allowDeleteProgram( Program program )
    {
        Collection<CaseAggregationCondition> conditions = aggregationConditionService
            .getAllCaseAggregationCondition();

        for ( CaseAggregationCondition condition : conditions )
        {
            Collection<Program> programs = aggregationConditionService.getProgramsInCondition( condition
                .getAggregationExpression() );

            if ( programs != null && programs.contains( program ) )
            {
                return condition.getName();
            }
        }

        return null;
    }
    
    @Override
    public String allowDeleteTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        Collection<CaseAggregationCondition> conditions = aggregationConditionService
            .getAllCaseAggregationCondition();

        for ( CaseAggregationCondition condition : conditions )
        {
            Collection<TrackedEntityAttribute> attributes = aggregationConditionService.getTrackedEntityAttributesInCondition( condition
                .getAggregationExpression() );

            if ( attributes != null && attributes.contains( attribute ) )
            {
                return condition.getName();
            }
        }

        return null;
    }
}
