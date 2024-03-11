package org.hisp.dhis.trackedentity.hibernate;

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

import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Abyot Asalefew Gizaw
 */
public class HibernateTrackedEntityAttributeStore
    extends HibernateIdentifiableObjectStore<TrackedEntityAttribute>
    implements TrackedEntityAttributeStore
{
    private final StatementBuilder statementBuilder;

    public HibernateTrackedEntityAttributeStore( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public List<TrackedEntityAttribute> getByDisplayOnVisitSchedule( boolean displayOnVisitSchedule )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "displayOnVisitSchedule" ), displayOnVisitSchedule ) ) );
    }

    @Override
    public List<TrackedEntityAttribute> getDisplayInListNoProgram()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "displayInListNoProgram" ), true ) ) );
    }

    @Override
    public Optional<String> getTrackedEntityInstanceUidWithUniqueAttributeValue( TrackedEntityInstanceQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Select clause
        // ---------------------------------------------------------------------

        SqlHelper hlp = new SqlHelper( true );

        String hql = "select tei.uid from TrackedEntityInstance tei ";

        if ( params.hasOrganisationUnits() )
        {
            String orgUnitUids = params.getOrganisationUnits().stream()
                .map( OrganisationUnit::getUid )
                .collect( Collectors.joining( ", ", "'", "'" ) );

            hql += "inner join tei.organisationUnit as ou ";
            hql += hlp.whereAnd() + " ou.uid in (" + orgUnitUids + ") ";
        }

        for ( QueryItem item : params.getAttributes() )
        {
            for ( QueryFilter filter : item.getFilters() )
            {
                final String encodedFilter = filter.getSqlFilter( statementBuilder.encode( StringUtils.lowerCase( filter.getFilter() ), false ) );

                hql += hlp.whereAnd() + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";
                hql += " and teav.attribute.uid='" + item.getItemId() + "'";

                if ( item.isNumeric() )
                {
                    hql += " and teav.plainValue " + filter.getSqlOperator() + encodedFilter + ")";
                }
                else
                {
                    hql += " and lower(teav.plainValue) " + filter.getSqlOperator() + encodedFilter + ")";
                }
            }
        }

        if ( !params.isIncludeDeleted() )
        {
            hql += hlp.whereAnd() + " tei.deleted is false";
        }

        Query<String> query = getTypedQuery( hql );
        query.setMaxResults( 1 );

        Iterator<String> it = query.iterate();

        if ( it.hasNext() )
        {
            return Optional.of( it.next());
        }

        return Optional.empty();
    }
}
