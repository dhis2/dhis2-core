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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.option.Option;
import org.springframework.stereotype.Component;

/**
 * Invalidates {@link DefaultProgramRuleEntityMapperService}'s per-option-set options cache whenever
 * an {@link Option} is inserted, updated, or deleted. Registered globally against Hibernate's
 * post-commit events by {@link OptionCacheInvalidationListenerConfigurer}, mirroring {@code
 * DeletedObjectPostDeleteEventListener}'s registration pattern.
 *
 * <p>This has to hook Hibernate directly rather than a service method: Option/OptionSet writes can
 * arrive via {@code DefaultOptionService}, the generic metadata CRUD controller, or a metadata
 * import, none of which funnel through a single call site that could invalidate the cache itself.
 *
 * <p>Same-node only - a write committed on another node in a cluster is not seen by this listener,
 * so cross-node staleness still falls back to the cache's TTL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptionCacheInvalidationListener
    implements PostCommitInsertEventListener,
        PostCommitUpdateEventListener,
        PostCommitDeleteEventListener {

  // Hibernate's PostInsert/PostUpdate/PostDeleteEventListener all extend Serializable, making this
  // class transitively Serializable even though it's just a Spring singleton registered with the
  // EventListenerRegistry and never actually serialized. transient satisfies that contract without
  // changing runtime behaviour.
  private final transient DefaultProgramRuleEntityMapperService mapperService;

  @Override
  public void onPostInsert(PostInsertEvent event) {
    invalidate(event.getEntity());
  }

  @Override
  public void onPostUpdate(PostUpdateEvent event) {
    invalidate(event.getEntity());
  }

  @Override
  public void onPostDelete(PostDeleteEvent event) {
    invalidate(event.getEntity());
  }

  private void invalidate(Object entity) {
    if (entity instanceof Option option && option.getOptionSet() != null) {
      // getId() never triggers a proxy load, even if the OptionSet association is lazy and
      // otherwise uninitialized at this point.
      mapperService.invalidateOptionsCache(option.getOptionSet().getId());
    }
  }

  @Override
  public boolean requiresPostCommitHanding(EntityPersister persister) {
    return true;
  }

  @Override
  public boolean requiresPostCommitHandling(EntityPersister persister) {
    return PostCommitUpdateEventListener.super.requiresPostCommitHandling(persister);
  }

  @Override
  public void onPostInsertCommitFailed(PostInsertEvent event) {
    log.debug("onPostInsertCommitFailed: " + event);
  }

  @Override
  public void onPostUpdateCommitFailed(PostUpdateEvent event) {
    log.debug("onPostUpdateCommitFailed: " + event);
  }

  @Override
  public void onPostDeleteCommitFailed(PostDeleteEvent event) {
    log.debug("onPostDeleteCommitFailed: " + event);
  }
}
