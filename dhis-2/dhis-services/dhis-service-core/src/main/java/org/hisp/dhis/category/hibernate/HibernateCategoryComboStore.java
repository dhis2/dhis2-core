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
package org.hisp.dhis.category.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import java.util.Collection;
import java.util.List;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboStore;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Lars Helge Overland
 */
@Repository("org.hisp.dhis.category.CategoryComboStore")
public class HibernateCategoryComboStore extends HibernateIdentifiableObjectStore<CategoryCombo>
    implements CategoryComboStore {
  public HibernateCategoryComboStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, CategoryCombo.class, aclService, true);
  }

  @Override
  public List<CategoryCombo> getCategoryCombosByDimensionType(DataDimensionType dataDimensionType) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("dataDimensionType"), dataDimensionType))
            .addPredicate(root -> builder.equal(root.get("name"), "default")));
  }

  @Override
  public void preloadCategoryComboAssociations(Collection<CategoryCombo> categoryCombos) {
    if (categoryCombos.isEmpty()) {
      return;
    }

    // One query per collection of CategoryCombo: fetching categories and optionCombos in the same
    // query would join the two independently and return their Cartesian product.
    preload(
        """
        select cc from CategoryCombo cc
        left join fetch cc.categories c
        left join fetch c.categoryOptions
        where cc in :categoryCombos
        """,
        CategoryCombo.class,
        categoryCombos);

    // The two queries below cannot be merged, and their order matters. CategoryCombo.optionCombos
    // is a Set, so filling it calls CategoryOptionCombo.hashCode(), which reads categoryOptions:
    // fetching both in one query fails with "collection was evicted", and fetching optionCombos on
    // its own would lazy-load each combo's category options one at a time. So the category options
    // are put in the session first, rooted at the option combo, and only then are the optionCombos
    // sets filled.
    preload(
        """
        select coc from CategoryCombo cc
        join cc.optionCombos coc
        left join fetch coc.categoryOptions
        where cc in :categoryCombos
        """,
        CategoryOptionCombo.class,
        categoryCombos);
    preload(
        """
        select cc from CategoryCombo cc
        left join fetch cc.optionCombos
        where cc in :categoryCombos
        """,
        CategoryCombo.class,
        categoryCombos);
  }

  /** Runs a query only for its side effect of initialising the collections it fetch-joins. */
  private <C> void preload(
      String hql, Class<C> resultType, Collection<CategoryCombo> categoryCombos) {
    getQuery(hql, resultType).setParameter("categoryCombos", categoryCombos).list();
  }
}
