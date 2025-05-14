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
package org.hisp.dhis.dataelement.hibernate;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Repository("org.hisp.dhis.dataelement.DataElementOperandStore")
public class HibernateDataElementOperandStore
    extends HibernateIdentifiableObjectStore<DataElementOperand>
    implements DataElementOperandStore {
  public HibernateDataElementOperandStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, DataElementOperand.class, aclService, false);

    transientIdentifiableProperties = true;
  }

  @Nonnull
  @Override
  public List<DataElementOperand> getAllOrderedName() {
    return getQuery("from DataElementOperand d").list();
  }

  @Nonnull
  @Override
  public List<DataElementOperand> getAllOrderedName(int first, int max) {
    return getQuery("from DataElementOperand d").setFirstResult(first).setMaxResults(max).list();
  }

  @Override
  public List<DataElementOperand> getByDataElement(Collection<DataElement> dataElements) {
    return getQuery(
            """
            from DataElementOperand deo \
            where deo.dataElement in :dataElements""")
        .setParameter("dataElements", dataElements)
        .list();
  }

  @Override
  public List<DataElementOperand> getByCategoryOptionCombo(@Nonnull Collection<UID> uids) {
    if (uids.isEmpty()) return List.of();
    return getQuery(
            """
            select distinct deo from DataElementOperand deo \
            join deo.categoryOptionCombo coc \
            where coc.uid in :uids""")
        .setParameter("uids", UID.toValueList(uids))
        .getResultList();
  }
}
