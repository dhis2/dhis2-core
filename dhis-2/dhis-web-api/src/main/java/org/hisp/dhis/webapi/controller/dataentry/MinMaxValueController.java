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
package org.hisp.dhis.webapi.controller.dataentry;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.webapi.controller.datavalue.DataValidator;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.webdomain.datavalue.MinMaxValueDto;
import org.hisp.dhis.webapi.webdomain.datavalue.MinMaxValueQueryParams;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags( "data" )
@RestController
@RequiredArgsConstructor
@RequestMapping( "/dataEntry" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class MinMaxValueController
{
    private final MinMaxDataElementService minMaxValueService;

    private final DataValidator dataValidator;

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_ADD')" )
    @PostMapping( "/minMaxValues" )
    @ResponseStatus( value = HttpStatus.OK )
    public void saveOrUpdateMinMaxValue( @RequestBody MinMaxValueDto valueDto )
    {
        saveOrUpdateMinMaxDataElement( valueDto );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_DELETE')" )
    @DeleteMapping( "/minMaxValues" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void removeMinMaxValue( MinMaxValueQueryParams params )
    {
        removeMinMaxDataElement( params );
    }

    /**
     * Saves or updates a {@link MinMaxDataElement}.
     *
     * @param dto the {@link MinMaxValueDto}.
     */
    private void saveOrUpdateMinMaxDataElement( MinMaxValueDto dto )
    {
        DataElement de = dataValidator.getAndValidateDataElement( dto.getDataElement() );
        OrganisationUnit ou = dataValidator.getAndValidateOrganisationUnit( dto.getOrgUnit() );
        CategoryOptionCombo coc = dataValidator.getAndValidateCategoryOptionCombo( dto.getCategoryOptionCombo() );
        dataValidator.validateMinMaxValues( dto.getMinValue(), dto.getMaxValue() );
        MinMaxDataElement value = minMaxValueService.getMinMaxDataElement( ou, de, coc );

        if ( value != null )
        {
            value.setMin( dto.getMinValue() );
            value.setMax( dto.getMaxValue() );
            value.setGenerated( false );

            minMaxValueService.updateMinMaxDataElement( value );
        }
        else
        {
            value = new MinMaxDataElement( de, ou, coc, dto.getMinValue(), dto.getMaxValue() );

            minMaxValueService.addMinMaxDataElement( value );
        }
    }

    /**
     * Removes a {@link MinMaxDataElement}.
     *
     * @param params the {@link MinMaxValueQueryParams}.
     */
    private void removeMinMaxDataElement( MinMaxValueQueryParams params )
    {
        DataElement de = dataValidator.getAndValidateDataElement( params.getDe() );
        OrganisationUnit ou = dataValidator.getAndValidateOrganisationUnit( params.getOu() );
        CategoryOptionCombo coc = dataValidator.getAndValidateCategoryOptionCombo( params.getCo() );
        MinMaxDataElement value = minMaxValueService.getMinMaxDataElement( ou, de, coc );

        if ( value != null )
        {
            minMaxValueService.deleteMinMaxDataElement( value );
        }
    }
}
