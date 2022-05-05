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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static java.util.function.Predicate.not;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper
interface MetadataIdentifierMapper
{

    @Named( "programToMetadataIdentifier" )
    default MetadataIdentifier fromProgram( String identifier,
        @Context TrackerIdSchemeParams idSchemeParams )
    {
        return idSchemeParams.getProgramIdScheme().toMetadataIdentifier( identifier );
    }

    @Named( "programStageToMetadataIdentifier" )
    default MetadataIdentifier fromProgramStage( String identifier,
        @Context TrackerIdSchemeParams idSchemeParams )
    {
        return idSchemeParams.getProgramStageIdScheme().toMetadataIdentifier( identifier );
    }

    @Named( "orgUnitToMetadataIdentifier" )
    default MetadataIdentifier fromOrgUnit( String identifier,
        @Context TrackerIdSchemeParams idSchemeParams )
    {
        return idSchemeParams.getOrgUnitIdScheme().toMetadataIdentifier( identifier );
    }

    @Named( "attributeOptionComboToMetadataIdentifier" )
    default MetadataIdentifier fromAttributeOptionCombo( String identifier,
        @Context TrackerIdSchemeParams idSchemeParams )
    {
        return idSchemeParams.getCategoryOptionComboIdScheme().toMetadataIdentifier( identifier );
    }

    @Named( "attributeCategoryOptionsToMetadataIdentifier" )
    default Set<MetadataIdentifier> fromAttributeCategoryOptions( String identifiers,
        @Context TrackerIdSchemeParams idSchemeParams )
    {
        if ( identifiers == null || StringUtils.isBlank( identifiers ) )
        {
            return Collections.emptySet();
        }

        return TextUtils.splitToSet( identifiers, TextUtils.SEMICOLON ).stream()
            .map( String::trim )
            .filter( not( String::isEmpty ) )
            .map( id -> idSchemeParams.getCategoryOptionIdScheme().toMetadataIdentifier( id ) )
            .collect( Collectors.toSet() );
    }
}
