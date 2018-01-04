package org.hisp.dhis.dxf2.csv;

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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CsvImportServiceTest
    extends DhisSpringTest
{
    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    private InputStream inputBasicObjects;

    private InputStream orgUnitGroupMembership;

    private OrganisationUnit organisationUnit_A;

    private OrganisationUnit organisationUnit_B;

    private OrganisationUnit organisationUnit_C;

    private OrganisationUnit organisationUnit_D;

    private OrganisationUnitGroup organisationUnitGroup_A;

    private OrganisationUnitGroup organisationUnitGroup_B;

    @Override
    protected void setUpTest()
        throws Exception
    {
        inputBasicObjects = new ClassPathResource( "csv/basic_objects.csv" ).getInputStream();
        orgUnitGroupMembership = new ClassPathResource( "csv/org_unit_group_membership.csv" ).getInputStream();

        organisationUnitGroup_A = createOrganisationUnitGroup( 'A' );
        organisationUnitGroup_B = createOrganisationUnitGroup( 'B' );

        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup_A );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroup_B );

        organisationUnit_A = createOrganisationUnit( 'A' );
        organisationUnit_B = createOrganisationUnit( 'B' );
        organisationUnit_C = createOrganisationUnit( 'C' );
        organisationUnit_D = createOrganisationUnit( 'D' );

        organisationUnitService.addOrganisationUnit( organisationUnit_A );
        organisationUnitService.addOrganisationUnit( organisationUnit_B );
        organisationUnitService.addOrganisationUnit( organisationUnit_C );
        organisationUnitService.addOrganisationUnit( organisationUnit_D );
    }

    @Test
    public void testCategoryOptionImport()
        throws IOException
    {
        Metadata metadata = csvImportService.fromCsv( inputBasicObjects, CsvImportClass.CATEGORY_OPTION );

        assertEquals( 3, metadata.getCategoryOptions().size() );

        for ( DataElementCategoryOption categoryOption : metadata.getCategoryOptions() )
        {
            assertNotNull( categoryOption.getUid() );
            assertNotNull( categoryOption.getName() );
            assertNotNull( categoryOption.getShortName() );
        }
    }

    @Test
    public void testOrganisationUnitGroupMembershipImport()
        throws IOException
    {
        Metadata metadata = csvImportService
            .fromCsv( orgUnitGroupMembership, CsvImportClass.ORGANISATION_UNIT_GROUP_MEMBERSHIP );

        assertEquals( 2, metadata.getOrganisationUnitGroups().size() );
    }
}
