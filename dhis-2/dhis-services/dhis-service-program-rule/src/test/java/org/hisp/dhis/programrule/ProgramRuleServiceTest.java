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
package org.hisp.dhis.programrule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.program.ProgramStageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramRuleServiceTest extends IntegrationTestBase {

  private Program programA;

  private Program programB;

  private Program programC;

  private ProgramStage programStageA;

  private ProgramStage programStageB;

  private ProgramStageSection programStageSectionA;

  private ProgramRule programRuleA;

  private ProgramRuleAction programRuleActionA;

  private ProgramRuleAction programRuleActionB;

  private ProgramRuleVariable programRuleVariableA;

  private ProgramRuleVariable programRuleVariableB;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private ProgramStageSectionService programStageSectionService;

  @Autowired private ProgramRuleService programRuleService;

  @Autowired private ProgramRuleActionService programRuleActonService;

  @Autowired private ProgramRuleVariableService programRuleVariableService;

  @Autowired private DeletedObjectStore deletedObjectStore;

  @Override
  public void setUpTest() {
    programA = createProgram('A', null, null);
    programB = createProgram('B', null, null);
    programC = createProgram('C', null, null);
    programService.addProgram(programA);
    programService.addProgram(programB);
    programService.addProgram(programC);
    programStageSectionA = createProgramStageSection('A', 1);
    programStageSectionService.saveProgramStageSection(programStageSectionA);
    programStageA = createProgramStage('A', 1);
    programStageA.setProgram(programA);
    Set<ProgramStage> stagesA = new HashSet<>();
    stagesA.add(programStageA);
    programA.setProgramStages(stagesA);
    programStageService.saveProgramStage(programStageA);
    programStageB = createProgramStage('B', 1);
    programStageB.setProgram(programA);
    programStageB.setProgramStageSections(Sets.newHashSet(programStageSectionA));
    programStageService.saveProgramStage(programStageB);
    programStageSectionA.setProgramStage(programStageB);
    programStageSectionService.updateProgramStageSection(programStageSectionA);
    // Add a tree of variables, rules and actions to programA:
    programRuleA = createProgramRule('A', programA);
    programRuleService.addProgramRule(programRuleA);
    programRuleActionA = createProgramRuleAction('A', programRuleA);
    programRuleActionB = createProgramRuleAction('B', programRuleA);
    programRuleActonService.addProgramRuleAction(programRuleActionA);
    programRuleActonService.addProgramRuleAction(programRuleActionB);
    programRuleA
        .getProgramRuleActions()
        .addAll(Sets.newHashSet(programRuleActionA, programRuleActionB));
    programRuleService.updateProgramRule(programRuleA);
    programRuleVariableA = createProgramRuleVariable('A', programA);
    programRuleVariableB = createProgramRuleVariable('B', programA);
    programRuleVariableService.addProgramRuleVariable(programRuleVariableA);
    programRuleVariableService.addProgramRuleVariable(programRuleVariableB);
  }

  @Test
  void testAddGet() {
    ProgramRule ruleA =
        new ProgramRule("RuleA", "descriptionA", programA, programStageA, null, "true", null);
    ProgramRule ruleB = new ProgramRule("RuleB", "descriptionA", programA, null, null, "$a < 1", 1);
    ProgramRule ruleC =
        new ProgramRule(
            "RuleC", "descriptionA", programA, null, null, "($a < 1 && $a > -10) && !$b", 0);
    long idA = programRuleService.addProgramRule(ruleA);
    long idB = programRuleService.addProgramRule(ruleB);
    long idC = programRuleService.addProgramRule(ruleC);
    assertEquals(ruleA, programRuleService.getProgramRule(idA));
    assertEquals(ruleB, programRuleService.getProgramRule(idB));
    assertEquals(ruleC, programRuleService.getProgramRule(idC));
  }

  @Test
  void testAddGetDefaultValuesForEvaluationTimeAndEnvironments() {
    ProgramRule ruleA =
        new ProgramRule("RuleA", "descriptionA", programA, programStageA, null, "true", null);
    long idA = programRuleService.addProgramRule(ruleA);
    ProgramRuleAction ruleActionA = createProgramRuleAction('A', ruleA);
    programRuleActonService.addProgramRuleAction(ruleActionA);
    ruleA.setProgramRuleActions(Sets.newHashSet(ruleActionA));
    programRuleService.updateProgramRule(ruleA);
    ProgramRule retrievedProgramRule = programRuleService.getProgramRule(idA);
    Set<ProgramRuleActionEvaluationTime> evaluationTimesForActions =
        retrievedProgramRule.getProgramRuleActions().stream()
            .map(ProgramRuleAction::getProgramRuleActionEvaluationTime)
            .collect(Collectors.toSet());
    Set<ProgramRuleActionEvaluationEnvironment> evaluationEnvironmentsForActions =
        retrievedProgramRule.getProgramRuleActions().stream()
            .flatMap(action -> action.getProgramRuleActionEvaluationEnvironments().stream())
            .collect(Collectors.toSet());
    assertEquals(ruleA, retrievedProgramRule);
    assertEquals(1, evaluationTimesForActions.size());
    assertTrue(evaluationTimesForActions.contains(ProgramRuleActionEvaluationTime.ALWAYS));
    assertEquals(
        ProgramRuleActionEvaluationEnvironment.getDefault().size(),
        evaluationEnvironmentsForActions.size());
    assertEquals(
        ProgramRuleActionEvaluationEnvironment.getDefault(), evaluationEnvironmentsForActions);
  }

  @Test
  void testAddGetNotDefaultValuesForEvaluationTimeAndEnvironments() {
    ProgramRule ruleA =
        new ProgramRule("RuleA", "descriptionA", programA, programStageA, null, "true", null);
    long idA = programRuleService.addProgramRule(ruleA);
    ProgramRuleAction ruleActionA = createProgramRuleAction('A', ruleA);
    ruleActionA.setProgramRuleActionEvaluationTime(ProgramRuleActionEvaluationTime.ON_COMPLETE);
    ruleActionA.setProgramRuleActionEvaluationEnvironments(
        Sets.newHashSet(ProgramRuleActionEvaluationEnvironment.ANDROID));
    programRuleActonService.addProgramRuleAction(ruleActionA);
    ruleA.setProgramRuleActions(Sets.newHashSet(ruleActionA));
    programRuleService.updateProgramRule(ruleA);
    ProgramRule retrievedProgramRule = programRuleService.getProgramRule(idA);
    Set<ProgramRuleActionEvaluationTime> evaluationTimesForActions =
        retrievedProgramRule.getProgramRuleActions().stream()
            .map(ProgramRuleAction::getProgramRuleActionEvaluationTime)
            .collect(Collectors.toSet());
    Set<ProgramRuleActionEvaluationEnvironment> evaluationEnvironmentsForActions =
        retrievedProgramRule.getProgramRuleActions().stream()
            .flatMap(action -> action.getProgramRuleActionEvaluationEnvironments().stream())
            .collect(Collectors.toSet());
    assertEquals(ruleA, retrievedProgramRule);
    assertEquals(1, evaluationTimesForActions.size());
    assertTrue(evaluationTimesForActions.contains(ProgramRuleActionEvaluationTime.ON_COMPLETE));
    assertEquals(1, evaluationEnvironmentsForActions.size());
    assertEquals(
        Sets.newHashSet(ProgramRuleActionEvaluationEnvironment.ANDROID),
        evaluationEnvironmentsForActions);
  }

  @Test
  void testAddRetrieveProgramRulesByEvaluationTimeAndEnvironments() {
    ProgramRule ruleA =
        new ProgramRule("RuleA", "descriptionA", programA, programStageA, null, "true", null);
    ProgramRule ruleB =
        new ProgramRule("RuleB", "descriptionB", programB, programStageA, null, "true", null);
    long idA = programRuleService.addProgramRule(ruleA);
    long idB = programRuleService.addProgramRule(ruleB);
    ProgramRuleAction ruleActionA = createProgramRuleAction('C', ruleA);
    ruleActionA.setProgramRuleActionEvaluationTime(ProgramRuleActionEvaluationTime.ON_COMPLETE);
    ruleActionA.setProgramRuleActionEvaluationEnvironments(
        Sets.newHashSet(ProgramRuleActionEvaluationEnvironment.ANDROID));
    programRuleActonService.addProgramRuleAction(ruleActionA);
    ProgramRuleAction ruleActionB = createProgramRuleAction('D', ruleB);
    ruleActionB.setProgramRuleActionEvaluationTime(ProgramRuleActionEvaluationTime.ON_DATA_ENTRY);
    ruleActionB.setProgramRuleActionEvaluationEnvironments(
        Sets.newHashSet(ProgramRuleActionEvaluationEnvironment.WEB));
    programRuleActonService.addProgramRuleAction(ruleActionB);
    ruleA.setProgramRuleActions(Sets.newHashSet(ruleActionA));
    programRuleService.updateProgramRule(ruleA);
    ruleB.setProgramRuleActions(Sets.newHashSet(ruleActionB));
    programRuleService.updateProgramRule(ruleB);
    ProgramRule retrievedProgramRuleA = programRuleService.getProgramRule(idA);
    ProgramRule retrievedProgramRuleB = programRuleService.getProgramRule(idB);
    assertEquals(ruleA, retrievedProgramRuleA);
    assertEquals(ruleB, retrievedProgramRuleB);
    assertEquals(
        2,
        programRuleService
            .getProgramRulesByEvaluationEnvironment(ProgramRuleActionEvaluationEnvironment.WEB)
            .size());
    assertEquals(
        2,
        programRuleService
            .getProgramRulesByEvaluationEnvironment(ProgramRuleActionEvaluationEnvironment.ANDROID)
            .size());
    assertEquals(
        1,
        programRuleService
            .getProgramRulesByEvaluationTime(ProgramRuleActionEvaluationTime.ALWAYS)
            .size());
    assertEquals(
        2,
        programRuleService
            .getProgramRulesByEvaluationTime(ProgramRuleActionEvaluationTime.ON_COMPLETE)
            .size());
    assertEquals(
        2,
        programRuleService
            .getProgramRulesByEvaluationTime(ProgramRuleActionEvaluationTime.ON_DATA_ENTRY)
            .size());
  }

  @Test
  void testAddRetrieveProgramRulesAndActionsByEvaluationTimeAndEnvironments() {
    ProgramRule ruleA =
        new ProgramRule("RuleA", "descriptionA", programA, programStageA, null, "true", null);
    ProgramRule ruleB =
        new ProgramRule("RuleB", "descriptionB", programB, programStageA, null, "true", null);
    long idA = programRuleService.addProgramRule(ruleA);
    long idB = programRuleService.addProgramRule(ruleB);
    ProgramRuleAction ruleActionA1 = createProgramRuleAction('C', ruleA);
    ruleActionA1.setProgramRuleActionEvaluationTime(ProgramRuleActionEvaluationTime.ON_COMPLETE);
    ruleActionA1.setProgramRuleActionEvaluationEnvironments(
        Sets.newHashSet(ProgramRuleActionEvaluationEnvironment.ANDROID));
    programRuleActonService.addProgramRuleAction(ruleActionA1);
    ProgramRuleAction ruleActionA2 = createProgramRuleAction('D', ruleA);
    ruleActionA2.setProgramRuleActionEvaluationTime(ProgramRuleActionEvaluationTime.ON_DATA_ENTRY);
    ruleActionA2.setProgramRuleActionEvaluationEnvironments(
        Sets.newHashSet(ProgramRuleActionEvaluationEnvironment.WEB));
    programRuleActonService.addProgramRuleAction(ruleActionA2);
    ProgramRuleAction ruleActionB = createProgramRuleAction('E', ruleB);
    ruleActionB.setProgramRuleActionEvaluationTime(ProgramRuleActionEvaluationTime.ALWAYS);
    ruleActionB.setProgramRuleActionEvaluationEnvironments(
        Sets.newHashSet(
            ProgramRuleActionEvaluationEnvironment.WEB,
            ProgramRuleActionEvaluationEnvironment.ANDROID));
    programRuleActonService.addProgramRuleAction(ruleActionB);
    ruleA.setProgramRuleActions(Sets.newHashSet(ruleActionA1, ruleActionA2));
    programRuleService.updateProgramRule(ruleA);
    ruleB.setProgramRuleActions(Sets.newHashSet(ruleActionB));
    programRuleService.updateProgramRule(ruleB);
    ProgramRule retrievedProgramRuleA = programRuleService.getProgramRule(idA);
    ProgramRule retrievedProgramRuleB = programRuleService.getProgramRule(idB);
    assertEquals(ruleA, retrievedProgramRuleA);
    assertEquals(ruleB, retrievedProgramRuleB);
    assertEquals(
        3,
        programRuleService
            .getProgramRulesByEvaluationEnvironment(ProgramRuleActionEvaluationEnvironment.WEB)
            .size());
    assertEquals(
        3,
        programRuleService
            .getProgramRulesByEvaluationEnvironment(ProgramRuleActionEvaluationEnvironment.ANDROID)
            .size());
    assertEquals(
        2,
        programRuleService
            .getProgramRulesByEvaluationTime(ProgramRuleActionEvaluationTime.ALWAYS)
            .size());
    assertEquals(
        3,
        programRuleService
            .getProgramRulesByEvaluationTime(ProgramRuleActionEvaluationTime.ON_COMPLETE)
            .size());
    assertEquals(
        3,
        programRuleService
            .getProgramRulesByEvaluationTime(ProgramRuleActionEvaluationTime.ON_DATA_ENTRY)
            .size());
    ProgramRule programRuleARetrievedByEnvironment =
        programRuleService
            .getProgramRulesByEvaluationEnvironment(ProgramRuleActionEvaluationEnvironment.WEB)
            .stream()
            .filter(rule -> rule.getId() == idA)
            .findAny()
            .get();
    assertEquals(1, programRuleARetrievedByEnvironment.getProgramRuleActions().size());
    ProgramRule programRuleARetrievedByEvaluationTime =
        programRuleService
            .getProgramRulesByEvaluationTime(ProgramRuleActionEvaluationTime.ON_COMPLETE)
            .stream()
            .filter(rule -> rule.getId() == idA)
            .findAny()
            .get();
    assertEquals(1, programRuleARetrievedByEvaluationTime.getProgramRuleActions().size());
  }

  @Test
  void testGetByProgram() {
    ProgramRule ruleD =
        new ProgramRule("RuleD", "descriptionD", programB, null, null, "true", null);
    ProgramRule ruleE = new ProgramRule("RuleE", "descriptionE", programB, null, null, "$a < 1", 1);
    ProgramRule ruleF =
        new ProgramRule(
            "RuleF", "descriptionF", programB, null, null, "($a < 1 && $a > -10) && !$b", 0);
    // Add a rule that is not part of programB....
    ProgramRule ruleG = new ProgramRule("RuleG", "descriptionG", programA, null, null, "!false", 0);
    programRuleService.addProgramRule(ruleD);
    programRuleService.addProgramRule(ruleE);
    programRuleService.addProgramRule(ruleF);
    programRuleService.addProgramRule(ruleG);
    // Get all the 3 rules for programB
    List<ProgramRule> rules = programRuleService.getProgramRule(programB);
    assertEquals(3, rules.size());
    assertTrue(rules.contains(ruleD));
    assertTrue(rules.contains(ruleE));
    assertTrue(rules.contains(ruleF));
    // Make sure that the rule connected to program A is not returned as
    // part of list of rules in program B.
    assertFalse(rules.contains(ruleG));
    assertEquals(ruleD.getId(), programRuleService.getProgramRuleByName("RuleD", programB).getId());
    assertEquals(3, programRuleService.getProgramRules(programB, "rule").size());
  }

  @Test
  void testGetProgramRulesByActionTypes() {
    ProgramRule ruleD =
        new ProgramRule("RuleD", "descriptionD", programB, null, null, "true", null);
    ProgramRule ruleE = new ProgramRule("RuleE", "descriptionE", programB, null, null, "$a < 1", 1);
    // Add a rule that is not part of programB
    ProgramRule ruleG = new ProgramRule("RuleG", "descriptionG", programA, null, null, "!false", 0);
    programRuleService.addProgramRule(ruleD);
    programRuleService.addProgramRule(ruleE);
    programRuleService.addProgramRule(ruleG);
    ProgramRuleAction actionD = createProgramRuleAction('D');
    actionD.setProgramRuleActionType(ProgramRuleActionType.SENDMESSAGE);
    actionD.setProgramRule(ruleD);
    programRuleActonService.addProgramRuleAction(actionD);
    ruleD.setProgramRuleActions(Sets.newHashSet(actionD));
    programRuleService.updateProgramRule(ruleD);
    // Get all the 3 rules for programB
    List<ProgramRule> rules =
        programRuleService.getProgramRulesByActionTypes(
            programB, ProgramRuleActionType.IMPLEMENTED_ACTIONS);
    assertEquals(1, rules.size());
    assertTrue(rules.contains(ruleD));
    assertFalse(rules.contains(ruleG));
  }

  @Test
  void testUpdate() {
    ProgramRule ruleH =
        new ProgramRule("RuleA", "descriptionA", programA, programStageA, null, "true", null);
    long idH = programRuleService.addProgramRule(ruleH);
    ruleH.setCondition("$newcondition == true");
    ruleH.setName("new name");
    ruleH.setDescription("new desc");
    ruleH.setPriority(99);
    ruleH.setProgram(programC);
    programRuleService.updateProgramRule(ruleH);
    assertEquals(ruleH, programRuleService.getProgramRule(idH));
  }

  @Test
  void testDeleteProgramRule() {
    ProgramRule ruleI =
        new ProgramRule("RuleI", "descriptionI", programB, null, null, "true", null);
    ProgramRule ruleJ = new ProgramRule("RuleJ", "descriptionJ", programB, null, null, "$a < 1", 1);
    long idI = programRuleService.addProgramRule(ruleI);
    long idJ = programRuleService.addProgramRule(ruleJ);
    assertNotNull(programRuleService.getProgramRule(idI));
    assertNotNull(programRuleService.getProgramRule(idJ));
    programRuleService.deleteProgramRule(ruleI);
    assertNull(programRuleService.getProgramRule(idI));
    assertNotNull(programRuleService.getProgramRule(idJ));
    programRuleService.deleteProgramRule(ruleJ);
    assertNull(programRuleService.getProgramRule(idI));
    assertNull(programRuleService.getProgramRule(idJ));
  }

  @Test
  void testDeleteDeletedObjectWithCascade() {
    ProgramRule programRule = createProgramRule('C', programA);
    ProgramRuleAction programRuleAction = createProgramRuleAction('D');
    programRuleAction.setProgramRuleActionType(ProgramRuleActionType.SENDMESSAGE);
    programRuleAction.setProgramRule(programRule);
    programRule.setProgramRuleActions(Sets.newHashSet(programRuleAction));
    programRuleService.addProgramRule(programRule);
    String programRuleUID = programRule.getUid();
    String programRuleActionUID = programRuleAction.getUid();
    programRuleService.deleteProgramRule(programRule);
    ProgramRule programRule1 = createProgramRule('X', programA);
    programRule1.setUid(programRuleUID);
    ProgramRuleAction programRuleAction1 = createProgramRuleAction('D');
    programRuleAction1.setProgramRuleActionType(ProgramRuleActionType.SENDMESSAGE);
    programRuleAction1.setProgramRule(programRule1);
    programRuleAction1.setUid(programRuleActionUID);
    programRule1.setProgramRuleActions(Sets.newHashSet(programRuleAction1));
    programRuleService.addProgramRule(programRule1);
    programRuleService.deleteProgramRule(programRule1);
    assertNotNull(deletedObjectStore.query(new DeletedObjectQuery(programRule1)));
  }

  @Test
  void testCascadingDeleteProgram() {
    programService.deleteProgram(programA);
    assertNull(programRuleService.getProgramRule(programRuleA.getId()));
    assertNull(programRuleActonService.getProgramRuleAction(programRuleActionA.getId()));
    assertNull(programRuleActonService.getProgramRuleAction(programRuleActionB.getId()));
    assertNull(programRuleVariableService.getProgramRuleVariable(programRuleVariableA.getId()));
    assertNull(programRuleVariableService.getProgramRuleVariable(programRuleVariableB.getId()));
  }

  @Test
  void testDoNotAllowDeleteProgramStageBecauseOfLinkWithProgramRule() {
    programRuleA.setProgramStage(programStageA);
    programRuleService.updateProgramRule(programRuleA);
    assertThrows(
        DeleteNotAllowedException.class,
        () -> programStageService.deleteProgramStage(programStageA),
        "ProgramRuleA");
  }

  @Test
  void testDoNotAllowDeleteProgramStageSectionBecauseOfLinkWithProgramRuleAction() {
    programRuleActionA.setProgramStageSection(programStageSectionA);
    programRuleActonService.updateProgramRuleAction(programRuleActionA);
    programRuleA.getProgramRuleActions().add(programRuleActionA);
    programRuleService.updateProgramRule(programRuleA);
    assertThrows(
        DeleteNotAllowedException.class,
        () -> programStageSectionService.deleteProgramStageSection(programStageSectionA),
        "ProgramRuleA");
  }

  @Test
  void testDoNotAllowDeleteProgramStageBecauseOfLinkWithProgramRuleActionAndSection() {
    programRuleActionA.setProgramStageSection(programStageSectionA);
    programRuleActonService.updateProgramRuleAction(programRuleActionA);
    programRuleA.getProgramRuleActions().add(programRuleActionA);
    programRuleService.updateProgramRule(programRuleA);
    assertThrows(
        DeleteNotAllowedException.class,
        () -> programStageService.deleteProgramStage(programStageB),
        "ProgramRuleA");
  }

  @Test
  void testDoNotAllowDeleteProgramStageBecauseOfLinkWithProgramRuleAction() {
    programRuleActionA.setProgramStage(programStageA);
    programRuleActonService.updateProgramRuleAction(programRuleActionA);
    programRuleA.getProgramRuleActions().add(programRuleActionA);
    programRuleService.updateProgramRule(programRuleA);
    assertThrows(
        DeleteNotAllowedException.class,
        () -> programStageService.deleteProgramStage(programStageA),
        "ProgramRuleA");
  }

  @Test
  void testDoNotAllowDeleteProgramStageBecauseOfLinkWithProgramRuleVariable() {
    programRuleVariableA.setProgramStage(programStageA);
    programRuleVariableService.updateProgramRuleVariable(programRuleVariableA);
    assertThrows(
        DeleteNotAllowedException.class,
        () -> programStageService.deleteProgramStage(programStageA),
        "ProgramRuleVariableA");
  }
}
