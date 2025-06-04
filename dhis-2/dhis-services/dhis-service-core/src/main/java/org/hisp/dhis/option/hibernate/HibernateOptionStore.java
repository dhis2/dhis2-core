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
package org.hisp.dhis.option.hibernate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import org.hibernate.query.Query;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Chau Thu Tran
 */
@Repository("org.hisp.dhis.option.OptionStore")
public class HibernateOptionStore extends HibernateIdentifiableObjectStore<Option>
    implements OptionStore {
  public HibernateOptionStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Option.class, aclService, true);
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  public List<Option> findOptionsByNamePattern(UID optionSet, String infix, Integer maxResults) {
    String hql =
        "select option from OptionSet as optionset "
            + "join optionset.options as option where optionset.uid = :optionSetId ";

    if (infix != null && !infix.isEmpty()) {
      hql += "and lower(option.name) like lower('%" + infix + "%') ";
    }

    hql += "order by option.sortOrder";

    Query<Option> query = getQuery(hql);
    query.setParameter("optionSetId", optionSet.getValue());

    if (maxResults != null) {
      query.setMaxResults(maxResults);
    }

    return query.list();
  }

  @Override
  public Optional<Option> findOptionByCode(@Nonnull UID optionSet, @Nonnull String code) {
    String sql =
        """
      select * from optionvalue
      where optionsetid = (select optionsetid from optionset s where s.uid = :uid)
      and code = :code""";
    @SuppressWarnings("unchecked")
    List<Option> options =
        nativeSynchronizedQuery(sql)
            .setParameter("uid", optionSet.getValue())
            .setParameter("code", code)
            .list();
    return options.isEmpty() ? Optional.empty() : Optional.of(options.get(0));
  }

  @Override
  public boolean existsAllOptions(@Nonnull UID optionSet, @Nonnull Collection<String> codes) {
    String sql =
        """
      select count(*) from optionvalue
      where optionsetid = (select optionsetid from optionset s where s.uid = :uid)
      and code in :codes
      """;
    Object res =
        nativeSynchronizedQuery(sql)
            .setParameter("uid", optionSet.getValue())
            .setParameterList("codes", codes)
            .getSingleResult();
    return res instanceof Number n && n.intValue() == codes.size();
  }
}
