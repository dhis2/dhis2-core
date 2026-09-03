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
package org.hisp.dhis.period.hibernate;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriodStore;
import org.hisp.dhis.translation.JsonTranslations;
import org.hisp.dhis.translation.Translation;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Repository;

@Repository
public class HibernateRelativePeriodStore implements RelativePeriodStore {

  private final EntityManager entityManager;
  private final DataSource dataSource;

  public HibernateRelativePeriodStore(EntityManager entityManager, DataSource dataSource) {
    this.entityManager = entityManager;
    this.dataSource = dataSource;
  }

  private <R> R runAutoJoinTransaction(Function<StatelessSession, R> query) {
    return HibernateGenericStore.runAutoJoinTransaction(
        dataSource, getSession().getSessionFactory(), query);
  }

  @Override
  public boolean updateLabel(
      @Nonnull RelativePeriodEnum name, @CheckForNull String label, @CheckForNull Locale locale) {
    if (locale == null) {
      String sql =
          """
        UPDATE relativeperiod
        SET label = :label
        WHERE name = :name""";

      String newValue = isBlank(label) ? null : label;
      return runAutoJoinTransaction(
              session ->
                  session
                      .createNativeQuery(sql)
                      .setParameter("name", name.name())
                      .setParameter("label", newValue)
                      .executeUpdate())
          > 0;
    }
    // erase potentially existing value
    String sql =
        """
      UPDATE relativeperiod
      SET translations = (
        SELECT jsonb_agg(elem)
        FROM jsonb_array_elements(translations) AS elem
        WHERE elem->>'locale' <> :locale
      )
      WHERE name = :name""";
    boolean erased =
        runAutoJoinTransaction(
                session ->
                    session
                        .createNativeQuery(sql)
                        .setParameter("name", name.name())
                        .setParameter("locale", locale.toString())
                        .executeUpdate())
            > 0;
    // now insert the language object
    if (isBlank(label)) return erased;
    String sql2 =
        """
      UPDATE relativeperiod
      SET translations = translations ||
        jsonb_build_array(jsonb_build_object('locale',:locale,'property','NAME','value',:value))
      WHERE name = :name""";
    boolean inserted =
        runAutoJoinTransaction(
                session ->
                    session
                        .createNativeQuery(sql2)
                        .setParameter("name", name.name())
                        .setParameter("locale", locale.toString())
                        .setParameter("value", label)
                        .executeUpdate())
            > 0;
    return erased || inserted;
  }

  @Override
  public boolean updateLabel(
      @Nonnull RelativePeriodEnum name, @Nonnull Collection<Translation> translations) {
    List<Translation> keep = translations.stream().filter(t -> isNotBlank(t.getValue())).toList();
    if (keep.isEmpty()) {
      String sql =
          """
        UPDATE relativeperiod
        SET translations = '[]'
        WHERE name = :name
        """;
      return runAutoJoinTransaction(
              session ->
                  session.createNativeQuery(sql).setParameter("name", name.name()).executeUpdate())
          > 0;
    }
    String sql = createLabelUpdateQuery(keep);
    return runAutoJoinTransaction(
            session -> {
              NativeQuery<?> query =
                  session.createNativeQuery(sql).setParameter("name", name.name());
              int i = 0;
              for (Translation t : keep) {
                query.setParameter("locale" + i, t.getLocale().toString());
                query.setParameter("value" + i, t.getValue());
                i++;
              }
              return query.executeUpdate();
            })
        > 0;
  }

  @Nonnull
  private static String createLabelUpdateQuery(List<Translation> translations) {
    @Language("sql")
    String sql1 =
        """
    UPDATE relativeperiod
    SET translations = jsonb_build_array(
      jsonb_build_object('locale',:locale,'property','NAME','value',:value )
    )
    where name = :name""";
    StringBuilder json = new StringBuilder();
    for (int k = 0; k < translations.size(); k++) {
      if (k > 0) json.append(",\n");
      json.append(
          "jsonb_build_object('locale',:locale%d,'property','NAME','value',:value%d)"
              .formatted(k, k));
    }
    return sql1.replace(
        "jsonb_build_object('locale',:locale,'property','NAME','value',:value )", json);
  }

  @Override
  public List<Labels> getAllLabels() {
    String sql = "SELECT name, label, translations #>> '{}' FROM relativeperiod";
    return runAutoJoinTransaction(
        session ->
            session.createNativeQuery(sql).stream()
                .map(HibernateRelativePeriodStore::toLabels)
                .toList());
  }

  private static Labels toLabels(Object row) {
    if (!(row instanceof Object[] columns))
      throw new IllegalArgumentException("Relative period labels must be an Object[]");
    return new Labels(
        RelativePeriodEnum.valueOf((String) columns[0]),
        (String) columns[1],
        JsonMixed.of((String) columns[2]).as(JsonTranslations.class));
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }
}
