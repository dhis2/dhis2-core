package org.hisp.dhis.minmax.hibernate;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Kristian Nordal
 */
public class HibernateMinMaxDataElementStore
    extends HibernateGenericStore<MinMaxDataElement>
    implements MinMaxDataElementStore
{
    // -------------------------------------------------------------------------
    // MinMaxDataElementStore Implementation
    // -------------------------------------------------------------------------

    @Override
    public MinMaxDataElement get( OrganisationUnit source, DataElement dataElement,
        DataElementCategoryOptionCombo optionCombo )
    {
        return (MinMaxDataElement) getCriteria( 
            Restrictions.eq( "source", source ),
            Restrictions.eq( "dataElement", dataElement ), 
            Restrictions.eq( "optionCombo", optionCombo ) ).uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<MinMaxDataElement> get( OrganisationUnit source, DataElement dataElement )
    {
        return getCriteria( 
            Restrictions.eq( "source", source ), 
            Restrictions.eq( "dataElement", dataElement ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<MinMaxDataElement> get( OrganisationUnit source, Collection<DataElement> dataElements )
    {
        if ( dataElements.size() == 0 )
        {
            return new ArrayList<>();
        }

        return getCriteria( 
            Restrictions.eq( "source", source ), 
            Restrictions.in( "dataElement", dataElements ) ).list();
    }
    
    @Override
    public void delete( OrganisationUnit organisationUnit )
    {
        String hql = "delete from MinMaxDataElement m where m.source = :source";
        
        getQuery( hql ).setEntity( "source", organisationUnit ).executeUpdate();
    }
    
    @Override
    public void delete( DataElement dataElement )
    {
        String hql = "delete from MinMaxDataElement m where m.dataElement = :dataElement";
        
        getQuery( hql ).setEntity( "dataElement", dataElement ).executeUpdate();
    }
    
    @Override
    public void delete( DataElementCategoryOptionCombo optionCombo )
    {
        String hql = "delete from MinMaxDataElement m where m.optionCombo = :optionCombo";
        
        getQuery( hql ).setEntity( "optionCombo", optionCombo ).executeUpdate();
    }
    
    @Override
    public void delete( Collection<DataElement> dataElements, Collection<OrganisationUnit> organisationUnits )
    {
        String hql = "delete from MinMaxDataElement m where m.dataElement in (:dataElements) and m.source in (:organisationUnits)";
        
        getQuery( hql ).
            setParameterList( "dataElements", dataElements ).
            setParameterList( "organisationUnits", organisationUnits ).executeUpdate();
    }
}
