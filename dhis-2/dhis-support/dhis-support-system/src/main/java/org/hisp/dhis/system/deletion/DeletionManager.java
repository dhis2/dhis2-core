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

import java.util.function.Consumer;
import java.util.function.Function;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ObjectDeletionRequestedEvent;

/**
 * @author Lars Helge Overland
 */
public interface DeletionManager {
  String ID = DeletionManager.class.getName();

  /**
   * Register a handler for vetoing.
   *
   * @param type type of object about to be deleted
   * @param vetoFunction a {@link Function} that when given the object about to be deleted either
   *     produces a {@link DeletionVeto} or returns {@link DeletionVeto#ACCEPT}
   * @param <T> type of the object about to be deleted
   */
  <T extends IdentifiableObject> void whenVetoing(
      Class<T> type, Function<T, DeletionVeto> vetoFunction);

  /**
   * Register a handler to listen deletion of a given object type.
   *
   * @param type type of object being deleted
   * @param action action to perform then the object being deleted, accepting the deleted object
   * @param <T> type of the object being deleted
   */
  <T extends IdentifiableObject> void whenDeleting(Class<T> type, Consumer<T> action);

  /**
   * Register a handler to listen deletion of a given object type.
   *
   * @param type type of object being deleted
   * @param action action to perform then the object being deleted, accepting the deleted object
   * @param <T> type of the object being deleted
   */
  <T extends EmbeddedObject> void whenDeletingEmbedded(Class<T> type, Consumer<T> action);

  /**
   * Must be in the interface to allow spring to call the method.
   *
   * <p>This should not be called manually.
   *
   * @param event consumed event
   */
  void onDeletion(ObjectDeletionRequestedEvent event);

  /**
   * Must be in the interface to allow spring to call the method.
   *
   * <p>This should not be called manually.
   *
   * @param event consumed event
   */
  void onDeletionWithoutRollBack(ObjectDeletionRequestedEvent event);
}
