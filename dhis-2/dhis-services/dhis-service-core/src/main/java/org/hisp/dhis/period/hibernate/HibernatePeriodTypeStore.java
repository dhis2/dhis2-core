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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.hibernate.HibernateAutoTransactionStore;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeStore;
import org.hisp.dhis.translation.JsonTranslations;
import org.hisp.dhis.translation.Translation;
import org.intellij.lang.annotations.Language;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HibernatePeriodTypeStore extends HibernateAutoTransactionStore<PeriodType>
    implements PeriodTypeStore {

  public HibernatePeriodTypeStore(
      EntityManager entityManager,
      DataSource dataSource,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher) {
    super(entityManager, dataSource, jdbcTemplate, publisher, PeriodType.class, false);
  }

  @Override
  public void addPeriodType(@Nonnull PeriodType periodType) {
    String name = periodType.getName();

    String sql1 = "SELECT periodtypeid from periodtype where name = :name";
    String sql2 =
        """
        INSERT INTO periodtype (periodtypeid, name)
        VALUES (nextval('hibernate_sequence'), :name)""";
    Object id =
        runAutoJoinTransaction(
            session -> {
              Object pk =
                  getSingleResult(session.createNativeQuery(sql1).setParameter("name", name));
              if (pk != null) {
                return pk;
              }
              session
                  .createNativeQuery(sql2)
                  // PeriodType, not the store's own Period
                  .addSynchronizedEntityClass(PeriodType.class)
                  .setParameter("name", name)
                  .executeUpdate();
              return session.createNativeQuery("SELECT lastval()").uniqueResult();
            });
    if (id instanceof Number n) {
      int periodTypeId = n.intValue();
      periodType.setId(periodTypeId);
      return;
    }
    throw new IllegalStateException("Failed to upsert period type: " + name);
  }

  @Override
  public boolean updatePeriodTypeLabel(
      @Nonnull String name, @CheckForNull String label, @CheckForNull Locale locale) {
    if (locale == null) {
      String sql =
          """
        UPDATE periodtype
        SET label = :label
        WHERE name = :name""";

      String newValue = isBlank(label) ? null : label;
      return runAutoJoinTransaction(
              session ->
                  session
                      .createNativeQuery(sql)
                      .addSynchronizedEntityClass(PeriodType.class)
                      .setParameter("name", name)
                      .setParameter("label", newValue)
                      .executeUpdate())
          > 0;
    }
    // erase potentially existing value
    String sql =
        """
      UPDATE periodtype
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
                        .addSynchronizedEntityClass(PeriodType.class)
                        .setParameter("name", name)
                        .setParameter("locale", locale.toString())
                        .executeUpdate())
            > 0;
    // now insert the language object
    if (isBlank(label)) return erased;
    String sql2 =
        """
      UPDATE periodtype
      SET translations = translations ||
        jsonb_build_array(jsonb_build_object('locale',:locale,'property','NAME','value',:value))
      WHERE name = :name""";
    boolean inserted =
        runAutoJoinTransaction(
                session ->
                    session
                        .createNativeQuery(sql2)
                        .addSynchronizedEntityClass(PeriodType.class)
                        .setParameter("name", name)
                        .setParameter("locale", locale.toString())
                        .setParameter("value", label)
                        .executeUpdate())
            > 0;
    return erased || inserted;
  }

  @Override
  public boolean updatePeriodTypeLabel(
      @Nonnull String name, @Nonnull Collection<Translation> translations) {
    List<Translation> keep = translations.stream().filter(t -> isNotBlank(t.getValue())).toList();
    if (keep.isEmpty()) {
      String sql =
          """
        UPDATE periodtype
        SET translations = '[]'
        WHERE name = :name
        """;
      return runAutoJoinTransaction(
              session ->
                  session
                      .createNativeQuery(sql)
                      .addSynchronizedEntityClass(PeriodType.class)
                      .setParameter("name", name)
                      .executeUpdate())
          > 0;
    }
    @Language("sql")
    String sql1 =
        """
    UPDATE periodtype
    SET translations = jsonb_build_array(
      jsonb_build_object('locale',:locale,'property','NAME','value',:value )
    )
    where name = :name""";
    StringBuilder json = new StringBuilder();
    for (int k = 0; k < keep.size(); k++) {
      if (k > 0) json.append(",\n");
      json.append(
          "jsonb_build_object('locale',:locale%d,'property','NAME','value',:value%d)"
              .formatted(k, k));
    }
    String sql =
        sql1.replace(
            "jsonb_build_object('locale',:locale,'property','NAME','value',:value )", json);
    return runAutoJoinTransaction(
            session -> {
              NativeQuery<?> query =
                  session
                      .createNativeQuery(sql)
                      .addSynchronizedEntityClass(PeriodType.class)
                      .setParameter("name", name);
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

  @Override
  public List<PeriodTypeLabels> getAllPeriodTypeLabels() {
    String sql = "SELECT name, label, translations #>> '{}' FROM periodtype";
    return runAutoJoinTransaction(
        session ->
            session.createNativeQuery(sql).stream()
                .map(HibernatePeriodTypeStore::toLabels)
                .toList());
  }

  private static PeriodTypeLabels toLabels(Object row) {
    if (!(row instanceof Object[] columns))
      throw new IllegalArgumentException("Period type labels must be an Object[]");
    return new PeriodTypeLabels(
        (String) columns[0],
        (String) columns[1],
        JsonMixed.of((String) columns[2]).as(JsonTranslations.class));
  }

  @Nonnull
  @Override
  public List<PeriodType> getAllPeriodTypes() {
    return getSession()
        .createNativeQuery("select * from periodtype order by name asc", PeriodType.class)
        .list();
  }
}
