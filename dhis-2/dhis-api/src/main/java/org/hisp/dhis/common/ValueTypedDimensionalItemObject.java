package org.hisp.dhis.common;

import org.hisp.dhis.option.OptionSet;

/**
 * @author Henning Håkonsen
 */
public interface ValueTypedDimensionalItemObject
    extends DimensionalItemObject
{
    boolean hasOptionSet();

    OptionSet getOptionSet();

    ValueType getValueType();
}
