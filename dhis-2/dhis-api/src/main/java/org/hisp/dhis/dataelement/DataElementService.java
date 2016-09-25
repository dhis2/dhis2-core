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
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.hierarchy.HierarchyViolationException;
import org.hisp.dhis.period.PeriodType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines service functionality for DataElements and DataElementGroups.
 *
 * @author Kristian Nordal
 * @version $Id: DataElementService.java 6289 2008-11-14 17:53:24Z larshelg $
 */
public interface DataElementService
{
    String ID = DataElementService.class.getName();

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    /**
     * Adds a DataElement.
     *
     * @param dataElement the DataElement to add.
     * @return a generated unique id of the added DataElement.
     */
    int addDataElement( DataElement dataElement );

    /**
     * Updates a DataElement.
     *
     * @param dataElement the DataElement to update.
     */
    void updateDataElement( DataElement dataElement );

    /**
     * Deletes a DataElement. The DataElement is also removed from any
     * DataElementGroups it is a member of. It is not possible to delete a
     * DataElement with children.
     *
     * @param dataElement the DataElement to delete.
     * @throws HierarchyViolationException if the DataElement has children.
     */
    void deleteDataElement( DataElement dataElement );

    /**
     * Returns a DataElement.
     *
     * @param id the id of the DataElement to return.
     * @return the DataElement with the given id, or null if no match.
     */
    DataElement getDataElement( int id );

    /**
     * Returns the DataElement with the given UID.
     *
     * @param uid the UID.
     * @return the DataElement with the given UID, or null if no match.
     */
    DataElement getDataElement( String uid );

    /**
     * Returns the DataElement with the given code.
     *
     * @param code the code.
     * @return the DataElement with the given code, or null if no match.
     */
    DataElement getDataElementByCode( String code );

    /**
     * Returns List of DataElements with a given key.
     *
     * @param key the name of the DataElement to return.
     * @return List of DataElements with a given key, or all dataelements if no
     * match.
     */
    List<DataElement> searchDataElementsByName( String key );

    /**
     * Returns a DataElement with a given short name.
     *
     * @param shortName the short name of the DataElement to return.
     * @return the DataElement with the given short name, or null if no match.
     */
    DataElement getDataElementByShortName( String shortName );
    
    /**
     * Returns all DataElements.
     *
     * @return a list of all DataElements, or an empty list if there
     * are no DataElements.
     */
    List<DataElement> getAllDataElements();

    /**
     * Returns all DataElements with corresponding identifiers. Returns all
     * DataElements if the given argument is null.
     *
     * @param uids the collection of uids.
     * @return a list of DataElements.
     */
    List<DataElement> getDataElementsByUid( Collection<String> uids );

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
     * @param aggregationType the aggregation type of the DataElements
     *                        to return.
     * @return a list of all DataElements with the given aggregation
     * operator, or an empty collection if no DataElements have the
     * aggregation operator.
     */
    List<DataElement> getDataElementsByAggregationType( AggregationType aggregationType );

    /**
     * Returns all DataElements with the given domain type.
     *
     * @param domainType the DataElementDomainType.
     * @return all DataElements with the given domainType.
     */
    List<DataElement> getDataElementsByDomainType( DataElementDomain domainType );

    /**
     * Returns all DataElements with the given domain type.
     *
     * @param domainType the DataElementDomainType.
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
     * Returns all DataElements with the given type.
     *
     * @param valueType The value type.
     * @return all DataElements with the given value type.
     */
    List<DataElement> getDataElementsByValueType( ValueType valueType );

    /**
     * Returns the DataElements with the given PeriodType.
     *
     * @param periodType the PeriodType.
     * @return a list of DataElements.
     */
    List<DataElement> getDataElementsByPeriodType( PeriodType periodType );

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
     * Returns all DataElements which have the given aggregation level assigned.
     *
     * @param aggregationLevel the aggregation level.
     * @return all DataElements which have the given aggregation level assigned.
     */
    List<DataElement> getDataElementsByAggregationLevel( int aggregationLevel );

    List<DataElement> getDataElementsLikeName( String name );

    /**
     * Returns a mapping of data element uid and associated category option combo
     * uids.
     *
     * @param dataElementUids the uids of the data elements to include in the map.
     * @return a ListMap.
     */
    ListMap<String, String> getDataElementCategoryOptionComboMap( Set<String> dataElementUids );

