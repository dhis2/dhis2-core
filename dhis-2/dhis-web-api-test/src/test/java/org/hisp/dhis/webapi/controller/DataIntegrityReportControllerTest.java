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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.hisp.dhis.webapi.utils.WebClientUtils.objectReference;
import static org.hisp.dhis.webapi.utils.WebClientUtils.objectReferences;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonDocument.JsonNodeType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link DataIntegrityController} API with focus API returning
 * {@link org.hisp.dhis.dataintegrity.FlattenedDataIntegrityReport}.
 *
 * @author Jan Bernitt
 */
class DataIntegrityReportControllerTest extends AbstractDataIntegrityControllerTest
{
    /**
     * Needed to create cyclic references for org units
     */
    @Autowired
    private OrganisationUnitStore organisationUnitStore;

    @Test
    void testNoViolations()
    {
        // if the report does not have any strings in the JSON tree there are no
        // errors since all collection/maps have string values
        JsonDataIntegrityReport report = getDataIntegrityReport();
        assertEquals( 0, report.node().count( JsonNodeType.STRING ) );
    }

    @Test
    void testDataElementChecksOnly()
    {
        JsonDataIntegrityReport report = getDataIntegrityReport( "/dataIntegrity?checks=data-element*" );
        assertEquals( 5, report.size() );
        assertTrue( report.has( "dataElementsWithoutDataSet", "dataElementsWithoutGroups",
            "dataElementsAssignedToDataSetsWithDifferentPeriodTypes", "dataElementsViolatingExclusiveGroupSets",
            "dataElementsInDataSetNotInForm" ) );
    }

    @Test
    void testExclusiveGroupsChecksOnly()
    {
        JsonDataIntegrityReport report = getDataIntegrityReport( "/dataIntegrity?checks=*exclusive-group*" );
        assertEquals( 3, report.size() );
        assertTrue( report.has( "dataElementsViolatingExclusiveGroupSets", "indicatorsViolatingExclusiveGroupSets",
            "organisationUnitsViolatingExclusiveGroupSets" ) );
    }

    @Test
    void testPeriodsDuplicatesOnly()
    {
        JsonDataIntegrityReport report = getDataIntegrityReport( "/dataIntegrity?checks=PERIODS_DUPLICATES" );
        assertEquals( 1, report.size() );
        assertTrue( report.getArray( "duplicatePeriods" ).exists() );
    }

    @Test
    void testOrphanedOrganisationUnits()
    {
        // should match:
        String ouId = addOrganisationUnit( "OrphanedUnit" );
        // should not match:
        String ouRootId = addOrganisationUnit( "root" );
        addOrganisationUnit( "leaf", ouRootId );
        assertEquals( singletonList( "OrphanedUnit:" + ouId ),
            getDataIntegrityReport().getOrphanedOrganisationUnits().toList( JsonString::string ) );
    }

    @Test
    void testOrganisationUnitsWithoutGroups()
    {
        // should match:
        String ouId = addOrganisationUnit( "noGroupSet" );
        // should not match:
        addOrganisationUnitGroup( "group", addOrganisationUnit( "hasGroupSet" ) );
        assertEquals( singletonList( "noGroupSet:" + ouId ),
            getDataIntegrityReport().getOrganisationUnitsWithoutGroups().toList( JsonString::string ) );
    }

    @Test
    void testOrganisationUnitsWithCyclicReferences()
    {
        String ouIdA = addOrganisationUnit( "A" );
        String ouIdB = addOrganisationUnit( "B", ouIdA );
        // create cyclic references (seemingly not possible via REST API)
        OrganisationUnit ouA = organisationUnitStore.getByUid( ouIdA );
        OrganisationUnit ouB = organisationUnitStore.getByUid( ouIdB );
        ouA.setParent( ouB );
        ouB.getChildren().add( ouA );
        organisationUnitStore.save( ouB );
        assertContainsOnly(
            getDataIntegrityReport().getOrganisationUnitsWithCyclicReferences().toList( JsonString::string ),
            "A:" + ouIdA, "B:" + ouIdB );
    }

    @Test
    void testOrganisationUnitsViolatingExclusiveGroupSets()
    {
        String ouIdA = addOrganisationUnit( "A" );
        String ouIdB = addOrganisationUnit( "B" );
        addOrganisationUnit( "C" );
        // all groups created are compulsory
        String groupA0Id = addOrganisationUnitGroup( "A0", ouIdA );
        String groupB1Id = addOrganisationUnitGroup( "B1", ouIdB );
        String groupB2Id = addOrganisationUnitGroup( "B2", ouIdB );
        addOrganisationUnitGroupSet( "K", groupA0Id );
        addOrganisationUnitGroupSet( "X", groupB1Id, groupB2Id );
        assertEquals( singletonMap( "B:" + ouIdB, asList( "B1:" + groupB1Id, "B2:" + groupB2Id ) ),
            getDataIntegrityReport().getOrganisationUnitsViolatingExclusiveGroupSets().toMap( JsonString::string,
                String::compareTo ) );
    }

    private String addOrganisationUnit( String name )
    {
        return assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits", "{'name':'" + name + "', 'shortName':'" + name + "', 'openingDate':'2021'}" ) );
    }

    private String addOrganisationUnit( String name, String parentId )
    {
        return assertStatus( HttpStatus.CREATED, POST( "/organisationUnits", "{'name':'" + name + "', 'shortName':'"
            + name + "', 'openingDate':'2021', 'parent': " + objectReference( parentId ) + " }" ) );
    }

    private String addOrganisationUnitGroup( String name, String... memberIds )
    {
        return assertStatus( HttpStatus.CREATED, POST( "/organisationUnitGroups",
            "{'name':'" + name + "', 'organisationUnits': " + objectReferences( memberIds ) + "}" ) );
    }

    private String addOrganisationUnitGroupSet( String name, String... groupIds )
    {
        // OBS! note that we make them compulsory
        return assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnitGroupSets", "{'name':'" + name + "', 'shortName':'" + name
                + "', 'compulsory': true, 'organisationUnitGroups': " + objectReferences( groupIds ) + "}" ) );
    }

    private JsonDataIntegrityReport getDataIntegrityReport()
    {
        return getDataIntegrityReport( "/dataIntegrity" );
    }

    private JsonDataIntegrityReport getDataIntegrityReport( String url )
    {
        HttpResponse httpResponse = POST( url );
        assertTrue( httpResponse.location().startsWith( "http://localhost/dataIntegrity/details?checks=" ) );
        JsonObject response = httpResponse.content().getObject( "response" );
        String id = response.getString( "id" ).string();
        String jobType = response.getString( "jobType" ).string();
        return GET( "/system/taskSummaries/{type}/{id}", jobType, id ).content().as( JsonDataIntegrityReport.class );
    }
}
