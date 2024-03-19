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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.validation;

import static org.hisp.dhis.dxf2.importsummary.ImportSummary.success;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.Checker;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Program;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class AttributeOptionComboCheck implements Checker {
  @Override
  public ImportSummary check(ImmutableEvent event, WorkContext ctx) {
    Program program = ctx.getProgramsMap().get(event.getProgram());
    CategoryOptionCombo coc = ctx.getCategoryOptionComboMap().get(event.getUid());

    if (coc != null
        && coc.isDefault()
        && program.getCategoryCombo() != null
        && !program.getCategoryCombo().isDefault()) {
      ImportSummary importSummary = new ImportSummary(event.getEvent());
      importSummary.addConflict(
          "attributeOptionCombo",
          "Valid attribute option combo must be specified since program does not have the default category combo");
      importSummary.setStatus(ImportStatus.ERROR);
      return importSummary.incrementIgnored();
    }

    return success();
  }
}
