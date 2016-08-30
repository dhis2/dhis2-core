package org.hisp.dhis.dxf2.gml;

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

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Halvdan Hoem Grelland
 */
public class GmlImportServiceTest
    extends DhisSpringTest
{
    private InputStream inputStream;

    private User user;

    private OrganisationUnit boOrgUnit, bontheOrgUnit, ojdOrgUnit, bliOrgUnit, forskOrgUnit;

    private ImportOptions importOptions;

    private TaskId taskId;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private GmlImportService gmlImportService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private UserService userService;

    @Override
    public void setUpTest()
        throws IOException
    {
        inputStream = new ClassPathResource( "gml/testGmlPayload.gml" ).getInputStream();

        /**
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

        boOrgUnit = createOrganisationUnit( 'A' );
        boOrgUnit.setName( "Bo" );
        organisationUnitService.addOrganisationUnit( boOrgUnit );

        bontheOrgUnit = createOrganisationUnit( 'B' );
        bontheOrgUnit.setName( "AA Bonthe" ); // Match on Code, therefore wrong name
        bontheOrgUnit.setCode( "CODE_BONTHE" );
        organisationUnitService.addOrganisationUnit( bontheOrgUnit );

        ojdOrgUnit = createOrganisationUnit( 'C' );
        ojdOrgUnit.setUid( "ImspTQPwCqd" );
        ojdOrgUnit.setName( "AA Ole Johan Dahls Hus" ); // Match on UID, therefore wrong name
        organisationUnitService.addOrganisationUnit( ojdOrgUnit );

        bliOrgUnit = createOrganisationUnit( 'D' );
        bliOrgUnit.setName( "Blindern" );
        organisationUnitService.addOrganisationUnit( bliOrgUnit );

        forskOrgUnit = createOrganisationUnit( 'E' );
        forskOrgUnit.setName( "Forskningsparken" );
        organisationUnitService.addOrganisationUnit( forskOrgUnit );

        user = createUser( 'X' );
        userService.addUser( user );

        taskId = new TaskId( TaskCategory.METADATA_IMPORT, user );

        importOptions = new ImportOptions().setImportStrategy( ImportStrategy.UPDATE );
        importOptions.setDryRun( false );
        importOptions.setPreheatCache( true );
    }

    @After
    public void after()
    {
        IOUtils.closeQuietly( inputStream );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testImportGml()
        throws Exception
    {
        MetadataImportParams importParams = new MetadataImportParams();
        importParams.setTaskId( taskId );
        importParams.setUser( user );

        gmlImportService.importGml( inputStream, importParams );

        assertNotNull( boOrgUnit.getCoordinates() );
        assertNotNull( boOrgUnit.getFeatureType() );

        assertNotNull( bontheOrgUnit.getCoordinates() );
        assertNotNull( bontheOrgUnit.getFeatureType() );

        assertNotNull( ojdOrgUnit.getCoordinates() );
        assertNotNull( ojdOrgUnit.getFeatureType() );

        assertNotNull( bliOrgUnit.getCoordinates() );
        assertNotNull( bliOrgUnit.getFeatureType() );

        assertNotNull( forskOrgUnit.getCoordinates() );
        assertNotNull( forskOrgUnit.getFeatureType() );

        // Check if data is correct
        assertEquals( 1, boOrgUnit.getCoordinatesAsList().size() );
        assertEquals( 18, bontheOrgUnit.getCoordinatesAsList().size() );
        assertEquals( 1, ojdOrgUnit.getCoordinatesAsList().size() );
        assertEquals( 1, bliOrgUnit.getCoordinatesAsList().size() );
        assertEquals( 1, forskOrgUnit.getCoordinatesAsList().size() );

        assertEquals( 76, boOrgUnit.getCoordinatesAsList().get( 0 ).getNumberOfCoordinates() );
        assertEquals( 189, bontheOrgUnit.getCoordinatesAsList().get( 1 ).getNumberOfCoordinates() );
        assertEquals( 1, ojdOrgUnit.getCoordinatesAsList().get( 0 ).getNumberOfCoordinates() );
        assertEquals( 1, bliOrgUnit.getCoordinatesAsList().get( 0 ).getNumberOfCoordinates() );
        assertEquals( 76, forskOrgUnit.getCoordinatesAsList().get( 0 ).getNumberOfCoordinates() );
    }
}
