package org.hisp.dhis.attribute.hibernate;

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

import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeStore;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mortenoh
 */
public class HibernateAttributeStore
    extends HibernateIdentifiableObjectStore<Attribute>
    implements AttributeStore
{
    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getDataElementAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "dataElementAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getDataElementGroupAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "dataElementGroupAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getIndicatorAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "indicatorAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getIndicatorGroupAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "indicatorGroupAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getDataSetAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "dataSetAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getOrganisationUnitAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "organisationUnitAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getOrganisationUnitGroupAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "organisationUnitGroupAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getOrganisationUnitGroupSetAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "organisationUnitGroupSetAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getUserAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "userAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getUserGroupAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "userGroupAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getProgramAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "programAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getProgramStageAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "programStageAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getTrackedEntityAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "trackedEntityAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getTrackedEntityAttributeAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "trackedEntityAttributeAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getCategoryOptionAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "categoryOptionAttribute", true ) ).list() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Attribute> getCategoryOptionGroupAttributes()
    {
        return new ArrayList<>( getCriteria( Restrictions.eq( "categoryOptionGroupAttribute", true ) ).list() );
    }
}
