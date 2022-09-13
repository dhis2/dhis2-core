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
package org.hisp.dhis.organisationunit;

import static java.util.Arrays.asList;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class OrganisationUnitStoreTest extends OrganisationUnitBaseSpringTest
{

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------
    @Test
    void testGetOrganisationUnitsWithoutGroups()
    {
        // 1
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        // 2
        OrganisationUnit ouB = addOrganisationUnit( 'B', ouA );
        // 2
        OrganisationUnit ouC = addOrganisationUnit( 'C', ouA );
        // 3
        OrganisationUnit ouD = addOrganisationUnit( 'D', ouB );
        // 3
        OrganisationUnit ouE = addOrganisationUnit( 'E', ouB );
        addOrganisationUnitGroup( 'A', ouA );
        addOrganisationUnitGroup( 'B', ouB );
        assertContainsOnly( List.of( ouC, ouD, ouE ), unitStore.getOrganisationUnitsWithoutGroups() );
    }

    @Test
    void testGetOrganisationUnitHierarchyMemberCount()
    {
        // 1
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        // 2
        OrganisationUnit ouB = addOrganisationUnit( 'B', ouA );
        // 2
        OrganisationUnit ouC = addOrganisationUnit( 'C', ouA );
        // 3
        OrganisationUnit ouD = addOrganisationUnit( 'D', ouB );
        // 3
        OrganisationUnit ouE = addOrganisationUnit( 'E', ouB );
        // 3
        OrganisationUnit ouF = addOrganisationUnit( 'F', ouC );
        // 3
        OrganisationUnit ouG = addOrganisationUnit( 'G', ouC );
        DataSet dsA = addDataSet( 'A', ouD, ouE, ouG );
        DataSet dsB = addDataSet( 'B', ouD );
        assertEquals( 3L, unitStore.getOrganisationUnitHierarchyMemberCount( ouA, dsA, "dataSets" ).longValue() );
        assertEquals( 2L, unitStore.getOrganisationUnitHierarchyMemberCount( ouB, dsA, "dataSets" ).longValue() );
        assertEquals( 1L, unitStore.getOrganisationUnitHierarchyMemberCount( ouA, dsB, "dataSets" ).longValue() );
    }

    @Test
    void testGetOrganisationUnitsWithProgram()
    {
        // 1
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        // 2
        OrganisationUnit ouB = addOrganisationUnit( 'B', ouA );
        // 2
        OrganisationUnit ouC = addOrganisationUnit( 'C', ouA );
        // 3
        OrganisationUnit ouD = addOrganisationUnit( 'D', ouB );
        // 3
        OrganisationUnit ouE = addOrganisationUnit( 'E', ouB );
        // 3
        OrganisationUnit ouF = addOrganisationUnit( 'F', ouC );
        // 3
        OrganisationUnit ouG = addOrganisationUnit( 'G', ouC );
        Program prA = addProgram( 'A', ouA, ouB, ouC, ouE );
        Program prB = addProgram( 'B', ouA, ouD, ouE );
        assertContainsOnly( List.of( ouA, ouB, ouC, ouE ), unitStore.getOrganisationUnitsWithProgram( prA ) );
        assertContainsOnly( List.of( ouA, ouD, ouE ), unitStore.getOrganisationUnitsWithProgram( prB ) );
    }

    @Test
    void testGetOrganisationUnits()
    {
        // 1
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        // 2
        OrganisationUnit ouB = addOrganisationUnit( 'B', ouA );
        // 2
        OrganisationUnit ouC = addOrganisationUnit( 'C', ouA );
        // 3
        OrganisationUnit ouD = addOrganisationUnit( 'D', ouB );
        // 3
        OrganisationUnit ouE = addOrganisationUnit( 'E', ouB );
        // 3
        OrganisationUnit ouF = addOrganisationUnit( 'F', ouC );
        // 3
        OrganisationUnit ouG = addOrganisationUnit( 'G', ouC );
        OrganisationUnitGroup ougA = addOrganisationUnitGroup( 'A', ouD, ouF );
        OrganisationUnitGroup ougB = addOrganisationUnitGroup( 'B', ouE, ouG );
        // Query
        assertContainsOnly( List.of( ouC ),
            unitStore.getOrganisationUnits( createParams( OrganisationUnitQueryParams::setQuery, "UnitC" ) ) );
        // Query
        assertContainsOnly( List.of( ouA ), unitStore.getOrganisationUnits(
            createParams( OrganisationUnitQueryParams::setQuery, "OrganisationUnitCodeA" ) ) );
        // Parents
        assertContainsOnly( List.of( ouC, ouF, ouG ), unitStore.getOrganisationUnits(
            createParams( OrganisationUnitQueryParams::setParents, Sets.newHashSet( ouC, ouF ) ) ) );
        // Groups
        assertContainsOnly( List.of( ouD, ouF ), unitStore.getOrganisationUnits(
            createParams( OrganisationUnitQueryParams::setGroups, Sets.newHashSet( ougA ) ) ) );
        // Groups
        assertContainsOnly( List.of( ouD, ouF, ouE, ouG ), unitStore.getOrganisationUnits(
            createParams( OrganisationUnitQueryParams::setGroups, Sets.newHashSet( ougA, ougB ) ) ) );
        // Levels
        assertContainsOnly( List.of( ouB, ouC ), unitStore.getOrganisationUnits(
            createParams( OrganisationUnitQueryParams::setLevels, Sets.newHashSet( 2 ) ) ) );
        // Levels
        assertContainsOnly( List.of( ouB, ouC, ouD, ouE, ouF, ouG ), unitStore.getOrganisationUnits(
            createParams( OrganisationUnitQueryParams::setLevels, Sets.newHashSet( 2, 3 ) ) ) );
        // Levels and groups
        assertContainsOnly( List.of( ouD, ouF ), unitStore.getOrganisationUnits( createParams( params -> {
            params.setLevels( Sets.newHashSet( 3 ) );
            params.setGroups( Sets.newHashSet( ougA ) );
        } ) ) );
        // Parents and groups
        assertContainsOnly( List.of( ouG ), unitStore.getOrganisationUnits( createParams( params -> {
            params.setParents( Sets.newHashSet( ouC ) );
            params.setGroups( Sets.newHashSet( ougB ) );
        } ) ) );
        // Parents and max levels
        assertContainsOnly( List.of( ouA, ouB, ouC ), unitStore.getOrganisationUnits( createParams( params -> {
            params.setParents( Sets.newHashSet( ouA ) );
            params.setMaxLevels( 2 );
        } ) ) );
        // Parents and max levels
        assertContainsOnly( List.of( ouA, ouB, ouC, ouD, ouE, ouF, ouG ),
            unitStore.getOrganisationUnits( createParams( params -> {
                params.setParents( Sets.newHashSet( ouA ) );
                params.setMaxLevels( 3 );
            } ) ) );
        // Parents and max levels
        assertContainsOnly( List.of( ouA ), unitStore.getOrganisationUnits( createParams( params -> {
            params.setParents( Sets.newHashSet( ouA ) );
            params.setMaxLevels( 1 );
        } ) ) );
        // Parents and max levels
        assertContainsOnly( List.of( ouB, ouD, ouE ), unitStore.getOrganisationUnits( createParams( params -> {
            params.setParents( Sets.newHashSet( ouB ) );
            params.setMaxLevels( 3 );
        } ) ) );
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitLevel
    // -------------------------------------------------------------------------
    @Test
    void testAddGetOrganisationUnitLevel()
    {
        OrganisationUnitLevel levelA = addOrganisationUnitLevel( 1, "National" );
        OrganisationUnitLevel levelB = addOrganisationUnitLevel( 2, "District" );
        assertEquals( levelA, levelStore.get( levelA.getId() ) );
        assertEquals( levelB, levelStore.get( levelB.getId() ) );
    }

    @Test
    void testGetOrganisationUnitLevels()
    {
        OrganisationUnitLevel levelA = addOrganisationUnitLevel( 1, "National" );
        OrganisationUnitLevel levelB = addOrganisationUnitLevel( 2, "District" );
        assertContainsOnly( List.of( levelA, levelB ), levelStore.getAll() );
    }

    @Test
    void testRemoveOrganisationUnitLevel()
    {
        OrganisationUnitLevel levelA = addOrganisationUnitLevel( 1, "National" );
        OrganisationUnitLevel levelB = addOrganisationUnitLevel( 2, "District" );
        assertNotNull( levelStore.get( levelA.getId() ) );
        assertNotNull( levelStore.get( levelB.getId() ) );
        levelStore.delete( levelA );
        assertNull( levelStore.get( levelA.getId() ) );
        assertNotNull( levelStore.get( levelB.getId() ) );
        levelStore.delete( levelB );
        assertNull( levelStore.get( levelA.getId() ) );
        assertNull( levelStore.get( levelB.getId() ) );
    }

    @Test
    void testGetMaxLevel()
    {
        assertEquals( 0, unitStore.getMaxLevel() );
        // 1
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        assertEquals( 1, unitStore.getMaxLevel() );
        // 2
        OrganisationUnit ouB = addOrganisationUnit( 'B', ouA );
        // 2
        OrganisationUnit ouC = addOrganisationUnit( 'C', ouA );
        assertEquals( 2, unitStore.getMaxLevel() );
        // 3
        addOrganisationUnit( 'D', ouB );
        // 3
        addOrganisationUnit( 'E', ouB );
        // 3
        addOrganisationUnit( 'F', ouC );
        // 3
        addOrganisationUnit( 'G', ouC );
        assertEquals( 3, unitStore.getMaxLevel() );
    }

    @Test
    void testGetOrganisationUnitsWithCyclicReferences_DirectCycle()
    {
        // 1
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        // 2
        OrganisationUnit ouB = createOrganisationUnit( 'B', ouA );
        ouA.setParent( ouB );
        ouB.getChildren().add( ouA );
        unitStore.save( ouB );
        assertContainsOnly( Set.of( ouA, ouB ), unitStore.getOrganisationUnitsWithCyclicReferences() );
    }

    @Test
    void testGetOrganisationUnitsWithCyclicReferences_DistantCycle()
    {
        // 1
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        // 2
        OrganisationUnit ouB = addOrganisationUnit( 'B', ouA );
        // 3
        OrganisationUnit ouC = addOrganisationUnit( 'C', ouB );
        // 4
        OrganisationUnit ouD = createOrganisationUnit( 'D', ouC );
        ouA.setParent( ouD );
        ouD.getChildren().add( ouA );
        unitStore.save( ouD );
        assertContainsOnly( Set.of( ouA, ouB, ouC, ouD ), unitStore.getOrganisationUnitsWithCyclicReferences() );
    }

    @Test
    void testGetOrphanedOrganisationUnits1()
    {
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        OrganisationUnit ouB = addOrganisationUnit( 'B' );
        assertContainsOnly( List.of( ouA, ouB ), unitStore.getOrphanedOrganisationUnits() );
    }

    @Test
    void testGetOrphanedOrganisationUnits2()
    {
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        OrganisationUnit ouB = addOrganisationUnit( 'B' );
        OrganisationUnit ouC = addOrganisationUnit( 'C', ouA );
        assertContainsOnly( List.of( ouB ), unitStore.getOrphanedOrganisationUnits() );
    }

    @Test
    void testGetOrganisationUnitsViolatingExclusiveGroupSets()
    {
        OrganisationUnit ouA = addOrganisationUnit( 'A' );
        OrganisationUnit ouB = addOrganisationUnit( 'B' );
        OrganisationUnit ouC = addOrganisationUnit( 'C' );
        OrganisationUnit ouD = addOrganisationUnit( 'D' );
        OrganisationUnitGroup groupA = addOrganisationUnitGroup( 'A', ouA );
        OrganisationUnitGroup groupB = addOrganisationUnitGroup( 'B', ouB );
        OrganisationUnitGroup groupC = addOrganisationUnitGroup( 'C', ouC );
        OrganisationUnitGroup groupD = addOrganisationUnitGroup( 'D', ouD );
        OrganisationUnitGroup groupE = addOrganisationUnitGroup( 'E', ouD );
        addOrganisationUnitGroupSet( 'K', groupA );
        addOrganisationUnitGroupSet( 'X', groupC, groupD, groupE );
        // unit D is in group D and E which are both in set X
        assertContainsOnly( List.of( ouD ), unitStore.getOrganisationUnitsViolatingExclusiveGroupSets() );
    }

    private Program addProgram( char uniqueCharacter, OrganisationUnit... units )
    {
        Program program = createProgram( uniqueCharacter );
        for ( OrganisationUnit unit : units )
        {
            unit.getPrograms().add( program );
            program.getOrganisationUnits().add( unit );
        }
        idObjectManager.save( program );
        return program;
    }

    private DataSet addDataSet( char uniqueCharacter, OrganisationUnit... units )
    {
        DataSet dataSet = createDataSet( uniqueCharacter );
        asList( units ).forEach( dataSet::addOrganisationUnit );
        dataSetService.addDataSet( dataSet );
        return dataSet;
    }

    private static <T> OrganisationUnitQueryParams createParams( BiConsumer<OrganisationUnitQueryParams, T> setter,
        T value )
    {
        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        setter.accept( params, value );
        return params;
    }

    private static OrganisationUnitQueryParams createParams( Consumer<OrganisationUnitQueryParams> init )
    {
        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        init.accept( params );
        return params;
    }
}
