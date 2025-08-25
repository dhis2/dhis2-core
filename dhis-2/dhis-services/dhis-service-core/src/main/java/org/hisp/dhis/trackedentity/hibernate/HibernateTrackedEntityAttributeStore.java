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
package org.hisp.dhis.trackedentity.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew Gizaw
 */
@Repository("org.hisp.dhis.trackedentity.TrackedEntityAttributeStore")
public class HibernateTrackedEntityAttributeStore
    extends HibernateIdentifiableObjectStore<TrackedEntityAttribute>
    implements TrackedEntityAttributeStore {

  public HibernateTrackedEntityAttributeStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, TrackedEntityAttribute.class, aclService, true);
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  public List<TrackedEntityAttribute> getByDisplayOnVisitSchedule(boolean displayOnVisitSchedule) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(
                root -> builder.equal(root.get("displayOnVisitSchedule"), displayOnVisitSchedule)));
  }

  @Override
  public List<TrackedEntityAttribute> getDisplayInListNoProgram() {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("displayInListNoProgram"), true)));
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Set<TrackedEntityAttribute> getTrackedEntityAttributesByTrackedEntityTypes() {
    TypedQuery<TrackedEntityTypeAttribute> query =
        entityManager.createQuery(
            "select distinct tea from TrackedEntityType tet inner join tet.trackedEntityTypeAttributes tea",
            TrackedEntityTypeAttribute.class);

    Set<TrackedEntityTypeAttribute> trackedEntityTypeAttributes =
        new HashSet<>(query.getResultList());

    return trackedEntityTypeAttributes.stream()
        .map(TrackedEntityTypeAttribute::getTrackedEntityAttribute)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<TrackedEntityAttribute> getAllTrigramIndexableTrackedEntityAttributes() {
    List<Object[]> results =
        getSession()
            .createNativeQuery(
                """
        SELECT tea.trackedentityattributeid, tea.uid, tea.name
        FROM trackedentityattribute tea
        WHERE tea.trigramindexable = true
        AND (
            NOT tea.blockedsearchoperators @> CAST('["LIKE"]' AS jsonb)
            OR NOT tea.blockedsearchoperators @> CAST('["EW"]' AS jsonb)
        )
        """)
            .getResultList();

    Set<TrackedEntityAttribute> trackedEntityAttributes = new HashSet<>();
    for (Object[] row : results) {
      TrackedEntityAttribute tea = new TrackedEntityAttribute();
      tea.setId(((BigInteger) row[0]).intValue());
      tea.setUid((String) row[1]);
      tea.setName((String) row[2]);
      trackedEntityAttributes.add(tea);
    }

    return trackedEntityAttributes;
  }

  @Override
  public Set<UID> getAllTrigramIndexedTrackedEntityAttributes() {
    List<String> result =
        entityManager
            .createNativeQuery(
                """
        select tea.uid
        from pg_indexes idx
        join trackedentityattribute tea
          on cast(substring(idx.indexdef from 'trackedentityattributeid\\s*=\\s*(\\d+)') as bigint) = tea.trackedentityattributeid
         and idx.tablename = 'trackedentityattributevalue'
         and idx.indexdef ilike '%gin_trgm_ops%'
         and idx.indexdef ilike '%WHERE%'
         and idx.indexdef ~ '[(]?\\s*trackedentityattributeid\\s*=\\s*\\d+\\s*[)]?'
    """)
            .getResultList();

    return result.stream().map(UID::of).collect(Collectors.toSet());
  }

  @Override
  public Set<String> getTrackedEntityAttributesInProgram(Program program) {
    TypedQuery<String> query =
        entityManager.createQuery(
            "select distinct pa.attribute.uid from Program p inner join p.programAttributes pa where p.uid = :program",
            String.class);
    query.setParameter("program", program.getUid());

    return new HashSet<>(query.getResultList());
  }
}
