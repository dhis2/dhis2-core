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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests for orgunits which have multiple spaces in their names or shortnames
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitNamesMultipleSpacesTest extends DhisControllerIntegrationTest
{
    @Autowired
    private OrganisationUnitService orgUnitService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private TransactionTemplate txTemplate;

    private OrganisationUnit unitA;

    private OrganisationUnit unitB;

    private OrganisationUnit unitC;

    private User superUser;

    @Test
    void testOrgUnitMultipleSpaces()
    {
        doInTransaction( () -> {

            superUser = preCreateInjectAdminUser();
            injectSecurityContext( superUser );

            unitA = createOrganisationUnit( 'A' );
            unitA.setName( "Lots     of      spaces" );
            unitA.setShortName( "Lots     of      spaces" );
            orgUnitService.addOrganisationUnit( unitA );

            unitB = createOrganisationUnit( 'B' );
            unitB.setName( "Some spaces" );
            unitB.setShortName( "Some      spaces" );
            orgUnitService.addOrganisationUnit( unitB );

            unitC = createOrganisationUnit( 'C' );
            unitC.setName( "Just enough space" );
            unitC.setShortName( "Just enough space" );
            orgUnitService.addOrganisationUnit( unitC );

            dbmsManager.clearSession();
        } );

        postSummary( "orgunit_multiple_spaces" );
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/orgunit_multiple_spaces/summary" ).content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 2, summary.getCount() );
        assertEquals( 66, summary.getPercentage().intValue() );

        postDetails( "orgunit_multiple_spaces" );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/orgunit_multiple_spaces/details" ).content()
            .as( JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertTrue( details.isObject() );
        JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
        assertTrue( issues.exists() );

        assertEquals( 2, issues.size() );

        HashSet<String> issueUIDs = new HashSet<String>();
        issueUIDs.add( issues.get( 0 ).getId() );
        issueUIDs.add( issues.get( 1 ).getId() );

        HashSet<String> orgUnitUIDs = new HashSet<String>();
        orgUnitUIDs.add( unitA.getUid() );
        orgUnitUIDs.add( unitB.getUid() );

        assertEquals( issueUIDs, orgUnitUIDs );
        assertEquals( "orgunits", details.getIssuesIdType() );
    }

    protected final void postDetails( String check )
    {
        WebClient.HttpResponse trigger = POST( "/dataIntegrity/details?checks=" + check );
        assertEquals( "http://localhost/dataIntegrity/details?checks=" + check, trigger.location() );
        assertTrue( trigger.content().isA( JsonWebMessage.class ) );
    }

    protected final void postSummary( String check )
    {
        WebClient.HttpResponse trigger = POST( "/dataIntegrity/summary?checks=" + check );
        assertEquals( "http://localhost/dataIntegrity/summary?checks=" + check, trigger.location() );
        assertTrue( trigger.content().isA( JsonWebMessage.class ) );
    }

    protected void doInTransaction( Runnable operation )
    {
        final int defaultPropagationBehaviour = txTemplate.getPropagationBehavior();
        txTemplate.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRES_NEW );
        txTemplate.execute( status -> {
            operation.run();
            return null;
        } );
        // restore original propagation behaviour
        txTemplate.setPropagationBehavior( defaultPropagationBehaviour );
    }
}
