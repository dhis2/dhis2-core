/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.data.DefaultDataQueryService;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class AnalyticsControllerTest
{
    private final static String ENDPOINT = "/analytics";

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private ContextUtils contextUtils;

    @Mock
    private DimensionService dimensionService;

    @BeforeEach
    public void setUp()
    {
        final DataQueryService dataQueryService = new DefaultDataQueryService(
            mock( IdentifiableObjectManager.class ),
            mock( OrganisationUnitService.class ),
            dimensionService, mock( AnalyticsSecurityManager.class ), mock( SystemSettingManager.class ),
            mock( AclService.class ), mock( CurrentUserService.class ),
            mock( I18nManager.class ) );

        // Controller under test
        final AnalyticsController controller = new AnalyticsController( dataQueryService, analyticsService,
            contextUtils );

        mockMvc = MockMvcBuilders.standaloneSetup( controller ).build();

        // When
        when( dimensionService.getDataDimensionalItemObject( IdScheme.UID, "fbfJHSPpUQD" ) )
            .thenReturn( new DataElement( "alfa" ) );
        when( dimensionService.getDataDimensionalItemObject( IdScheme.UID, "cYeuwXTCPkU" ) )
            .thenReturn( new DataElement( "beta" ) );
        when( analyticsService.getAggregatedDataValues( Mockito.any( DataQueryParams.class ), Mockito.any(),
            Mockito.any() ) )
                .thenReturn( buildMockGrid() );
    }

    @Test
    void verifyJsonRequest()
        throws Exception
    {
        // Then
        mockMvc.perform( get( ENDPOINT )
            .param( "dimension", "dx:fbfJHSPpUQD;cYeuwXTCPkU" )
            .param( "filter", "pe:2014Q1;2014Q2" ) )
            .andExpect( status().isOk() )
            .andExpect( jsonPath( "$" ).exists() )
            .andExpect( content().contentType( "application/json" ) );
    }

    @Test
    void verifyXmlRequest()
        throws Exception
    {
        // Then
        mockMvc.perform( get( ENDPOINT + ".xml" )
            .param( "dimension", "dx:fbfJHSPpUQD;cYeuwXTCPkU" )
            .param( "filter", "pe:2014Q1;2014Q2" ) )
            // .andExpect( content().contentType( "application/xml" ) ) // Note:
            // we do not
            // send contentType with xml payload
            .andExpect( content().string( notNullValue() ) )
            .andExpect( content().string( startsWith( "<?xml version='1.0' encoding='UTF-8'?>" ) ) )
            .andExpect( status().isOk() );
    }

    @Test
    void verifyHtmlRequest()
        throws Exception
    {
        // Then
        mockMvc.perform( get( ENDPOINT + ".html" )
            .param( "dimension", "dx:fbfJHSPpUQD;cYeuwXTCPkU" )
            .param( "filter", "pe:2014Q1;2014Q2" ) )
            // .andExpect( content().contentType( "application/xml" ) ) // Note:
            // we do not
            // send contentType with html payload
            .andExpect( content().string( notNullValue() ) )
            .andExpect( content().string( startsWith( "<div class=\"gridDiv\">" ) ) )
            .andExpect( status().isOk() );
    }

    @Test
    void verifyHtmlCssRequest()
        throws Exception
    {
        // Then
        mockMvc.perform( get( ENDPOINT + ".html+css" )
            .param( "dimension", "dx:fbfJHSPpUQD;cYeuwXTCPkU" )
            .param( "filter", "pe:2014Q1;2014Q2" ) )
            // .andExpect( content().contentType( "text/html" ) )
            // Note: we do not send contentType with html+css payload
            .andExpect( content().string( notNullValue() ) )
            .andExpect( content().string( startsWith( "<style type=\"text/css\">" ) ) )
            .andExpect( status().isOk() );
    }

    @Test
    void verifyCsvRequest()
        throws Exception
    {
        // Then
        mockMvc.perform( get( ENDPOINT + ".csv" )
            .param( "dimension", "dx:fbfJHSPpUQD;cYeuwXTCPkU" )
            .param( "filter", "pe:2014Q1;2014Q2" ) )
            // .andExpect( content().contentType( "application/csv" ) )
            // Note: we do not send contentType with csv payload
            .andExpect( content().string( notNullValue() ) )
            .andExpect( content().string( "\"\",,,\nde1,ou2,pe1,3\n" +
                "de2,ou3,pe2,5\n" ) )
            .andExpect( status().isOk() );
    }

    @Test
    void verifyXlsRequest()
        throws Exception
    {
        // Then
        final ResultActions resultActions = mockMvc.perform( get( ENDPOINT + ".xls" )
            .param( "dimension", "dx:fbfJHSPpUQD;cYeuwXTCPkU" )
            .param( "filter", "pe:2014Q1;2014Q2" ) )
            // .andExpect( content().contentType( "application/xls" ) )
            // Note: we do not send contentType with xsl payload
            .andExpect( status().isOk() );

        // Convert content to Excel sheet
        final byte[] excel = resultActions.andReturn().getResponse().getContentAsByteArray();
        InputStream is = new ByteArrayInputStream( excel );
        Workbook book = WorkbookFactory.create( is );
        assertThat( book.getSheetAt( 0 ).getRow( 2 ).getCell( 0 ).getStringCellValue(), is( "de1" ) );
        assertThat( book.getSheetAt( 0 ).getRow( 2 ).getCell( 1 ).getStringCellValue(), is( "ou2" ) );
        assertThat( book.getSheetAt( 0 ).getRow( 2 ).getCell( 2 ).getStringCellValue(), is( "pe1" ) );
        assertThat( book.getSheetAt( 0 ).getRow( 2 ).getCell( 3 ).getStringCellValue(), is( "3" ) );
    }

    @Test
    void verifyJrxmlRequest()
        throws Exception
    {
        when( analyticsService.getAggregatedDataValues( Mockito.any( DataQueryParams.class ) ) )
            .thenReturn( buildMockGrid() );

        // Then
        mockMvc.perform( get( ENDPOINT + ".jrxml" )
            .param( "dimension", "dx:fbfJHSPpUQD;cYeuwXTCPkU" )
            .param( "filter", "pe:2014Q1;2014Q2" ) )
            // .andExpect( content().contentType( "application/xml" ) )
            // Note: we do not send contentType with jrxml payload
            .andExpect( content().string( notNullValue() ) )
            .andExpect( content().string( startsWith( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ) ) )
            .andExpect( status().isOk() );
    }

    private Grid buildMockGrid()
    {
        Grid grid = new ListGrid();
        grid.addHeader( new GridHeader( "a" ) );
        grid.addHeader( new GridHeader( "b" ) );
        grid.addHeader( new GridHeader( "c" ) );
        grid.addHeader( new GridHeader( "d" ) );

        grid.addRow();
        grid.addValue( "de1" );
        grid.addValue( "ou2" );
        grid.addValue( "pe1" );
        grid.addValue( 3 );

        grid.addRow();
        grid.addValue( "de2" );
        grid.addValue( "ou3" );
        grid.addValue( "pe2" );
        grid.addValue( 5 );
        return grid;
    }
}
