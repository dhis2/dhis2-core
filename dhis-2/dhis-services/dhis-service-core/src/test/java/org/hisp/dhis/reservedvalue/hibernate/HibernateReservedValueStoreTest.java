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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    private final ReservedValue reservedValueA = new ReservedValue( Objects.TRACKEDENTITYATTRIBUTE.name(), "A",
        "00X", "001", null );

    private final ReservedValue usedValueA = new ReservedValue( Objects.TRACKEDENTITYATTRIBUTE.name(), "A",
        "00X", "002", null );

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
        reservedValueA.setExpiryDate( futureDate );
        usedValueA.setExpiryDate( futureDate );

        reservedValueStore.save( reservedValueA );

        OrganisationUnit ou = createOrganisationUnit( "OU" );
        organisationUnitStore.save( ou );

        TrackedEntityInstance tei = createTrackedEntityInstance( ou );
        trackedEntityInstanceStore.save( tei );

        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'Y' );
        tea.setUid( "A" );
        trackedEntityAttributeStore.save( tea );

        TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue( 'Z', tei, tea );
        teav.setValue( "002" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( teav );

    }

    @Test
    public void reserveValuesSingleValue()
    {
        int count = reservedValueStore.getCount();
        ReservedValue rv = getFreeReservedValue();
        List<ReservedValue> res = reservedValueStore.reserveValuesJpa( rv, Lists.newArrayList( rv.getValue() ) );

        assertEquals( 1, res.size() );
        assertEquals( reservedValueStore.getCount(), count + 1 );
    }

    @Test
    public void isReservedShouldBeTrue()
    {
        assertTrue(
            reservedValueStore.isReserved( reservedValueA.getOwnerObject(), reservedValueA.getOwnerUid(), "001" ) );
    }

    @Test
    public void isReservedShouldBeFalse()
    {
        assertFalse(
            reservedValueStore.isReserved( reservedValueA.getOwnerObject(), reservedValueA.getOwnerUid(), "100" ) );
    }

    @Test
    public void reserveValuesMultipleValues()
    {
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
    public void reserveValuesSingleValueAlreadyReserved()
    {
        int count = reservedValueStore.getCount();

        List<ReservedValue> res = reservedValueStore
            .reserveValuesJpa( reservedValueA, Lists.newArrayList( reservedValueA.getValue() ) );

        assertEquals( 0, res.size() );
        assertEquals( count, reservedValueStore.getCount() );
    }

    @Test
    public void reserveValuesSingleValueAlreadyUsed()
    {
        int count = reservedValueStore.getCount();

        List<ReservedValue> res = reservedValueStore
            .reserveValuesJpa( reservedValueA, Lists.newArrayList( reservedValueA.getValue() ) );

        assertEquals( 0, res.size() );
        assertEquals( count, reservedValueStore.getCount() );
    }

    @Test
    public void reserveValuesMultipleValuesAlreadyReservedAndUsed()
    {
        int count = reservedValueStore.getCount();

        List<ReservedValue> res = reservedValueStore
            .reserveValuesJpa( reservedValueA, Lists.newArrayList( "001", "002", "003", "004" ) );

        assertEquals( 1, count );
        assertEquals( 3, res.size() );
        assertEquals( (count + 3), reservedValueStore.getCount() );
    }

    @Test
    public void getIfReservedValuesReturnsReservedValue()
    {
        List<ReservedValue> res = reservedValueStore
            .getIfReservedValues( reservedValueA, Lists.newArrayList( reservedValueA.getValue() ) );

        assertEquals( reservedValueA, res.get( 0 ) );
        assertEquals( 1, res.size() );
    }

    @Test
    public void getIfReservedValuesReturnEmptyListWhenNotReserved()
    {
        List<ReservedValue> res = reservedValueStore
            .getIfReservedValues( getFreeReservedValue(), Lists.newArrayList( "999" ) );

        assertEquals( 0, res.size() );
    }

    @Test
    public void getIfReservedValuesReturnOnlyReservedValuesWhenSendingMultipleValues()
    {
        List<ReservedValue> res = reservedValueStore
            .getIfReservedValues( reservedValueA, Lists.newArrayList( "001", "002", "003" ) );

        assertEquals( reservedValueA, res.get( 0 ) );
        assertEquals( 1, res.size() );
    }

    @Test
    public void removeExpiredReservationsRemovesExpiredReservation()
    {
        Calendar pastDate = Calendar.getInstance();
        pastDate.add( Calendar.DATE, -1 );
        ReservedValue expired = getFreeReservedValue();
        expired.setExpiryDate( pastDate.getTime() );
        reservedValueStore.reserveValuesJpa( expired, Lists.newArrayList( expired.getValue() ) );

        assertEquals( expired,
            reservedValueStore.getIfReservedValues( expired, Lists.newArrayList( expired.getValue() ) ).get( 0 ) );

        reservedValueStore.removeExpiredReservations();

        assertFalse( reservedValueStore.getIfReservedValues( expired, Lists.newArrayList( expired.getValue() ) )
            .contains( expired ) );
    }

    @Test
    public void removeExpiredReservationsDoesNotRemoveAnythingIfNothingHasExpired()
    {
        int num = reservedValueStore.getCount();

        reservedValueStore.removeExpiredReservations();

        assertEquals( num, reservedValueStore.getCount() );
    }

    // Helper methods

    private ReservedValue getFreeReservedValue()
    {
        return new ReservedValue(
            Objects.TRACKEDENTITYATTRIBUTE.name(),
            "FREE",
            "00X",
            String.format( "%03d", counter++ ),
            futureDate );
    }
}
