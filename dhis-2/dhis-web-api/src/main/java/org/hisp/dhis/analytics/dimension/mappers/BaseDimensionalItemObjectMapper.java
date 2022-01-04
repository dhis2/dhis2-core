/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics.dimension.mappers;

import java.util.Set;

import lombok.Getter;

import org.hisp.dhis.analytics.dimension.BaseDimensionMapper;
import org.hisp.dhis.analytics.dimension.DimensionResponse;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Service;

@Service
public class BaseDimensionalItemObjectMapper extends BaseDimensionMapper
{

    @Getter
    private final Set<Class<? extends BaseIdentifiableObject>> supportedClasses = Set.of(
        ProgramIndicator.class,
        DataElement.class,
        TrackedEntityAttribute.class );

    @Override
    public DimensionResponse map( BaseIdentifiableObject dimension )
    {
        BaseDimensionalItemObject baseDimensionalItemObject = (BaseDimensionalItemObject) dimension;
        DimensionResponse responseWithDimensionType = super.map( dimension )
            .withDimensionType( baseDimensionalItemObject.getDimensionItemType().name() );
        if ( dimension instanceof ValueTypedDimensionalItemObject )
        {
            ValueTypedDimensionalItemObject valueTypedDimensionalItemObject = (ValueTypedDimensionalItemObject) dimension;
            return responseWithDimensionType
                .withValueType( valueTypedDimensionalItemObject.getValueType().name() );
        }
        return responseWithDimensionType;
    }

}
