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
package org.hisp.dhis.minmax;

import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Transactional
@Service( "org.hisp.dhis.minmax.MinMaxDataElementService" )
public class DefaultMinMaxDataElementService
    implements MinMaxDataElementService
{
    private final MinMaxDataElementStore minMaxDataElementStore;

    // -------------------------------------------------------------------------
    // MinMaxDataElementService implementation
    // -------------------------------------------------------------------------

    @Override
    public long addMinMaxDataElement( MinMaxDataElement minMaxDataElement )
    {
        minMaxDataElementStore.save( minMaxDataElement );

        return minMaxDataElement.getId();
    }

    @Override
    public void deleteMinMaxDataElement( MinMaxDataElement minMaxDataElement )
    {
        minMaxDataElementStore.delete( minMaxDataElement );
    }

    @Override
    public void updateMinMaxDataElement( MinMaxDataElement minMaxDataElement )
    {
        minMaxDataElementStore.update( minMaxDataElement );
    }

    @Override
    public MinMaxDataElement getMinMaxDataElement( long id )
    {
        return minMaxDataElementStore.get( id );
    }

    @Override
    public MinMaxDataElement getMinMaxDataElement( OrganisationUnit source, DataElement dataElement,
        CategoryOptionCombo optionCombo )
    {
        return minMaxDataElementStore.get( source, dataElement, optionCombo );
    }

    @Override
    public List<MinMaxDataElement> getMinMaxDataElements( OrganisationUnit source,
        Collection<DataElement> dataElements )
    {
        return minMaxDataElementStore.get( source, dataElements );
    }

    @Override
    public List<MinMaxDataElement> getMinMaxDataElements( MinMaxDataElementQueryParams query )
    {
        return minMaxDataElementStore.query( query );
    }

    @Override
    public int countMinMaxDataElements( MinMaxDataElementQueryParams query )
    {
        return minMaxDataElementStore.countMinMaxDataElements( query );
    }

    @Override
    public void removeMinMaxDataElements( OrganisationUnit organisationUnit )
    {
        minMaxDataElementStore.delete( organisationUnit );
    }

    @Override
    public void removeMinMaxDataElements( DataElement dataElement )
    {
        minMaxDataElementStore.delete( dataElement );
    }

    @Override
    public void removeMinMaxDataElements( CategoryOptionCombo optionCombo )
    {
        minMaxDataElementStore.delete( optionCombo );
    }

    @Override
    public void removeMinMaxDataElements( Collection<DataElement> dataElements, OrganisationUnit parent )
    {
        minMaxDataElementStore.delete( dataElements, parent );
    }
}
