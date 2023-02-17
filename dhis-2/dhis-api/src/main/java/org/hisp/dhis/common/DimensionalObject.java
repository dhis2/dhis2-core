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
package org.hisp.dhis.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.program.ProgramStage;

/**
 * @author Lars Helge Overland
 */
public interface DimensionalObject
    extends NameableObject, GroupableItem
{
    String DATA_X_DIM_ID = "dx"; // in, de, ds, do

    String DATA_COLLAPSED_DIM_ID = "dy"; // Collapsed event data dimensions

    String CATEGORYOPTIONCOMBO_DIM_ID = "co";

    String ATTRIBUTEOPTIONCOMBO_DIM_ID = "ao";

    String PERIOD_DIM_ID = "pe";

    String ORGUNIT_DIM_ID = "ou";

    String ORGUNIT_GROUP_DIM_ID = "oug"; // Used for org unit target

    String ITEM_DIM_ID = "item";

    String DIMENSION_SEP = "-";

    String LONGITUDE_DIM_ID = "longitude";

    String LATITUDE_DIM_ID = "latitude";

    String DIMENSION_NAME_SEP = ":";

    String PERIOD_FREE_RANGE_SEPARATOR = "_";

    String QUERY_MODS_ID_SEPARATOR = "_";

    String OPTION_SEP = ";";

    String MULTI_CHOICES_OPTION_SEP = ",";

    String ITEM_SEP = "-";

    String PROGRAMSTAGE_SEP = ".";

    String VALUE_COLUMN_NAME = "value";

    String TEXTVALUE_COLUMN_NAME = "textvalue";

    List<String> STATIC_DIMS = List.of(
        LONGITUDE_DIM_ID, LATITUDE_DIM_ID );

    Map<String, String> PRETTY_NAMES = Map.of(
        DATA_X_DIM_ID, "Data",
        CATEGORYOPTIONCOMBO_DIM_ID, "Data details",
        PERIOD_DIM_ID, "Period",
        ORGUNIT_DIM_ID, "Organisation unit" );

    Set<Class<? extends BaseDimensionalObject>> DYNAMIC_DIMENSION_CLASSES = Set.of(
        Category.class, DataElementGroupSet.class, OrganisationUnitGroupSet.class, CategoryOptionGroupSet.class );

    Map<Class<? extends DimensionalObject>, Class<? extends DimensionalItemObject>> DIMENSION_CLASS_ITEM_CLASS_MAP = Map
        .of(
            Category.class, CategoryOption.class,
            DataElementGroupSet.class, DataElementGroup.class,
            OrganisationUnitGroupSet.class, OrganisationUnitGroup.class,
            CategoryOptionGroupSet.class, CategoryOptionGroup.class );

    Set<ValueType> ARITHMETIC_VALUE_TYPES = Set.of(
        ValueType.BOOLEAN, ValueType.TRUE_ONLY, ValueType.NUMBER, ValueType.INTEGER,
        ValueType.INTEGER_POSITIVE, ValueType.INTEGER_NEGATIVE, ValueType.INTEGER_ZERO_OR_POSITIVE,
        ValueType.UNIT_INTERVAL, ValueType.PERCENTAGE );

    /**
     * Gets the dimension identifier.
     */
    String getDimension();

    /**
     * Gets the dimension type.
     */
    DimensionType getDimensionType();

    /**
     * Gets the data dimension type. Can be null. Only applicable for
     * {@link DimensionType#CATEGORY}.
     */
    DataDimensionType getDataDimensionType();

    /**
     * Gets the dimension name, which corresponds to a column in the analytics
     * tables, with fall back to dimension.
     */
    String getDimensionName();

    /**
     * Gets the dimension display name.
     */
    String getDimensionDisplayName();

    /**
     * Returns the value type of the dimension.
     *
     * NOTE: not all dimensional objects have a ValueType, hence this method
     * will return null in such cases.
     */
    ValueType getValueType();

    /**
     * Returns the option set of the dimension, if any.
     */
    OptionSet getOptionSet();

    /**
     * Dimension items.
     */
    List<DimensionalItemObject> getItems();

    /**
     * Indicates whether all available items in this dimension are included.
     */
    boolean isAllItems();

    /**
     * Indicates whether this dimension has any dimension items.
     */
    default boolean hasItems()
    {
        return !getItems().isEmpty();
    }

    /**
     * Gets the legend set.
     */
    LegendSet getLegendSet();

    /**
     * Indicates whether this dimension has a legend set.
     */
    default boolean hasLegendSet()
    {
        return getLegendSet() != null;
    }

    /**
     * Gets the program stage (not persisted).
     */
    ProgramStage getProgramStage();

    /**
     * Indicates whether this dimension has a program stage (not persisted).
     */
    default boolean hasProgramStage()
    {
        return getProgramStage() != null;
    }

    /**
     * Gets the aggregation type.
     */
    AggregationType getAggregationType();

    /**
     * Gets the filter. Contains operator and filter. Applicable for events.
     */
    String getFilter();

    /**
     * Gets the events repetition. Only applicable for events.
     */
    EventRepetition getEventRepetition();

    /**
     * Indicates the analytics type of this dimensional object.
     */
    AnalyticsType getAnalyticsType();

    /**
     * Indicates whether this object should be handled as a data dimension.
     * Persistent property.
     */
    boolean isDataDimension();

    /**
     * Indicates whether this dimension is fixed, meaning that the name of the
     * dimension will be returned as is for all dimension items in the response.
     */
    boolean isFixed();

    /**
     * Returns a unique key representing this dimension.
     */
    String getKey();

    /**
     * Returns dimension item keywords for this dimension.
     */
    DimensionItemKeywords getDimensionItemKeywords();
}
