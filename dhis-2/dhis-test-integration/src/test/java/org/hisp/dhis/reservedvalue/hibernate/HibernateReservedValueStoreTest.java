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
package org.hisp.dhis.reservedvalue.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.common.Objects;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.dhis.reservedvalue.ReservedValueStore;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

class HibernateReservedValueStoreTest extends SingleSetupIntegrationTestBase
{

    private static int counter = 1;

    private final static String teaUid = "tea";

    private final static String prog001 = "001";

    private final static String prog002 = "002";

    private Date futureDate;

    private static final String key = "RANDOM(###)";

    private final ReservedValue.ReservedValueBuilder reservedValue = ReservedValue.builder()
        .ownerObject( Objects.TRACKEDENTITYATTRIBUTE.name() ).created( new Date() ).ownerUid( teaUid ).key( key )
        .expiryDate( futureDate );

    @Autowired
    private ReservedValueStore reservedValueStore;

    @Autowired
    private OrganisationUnitStore organisationUnitStore;

    @Autowired
    private TrackedEntityInstanceStore trackedEntityInstanceStore;

    @Autowired
    private TrackedEntityAttributeStore trackedEntityAttributeStore;

    @Autowired
    private TrackedEntityAttributeValueStore trackedEntityAttributeValueStore;

    @Override
    protected void setUpTest()
    {
        Calendar future = Calendar.getInstance();
        future.add( Calendar.DATE, 10 );
        futureDate = future.getTime();
        reservedValue.expiryDate( futureDate );
    }

    @Test
    void reserveValuesSingleValue()
    {
        reservedValueStore.save( reservedValue.value( prog001 ).build() );
        int count = reservedValueStore.getCount();
        ReservedValue rv = reservedValue.value( prog002 ).build();
        List<ReservedValue> res = reservedValueStore.reserveValuesJpa( rv, Lists.newArrayList( rv.getValue() ) );
        assertEquals( 1, res.size() );
        assertEquals( reservedValueStore.getCount(), count + 1 );
    }

