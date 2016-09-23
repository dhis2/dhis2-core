package org.hisp.dhis.dataelement;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataset.DataSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Defines the functionality for persisting DataElements and DataElementGroups.
 *
 * @author Torgeir Lorange Ostby
 */
public interface DataElementStore
    extends GenericDimensionalObjectStore<DataElement>
{
    String ID = DataElementStore.class.getName();

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    /**
     * Returns List of DataElements with a given key.
     *
     * @param key the name of the DataElement to return.
     * @return List of DataElements with a given key, or all dataelements if no match.
     */
    List<DataElement> searchDataElementsByName( String key );

    /**
     * Returns all DataElements with types that are possible to aggregate. The
     * types are currently INT and BOOL.
     *
     * @return all DataElements with types that are possible to aggregate.
     */
    List<DataElement> getAggregateableDataElements();

    /**
     * Returns all DataElements with a given aggregation operator.
     *
     * @param aggregationType the aggregation operator of the DataElements
     *                            to return.
     * @return a collection of all DataElements with the given aggregation
     * operator, or an empty collection if no DataElements have the
     * aggregation operator.
     */
    List<DataElement> getDataElementsByAggregationType( AggregationType aggregationType );

    /**
     * Returns all DataElements with the given domain type.
     *
     * @param domainType the domainType.
     * @return all DataElements with the given domainType.
     */
    List<DataElement> getDataElementsByDomainType( DataElementDomain domainType );

    /**
     * Returns all DataElements with the given domain type.
     *
     * @param domainType the domainType.
     * @return all DataElements with the given domainType.
     */
    List<DataElement> getDataElementsByDomainType( DataElementDomain domainType, int first, int max );

    /**
     * Returns all DataElements with the given value types.
     *
     * @param valueTypes The value types.
     * @return all DataElements with the given value types.
     */
    List<DataElement> getDataElementsByValueTypes( Collection<ValueType> valueTypes );

    /**
     * Returns all DataElements with the given value type.
     *
     * @param valueType The value type.
     * @return all DataElements with the given value type.
     */
    List<DataElement> getDataElementsByValueType( ValueType valueType );

    /**
     * Returns all DataElements with the given category combo.
     *
     * @param categoryCombo the DataElementCategoryCombo.
     * @return all DataElements with the given category combo.
     */
    List<DataElement> getDataElementByCategoryCombo( DataElementCategoryCombo categoryCombo );

    /**
     * Returns all DataElements which are associated with one or more
     * DataElementGroupSets.
     *
     * @return all DataElements which are associated with one or more
     * DataElementGroupSets.
     */
    List<DataElement> getDataElementsWithGroupSets();

    /**
     * Defines the given data elements as zero is significant.
     *
     * @param dataElementIds identifiers of data elements where zero is significant.
     */
    void setZeroIsSignificantForDataElements( Collection<Integer> dataElementIds );

    /**
     * Returns all DataElement which zeroIsSignificant property is true or false
     *
     * @param zeroIsSignificant is zeroIsSignificant property
     * @return a collection of all DataElement
     */
    List<DataElement> getDataElementsByZeroIsSignificant( boolean zeroIsSignificant );

    /**
     * Returns all DataElements which are not member of any DataElementGroups.
     *
     * @return all DataElements which are not member of any DataElementGroups.
     */
    List<DataElement> getDataElementsWithoutGroups();

    /**
     * Returns all DataElements which are not assigned to any DataSets.
     *
     * @return all DataElements which are not assigned to any DataSets.
     */
    List<DataElement> getDataElementsWithoutDataSets();

    /**
     * Returns all DataElements which are assigned to at least one DataSet.
     *
     * @return all DataElements which are assigned to at least one DataSet.
     */
    List<DataElement> getDataElementsWithDataSets();

    /**
     * Returns all DataElements which are assigned to any of the given DataSets.
     *
     * @param dataSets the collection of DataSets.
     * @return all DataElements which are assigned to any of the given DataSets.
     */
    List<DataElement> getDataElementsByDataSets( Collection<DataSet> dataSets );

    /**
     * Returns all DataElements which have the given aggregation level assigned.
     *
     * @param aggregationLevel the aggregation level.
     * @return all DataElements which have the given aggregation level assigned.
     */
    List<DataElement> getDataElementsByAggregationLevel( int aggregationLevel );

    /**
     * Returns a mapping of data element uid and associated category option combo
     * uids.
     *
     * @param dataElementUids the uids of the data elements to include in the map.
     * @return a ListMap.
     */
    ListMap<String, String> getDataElementCategoryOptionComboMap( Set<String> dataElementUids );

    List<DataElement> get( DataSet dataSet, String key, Integer max );

    int getCountByDomainType( DataElementDomain domainType );
}
