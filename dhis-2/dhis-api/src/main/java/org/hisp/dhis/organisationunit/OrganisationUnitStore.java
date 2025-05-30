/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.organisationunit;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.program.Program;

/**
 * Defines methods for persisting OrganisationUnits.
 *
 * @author Kristian Nordal
 */
public interface OrganisationUnitStore
    extends IdentifiableObjectStore<OrganisationUnit>, OrganisationUnitDataIntegrityProvider {

  /**
   * Returns all OrganisationUnits that the user has access to.
   *
   * @param username of the user.
   * @return
   */
  List<String> getOrganisationUnitsUidsByUser(String username);

  /**
   * Returns all search scope OrganisationUnits that the user has access to.
   *
   * @param username of the user.
   * @return
   */
  List<String> getSearchOrganisationUnitsUidsByUser(String username);

  /**
   * Returns all data view scope OrganisationUnits that the user has access to.
   *
   * @param username of the user.
   * @return
   */
  List<String> getDataViewOrganisationUnitsUidsByUser(String username);

  /**
   * Returns all OrganisationUnits by lastUpdated.
   *
   * @param lastUpdated OrganisationUnits from this date
   * @return a list of all OrganisationUnits, or an empty list if there are no OrganisationUnits.
   */
  List<OrganisationUnit> getAllOrganisationUnitsByLastUpdated(Date lastUpdated);

  /**
   * Returns all root OrganisationUnits. A root OrganisationUnit is an OrganisationUnit with no
   * parent/has the parent set to null.
   *
   * @return a list containing all root OrganisationUnits, or an empty list if there are no
   *     OrganisationUnits.
   */
  List<OrganisationUnit> getRootOrganisationUnits();

  /**
   * Returns OrganisationUnits which are associated with the given Program.
   *
   * @param program the {@link Program}.
   * @return
   */
  List<OrganisationUnit> getOrganisationUnitsWithProgram(Program program);

  /**
   * Retrieves organisation units associated with the given program uid.
   *
   * @param programUid the {@link Program} uid.
   * @return a list of {@link OrganisationUnit} found.
   */
  List<OrganisationUnit> getOrganisationUnitsByProgram(String programUid);

  /**
   * Retrieves organisation units associated with the given data set uid.
   *
   * @param dataSetUid the {@link DataSet} uid.
   * @return a list of {@link OrganisationUnit} found.
   */
  List<OrganisationUnit> getOrganisationUnitsByDataSet(String dataSetUid);

  /**
   * Returns the count of OrganisationUnits which are part of the sub-hierarchy of the given parent
   * OrganisationUnit and members of the given object based on the collection of the given
   * collection name.
   *
   * @param parent the parent OrganisationUnit.
   * @param member the member object.
   * @param collectionName the name of the collection.
   * @return the count of member OrganisationUnits.
   */
  Long getOrganisationUnitHierarchyMemberCount(
      OrganisationUnit parent, Object member, String collectionName);

  /**
   * Returns a list of OrganisationUnits based on the given params.
   *
   * @param params the params.
   * @return a list of OrganisationUnits.
   */
  List<OrganisationUnit> getOrganisationUnits(OrganisationUnitQueryParams params);

  /**
   * Retrieves the objects where its coordinate is within the 4 area points. 4 area points are Index
   * 0: Maximum latitude (north edge of box shape) Index 1: Maxium longitude (east edge of box
   * shape) Index 2: Minimum latitude (south edge of box shape) Index 3: Minumum longitude (west
   * edge of box shape)
   *
   * @param box the 4 area points.
   * @return a list of objects.
   */
  List<OrganisationUnit> getWithinCoordinateArea(double[] box);

  /** Updates the <code>path</code> property of all org units. */
  void updatePaths();

  /** Explicitly updates the <code>path</code> property of all org units. */
  void forceUpdatePaths();

  /**
   * Returns the number of organsiation unit levels in the database based on the organisation unit
   * hierarchy.
   *
   * @return number of levels, 0 if no organisation units are present.
   */
  int getMaxLevel();

  /**
   * Sets the geometry field for org units to null.
   *
   * @return then number of affected org units.
   */
  int updateAllOrganisationUnitsGeometryToNull();

  /**
   * Get OrganisationUnits with refs to any of the CategoryOptions passed in
   *
   * @param categoryOptions categoryOptions refs
   * @return OrganisationUnits with refs to any of the CategoryOptions passed in
   */
  List<OrganisationUnit> getByCategoryOption(Collection<String> categoryOptions);
}
