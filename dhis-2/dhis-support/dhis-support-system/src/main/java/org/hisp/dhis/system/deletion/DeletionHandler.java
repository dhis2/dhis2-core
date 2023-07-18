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
import javax.annotation.PostConstruct;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A DeletionHandler should override methods for objects that, when deleted, will affect the current
 * object in any way. Eg. a DeletionHandler for DataElementGroup should override the
 * deleteDataElement(..) method which should remove the DataElement from all DataElementGroups.
 * Also, it should override the allowDeleteDataElement() method and return a non-null String value
 * if there exists objects that are dependent on the DataElement and are considered not be deleted.
 * The return value could be a hint for which object is denying the deletion, like the name.
 *
 * @author Lars Helge Overland
 */
public abstract class DeletionHandler {
  private DeletionManager manager;

  @Autowired
  public void setManager(DeletionManager manager) {
    this.manager = manager;
  }

  protected final <T extends IdentifiableObject> void whenVetoing(
      Class<T> type, Function<T, DeletionVeto> vetoFunction) {
    manager.whenVetoing(type, vetoFunction);
  }

  protected final <T extends IdentifiableObject> void whenDeleting(
      Class<T> type, Consumer<T> action) {
    manager.whenDeleting(type, action);
  }

  protected final <T extends EmbeddedObject> void whenDeletingEmbedded(
      Class<T> type, Consumer<T> action) {
    manager.whenDeletingEmbedded(type, action);
  }

  @PostConstruct
  public final void init() {
    register();
  }

  protected abstract void register();
}
