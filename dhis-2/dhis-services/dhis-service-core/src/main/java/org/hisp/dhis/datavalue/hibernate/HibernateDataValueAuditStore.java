package org.hisp.dhis.datavalue.hibernate;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
public class HibernateDataValueAuditStore extends HibernateGenericStore<DataValueAudit>
    implements DataValueAuditStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private PeriodStore periodStore;

    public void setPeriodStore( PeriodStore periodStore )
    {
        this.periodStore = periodStore;
    }

    // -------------------------------------------------------------------------
    // DataValueAuditStore implementation
    // -------------------------------------------------------------------------

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
    public List<DataValueAudit> getDataValueAudits( DataValue dataValue )
    {
        return getDataValueAudits( Lists.newArrayList( dataValue.getDataElement() ),
            Lists.newArrayList( dataValue.getPeriod() ),
            Lists.newArrayList( dataValue.getSource() ),
            dataValue.getCategoryOptionCombo(),
            dataValue.getAttributeOptionCombo(), null );
    }

    @Override
    public List<DataValueAudit> getDataValueAudits( List<DataElement> dataElements, List<Period> periods, List<OrganisationUnit> organisationUnits,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo, AuditType auditType )
    {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();

        List<Function<Root<DataValueAudit>, Predicate>> predicates = getDataValueAuditPredicates( builder, dataElements,
            periods, organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType );

        if ( !predicates.isEmpty() )
        {
            return getList( builder, newJpaParameters()
                .addPredicate( root -> builder.and( predicates.stream().map( p -> p.apply( root ) ).collect(
                    Collectors.toList() ).toArray( new Predicate[ predicates.size() ] ) ) )
                .addOrder( root -> builder.desc( root.get( "created" ) ) ) );
        }
        else
        {
            return new ArrayList<>();
        }
    }

    @Override
    public List<DataValueAudit> getDataValueAudits( List<DataElement> dataElements, List<Period> periods, List<OrganisationUnit> organisationUnits,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo, AuditType auditType, int first, int max )
    {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();

        List<Function<Root<DataValueAudit>, Predicate>> predicates = getDataValueAuditPredicates( builder, dataElements, periods, organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType );

        if ( !predicates.isEmpty() )
        {
            return getList( builder, newJpaParameters()
                .addPredicate( root -> builder.and( predicates.stream().map( p -> p.apply( root ) ).collect(
                    Collectors.toList() ).toArray( new Predicate[ predicates.size() ] ) ) )
                .addOrder( root -> builder.desc( root.get( "created" ) ) )
                .setFirstResult( first )
                .setMaxResults( max ) );
        }
        else
        {
            return new ArrayList<>();
        }
    }

    @Override
    public int countDataValueAudits( List<DataElement> dataElements, List<Period> periods, List<OrganisationUnit> organisationUnits,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo, AuditType auditType )
    {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();

        List<Function<Root<DataValueAudit>, Predicate>> predicates = getDataValueAuditPredicates( builder, dataElements, periods, organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType );

        if ( !predicates.isEmpty() )
        {
            return getCount( builder, newJpaParameters()
                .addPredicate( root -> builder.and( predicates.stream().map( p -> p.apply( root ) ).collect(
                    Collectors.toList() ).toArray( new Predicate[ predicates.size() ] ) ) )
                .count( root -> builder.countDistinct( root.get( "id" ) ) ) ).intValue();
        }
        else
        {
            return 0;
        }
    }

    /**
     * Return list of Predicates generated from given parameters
     * Return empty list if given Period does not exist in database, means no data available.
     * @param builder
     * @param dataElements
     * @param periods
     * @param organisationUnits
     * @param categoryOptionCombo
     * @param attributeOptionCombo
     * @param auditType
     * @return
     */
    private List<Function<Root<DataValueAudit>, Predicate>> getDataValueAuditPredicates( CriteriaBuilder builder, List<DataElement> dataElements, List<Period> periods, List<OrganisationUnit> organisationUnits,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo, AuditType auditType )
    {
        List<Period> storedPeriods = new ArrayList<>();

        if ( periods != null && !periods.isEmpty() )
        {
            for ( Period period : periods )
            {
                Period storedPeriod = periodStore.reloadPeriod( period );

                if ( storedPeriod != null )
                {
                    storedPeriods.add( storedPeriod );
                }
            }
        }

        List<Function<Root<DataValueAudit>, Predicate>> predicates = new ArrayList<>();

        if ( storedPeriods != null && !storedPeriods.isEmpty() )
        {
            predicates.add( root -> root.get( "period" ).in( storedPeriods ) );
        }
        else if ( periods != null && !periods.isEmpty() )
        {
            return predicates;
        }

        if ( dataElements != null && !dataElements.isEmpty() )
        {
            predicates.add( root -> root.get( "dataElement" ).in( dataElements ) );
        }

        if ( organisationUnits != null && !organisationUnits.isEmpty() )
        {
            predicates.add( root -> root.get( "organisationUnit" ).in( organisationUnits ) );
        }

        if ( categoryOptionCombo != null )
        {
            predicates.add( root -> builder.equal( root.get( "categoryOptionCombo" ), categoryOptionCombo ) );
        }

        if ( attributeOptionCombo != null )
        {
            predicates.add( root -> builder.equal( root.get( "attributeOptionCombo" ), attributeOptionCombo ) );
        }

        if ( auditType != null )
        {
            predicates.add( root -> builder.equal( root.get( "auditType" ), auditType ) );
        }

        return predicates;
    }
}