package org.hisp.dhis.webapi.controller.mapping;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.geotools.geojson.geom.GeometryJSON;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Luciano Fiandesio
 */
public class GeoFeatureControllerTest
{
    private MockMvc mockMvc;

    @Mock
    private DataQueryService dataQueryService;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CurrentUserService currentUserService;

    private BeanRandomizer beanRandomizer = new BeanRandomizer();

    private final static String POINT = "{" +
        "\"type\": \"Point\"," +
        "\"coordinates\": [" +
        "51.17431640625," +
        "15.537052823106482" +
        "]" +
        "}";

    @InjectMocks
    private GeoFeatureController geoFeatureController;

    private final static String ENDPOINT = "/geoFeatures";

    @Before
    public void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup( geoFeatureController ).build();
    }

    @Test
    public void verifyGeoFeaturesReturnsOuData()
        throws Exception
    {
        OrganisationUnit ouA = createOrgUnitWithCoordinates();
        OrganisationUnit ouB = createOrgUnitWithCoordinates();
        OrganisationUnit ouC = createOrgUnitWithCoordinates();
        // This ou should be filtered out since it has no Coordinates
        OrganisationUnit ouD = createOrgUnitWithoutCoordinates();

        User user = beanRandomizer.randomObject(User.class);
        DataQueryParams params = DataQueryParams.newBuilder().withOrganisationUnits( getList( ouA, ouB, ouC, ouD ) )
            .build();

        when( dataQueryService.getFromRequest( any( DataQueryRequest.class ) ) ).thenReturn( params );
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        mockMvc.perform( get( ENDPOINT ).accept(ContextUtils.CONTENT_TYPE_JSON)
            .param( "ou", "ou:LEVEL-2;LEVEL-3" ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentType(ContextUtils.CONTENT_TYPE_JSON) )
            .andExpect( jsonPath( "$", hasSize( 3 ) ) );
    }

    private OrganisationUnit createOrgUnitWithoutCoordinates()
    {
        return beanRandomizer.randomObject( OrganisationUnit.class, "parent", "geometry" );
    }

    private OrganisationUnit createOrgUnitWithCoordinates()
        throws IOException
    {
        OrganisationUnit ou = createOrgUnitWithoutCoordinates();
        ou.setGeometry( new GeometryJSON().read( POINT ) );
        return ou;
    }
}