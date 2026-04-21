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
package org.hisp.dhis.reservedvalue.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.Objects.TRACKEDENTITYATTRIBUTE;
import static org.hisp.dhis.common.collection.CollectionUtils.isEmpty;

import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.Query;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.jdbc.batchhandler.ReservedValueBatchHandler;
import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.dhis.reservedvalue.ReservedValueStore;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Stian Sandvold
 */
@Repository("org.hisp.dhis.reservedvalue.ReservedValueStore")
@Slf4j
public class HibernateReservedValueStore extends HibernateGenericStore<ReservedValue>
    implements ReservedValueStore {
  private final BatchHandlerFactory batchHandlerFactory;

  public HibernateReservedValueStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      BatchHandlerFactory batchHandlerFactory) {
    super(entityManager, jdbcTemplate, publisher, ReservedValue.class, false);

    checkNotNull(batchHandlerFactory);

    this.batchHandlerFactory = batchHandlerFactory;
  }

  private static final String INSERT_AVAILABLE_VALUES_SQL =
      "INSERT INTO reservedvalue (reservedvalueid, ownerobject, owneruid, key, value, expirydate, created) "
          + "SELECT nextval('reservedvalue_sequence'), ?, ?, ?, v, ?, ? FROM unnest(?) AS v "
          + "WHERE NOT EXISTS ("
          + "SELECT 1 FROM trackedentityattributevalue "
          + "WHERE trackedentityattributeid = ? AND LOWER(value) = LOWER(v)) "
          + "AND NOT EXISTS ("
          + "SELECT 1 FROM reservedvalue "
          + "WHERE ownerobject = ? AND owneruid = ? AND key = ? AND LOWER(value) = LOWER(v)) "
          + "LIMIT ? ON CONFLICT DO NOTHING RETURNING value";

  @Override
  public List<String> insertAvailableValues(
      ReservedValue reservedValue, List<String> candidates, int limit) {
    if (isEmpty(candidates)) {
      return List.of();
    }

    return jdbcTemplate.query(
        conn -> {
          PreparedStatement ps = conn.prepareStatement(INSERT_AVAILABLE_VALUES_SQL);
          ps.setString(1, reservedValue.getOwnerObject());
          ps.setString(2, reservedValue.getOwnerUid());
          ps.setString(3, reservedValue.getKey());
          ps.setTimestamp(4, new Timestamp(reservedValue.getExpiryDate().getTime()));
          ps.setTimestamp(5, new Timestamp(reservedValue.getCreated().getTime()));
          ps.setArray(6, conn.createArrayOf("text", candidates.toArray()));
          ps.setLong(7, reservedValue.getTrackedEntityAttributeId());
          ps.setString(8, reservedValue.getOwnerObject());
          ps.setString(9, reservedValue.getOwnerUid());
          ps.setString(10, reservedValue.getKey());
          ps.setInt(11, limit);
          return ps;
        },
        (rs, rowNum) -> rs.getString("value"));
  }

  @Override
  public void reserveValues(List<ReservedValue> reservedValues) {
    try (BatchHandler<ReservedValue> batchHandler =
        batchHandlerFactory.createBatchHandler(ReservedValueBatchHandler.class).init()) {
      reservedValues.forEach(batchHandler::addObject);
      batchHandler.flush();
    } catch (Exception e) {
      log.error("Failed to reserve values", e);
    }
  }

  @Override
  public int getNumberOfUsedValues(ReservedValue reservedValue) {
    Query<Long> query =
        getTypedQuery("SELECT count(*) FROM ReservedValue WHERE owneruid = :uid AND key = :key");

    Long count =
        query
            .setParameter("uid", reservedValue.getOwnerUid())
            .setParameter("key", reservedValue.getKey())
            .getSingleResult();

    if (Objects.valueOf(reservedValue.getOwnerObject()).equals(TRACKEDENTITYATTRIBUTE)) {
      Query<Long> attrQuery =
          getTypedQuery(
              "SELECT count(*) "
                  + "FROM TrackedEntityAttributeValue "
                  + "WHERE attribute = "
                  + "( FROM TrackedEntityAttribute "
                  + "WHERE uid = :uid ) "
                  + "AND value LIKE :value ");

      count +=
          attrQuery
              .setParameter("uid", reservedValue.getOwnerUid())
              .setParameter("value", reservedValue.getValue())
              .getSingleResult();
    }

    return count.intValue();
  }

  @Override
  public boolean useReservedValue(String ownerUID, String value) {
    return getQuery("DELETE FROM ReservedValue WHERE owneruid = :uid AND value = :value")
            .setParameter("uid", ownerUID)
            .setParameter("value", value)
            .executeUpdate()
        == 1;
  }

  @Override
  public void deleteReservedValueByUid(String uid) {
    getQuery("DELETE FROM ReservedValue WHERE owneruid = :uid")
        .setParameter("uid", uid)
        .executeUpdate();
  }

  @Override
  public boolean isReserved(String ownerObject, String ownerUID, String value) {
    String hql =
        "from ReservedValue rv where rv.ownerObject =:ownerObject and rv.ownerUid =:ownerUid "
            + "and rv.value =:value";

    return !getQuery(hql)
        .setParameter("ownerObject", ownerObject)
        .setParameter("ownerUid", ownerUID)
        .setParameter("value", value)
        .getResultList()
        .isEmpty();
  }

  @Override
  public int removeExpiredValues() {
    return jdbcTemplate.update(
        "DELETE FROM reservedvalue WHERE reservedvalueid IN "
            + "(SELECT reservedvalueid FROM reservedvalue WHERE expirydate < now() LIMIT ?)",
        DELETE_BATCH_SIZE);
  }

  @Override
  public int removeUsedValues() {
    return jdbcTemplate.update(
        "DELETE FROM reservedvalue WHERE reservedvalueid IN ("
            + "SELECT rv.reservedvalueid FROM reservedvalue rv "
            + "JOIN trackedentityattribute tea ON rv.owneruid = tea.uid "
            + "JOIN trackedentityattributevalue teav ON teav.trackedentityattributeid = tea.trackedentityattributeid "
            + "AND lower(teav.value) = lower(rv.value) "
            + "LIMIT ?)",
        DELETE_BATCH_SIZE);
  }
}
