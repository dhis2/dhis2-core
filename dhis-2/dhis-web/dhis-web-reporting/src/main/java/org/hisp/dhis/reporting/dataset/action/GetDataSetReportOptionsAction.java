package org.hisp.dhis.reporting.dataset.action;

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

import static org.hisp.dhis.period.PeriodType.getAvailablePeriodTypes;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.joda.time.DateTime;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class GetDataSetReportOptionsAction
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
    
    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private OrganisationUnitGroupService organisationUnitGroupService;

    public void setOrganisationUnitGroupService( OrganisationUnitGroupService organisationUnitGroupService )
    {
        this.organisationUnitGroupService = organisationUnitGroupService;
    }
    
    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private SelectionTreeManager selectionTreeManager;

    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String ds;

    public String getDs()
    {
        return ds;
    }

    public void setDs( String ds )
    {
        this.ds = ds;
    }

    private String pe;

    public String getPe()
    {
        return pe;
    }

    public void setPe( String pe )
    {
        this.pe = pe;
    }

    private String ou;

    public void setOu( String ou )
    {
        this.ou = ou;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<DataSet> dataSets;

    public List<DataSet> getDataSets()
    {
        return dataSets;
    }

    private List<PeriodType> periodTypes;

    public List<PeriodType> getPeriodTypes()
    {
        return periodTypes;
    }

    private boolean render;
    
    public boolean isRender()
    {
        return render;
    }
    
    private int offset;

    public int getOffset()
    {
        return offset;
    }

    private PeriodType periodType;
    
    public PeriodType getPeriodType()
    {
        return periodType;
    }

    private DataElementCategoryCombo defaultCategoryCombo;

    public DataElementCategoryCombo getDefaultCategoryCombo()
    {
        return defaultCategoryCombo;
    }

    private List<DataElementCategoryCombo> categoryCombos;
    
    public List<DataElementCategoryCombo> getCategoryCombos()
    {
        return categoryCombos;
    }

    private List<CategoryOptionGroupSet> categoryOptionGroupSets;
    
    public List<CategoryOptionGroupSet> getCategoryOptionGroupSets()
    {
        return categoryOptionGroupSets;
    }

    private List<OrganisationUnitGroupSet> organisationUnitGroupSets;

    public List<OrganisationUnitGroupSet> getOrganisationUnitGroupSets()
    {
        return organisationUnitGroupSets;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {        
        periodTypes = getAvailablePeriodTypes();
        
        render = ( ds != null && pe != null && ou != null );
        
        if ( pe != null && getPeriodFromIsoString( pe ) != null )
        {
            Period period = getPeriodFromIsoString( pe );
                        
            offset = new DateTime( period.getStartDate() ).getYear() - new DateTime().getYear();
            
            periodType = period.getPeriodType();
            
            selectionTreeManager.setSelectedOrganisationUnit( organisationUnitService.getOrganisationUnit( ou ) ); //TODO set unit state in client instead
        }

        defaultCategoryCombo = categoryService.getDefaultDataElementCategoryCombo();

        dataSets = new ArrayList<>( dataSetService.getAllDataSets() );
        categoryCombos = new ArrayList<>( categoryService.getAttributeCategoryCombos() );
        categoryOptionGroupSets = new ArrayList<>( categoryService.getAllCategoryOptionGroupSets() );
        organisationUnitGroupSets = new ArrayList<>( organisationUnitGroupService.getAllOrganisationUnitGroupSets() );

        Collections.sort( dataSets );   
        Collections.sort( categoryCombos );
        Collections.sort( categoryOptionGroupSets );
        Collections.sort( organisationUnitGroupSets );
        
        return SUCCESS;
    }
}
