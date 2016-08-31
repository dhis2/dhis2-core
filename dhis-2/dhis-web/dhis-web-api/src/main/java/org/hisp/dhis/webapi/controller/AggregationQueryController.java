package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2014, University of Oslo
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.caseaggregation.AggregationQueries;
import org.hisp.dhis.caseaggregation.AggregationQuery;
import org.hisp.dhis.caseaggregation.CaseAggregationCondition;
import org.hisp.dhis.caseaggregation.CaseAggregationConditionService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping( value = AggregationQueryController.RESOURCE_PATH )
public class AggregationQueryController
{
    private static final Log log = LogFactory.getLog( AggregationQueryController.class );

    public static final String RESOURCE_PATH = "/aggregationQueries";

    @Autowired
    private CaseAggregationConditionService aggregationConditionService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_AGGREGATION')" )
    public void postAggregationQueryJson( @RequestBody AggregationQueries aggregationQueries,
        HttpServletResponse response )
    {
        for ( AggregationQuery aggregationQuery : aggregationQueries )
        {
            CaseAggregationCondition caseAggregationCondition = convertToCaseAggregationCondition( aggregationQuery );
            
            if ( caseAggregationCondition.getId() == 0 )
            {
                aggregationConditionService.addCaseAggregationCondition( caseAggregationCondition );
            }
            else
            {
                aggregationConditionService.updateCaseAggregationCondition( caseAggregationCondition );
            }
        }
    }
    
    private CaseAggregationCondition convertToCaseAggregationCondition( AggregationQuery aggregationQuery )
    {
        String id = aggregationQuery.getId();
        CaseAggregationCondition expression = aggregationConditionService.getCaseAggregationConditionByUid( id );
        
        if ( expression == null )
        {
            expression = new CaseAggregationCondition();
            expression.setUid( id );
        }

        expression.setName( aggregationQuery.getName() );
        expression.setOperator( aggregationQuery.getOperator() );
        expression.setAggregationDataElement( dataElementService.getDataElement( aggregationQuery.getDataElement() ) );

        if ( aggregationQuery.getCategoryOptionCombo() == null )
        {
            expression.setOptionCombo( dataElementCategoryService.getDefaultDataElementCategoryOptionCombo() );
        }
        else
        {
            expression.setOptionCombo( dataElementCategoryService.getDataElementCategoryOptionCombo( aggregationQuery.getCategoryOptionCombo() ) );
        }

        String deIdForGroupBy = aggregationQuery.getDataElementForGroupBy();
        
        if ( deIdForGroupBy != null )
        {
            DataElement deSum = dataElementService.getDataElement( deIdForGroupBy );
            expression.setDeSum( deSum );
        }

        expression.setAggregationExpression( convertDataElementExpression( aggregationQuery.getExpression() ) );

        return expression;
    }

    private String convertDataElementExpression( String expression )
    {
        String uidPattern = "[A-Za-z0-9]+";
        String uidTokenPattern = "(#\\{(" + uidPattern + ")})";

        StringBuffer replacedExpressionBuffer = new StringBuffer();

        Pattern dePattern = Pattern.compile( "(?<=\\[" + CaseAggregationCondition.OBJECT_PROGRAM_STAGE_DATAELEMENT
            + CaseAggregationCondition.SEPARATOR_OBJECT + ")"
            + uidTokenPattern + CaseAggregationCondition.SEPARATOR_ID
            + uidTokenPattern + CaseAggregationCondition.SEPARATOR_ID
            + uidTokenPattern
            + "(?=\\])" );

        Matcher matcher = dePattern.matcher( expression );
        
        while ( matcher.find() )
        {
            String programUid = matcher.group( 2 );
            int programId = programService.getProgram( programUid ).getId();

            String programStageUid = matcher.group( 4 );
            int programStageId = programStageService.getProgramStage( programStageUid ).getId();

            String dataElementUid = matcher.group( 6 );
            int dataElementId = dataElementService.getDataElement( dataElementUid ).getId();

            matcher.appendReplacement( replacedExpressionBuffer,
                String.format( "%d.%d.%d", programId, programStageId, dataElementId ) );
        }

        matcher.appendTail( replacedExpressionBuffer );

        String parsedExpression = replacedExpressionBuffer.toString();
        log.info( "Replacing " + expression + " with " + parsedExpression );
        return parsedExpression;
    }
}
