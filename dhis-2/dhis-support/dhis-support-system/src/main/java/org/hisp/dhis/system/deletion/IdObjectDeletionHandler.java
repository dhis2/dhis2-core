/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.lang.reflect.ParameterizedType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This base deletion handler for {@link IdentifiableObject} implements method {@link
 * IdObjectDeletionHandler#allowDeleteUser(User)} by default. If there is any object has property
 * createdBy or lastUpdatedBy linked to deleting {@link User} then the deletion is vetoed.
 */
public abstract class IdObjectDeletionHandler<T extends IdentifiableObject>
    extends JdbcDeletionHandler {
  protected final DeletionVeto VETO;

  private final Class<T> klass;

  protected IdentifiableObjectManager idObjectManager;

  @Autowired
  public void setIdObjectManager(IdentifiableObjectManager idObjectManager) {
    this.idObjectManager = idObjectManager;
  }

  @SuppressWarnings("unchecked")
  protected IdObjectDeletionHandler() {
    this.klass =
        (Class<T>)
            (((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    this.VETO = new DeletionVeto(klass);
  }

  @Override
  protected final void register() {
    whenVetoing(User.class, this::allowDeleteUser);
    registerHandler();
  }

  protected abstract void registerHandler();

  private DeletionVeto allowDeleteUser(User user) {
    return idObjectManager.findByUser(klass, user).isEmpty() ? DeletionVeto.ACCEPT : VETO;
  }
}
