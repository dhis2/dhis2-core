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
package org.hisp.dhis.dataintegrity.hibernate;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dataintegrity.DataIntegrityCheck;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue;
import org.hisp.dhis.dataintegrity.DataIntegrityStore;
import org.hisp.dhis.dataintegrity.DataIntegritySummary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * As we want each check to be its own transaction the @{@link Transactional} annotation is used on
 * the store and not the service level in this case.
 *
 * @author Jan Bernitt
 */
@Repository
@RequiredArgsConstructor
public class HibernateDataIntegrityStore implements DataIntegrityStore {
  private final SessionFactory sessionFactory;

  @Override
  @Transactional(readOnly = true)
  public DataIntegritySummary querySummary(DataIntegrityCheck check, String sql) {
    Date startTime = new Date();
    Object summary = sessionFactory.getCurrentSession().createNativeQuery(sql).getSingleResult();
    return new DataIntegritySummary(
        check, startTime, new Date(), null, parseCount(summary), parsePercentage(summary));
  }

  @Override
  @Transactional(readOnly = true)
  public DataIntegrityDetails queryDetails(DataIntegrityCheck check, String sql) {
    Date startTime = new Date();
    @SuppressWarnings("unchecked")
    List<Object[]> rows = sessionFactory.getCurrentSession().createNativeQuery(sql).getResultList();
    return new DataIntegrityDetails(
        check,
        startTime,
        new Date(),
        null,
        rows.stream()
            .map(
                row ->
                    new DataIntegrityIssue(
                        getIndex(row, 0), getIndex(row, 1), getIndex(row, 2), getRefs(row, 3)))
            .collect(toUnmodifiableList()));
  }

  private static String getIndex(Object[] row, int index) {
    return row.length <= index ? null : (String) row[index];
  }

  private static List<String> getRefs(Object[] row, int index) {
    return row.length <= index ? null : List.of((String[]) row[index]);
  }

  private static Double parsePercentage(Object value) {
    if (!(value instanceof Object[])) {
      return null;
    }
    Object[] row = (Object[]) value;
    if (row.length < 2 || row[1] == null) {
      return null;
    }
    value = row[1];
    if (value instanceof String) {
      return Double.parseDouble(value.toString().replace("%", ""));
    }
    return ((Number) value).doubleValue();
  }

  private static int parseCount(Object value) {
    if (value instanceof Object[]) {
      Object[] row = (Object[]) value;
      return row.length == 0 ? -1 : parseCount(row[0]);
    }
    if (value == null) {
      return 0;
    }
    if (value instanceof String) {
      return Integer.parseInt((String) value);
    }
    return ((Number) value).intValue();
  }
}
