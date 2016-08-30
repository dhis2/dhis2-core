package org.hisp.dhis.webapi.documentation.controller;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class LockExceptionControllerDocumentation
    extends DhisWebSpringTest
{
    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataSetService dataSetService;

    @Test
    public void testAddLockException() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        PeriodType periodType = periodService.getPeriodTypeByName( "Monthly" );

        Period period = createPeriod( periodType, getDate( 2016, 12, 1 ), getDate( 2016, 12, 31 ) );
        manager.save( period );

        OrganisationUnit orgUnit = createOrganisationUnit( 'B' );
        manager.save( orgUnit );

        DataSet dataSet = createDataSet( 'A', periodType );
        dataSet.addOrganisationUnit( orgUnit );
        manager.save( dataSet );

        String postUrl = "/lockExceptions?ou=" + orgUnit.getUid() + "&pe=201612&ds=" + dataSet.getUid();

        mvc.perform( post( postUrl ).session( session ).accept( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().is( 201 ) )
            .andExpect( content().contentTypeCompatibleWith( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andDo( documentPrettyPrint( "lockExceptions/add" ) );
    }

    @Test
    public void testDeleteLockException() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        PeriodType periodType = periodService.getPeriodTypeByName( "Monthly" );

        Period period = createPeriod( periodType, getDate( 2016, 12, 1 ), getDate( 2016, 12, 31 ) );
        manager.save( period );

        OrganisationUnit orgUnit = createOrganisationUnit( 'B' );
        manager.save( orgUnit );

        DataSet dataSet = createDataSet( 'A', periodType );
        dataSet.addOrganisationUnit( orgUnit );
        manager.save( dataSet );

        LockException lockException = new LockException( period, orgUnit, dataSet );
        dataSetService.addLockException( lockException );

        String deleteUrl = "/lockExceptions?ou=" + orgUnit.getUid() + "&pe=201612&ds=" + dataSet.getUid();

        mvc.perform( delete( deleteUrl ).session( session ).accept( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().isNoContent() )
            .andDo( documentPrettyPrint( "lockExceptions/delete" )
            );
    }

    @Test
    public void testGetLockException() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        PeriodType periodType = periodService.getPeriodTypeByName( "Monthly" );

        Period period = createPeriod( periodType, getDate( 2016, 12, 1 ), getDate( 2016, 12, 31 ) );
        manager.save( period );

        OrganisationUnit orgUnit = createOrganisationUnit( 'B' );
        manager.save( orgUnit );

        DataSet dataSet = createDataSet( 'A', periodType );
        dataSet.addOrganisationUnit( orgUnit );
        manager.save( dataSet );

        LockException lockException = new LockException( period, orgUnit, dataSet );
        dataSetService.addLockException( lockException );

        String getUrl = "/lockExceptions?filter=organisationUnit.id:eq:" + orgUnit.getUid() + "&filter=period:eq:201612&filter=dataSet.id:eq:" + dataSet.getUid();

        Lists.newArrayList(
            fieldWithPath( "period" ).description( "Property" ),
            fieldWithPath( "organisationUnit" ).description( "Property" ),
            fieldWithPath( "dataSet" ).description( "Property" )
        );

        mvc.perform( get( getUrl ).session( session ).accept( MediaType.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().is( 200 ) )
            .andDo( documentPrettyPrint( "lockExceptions/get" )
            ).andReturn();
    }
}
