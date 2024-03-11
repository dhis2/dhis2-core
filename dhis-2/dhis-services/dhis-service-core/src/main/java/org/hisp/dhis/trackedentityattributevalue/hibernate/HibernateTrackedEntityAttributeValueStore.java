package org.hisp.dhis.trackedentityattributevalue.hibernate;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.query.Query;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Abyot Asalefew
 */
public class HibernateTrackedEntityAttributeValueStore
    extends HibernateGenericStore<TrackedEntityAttributeValue>
    implements TrackedEntityAttributeValueStore
{
    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public void saveVoid( TrackedEntityAttributeValue attributeValue )
    {
        sessionFactory.getCurrentSession().save( attributeValue );
    }

    @Override
    public int deleteByTrackedEntityInstance( TrackedEntityInstance entityInstance )
    {
        Query<TrackedEntityAttributeValue> query = getQuery( "delete from TrackedEntityAttributeValue where entityInstance = :entityInstance" );
        query.setParameter( "entityInstance", entityInstance );
        return query.executeUpdate();
    }

    @Override
    public TrackedEntityAttributeValue get( TrackedEntityInstance entityInstance, TrackedEntityAttribute attribute )
    {
        String query = " from TrackedEntityAttributeValue v where v.entityInstance =:entityInstance and attribute =:attribute";

        Query<TrackedEntityAttributeValue> typedQuery = getQuery( query )
            .setParameter( "entityInstance", entityInstance )
            .setParameter( "attribute", attribute );

        return getSingleResult( typedQuery );
    }

    @Override
    public List<TrackedEntityAttributeValue> get( TrackedEntityInstance entityInstance )
    {
        String query = " from TrackedEntityAttributeValue v where v.entityInstance =:entityInstance";

        Query<TrackedEntityAttributeValue> typedQuery = getQuery( query ).setParameter( "entityInstance", entityInstance );

        return getList( typedQuery );
    }

    @Override
    public List<TrackedEntityAttributeValue> get( TrackedEntityAttribute attribute )
    {
        String query = " from TrackedEntityAttributeValue v where v.attribute =:attribute";

        Query<TrackedEntityAttributeValue> typedQuery = getQuery( query ).setParameter( "attribute", attribute );

        return getList( typedQuery );
    }

    @Override
    public List<TrackedEntityAttributeValue> get( Collection<TrackedEntityInstance> entityInstances )
    {
        if ( entityInstances == null || entityInstances.isEmpty() )
        {
            return new ArrayList<>();
        }

        String query = " from TrackedEntityAttributeValue v where v.entityInstance  in :entityInstances";

        Query<TrackedEntityAttributeValue> typedQuery = getQuery( query ).setParameter( "entityInstances", entityInstances );

        return getList( typedQuery );
    }

    @Override
    public List<TrackedEntityAttributeValue> searchByValue( TrackedEntityAttribute attribute, String searchText )
    {
        String query = " from TrackedEntityAttributeValue v where v.attribute =:attribute and lower(v.plainValue) like :searchText";

        Query<TrackedEntityAttributeValue> typedQuery = getQuery( query )
            .setParameter( "attribute", attribute )
            .setParameter( "searchText", "%" + StringUtils.lowerCase( searchText  ) + "%");

        return getList( typedQuery );
    }

    @Override
    public List<TrackedEntityAttributeValue> get( TrackedEntityAttribute attribute, String value )
    {
        String query = " from TrackedEntityAttributeValue v where v.attribute =:attribute and lower(v.plainValue) like :value";

        Query<TrackedEntityAttributeValue> typedQuery = getQuery( query )
            .setParameter( "attribute", attribute )
            .setParameter( "value", StringUtils.lowerCase( value ) );

        return getList( typedQuery );
    }

    @Override
    public List<TrackedEntityAttributeValue> get( TrackedEntityInstance entityInstance, Program program )
    {
        String query = " from TrackedEntityAttributeValue v where v.entityInstance =:entityInstance and v.attribute.program =:program";

        Query<TrackedEntityAttributeValue> typedQuery = getQuery( query );
        typedQuery.setParameter( "entityInstance", entityInstance );
        typedQuery.setParameter( "program", program );

        return getList( typedQuery );
    }

    @Override
    public int getCountOfAssignedTEAValues( TrackedEntityAttribute attribute )
    {
        Query query = getQuery( "select count(distinct c) from TrackedEntityAttributeValue c where c.attribute = :attribute" );
        query.setParameter( "attribute", attribute );

        return ((Long) query.getSingleResult()).intValue();
    }
}
