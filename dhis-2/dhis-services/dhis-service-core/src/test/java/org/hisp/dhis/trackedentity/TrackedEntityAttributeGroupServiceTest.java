package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class TrackedEntityAttributeGroupServiceTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityAttributeGroupService attributeGroupService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    private TrackedEntityAttributeGroup attributeGroupA;

    private TrackedEntityAttributeGroup attributeGroupB;

    @Override
    public void setUpTest()
    {
        TrackedEntityAttribute attributeA = createTrackedEntityAttribute( 'A' );
        TrackedEntityAttribute attributeB = createTrackedEntityAttribute( 'B' );
        TrackedEntityAttribute attributeC = createTrackedEntityAttribute( 'C' );

        attributeService.addTrackedEntityAttribute( attributeA );
        attributeService.addTrackedEntityAttribute( attributeB );
        attributeService.addTrackedEntityAttribute( attributeC );

        List<TrackedEntityAttribute> attributesA = new ArrayList<>();
        attributesA.add( attributeA );
        attributesA.add( attributeB );

        List<TrackedEntityAttribute> attributesB = new ArrayList<>();
        attributesB.add( attributeC );

        attributeGroupA = createTrackedEntityAttributeGroup( 'A', attributesA );
        attributeGroupB = createTrackedEntityAttributeGroup( 'B', attributesB );
    }

    @Test
    public void testsaveTrackedEntityAttributeGroup()
    {
        int idA = attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupA );
        int idB = attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupB );

        assertNotNull( attributeGroupService.getTrackedEntityAttributeGroup( idA ) );
        assertNotNull( attributeGroupService.getTrackedEntityAttributeGroup( idB ) );
    }

    @Test
    public void testdeleteTrackedEntityAttributeGroup()
    {
        int idA = attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupA );
        int idB = attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupB );

        assertNotNull( attributeGroupService.getTrackedEntityAttributeGroup( idA ) );
        assertNotNull( attributeGroupService.getTrackedEntityAttributeGroup( idB ) );

        attributeGroupService.deleteTrackedEntityAttributeGroup( attributeGroupA );

        assertNull( attributeGroupService.getTrackedEntityAttributeGroup( idA ) );
        assertNotNull( attributeGroupService.getTrackedEntityAttributeGroup( idB ) );

        attributeGroupService.deleteTrackedEntityAttributeGroup( attributeGroupB );

        assertNull( attributeGroupService.getTrackedEntityAttributeGroup( idA ) );
        assertNull( attributeGroupService.getTrackedEntityAttributeGroup( idB ) );
    }

    @Test
    public void testUpdateEntityInstanceAttributeGroup()
    {
        int idA = attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupA );

        assertNotNull( attributeGroupService.getTrackedEntityAttributeGroup( idA ) );

        attributeGroupA.setName( "B" );
        attributeGroupService.updateTrackedEntityAttributeGroup( attributeGroupA );

        assertEquals( "B", attributeGroupService.getTrackedEntityAttributeGroup( idA ).getName() );
    }

    @Test
    public void testgetTrackedEntityAttributeGroupById()
    {
        int idA = attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupA );
        int idB = attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupB );

        assertEquals( attributeGroupA, attributeGroupService.getTrackedEntityAttributeGroup( idA ) );
        assertEquals( attributeGroupB, attributeGroupService.getTrackedEntityAttributeGroup( idB ) );
    }

    @Test
    public void testgetTrackedEntityAttributeGroupByName()
    {
        int idA = attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupA );

        assertNotNull( attributeGroupService.getTrackedEntityAttributeGroup( idA ) );
        assertEquals( attributeGroupA.getName(),
            attributeGroupService.getTrackedEntityAttributeGroupByName( "TrackedEntityAttributeGroupA" ).getName() );
    }

    @Test
    public void testGetAllTrackedEntityAttributeGroups()
    {
        attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupA );
        attributeGroupService.addTrackedEntityAttributeGroup( attributeGroupB );

        assertTrue( equals( attributeGroupService.getAllTrackedEntityAttributeGroups(), attributeGroupA, attributeGroupB ) );
    }

}
