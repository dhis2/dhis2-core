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
package org.hisp.dhis.dataelement;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.hierarchy.HierarchyViolationException;
import org.hisp.dhis.period.PeriodType;

/**
 * Defines service functionality for DataElements and DataElementGroups.
 *
 * @author Kristian Nordal
 */
public interface DataElementService {
  // -------------------------------------------------------------------------
  // DataElement
  // -------------------------------------------------------------------------

  /**
   * Adds a DataElement.
   *
   * @param dataElement the DataElement to add.
   * @return a generated unique id of the added DataElement.
   */
  long addDataElement(DataElement dataElement);

  /**
   * Updates a DataElement.
   *
   * @param dataElement the DataElement to update.
   */
  void updateDataElement(DataElement dataElement);

  /**
   * Validate the data consistency of the provided data element.
   *
   * @param dataElement the element to check for validity
   * @throws IllegalQueryException when the provided data element is not valid
   */
  void validateDateElement(DataElement dataElement) throws IllegalQueryException;

  /**
   * Deletes a DataElement. The DataElement is also removed from any DataElementGroups it is a
   * member of. It is not possible to delete a DataElement with children.
   *
   * @param dataElement the DataElement to delete.
   * @throws HierarchyViolationException if the DataElement has children.
   */
  void deleteDataElement(DataElement dataElement);

  /**
   * Returns a DataElement.
   *
   * @param id the id of the DataElement to return.
   * @return the DataElement with the given id, or null if no match.
   */
  DataElement getDataElement(long id);

  /**
   * Returns the DataElement with the given UID.
   *
   * @param uid the UID.
   * @return the DataElement with the given UID, or null if no match.
   */
  DataElement getDataElement(String uid);

  /**
   * Returns the DataElement with the given code.
   *
   * @param code the code.
   * @return the DataElement with the given code, or null if no match.
   */
  DataElement getDataElementByCode(String code);

  /**
   * Returns all DataElements.
   *
   * @return a list of all DataElements, or an empty list if there are no DataElements.
   */
  List<DataElement> getAllDataElements();

  /**
   * Returns all DataElements of a given type.
   *
   * @param valueType the value type restriction
   * @return a list of all DataElements with the given value type, or an empty list if there are no
   *     DataElements.
   */
  List<DataElement> getAllDataElementsByValueType(ValueType valueType);

  /**
   * Returns all DataElements with the given domain type.
   *
   * @param domainType the DataElementDomainType.
   * @return all DataElements with the given domainType.
   */
  List<DataElement> getDataElementsByDomainType(DataElementDomain domainType);

  /**
   * Returns the DataElements with the given PeriodType.
   *
   * @param periodType the PeriodType.
   * @return a list of DataElements.
   */
  List<DataElement> getDataElementsByPeriodType(PeriodType periodType);

  /**
   * Returns all DataElements with the given category combo.
   *
   * @param categoryCombo the CategoryCombo.
   * @return all DataElements with the given category combo.
   */
  List<DataElement> getDataElementByCategoryCombo(CategoryCombo categoryCombo);

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
  List<DataElement> getDataElementsByAggregationLevel(int aggregationLevel);

  // -------------------------------------------------------------------------
  // DataElementGroup
  // -------------------------------------------------------------------------

  /**
   * Adds a DataElementGroup.
   *
   * @param dataElementGroup the DataElementGroup to add.
   * @return a generated unique id of the added DataElementGroup.
   */
  long addDataElementGroup(DataElementGroup dataElementGroup);

  /**
   * Updates a DataElementGroup.
   *
   * @param dataElementGroup the DataElementGroup to update.
   */
  void updateDataElementGroup(DataElementGroup dataElementGroup);

  /**
   * Deletes a DataElementGroup.
   *
   * @param dataElementGroup the DataElementGroup to delete.
   */
  void deleteDataElementGroup(DataElementGroup dataElementGroup);

  /**
   * Returns a DataElementGroup.
   *
   * @param id the id of the DataElementGroup to return.
   * @return the DataElementGroup with the given id, or null if no match.
   */
  DataElementGroup getDataElementGroup(long id);

  /**
   * Returns the data element groups with the given uids.
   *
   * @param uids the uid collection.
   * @return the data element groups with the given uids.
   */
  List<DataElementGroup> getDataElementGroupsByUid(@Nonnull Collection<String> uids);

  /**
   * Returns the DataElementGroup with the given UID.
   *
   * @param uid the UID of the DataElementGroup to return.
   * @return the DataElementGroup with the given UID, or null if no match.
   */
  DataElementGroup getDataElementGroup(String uid);

  /**
   * Returns a DataElementGroup with a given name.
   *
   * @param name the name of the DataElementGroup to return.
   * @return the DataElementGroup with the given name, or null if no match.
   */
  DataElementGroup getDataElementGroupByName(String name);

  /**
   * Returns all DataElementGroups.
   *
   * @return a collection of all DataElementGroups, or an empty collection if no DataElementGroups
   *     exist.
   */
  List<DataElementGroup> getAllDataElementGroups();

  /**
   * Returns all DataElements which zeroIsSignificant property is true or false.
   *
   * @param zeroIsSignificant whether zero is significant is true for this query.
   * @return a collection of DataElements.
   */
  List<DataElement> getDataElementsByZeroIsSignificant(boolean zeroIsSignificant);

  // -------------------------------------------------------------------------
  // DataElementGroupSet
  // -------------------------------------------------------------------------

  long addDataElementGroupSet(DataElementGroupSet groupSet);

  void updateDataElementGroupSet(DataElementGroupSet groupSet);

  void deleteDataElementGroupSet(DataElementGroupSet groupSet);

  DataElementGroupSet getDataElementGroupSet(long id);

  DataElementGroupSet getDataElementGroupSet(String uid);

  DataElementGroupSet getDataElementGroupSetByName(String name);

  List<DataElementGroupSet> getAllDataElementGroupSets();

  List<DataElement> getByAttributeAndValue(Attribute attribute, String value);

  List<DataElement> getByAttribute(Attribute attribute);

  DataElement getByUniqueAttributeValue(Attribute attribute, String value);
}
