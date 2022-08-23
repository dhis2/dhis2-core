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
package org.hisp.dhis.datavalue.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.SessionFactory;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
@Repository( "org.hisp.dhis.datavalue.DataValueAuditStore" )
public class HibernateDataValueAuditStore extends HibernateGenericStore<DataValueAudit>
    implements DataValueAuditStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private PeriodStore periodStore;

    public HibernateDataValueAuditStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, PeriodStore periodStore )
    {
        super( sessionFactory, jdbcTemplate, publisher, DataValueAudit.class, false );

        checkNotNull( periodStore );

        this.periodStore = periodStore;
    }

    // -------------------------------------------------------------------------
    // DataValueAuditStore implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void updateDataValueAudit( DataValueAudit dataValueAudit )
    {
        getSession().update( dataValueAudit );
    }

    @Override
    public void addDataValueAudit( DataValueAudit dataValueAudit )
    {
        getSession().save( dataValueAudit );
    }

    @Override
    public void deleteDataValueAudits( OrganisationUnit organisationUnit )
    {
        String hql = "delete from DataValueAudit d where d.organisationUnit = :unit";

        getSession().createQuery( hql ).setParameter( "unit", organisationUnit ).executeUpdate();
    }

    @Override
    public void deleteDataValueAudits( DataElement dataElement )
    {
        String hql = "delete from DataValueAudit d where d.dataElement = :dataElement";

        getSession().createQuery( hql ).setParameter( "dataElement", dataElement ).executeUpdate();
    }

    @Override
    public List<DataValueAudit> getDataValueAudits( DataValueAuditQueryParams params )
    {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();

        JpaQueryParameters<DataValueAudit> queryParams = newJpaParameters()
            .addPredicates( getDataValueAuditPredicates( builder, params ) )
            .addOrder( root -> builder.desc( root.get( "created" ) ) );

        if ( params.hasPaging() )
        {
            queryParams
                .setFirstResult( params.getPager().getOffset() )
                .setMaxResults( params.getPager().getPageSize() );
        }

        return getList( builder, queryParams );
    }

    @Override
    public int countDataValueAudits( DataValueAuditQueryParams params )
    {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();

        List<Function<Root<DataValueAudit>, Predicate>> predicates = getDataValueAuditPredicates( builder, params );

        return getCount( builder, newJpaParameters()
            .addPredicates( predicates )
            .count( root -> builder.countDistinct( root.get( "id" ) ) ) ).intValue();
    }

    /**
     * Returns a list of Predicates generated from given parameters. Returns an
     * empty list if given Period does not exist in database.
     *
     * @param builder the {@link CriteriaBuilder}.
     * @param params the {@link DataValueAuditQueryParams}.
     */
    private List<Function<Root<DataValueAudit>, Predicate>> getDataValueAuditPredicates( CriteriaBuilder builder,
        DataValueAuditQueryParams params )
    {
        List<Period> storedPeriods = new ArrayList<>();

        if ( !params.getPeriods().isEmpty() )
        {
            for ( Period period : params.getPeriods() )
            {
                Period storedPeriod = periodStore.reloadPeriod( period );

                if ( storedPeriod != null )
                {
                    storedPeriods.add( storedPeriod );
                }
            }
        }

        List<Function<Root<DataValueAudit>, Predicate>> predicates = new ArrayList<>();

        if ( !storedPeriods.isEmpty() )
        {
            predicates.add( root -> root.get( "period" ).in( storedPeriods ) );
        }
        else if ( !params.getPeriods().isEmpty() )
        {
            return predicates;
        }

        if ( !params.getDataElements().isEmpty() )
        {
            predicates.add( root -> root.get( "dataElement" ).in( params.getDataElements() ) );
        }

        if ( !params.getOrgUnits().isEmpty() )
        {
            predicates.add( root -> root.get( "organisationUnit" ).in( params.getOrgUnits() ) );
        }

        if ( params.getCategoryOptionCombo() != null )
        {
            predicates
                .add( root -> builder.equal( root.get( "categoryOptionCombo" ), params.getCategoryOptionCombo() ) );
        }

        if ( params.getAttributeOptionCombo() != null )
        {
            predicates
                .add( root -> builder.equal( root.get( "attributeOptionCombo" ), params.getAttributeOptionCombo() ) );
        }

        if ( !params.getAuditTypes().isEmpty() )
        {
            predicates.add( root -> root.get( "auditType" ).in( params.getAuditTypes() ) );
        }

        return predicates;
    }
}
