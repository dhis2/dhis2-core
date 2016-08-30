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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.comparator.AttributeSortOrderComparator;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.system.predicate.ArithmeticValueTypeTrackedEntityAttributeFilter;
import org.hisp.dhis.system.util.AttributeUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 */
public class GetProgramIndicatorAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramIndicatorService programIndicatorService;

    public void setProgramIndicatorService( ProgramIndicatorService programIndicatorService )
    {
        this.programIndicatorService = programIndicatorService;
    }

    @Autowired
    private ConstantService constantService;
    
    @Autowired
    private ProgramService programService;
    
    @Autowired
    private LegendService legendService;

    @Autowired
    private AttributeService attributeService;

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }
    
    private Integer programId;

    public void setProgramId( Integer programId )
    {
        this.programId = programId;
    }

    private ProgramIndicator programIndicator;

    public ProgramIndicator getProgramIndicator()
    {
        return programIndicator;
    }

    private Program program;
    
    public Program getProgram()
    {
        return program;
    }

    private String expressionDescription;
    
    public String getExpressionDescription()
    {
        return expressionDescription;
    }

    private String filterDescription;
    
    public String getFilterDescription()
    {
        return filterDescription;
    }

    private String filter;
    
    public String getFilter()
    {
        return filter;
    }

    private List<TrackedEntityAttribute> expressionAttributes = new ArrayList<>();
        
    public List<TrackedEntityAttribute> getExpressionAttributes()
    {
        return expressionAttributes;
    }

    private List<Constant> constants = new ArrayList<>();

    public List<Constant> getConstants()
    {
        return constants;
    }
    
    private List<LegendSet> legendSets = new ArrayList<>();

    public List<LegendSet> getLegendSets()
    {
        return legendSets;
    }

    private List<Attribute> attributes;

    public List<Attribute> getAttributes() { return attributes;  }

    private Map<Integer, String> attributeValues = new HashMap<>();

    public Map<Integer, String> getAttributeValues() { return attributeValues; }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( id != null )
        {
            programIndicator = programIndicatorService.getProgramIndicator( id );
            program = programIndicator.getProgram();
            expressionDescription = programIndicatorService.getExpressionDescription( programIndicator.getExpression() );
            filterDescription = programIndicatorService.getExpressionDescription( programIndicator.getFilter() );
            filter = programIndicatorService.getExpressionDescription( programIndicator.getFilter() );

            attributeValues = AttributeUtils.getAttributeValueMap( programIndicator.getAttributeValues() );

        }
        else if ( programId != null )
        {            
            program = programService.getProgram( programId );
        }
        
        expressionAttributes = new ArrayList<>( program.getTrackedEntityAttributes() );
        constants = constantService.getAllConstants();
        legendSets = legendService.getAllLegendSets();

        expressionAttributes = expressionAttributes.stream().filter( ArithmeticValueTypeTrackedEntityAttributeFilter.INSTANCE ).collect( Collectors.toList() );

        attributes = new ArrayList<>( attributeService.getAttributes( ProgramIndicator.class ) );

        Collections.sort( attributes, AttributeSortOrderComparator.INSTANCE );
        Collections.sort( expressionAttributes );
        Collections.sort( constants );
        Collections.sort( legendSets );
        
        return SUCCESS;
    }
}
