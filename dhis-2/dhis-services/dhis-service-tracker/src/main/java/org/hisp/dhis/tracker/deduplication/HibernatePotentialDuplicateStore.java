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
package org.hisp.dhis.tracker.deduplication;

import static org.hisp.dhis.changelog.ChangeLogType.CREATE;
import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;
import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerStore;
import org.hisp.dhis.tracker.export.trackedentity.HibernateTrackedEntityChangeLogStore;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// This class is annotated with @Component instead of @Repository because @Repository creates a
// proxy that can't be used to inject the class.
@Component("org.hisp.dhis.tracker.deduplication.HibernatePotentialDuplicateStore")
class HibernatePotentialDuplicateStore
    extends HibernateIdentifiableObjectStore<PotentialDuplicate> {
  private final AuditManager auditManager;

  private final HibernateTrackedEntityChangeLogStore hibernateTrackedEntityChangeLogStore;

  private final TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore;

  private final DhisConfigurationProvider config;

  public HibernatePotentialDuplicateStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService,
      AuditManager auditManager,
      HibernateTrackedEntityChangeLogStore hibernateTrackedEntityChangeLogStore,
      TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore,
      DhisConfigurationProvider config) {
    super(entityManager, jdbcTemplate, publisher, PotentialDuplicate.class, aclService, false);
    this.auditManager = auditManager;
    this.hibernateTrackedEntityChangeLogStore = hibernateTrackedEntityChangeLogStore;
    this.trackedEntityProgramOwnerStore = trackedEntityProgramOwnerStore;
    this.config = config;
  }

  public List<PotentialDuplicate> getPotentialDuplicates(PotentialDuplicateCriteria criteria) {
    TypedQuery<PotentialDuplicate> query = getQuery(criteria);
    return query.getResultList();
  }

  public Page<PotentialDuplicate> getPotentialDuplicates(
      PotentialDuplicateCriteria criteria, PageParams pageParams) {
    if (pageParams.isPageTotal()) {
      throw new UnsupportedOperationException("pageTotal is not supported");
    }

    TypedQuery<PotentialDuplicate> query = getQuery(criteria);

    query.setFirstResult(pageParams.getOffset());
    query.setMaxResults(
        pageParams.getPageSize() + 1); // get extra item to determine if there is a nextPage

    return new Page<>(query.getResultList(), pageParams);
  }

  private TypedQuery<PotentialDuplicate> getQuery(PotentialDuplicateCriteria criteria) {
    CriteriaBuilder cb = getCriteriaBuilder();
    CriteriaQuery<PotentialDuplicate> cq = cb.createQuery(PotentialDuplicate.class);
    Root<PotentialDuplicate> root = cq.from(PotentialDuplicate.class);
    cq.where(getQueryPredicates(criteria, cb, root));
    cq.orderBy(
        criteria.getOrder().stream()
            .map(
                order ->
                    order.getDirection().isAscending()
                        ? cb.asc(root.get(order.getField()))
                        : cb.desc(root.get(order.getField())))
            .toList());
    return entityManager.createQuery(cq);
  }

  private Predicate[] getQueryPredicates(
      PotentialDuplicateCriteria query, CriteriaBuilder builder, Root<PotentialDuplicate> root) {
    List<Predicate> predicateList = new ArrayList<>();

    predicateList.add(root.get("status").in(getInStatusValue(query.getStatus())));

    if (!query.getTrackedEntities().isEmpty()) {
      predicateList.add(
          builder.and(
              builder.or(
                  root.get("original").in(query.getTrackedEntities()),
                  root.get("duplicate").in(query.getTrackedEntities()))));
    }

    return predicateList.toArray(new Predicate[0]);
  }

  private List<DeduplicationStatus> getInStatusValue(DeduplicationStatus status) {
    return status == DeduplicationStatus.ALL
        ? Arrays.stream(DeduplicationStatus.values())
            .filter(s -> s != DeduplicationStatus.ALL)
            .toList()
        : Collections.singletonList(status);
  }

  @SuppressWarnings("unchecked")
  public boolean exists(PotentialDuplicate potentialDuplicate)
      throws PotentialDuplicateConflictException {
    if (potentialDuplicate.getOriginal() == null || potentialDuplicate.getDuplicate() == null) {
      throw new PotentialDuplicateConflictException(
          "Can't search for pair of potential duplicates: original and duplicate must not be null");
    }

    NativeQuery<BigInteger> query =
        nativeSynchronizedQuery(
            """
            select count(potentialduplicateid) from potentialduplicate pd where (pd.original =\
             :original and pd.duplicate = :duplicate) or (pd.original = :duplicate and\
             pd.duplicate = :original)""");

    query.setParameter("original", potentialDuplicate.getOriginal().getValue());
    query.setParameter("duplicate", potentialDuplicate.getDuplicate().getValue());

    return query.getSingleResult().intValue() != 0;
  }

  public void moveTrackedEntityAttributeValues(
      TrackedEntity original, TrackedEntity duplicate, Set<UID> trackedEntityAttributes) {
    // Collect existing teav from original for the tea list
    Map<String, TrackedEntityAttributeValue> originalAttributeValueMap = new HashMap<>();
    original
        .getTrackedEntityAttributeValues()
        .forEach(
            oav -> {
              if (trackedEntityAttributes.contains(UID.of(oav.getAttribute()))) {
                originalAttributeValueMap.put(oav.getAttribute().getUid(), oav);
              }
            });

    duplicate.getTrackedEntityAttributeValues().stream()
        .filter(av -> trackedEntityAttributes.contains(UID.of(av.getAttribute())))
        .forEach(
            dav -> {
              String previousValue = null;
              TrackedEntityAttributeValue updatedTeav;
              ChangeLogType changeLogType;
              if (originalAttributeValueMap.containsKey(dav.getAttribute().getUid())) {
                // Teav exists in original, overwrite the value
                updatedTeav = originalAttributeValueMap.get(dav.getAttribute().getUid());
                previousValue = updatedTeav.getPlainValue();
                updatedTeav.setValue(dav.getPlainValue());
                changeLogType = UPDATE;
              } else {
                // teav does not exist in original, so create new and attach
                // it to original
                updatedTeav = new TrackedEntityAttributeValue();
                updatedTeav.setAttribute(dav.getAttribute());
                updatedTeav.setTrackedEntity(original);
                updatedTeav.setValue(dav.getPlainValue());
                changeLogType = CREATE;
              }
              getSession().delete(dav);
              // We need to flush to make sure the previous teav is
              // deleted.
              // Or else we might end up breaking a
              // constraint, since hibernate does not respect order.
              getSession().flush();

              getSession().saveOrUpdate(updatedTeav);

              addTrackedEntityChangeLog(updatedTeav, previousValue, changeLogType);
            });
  }

  private void addTrackedEntityChangeLog(
      TrackedEntityAttributeValue createOrUpdateTeav,
      String previousValue,
      ChangeLogType changeLogType) {
    String currentUsername = CurrentUserUtil.getCurrentUsername();

    TrackedEntityChangeLog updatedTrackedEntityChangeLog =
        new TrackedEntityChangeLog(
            createOrUpdateTeav.getTrackedEntity(),
            createOrUpdateTeav.getAttribute(),
            previousValue,
            createOrUpdateTeav.getPlainValue(),
            changeLogType,
            new Date(),
            currentUsername);

    if (config.isEnabled(CHANGELOG_TRACKER)) {
      hibernateTrackedEntityChangeLogStore.addTrackedEntityChangeLog(updatedTrackedEntityChangeLog);
    }
  }

  public void moveRelationships(
      TrackedEntity original, TrackedEntity duplicate, Set<UID> relationships) {
    List<RelationshipItem> duplicateRelationshipItems =
        duplicate.getRelationshipItems().stream()
            .filter(r -> relationships.contains(UID.of(r.getRelationship())))
            .toList();
    duplicateRelationshipItems.forEach(
        ri -> {
          ri.setTrackedEntity(original);
          original.getRelationshipItems().add(ri);
          getSession().update(ri);
        });

    duplicateRelationshipItems.forEach(duplicate.getRelationshipItems()::remove);
  }

  public void moveEnrollments(
      TrackedEntity original, TrackedEntity duplicate, Set<UID> enrollments) {
    List<Enrollment> enrollmentList =
        duplicate.getEnrollments().stream()
            .filter(e -> !e.isDeleted())
            .filter(e -> enrollments.contains(UID.of(e)))
            .toList();

    enrollmentList.forEach(duplicate.getEnrollments()::remove);

    User currentUser =
        entityManager.getReference(User.class, CurrentUserUtil.getCurrentUserDetails().getId());

    enrollmentList.forEach(
        e -> {
          e.setTrackedEntity(original);
          e.setLastUpdatedBy(currentUser);
          e.setLastUpdatedByUserInfo(
              UserInfoSnapshot.from(CurrentUserUtil.getCurrentUserDetails()));
          e.setLastUpdated(new Date());
          getSession().update(e);
          TrackedEntityProgramOwner trackedEntityProgramOwner =
              trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(
                  duplicate, e.getProgram());
          trackedEntityProgramOwner.setTrackedEntity(original);
          trackedEntityProgramOwnerStore.update(trackedEntityProgramOwner);
        });

    // Flush to update records before we delete duplicate, or else it might
    // be soft-deleted by hibernate.
    getSession().flush();
  }

  public void auditMerge(DeduplicationMergeParams params) {
    TrackedEntity duplicate = params.getDuplicate();
    MergeObject mergeObject = params.getMergeObject();

    mergeObject
        .getRelationships()
        .forEach(
            rel ->
                duplicate.getRelationshipItems().stream()
                    .map(RelationshipItem::getRelationship)
                    .filter(r -> r.getUid().equals(rel.getValue()))
                    .findAny()
                    .ifPresent(
                        relationship ->
                            auditManager.send(
                                Audit.builder()
                                    .auditScope(AuditScope.TRACKER)
                                    .auditType(AuditType.UPDATE)
                                    .createdAt(LocalDateTime.now())
                                    .object(relationship)
                                    .klass(
                                        HibernateProxyUtils.getRealClass(relationship)
                                            .getCanonicalName())
                                    .uid(rel.getValue())
                                    .auditableEntity(
                                        new AuditableEntity(Relationship.class, relationship))
                                    .build())));
  }
}
