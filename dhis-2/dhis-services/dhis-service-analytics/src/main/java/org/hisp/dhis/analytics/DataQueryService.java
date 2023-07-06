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
package org.hisp.dhis.analytics;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Service which provides methods for assembling DataQueryParams objects.
 *
 * @author Lars Helge Overland
 */
public interface DataQueryService {
  /**
   * Creates a data query parameter object from the given request.
   *
   * @param request request wrapper object containing the URL parameters.
   * @return a data query parameter object created based on the given URL info.
   * @throws IllegalQueryException if the query is illegal.
   */
  DataQueryParams getFromRequest(DataQueryRequest request);

  /**
   * Creates a data query parameter object from the given BaseAnalyticalObject.
   *
   * @param object the BaseAnalyticalObject.
   * @return a data query parameter object created based on the given BaseAnalyticalObject.
   * @throws IllegalQueryException if the query is illegal.
   */
  DataQueryParams getFromAnalyticalObject(AnalyticalObject object);

  /**
   * Creates a list of DimensionalObject from the given set of dimension params.
   *
   * @param dimensionParams the dimension URL parameter.
   * @param relativePeriodDate the date to use as basis for relative periods.
   * @param userOrgUnit the user organisation unit parameter, overrides current user, can be null.
   * @param inputIdScheme the identifier scheme to interpret dimension and filters.
   * @return a list of DimensionalObject.
   * @throws IllegalQueryException if the query is illegal.
   */
  default List<DimensionalObject> getDimensionalObjects(
      Set<String> dimensionParams,
      Date relativePeriodDate,
      String userOrgUnit,
      DisplayProperty displayProperty,
      IdScheme inputIdScheme) {
    List<DimensionalObject> list = new ArrayList<>();
    List<OrganisationUnit> userOrgUnits = getUserOrgUnits(null, userOrgUnit);

    if (dimensionParams != null) {
      for (String param : dimensionParams) {
        String dimension = getDimensionFromParam(param);
        List<String> items = getDimensionItemsFromParam(param);

        if (dimension != null && items != null) {
          list.add(
              getDimension(
                  dimension,
                  items,
                  relativePeriodDate,
                  userOrgUnits,
                  false,
                  displayProperty,
                  inputIdScheme));
        }
      }
    }

    return list;
  }

  /**
   * Returns a persisted DimensionalObject generated from the given dimension identifier and list of
   * dimension options.
   *
   * <p>For the pe dimension items, relative periods represented by enums will be replaced by real
   * ISO periods relative to the current date. For the ou dimension items, the user organisation
   * unit enums USER_ORG_UNIT|USER_ORG_UNIT_CHILDREN will be replaced by the persisted organisation
   * units for the current user.
   *
   * @param dimension the dimension identifier.
   * @param items the dimension items.
   * @param relativePeriodDate the date to use for generating relative periods, can be null.
   * @param userOrgUnits the list of user organisation units, overrides current user, can be null.
   * @param allowNull return null if no dimension was found.
   * @param inputIdScheme the identifier scheme to interpret dimension and filters.
   * @return list of DimensionalObjects.
   * @throws IllegalQueryException if the query is illegal.
   */
  DimensionalObject getDimension(
      String dimension,
      List<String> items,
      Date relativePeriodDate,
      List<OrganisationUnit> userOrgUnits,
      boolean allowNull,
      DisplayProperty displayProperty,
      IdScheme inputIdScheme);

  /**
   * Returns a persisted DimensionalObject generated from the given dimension identifier and list of
   * dimension options.
   *
   * <p>For the pe dimension items, relative periods represented by enums will be replaced by real
   * ISO periods relative to the current date. For the ou dimension items, the user organisation
   * unit enums USER_ORG_UNIT|USER_ORG_UNIT_CHILDREN will be replaced by the persisted organisation
   * units for the current user.
   *
   * @param dimension the dimension identifier.
   * @param items the dimension items.
   * @param request query request periods, can be null.
   * @param userOrgUnits the list of user organisation units, overrides current user, can be null.
   * @param allowNull return null if no dimension was found.
   * @param inputIdScheme the identifier scheme to interpret dimension and filters.
   * @return list of DimensionalObjects.
   * @throws IllegalQueryException if the query is illegal.
   */
  DimensionalObject getDimension(
      String dimension,
      List<String> items,
      EventDataQueryRequest request,
      List<OrganisationUnit> userOrgUnits,
      boolean allowNull,
      IdScheme inputIdScheme);

  /**
   * Returns a list of user organisation units, looking first at the given user org unit parameter,
   * second at the organisation units associated with the current user. Returns an empty list if no
   * organisation units are found.
   *
   * @param params the data query parameters.
   * @param userOrgUnit the user org unit parameter string.
   * @return a list of organisation units.
   */
  List<OrganisationUnit> getUserOrgUnits(DataQueryParams params, String userOrgUnit);
}