    @Test
    void isReservedShouldBeTrue()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        assertTrue( reservedValueStore.isReserved( rv.getOwnerObject(), rv.getOwnerUid(), prog001 ) );
    }

    @Test
    void isReservedShouldBeFalse()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        assertFalse( reservedValueStore.isReserved( rv.getOwnerObject(), rv.getOwnerUid(), "100" ) );
    }

    @Test
    void reserveValuesMultipleValues()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        int count = reservedValueStore.getCount();
        ArrayList<String> values = new ArrayList<>();
        int n = 10;
        for ( int i = 0; i < n; i++ )
        {
            values.add( String.format( "%03d", counter++ ) );
        }
        List<ReservedValue> res = reservedValueStore.reserveValuesJpa( getFreeReservedValue(), values );
        assertEquals( n, res.size() );
        assertEquals( (count + n), reservedValueStore.getCount() );
    }

    @Test
    void reserveValuesMultipleValuesAlreadyReservedAndUsed()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        int count = reservedValueStore.getCount();
        List<ReservedValue> res = reservedValueStore.reserveValuesJpa( rv, Lists.newArrayList( "002", "003", "004" ) );
        assertEquals( 1, count );
        assertEquals( 3, res.size() );
        assertEquals( (count + 3), reservedValueStore.getCount() );
    }

    @Test
    void getIfReservedValuesReturnsReservedValue()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        List<ReservedValue> res = reservedValueStore.getAvailableValues( rv, Lists.newArrayList( rv.getValue() ),
            rv.getOwnerObject() );
        assertEquals( rv, res.get( 0 ) );
        assertEquals( 1, res.size() );
    }

    @Test
    void getAvailableValuesWhenNotReserved()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        assertEquals( 1, reservedValueStore.getAll().size() );
        List<ReservedValue> res = reservedValueStore.getAvailableValues( rv, Lists.newArrayList( prog001, prog002 ),
            rv.getOwnerObject() );
        assertEquals( 1, res.size() );
        assertTrue( res.stream().anyMatch( r -> r.getValue().equals( prog002 ) ) );
    }

    @Test
    void getAvailableValuesWhenAlreadyUsed()
        throws TextPatternParser.TextPatternParsingException,
        IllegalAccessException
    {
        OrganisationUnit ou = createOrganisationUnit( "OU" );
        organisationUnitStore.save( ou );
        TrackedEntity tei = createTrackedEntityInstance( ou );
        trackedEntityInstanceStore.save( tei );
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'Y' );
        TextPattern textPattern = TextPatternParser.parse( key );
        textPattern.setOwnerObject( Objects.fromClass( tea.getClass() ) );
        textPattern.setOwnerUid( tea.getUid() );
        tea.setTextPattern( textPattern );
        tea.setUid( teaUid );
        trackedEntityAttributeStore.save( tea );
        TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue( 'Z', tei, tea );
        teav.setValue( prog001 );
        trackedEntityAttributeValueStore.save( teav );
        ReservedValue rv = reservedValue.value( prog001 ).build();
        rv.setTrackedEntityAttributeId( teav.getAttribute().getId() );
        assertEquals( 1, trackedEntityAttributeValueStore.getAll().size() );
        assertEquals( 0, reservedValueStore.getAll().size() );
        List<ReservedValue> res = reservedValueStore.getAvailableValues( rv, Lists.newArrayList( prog001, prog002 ),
            rv.getOwnerObject() );
        assertFalse( res.stream().anyMatch( r -> r.getValue().equals( prog001 ) ) );
        assertTrue( res.stream().anyMatch( r -> r.getValue().equals( prog002 ) ) );
        assertEquals( 1, res.size() );
    }

    @Test
    void removeExpiredReservations()
    {
        Calendar pastDate = Calendar.getInstance();
        pastDate.add( Calendar.DATE, -1 );
        reservedValue.expiryDate( pastDate.getTime() );
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.reserveValuesJpa( rv, Lists.newArrayList( rv.getValue() ) );
        assertTrue( reservedValueStore.isReserved( Objects.TRACKEDENTITYATTRIBUTE.name(), teaUid, prog001 ) );
        reservedValueStore.removeUsedOrExpiredReservations();
        assertFalse( reservedValueStore.getAll().contains( rv ) );
    }

    @Test
    void removeExpiredReservationsDoesNotRemoveAnythingIfNothingHasExpired()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        int num = reservedValueStore.getCount();
        reservedValueStore.removeUsedOrExpiredReservations();
        assertEquals( num, reservedValueStore.getCount() );
    }

    @Test
    void shouldNotAddAlreadyReservedValues()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        OrganisationUnit ou = createOrganisationUnit( "OU" );
        organisationUnitStore.save( ou );
        TrackedEntity tei = createTrackedEntityInstance( ou );
        trackedEntityInstanceStore.save( tei );
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'Y' );
        tea.setUid( teaUid );
        trackedEntityAttributeStore.save( tea );
        TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue( 'Z', tei, tea );
        teav.setValue( prog001 );
        trackedEntityAttributeValueStore.save( teav );
        assertEquals( 1, reservedValueStore.getCount() );
    }

    @Test
    void shouldRemoveAlreadyUsedReservedValues()
    {
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        OrganisationUnit ou = createOrganisationUnit( "OU" );
        organisationUnitStore.save( ou );
        TrackedEntity tei = createTrackedEntityInstance( ou );
        trackedEntityInstanceStore.save( tei );
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'Y' );
        tea.setUid( teaUid );
        trackedEntityAttributeStore.save( tea );
        TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue( 'Z', tei, tea );
        teav.setValue( prog001 );
        trackedEntityAttributeValueStore.save( teav );
        reservedValueStore.removeUsedOrExpiredReservations();
        assertFalse( reservedValueStore.isReserved( Objects.TRACKEDENTITYATTRIBUTE.name(), teaUid, prog001 ) );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    void shouldRemoveAlreadyUsedOrExpiredReservedValues()
    {
        // expired value
        Calendar pastDate = Calendar.getInstance();
        pastDate.add( Calendar.DATE, -1 );
        reservedValueStore.reserveValuesJpa( reservedValue.expiryDate( pastDate.getTime() ).value( prog002 ).build(),
            Lists.newArrayList( prog002 ) );
        // used value
        OrganisationUnit ou = createOrganisationUnit( "OU" );
        organisationUnitStore.save( ou );
        TrackedEntity tei = createTrackedEntityInstance( ou );
        trackedEntityInstanceStore.save( tei );
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'Y' );
        tea.setUid( teaUid );
        trackedEntityAttributeStore.save( tea );
        TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue( 'Z', tei, tea );
        teav.setValue( prog001 );
        trackedEntityAttributeValueStore.save( teav );
        ReservedValue rv = reservedValue.value( prog001 ).build();
        reservedValueStore.save( rv );
        reservedValueStore.removeUsedOrExpiredReservations();
        assertFalse( reservedValueStore.isReserved( Objects.TRACKEDENTITYATTRIBUTE.name(), teaUid, prog001 ) );
        assertFalse( reservedValueStore.isReserved( Objects.TRACKEDENTITYATTRIBUTE.name(), teaUid, prog002 ) );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    private ReservedValue getFreeReservedValue()
    {
        return ReservedValue.builder().ownerObject( Objects.TRACKEDENTITYATTRIBUTE.name() ).created( new Date() )
            .ownerUid( "FREE" ).key( "00X" ).value( String.format( "%03d", counter++ ) ).expiryDate( futureDate )
            .build();
    }
}
