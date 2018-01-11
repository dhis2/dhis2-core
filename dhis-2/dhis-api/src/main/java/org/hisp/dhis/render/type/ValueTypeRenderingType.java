package org.hisp.dhis.render.type;

import com.google.common.collect.ImmutableSet;

public enum ValueTypeRenderingType
{
    DEFAULT,
    DROPDOWN,
    VERTICAL_RADIOBUTTONS,
    HORIZONTAL_RADIOBUTTONS,
    VERTICAL_CHECKBOXES,
    HORIZONTAL_CHECKBOXES,
    SHARED_HEADER_RADIOBUTTONS,
    ICONS_AS_BUTTONS,
    SPINNER,
    ICON,
    TOGGLE,
    VALUE,
    SLIDER,
    LINEAR_SCALE;

    public static final ImmutableSet<ValueTypeRenderingType> OPTION_SET_TYPES = ImmutableSet
        .of( DEFAULT, DROPDOWN, VERTICAL_RADIOBUTTONS, HORIZONTAL_RADIOBUTTONS, VERTICAL_CHECKBOXES,
            HORIZONTAL_CHECKBOXES, SHARED_HEADER_RADIOBUTTONS, ICONS_AS_BUTTONS, SPINNER, ICON );

    public static final ImmutableSet<ValueTypeRenderingType> BOOLEAN_TYPES = ImmutableSet
        .of( DEFAULT, VERTICAL_RADIOBUTTONS, HORIZONTAL_RADIOBUTTONS, VERTICAL_CHECKBOXES,
            HORIZONTAL_CHECKBOXES, TOGGLE );

    public static final ImmutableSet<ValueTypeRenderingType> NUMERIC_TYPES = ImmutableSet
        .of( DEFAULT, VALUE, SLIDER, LINEAR_SCALE, SPINNER );
}
