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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

/**
 * @author Jan Bernitt
 */
class OrganisationUnitGroupStoreTest extends OrganisationUnitBaseSpringTest
{

    @Test
    void testGetOrganisationUnitGroupsWithoutGroupSets()
    {
        OrganisationUnit someUnit = addOrganisationUnit( 'A' );
        OrganisationUnitGroup noSet = addOrganisationUnitGroup( 'X', someUnit );
        OrganisationUnitGroup withSet = addOrganisationUnitGroup( 'W', someUnit );
        addOrganisationUnitGroupSet( 'S', withSet );
        assertContainsOnly( List.of( noSet ), groupStore.getOrganisationUnitGroupsWithoutGroupSets() );
    }

    @Test
    void testGetOrganisationUnitGroupsWithGroupSets()
    {
        OrganisationUnit someUnit = addOrganisationUnit( 'A' );
        addOrganisationUnitGroup( 'X', someUnit );
        OrganisationUnitGroup withSet = addOrganisationUnitGroup( 'W', someUnit );
        addOrganisationUnitGroupSet( 'S', withSet );
        assertContainsOnly( List.of( withSet ), groupStore.getOrganisationUnitGroupsWithGroupSets() );
    }

    @Test
    void testGetOrgUnitGroupInGroupSet()
    {
        OrganisationUnit organisationUnitA = addOrganisationUnit( 'A' );
        OrganisationUnit organisationUnitB = addOrganisationUnit( 'B' );
        OrganisationUnit organisationUnitC = addOrganisationUnit( 'C' );
        OrganisationUnitGroup groupA = addOrganisationUnitGroup( 'A', organisationUnitA );
        OrganisationUnitGroup groupB = addOrganisationUnitGroup( 'B', organisationUnitB );
        OrganisationUnitGroup groupC = addOrganisationUnitGroup( 'C', organisationUnitC );
        Set<OrganisationUnitGroup> groups = Sets.newHashSet( groupA, groupB );
        OrganisationUnitGroupSet groupSet = addOrganisationUnitGroupSet( 'A', groupA, groupC );
        assertEquals( groupA, groupStore.getOrgUnitGroupInGroupSet( groups, groupSet ) );
    }
}
