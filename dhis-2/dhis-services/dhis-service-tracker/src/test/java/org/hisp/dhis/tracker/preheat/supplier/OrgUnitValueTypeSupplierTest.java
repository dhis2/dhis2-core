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
package org.hisp.dhis.tracker.preheat.supplier;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class OrgUnitValueTypeSupplierTest extends DhisConvenienceTest
{

    private OrgUnitValueTypeSupplier supplier;

    private TrackerPreheat preheat;

    @Mock
    private IdentifiableObjectManager manager;

    @BeforeEach
    public void setUp()
    {
        preheat = new TrackerPreheat();
        supplier = new OrgUnitValueTypeSupplier( manager );
    }

    @Test
    void testSupplierAddsOrgUnitReferencedByTEIAttributes()
    {
        preheat.put( TrackerIdSchemeParam.UID,
            List.of( teaNumeric( "numeric" ), teaOrgUnit( "hQKI6KcEu5t" ) ) );

        OrganisationUnit orgUnit = orgUnit( "kKacJUdANDC" );
        when( manager.getByUid( OrganisationUnit.class, List.of( "kKacJUdANDC" ) ) )
            .thenReturn( List.of( orgUnit ) );

        TrackerImportParams params = params( TrackerIdSchemeParams.builder().build() )
            .trackedEntities(
                List.of( trackedEntity( numericAttribute(), orgUnitAttribute( "hQKI6KcEu5t", "kKacJUdANDC" ) ) ) )
            .build();

        supplier.preheatAdd( params, preheat );

        assertContainsOnly( List.of( orgUnit ), preheat.getAll( OrganisationUnit.class ) );
    }

    @Test
    void testSupplierDoesNotAddOrgUnitIfTEIAttributeValueIsEmpty()
    {
        preheat.put( TrackerIdSchemeParam.UID, List.of( teaOrgUnit( "hQKI6KcEu5t" ) ) );

        TrackerImportParams params = params( TrackerIdSchemeParams.builder().build() )
            .trackedEntities( List.of( trackedEntity( orgUnitAttribute( "hQKI6KcEu5t", "" ) ) ) )
            .build();

        supplier.preheatAdd( params, preheat );

        assertEquals( Collections.emptyList(), preheat.getAll( OrganisationUnit.class ) );
    }

    @Test
    void testSupplierAddsOrgUnitReferencedByEnrollmentAttributes()
    {
        preheat.put( TrackerIdSchemeParam.UID,
            List.of( teaNumeric( "numeric" ), teaOrgUnit( "hQKI6KcEu5t" ) ) );

        OrganisationUnit orgUnit = orgUnit( "kKacJUdANDC" );
        when( manager.getByUid( OrganisationUnit.class, List.of( "kKacJUdANDC" ) ) )
            .thenReturn( List.of( orgUnit ) );

        TrackerImportParams params = params( TrackerIdSchemeParams.builder().build() )
            .enrollments(
                List.of( enrollment( numericAttribute(), orgUnitAttribute( "hQKI6KcEu5t", "kKacJUdANDC" ) ) ) )
            .build();

        supplier.preheatAdd( params, preheat );

        assertContainsOnly( List.of( orgUnit ), preheat.getAll( OrganisationUnit.class ) );
    }

    @Test
    void testSupplierAddsOrgUnitReferencedByEventDataElement()
    {
        preheat.put( TrackerIdSchemeParam.UID,
            List.of( numericDataElement( "numeric" ), orgUnitDataElement( "hQKI6KcEu5t" ) ) );

        OrganisationUnit orgUnit = orgUnit( "kKacJUdANDC" );
        when( manager.getByUid( OrganisationUnit.class, List.of( "kKacJUdANDC" ) ) )
            .thenReturn( List.of( orgUnit ) );

        TrackerImportParams params = params( TrackerIdSchemeParams.builder().build() )
            .events( List.of( event( dataValue( "numeric", "2" ), dataValue( "hQKI6KcEu5t", "kKacJUdANDC" ) ) ) )
            .build();

        supplier.preheatAdd( params, preheat );

        assertContainsOnly( List.of( orgUnit ), preheat.getAll( OrganisationUnit.class ) );
    }

    @Test
    void testSupplierDoesNotAddOrgUnitIfEventDataValueValueIsEmpty()
    {
        preheat.put( TrackerIdSchemeParam.UID, List.of( orgUnitDataElement( "hQKI6KcEu5t" ) ) );

        TrackerImportParams params = params( TrackerIdSchemeParams.builder().build() )
            .events( List.of( event( dataValue( "hQKI6KcEu5t", "" ) ) ) )
            .build();

        supplier.preheatAdd( params, preheat );

        assertEquals( Collections.emptyList(), preheat.getAll( OrganisationUnit.class ) );
    }

    private TrackedEntityAttribute teaNumeric( String uid )
    {
        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        attribute.setUid( uid );
        attribute.setValueType( ValueType.NUMBER );
        return attribute;
    }

    private TrackedEntityAttribute teaOrgUnit( String uid )
    {
        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        attribute.setUid( uid );
        attribute.setValueType( ValueType.ORGANISATION_UNIT );
        return attribute;
    }

    private OrganisationUnit orgUnit( String uid )
    {
        OrganisationUnit orgUnit = createOrganisationUnit( 'A' );
        orgUnit.setUid( uid );
        return orgUnit;
    }

    private TrackerImportParams.TrackerImportParamsBuilder params( TrackerIdSchemeParams idSchemes )
    {
        return TrackerImportParams.builder().idSchemes( idSchemes );
    }

    private Attribute numericAttribute()
    {
        return Attribute.builder()
            .attribute( MetadataIdentifier.ofUid( "numeric" ) )
            .valueType( ValueType.NUMBER )
            .build();
    }

    private Attribute orgUnitAttribute( String uid, String value )
    {
        return Attribute.builder()
            .attribute( MetadataIdentifier.ofUid( uid ) )
            .valueType( ValueType.ORGANISATION_UNIT )
            .value( value )
            .build();
    }

    private TrackedEntity trackedEntity( Attribute... attributes )
    {
        return TrackedEntity.builder()
            .attributes( attributes( attributes ) )
            .build();
    }

    private Enrollment enrollment( Attribute... attributes )
    {
        return Enrollment.builder()
            .attributes( attributes( attributes ) )
            .build();
    }

    private List<Attribute> attributes( Attribute[] attributes )
    {
        List<Attribute> attrs = new ArrayList<>();
        for ( Attribute at : attributes )
        {
            attrs.add( at );
        }
        return attrs;
    }

    private DataElement numericDataElement( String uid )
    {
        DataElement element = createDataElement( 'A' );
        element.setUid( uid );
        element.setValueType( ValueType.NUMBER );
        return element;
    }

    private DataElement orgUnitDataElement( String uid )
    {
        DataElement element = createDataElement( 'A' );
        element.setUid( uid );
        element.setValueType( ValueType.ORGANISATION_UNIT );
        return element;
    }

    private Event event( DataValue... dataValues )
    {
        return Event.builder()
            .dataValues( dataValues( dataValues ) )
            .build();
    }

    private Set<DataValue> dataValues( DataValue[] dataValues )
    {
        Set<DataValue> dvs = new HashSet<>();
        for ( DataValue dv : dataValues )
        {
            dvs.add( dv );
        }
        return dvs;
    }

    private DataValue dataValue( String uid, String value )
    {
        return DataValue.builder()
            .dataElement( MetadataIdentifier.ofUid( uid ) )
            .value( value )
            .build();
    }
}