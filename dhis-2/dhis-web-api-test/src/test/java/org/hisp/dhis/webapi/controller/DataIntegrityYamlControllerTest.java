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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityCheck;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link DataIntegrityController} API with focus on the checks
 * originating from a YAML configuration.
 */
public class DataIntegrityYamlControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testGetAvailableChecks()
    {
        JsonList<JsonDataIntegrityCheck> checks = GET( "/dataIntegrity" ).content()
            .asList( JsonDataIntegrityCheck.class );

        assertFalse( checks.isEmpty() );
        Optional<JsonDataIntegrityCheck> categories_no_options = checks.stream()
            .filter( check -> check.getName().equals( "categories_no_options" ) ).findFirst();
        assertTrue( categories_no_options.isPresent() );
    }

    @Test
    public void testSummaries_categories_no_options()
    {
        assertStatus( HttpStatus.CREATED,
            POST( "/categories", "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}" ) );

        JsonObject content = GET( "/dataIntegrity/summary?checks=categories-no-options" ).content();

        JsonDataIntegritySummary summary = content.get( "categories_no_options", JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertEquals( 1, summary.getCount() );
        assertEquals( 50, summary.getPercentage().intValue() );
    }

    @Test
    public void testDetails_categories_no_options()
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
}
