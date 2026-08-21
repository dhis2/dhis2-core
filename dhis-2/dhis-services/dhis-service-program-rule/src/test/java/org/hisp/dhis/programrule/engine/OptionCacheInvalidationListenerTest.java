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
package org.hisp.dhis.programrule.engine;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionCacheInvalidationListenerTest {

  @Mock private DefaultProgramRuleEntityMapperService mapperService;

  @Mock private PostInsertEvent postInsertEvent;

  @Mock private PostUpdateEvent postUpdateEvent;

  @Mock private PostDeleteEvent postDeleteEvent;

  private OptionCacheInvalidationListener listener;

  @BeforeEach
  void setUp() {
    listener = new OptionCacheInvalidationListener(mapperService);
  }

  private Option optionOf(long optionSetId) {
    OptionSet optionSet = new OptionSet();
    optionSet.setId(optionSetId);
    Option option = new Option();
    option.setOptionSet(optionSet);
    return option;
  }

  @Test
  void shouldInvalidateOwningOptionSetOnInsert() {
    when(postInsertEvent.getEntity()).thenReturn(optionOf(42L));

    listener.onPostInsert(postInsertEvent);

    verify(mapperService).invalidateOptionsCache(42L);
  }

  @Test
  void shouldInvalidateOwningOptionSetOnUpdate() {
    when(postUpdateEvent.getEntity()).thenReturn(optionOf(7L));

    listener.onPostUpdate(postUpdateEvent);

    verify(mapperService).invalidateOptionsCache(7L);
  }

  @Test
  void shouldInvalidateOwningOptionSetOnDelete() {
    when(postDeleteEvent.getEntity()).thenReturn(optionOf(13L));

    listener.onPostDelete(postDeleteEvent);

    verify(mapperService).invalidateOptionsCache(13L);
  }

  @Test
  void shouldIgnoreNonOptionEntities() {
    when(postInsertEvent.getEntity()).thenReturn(new Program());

    listener.onPostInsert(postInsertEvent);

    verify(mapperService, never()).invalidateOptionsCache(anyLong());
  }

  @Test
  void shouldIgnoreOptionWithoutAnOptionSet() {
    when(postInsertEvent.getEntity()).thenReturn(new Option());

    listener.onPostInsert(postInsertEvent);

    verify(mapperService, never()).invalidateOptionsCache(anyLong());
  }
}
