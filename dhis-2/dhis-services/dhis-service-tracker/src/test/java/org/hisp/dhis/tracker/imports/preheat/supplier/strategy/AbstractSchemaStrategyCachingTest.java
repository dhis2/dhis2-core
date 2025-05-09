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
package org.hisp.dhis.tracker.imports.preheat.supplier.strategy;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.descriptors.ProgramSchemaDescriptor;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.test.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.cache.PreheatCacheService;
import org.hisp.dhis.tracker.imports.preheat.mappers.CopyMapper;
import org.hisp.dhis.tracker.imports.preheat.mappers.ProgramMapper;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class AbstractSchemaStrategyCachingTest extends TestBase {

  @Mock private PreheatCacheService cache;

  @Mock private IdentifiableObjectManager manager;

  @Mock private QueryService queryService;

  @Mock private SchemaService schemaService;

  private TrackerPreheat preheat;

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @BeforeEach
  public void setUp() {
    preheat = new TrackerPreheat();
    injectSecurityContext(new SystemUser());
  }

  @Test
  void verifyObjectInCacheIsReturned() {
    // Given
    final Schema schema = new ProgramSchemaDescriptor().getSchema();

    String uid = CodeGenerator.generateUid();

    Program program = rnd.nextObject(Program.class);
    when(cache.get(Program.class.getSimpleName(), uid)).thenReturn(Optional.of(program));

    ProgramStrategy strategy = new ProgramStrategy(schemaService, queryService, manager, cache);

    // When
    strategy.queryForIdentifiableObjects(
        preheat,
        schema,
        TrackerIdSchemeParam.UID,
        singletonList(singletonList(uid)),
        ProgramMapper.INSTANCE.getClass());

    // Then
    assertThat(preheat.getAll(Program.class), hasSize(1));
  }

  @Test
  void verifyObjectNotInCacheIsFetchedFromDbAndPutInCache() {
    // Given
    final Schema schema = new ProgramSchemaDescriptor().getSchema();

    String uid = CodeGenerator.generateUid();

    Program program = rnd.nextObject(Program.class);

    when(cache.get(Program.class.getSimpleName(), uid)).thenReturn(Optional.empty());

    doReturn(singletonList(program)).when(queryService).query(any(Query.class));
    ProgramStrategy strategy = new ProgramStrategy(schemaService, queryService, manager, cache);

    // When
    strategy.queryForIdentifiableObjects(
        preheat,
        schema,
        TrackerIdSchemeParam.UID,
        singletonList(singletonList(uid)),
        CopyMapper.class);

    // Then
    assertThat(preheat.getAll(Program.class), hasSize(1));

    verify(cache, times(1)).put(eq("Program"), anyString(), any(), eq(20), eq(10L));
  }
}
