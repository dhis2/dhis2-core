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
package org.hisp.dhis.analytics;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;

/**
 * Manager for analytics engine security concerns.
 *
 * @author Lars Helge Overland
 */
public interface AnalyticsSecurityManager {
  /**
   * Decides whether the current user has privileges to execute the given query.
   *
   * @param params the data query parameters.
   * @throws IllegalQueryException if the current user does not have privileges to execute the given
   *     query.
   */
  void decideAccess(DataQueryParams params) throws IllegalQueryException;

  /**
   * Decides whether the current user has privileges to execute the given query.
   *
   * @param queryOrgUnits the organisation units to check access for.
   * @param readObjects the objects to check access for.
   * @throws IllegalQueryException if the current user does not have privileges to execute the given
   *     query.
   */
  void decideAccess(List<OrganisationUnit> queryOrgUnits, Set<IdentifiableObject> readObjects);

  /**
   * Decides whether the current user has privileges to execute the given event query.
   *
   * @param params the event data query parameters.
   * @throws IllegalQueryException if the current user does not have privileges to execute the given
   *     query.
   */
  void decideAccessEventQuery(EventQueryParams params) throws IllegalQueryException;

  /** Decides whether the current user has privileges to execute the given event analytics query. */
  void decideAccessEventAnalyticsAuthority();

  /**
   * Returns the current user. Looks for a current user to be specified for the given query, if not
   * uses the current user from the security context.
   *
   * @param params the data query parameters.
   * @return the current user.
   */
  User getCurrentUser(DataQueryParams params);

  /**
   * Returns a query with relevant data approval levels if system is configured to hide unapproved
   * data from analytics and if there are relevant approval levels for current user. Populates the
   * approvalLevels property of the given query and sets the level property of each related
   * organisation unit.
   *
   * @param params the data query parameters.
   * @return a data query parameters.
   * @throws IllegalQueryException is the specified approval level does not exist.
   */
  DataQueryParams withDataApprovalConstraints(DataQueryParams params);

  /**
   * Returns a query with two constraints applied:
   *
   * <p>Organisation unit constraints will be added as filters to this query based on the "data
   * view" organisation units associated with the current user. If organisation units are already
   * specified with accessible items in the query, no action is taken.
   *
   * <p>Dimension constraints with all accessible dimension items will be added as filters to this
   * query. If current user has no dimension constraints, no action is taken. If the constraint
   * dimensions are already specified with accessible items in the query, no action is taken. If the
   * current user does not have accessible items in any dimension constraint, an
   * IllegalQueryException is thrown.
   *
   * @param params the data query parameters.
   * @return a data query parameters.
   * @throws IllegalQueryException if user has dimension constraints specified but no access to any
   *     items in those dimensions.
   */
  DataQueryParams withUserConstraints(DataQueryParams params);

  /**
   * Returns a query with two constraints applied:
   *
   * <p>Organisation unit constraints will be added as filters to this query based on the "data
   * view" organisation units associated with the current user. If organisation units are already
   * specified with accessible items in the query, no action is taken.
   *
   * <p>Dimension constraints with all accessible dimension items will be added as filters to this
   * query. If current user has no dimension constraints, no action is taken. If the constraint
   * dimensions are already specified with accessible items in the query, no action is taken. If the
   * current user does not have accessible items in any dimension constraint, an
   * IllegalQueryException is thrown.
   *
   * @param params the event query parameters.
   * @return an event query parameters.
   * @throws IllegalQueryException if user has dimension constraints specified but no access to any
   *     items in those dimensions.
   */
  EventQueryParams withUserConstraints(EventQueryParams params);
}
