package org.hisp.dhis.common.hibernate;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hibernate.Query;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateAnalyticalObjectStore<T extends BaseAnalyticalObject>
    extends HibernateIdentifiableObjectStore<T> implements AnalyticalObjectStore<T>
{
    //TODO program indicator, tracked entity attribute
    
    @Override
    public int countIndicatorAnalyticalObject( Indicator indicator )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c join c.dataDimensionItems d where d.indicator = :indicator" );
        query.setEntity( "indicator", indicator );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public int countDataElementAnalyticalObject( DataElement dataElement )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c join c.dataDimensionItems d where d.dataElement = :dataElement" );
        query.setEntity( "dataElement", dataElement );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public int countDataSetAnalyticalObject( DataSet dataSet )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c join c.dataDimensionItems d where d.dataSet = :dataSet" );
        query.setEntity( "dataSet", dataSet );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public int countPeriodAnalyticalObject( Period period )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where :period in elements(c.periods)" );
        query.setEntity( "period", period );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public int countOrganisationUnitAnalyticalObject( OrganisationUnit organisationUnit )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where :organisationUnit in elements(c.organisationUnits)" );
        query.setEntity( "organisationUnit", organisationUnit );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public int countCategoryOptionGroupAnalyticalObject( CategoryOptionGroup categoryOptionGroup )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where :categoryOptionGroup in elements(c.categoryOptionGroups)" );
        query.setEntity( "categoryOptionGroup", categoryOptionGroup );

        return ((Long) query.uniqueResult()).intValue();
    }
}
