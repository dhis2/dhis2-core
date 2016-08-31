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

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.comparator.ObjectStringValueComparator;
import org.hisp.dhis.dataelement.DataElementOperand;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class DimensionalObjectUtils
{
    private static final Pattern INT_PATTERN = Pattern.compile( "^(0|-?[1-9]\\d*)$" );
    private static final Pattern DIMENSIONAL_OPERAND_PATTERN = Pattern.compile( "([a-zA-Z]\\w{10})\\.([a-zA-Z]\\w{10})" );
    
    public static final String TITLE_ITEM_SEP = ", ";

    public static List<DimensionalObject> getCopies( List<DimensionalObject> dimensions )
    {
        List<DimensionalObject> list = new ArrayList<>();
        
        if ( dimensions != null )
        {
            for ( DimensionalObject dimension : dimensions )
            {
                DimensionalObject object = ((BaseDimensionalObject) dimension).instance();
                list.add( object );
            }
        }
        
        return list;
    }
    
    /**
     * Creates a list of dimension identifiers based on the given list of 
     * DimensionalObjects.
     * 
     * @param dimensions the list of DimensionalObjects.
     * @return list of dimension identifiers.
     */
    public static List<String> getDimensions( List<DimensionalObject> dimensions )
    {
        List<String> dims = new ArrayList<>();
        
        if ( dimensions != null )
        {
            for ( DimensionalObject dimension : dimensions )
            {
                dims.add( dimension.getDimension() );
            }
        }
        
        return dims;
    }

    /**
     * Creates a two-dimensional array of dimension items based on the list of
     * DimensionalObjects. I.e. the list of items of each DimensionalObject is
     * converted to an array and inserted into the outer array in the same order.
     * 
     * @param dimensions the list of DimensionalObjects.
     * @return a two-dimensional array of NameableObjects.
     */
    public static NameableObject[][] getItemArray( List<DimensionalObject> dimensions )
    {
        List<NameableObject[]> arrays = new ArrayList<>();
        
        for ( DimensionalObject dimension : dimensions )
        {
            arrays.add( dimension.getItems().toArray( new NameableObject[0] ) );
        }
        
        return arrays.toArray( new NameableObject[0][] );
    }
    
    /**
     * Creates a map based on the given array of elements, where each pair of
     * elements are put on them map as a key-value pair.
     * 
     * @param elements the elements to put on the map.
     * @return a map.
     */
    @SafeVarargs
    public static final <T> Map<T, T> asMap( final T... elements )
    {
        Map<T, T> map = new HashMap<>();
        
        if ( elements != null && ( elements.length % 2 == 0 ) )
        {
            for ( int i = 0; i < elements.length; i += 2 )
            {
                map.put( elements[i], elements[i+1] );
            }
        }
        
        return map;
    }

    /**
     * Retrieves the dimension name from the given string. Returns the part of
     * the string preceding the dimension name separator, or the whole string if
     * the separator is not present.
     */
    public static String getDimensionFromParam( String param )
    {
        if ( param == null )
        {
            return null;
        }
        
        return param.split( DIMENSION_NAME_SEP ).length > 0 ? param.split( DIMENSION_NAME_SEP )[0] : param;
    }
    
    /**
     * Retrieves the dimension options from the given string. Looks for the part
     * succeeding the dimension name separator, if exists, splits the string part
     * on the option separator and returns the resulting values. If the dimension
     * name separator does not exist an empty list is returned, indicating that
     * all dimension options should be used.
     */
    public static List<String> getDimensionItemsFromParam( String param )
    {
        if ( param == null )
        {
            return null;
        }
        
        if ( param.split( DIMENSION_NAME_SEP ).length > 1 )
        {
            return new ArrayList<>( Arrays.asList( param.split( DIMENSION_NAME_SEP )[1].split( OPTION_SEP ) ) );
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Splits the given string on the ; character and returns the items in a 
     * list. Returns null if the given string is null.
     */
    public static List<String> getItemsFromParam( String param )
    {
        if ( param == null )
        {
            return null;
        }
        
        return new ArrayList<>( Arrays.asList( param.split( OPTION_SEP ) ) );
    }

    /**
     * Indicates whether at least one of the given dimenions has at least one
     * item.
     */
    public static boolean anyDimensionHasItems( Collection<DimensionalObject> dimensions )
    {
        if ( dimensions == null || dimensions.isEmpty() )
        {
            return false;
        }
        
        for ( DimensionalObject dim : dimensions )
        {
            if ( dim.hasItems() )
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Retrieves the level from a level parameter string, which is on the format
     * LEVEL-<level>-<item> .
     */
    public static int getLevelFromLevelParam( String param )
    {
        if ( param == null )   
        {
            return 0;
        }
        
        String[] split = param.split( ITEM_SEP );
        
        if ( split.length > 1 && INT_PATTERN.matcher( split[1] ).matches() )
        {
            return Integer.parseInt( split[1] );
        }
        
        return 0;
    }
    
    /**
     * Retrieves the uid from an org unit group parameter string, which is on
     * the format OU_GROUP-<uid> .
     */
    public static String getUidFromGroupParam( String param )
    {
        if ( param == null )
        {
            return null;
        }
        
        String[] split = param.split( ITEM_SEP );
        
        if ( split.length > 1 && split[1] != null )
        {
            return String.valueOf( split[1] );
        }
        
        return null;
    }
    
    /**
     * Sets items on the given dimension based on the unique values of the matching 
     * column in the given grid. Items are BaseNameableObjects where the name, 
     * code and short name properties are set to the column value. The dimension
     * analytics type must be equal to EVENT.
     * 
     * @param dimension the dimension.
     * @param naForNull indicates whether a [n/a] string should be used as
     *        replacement for null values.
     * @param grid the grid with data values.
     */
    public static void setDimensionItemsForFilters( DimensionalObject dimension, Grid grid, boolean naForNull )
    {
        if ( dimension == null || grid == null || !AnalyticsType.EVENT.equals( dimension.getAnalyticsType() ) )
        {
            return;
        }
            
        BaseDimensionalObject dim = (BaseDimensionalObject) dimension;
        
        List<String> filterItems = dim.getFilterItemsAsList();
        
        List<Object> values = new ArrayList<>( grid.getUniqueValues( dim.getDimension() ) );
        
        Collections.sort( values, ObjectStringValueComparator.INSTANCE );
        
        // Use order of items in filter if specified
        
        List<?> itemList = filterItems != null ? ListUtils.retainAll( filterItems, values ) : values;
                
        List<NameableObject> items = NameableObjectUtils.getNameableObjects( itemList, naForNull );
        
        dim.setItems( items );
    }
        
    /**
     * Accepts filter strings on the format:
     * </p>
     * <code>operator:filter:operator:filter</code>
     * </p>
     * and returns a pretty print version on the format:
     * </p>
     * <code>operator filter, operator filter</code>
     * 
     * @param filter the filter.
     * @return a pretty print version of the filter.
     */
    public static String getPrettyFilter( String filter )
    {
        if ( filter == null || !filter.contains( DIMENSION_NAME_SEP ) )
        {
            return null;
        }
        
        List<String> filterItems = new ArrayList<>();
        
        String[] split = filter.split( DIMENSION_NAME_SEP );

        for ( int i = 0; i < split.length; i += 2 )
        {
            QueryOperator operator = QueryOperator.fromString( split[i] );
            String value = split[i+1];
            
            if ( operator != null )
            {
                boolean ignoreOperator = ( QueryOperator.LIKE.equals( operator ) || QueryOperator.IN.equals( operator ) );
                
                value = value.replaceAll( QueryFilter.OPTION_SEP, TITLE_ITEM_SEP );
                
                filterItems.add( ( ignoreOperator ? StringUtils.EMPTY : ( operator.getValue() + " " ) ) + value );
            }
        }
        
        return StringUtils.join( filterItems, TITLE_ITEM_SEP );
    }

    /**
     * Indicates whether the given string is a valid full operand expression.
     * 
     * @param expression the expression.
     * @return true if valid full operand expression, false if not.
     */
    public static boolean isValidDimensionalOperand( String expression )
    {
        return expression != null && DIMENSIONAL_OPERAND_PATTERN.matcher( expression ).matches();
    }

    /**
     * Gets a set of unique data elements based on the given collection of operands.
     * 
     * @param operands the collection of operands.
     * @return a set of data elements.
     */
    public static Set<NameableObject> getDataElements( Collection<DataElementOperand> operands )
    {
        Set<NameableObject> set = Sets.newHashSet();
        
        for ( DataElementOperand operand : operands )
        {
            set.add( operand.getDataElement() );
        }
        
        return set;
    }
    
    /**
     * Gets a set of unique category option combos based on the given collection
     * of operands.
     * 
     * @param operands the collection of operands.
     * @return a set of category option combos.
     */
    public static Set<NameableObject> getCategoryOptionCombos( Collection<DataElementOperand> operands )
    {
        Set<NameableObject> set = Sets.newHashSet();
        
        for ( DataElementOperand operand : operands )
        {
            set.add( operand.getCategoryOptionCombo() );
        }
        
        return set;
    }
}
