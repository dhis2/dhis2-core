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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackerIdentifierCollectorTest
{

    private TrackerIdentifierCollector collector;

    @BeforeEach
    void setUp()
    {

        ProgramRuleService programRuleService = mock( ProgramRuleService.class );
        collector = new TrackerIdentifierCollector( programRuleService );
    }

    @Test
    void collectEnrollments()
    {

        OrganisationUnit orgUnit = new OrganisationUnit();
        Program p = new Program();
        Attribute att = new Attribute( CodeGenerator.generateUid() );
        p.setAttributeValues(
            Set.of( new AttributeValue( att, "sunshine" ), attributeValue( "grass" ) ) );
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( orgUnit.getUid() ) )
            .program( MetadataIdentifier.ofAttribute( att.getUid(), "sunshine" ) )
            .build();

        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( TrackerIdSchemeParams.builder()
                .orgUnitIdScheme( TrackerIdSchemeParam.UID )
                .programIdScheme( TrackerIdSchemeParam.ofAttribute( att.getUid() ) )
                .build() )
            .enrollments( Collections.singletonList( enrollment ) )
            .build();

        Map<Class<?>, Set<String>> ids = collector.collect( params );

        assertNotNull( ids );
        assertContainsOnly( ids.get( Enrollment.class ), enrollment.getUid() );
        assertContainsOnly( ids.get( Program.class ), "sunshine" );
        assertContainsOnly( ids.get( OrganisationUnit.class ), orgUnit.getUid() );
        assertContainsOnly( ids.get( TrackedEntityType.class ), "*" );
        assertContainsOnly( ids.get( RelationshipType.class ), "*" );
    }

    @Test
    void collectEnrollmentsWithOnlyEnrollmentSet()
    {

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .build();
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( TrackerIdSchemeParams.builder()
                .build() )
            .enrollments( Collections.singletonList( enrollment ) )
            .build();

        Map<Class<?>, Set<String>> ids = collector.collect( params );

        assertNotNull( ids );
        assertContainsOnly( ids.get( Enrollment.class ), enrollment.getUid() );
        assertContainsOnly( ids.get( TrackedEntityType.class ), "*" );
        assertContainsOnly( ids.get( RelationshipType.class ), "*" );
    }

    private AttributeValue attributeValue( String value )
    {
        return attributeValue( CodeGenerator.generateUid(), value );
    }

    private AttributeValue attributeValue( String uid, String value )
    {
        return new AttributeValue( new Attribute( uid ), value );
    }
}