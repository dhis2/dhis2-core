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
package org.hisp.dhis.webapi.dimension.mappers;

import java.util.Optional;
import java.util.Set;

import lombok.Getter;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.webapi.dimension.DimensionResponse;
import org.springframework.stereotype.Service;

@Service
public class ProgramStageDataElementMapper extends BaseDimensionalItemObjectMapper
{
    @Getter
    private final Set<Class<? extends BaseIdentifiableObject>> supportedClasses = Set.of(
        ProgramStageDataElement.class );

    @Override
    public DimensionResponse map( PrefixedDimension prefixedDimension, String prefix )
    {
        ProgramStageDataElement programStageDataElement = (ProgramStageDataElement) prefixedDimension.getItem();

        PrefixedDimension dataElementPrefixedDimension = PrefixedDimension.builder()
            .item( programStageDataElement.getDataElement() )
            .build();

        final DimensionResponse mapped = super.map( dataElementPrefixedDimension, prefix )
            .withValueType( programStageDataElement.getDataElement().getValueType().name() );

        return Optional.of( programStageDataElement )
            .map( ProgramStageDataElement::getDataElement )
            .map( DataElement::getOptionSet )
            .map( BaseIdentifiableObject::getUid )
            .map( mapped::withOptionSet )
            .orElse( mapped );
    }

}
