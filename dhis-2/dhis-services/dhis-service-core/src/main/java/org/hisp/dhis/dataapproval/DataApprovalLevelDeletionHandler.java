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

import java.util.Map;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Jim Grace
 */
@Component
public class DataApprovalLevelDeletionHandler extends IdObjectDeletionHandler<DataApprovalLevel> {
  @Override
  protected void registerHandler() {
    whenVetoing(CategoryOptionGroupSet.class, this::allowDeleteCategoryOptionGroupSet);
    whenVetoing(DataApprovalWorkflow.class, this::allowDeleteDataApprovalWorkflow);
  }

  public DeletionVeto allowDeleteCategoryOptionGroupSet(
      CategoryOptionGroupSet categoryOptionGroupSet) {
    String sql = "select 1 from dataapprovallevel where categoryoptiongroupsetid=:id limit 1";
    return vetoIfExists(VETO, sql, Map.of("id", categoryOptionGroupSet.getId()));
  }

  public DeletionVeto allowDeleteDataApprovalWorkflow(DataApprovalWorkflow workflow) {
    String sql = "select 1 from dataapprovalworkflowlevels where workflowid=:id limit 1";
    return vetoIfExists(VETO, sql, Map.of("id", workflow.getId()));
  }
}
