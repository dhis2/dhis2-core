/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.*;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.dhis.reservedvalue.ReservedValueStore;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class HibernateReservedValueStoreTest
    extends DhisSpringTest
{
    private static int counter = 1;

    private final static String teaUid = "tea";

    private final static String prog001 = "001";

    private final static String prog002 = "002";

    private final ReservedValue reservedValue = ReservedValue.builder()
        .ownerObject( Objects.TRACKEDENTITYATTRIBUTE.name() ).created( new Date() ).ownerUid( teaUid ).key( "00X" )
        .value( prog001 ).build();

    private Date futureDate;

    @Autowired
    private ReservedValueStore reservedValueStore;

    @Autowired
    private OrganisationUnitStore organisationUnitStore;

    @Autowired
    private TrackedEntityInstanceStore trackedEntityInstanceStore;

    @Autowired
    private TrackedEntityAttributeStore trackedEntityAttributeStore;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Override
    protected void setUpTest()
    {
        Calendar future = Calendar.getInstance();
        future.add( Calendar.DATE, 10 );
        futureDate = future.getTime();
        reservedValue.setExpiryDate( futureDate );
    }

    @Test
    public void reserveValuesSingleValue()
    {
        reservedValueStore.save( reservedValue );

        int count = reservedValueStore.getCount();

        ReservedValue rv = reservedValue.toBuilder().value( prog002 ).build();

        List<ReservedValue> res = reservedValueStore.reserveValuesJpa( rv, Lists.newArrayList( rv.getValue() ) );

        assertEquals( 1, res.size() );
        assertEquals( reservedValueStore.getCount(), count + 1 );
    }

    @Test
    public void isReservedShouldBeTrue()
    {
        reservedValueStore.save( reservedValue );

        assertTrue(
            reservedValueStore.isReserved( reservedValue.getOwnerObject(), reservedValue.getOwnerUid(),
                prog001 ) );
    }

    @Test
    public void isReservedShouldBeFalse()
    {
        reservedValueStore.save( reservedValue );

        assertFalse(
            reservedValueStore.isReserved( reservedValue.getOwnerObject(), reservedValue.getOwnerUid(), "100" ) );
    }

    @Test
    public void reserveValuesMultipleValues()
    {
        reservedValueStore.save( reservedValue );

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
    public void reserveValuesMultipleValuesAlreadyReservedAndUsed()
    {
        reservedValueStore.save( reservedValue );

        int count = reservedValueStore.getCount();

        List<ReservedValue> res = reservedValueStore
            .reserveValuesJpa( reservedValue, Lists.newArrayList( "002", "003", "004" ) );

        assertEquals( 1, count );
        assertEquals( 3, res.size() );
        assertEquals( (count + 3), reservedValueStore.getCount() );
    }

    @Test
    public void getIfReservedValuesReturnsReservedValue()
    {
        reservedValueStore.save( reservedValue );

        List<ReservedValue> res = reservedValueStore
            .getIfReservedValues( reservedValue, Lists.newArrayList( reservedValue.getValue() ) );

        assertEquals( reservedValue, res.get( 0 ) );
        assertEquals( 1, res.size() );
    }

    @Test
    public void getIfReservedValuesReturnEmptyListWhenNotReserved()
    {
        reservedValueStore.save( reservedValue );

        List<ReservedValue> res = reservedValueStore
            .getIfReservedValues( reservedValue, Lists.newArrayList( "999" ) );

        assertEquals( 0, res.size() );
    }

    @Test
    public void getIfReservedValuesReturnOnlyReservedValuesWhenSendingMultipleValues()
    {
        reservedValueStore.save( reservedValue );

        List<ReservedValue> res = reservedValueStore
            .getIfReservedValues( reservedValue, Lists.newArrayList( prog001, prog002 ) );

        assertTrue( res.stream().anyMatch( r -> r.getValue().equals( prog001 ) ) );
        assertFalse( res.stream().anyMatch( r -> r.getValue().equals( prog002 ) ) );

        assertEquals( 1, res.size() );
    }

    @Test
    public void removeExpiredReservationsRemovesExpiredReservation()
    {
        Calendar pastDate = Calendar.getInstance();
        pastDate.add( Calendar.DATE, -1 );
        reservedValue.setExpiryDate( pastDate.getTime() );

        reservedValueStore.reserveValuesJpa( reservedValue, Lists.newArrayList( reservedValue.getValue() ) );

        assertTrue( reservedValueStore.isReserved( Objects.TRACKEDENTITYATTRIBUTE.name(), teaUid, prog001 ) );

        reservedValueStore.removeUsedOrExpiredReservations();

        assertFalse( reservedValueStore
            .getIfReservedValues( reservedValue, Lists.newArrayList( reservedValue.getValue() ) )
            .contains( reservedValue ) );
    }

    @Test
    public void removeExpiredReservationsDoesNotRemoveAnythingIfNothingHasExpired()
    {
        reservedValueStore.save( reservedValue );

        int num = reservedValueStore.getCount();

        reservedValueStore.removeUsedOrExpiredReservations();

        assertEquals( num, reservedValueStore.getCount() );
    }

    @Test
    public void shouldNotAddAlreadyReservedValues()
    {

        reservedValueStore.save( reservedValue );

        OrganisationUnit ou = createOrganisationUnit( "OU" );
        organisationUnitStore.save( ou );

        TrackedEntityInstance tei = createTrackedEntityInstance( ou );
        trackedEntityInstanceStore.save( tei );

        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'Y' );
        tea.setUid( teaUid );
        trackedEntityAttributeStore.save( tea );

        TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue( 'Z', tei, tea );
        teav.setValue( prog001 );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( teav );

        reservedValueStore.reserveValuesAndCheckUniqueness( reservedValue,
            new ArrayList<String>( Collections.singletonList( prog001 ) ) );

        assertEquals( 1, reservedValueStore.getCount() );
    }

    @Test
    public void shouldRemoveAlreadyUsedReservedValues()
    {
        reservedValueStore.save( reservedValue );

        OrganisationUnit ou = createOrganisationUnit( "OU" );
        organisationUnitStore.save( ou );

        TrackedEntityInstance tei = createTrackedEntityInstance( ou );
        trackedEntityInstanceStore.save( tei );

        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'Y' );
        tea.setUid( teaUid );
        trackedEntityAttributeStore.save( tea );

        TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue( 'Z', tei, tea );
        teav.setValue( prog001 );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( teav );

        reservedValueStore.removeUsedOrExpiredReservations();

        assertFalse( reservedValueStore.isReserved( Objects.TRACKEDENTITYATTRIBUTE.name(), teaUid, prog001 ) );

        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    public void shouldRemoveAlreadyUsedOrExpiredReservedValues()
    {
        // expired value
        Calendar pastDate = Calendar.getInstance();
        pastDate.add( Calendar.DATE, -1 );

        reservedValueStore.reserveValuesJpa(
            reservedValue.toBuilder().expiryDate( pastDate.getTime() ).value( prog002 ).build(),
            Lists.newArrayList( prog002 ) );

        // used value
        OrganisationUnit ou = createOrganisationUnit( "OU" );
        organisationUnitStore.save( ou );

        TrackedEntityInstance tei = createTrackedEntityInstance( ou );
        trackedEntityInstanceStore.save( tei );

        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'Y' );
        tea.setUid( teaUid );
        trackedEntityAttributeStore.save( tea );

        TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue( 'Z', tei, tea );
        teav.setValue( prog001 );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( teav );

        reservedValueStore.save( reservedValue );

        reservedValueStore.removeUsedOrExpiredReservations();

        assertFalse( reservedValueStore.isReserved( Objects.TRACKEDENTITYATTRIBUTE.name(), teaUid, prog001 ) );
        assertFalse( reservedValueStore.isReserved( Objects.TRACKEDENTITYATTRIBUTE.name(), teaUid, prog002 ) );

        assertEquals( 0, reservedValueStore.getCount() );
    }

    private ReservedValue getFreeReservedValue()
    {
        return ReservedValue.builder().ownerObject( Objects.TRACKEDENTITYATTRIBUTE.name() ).created( new Date() )
            .ownerUid( "FREE" )
            .key( "00X" ).value( String.format( "%03d", counter++ ) ).expiryDate( futureDate ).build();
    }
}
