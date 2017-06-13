package org.hisp.dhis.commons.action;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.filter.AggregatableDataElementFilter;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.util.ContextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class GetDataElementsAction
    extends ActionPagingSupport<DataElement>
{
    private final static int ALL = 0;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // Input & output
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private Integer categoryComboId;

    public void setCategoryComboId( Integer categoryComboId )
    {
        this.categoryComboId = categoryComboId;
    }

    private Integer dataSetId;

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private String periodTypeName;

    public void setPeriodTypeName( String periodTypeName )
    {
        this.periodTypeName = periodTypeName;
    }

    private String key;

    public void setKey( String key )
    {
        this.key = key;
    }

    private boolean aggregate = false;

    public void setAggregate( boolean aggregate )
    {
        this.aggregate = aggregate;
    }

    public String domain;

    public void setDomain( String domain )
    {
        this.domain = domain;
    }

    private List<DataElement> dataElements;

    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( id != null && id != ALL )
        {
            DataElementGroup dataElementGroup = dataElementService.getDataElementGroup( id );

            if ( dataElementGroup != null )
            {
                dataElements = new ArrayList<>( dataElementGroup.getMembers() );
            }
        }
        else if ( categoryComboId != null && categoryComboId != ALL )
        {
            DataElementCategoryCombo categoryCombo = categoryService.getDataElementCategoryCombo( categoryComboId );

            if ( categoryCombo != null )
            {
                dataElements = new ArrayList<>(
                    dataElementService.getDataElementByCategoryCombo( categoryCombo ) );
            }
        }
        else if ( dataSetId != null )
        {
            DataSet dataset = dataSetService.getDataSet( dataSetId );

            if ( dataset != null )
            {
                dataElements = new ArrayList<>( dataset.getDataElements() );
            }
        }
        else if ( periodTypeName != null )
        {
            PeriodType periodType = periodService.getPeriodTypeByName( periodTypeName );

            if ( periodType != null )
            {
                dataElements = new ArrayList<>( dataElementService.getDataElementsByPeriodType( periodType ) );
            }
        }
        else if ( domain != null )
        {
            if ( domain.equals( DataElementDomain.AGGREGATE.getValue() ) )
            {
                dataElements = new ArrayList<>(
                    dataElementService.getDataElementsByDomainType( DataElementDomain.AGGREGATE ) );
            }
            else
            {
                dataElements = new ArrayList<>(
                    dataElementService.getDataElementsByDomainType( DataElementDomain.TRACKER ) );
            }
        }
        else
        {
            dataElements = new ArrayList<>( dataElementService.getAllDataElements() );

            ContextUtils.clearIfNotModified( ServletActionContext.getRequest(), ServletActionContext.getResponse(), dataElements );
        }

        if ( key != null )
        {
            dataElements = IdentifiableObjectUtils.filterNameByKey( dataElements, key, true );
        }

        if ( dataElements == null )
        {
            dataElements = new ArrayList<>();
        }

        Collections.sort( dataElements );

        if ( aggregate )
        {
            FilterUtils.filter( dataElements, new AggregatableDataElementFilter() );
        }

        if ( usePaging )
        {
            this.paging = createPaging( dataElements.size() );

            dataElements = dataElements.subList( paging.getStartPos(), paging.getEndPos() );
        }

        return SUCCESS;
    }
}
