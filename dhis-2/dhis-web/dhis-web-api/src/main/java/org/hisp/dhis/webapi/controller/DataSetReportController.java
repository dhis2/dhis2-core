package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datasetreport.DataSetReportService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( value = "/dataSetReport" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataSetReportController
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataSetReportService dataSetReportService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    WebMessageService webMessageService;

    @Autowired
    IdentifiableObjectManager idObjectManager;

    @RequestMapping( value = "/custom", method = RequestMethod.GET, produces = "text/html" )
    public @ResponseBody String getCustomDataSetReport( HttpServletResponse response,
        @RequestParam String ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) boolean selectedUnitOnly )
        throws Exception
    {
        OrganisationUnit orgUnit = getAndValidateOrgUnit( ou );
        DataSet dataSet = getAndValidateDataSet( ds );
        Period period = getAndValidatePeriod( pe );

        if ( !dataSet.getFormType().isCustom() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set form type must be 'custom': " + dataSet.getFormType() ) );
        }

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING );

        return dataSetReportService.getCustomDataSetReport( dataSet, period, orgUnit, filter, selectedUnitOnly );
    }

    @RequestMapping( method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody List<Grid> getDataSetReportAsJson( HttpServletResponse response,
        @RequestParam String ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) boolean selectedUnitOnly )
        throws Exception
    {
        OrganisationUnit orgUnit = getAndValidateOrgUnit( ou );
        DataSet dataSet = getAndValidateDataSet( ds );
        Period period = getAndValidatePeriod( pe );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING );

        return dataSetReportService.getDataSetReportAsGrid( dataSet, period, orgUnit, filter, selectedUnitOnly );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private OrganisationUnit getAndValidateOrgUnit( String ou ) throws WebMessageException
    {
        OrganisationUnit orgUnit = idObjectManager.get( OrganisationUnit.class, ou );

        if ( orgUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit does not exist: " + ou ) );
        }

        return orgUnit;
    }

    private DataSet getAndValidateDataSet( String ds ) throws WebMessageException
    {
        DataSet dataSet = dataSetService.getDataSet( ds );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set does not exist: " + ds ) );
        }

        return dataSet;
    }

    private Period getAndValidatePeriod( String pe ) throws WebMessageException
    {
        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period does not exist: " + pe ) );
        }

        return periodService.reloadPeriod( period );
    }
}
