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
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface DimensionService {
  /**
   * Returns items which the current user has data read access to.
   *
   * @param uid the dimension identifier.
   * @return a list of {@link DimensionalItemObject}, empty if none.
   */
  List<DimensionalItemObject> getCanReadDimensionItems(String uid);

  <T extends IdentifiableObject> List<T> getCanReadObjects(List<T> objects);

  <T extends IdentifiableObject> List<T> getCanReadObjects(User user, List<T> objects);

  DimensionType getDimensionType(String uid);

  List<DimensionalObject> getAllDimensions();

  List<DimensionalObject> getDimensionConstraints();

  DimensionalObject getDimensionalObjectCopy(String uid, boolean filterCanRead);

  void mergeAnalyticalObject(BaseAnalyticalObject object);

  void mergeEventAnalyticalObject(EventAnalyticalObject object);

  /**
   * Gets a dimension item object which are among the data dimension item objects. The composite
   * dimensional items themselves will be transient and the associated objects will be persistent.
   *
   * @param dimensionItem the dimension item identifier.
   * @return a dimensional item object.
   */
  DimensionalItemObject getDataDimensionalItemObject(String dimensionItem);

  /**
   * Gets a dimension item object which are among the data dimension item objects. The composite
   * dimensional items will be transient and the associated objects will be persistent.
   *
   * @param idScheme the idScheme to identify the item.
   * @param dimensionItem the dimension item identifier.
   * @return a dimensional item object.
   */
  DimensionalItemObject getDataDimensionalItemObject(IdScheme idScheme, String dimensionItem);

  /**
   * Gets a dimension item object from a dimension item id.
   *
   * @param dimensionalItemId the dimension item identifier.
   * @return a dimensional item object.
   */
  DimensionalItemObject getDataDimensionalItemObject(DimensionalItemId dimensionalItemId);

  /**
   * Gets a map from dimension item ids to their dimension item objects.
   *
   * @param itemIds a set of ids of the dimension item objects to get.
   * @return a map from the item ids to the dimension item objects.
   */
  Map<DimensionalItemId, DimensionalItemObject> getDataDimensionalItemObjectMap(
      Set<DimensionalItemId> itemIds);

  /**
   * Gets a map from dimension item ids to their dimension item objects without applying sharing
   * settings.
   *
   * @param itemIds a set of ids of the dimension item objects to get.
   * @return a map from the item ids to the dimension item objects.
   */
  Map<DimensionalItemId, DimensionalItemObject> getNoAclDataDimensionalItemObjectMap(
      Set<DimensionalItemId> itemIds);
}
