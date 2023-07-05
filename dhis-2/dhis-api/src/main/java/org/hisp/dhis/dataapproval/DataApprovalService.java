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
package org.hisp.dhis.dataapproval;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Jim Grace
 */
public interface DataApprovalService {
  String ID = DataApprovalService.class.getName();

  // -------------------------------------------------------------------------
  // Data approval workflow
  // -------------------------------------------------------------------------

  /**
   * Adds a DataApprovalWorkflow.
   *
   * @param workflow the DataApprovalWorkflow to add.
   * @return a generated unique id of the added Workflow.
   */
  long addWorkflow(DataApprovalWorkflow workflow);

  /**
   * Updates a DataApprovalWorkflow.
   *
   * @param workflow the DataApprovalWorkflow to update.
   */
  void updateWorkflow(DataApprovalWorkflow workflow);

  /**
   * Deletes a DataApprovalWorkflow.
   *
   * @param workflow the DataApprovalWorkflow to delete.
   */
  void deleteWorkflow(DataApprovalWorkflow workflow);

  /**
   * Returns a DataApprovalWorkflow.
   *
   * @param id the id of the DataApprovalWorkflow to return.
   * @return the DataApprovalWorkflow with the given id, or null if no match.
   */
  DataApprovalWorkflow getWorkflow(long id);

  /**
   * Returns a DataApprovalWorkflow.
   *
   * @param uid the uid of the DataApprovalWorkflow to return.
   * @return the DataApprovalWorkflow with the given id, or null if no match.
   */
  DataApprovalWorkflow getWorkflow(String uid);

  /**
   * Returns all DataApprovalWorkflows.
   *
   * @return a list of all DataApprovalWorkflows.
   */
  List<DataApprovalWorkflow> getAllWorkflows();

  // -------------------------------------------------------------------------
  // Data approval logic
  // -------------------------------------------------------------------------

  /**
   * Approves data.
   *
   * @param dataApprovalList describes the data to be approved.
   */
  void approveData(List<DataApproval> dataApprovalList);

  /**
   * Unapproves data.
   *
   * @param dataApprovalList describes the data to be unapproved.
   */
  void unapproveData(List<DataApproval> dataApprovalList);

  /**
   * Accepts data.
   *
   * @param dataApprovalList describes the data to be accepted.
   */
  void acceptData(List<DataApproval> dataApprovalList);

  /**
   * Unaccepts data.
   *
   * @param dataApprovalList describes the data to be unaccepted.
   */
  void unacceptData(List<DataApproval> dataApprovalList);

  /**
   * Adds a data approval. Prefer to use {@link DataApprovalService#approveData(List)}.
   *
   * @param dataApproval the DataApproval to add.
   */
  void addDataApproval(DataApproval dataApproval);

  /**
   * Gets the data approval record if it exists.
   *
   * @param approval the DataApproval object properties to look for
   * @return a data approval record.
   */
  DataApproval getDataApproval(DataApproval approval);

  /**
   * Tells whether data is approved (and therefore locked by approval.)
   *
   * @param workflow workflow to check for approval.
   * @param period Period to check for approval.
   * @param organisationUnit OrganisationUnit to check for approval.
   * @param attributeOptionCombo CategoryOptionCombo (if any) for approval.
   * @return true if data is approved.
   */
  boolean isApproved(
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo attributeOptionCombo);

  /**
   * Returns a map showing each data approval status for a list of data approval objects.
   *
   * @param dataApprovalList the data approvals to check.
   * @return the data approvals with status.
   */
  Map<DataApproval, DataApprovalStatus> getDataApprovalStatuses(
      List<DataApproval> dataApprovalList);

  /**
   * Returns the data approval status and permissions for a given data set, period, organisation
   * unit and attribute category combination. If attributeOptionCombo is null, the default option
   * combo will be used. If data is approved at multiple levels, the lowest level is returned.
   *
   * @param workflow workflow to check for approval.
   * @param period Period to check for approval.
   * @param organisationUnit OrganisationUnit to check for approval.
   * @param attributeOptionCombo CategoryOptionCombo (if any) for approval.
   * @return the data approval status.
   */
  DataApprovalStatus getDataApprovalStatus(
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo attributeOptionCombo);

  /**
   * Returns a list of approval status and permissions for all the attribute option combos that the
   * user is allowed to see or for specified attribute option combos.
   *
   * @param workflow workflow to check for approval.
   * @param period Period we are getting the status for
   * @param orgUnit Organisation unit we are getting the status for
   * @param orgUnitFilter Organisation unit filter for attribute option combos
   * @param attributeCombo attribute category combo to search within
   * @param attributeOptionCombos attribute option combos to get
   * @return list of statuses and permissions
   */
  List<DataApprovalStatus> getUserDataApprovalsAndPermissions(
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit orgUnit,
      OrganisationUnit orgUnitFilter,
      CategoryCombo attributeCombo,
      Set<CategoryOptionCombo> attributeOptionCombos);

  /**
   * Deletes DataApprovals for the given organisation unit.
   *
   * @param organisationUnit the organisation unit.
   */
  void deleteDataApprovals(OrganisationUnit organisationUnit);
}
