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
package org.hisp.dhis.deduplication.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.hisp.dhis.deduplication.PotentialDuplicateStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

@Disabled( "moveAttributes method do not really belong to a store now. We should a better place for it" )
class PotentialDuplicateStoreTEAVTest extends IntegrationTestBase
{

    @Autowired
    private PotentialDuplicateStore potentialDuplicateStore;

    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    private TrackedEntity original;

    private TrackedEntity duplicate;

    private TrackedEntity control;

    private TrackedEntityAttribute trackedEntityAttributeA;

    private TrackedEntityAttribute trackedEntityAttributeB;

    private TrackedEntityAttribute trackedEntityAttributeC;

    private TrackedEntityAttribute trackedEntityAttributeD;

    private TrackedEntityAttribute trackedEntityAttributeE;

    @BeforeEach
    void setupTest()
    {
        OrganisationUnit ou = createOrganisationUnit( "OU_A" );
        organisationUnitService.addOrganisationUnit( ou );
        original = createTrackedEntityInstance( ou );
        duplicate = createTrackedEntityInstance( ou );
        control = createTrackedEntityInstance( ou );
        trackedEntityService.addTrackedEntity( original );
        trackedEntityService.addTrackedEntity( duplicate );
        trackedEntityService.addTrackedEntity( control );
        trackedEntityAttributeA = createTrackedEntityAttribute( 'A' );
        trackedEntityAttributeB = createTrackedEntityAttribute( 'B' );
        trackedEntityAttributeC = createTrackedEntityAttribute( 'C' );
        trackedEntityAttributeD = createTrackedEntityAttribute( 'D' );
        trackedEntityAttributeE = createTrackedEntityAttribute( 'E' );
        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttributeA );
        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttributeB );
        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttributeC );
        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttributeD );
        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttributeE );
        original.addAttributeValue( createTrackedEntityAttributeValue( 'A', original, trackedEntityAttributeA ) );
        original.addAttributeValue( createTrackedEntityAttributeValue( 'A', original, trackedEntityAttributeB ) );
        original.addAttributeValue( createTrackedEntityAttributeValue( 'A', original, trackedEntityAttributeC ) );
        duplicate.addAttributeValue( createTrackedEntityAttributeValue( 'B', duplicate, trackedEntityAttributeA ) );
        duplicate.addAttributeValue( createTrackedEntityAttributeValue( 'B', duplicate, trackedEntityAttributeB ) );
        duplicate.addAttributeValue( createTrackedEntityAttributeValue( 'B', duplicate, trackedEntityAttributeC ) );
        duplicate.addAttributeValue( createTrackedEntityAttributeValue( 'B', duplicate, trackedEntityAttributeD ) );
        duplicate.addAttributeValue( createTrackedEntityAttributeValue( 'B', duplicate, trackedEntityAttributeE ) );
        control.addAttributeValue( createTrackedEntityAttributeValue( 'C', control, trackedEntityAttributeA ) );
        control.addAttributeValue( createTrackedEntityAttributeValue( 'C', control, trackedEntityAttributeB ) );
        control.addAttributeValue( createTrackedEntityAttributeValue( 'C', control, trackedEntityAttributeC ) );
        original.getTrackedEntityAttributeValues()
            .forEach( trackedEntityAttributeValueService::addTrackedEntityAttributeValue );
        duplicate.getTrackedEntityAttributeValues()
            .forEach( trackedEntityAttributeValueService::addTrackedEntityAttributeValue );
        control.getTrackedEntityAttributeValues()
            .forEach( trackedEntityAttributeValueService::addTrackedEntityAttributeValue );
    }

    @Test
    void moveTrackedEntityAttributeValuesSingleTea()
    {
        List<String> teas = Lists.newArrayList( trackedEntityAttributeA.getUid() );
        transactionTemplate.execute( status -> {
            potentialDuplicateStore.moveTrackedEntityAttributeValues( original, duplicate, teas );
            return null;
        } );
        transactionTemplate.execute( status -> {
            // Clear the session so we get new data from the DB for the next
            // queries.
            dbmsManager.clearSession();
            TrackedEntity _original = trackedEntityService
                .getTrackedEntity( original.getUid() );
            TrackedEntity _duplicate = trackedEntityService
                .getTrackedEntity( duplicate.getUid() );
            assertNotNull( _original );
            assertNotNull( _duplicate );
            assertEquals( 3, _original.getTrackedEntityAttributeValues().size() );
            assertEquals( 4, _duplicate.getTrackedEntityAttributeValues().size() );
            _original.getTrackedEntityAttributeValues().forEach( teav -> {
                if ( teas.contains( teav.getAttribute().getUid() ) )
                {
                    assertEquals( "AttributeB", teav.getValue() );
                }
                else
                {
                    assertEquals( "AttributeA", teav.getValue() );
                }
            } );
            TrackedEntity _control = trackedEntityService.getTrackedEntity( control.getUid() );
            assertNotNull( _control );
            assertEquals( 3, _control.getTrackedEntityAttributeValues().size() );
            return null;
        } );
    }

    @Test
    void moveTrackedEntityAttributeValuesMultipleTeas()
    {
        List<String> teas = Lists.newArrayList( trackedEntityAttributeA.getUid(), trackedEntityAttributeB.getUid() );
        transactionTemplate.execute( status -> {
            potentialDuplicateStore.moveTrackedEntityAttributeValues( original, duplicate, teas );
            return null;
        } );
        transactionTemplate.execute( status -> {
            // Clear the session so we get new data from the DB for the next
            // queries.
            dbmsManager.clearSession();
            TrackedEntity _original = trackedEntityService
                .getTrackedEntity( original.getUid() );
            TrackedEntity _duplicate = trackedEntityService
                .getTrackedEntity( duplicate.getUid() );
            assertNotNull( _original );
            assertNotNull( _duplicate );
            assertEquals( 3, _original.getTrackedEntityAttributeValues().size() );
            assertEquals( 3, _duplicate.getTrackedEntityAttributeValues().size() );
            _original.getTrackedEntityAttributeValues().forEach( teav -> {
                if ( teas.contains( teav.getAttribute().getUid() ) )
                {
                    assertEquals( "AttributeB", teav.getValue() );
                }
                else
                {
                    assertEquals( "AttributeA", teav.getValue() );
                }
            } );
            TrackedEntity _control = trackedEntityService.getTrackedEntity( control.getUid() );
            assertNotNull( _control );
            assertEquals( 3, _control.getTrackedEntityAttributeValues().size() );
            return null;
        } );
    }

    @Test
    void moveTrackedEntityAttributeValuesByOverwritingAndCreatingNew()
    {
        List<String> teas = Lists.newArrayList( trackedEntityAttributeD.getUid(), trackedEntityAttributeB.getUid() );
        transactionTemplate.execute( status -> {
            potentialDuplicateStore.moveTrackedEntityAttributeValues( original, duplicate, teas );
            return null;
        } );
        transactionTemplate.execute( status -> {
            // Clear the session so we get new data from the DB for the next
            // queries.
            dbmsManager.clearSession();
            TrackedEntity _original = trackedEntityService
                .getTrackedEntity( original.getUid() );
            TrackedEntity _duplicate = trackedEntityService
                .getTrackedEntity( duplicate.getUid() );
            assertNotNull( _original );
            assertNotNull( _duplicate );
            assertEquals( 4, _original.getTrackedEntityAttributeValues().size() );
            assertEquals( 3, _duplicate.getTrackedEntityAttributeValues().size() );
            _original.getTrackedEntityAttributeValues().forEach( teav -> {
                if ( teas.contains( teav.getAttribute().getUid() ) )
                {
                    assertEquals( "AttributeB", teav.getValue() );
                }
                else
                {
                    assertEquals( "AttributeA", teav.getValue() );
                }
            } );
            TrackedEntity _control = trackedEntityService.getTrackedEntity( control.getUid() );
            assertNotNull( _control );
            assertEquals( 3, _control.getTrackedEntityAttributeValues().size() );
            return null;
        } );
    }
}
