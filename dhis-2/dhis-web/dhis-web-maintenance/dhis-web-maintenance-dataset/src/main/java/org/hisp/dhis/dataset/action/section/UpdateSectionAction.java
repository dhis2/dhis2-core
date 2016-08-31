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

import static org.hisp.dhis.commons.util.TextUtils.equalsNullSafe;

public class UpdateSectionAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SectionService sectionService;

    public void setSectionService( SectionService sectionService )
    {
        this.sectionService = sectionService;
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

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer sectionId;

    public void setSectionId( Integer sectionId )
    {
        this.sectionId = sectionId;
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

    private Section section;

    public Section getSection()
    {
        return section;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        section = sectionService.getSection( sectionId.intValue() );

        List<DataElement> dataElements = new ArrayList<>();

        for ( String id : selectedDataElementList )
        {
            dataElements.add( dataElementService.getDataElement( Integer.parseInt( id ) ) );
        }

        List<Indicator> indicators = new ArrayList<>();

        for ( String id : selectedIndicatorList )
        {
            indicators.add( indicatorService.getIndicator( Integer.parseInt( id ) ) );
        }

        DataSet dataSet = section.getDataSet();
        
        section.setName( StringUtils.trimToNull( sectionName ) );
        section.setDescription( StringUtils.trimToNull( description ) );

        if ( dataSet != null ) // Check if version must be updated
        {
            if ( !( equalsNullSafe( sectionName, section.getName() ) && 
                dataElements.equals( section.getDataElements() ) && indicators.equals( section.getIndicators() ) ) )
            {
                dataSet.increaseVersion();
                
                dataSetService.updateDataSet( dataSet );
            }
        }

        section.setDataElements( dataElements );
        section.setIndicators( indicators );

        sectionService.updateSection( section );

        return SUCCESS;
    }
}
