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
package org.hisp.dhis.dxf2.deprecated.tracker.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class JdbcEventCommentStoreTest {

  private JdbcEventCommentStore jdbcEventCommentStore;

  @BeforeEach
  public void setUp() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    JdbcEventCommentStore jdbcEventCommentStore = new JdbcEventCommentStore(jdbcTemplate);
    this.jdbcEventCommentStore = Mockito.spy(jdbcEventCommentStore);
    doReturn(1L).when(this.jdbcEventCommentStore).saveComment(any());
    doNothing().when(this.jdbcEventCommentStore).saveCommentToEvent(anyLong(), anyLong(), anyInt());
  }

  @Test
  void verifyPSITableIsNotQueriedWhenNoComments() {
    List<Event> eventList = getProgramStageList(false);
    jdbcEventCommentStore.saveAllComments(eventList);
    verify(jdbcEventCommentStore, never()).getInitialSortOrder(any());
  }

  @Test
  void verifyPSITableIsNotQueriedWhenCommentsTextEmpty() {
    List<Event> eventList = getProgramStageList(true, true);
    jdbcEventCommentStore.saveAllComments(eventList);
    verify(jdbcEventCommentStore, never()).getInitialSortOrder(any());
  }

  @Test
  void verifyPSITableIsQueriedWhenComments() {
    List<Event> eventList = getProgramStageList(true);
    jdbcEventCommentStore.saveAllComments(eventList);
    verify(jdbcEventCommentStore).getInitialSortOrder(any());
  }

  private List<Event> getProgramStageList(boolean withComments) {
    return getProgramStageList(withComments, false);
  }

  private List<Event> getProgramStageList(boolean withComments, boolean emptyComment) {
    Event event = new Event();
    if (withComments) {
      event.setComments(List.of(getComment(emptyComment ? "" : "Some comment")));
    }
    return List.of(event);
  }

  private TrackedEntityComment getComment(String commentText) {
    return new TrackedEntityComment(commentText, "Some author");
  }
}
