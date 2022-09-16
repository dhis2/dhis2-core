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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonOrganisationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CrudControllerPagingTest extends DhisControllerConvenienceTest
{
    @Autowired
    private OrganisationUnitService ouService;

    @BeforeEach
    void setUp()
    {
        ouService.addOrganisationUnit( createOrganisationUnit( "A" ) );
        ouService.addOrganisationUnit( createOrganisationUnit( "B" ) );
        ouService.addOrganisationUnit( createOrganisationUnit( "C" ) );
        ouService.addOrganisationUnit( createOrganisationUnit( "D" ) );
        ouService.addOrganisationUnit( createOrganisationUnit( "E" ) );
    }

    @Test
    void testPage1()
    {
        JsonList<JsonOrganisationUnit> ous = GET( "/organisationUnits?paging=true&pageSize=2&page=1" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 2, ous.size() );
    }

    @Test
    void testPage3()
    {
        JsonList<JsonOrganisationUnit> ous = GET( "/organisationUnits?paging=true&pageSize=2&page=3" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 1, ous.size() );
    }

    @Test
    void testPage2AndOrderByDisplayName()
    {
        JsonList<JsonOrganisationUnit> ous = GET( "/organisationUnits?order=displayName&paging=true&pageSize=2&page=2" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 2, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
        assertEquals( "D", ous.get( 1 ).getDisplayName() );
    }

    @Test
    void testPage3AndOrderByDisplayName()
    {
        JsonList<JsonOrganisationUnit> ous = GET( "/organisationUnits?order=displayName&paging=true&pageSize=2&page=3" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 1, ous.size() );
        assertEquals( "E", ous.get( 0 ).getDisplayName() );
    }

    @Test
    void testPage2AndOrderByName()
    {
        JsonList<JsonOrganisationUnit> ous = GET( "/organisationUnits?order=name&paging=true&pageSize=2&page=2" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 2, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
        assertEquals( "D", ous.get( 1 ).getDisplayName() );
    }

    @Test
    void testPage2AndFilterByName()
    {
        JsonList<JsonOrganisationUnit> ous = GET(
            "/organisationUnits?filter=name:in:[A,B,C]&paging=true&pageSize=2&page=2" ).content( HttpStatus.OK )
                .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 1, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
    }

    @Test
    void testPage2AndFilterByDisplayName()
    {
        JsonList<JsonOrganisationUnit> ous = GET(
            "/organisationUnits?filter=displayName:in:[A,B,C]&paging=true&pageSize=2&page=2" ).content( HttpStatus.OK )
                .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 1, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
    }
}
