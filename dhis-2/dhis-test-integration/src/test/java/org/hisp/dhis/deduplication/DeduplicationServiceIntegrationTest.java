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
package org.hisp.dhis.deduplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeduplicationServiceIntegrationTest extends IntegrationTestBase
{

    @Autowired
    private DeduplicationService deduplicationService;

    @Autowired
    private PotentialDuplicateStore potentialDuplicateStore;

    @Autowired
    private UserService userService;

    private static final String teiA = "trackedentA";

    private static final String teiB = "trackedentB";

    private static final String teiC = "trackedentC";

    private static final String teiD = "trackedentD";

    @Override
    public void setUpTest()
    {
        super.userService = this.userService;
        User user = createUserWithAuth( "testUser" );
        injectSecurityContext( user );
    }

    @Test
    void testGetAllPotentialDuplicateByDifferentStatus()
        throws PotentialDuplicateConflictException
    {
        assertEquals( 0, deduplicationService.getAllPotentialDuplicates().size() );
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        PotentialDuplicate potentialDuplicate1 = new PotentialDuplicate( teiC, teiD );
        deduplicationService.addPotentialDuplicate( potentialDuplicate1 );
        PotentialDuplicateQuery potentialDuplicateQuery = new PotentialDuplicateQuery();
        potentialDuplicateQuery.setTeis( Arrays.asList( teiA, teiC ) );
        assertEquals( 2, deduplicationService.getAllPotentialDuplicatesBy( potentialDuplicateQuery ).size() );
        // set one potential duplicate to invalid
        potentialDuplicate.setStatus( DeduplicationStatus.INVALID );
        deduplicationService.updatePotentialDuplicate( potentialDuplicate );
        assertEquals( 2, deduplicationService.getAllPotentialDuplicates().size() );
        potentialDuplicateQuery.setStatus( DeduplicationStatus.OPEN );
        assertEquals( 1, deduplicationService.getAllPotentialDuplicatesBy( potentialDuplicateQuery ).size() );
        potentialDuplicateQuery.setStatus( DeduplicationStatus.INVALID );
        assertEquals( 1, deduplicationService.getAllPotentialDuplicatesBy( potentialDuplicateQuery ).size() );
        potentialDuplicateQuery.setStatus( DeduplicationStatus.ALL );
        assertEquals( 2, deduplicationService.getAllPotentialDuplicatesBy( potentialDuplicateQuery ).size() );
    }

    @Test
    void testAddPotentialDuplicate()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        assertNotEquals( 0, potentialDuplicate.getId() );
        assertEquals( potentialDuplicate,
            deduplicationService.getPotentialDuplicateById( potentialDuplicate.getId() ) );
    }

    @Test
    void testGetPotentialDuplicateByUid()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        assertNotEquals( 0, potentialDuplicate.getId() );
        assertEquals( potentialDuplicate,
            deduplicationService.getPotentialDuplicateByUid( potentialDuplicate.getUid() ) );
    }

    @Test
    void testGetPotentialDuplicateDifferentStatus()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        PotentialDuplicate potentialDuplicate1 = new PotentialDuplicate( teiC, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate1 );

        potentialDuplicate.setStatus( DeduplicationStatus.INVALID );
        deduplicationService.updatePotentialDuplicate( potentialDuplicate );

        potentialDuplicate1.setStatus( DeduplicationStatus.MERGED );
        deduplicationService.updatePotentialDuplicate( potentialDuplicate1 );

        PotentialDuplicateQuery potentialDuplicateQuery = new PotentialDuplicateQuery();
        potentialDuplicateQuery.setTeis( Collections.singletonList( teiB ) );
        potentialDuplicateQuery.setStatus( DeduplicationStatus.INVALID );
        assertEquals( Collections.singletonList( potentialDuplicate ),
            deduplicationService.getAllPotentialDuplicatesBy( potentialDuplicateQuery ) );
    }

    @Test
    void testCreatePotentialDuplicateNotCreationStatus()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        potentialDuplicate.setStatus( DeduplicationStatus.ALL );

        assertThrows( PotentialDuplicateConflictException.class,
            () -> deduplicationService.addPotentialDuplicate( potentialDuplicate ) );
    }

    @Test
    void testGetAllPotentialDuplicates()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        PotentialDuplicate potentialDuplicate1 = new PotentialDuplicate( teiC, teiD );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        deduplicationService.addPotentialDuplicate( potentialDuplicate1 );
        List<PotentialDuplicate> list = deduplicationService.getAllPotentialDuplicates();
        assertEquals( 2, list.size() );
        assertTrue( list.containsAll( Arrays.asList( potentialDuplicate, potentialDuplicate1 ) ) );
    }

    @Test
    void testExistsDuplicate()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        assertTrue( deduplicationService.exists( potentialDuplicate ) );
    }

    @Test
    void testShouldThrowWhenMissingTeiBProperty()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        assertThrows( PotentialDuplicateConflictException.class,
            () -> deduplicationService.exists( new PotentialDuplicate( teiA, null ) ) );
    }

    @Test
    void testShouldThrowWhenMissingTeiAProperty()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        assertThrows( PotentialDuplicateConflictException.class,
            () -> deduplicationService.exists( new PotentialDuplicate( null, teiB ) ) );
    }

    @Test
    void testExistsTwoTeisReverse()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        PotentialDuplicate potentialDuplicateReverse = new PotentialDuplicate( teiB, teiA );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        assertTrue( deduplicationService.exists( potentialDuplicateReverse ) );
    }

    @Test
    void testGetAllPotentialDuplicatedByQuery()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        PotentialDuplicate potentialDuplicate1 = new PotentialDuplicate( teiC, teiD );
        PotentialDuplicate potentialDuplicate2 = new PotentialDuplicate( teiA, teiD );
        PotentialDuplicateQuery query = new PotentialDuplicateQuery();
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        deduplicationService.addPotentialDuplicate( potentialDuplicate1 );
        deduplicationService.addPotentialDuplicate( potentialDuplicate2 );
        query.setTeis( Collections.singletonList( teiA ) );
        List<PotentialDuplicate> list = deduplicationService.getAllPotentialDuplicatesBy( query );
        assertEquals( 2, list.size() );
        assertTrue( list.contains( potentialDuplicate ) );
        assertFalse( list.contains( potentialDuplicate1 ) );
    }

    @Test
    void testCountPotentialDuplicates()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        PotentialDuplicate potentialDuplicate1 = new PotentialDuplicate( teiC, teiD );
        PotentialDuplicateQuery query = new PotentialDuplicateQuery();
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        deduplicationService.addPotentialDuplicate( potentialDuplicate1 );
        query.setStatus( DeduplicationStatus.ALL );
        assertEquals( 2, deduplicationService.countPotentialDuplicates( query ) );
        query.setStatus( DeduplicationStatus.OPEN );
        query.setTeis( Arrays.asList( teiA, teiC ) );
        assertEquals( 2, deduplicationService.countPotentialDuplicates( query ) );
        query.setTeis( Collections.singletonList( teiC ) );
        assertEquals( 1, deduplicationService.countPotentialDuplicates( query ) );
        query.setStatus( DeduplicationStatus.INVALID );
        assertEquals( 0, deduplicationService.countPotentialDuplicates( query ) );
    }

    @Test
    void testUpdatePotentialDuplicate()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        assertEquals( DeduplicationStatus.OPEN,
            deduplicationService.getPotentialDuplicateById( potentialDuplicate.getId() ).getStatus() );
        potentialDuplicate.setStatus( DeduplicationStatus.INVALID );
        deduplicationService.updatePotentialDuplicate( potentialDuplicate );
        assertEquals( DeduplicationStatus.INVALID,
            deduplicationService.getPotentialDuplicateById( potentialDuplicate.getId() ).getStatus() );
    }
}
