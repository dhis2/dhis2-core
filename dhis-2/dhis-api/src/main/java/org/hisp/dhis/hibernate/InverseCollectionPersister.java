/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.hibernate;

import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * Collection persister for a many-to-many collection that is read but never written, the annotation
 * equivalent of {@code inverse="true"} in a {@code hbm.xml} mapping.
 *
 * <p>JPA marks a collection as inverse only through {@code mappedBy}, which needs a property on the
 * other entity. Use this persister through {@link org.hibernate.annotations.Persister} when the
 * other entity has no such property. Hibernate then loads the join table rows but never inserts,
 * updates or deletes them, and the schema reports the property as non-owner.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class InverseCollectionPersister extends BasicCollectionPersister {
  public InverseCollectionPersister(
      Collection collectionBinding,
      CollectionDataAccess cacheAccessStrategy,
      PersisterCreationContext creationContext) {
    super(markInverse(collectionBinding), cacheAccessStrategy, creationContext);
  }

  private static Collection markInverse(Collection collectionBinding) {
    collectionBinding.setInverse(true);
    return collectionBinding;
  }
}
