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
package org.hisp.dhis.common.hibernate;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateAnalyticalObjectStore<T extends BaseAnalyticalObject>
    extends HibernateIdentifiableObjectStore<T>
    implements AnalyticalObjectStore<T>
{
    public HibernateAnalyticalObjectStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher,
        Class<T> clazz, CurrentUserService currentUserService, AclService aclService,
        boolean cacheable )
    {
        super( sessionFactory, jdbcTemplate, publisher, clazz, currentUserService, aclService, cacheable );
    }

    @Override
    public List<T> getAnalyticalObjects( ExpressionDimensionItem expressionDimensionItem )
    {
        String hql = "select distinct c from " + clazz.getName()
            + " c join c.dataDimensionItems d where d.expressionDimensionItem = :expressionDimensionItem";
        return getQuery( hql ).setParameter( "expressionDimensionItem", expressionDimensionItem ).list();
    }

    // TODO program indicator, tracked entity attribute

    @Override
    public List<T> getAnalyticalObjects( Indicator indicator )
    {
        String hql = "select distinct c from " + clazz.getName()
            + " c join c.dataDimensionItems d where d.indicator = :indicator";
        return getQuery( hql ).setParameter( "indicator", indicator ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( DataElement dataElement )
    {
        String hql = "select distinct c from " + clazz.getName()
            + " c join c.dataDimensionItems d" +
            " where d.dataElement = :dataElement" +
            " or d.dataElementOperand.dataElement = :dataElement" +
            " or d.programDataElement.dataElement = :dataElement";

        return getQuery( hql ).setParameter( "dataElement", dataElement ).list();
    }

    @Override
    public List<T> getAnalyticalObjectsByDataDimension( DataElement dataElement )
    {
        String hql = "select distinct c from " + clazz.getName()
            + " c join c.dataElementDimensions d where d.dataElement = :dataElement";
        return getQuery( hql ).setParameter( "dataElement", dataElement ).list();
    }

    @Override
    public List<T> getAnalyticalObjectsByDataDimension( TrackedEntityAttribute attribute )
    {
        String hql = "select distinct c from " + clazz.getName()
            + " c join c.attributeDimensions d where d.attribute = :attribute";
        return getQuery( hql ).setParameter( "attribute", attribute ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( DataSet dataSet )
    {
        String hql = "select distinct c from " + clazz.getName()
            + " c join c.dataDimensionItems d where d.reportingRate.dataSet = :dataSet";
        return getQuery( hql ).setParameter( "dataSet", dataSet ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( ProgramIndicator programIndicator )
    {
        String hql = "select distinct c from " + clazz.getName()
            + " c join c.dataDimensionItems d where d.programIndicator = :programIndicator";
        return getQuery( hql ).setParameter( "programIndicator", programIndicator ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( Period period )
    {
        String hql = "from " + clazz.getName() + " c where :period in elements(c.periods)";
        return getQuery( hql ).setParameter( "period", period ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( OrganisationUnit organisationUnit )
    {
        String hql = "from " + clazz.getName() + " c where :organisationUnit in elements(c.organisationUnits)";
        return getQuery( hql ).setParameter( "organisationUnit", organisationUnit ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( OrganisationUnitGroup organisationUnitGroup )
    {
        String hql = "from " + clazz.getName()
            + " c JOIN FETCH c.organisationUnitGroupSetDimensions d where :organisationUnitGroup in elements(d.items)";
        return getQuery( hql ).setParameter( "organisationUnitGroup", organisationUnitGroup ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( OrganisationUnitGroupSet organisationUnitGroupSet )
    {
        String hql = "from " + clazz.getName()
            + " c JOIN FETCH c.organisationUnitGroupSetDimensions d where :organisationUnitGroupSet = d.dimension";
        return getQuery( hql ).setParameter( "organisationUnitGroupSet", organisationUnitGroupSet ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( CategoryOptionGroup categoryOptionGroup )
    {
        String hql = "from " + clazz.getName() + " c where :categoryOptionGroup in elements(c.categoryOptionGroups)";
        return getQuery( hql ).setParameter( "categoryOptionGroup", categoryOptionGroup ).list();
    }

    @Override
    public List<T> getAnalyticalObjects( LegendSet legendSet )
    {
        String embeddedObject = "";

        if ( clazz == Visualization.class )
        {
            embeddedObject = "legendDefinitions.";
        }

        String hql = "from " + clazz.getName() + " c where c." + embeddedObject + "legendSet = :legendSet";
        return getQuery( hql ).setParameter( "legendSet", legendSet ).list();
    }

    @Override
    public long countAnalyticalObjects( Indicator indicator )
    {
        Query<Long> query = getTypedQuery( "select count(distinct c) from " + clazz.getName()
            + " c join c.dataDimensionItems d where d.indicator = :indicator" );
        query.setParameter( "indicator", indicator );
        return query.uniqueResult();
    }

    @Override
    public long countAnalyticalObjects( DataElement dataElement )
    {
        Query<Long> query = getTypedQuery( "select count(distinct c) from " + clazz.getName()
            + " c join c.dataDimensionItems d where d.dataElement = :dataElement" );
        query.setParameter( "dataElement", dataElement );
        return query.uniqueResult();
    }

    @Override
    public long countAnalyticalObjects( DataSet dataSet )
    {
        Query<Long> query = getTypedQuery( "select count(distinct c) from " + clazz.getName()
            + " c join c.dataDimensionItems d where d.dataSet = :dataSet" );
        query.setParameter( "dataSet", dataSet );
        return query.uniqueResult();
    }

    @Override
    public long countAnalyticalObjects( ProgramIndicator programIndicator )
    {
        Query<Long> query = getTypedQuery( "select count(distinct c) from " + clazz.getName()
            + " c join c.dataDimensionItems d where d.programIndicator = :programIndicator" );
        query.setParameter( "dataSet", programIndicator );
        return query.uniqueResult();
    }

    @Override
    public long countAnalyticalObjects( Period period )
    {
        Query<Long> query = getTypedQuery(
            "select count(distinct c) from " + clazz.getName() + " c where :period in elements(c.periods)" );
        query.setParameter( "period", period );
        return query.uniqueResult();
    }

    @Override
    public long countAnalyticalObjects( OrganisationUnit organisationUnit )
    {
        Query<Long> query = getTypedQuery( "select count(distinct c) from " + clazz.getName()
            + " c where :organisationUnit in elements(c.organisationUnits)" );
        query.setParameter( "organisationUnit", organisationUnit );
        return query.uniqueResult();
    }

    @Override
    public long countAnalyticalObjects( CategoryOptionGroup categoryOptionGroup )
    {
        Query<Long> query = getTypedQuery( "select count(distinct c) from " + clazz.getName()
            + " c where :categoryOptionGroup in elements(c.categoryOptionGroups)" );
        query.setParameter( "categoryOptionGroup", categoryOptionGroup );
        return query.uniqueResult();
    }
}
