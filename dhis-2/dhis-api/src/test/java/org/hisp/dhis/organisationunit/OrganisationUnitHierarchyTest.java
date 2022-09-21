/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.organisationunit;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class OrganisationUnitHierarchyTest
{
    @Test
    public void testGetGroupChildren()
    {
        OrganisationUnitGroup group = new OrganisationUnitGroup( "Group" );
        group.setId( 1 );

        OrganisationUnit unit2 = new OrganisationUnit( "Unit2" );
        OrganisationUnit unit4 = new OrganisationUnit( "Unit4" );
        OrganisationUnit unit6 = new OrganisationUnit( "Unit6" );
        OrganisationUnit unit8 = new OrganisationUnit( "Unit8" );
        OrganisationUnit unit10 = new OrganisationUnit( "Unit10" );
        OrganisationUnit unit12 = new OrganisationUnit( "Unit12" );

        unit2.setId( 2 );
        unit4.setId( 4 );
        unit6.setId( 6 );
        unit8.setId( 8 );
        unit10.setId( 10 );
        unit12.setId( 12 );

        group.addOrganisationUnit( unit2 );
        group.addOrganisationUnit( unit4 );
        group.addOrganisationUnit( unit6 );
        group.addOrganisationUnit( unit8 );
        group.addOrganisationUnit( unit10 );
        group.addOrganisationUnit( unit12 );

        List<OrganisationUnitRelationship> relationships = new ArrayList<>();

        relationships.add( new OrganisationUnitRelationship( 1, 2 ) );
        relationships.add( new OrganisationUnitRelationship( 1, 3 ) );
        relationships.add( new OrganisationUnitRelationship( 2, 4 ) );
        relationships.add( new OrganisationUnitRelationship( 2, 5 ) );
        relationships.add( new OrganisationUnitRelationship( 2, 6 ) );
        relationships.add( new OrganisationUnitRelationship( 3, 7 ) );
        relationships.add( new OrganisationUnitRelationship( 3, 8 ) );
        relationships.add( new OrganisationUnitRelationship( 3, 9 ) );
        relationships.add( new OrganisationUnitRelationship( 4, 10 ) );
        relationships.add( new OrganisationUnitRelationship( 4, 11 ) );
        relationships.add( new OrganisationUnitRelationship( 4, 12 ) );

        OrganisationUnitHierarchy hierarchy = new OrganisationUnitHierarchy( relationships );

        assertEquals( 6, hierarchy.getChildren( 1, group ).size() );

        assertEquals( 5, hierarchy.getChildren( 2, group ).size() );
        assertTrue( hierarchy.getChildren( 2, group ).contains( 2l ) );
        assertTrue( hierarchy.getChildren( 2, group ).contains( 4l ) );
        assertTrue( hierarchy.getChildren( 2, group ).contains( 6l ) );
        assertTrue( hierarchy.getChildren( 2, group ).contains( 10l ) );
        assertTrue( hierarchy.getChildren( 2, group ).contains( 12l ) );

        assertEquals( 1, hierarchy.getChildren( 3, group ).size() );
        assertTrue( hierarchy.getChildren( 3, group ).contains( 8l ) );

        assertEquals( 3, hierarchy.getChildren( 4, group ).size() );
        assertTrue( hierarchy.getChildren( 4, group ).contains( 4l ) );
        assertTrue( hierarchy.getChildren( 4, group ).contains( 10l ) );
        assertTrue( hierarchy.getChildren( 4, group ).contains( 12l ) );

        assertEquals( 0, hierarchy.getChildren( 11, group ).size() );

        assertFalse( hierarchy.getChildren( 5, group ).contains( 10l ) );
        assertFalse( hierarchy.getChildren( 3, group ).contains( 11l ) );
    }

    @Test
    public void testGetChildren()
    {
        Set<Long> parentIds = new HashSet<>();

        List<OrganisationUnitRelationship> relations = new ArrayList<>();

        int parentMax = 1000; // Increase to stress-test
        int childMax = 4;
        int childId = 0;

        for ( long parentId = 0; parentId < parentMax; parentId++ )
        {
            parentIds.add( parentId );

            for ( int j = 0; j < childMax; j++ )
            {
                relations.add( new OrganisationUnitRelationship( parentId, ++childId ) );
            }
        }

        OrganisationUnitHierarchy hierarchy = new OrganisationUnitHierarchy( relations );

        Set<Long> children = hierarchy.getChildren( parentIds );

        assertNotNull( children );
        assertEquals( (parentMax * childMax) + 1, children.size() );
    }

    @Test
    public void testGetChildrenA()
    {
        List<OrganisationUnitRelationship> relationships = new ArrayList<>();

        relationships.add( new OrganisationUnitRelationship( 1, 2 ) );
        relationships.add( new OrganisationUnitRelationship( 1, 3 ) );
        relationships.add( new OrganisationUnitRelationship( 2, 4 ) );
        relationships.add( new OrganisationUnitRelationship( 2, 5 ) );
        relationships.add( new OrganisationUnitRelationship( 2, 6 ) );
        relationships.add( new OrganisationUnitRelationship( 3, 7 ) );
        relationships.add( new OrganisationUnitRelationship( 3, 8 ) );
        relationships.add( new OrganisationUnitRelationship( 3, 9 ) );
        relationships.add( new OrganisationUnitRelationship( 4, 10 ) );
        relationships.add( new OrganisationUnitRelationship( 4, 11 ) );
        relationships.add( new OrganisationUnitRelationship( 4, 12 ) );

        OrganisationUnitHierarchy hierarchy = new OrganisationUnitHierarchy( relationships );

        testHierarchy( hierarchy );
    }

    @Test
    public void testGetChildrenB()
    {
        Map<Long, Set<Long>> relationships = new HashMap<>();

        relationships.put( 1l, getSet( 2l, 3l ) );
        relationships.put( 2l, getSet( 4l, 5l, 6l ) );
        relationships.put( 3l, getSet( 7l, 8l, 9l ) );
        relationships.put( 4l, getSet( 10l, 11l, 12l ) );

        OrganisationUnitHierarchy hierarchy = new OrganisationUnitHierarchy( relationships );

        testHierarchy( hierarchy );
    }

    private void testHierarchy( OrganisationUnitHierarchy hierarchy )
    {
        assertEquals( 12, hierarchy.getChildren( 1 ).size() );

        assertEquals( 7, hierarchy.getChildren( 2 ).size() );
        assertTrue( hierarchy.getChildren( 2 ).contains( 2l ) );
        assertTrue( hierarchy.getChildren( 2 ).contains( 4l ) );
        assertTrue( hierarchy.getChildren( 2 ).contains( 5l ) );
        assertTrue( hierarchy.getChildren( 2 ).contains( 6l ) );
        assertTrue( hierarchy.getChildren( 2 ).contains( 10l ) );
        assertTrue( hierarchy.getChildren( 2 ).contains( 11l ) );
        assertTrue( hierarchy.getChildren( 2 ).contains( 12l ) );

        assertEquals( 4, hierarchy.getChildren( 3 ).size() );
        assertTrue( hierarchy.getChildren( 3 ).contains( 3l ) );
        assertTrue( hierarchy.getChildren( 3 ).contains( 7l ) );
        assertTrue( hierarchy.getChildren( 3 ).contains( 8l ) );
        assertTrue( hierarchy.getChildren( 3 ).contains( 9l ) );

        assertEquals( 4, hierarchy.getChildren( 4 ).size() );
        assertTrue( hierarchy.getChildren( 4 ).contains( 4l ) );
        assertTrue( hierarchy.getChildren( 4 ).contains( 10l ) );
        assertTrue( hierarchy.getChildren( 4 ).contains( 11l ) );
        assertTrue( hierarchy.getChildren( 4 ).contains( 12l ) );

        assertEquals( 1, hierarchy.getChildren( 11 ).size() );
        assertTrue( hierarchy.getChildren( 11 ).contains( 11l ) );

        assertFalse( hierarchy.getChildren( 2 ).contains( 3l ) );
        assertFalse( hierarchy.getChildren( 2 ).contains( 8l ) );
    }

    private Set<Long> getSet( Long... ints )
    {
        Set<Long> set = new HashSet<>();

        Collections.addAll( set, ints );

        return set;
    }
}
