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
package org.hisp.dhis.deduplication;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

@Category( IntegrationTest.class )
public class DeduplicationServiceTest
    extends IntegrationTestBase
{

    @Autowired
    private DeduplicationService deduplicationService;

    @Autowired
    private PotentialDuplicateStore potentialDuplicateStore;

    @Autowired
    private UserService userService;

    private CurrentUserService currentUserService;

    @Override
    public void setUpTest()
    {
        super.userService = this.userService;
        User user = createUser( "testUser" );
        currentUserService = new MockCurrentUserService( user );
        setDependency( potentialDuplicateStore, "currentUserService", currentUserService );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Test
    public void testAddPotentialDuplicate()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( "ABCDEF12345" );
        long id = deduplicationService.addPotentialDuplicate( potentialDuplicate );

        assertNotNull( id );

        assertEquals( potentialDuplicate, deduplicationService.getPotentialDuplicateById( id ) );
    }

    @Test
    public void testGetPotentialDuplicateByUid()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( "ABCDEF12345" );
        long id = deduplicationService.addPotentialDuplicate( potentialDuplicate );

        assertNotNull( id );
        assertEquals( potentialDuplicate,
            deduplicationService.getPotentialDuplicateByUid( potentialDuplicate.getUid() ) );
    }

    @Test
    public void testGetAllPotentialDuplicates()
    {
        PotentialDuplicate pd1 = new PotentialDuplicate( "ABCDEFGHIJ1" );
        PotentialDuplicate pd2 = new PotentialDuplicate( "ABCDEFGHIJ2" );
        PotentialDuplicate pd3 = new PotentialDuplicate( "ABCDEFGHIJ3" );

        deduplicationService.addPotentialDuplicate( pd1 );
        deduplicationService.addPotentialDuplicate( pd2 );
        deduplicationService.addPotentialDuplicate( pd3 );

        List<PotentialDuplicate> list = deduplicationService.getAllPotentialDuplicates();

        assertEquals( 3, list.size() );
        assertTrue( list.contains( pd1 ) );
        assertTrue( list.contains( pd2 ) );
        assertTrue( list.contains( pd3 ) );
    }

    @Test
    public void testExistsOneTei()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( "ABCDEF12345" );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );

        assertTrue( deduplicationService.exists( potentialDuplicate ) );
    }

    @Test
    public void testExistsTwoTeis()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( "ABCDEFGHIJ1", "ABCDEFGHIJ2" );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );

        assertTrue( deduplicationService.exists( potentialDuplicate ) );
    }

    @Test
    public void testExistsTwoTeisReverse()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( "ABCDEFGHIJ1", "ABCDEFGHIJ2" );
        PotentialDuplicate potentialDuplicateReverse = new PotentialDuplicate( "ABCDEFGHIJ2", "ABCDEFGHIJ1" );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );

        assertTrue( deduplicationService.exists( potentialDuplicateReverse ) );
    }

    @Test
    public void testGetAllPotentialDuplicatedByQuery()
    {
        PotentialDuplicate pd1 = new PotentialDuplicate( "ABCDEFGHIJ1" );
        PotentialDuplicate pd2 = new PotentialDuplicate( "ABCDEFGHIJ1", "ABCDEFGHIJ2" );
        PotentialDuplicate pd3 = new PotentialDuplicate( "ABCDEFGHIJ3" );

        PotentialDuplicateQuery query = new PotentialDuplicateQuery();

        deduplicationService.addPotentialDuplicate( pd1 );
        deduplicationService.addPotentialDuplicate( pd2 );
        deduplicationService.addPotentialDuplicate( pd3 );

        query.setTeis( Arrays.asList( "ABCDEFGHIJ1" ) );

        List<PotentialDuplicate> list = deduplicationService.getAllPotentialDuplicates( query );

        assertEquals( 2, list.size() );
        assertTrue( list.contains( pd1 ) );
        assertTrue( list.contains( pd2 ) );
    }

    @Test
    public void testCountPotentialDuplicates()
    {
        PotentialDuplicate pd1 = new PotentialDuplicate( "ABCDEFGHIJ1" );
        PotentialDuplicate pd2 = new PotentialDuplicate( "ABCDEFGHIJ1", "ABCDEFGHIJ2" );
        PotentialDuplicate pd3 = new PotentialDuplicate( "ABCDEFGHIJ3" );

        PotentialDuplicateQuery query = new PotentialDuplicateQuery();

        deduplicationService.addPotentialDuplicate( pd1 );
        deduplicationService.addPotentialDuplicate( pd2 );
        deduplicationService.addPotentialDuplicate( pd3 );

        query.setTeis( Arrays.asList( "ABCDEFGHIJ1" ) );

        int count = deduplicationService.countPotentialDuplicates( query );

        assertEquals( 2, count );

        query.setTeis( Arrays.asList( "ABCDEFGHIJ2" ) );
        count = deduplicationService.countPotentialDuplicates( query );

        assertEquals( 1, count );
    }

    @Test
    public void testMarkPotentialDuplicateInvalid()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( "ABCDEFGHIJ1", "ABCDEFGHIJ2" );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );

        assertEquals( DeduplicationStatus.OPEN, potentialDuplicate.getStatus() );

        deduplicationService.markPotentialDuplicateInvalid( potentialDuplicate );

        assertEquals( DeduplicationStatus.INVALID, potentialDuplicate.getStatus() );
    }

}
