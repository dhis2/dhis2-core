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

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.caseaggregation.CaseAggregationCondition;
import org.hisp.dhis.caseaggregation.CaseAggregationConditionService;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * @version $ ShowUpdateCaseAggregationConditionFormAction.java May 26, 2011 11:43:19 AM $
 * 
 */
public class ShowUpdateCaseAggregationConditionFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CaseAggregationConditionService aggregationConditionService;

    public DataSetService dataSetService;

    public ProgramService programService;

    private TrackedEntityAttributeService attributeService;

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private Integer id;

    private Integer dataSetId;

    private CaseAggregationCondition caseAggregation;

    private String description;

    private List<TrackedEntityAttribute> attributes;

    private List<DataSet> dataSets;

    private List<Program> programs;
    
    private DataSet dataSet;

    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    public void setAggregationConditionService( CaseAggregationConditionService aggregationConditionService )
    {
        this.aggregationConditionService = aggregationConditionService;
    }

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    public void setAttributeService( TrackedEntityAttributeService attributeService )
    {
        this.attributeService = attributeService;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    public Integer getDataSetId()
    {
        return dataSetId;
    }

    public String getDescription()
    {
        return description;
    }

    public CaseAggregationCondition getCaseAggregation()
    {
        return caseAggregation;
    }

    public List<DataSet> getDataSets()
    {
        return dataSets;
    }

    public List<Program> getPrograms()
    {
        return programs;
    }

    public List<TrackedEntityAttribute> getAttributes()
    {
        return attributes;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public DataSet getDataSet()
    {
        return dataSet;
    }

    @Override
    public String execute()
        throws Exception
    {
        dataSets = dataSetService.getAllDataSets();
        Collections.sort( dataSets, IdentifiableObjectNameComparator.INSTANCE );

        programs = programService.getAllPrograms();
        Collections.sort( programs, IdentifiableObjectNameComparator.INSTANCE );

        attributes = attributeService.getAllTrackedEntityAttributes();
        Collections.sort( attributes, IdentifiableObjectNameComparator.INSTANCE );

        caseAggregation = aggregationConditionService.getCaseAggregationCondition( id );
        description = aggregationConditionService.getConditionDescription( caseAggregation.getAggregationExpression() );

        DataElement dataelement = caseAggregation.getAggregationDataElement();
        for ( DataSet _dataSet : dataSets )
        {
            if ( _dataSet.getDataElements().contains( dataelement ) )
            {
                dataSet = _dataSet;
                break;
            }
        }
        
        return SUCCESS;
    }
}