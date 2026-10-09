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
package org.hisp.dhis.tracker.test;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TrackedEntityAttributeValueUtils {
  private static final String INSERT_SQL =
      """
      insert into trackedentityattributevalue
        (trackedentityid, trackedentityattributeid, created, lastupdated, value, updatedby)
      values (?, ?, ?, ?, ?, ?)
      """;

  private TrackedEntityAttributeValueUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Saves {@code attributeValue} via JDBC, the way the tracker importer does. Within a transaction
   * the persistence context is flushed first, so the tracked entity and attribute saved through
   * Hibernate exist in the database. Outside a transaction the value is saved in its own one. The
   * value is not added to the persistence context, nor to the attribute values of its tracked
   * entity.
   */
  public static void saveTrackedEntityAttributeValue(
      EntityManager entityManager, TrackedEntityAttributeValue attributeValue) {
    attributeValue.setAutoFields();
    Work insert = connection -> insert(connection, attributeValue);

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      entityManager.flush();
      entityManager.unwrap(Session.class).doWork(insert);
      return;
    }

    try (Session session =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).openSession()) {
      Transaction transaction = session.beginTransaction();
      session.doWork(insert);
      transaction.commit();
    }
  }

  private static void insert(Connection connection, TrackedEntityAttributeValue attributeValue)
      throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
      ps.setLong(1, attributeValue.getTrackedEntity().getId());
      ps.setLong(2, attributeValue.getAttribute().getId());
      ps.setTimestamp(3, new Timestamp(attributeValue.getCreated().getTime()));
      ps.setTimestamp(4, new Timestamp(attributeValue.getLastUpdated().getTime()));
      ps.setString(5, attributeValue.getValue());
      ps.setString(6, attributeValue.getUpdatedBy());
      ps.executeUpdate();
    }
  }
}
