package org.hisp.dhis.dxf2.gml;

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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.common.Coordinate.CoordinateUtils.getCoordinatesAsList;
import static org.hisp.dhis.common.IdentifiableProperty.*;
import static org.hisp.dhis.system.util.GeoUtils.getCoordinatesFromGeometry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import org.hibernate.SessionFactory;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.CoordinatesTuple;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.schema.*;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Halvdan Hoem Grelland
 */
public class GmlImportServiceTest extends DhisConvenienceTest
{
    private InputStream inputStream;

    private User user;

    private OrganisationUnit boOrgUnit, bontheOrgUnit, ojdOrgUnit, bliOrgUnit, forskOrgUnit;

    private ImportOptions importOptions;

    private JobConfiguration id;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------


    private GmlImportService subject;

    @Mock
    private OrganisationUnitService organisationUnitService;

    private RenderService renderService;

    @Mock
    private SchemaService schemaService;

    @Mock
    private IdentifiableObjectManager identifiableObjectManager;

    @Mock
    private UserService _userService;

    @Mock
    private MetadataImportService metadataImportService;

    @Mock
    private Notifier notifier;

    @Mock
    private MergeService mergeService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUpTest()
        throws IOException
    {
        Jackson2PropertyIntrospectorService introspectorService = new Jackson2PropertyIntrospectorService();
        renderService = new DefaultRenderService();

        subject = new DefaultGmlImportService(renderService, identifiableObjectManager,
                schemaService, metadataImportService, notifier, mergeService);

        inputStream = new ClassPathResource( "gml/testGmlPayload.gml" ).getInputStream();

        /*
         * Create orgunits present in testGmlPayload.gml and set ID properties.
         *      Name                    - FeatureType   - ID property
         *      Bo                      - Poly          - Name
         *      Bonthe                  - Multi         - Code
         *      Ole Johan Dahls Hus     - Point         - Uid
         *      Blindern                - Point (pos)   - Name
         *      Forskningsparken        - Poly (list)   - Name
         *
         * Note: some of these are included to cover different coordinate element schemes
         *       such as <posList>, <coordinates> and <pos>.
         */

        this.userService = _userService;
        boOrgUnit = createOrganisationUnit( 'A' );
        boOrgUnit.setName( "Bo" );

        bontheOrgUnit = createOrganisationUnit( 'B' );
        bontheOrgUnit.setName( "AA Bonthe" ); // Match on Code, therefore wrong name
        bontheOrgUnit.setCode( "CODE_BONTHE" );

        ojdOrgUnit = createOrganisationUnit( 'C' );
        ojdOrgUnit.setUid( "ImspTQPwCqd" );
        ojdOrgUnit.setName( "AA Ole Johan Dahls Hus" ); // Match on UID, therefore wrong name

        bliOrgUnit = createOrganisationUnit( 'D' );
        bliOrgUnit.setName( "Blindern" );

        forskOrgUnit = createOrganisationUnit( 'E' );
        forskOrgUnit.setName( "Forskningsparken" );

        user = createAdminUser();

        id = new JobConfiguration( "gmlImportTest", JobType.METADATA_IMPORT, user.getUid(), true );

        importOptions = new ImportOptions().setImportStrategy( ImportStrategy.UPDATE );
        importOptions.setDryRun( false );
        importOptions.setPreheatCache( true );

        when(identifiableObjectManager.exists(OrganisationUnit.class, "ImspTQPwCqd")).thenReturn(true);
        when(identifiableObjectManager.getObjects(eq(OrganisationUnit.class), eq(UID),
                (Collection<String>) Mockito.argThat(containsInAnyOrder(ojdOrgUnit.getUid()))))
                .thenReturn(Lists.newArrayList(ojdOrgUnit));
        when(identifiableObjectManager.getObjects(eq(OrganisationUnit.class), eq(CODE),
                (Collection<String>) Mockito.argThat(containsInAnyOrder(bontheOrgUnit.getCode()))))
                .thenReturn(Lists.newArrayList(bontheOrgUnit));
        when(identifiableObjectManager.getObjects(eq(OrganisationUnit.class), eq(NAME),
                (Collection<String>) Mockito.argThat(containsInAnyOrder(boOrgUnit.getName(),
                        bliOrgUnit.getName(), forskOrgUnit.getName()))))
                .thenReturn(Lists.newArrayList(boOrgUnit, bliOrgUnit, forskOrgUnit));

    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testImportGml()
    {
        MetadataImportParams importParams = new MetadataImportParams();
        importParams.setId( id );
        importParams.setUser( user );

        subject.importGml( inputStream, importParams );

        assertNotNull( boOrgUnit.getGeometry() );

        assertNotNull( bontheOrgUnit.getGeometry() );

        assertNotNull( ojdOrgUnit.getGeometry() );

        assertNotNull( bliOrgUnit.getGeometry() );

        assertNotNull( forskOrgUnit.getGeometry() );


        // Check if data is correct
        assertEquals( 1, getCoordinates( boOrgUnit ).size() );
        assertEquals( 18, getCoordinates( bontheOrgUnit ).size() );
        assertEquals( 1, getCoordinates( ojdOrgUnit ).size() );
        assertEquals( 1, getCoordinates( bliOrgUnit ).size() );
        assertEquals( 1, getCoordinates( forskOrgUnit ).size() );

        assertEquals( 76, getCoordinates( boOrgUnit ).get( 0 ).getNumberOfCoordinates() );
        assertEquals( 189, getCoordinates( bontheOrgUnit ).get( 0 ).getNumberOfCoordinates() );
        assertEquals( 1, getCoordinates( ojdOrgUnit ).get( 0 ).getNumberOfCoordinates() );
        assertEquals( 1, getCoordinates( bliOrgUnit ).get( 0 ).getNumberOfCoordinates() );
        assertEquals( 76, getCoordinates( forskOrgUnit ).get( 0 ).getNumberOfCoordinates() );

    }

    private List<CoordinatesTuple> getCoordinates( OrganisationUnit orgUnit )
    {
        return getCoordinatesAsList( getCoordinatesFromGeometry( orgUnit.getGeometry() ),
            FeatureType.getTypeFromName( orgUnit.getGeometry().getGeometryType() ) );
    }
}
