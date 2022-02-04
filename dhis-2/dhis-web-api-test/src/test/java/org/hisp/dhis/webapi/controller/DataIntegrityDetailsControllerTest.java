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

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.dataintegrity.DataIntegrityCheckType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link DataIntegrityController} API with focus API returning
 * {@link org.hisp.dhis.dataintegrity.DataIntegrityDetails}.
 *
 * @author Jan Bernitt
 */
class DataIntegrityDetailsControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testCategories_no_options()
    {
        String uid = assertStatus( HttpStatus.CREATED,
            POST( "/categories", "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}" ) );
        JsonObject content = GET( "/dataIntegrity/details?checks=categories-no-options" ).content();
        JsonDataIntegrityDetails details = content.get( "categories_no_options", JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertEquals( 1, details.getIssues().size() );
        assertEquals( uid, details.getIssues().get( 0 ).getId() );
        assertEquals( "CatDog", details.getIssues().get( 0 ).getName() );
    }

    @Test
    void testLegacyChecksOnly()
    {
        for ( DataIntegrityCheckType type : DataIntegrityCheckType.values() )
        {
            JsonObject content = GET( "/dataIntegrity/details?checks={name}", type.getName() ).content();
            JsonDataIntegrityDetails details = content.get( type.getName(), JsonDataIntegrityDetails.class );
            assertTrue( details.exists() );
            assertTrue( details.isObject() );
            assertTrue( details.getIssues().isEmpty() );
        }
    }

    @Test
    void testSingleCheckByPath()
    {
        String uid = assertStatus( HttpStatus.CREATED,
            POST( "/categories", "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}" ) );
        JsonDataIntegrityDetails details = GET( "/dataIntegrity/categories-no-options/details" ).content()
            .as( JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertEquals( 1, details.getIssues().size() );
        assertEquals( uid, details.getIssues().get( 0 ).getId() );
        assertEquals( "CatDog", details.getIssues().get( 0 ).getName() );
    }
}
