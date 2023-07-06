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
package org.hisp.dhis.system.deletion;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ObjectDeletionRequestedEvent;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: Add support for failed allow tests on "transitive" deletion handlers which are called as
 * part of delete methods.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component("deletionManager")
public class DefaultDeletionManager implements DeletionManager {

  @SuppressWarnings("rawtypes")
  private static final Queue EMPTY = new LinkedList();

  private final ConcurrentMap<Class<?>, Queue<Function<?, DeletionVeto>>> vetoHandlersByType =
      new ConcurrentHashMap<>();

  private final ConcurrentMap<Class<?>, Queue<Consumer<?>>> deletionHandlersByType =
      new ConcurrentHashMap<>();

  @Override
  public <T extends IdentifiableObject> void whenVetoing(
      Class<T> type, Function<T, DeletionVeto> vetoFunction) {
    vetoHandlersByType
        .computeIfAbsent(type, key -> new ConcurrentLinkedQueue<>())
        .add(vetoFunction);
  }

  @Override
  public <T extends IdentifiableObject> void whenDeleting(Class<T> type, Consumer<T> action) {
    deletionHandlersByType.computeIfAbsent(type, key -> new ConcurrentLinkedQueue<>()).add(action);
  }

  @Override
  public <T extends EmbeddedObject> void whenDeletingEmbedded(Class<T> type, Consumer<T> action) {
    deletionHandlersByType.computeIfAbsent(type, key -> new ConcurrentLinkedQueue<>()).add(action);
  }

  @Override
  @Transactional
  @EventListener(condition = "#event.shouldRollBack")
  public void onDeletion(ObjectDeletionRequestedEvent event) {
    deleteObjects(event.getSource());
  }

  @Override
  @Transactional(noRollbackFor = DeleteNotAllowedException.class)
  @EventListener(condition = "!#event.shouldRollBack")
  public void onDeletionWithoutRollBack(ObjectDeletionRequestedEvent event) {
    deleteObjects(event.getSource());
  }

  private <T> void deleteObjects(T object) {
    Class<T> clazz = getClazz(object);
    @SuppressWarnings({"rawtypes", "unchecked"})
    Queue<Function<T, DeletionVeto>> vetoHandlers =
        (Queue) vetoHandlersByType.getOrDefault(clazz, EMPTY);
    @SuppressWarnings({"rawtypes", "unchecked"})
    Queue<Consumer<T>> deletionHandlers = (Queue) deletionHandlersByType.getOrDefault(clazz, EMPTY);
    if (vetoHandlers.isEmpty() && deletionHandlers.isEmpty()) {
      log.debug("No deletion handlers registered, aborting deletion handling");
      return;
    }

    log.debug("Veto handlers detected: " + vetoHandlers.size());
    log.debug("Deletion handlers detected: " + deletionHandlers.size());

    String className = clazz.getSimpleName();

    // ---------------------------------------------------------------------
    // Verify that object is allowed to be deleted
    // ---------------------------------------------------------------------

    String handlerName = "";
    try {
      for (Function<T, DeletionVeto> handler : vetoHandlers) {
        handlerName = handler.toString();
        log.debug("Check if allowed using " + handlerName + " for class " + className);

        DeletionVeto veto = handler.apply(object);

        if (veto.isVetoed()) {
          ErrorMessage errorMessage = new ErrorMessage(ErrorCode.E4030, veto.getMessage());

          log.debug("Delete was not allowed by " + handlerName + ": " + errorMessage.toString());

          throw new DeleteNotAllowedException(errorMessage);
        }
      }
    } catch (DeleteNotAllowedException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Deletion failed, veto handler '" + handlerName + "' threw an exception: ", ex);
      return;
    }

    // ---------------------------------------------------------------------
    // Delete associated objects
    // ---------------------------------------------------------------------

    handlerName = "";
    try {
      for (Consumer<T> handler : deletionHandlers) {
        handlerName = handler.toString();

        log.debug("Deleting object using " + handlerName + " for class " + className);

        handler.accept(object);
      }
    } catch (Exception ex) {
      log.error("Deletion failed, deletion handler '" + handlerName + "' threw an exception: ", ex);
      return;
    }

    log.debug("Deleted objects associated with object of type " + className);
  }

  @SuppressWarnings("unchecked")
  private <T> Class<T> getClazz(T object) {
    return HibernateProxyUtils.getRealClass(object);
  }
}