    Map<String, Integer> getDataElementUidIdMap();
    
    // -------------------------------------------------------------------------
    // DataElementGroup
    // -------------------------------------------------------------------------

    /**
     * Adds a DataElementGroup.
     *
     * @param dataElementGroup the DataElementGroup to add.
     * @return a generated unique id of the added DataElementGroup.
     */
    int addDataElementGroup( DataElementGroup dataElementGroup );

    /**
     * Updates a DataElementGroup.
     *
     * @param dataElementGroup the DataElementGroup to update.
     */
    void updateDataElementGroup( DataElementGroup dataElementGroup );

    /**
     * Deletes a DataElementGroup.
     *
     * @param dataElementGroup the DataElementGroup to delete.
     */
    void deleteDataElementGroup( DataElementGroup dataElementGroup );

    /**
     * Returns a DataElementGroup.
     *
     * @param id the id of the DataElementGroup to return.
     * @return the DataElementGroup with the given id, or null if no match.
     */
    DataElementGroup getDataElementGroup( int id );

    /**
     * Returns the data element groups with the given uids.
     *
     * @param uids the uid collection.
     * @return the data element groups with the given uids.
     */
    List<DataElementGroup> getDataElementGroupsByUid( Collection<String> uids );

    /**
     * Returns the DataElementGroup with the given UID.
     *
     * @param id the UID of the DataElementGroup to return.
     * @return the DataElementGroup with the given UID, or null if no match.
     */
    DataElementGroup getDataElementGroup( String uid );

    /**
     * Returns a DataElementGroup with a given name.
     *
     * @param name the name of the DataElementGroup to return.
     * @return the DataElementGroup with the given name, or null if no match.
     */
    DataElementGroup getDataElementGroupByName( String name );

    /**
     * Returns all DataElementGroups.
     *
     * @return a collection of all DataElementGroups, or an empty collection if
     * no DataElementGroups exist.
     */
    List<DataElementGroup> getAllDataElementGroups();


    /**
     * Returns a DataElementGroup with a given short name.
     *
     * @param shortName the short name of the DataElementGroup to return.
     * @return the DataElementGroup with the given short name, or null if no match.
     */
    DataElementGroup getDataElementGroupByShortName( String shortName );

    /**
     * Returns a DataElementGroup with a given code.
     *
     * @param code the shortName of the DataElementGroup to return.
     * @return the DataElementGroup with the given code, or null if no match.
     */
    DataElementGroup getDataElementGroupByCode( String code );

    /**
     * Returns data elements with identifier in the given id.
     *
     * @param groupId is the id of data element group.
     * @return data elements with identifier in the given id.
     */
    Set<DataElement> getDataElementsByGroupId( int groupId );

    /**
     * Defines the given data elements as zero is significant. All other data
     * elements are defined as zero is in-significant.
     *
     * @param dataElementIds identifiers of data elements where zero is
     *                       significant.
     */
    void setZeroIsSignificantForDataElements( Collection<Integer> dataElementIds );

    /**
     * Returns all DataElement which zeroIsSignificant property is true or false
     *
     * @param list is zeroIsSignificant property
     * @return a collection of all DataElement
     */
    List<DataElement> getDataElementsByZeroIsSignificant( boolean zeroIsSignificant );

    /**
     * Returns all DataElement which zeroIsSignificant property is true or false
     *
     * @param zeroIsSignificant is zeroIsSignificant property
     * @param dataElementGroup  is group contain data elements
     * @return a set of data elements.
     */
    Set<DataElement> getDataElementsByZeroIsSignificantAndGroup( boolean zeroIsSignificant,
        DataElementGroup dataElementGroup );

    // -------------------------------------------------------------------------
    // DataElementGroupSet
    // -------------------------------------------------------------------------

    int addDataElementGroupSet( DataElementGroupSet groupSet );

    void updateDataElementGroupSet( DataElementGroupSet groupSet );

    void deleteDataElementGroupSet( DataElementGroupSet groupSet );

    DataElementGroupSet getDataElementGroupSet( int id );

    DataElementGroupSet getDataElementGroupSet( String uid );

    DataElementGroupSet getDataElementGroupSetByName( String name );

    List<DataElementGroupSet> getCompulsoryDataElementGroupSetsWithMembers();

    List<DataElementGroupSet> getAllDataElementGroupSets();

    List<DataElementGroupSet> getDataElementGroupSetsByUid( Collection<String> uids );
}
