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
package org.hisp.dhis.tracker.imports.preheat.mappers;

import static org.hisp.dhis.tracker.imports.preheat.mappers.AttributeCreator.attributeValue;
import static org.hisp.dhis.tracker.imports.preheat.mappers.AttributeCreator.attributeValues;
import static org.hisp.dhis.tracker.imports.preheat.mappers.AttributeCreator.setIdSchemeFields;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.Test;

class OrganisationUnitMapperTest
{
    @Test
    void testIdSchemeRelatedFieldsAreMapped()
    {

        OrganisationUnit orgUnit = setIdSchemeFields(
            new OrganisationUnit(),
            "HpSAvRWtdDR",
            "meet",
            "green",
            attributeValues( "m0GpPuMUfFW", "purple" ) );

        OrganisationUnit mapped = OrganisationUnitMapper.INSTANCE.map( orgUnit );

        assertEquals( "HpSAvRWtdDR", mapped.getUid() );
        assertEquals( "meet", mapped.getName() );
        assertEquals( "green", mapped.getCode() );
        assertContainsOnly( Set.of( attributeValue( "m0GpPuMUfFW", "purple" ) ), mapped.getAttributeValues() );
    }

    @Test
    void testParentFieldsAreMapped()
    {
        OrganisationUnit rootOrgUnit = new OrganisationUnit();
        rootOrgUnit.setUid( "root" );
        OrganisationUnit level1OrgUnit = new OrganisationUnit();
        level1OrgUnit.setUid( "level1" );
        OrganisationUnit level2OrgUnit = new OrganisationUnit();
        level2OrgUnit.setUid( "level2" );

        level2OrgUnit.setParent( level1OrgUnit );
        level1OrgUnit.setParent( rootOrgUnit );
        rootOrgUnit.setParent( null );

        OrganisationUnit mapped = OrganisationUnitMapper.INSTANCE.map( level2OrgUnit );

        assertEquals( "level2", mapped.getUid() );
        assertNotNull( mapped.getParent() );
        assertEquals( "level1", mapped.getParent().getUid() );
        assertNotNull( mapped.getParent().getParent() );
        assertEquals( "root", mapped.getParent().getParent().getUid() );
        assertNull( mapped.getParent().getParent().getParent() );
    }
}