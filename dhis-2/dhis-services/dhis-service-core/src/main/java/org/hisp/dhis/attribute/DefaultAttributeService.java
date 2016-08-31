package org.hisp.dhis.attribute;

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

import static org.hisp.dhis.i18n.I18nUtils.i18n;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.i18n.I18nService;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mortenoh
 */
@Transactional
public class DefaultAttributeService
    implements AttributeService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private AttributeStore attributeStore;

    public void setAttributeStore( AttributeStore attributeStore )
    {
        this.attributeStore = attributeStore;
    }

    private GenericStore<AttributeValue> attributeValueStore;

    public void setAttributeValueStore( GenericStore<AttributeValue> attributeValueStore )
    {
        this.attributeValueStore = attributeValueStore;
    }

    private I18nService i18nService;

    public void setI18nService( I18nService service )
    {
        i18nService = service;
    }

    // -------------------------------------------------------------------------
    // Attribute implementation
    // -------------------------------------------------------------------------

    @Override
    public void addAttribute( Attribute attribute )
    {
        attributeStore.save( attribute );
    }

    @Override
    public void updateAttribute( Attribute attribute )
    {
        attributeStore.update( attribute );
    }

    @Override
    public void deleteAttribute( Attribute attribute )
    {
        attributeStore.delete( attribute );
    }

    @Override
    public Attribute getAttribute( int id )
    {
        return i18n( i18nService, attributeStore.get( id ) );
    }

    @Override
    public Attribute getAttribute( String uid )
    {
        return i18n( i18nService, attributeStore.getByUid( uid ) );
    }

    @Override
    public Attribute getAttributeByName( String name )
    {
        return i18n( i18nService, attributeStore.getByName( name ) );
    }

    @Override
    public Attribute getAttributeByCode( String code )
    {
        return i18n( i18nService, attributeStore.getByCode( code ) );
    }

    @Override
    public List<Attribute> getAllAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getAll() ) );
    }

    @Override
    public List<Attribute> getDataElementAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getDataElementAttributes() ) );
    }

    @Override
    public List<Attribute> getDataElementGroupAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getDataElementGroupAttributes() ) );
    }

    @Override
    public List<Attribute> getIndicatorAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getIndicatorAttributes() ) );
    }

    @Override
    public List<Attribute> getIndicatorGroupAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getIndicatorGroupAttributes() ) );
    }

    @Override
    public List<Attribute> getDataSetAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getDataSetAttributes() ) );
    }

    @Override
    public List<Attribute> getOrganisationUnitAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getOrganisationUnitAttributes() ) );
    }

    @Override
    public List<Attribute> getOrganisationUnitGroupAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getOrganisationUnitGroupAttributes() ) );
    }

    @Override public List<Attribute> getOrganisationUnitGroupSetAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getOrganisationUnitGroupSetAttributes() ) );
    }

    @Override
    public List<Attribute> getUserAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getUserAttributes() ) );
    }

    @Override
    public List<Attribute> getUserGroupAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getUserGroupAttributes() ) );
    }

    @Override
    public List<Attribute> getProgramAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getProgramAttributes() ) );
    }

    @Override
    public List<Attribute> getProgramStageAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getProgramStageAttributes() ) );
    }

    @Override
    public List<Attribute> getTrackedEntityAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getTrackedEntityAttributes() ) );
    }

    @Override
    public List<Attribute> getTrackedEntityAttributeAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getTrackedEntityAttributeAttributes() ) );
    }

    @Override
    public List<Attribute> getCategoryOptionAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getCategoryOptionAttributes() ) );
    }

    @Override
    public List<Attribute> getCategoryOptionGroupAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getCategoryOptionGroupAttributes() ) );
    }

    @Override
    public int getAttributeCount()
    {
        return attributeStore.getCount();
    }

    @Override
    public int getAttributeCountByName( String name )
    {
        return attributeStore.getCountLikeName( name );
    }

    @Override
    public List<Attribute> getAttributesBetween( int first, int max )
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getAllOrderedName( first, max ) ) );
    }

    @Override
    public List<Attribute> getAttributesBetweenByName( String name, int first, int max )
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getAllLikeName( name, first, max ) ) );
    }

    // -------------------------------------------------------------------------
    // AttributeValue implementation
    // -------------------------------------------------------------------------

    @Override
    public void addAttributeValue( AttributeValue attributeValue )
    {
        attributeValue.setAutoFields();
        attributeValueStore.save( attributeValue );
    }

    @Override
    public void updateAttributeValue( AttributeValue attributeValue )
    {
        attributeValue.setAutoFields();
        attributeValueStore.update( attributeValue );
    }

    @Override
    public void deleteAttributeValue( AttributeValue attributeValue )
    {
        attributeValueStore.delete( attributeValue );
    }

    @Override
    public AttributeValue getAttributeValue( int id )
    {
        return attributeValueStore.get( id );
    }

    @Override
    public List<AttributeValue> getAllAttributeValues()
    {
        return new ArrayList<>( attributeValueStore.getAll() );
    }

    @Override
    public int getAttributeValueCount()
    {
        return attributeValueStore.getCount();
    }
}
