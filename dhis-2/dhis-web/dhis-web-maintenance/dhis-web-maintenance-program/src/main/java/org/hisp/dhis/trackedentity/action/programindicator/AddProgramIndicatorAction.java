package org.hisp.dhis.trackedentity.action.programindicator;

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
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

import java.util.List;

/**
 * @author Chau Thu Tran
 * @version $ AddProgramIndicatorAction.java Apr 16, 2013 3:24:51 PM $
 */
public class AddProgramIndicatorAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    private ProgramIndicatorService programIndicatorService;

    public void setProgramIndicatorService( ProgramIndicatorService programIndicatorService )
    {
        this.programIndicatorService = programIndicatorService;
    }

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private LegendService legendService;

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    private Integer programId;

    public void setProgramId( Integer programId )
    {
        this.programId = programId;
    }

    public Integer getProgramId()
    {
        return programId;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String shortName;

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    private String code;

    public void setCode( String code )
    {
        this.code = code;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private String expression;

    public void setExpression( String expression )
    {
        this.expression = expression;
    }

    private String filter;

    public void setFilter( String filter )
    {
        this.filter = filter;
    }

    private String aggregationType;
    
    public void setAggregationType( String aggregationType )
    {
        this.aggregationType = aggregationType;
    }

    private Integer decimals;

    public void setDecimals( Integer decimals )
    {
        this.decimals = decimals;
    }
    
    private Integer legendSetId;
    
    public void setLegendSetId( Integer legendSetId )
    {
        this.legendSetId = legendSetId;
    }

    private Boolean displayInForm;

    public void setDisplayInForm( Boolean displayInForm )
    {
        this.displayInForm = displayInForm;
    }

    private List<String> jsonAttributeValues;

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
            this.jsonAttributeValues = jsonAttributeValues;
    }
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        Program program = programService.getProgram( programId );
        
        LegendSet legendSet = legendService.getLegendSet( legendSetId );
        
        ProgramIndicator indicator = new ProgramIndicator();
        indicator.setName( StringUtils.trimToNull( name ) );
        indicator.setShortName( StringUtils.trimToNull( shortName ) );
        indicator.setCode( StringUtils.trimToNull( code ) );
        indicator.setDescription( StringUtils.trimToNull( description ) );
        indicator.setProgram( program );
        indicator.setExpression( StringUtils.trimToNull( expression ) );
        indicator.setFilter( StringUtils.trimToNull( filter ) );
        indicator.setAggregationType( AggregationType.valueOf( aggregationType ) );
        indicator.setDecimals( decimals );
        indicator.setLegendSet( legendSet );
        indicator.setDisplayInForm( displayInForm );

        if ( jsonAttributeValues != null )
        {
            attributeService.updateAttributeValues( indicator, jsonAttributeValues );
        }

        programIndicatorService.addProgramIndicator( indicator );

        return SUCCESS;
    }
}
