package org.hisp.dhis.commons.action;

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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.system.filter.AggregatableDataElementFilter;
import org.hisp.dhis.system.filter.DataElementPeriodTypeAllowAverageFilter;
import org.hisp.dhis.system.filter.DataElementPeriodTypeFilter;
import org.hisp.dhis.commons.filter.Filter;
import org.hisp.dhis.commons.filter.FilterUtils;

/**
 * @author Lars Helge Overland
 */
public class GetOperandsAction
    extends ActionPagingSupport<DataElementOperand>
{
    private static Filter<DataElement> AGGREGATABLE_FILTER = new AggregatableDataElementFilter();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private DataElementCategoryService dataElementCategoryService;

    public void setDataElementCategoryService( DataElementCategoryService dataElementCategoryService )
    {
        this.dataElementCategoryService = dataElementCategoryService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    // -------------------------------------------------------------------------
    // Exclusive filters
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }
    
    private String uid;

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    private String key;

    public void setKey( String key )
    {
        this.key = key;
    }

    private Integer dataSetId;

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    // -------------------------------------------------------------------------
    // Inclusive filters
    // -------------------------------------------------------------------------

    private String aggregationOperator;

    public void setAggregationOperator( String aggregationOperator )
    {
        this.aggregationOperator = aggregationOperator;
    }

    private String periodType;

    public void setPeriodType( String periodType )
    {
        this.periodType = periodType;
    }

    private boolean periodTypeAllowAverage;
    
    public void setPeriodTypeAllowAverage( boolean periodTypeAllowAverage )
    {
        this.periodTypeAllowAverage = periodTypeAllowAverage;
    }

    private boolean includeTotals;

    public void setIncludeTotals( boolean includeTotals )
    {
        this.includeTotals = includeTotals;
    }
    
    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    public List<DataElementOperand> operands;

    public List<DataElementOperand> getOperands()
    {
        return operands;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        List<DataElement> dataElements = new ArrayList<>();

        if ( id != null )
        {
            dataElements = new ArrayList<>( dataElementService.getDataElementGroup( id ).getMembers() );
        }
        else if ( uid != null )
        {
            dataElements = new ArrayList<>( dataElementService.getDataElementGroup( uid ).getMembers() );
        }
        else if ( !StringUtils.isEmpty( key ) )
        {
            dataElements = new ArrayList<>( dataElementService.getDataElementsLikeName( key ) );
        }
        else if ( dataSetId != null )
        {
            dataElements = new ArrayList<>( dataSetService.getDataSet( dataSetId ).getDataElements() );
        }
        else
        {
            dataElements = new ArrayList<>( dataElementService.getAllDataElements() );
        }

        if ( aggregationOperator != null )
        {
            FilterUtils.filter( dataElements, AGGREGATABLE_FILTER );
        }

        if ( periodType != null && !periodTypeAllowAverage )
        {
            FilterUtils.filter( dataElements, new DataElementPeriodTypeFilter( periodType ) );
        }
        else if ( periodType != null && periodTypeAllowAverage )
        {
            FilterUtils.filter( dataElements, new DataElementPeriodTypeAllowAverageFilter( periodType ) );
        }

        Collections.sort( dataElements, IdentifiableObjectNameComparator.INSTANCE );
        
        operands = new ArrayList<>( dataElementCategoryService.getOperands( dataElements,
            includeTotals ) );

        if ( usePaging )
        {
            this.paging = createPaging( operands.size() );

            operands = operands.subList( paging.getStartPos(), paging.getEndPos() );
        }

        return SUCCESS;
    }
    
}
