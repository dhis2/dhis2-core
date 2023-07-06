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
package org.hisp.dhis.tracker.programrule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Zubair Asghar
 */
public class ProgramRuleVariableIntegrationTest extends TransactionalIntegrationTest {
  @Autowired private RenderService _renderService;

  @Autowired private UserService _userService;

  @Autowired private ProgramRuleVariableService programRuleVariableService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ProgramService programService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Override
  public void setUpTest() throws Exception {
    renderService = _renderService;
    userService = _userService;
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(
            new ClassPathResource("tracker/tracker_metadata_with_program_rules_variables.json")
                .getInputStream(),
            RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    ObjectBundleValidationReport validationReport = objectBundleValidationService.validate(bundle);
    assertFalse(validationReport.hasErrorReports());
    objectBundleService.commit(bundle);
  }

  @Test
  public void shouldAssignValueTypeFromTrackedEntityAttributeToProgramRuleVariable() {
    Program program = programService.getProgram("BFcipDERJne");
    TrackedEntityAttribute trackedEntityAttribute =
        trackedEntityAttributeService.getTrackedEntityAttribute("sYn3tkL3XKa");
    List<ProgramRuleVariable> ruleVariables =
        programRuleVariableService.getProgramRuleVariable(program);

    ProgramRuleVariable prv =
        ruleVariables.stream()
            .filter(r -> ProgramRuleVariableSourceType.TEI_ATTRIBUTE == r.getSourceType())
            .findFirst()
            .get();
    assertEquals(trackedEntityAttribute.getValueType(), prv.getValueType());
  }

  @Test
  public void shouldAssignDefaultValueTypeToProgramRuleVariable() {
    Program program = programService.getProgram("BFcipDERJne");
    List<ProgramRuleVariable> ruleVariables =
        programRuleVariableService.getProgramRuleVariable(program);

    ProgramRuleVariable prv =
        ruleVariables.stream()
            .filter(r -> ProgramRuleVariableSourceType.CALCULATED_VALUE == r.getSourceType())
            .findFirst()
            .get();
    assertEquals(ValueType.TEXT, prv.getValueType());
  }
}
