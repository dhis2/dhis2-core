package org.hisp.dhis.schema.patch;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 *
 */

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PatchServiceTest
    extends DhisSpringTest
{
    @Autowired
    private PatchService patchService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    public void testUpdateName()
    {
        DataElement dataElement = createDataElement( 'A' );

        Patch patch = new Patch()
            .addChange( new Change( "name", ChangeOperation.ADDITION, "Updated Name" ) );

        patchService.apply( patch, dataElement );

        assertEquals( "Updated Name", dataElement.getName() );
    }

    @Test
    public void testAddDataElementToGroup()
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        manager.save( deA );
        manager.save( deB );

        assertTrue( dataElementGroup.getMembers().isEmpty() );

        Patch patch = new Patch()
            .addChange( new Change( "name", ChangeOperation.ADDITION, "Updated Name" ) )
            .addChange( new Change( "dataElements", ChangeOperation.ADDITION, deA.getUid() ) )
            .addChange( new Change( "dataElements", ChangeOperation.ADDITION, deB.getUid() ) );

        patchService.apply( patch, dataElementGroup );

        assertEquals( "Updated Name", dataElementGroup.getName() );
        assertEquals( 2, dataElementGroup.getMembers().size() );
    }

    @Test
    public void testDeleteDataElementFromGroup()
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        manager.save( deA );
        manager.save( deB );

        dataElementGroup.addDataElement( deA );
        dataElementGroup.addDataElement( deB );

        assertEquals( 2, dataElementGroup.getMembers().size() );

        Patch patch = new Patch()
            .addChange( new Change( "name", ChangeOperation.ADDITION, "Updated Name" ) )
            .addChange( new Change( "dataElements", ChangeOperation.DELETION, deA.getUid() ) );

        patchService.apply( patch, dataElementGroup );

        assertEquals( "Updated Name", dataElementGroup.getName() );
        assertEquals( 1, dataElementGroup.getMembers().size() );

        patch = new Patch()
            .addChange( new Change( "dataElements", ChangeOperation.DELETION, deB.getUid() ) );

        patchService.apply( patch, dataElementGroup );

        assertTrue( dataElementGroup.getMembers().isEmpty() );
    }

    @Test
    public void testAddAggLevelsToDataElement()
    {
        DataElement dataElement = createDataElement( 'A' );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );

        Patch patch = new Patch()
            .addChange( new Change( "name", ChangeOperation.ADDITION, "Updated Name" ) )
            .addChange( new Change( "aggregationLevels", ChangeOperation.ADDITION, 1 ) )
            .addChange( new Change( "aggregationLevels", ChangeOperation.ADDITION, 2 ) );

        patchService.apply( patch, dataElement );

        assertEquals( 2, dataElement.getAggregationLevels().size() );
    }

    @Test
    public void testAddStringAggLevelsToDataElement()
    {
        DataElement dataElement = createDataElement( 'A' );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );

        Patch patch = new Patch()
            .addChange( new Change( "name", ChangeOperation.ADDITION, "Updated Name" ) )
            .addChange( new Change( "aggregationLevels", ChangeOperation.ADDITION, "abc" ) )
            .addChange( new Change( "aggregationLevels", ChangeOperation.ADDITION, "def" ) );

        patchService.apply( patch, dataElement );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );
    }
}
