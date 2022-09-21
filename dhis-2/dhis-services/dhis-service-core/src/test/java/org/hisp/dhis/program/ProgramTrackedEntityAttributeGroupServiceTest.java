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
package org.hisp.dhis.program;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class ProgramTrackedEntityAttributeGroupServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramTrackedEntityAttributeGroupService service;

    @Autowired
    private IdentifiableObjectManager manager;

    private Program prA;

    private TrackedEntityAttribute teaA;

    private TrackedEntityAttribute teaB;

    private ProgramTrackedEntityAttribute attrA;

    private ProgramTrackedEntityAttribute attrB;

    private ProgramTrackedEntityAttributeGroup group;

    @Override
    public void setUpTest()
    {
        prA = createProgram( 'A' );
        manager.save( prA );

        teaA = createTrackedEntityAttribute( 'A' );
        teaB = createTrackedEntityAttribute( 'B' );
        manager.save( teaA );
        manager.save( teaB );

        attrA = createProgramTrackedEntityAttribute( prA, teaA );
        attrB = createProgramTrackedEntityAttribute( prA, teaB );
        Set<ProgramTrackedEntityAttribute> attributes = new HashSet<>();
        attributes.add( attrA );
        attributes.add( attrB );

        group = createProgramTrackedEntityAttributeGroup( 'A' );
    }

    @Test
    public void testAddAndUpdate()
    {
        manager.save( attrA );
        manager.save( attrB );

        group.addAttribute( attrA );
        group.addAttribute( attrB );

        service.addProgramTrackedEntityAttributeGroup( group );

        assertNotNull( service.getProgramTrackedEntityAttributeGroup( group.getUid() ) );

        assertEquals( 2, group.getAttributes().size() );

        group.setShortName( "updatedShortName" );

        service.updateProgramTrackedEntityAttributeGroup( group );

        assertEquals( "updatedShortName",
            service.getProgramTrackedEntityAttributeGroup( group.getUid() ).getShortName() );

    }

    @Test
    public void testDelete()
    {
        service.addProgramTrackedEntityAttributeGroup( group );

        service.deleteProgramTrackedEntityAttributeGroup( group );

        assertNull( service.getProgramTrackedEntityAttributeGroup( group.getUid() ) );
    }

    @Test
    public void testRemoveMember()
    {
        service.addProgramTrackedEntityAttributeGroup( group );

        group.addAttribute( attrA );

        manager.update( group );

        assertEquals( 1, group.getAttributes().size() );

        group.removeAttribute( attrA );

        manager.update( group );

        assertEquals( 0, group.getAttributes().size() );
    }
}
