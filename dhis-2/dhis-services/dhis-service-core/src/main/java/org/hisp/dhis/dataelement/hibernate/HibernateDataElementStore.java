package org.hisp.dhis.dataelement.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementStore;

import java.util.List;

/**
 * @author Torgeir Lorange Ostby
 */
public class HibernateDataElementStore
    extends HibernateIdentifiableObjectStore<DataElement>
    implements DataElementStore
{
    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataElement> getDataElementsByDomainType( DataElementDomain domainType )
    {
        return getCriteria( Restrictions.eq( "domainType", domainType ) ).list();
    }
    
    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataElement> getDataElementByCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        return getCriteria( Restrictions.eq( "categoryCombo", categoryCombo ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataElement> getDataElementsByZeroIsSignificant( boolean zeroIsSignificant )
    {
        Criteria criteria = getCriteria();
        criteria.add( Restrictions.eq( "zeroIsSignificant", zeroIsSignificant ) );
        criteria.add( Restrictions.in( "valueType", ValueType.NUMERIC_TYPES ) );

        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataElement> getDataElementsWithoutGroups()
    {
        String hql = "from DataElement d where size(d.groups) = 0";

        return getQuery( hql ).setCacheable( true ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataElement> getDataElementsWithoutDataSets()
    {
        String hql = "from DataElement d where size(d.dataSetElements) = 0 and d.domainType =:domainType";

        return getQuery( hql ).setParameter( "domainType", DataElementDomain.AGGREGATE ).setCacheable( true ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataElement> getDataElementsWithDataSets()
    {
        String hql = "from DataElement d where size(d.dataSetElements) > 0";

        return getQuery( hql ).setCacheable( true ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataElement> getDataElementsByAggregationLevel( int aggregationLevel )
    {
        String hql = "from DataElement de join de.aggregationLevels al where al = :aggregationLevel";

        return getQuery( hql ).setInteger( "aggregationLevel", aggregationLevel ).list();
    }
}
