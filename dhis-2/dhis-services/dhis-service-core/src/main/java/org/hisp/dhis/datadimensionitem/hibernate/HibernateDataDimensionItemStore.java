/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.datadimensionitem.hibernate;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.indicator.Indicator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author david mackessy
 */
@Repository
public class HibernateDataDimensionItemStore extends HibernateGenericStore<DataDimensionItem>
    implements org.hisp.dhis.datadimensionitem.DataDimensionItemStore {

  public HibernateDataDimensionItemStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, DataDimensionItem.class, false);
  }

  @Override
  public List<DataDimensionItem> getIndicatorDataDimensionItems(List<Indicator> indicators) {
    String sql =
        """
        select * from datadimensionitem d \
        where d.indicatorid in :indicators""";
    return nativeSynchronizedTypedQuery(sql).setParameter("indicators", indicators).list();
  }

  @Override
  public List<DataDimensionItem> getDataElementDataDimensionItems(List<DataElement> dataElements) {
    return getQuery(
            """
             from DataDimensionItem d
             where d.dataElement in :dataElements
             """)
        .setParameter("dataElements", dataElements)
        .getResultList();
  }

  @Override
  public int updateDeoCategoryOptionCombo(@Nonnull Collection<Long> cocIds, long newCocId) {
    if (cocIds.isEmpty()) return 0;

    String sql =
        """
        update datadimensionitem \
        set dataelementoperand_categoryoptioncomboid = %s \
        where dataelementoperand_categoryoptioncomboid in (%s)"""
            .formatted(
                newCocId, cocIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
    return jdbcTemplate.update(sql);
  }
}
