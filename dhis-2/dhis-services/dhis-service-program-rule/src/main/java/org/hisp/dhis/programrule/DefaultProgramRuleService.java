/*
 * Copyright (c) 2004-2026, University of Oslo
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.program.Program;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author markusbekken
 */
@Service("org.hisp.dhis.programrule.ProgramRuleService")
public class DefaultProgramRuleService implements ProgramRuleService {
  private static final String DATA_ELEMENTS_KEY = "dataElements";

  private static final String TRACKED_ENTITY_ATTRIBUTES_KEY = "trackedEntityAttributes";

  private final ProgramRuleStore programRuleStore;

  /**
   * {@code getProgramRulesByActionTypes}'s result keyed by {@code program.getUid()} - the {@code
   * actionTypes} argument is always {@link ProgramRuleActionType#SERVER_SUPPORTED_TYPES} at every
   * call site in the codebase, so it doesn't need to be part of the key.
   *
   * <p>A program's rules and their actions change only on rare admin edits, but this query ran
   * fresh on every tracker import that touched the program - discovered profiling a tracker import
   * where it showed up as real, repeated DB cost for an answer that hadn't changed since the
   * previous import. Invalidated wholesale (not per-program) by {@link
   * ProgramRuleActionCacheInvalidationListener} on any {@link ProgramRuleAction} write, since those
   * writes are rare and can arrive from several unrelated call sites (generic metadata CRUD,
   * metadata import, this service) that have no single method to hook into. The 3 hour TTL
   * (matching {@code programRuleVariablesCache}'s precedent in this module) is a backstop for
   * writes on another node in a cluster, which this same-node listener cannot see.
   */
  private final Cache<List<ProgramRule>> programRulesByActionTypes;

  /**
   * {@code getDataElementsPresentInProgramRules()}/{@code
   * getTrackedEntityAttributesPresentInProgramRules()}'s results, both global (no program scope)
   * and both queried with the same fixed action-type set at every call site. Same staleness/
   * invalidation reasoning as {@link #programRulesByActionTypes}.
   */
  private final Cache<List<String>> programRuleActionUids;

  public DefaultProgramRuleService(ProgramRuleStore programRuleStore, CacheProvider cacheProvider) {
    this.programRuleStore = programRuleStore;
    this.programRulesByActionTypes = cacheProvider.createProgramRulesByActionTypesCache();
    this.programRuleActionUids = cacheProvider.createProgramRuleActionUidsCache();
  }

  /**
   * Evicts both program-rule-action caches, so the next read recomputes them instead of serving a
   * stale answer. Called by {@link ProgramRuleActionCacheInvalidationListener}.
   */
  void invalidateProgramRuleActionCaches() {
    programRulesByActionTypes.invalidateAll();
    programRuleActionUids.invalidateAll();
  }

  // -------------------------------------------------------------------------
  // ProgramRule implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addProgramRule(ProgramRule programRule) {
    programRuleStore.save(programRule);
    return programRule.getId();
  }

  @Override
  @Transactional
  public void deleteProgramRule(ProgramRule programRule) {
    programRuleStore.delete(programRule);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesForEnrollment(
      Program program, Set<ProgramRuleActionType> actionTypes) {
    return programRuleStore.getProgramRulesForEnrollment(program, actionTypes);
  }

  @Override
  @Transactional
  public void updateProgramRule(ProgramRule programRule) {
    programRuleStore.update(programRule);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramRule getProgramRule(long id) {
    return programRuleStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramRule getProgramRule(String uid) {
    return programRuleStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramRule getProgramRuleByName(String name, Program program) {
    return programRuleStore.getByName(name, program);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getDataElementsPresentInProgramRules() {
    return programRuleActionUids.get(
        DATA_ELEMENTS_KEY,
        key ->
            programRuleStore.getDataElementsPresentInProgramRules(
                ProgramRuleActionType.SERVER_SUPPORTED_TYPES));
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getTrackedEntityAttributesPresentInProgramRules() {
    return programRuleActionUids.get(
        TRACKED_ENTITY_ATTRIBUTES_KEY,
        key ->
            programRuleStore.getTrackedEntityAttributesPresentInProgramRules(
                ProgramRuleActionType.SERVER_SUPPORTED_TYPES));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getAllProgramRule() {
    return programRuleStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesByActionTypes(
      Program program, Set<ProgramRuleActionType> actionTypes) {
    String key =
        program.getUid()
            + ":"
            + actionTypes.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    return programRulesByActionTypes.get(
        key, k -> programRuleStore.getProgramRulesByActionTypes(program, actionTypes));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRule(Program program) {
    return programRuleStore.get(program);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRules(Program program, String key) {
    return programRuleStore.get(program, key);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesWithNoPriority() {
    return programRuleStore.getProgramRulesWithNoPriority();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesWithNoCondition() {
    return programRuleStore.getProgramRulesWithNoCondition();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesByEvaluationTime(
      ProgramRuleActionEvaluationTime evaluationTime) {
    return programRuleStore.getProgramRulesByEvaluationTime(evaluationTime);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesByEvaluationEnvironment(
      ProgramRuleActionEvaluationEnvironment evaluationEnvironment) {
    return programRuleStore.getProgramRulesByEvaluationEnvironment(evaluationEnvironment);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesWithNoAction() {
    return programRuleStore.getProgramRulesWithNoAction();
  }
}
