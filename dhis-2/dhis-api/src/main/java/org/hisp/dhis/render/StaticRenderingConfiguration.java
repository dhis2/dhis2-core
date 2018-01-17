package org.hisp.dhis.render;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.render.type.ValueTypeRenderingType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import java.util.Set;

/**
 * This class represents the constraint rules enforced by the application on DataElement, TrackedEntityAttribute, ValueType,
 * OptionSet and RenderTypes.
 */
public class StaticRenderingConfiguration
{
    public static final Set<ObjectValueTypeRenderingOption> RENDERING_OPTIONS_MAPPING = ImmutableSet.<ObjectValueTypeRenderingOption>builder()

        // Boolean
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.TRUE_ONLY, false,
            ValueTypeRenderingType.BOOLEAN_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.BOOLEAN, false,
            ValueTypeRenderingType.BOOLEAN_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.TRUE_ONLY, false,
            ValueTypeRenderingType.BOOLEAN_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.BOOLEAN, false,
            ValueTypeRenderingType.BOOLEAN_TYPES ) )

        // OptionSet
        .add( new ObjectValueTypeRenderingOption( DataElement.class, null, true,
            ValueTypeRenderingType.OPTION_SET_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, null, true,
            ValueTypeRenderingType.OPTION_SET_TYPES ) )

        // Numeric
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.INTEGER, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.INTEGER_POSITIVE, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.INTEGER_NEGATIVE, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.INTEGER_ZERO_OR_POSITIVE, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.NUMBER, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.UNIT_INTERVAL, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( DataElement.class, ValueType.PERCENTAGE, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.INTEGER, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.INTEGER_POSITIVE, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.INTEGER_NEGATIVE, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.INTEGER_ZERO_OR_POSITIVE, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.NUMBER, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.UNIT_INTERVAL, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )
        .add( new ObjectValueTypeRenderingOption( TrackedEntityAttribute.class, ValueType.PERCENTAGE, false,
            ValueTypeRenderingType.NUMERIC_TYPES ) )

        .build();

}
