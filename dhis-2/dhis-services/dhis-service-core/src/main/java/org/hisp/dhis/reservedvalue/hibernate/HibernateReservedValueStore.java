package org.hisp.dhis.reservedvalue.hibernate;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hibernate.query.Query;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.jdbc.batchhandler.ReservedValueBatchHandler;
import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.dhis.reservedvalue.ReservedValueStore;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.Objects.TRACKEDENTITYATTRIBUTE;

/**
 * @author Stian Sandvold
 */
@Transactional
public class HibernateReservedValueStore
    extends HibernateGenericStore<ReservedValue>
    implements ReservedValueStore
{

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    @Override
    public List<ReservedValue> reserveValues( ReservedValue reservedValue,
        List<String> values )
    {
        BatchHandler<ReservedValue> batchHandler = batchHandlerFactory
            .createBatchHandler( ReservedValueBatchHandler.class ).init();

        List<String> availableValues = getIfAvailable( reservedValue, values );

        List<ReservedValue> toAdd = new ArrayList<>();

        availableValues.forEach( ( value ) -> {
            ReservedValue rv = new ReservedValue(
                reservedValue.getOwnerObject(),
                reservedValue.getOwnerUid(),
                reservedValue.getKey(),
                value,
                reservedValue.getExpiryDate()
            );

            rv.setCreated( reservedValue.getCreated() );

            batchHandler.addObject( rv );
            toAdd.add( rv );

        } );

        batchHandler.flush();

        return toAdd;
    }

    @Override
    public List<ReservedValue> getIfReservedValues( ReservedValue reservedValue,
        List<String> values )
    {
        String hql = "from ReservedValue rv where rv.ownerObject =:ownerObject and rv.ownerUid =:ownerUid " +
            "and rv.key =:key and rv.value in :values";

        return getQuery( hql )
            .setParameter( "ownerObject", reservedValue.getOwnerObject() )
            .setParameter( "ownerUid", reservedValue.getOwnerUid() )
            .setParameter( "key", reservedValue.getKey() )
            .setParameter( "values", values )
            .getResultList();
    }

    @Override
    public int getNumberOfUsedValues( ReservedValue reservedValue )
    {
        Query<Long> query = getTypedQuery( "SELECT count(*) FROM ReservedValue WHERE owneruid = :uid AND key = :key" );

        Long count = query.setParameter( "uid", reservedValue.getOwnerUid() )
            .setParameter( "key", reservedValue.getKey() )
            .getSingleResult();


        if ( Objects.valueOf( reservedValue.getOwnerObject() ).equals( TRACKEDENTITYATTRIBUTE ) )
        {
            Query<Long> attrQuery = getTypedQuery(
            "SELECT count(*) " +
                "FROM TrackedEntityAttributeValue " +
                "WHERE attribute = " +
                "( FROM TrackedEntityAttribute " +
                "WHERE uid = :uid ) " +
                "AND value LIKE :value " );

            count += attrQuery.setParameter( "uid", reservedValue.getOwnerUid() )
            .setParameter( "value", reservedValue.getValue() )
            .getSingleResult();
        }

        return count.intValue();
    }

    @Override
    public void removeExpiredReservations()
    {
        getQuery( "DELETE FROM ReservedValue WHERE expiryDate < :now" )
            .setParameter( "now", new Date() )
            .executeUpdate();
    }

    @Override
    public boolean useReservedValue( String ownerUID, String value )
    {
        return getQuery( "DELETE FROM ReservedValue WHERE owneruid = :uid AND value = :value" )
            .setParameter( "uid", ownerUID )
            .setParameter( "value", value )
            .executeUpdate() == 1;
    }

    @Override
    public void deleteReservedValueByUid( String uid )
    {
        getQuery( "DELETE FROM ReservedValue WHERE owneruid = :uid" )
            .setParameter( "uid", uid )
            .executeUpdate();
    }

    @Override
    public boolean isReserved( String ownerObject, String ownerUID, String value )
    {
        String hql = "from ReservedValue rv where rv.ownerObject =:ownerObject and rv.ownerUid =:ownerUid " +
            "and rv.value =:value";

        return !getQuery( hql )
            .setParameter( "ownerObject", ownerObject )
            .setParameter( "ownerUid", ownerUID )
            .setParameter( "value", value )
            .getResultList()
            .isEmpty();
    }

    // Helper methods

    private List<String> getIfAvailable( ReservedValue reservedValue, List<String> values )
    {
        values.removeAll( getIfReservedValues( reservedValue, values ).stream()
            .map( ReservedValue::getValue )
            .collect( Collectors.toList() ) );

        // All values supplied is unavailable
        if ( values.isEmpty() )
        {
            return values;
        }

        if ( Objects.valueOf( reservedValue.getOwnerObject() ).equals( TRACKEDENTITYATTRIBUTE ) )
        {
            values.removeAll( getSqlQuery(
                "SELECT value FROM trackedentityattributevalue WHERE trackedentityattributeid = (SELECT trackedentityattributeid FROM trackedentityattribute WHERE uid = ?1) AND value IN ?2" )
                .setParameter( 1, reservedValue.getOwnerUid() )
                .setParameter( 2, values )
                .list() );
        }

        return values;

    }
}
