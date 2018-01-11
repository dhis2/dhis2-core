package org.hisp.dhis.dxf2.configuration;

import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.render.type.ValueTypeRenderingType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import java.util.Set;

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
