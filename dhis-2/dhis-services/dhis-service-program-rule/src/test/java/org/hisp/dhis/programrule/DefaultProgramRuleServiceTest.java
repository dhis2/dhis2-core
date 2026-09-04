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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProgramRuleServiceTest {

  @Mock private ProgramRuleStore programRuleStore;

  @Mock private CacheProvider cacheProvider;

  private DefaultProgramRuleService service;

  private ProgramRuleActionCaches caches;

  private Program program;

  @BeforeEach
  void setUp() {
    when(cacheProvider.<List<ProgramRule>>createProgramRulesByActionTypesCache())
        .thenReturn(new SimpleCacheBuilder<List<ProgramRule>>().build());
    when(cacheProvider.<List<String>>createProgramRuleActionUidsCache())
        .thenReturn(new SimpleCacheBuilder<List<String>>().build());

    caches = new ProgramRuleActionCaches(cacheProvider);
    service = new DefaultProgramRuleService(programRuleStore, caches);

    program = new Program();
    program.setUid("program12345");
  }

  @Test
  void getDataElementsPresentInProgramRulesHitsTheStoreOnlyOnce() {
    when(programRuleStore.getDataElementsPresentInProgramRules(
            ProgramRuleActionType.SERVER_SUPPORTED_TYPES))
        .thenReturn(List.of("dataElementUid1"));

    List<String> first = service.getDataElementsPresentInProgramRules();
    List<String> second = service.getDataElementsPresentInProgramRules();

    assertEquals(List.of("dataElementUid1"), first);
    assertEquals(List.of("dataElementUid1"), second);
    verify(programRuleStore, times(1))
        .getDataElementsPresentInProgramRules(ProgramRuleActionType.SERVER_SUPPORTED_TYPES);
  }

  @Test
  void getTrackedEntityAttributesPresentInProgramRulesHitsTheStoreOnlyOnce() {
    when(programRuleStore.getTrackedEntityAttributesPresentInProgramRules(
            ProgramRuleActionType.SERVER_SUPPORTED_TYPES))
        .thenReturn(List.of("attributeUid1"));

    service.getTrackedEntityAttributesPresentInProgramRules();
    service.getTrackedEntityAttributesPresentInProgramRules();

    verify(programRuleStore, times(1))
        .getTrackedEntityAttributesPresentInProgramRules(
            ProgramRuleActionType.SERVER_SUPPORTED_TYPES);
  }

  @Test
  void getProgramRulesByActionTypesHitsTheStoreOnlyOncePerProgram() {
    Set<ProgramRuleActionType> actionTypes = ProgramRuleActionType.SERVER_SUPPORTED_TYPES;
    when(programRuleStore.getProgramRulesByActionTypes(eq(program), any())).thenReturn(List.of());

    service.getProgramRulesByActionTypes(program, actionTypes);
    service.getProgramRulesByActionTypes(program, actionTypes);

    verify(programRuleStore, times(1)).getProgramRulesByActionTypes(program, actionTypes);
  }

  @Test
  void invalidatingTheCachesForcesARecompute() {
    when(programRuleStore.getDataElementsPresentInProgramRules(
            ProgramRuleActionType.SERVER_SUPPORTED_TYPES))
        .thenReturn(List.of("dataElementUid1"));

    service.getDataElementsPresentInProgramRules();
    caches.invalidateAll();
    service.getDataElementsPresentInProgramRules();

    verify(programRuleStore, times(2))
        .getDataElementsPresentInProgramRules(ProgramRuleActionType.SERVER_SUPPORTED_TYPES);
  }
}
