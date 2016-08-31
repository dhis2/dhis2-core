package org.hisp.dhis.dataset.action.section;

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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;

import com.opensymphony.xwork2.Action;

public class AddSectionAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private IndicatorService indicatorService;
    
    public void setIndicatorService( IndicatorService indicatorService )
    {
        this.indicatorService = indicatorService;
    }

    private SectionService sectionService;

    public void setSectionService( SectionService sectionService )
    {
        this.sectionService = sectionService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer dataSetId;

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    public Integer getDataSetId()
    {
        return dataSetId;
    }

    private Integer categoryComboId;

    public void setCategoryComboId( Integer categoryComboId )
    {
        this.categoryComboId = categoryComboId;
    }

    public Integer getCategoryComboId()
    {
        return categoryComboId;
    }
    
    private String sectionName;

    public void setSectionName( String sectionName )
    {
        this.sectionName = sectionName;
    }
    
    private String description;
    
    public void setDescription( String description )
    {
        this.description = description;
    }

    private List<String> selectedDataElementList = new ArrayList<>();

    public void setSelectedDataElementList( List<String> selectedDataElementList )
    {
        this.selectedDataElementList = selectedDataElementList;
    }

    private List<String> selectedIndicatorList = new ArrayList<>();

    public void setSelectedIndicatorList( List<String> selectedIndicatorList )
    {
        this.selectedIndicatorList = selectedIndicatorList;
    }
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------


    @Override
    public String execute()
        throws Exception
    {
        DataSet dataSet = dataSetService.getDataSet( dataSetId );

        Section section = new Section();

        section.setDataSet( dataSet );
        section.setName( StringUtils.trimToNull( sectionName ) );
        section.setDescription( StringUtils.trimToNull( description ) );
        section.setSortOrder( 0 );

        List<DataElement> selectedDataElements = new ArrayList<>();

        for ( String id : selectedDataElementList )
        {
            selectedDataElements.add( dataElementService.getDataElement( Integer.parseInt( id ) ) );
        }
        
        List<Indicator> selectedIndicators = new ArrayList<>();
        
        for ( String id : selectedIndicatorList )
        {
            selectedIndicators.add( indicatorService.getIndicator( Integer.parseInt( id ) ) );
        }

        section.setDataElements( selectedDataElements );
        section.setIndicators( selectedIndicators );
        dataSet.getSections().add( section );
        sectionService.addSection( section );

        dataSet.increaseVersion();
        dataSetService.updateDataSet( dataSet );
        
        return SUCCESS;
    }
}
