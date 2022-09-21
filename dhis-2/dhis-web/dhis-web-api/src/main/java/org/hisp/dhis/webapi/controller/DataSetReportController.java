/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.webapi.controller;

import java.util.*;

import javax.servlet.http.*;

import org.hisp.dhis.common.*;
import org.hisp.dhis.common.cache.*;
import org.hisp.dhis.dataset.*;
import org.hisp.dhis.datasetreport.*;
import org.hisp.dhis.dxf2.webmessage.*;
import org.hisp.dhis.organisationunit.*;
import org.hisp.dhis.period.*;
import org.hisp.dhis.system.grid.*;
import org.hisp.dhis.util.*;
import org.hisp.dhis.webapi.mvc.annotation.*;
import org.hisp.dhis.webapi.service.*;
import org.hisp.dhis.webapi.utils.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

/**
 * @author Stian Sandvold
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataSetReportController
{
    private static final String RESOURCE_PATH = "/dataSetReport";

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

    /*@RequestMapping( value = RESOURCE_PATH
        + "/custom", method = RequestMethod.GET, produces = "text/html;charset=UTF-8" )
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
            throw new WebMessageException(
                WebMessageUtils.conflict( "Data set form type must be 'custom': " + dataSet.getFormType() ) );
        }

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML,
            CacheStrategy.RESPECT_SYSTEM_SETTING );

        return dataSetReportService.getCustomDataSetReport( dataSet, period, orgUnit, filter, selectedUnitOnly );
    }*/
    
    @RequestMapping( value = RESOURCE_PATH+ "/custom", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody List<Grid> getSectionDataSetReportAsJson( HttpServletResponse response,
        @RequestParam String ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) boolean selectedUnitOnly,
        @RequestParam( required = false ) int noOfSignatures )
        throws Exception
    {
        OrganisationUnit orgUnit = getAndValidateOrgUnit( ou );
        DataSet dataSet = getAndValidateDataSet( ds );
        Period period = getAndValidatePeriod( pe );
        //filter = ObjectUtils.firstNonNull( filter, dimension );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return dataSetReportService.getSectionDataSetReport( dataSet, period, orgUnit, filter, selectedUnitOnly );
    }

    @RequestMapping( value = RESOURCE_PATH, method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody List<Grid> getDataSetReportAsJson( HttpServletResponse response,
        @RequestParam String ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) Set<String> dimension, // TODO remove,
                                                                 // deprecated
                                                                 // in 2.31
        @RequestParam( required = false ) boolean selectedUnitOnly,
    	@RequestParam( required = false ) int noOfSignatures )
        throws Exception
    {
        OrganisationUnit orgUnit = getAndValidateOrgUnit( ou );
        DataSet dataSet = getAndValidateDataSet( ds );
        Period period = getAndValidatePeriod( pe );
        filter = ObjectUtils.firstNonNull( filter, dimension );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        return dataSetReportService.getDataSetReportAsGrid( dataSet, period, orgUnit, filter, selectedUnitOnly );
    }

    @RequestMapping( value = RESOURCE_PATH + ".xls", method = RequestMethod.GET )
    public void getDataSetReportAsExcel( HttpServletResponse response,
        @RequestParam String ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) boolean selectedUnitOnly,
        @RequestParam( required = false ) int noOfSignatures )
        throws Exception
    {
        OrganisationUnit orgUnit = getAndValidateOrgUnit( ou );
        DataSet dataSet = getAndValidateDataSet( ds );
        Period period = getAndValidatePeriod( pe );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL,
            CacheStrategy.RESPECT_SYSTEM_SETTING );
        List<Grid> grids = dataSetReportService.getDataSetReportAsGrid( dataSet, period, orgUnit, filter,
            selectedUnitOnly );
        GridUtils.toXls( grids, response.getOutputStream() );
    }

    @RequestMapping( value = RESOURCE_PATH + ".pdf", method = RequestMethod.GET )
    public void getDataSetReportAsPdf( HttpServletResponse response,
        @RequestParam String ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) boolean selectedUnitOnly,
        @RequestParam( required = false ) int noOfSignatures )
        throws Exception
    {
        OrganisationUnit orgUnit = getAndValidateOrgUnit( ou );
        DataSet dataSet = getAndValidateDataSet( ds );
        Period period = getAndValidatePeriod( pe );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.RESPECT_SYSTEM_SETTING );
        List<Grid> grids = dataSetReportService.getDataSetReportAsGrid( dataSet, period, orgUnit, filter,
            selectedUnitOnly );
        GridUtils.toPdfCustom( grids, response.getOutputStream(), noOfSignatures );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private OrganisationUnit getAndValidateOrgUnit( String ou )
        throws WebMessageException
    {
        OrganisationUnit orgUnit = idObjectManager.get( OrganisationUnit.class, ou );

        if ( orgUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit does not exist: " + ou ) );
        }

        return orgUnit;
    }

    private DataSet getAndValidateDataSet( String ds )
        throws WebMessageException
    {
        DataSet dataSet = dataSetService.getDataSet( ds );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set does not exist: " + ds ) );
        }

        return dataSet;
    }

    private Period getAndValidatePeriod( String pe )
        throws WebMessageException
    {
        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period does not exist: " + pe ) );
        }

        return periodService.reloadPeriod( period );
    }
}
