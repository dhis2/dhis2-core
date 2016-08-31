package org.hisp.dhis.commons.filter;

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

import static org.hisp.dhis.i18n.I18nUtils.getCountByName;
import static org.hisp.dhis.i18n.I18nUtils.getObjectsBetween;
import static org.hisp.dhis.i18n.I18nUtils.getObjectsBetweenByName;

import java.util.List;

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.common.filter.MetaDataFilter;
import org.hisp.dhis.common.filter.MetaDataFilterService;
import org.hisp.dhis.i18n.I18nService;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ovidiu Rosu <rosu.ovi@gmail.com>
 */
@Transactional
public class DefaultMetaDataFilterService
    implements MetaDataFilterService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<MetaDataFilter> metaDataFilterStore;

    public void setMetaDataFilterStore( GenericIdentifiableObjectStore<MetaDataFilter> metaDataFilterStore )
    {
        this.metaDataFilterStore = metaDataFilterStore;
    }

    private I18nService i18nService;

    public void setI18nService( I18nService i18nService )
    {
        this.i18nService = i18nService;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @Override
    public MetaDataFilter getFilter( Integer id )
    {
        return metaDataFilterStore.get( id );
    }

    @Override
    public MetaDataFilter getFilterByUid( String uid )
    {
        return metaDataFilterStore.getByUid( uid );
    }

    @Override
    public List<MetaDataFilter> getAllFilters()
    {
        return metaDataFilterStore.getAll();
    }

    @Override
    public List<MetaDataFilter> getFiltersBetweenByName( String name, int first, int max )
    {
        return getObjectsBetweenByName( i18nService, metaDataFilterStore, name, first, max );
    }

    @Override
    public List<MetaDataFilter> getFiltersBetween( int first, int max )
    {
        return getObjectsBetween( i18nService, metaDataFilterStore, first, max );
    }

    @Override
    public void saveFilter( MetaDataFilter metaDataFilter )
    {
        metaDataFilterStore.save( metaDataFilter );
    }

    @Override
    public void updateFilter( MetaDataFilter metaDataFilter )
    {
        metaDataFilterStore.update( metaDataFilter );
    }

    @Override
    public void deleteFilter( MetaDataFilter metaDataFilter )
    {
        metaDataFilterStore.delete( metaDataFilter );
    }

    @Override
    public int getFilterCountByName( String name )
    {
        return getCountByName( i18nService, metaDataFilterStore, name );
    }

    @Override
    public int getFilterCount()
    {
        return metaDataFilterStore.getCount();
    }
}
