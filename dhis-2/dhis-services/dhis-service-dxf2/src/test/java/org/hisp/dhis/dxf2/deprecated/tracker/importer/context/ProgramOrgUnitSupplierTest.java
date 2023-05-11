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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class ProgramOrgUnitSupplierTest extends AbstractSupplierTest<Long>
{
    private ProgramOrgUnitSupplier subject;

    @BeforeEach
    void setUp()
    {
        this.subject = new ProgramOrgUnitSupplier( jdbcTemplate );
    }

    @Test
    void verifySupplier()
        throws SQLException
    {
        // Org Unit //
        OrganisationUnit ou1 = new OrganisationUnit();
        ou1.setId( 1 );
        ou1.setUid( "abcded" );
        OrganisationUnit ou2 = new OrganisationUnit();
        ou2.setId( 2 );
        ou2.setUid( "fgfgfg" );
        // create 2 events to import - each one pointing to a different org unit
        Event event = new Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setOrgUnit( "abcded" );
        Event event2 = new Event();
        event2.setUid( CodeGenerator.generateUid() );
        event2.setOrgUnit( "fgfgfg" );
        Map<String, OrganisationUnit> organisationUnitMap = new HashMap<>();
        organisationUnitMap.put( event.getOrgUnit(), ou1 );
        organisationUnitMap.put( event.getOrgUnit(), ou2 );
        when( mockResultSet.next() ).thenReturn( true ).thenReturn( true ).thenReturn( false );
        when( mockResultSet.getLong( "programid" ) ).thenReturn( 100L );
        when( mockResultSet.getLong( "organisationunitid" ) ).thenReturn( 1L, 2L );
        // mock result-set extraction
        mockResultSetExtractor( mockResultSet );
        final Map<Long, List<Long>> longListMap = subject.get( ImportOptions.getDefaultImportOptions(),
            List.of( event, event2 ), organisationUnitMap );
        assertThat( longListMap.keySet(), hasSize( 1 ) );
        assertThat( longListMap.get( 100L ), hasSize( 2 ) );
        assertThat( longListMap.get( 100L ), containsInAnyOrder( 1L, 2L ) );
    }
}
