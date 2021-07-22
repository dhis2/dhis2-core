/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.approval.dataset.action;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.FormType;
import org.hisp.dhis.datasetreport.DataSetReportService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * @author Lars Helge Overland
 */
public class GenerateDataSetReportAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataSetReportService dataSetReportService;

    public void setDataSetReportService( DataSetReportService dataSetReportService )
    {
        this.dataSetReportService = dataSetReportService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private CompleteDataSetRegistrationService registrationService;

    public void setRegistrationService( CompleteDataSetRegistrationService registrationService )
    {
        this.registrationService = registrationService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private CategoryService categoryService;

    public void setCategoryService( CategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    @Autowired
    private ContextUtils contextUtils;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String ds;

    public void setDs( String ds )
    {
        this.ds = ds;
    }

    private String pe;

    public void setPe( String pe )
    {
        this.pe = pe;
    }

    private String ou;

    public void setOu( String ou )
    {
        this.ou = ou;
    }

    /**
     * Dimensional parameters, follows the standard analytics format, e.g.
     * <dim-id>:<dim-item>;<dim-item>
     */
    private Set<String> dimension;

    public void setDimension( Set<String> dimension )
    {
        this.dimension = dimension;
    }

    private boolean selectedUnitOnly;

    public boolean isSelectedUnitOnly()
    {
        return selectedUnitOnly;
    }

    public void setSelectedUnitOnly( boolean selectedUnitOnly )
    {
        this.selectedUnitOnly = selectedUnitOnly;
    }

    private String type;

    public void setType( String type )
    {
        this.type = type;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private OrganisationUnit selectedOrgunit;

    public OrganisationUnit getSelectedOrgunit()
    {
        return selectedOrgunit;
    }

    private DataSet selectedDataSet;

    public DataSet getSelectedDataSet()
    {
        return selectedDataSet;
    }

    private Period selectedPeriod;

    public Period getSelectedPeriod()
    {
        return selectedPeriod;
    }

    private CompleteDataSetRegistration registration;

    public CompleteDataSetRegistration getRegistration()
    {
        return registration;
    }

    private String customDataEntryFormCode;

    public String getCustomDataEntryFormCode()
    {
        return customDataEntryFormCode;
    }

    private List<Grid> grids = new ArrayList<>();

    public List<Grid> getGrids()
    {
        return grids;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        // ---------------------------------------------------------------------
        // Configure response
        // ---------------------------------------------------------------------

        HttpServletResponse response = ServletActionContext.getResponse();

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            null, false );

        // ---------------------------------------------------------------------
        // Assemble report
        // ---------------------------------------------------------------------

        selectedDataSet = dataSetService.getDataSetNoAcl( ds );

        if ( pe != null )
        {
            selectedPeriod = PeriodType.getPeriodFromIsoString( pe );
            selectedPeriod = periodService.reloadPeriod( selectedPeriod );
        }

        selectedOrgunit = organisationUnitService.getOrganisationUnit( ou );

        FormType formType = selectedDataSet.getFormType();

        CategoryOptionCombo attributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        registration = registrationService.getCompleteDataSetRegistration( selectedDataSet, selectedPeriod,
            selectedOrgunit, attributeOptionCombo );

        if ( formType.isCustom() && type == null )
        {
            customDataEntryFormCode = dataSetReportService.getCustomDataSetReport( selectedDataSet,
                newArrayList( selectedPeriod ), selectedOrgunit, dimension, selectedUnitOnly );
        }
        else
        {
            grids = dataSetReportService.getDataSetReportAsGrid( selectedDataSet,
                newArrayList( selectedPeriod ), selectedOrgunit, dimension, selectedUnitOnly );
        }

        return type != null ? type : formType.toString();
    }
}
