package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
* @author Lars Helge Overland
*/
public interface DimensionalObject
    extends NameableObject
{
    String DATA_X_DIM_ID = "dx"; // in, de, ds, do
    String DATA_COLLAPSED_DIM_ID = "dy"; // Collapsed event data dimensions
    String CATEGORYOPTIONCOMBO_DIM_ID = "co";
    String ATTRIBUTEOPTIONCOMBO_DIM_ID = "ao";
    String PERIOD_DIM_ID = "pe";
    String ORGUNIT_DIM_ID = "ou";
    String ORGUNIT_GROUP_DIM_ID = "oug"; // Used for org unit target
    String ITEM_DIM_ID = "item";

    String OU_MODE_SELECTED = "selected"; //TODO replace with OrganisationUnitSelectionMode
    String OU_MODE_CHILDREN = "children";
    String OU_MODE_DESCENDANTS = "descendants";
    String OU_MODE_ALL = "all";
    
    String DIMENSION_SEP = "-";

    String LONGITUDE_DIM_ID = "longitude";
    String LATITUDE_DIM_ID = "latitude";

    String DIMENSION_NAME_SEP = ":";
    String OPTION_SEP = ";";
    String ITEM_SEP = "-";
        
    List<String> STATIC_DIMS = Arrays.asList( 
        LONGITUDE_DIM_ID, LATITUDE_DIM_ID );
    
    Map<String, String> PRETTY_NAMES = DimensionalObjectUtils.asMap( 
        DATA_X_DIM_ID, "Data",
        CATEGORYOPTIONCOMBO_DIM_ID, "Data details",
        PERIOD_DIM_ID, "Period",
        ORGUNIT_DIM_ID, "Organisation unit" );
    
    Map<DimensionType, Class<? extends DimensionalObject>> DYNAMIC_DIMENSION_TYPE_CLASS_MAP = ImmutableMap.<DimensionType, Class<? extends DimensionalObject>>builder().
        put( DimensionType.CATEGORY, DataElementCategory.class ).
        put( DimensionType.DATAELEMENT_GROUPSET, DataElementGroupSet.class ).
        put( DimensionType.ORGANISATIONUNIT_GROUPSET, OrganisationUnitGroupSet.class ).
        put( DimensionType.CATEGORYOPTION_GROUPSET, CategoryOptionGroupSet.class ).
        put( DimensionType.PROGRAM_ATTRIBUTE, TrackedEntityAttribute.class ).
        put( DimensionType.PROGRAM_DATAELEMENT, DataElement.class ).build();              

    Map<Class<? extends DimensionalObject>, Class<? extends NameableObject>> DIMENSION_CLASS_ITEM_CLASS_MAP = ImmutableMap.<Class<? extends DimensionalObject>, Class<? extends NameableObject>>builder().
        put( DataElementCategory.class, DataElementCategoryOption.class ).
        put( DataElementGroupSet.class, DataElementGroup.class ).
        put( OrganisationUnitGroupSet.class, OrganisationUnitGroup.class ).
        put( CategoryOptionGroupSet.class, CategoryOptionGroup.class ).build();
    
    Set<ValueType> ARITHMETIC_VALUE_TYPES = Sets.newHashSet(
        ValueType.BOOLEAN, ValueType.TRUE_ONLY, ValueType.NUMBER, ValueType.INTEGER, 
        ValueType.INTEGER_POSITIVE, ValueType.INTEGER_NEGATIVE, ValueType.INTEGER_ZERO_OR_POSITIVE, 
        ValueType.UNIT_INTERVAL, ValueType.PERCENTAGE
    );
    
    /**
     * Gets the dimension identifier.
     */
    String getDimension();
    
    /**
     * Gets the dimension type.
     */
    DimensionType getDimensionType();
    
    /**
     * Gets the dimension name, which corresponds to a column in the analytics
     * tables, with fall back to dimension.
     */
    String getDimensionName();
     
    /**
     * Dimension items.
     */
    List<NameableObject> getItems();

    /**
     * Indicates whether all available items in this dimension are included.
     */
    boolean isAllItems();
    
    /**
     * Indicates whether this dimension has any dimension items.
     */
    boolean hasItems();
    
    /**
     * Gets the legend set.
     */
    LegendSet getLegendSet();

    /**
     * Indicates whether this dimension has a legend set.
     */
    boolean hasLegendSet();
    
    /**
     * Gets the aggregation type.
     */
    AggregationType getAggregationType();
    
    /**
     * Gets the filter. Contains operator and filter. Applicable for events.
     */
    String getFilter();

    /**
     * Indicates the analytics type of this dimensional object.
     */
    AnalyticsType getAnalyticsType();
    
    /**
     * Indicates whether this object should be handled as a data dimension. 
     * Persistent property.
     */
    boolean isDataDimension();
}
