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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class AbstractDataIntegrityIntegrationTest extends DhisControllerIntegrationTest
{
    public final JsonDataIntegrityDetails getDetails( String check )
    {
        JsonObject content = GET( "/dataIntegrity/details?checks={check}&timeout=1000", check ).content();
        JsonDataIntegrityDetails details = content.get( check.replace( '-', '_' ), JsonDataIntegrityDetails.class );
        assertTrue( details.exists(), "check " + check + " did not complete in time or threw an exception" );
        assertTrue( details.isObject() );
        return details;
    }

    public final void postDetails( String check )
    {
        HttpResponse trigger = POST( "/dataIntegrity/details?checks=" + check );
        assertEquals( "http://localhost/dataIntegrity/details?checks=" + check, trigger.location() );
        assertTrue( trigger.content().isA( JsonWebMessage.class ) );
    }

    public final JsonDataIntegritySummary getSummary( String check )
    {
        JsonObject content = GET( "/dataIntegrity/summary?checks={check}&timeout=1000", check ).content();
        JsonDataIntegritySummary summary = content.get( check.replace( '-', '_' ), JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        return summary;
    }

    public final void postSummary( String check )
    {
        HttpResponse trigger = POST( "/dataIntegrity/summary?checks=" + check );
        assertEquals( "http://localhost/dataIntegrity/summary?checks=" + check, trigger.location() );
        assertTrue( trigger.content().isA( JsonWebMessage.class ) );
    }

    public final void organisationUnitPositiveTestTemplate( String check, Integer expectedCount,
        Integer expectedPercentage, String expectedUnit, String expectedName, String expectedComment )
    {
        postSummary( check );
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/" + check + "/summary" )
            .content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( expectedCount, summary.getCount() );
        assertEquals( expectedPercentage, summary.getPercentage().intValue() );

        postDetails( check );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/" + check + "/details" )
            .content()
            .as( JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertTrue( details.isObject() );
        JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
        assertTrue( issues.exists() );
        assertEquals( 1, issues.size() );
        assertEquals( expectedUnit, issues.get( 0 ).getId() );
        assertEquals( expectedName, issues.get( 0 ).getName() );

        if ( expectedComment != null )
        {
            assertTrue( issues.get( 0 ).getComment().toString().contains( expectedComment ) );
        }
        assertEquals( "orgunits", details.getIssuesIdType() );
    }

    public final void organisationUnitPositiveTestTemplate( String check, Integer expectedCount,
        Integer expectedPercentage, Set<String> expectedDetailsUnits,
        Set<String> expectedDetailsNames, Set<String> expectedDetailsComments )
    {

        postSummary( check );
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/" + check + "/summary" )
            .content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( expectedCount, summary.getCount() );
        assertEquals( expectedPercentage, summary.getPercentage().intValue() );

        postDetails( check );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/" + check + "/details" )
            .content()
            .as( JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertTrue( details.isObject() );
        JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
        assertTrue( issues.exists() );

        Set issueUIDs = issues.stream().map( issue -> issue.getId() ).collect( Collectors.toSet() );
        assertEquals( issueUIDs, expectedDetailsUnits );

        if ( expectedDetailsNames != null )
        {
            Set detailsNames = issues.stream().map( issue -> issue.getName() ).collect( Collectors.toSet() );
            assertEquals( expectedDetailsNames, detailsNames );
        }
        if ( expectedDetailsComments != null )
        {
            Set detailsComments = issues.stream().map( issue -> issue.getComment() ).collect( Collectors.toSet() );
            assertEquals( expectedDetailsNames, detailsComments );
        }

        assertEquals( "orgunits", details.getIssuesIdType() );
    }

    void deleteAllOrgUnits()
    {
        GET( "/organisationUnits/gist?fields=id&headless=true" ).content().stringValues()
            .forEach( id -> DELETE( "/organisationUnits/" + id ) );
        JsonResponse response = GET( "/organisationUnits/" ).content();
        JsonArray dimensions = response.getArray( "organisationUnits" );
        assertEquals( 0, dimensions.size() );
    }

    boolean DeleteMetadataObject( String endpoint, String uid )

    {

        if ( endpoint == null )
        {
            return false;
        }

        if ( uid != null )
        {
            assertStatus( HttpStatus.OK,
                DELETE( "/" + endpoint + "/" + uid ) );
            assertStatus( HttpStatus.NOT_FOUND,
                GET( "/" + endpoint + "/" + uid ) );
        }
        return true;
    }

    @Autowired
    private TransactionTemplate txTemplate;

    protected void doInTransaction( Runnable operation )
    {
        txTemplate.execute( status -> {
            operation.run();
            return null;
        } );
        ;
    }
}
