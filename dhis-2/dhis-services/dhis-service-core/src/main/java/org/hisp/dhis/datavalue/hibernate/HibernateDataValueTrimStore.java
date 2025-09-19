/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.datavalue.hibernate;

import static org.hibernate.LockMode.PESSIMISTIC_WRITE;

import jakarta.persistence.EntityManager;
import org.hibernate.LockOptions;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueTrimStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Store for data value related queries executed by jobs to trim and adjust the data on a regular
 * basis.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@Repository
public class HibernateDataValueTrimStore extends HibernateGenericStore<DataValue>
    implements DataValueTrimStore {

  public HibernateDataValueTrimStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, DataValue.class, false);
  }

  @Override
  public int updateFileResourcesNotAssignedToAnyDataValue() {
    // using with to do this in a single pass on the datavalue table
    // abort if we cannot get the locks quickly
    String sql =
        """
        WITH candidates AS (
            SELECT uid
            FROM fileresource
            WHERE domain = 'DATA_VALUE' AND isassigned = true
        ),
        fr_datavalues AS (
            SELECT dv.value
            FROM datavalue dv
            JOIN dataelement de ON de.dataelementid = dv.dataelementid
            WHERE de.valuetype = 'FILE_RESOURCE'
        )
        UPDATE fileresource fr
        SET isassigned = false
        FROM candidates c
        LEFT JOIN fr_datavalues frdv ON frdv.value = c.uid
        WHERE fr.isassigned = true
          AND fr.uid = c.uid
          AND frdv.value IS NULL""";
    return getSession()
        .createNativeQuery(sql)
        .setLockOptions(new LockOptions(PESSIMISTIC_WRITE).setTimeOut(1000))
        .executeUpdate();
  }

  @Override
  public int updateFileResourcesAssignedToAnyDataValue() {
    // using with to do this in a single pass on the datavalue table
    // abort if we cannot get the locks quickly
    String sql =
        """
        WITH candidates AS (
            SELECT uid
            FROM fileresource
            WHERE domain = 'DATA_VALUE' AND isassigned = false
        ),
        fr_datavalues AS (
            SELECT DISTINCT dv.value
            FROM datavalue dv
            JOIN dataelement de ON de.dataelementid = dv.dataelementid
            WHERE de.valuetype = 'FILE_RESOURCE'
        )
        UPDATE fileresource fr
        SET isassigned = true
        FROM candidates c
        JOIN fr_datavalues frdv ON frdv.value = c.uid
        WHERE fr.isassigned = false
          AND fr.uid = c.uid""";
    return getSession()
        .createNativeQuery(sql)
        .setLockOptions(new LockOptions(PESSIMISTIC_WRITE).setTimeOut(1000))
        .executeUpdate();
  }

  @Override
  public int updateDeletedIfNotZeroIsSignificant() {
    // abort if we cannot get the locks quickly
    String sql =
        """
        UPDATE datavalue dv
        SET deleted = true
        FROM dataelement de
        WHERE dv.deleted = false
          AND de.zeroissignificant = false
          AND dv.dataelementid = de.dataelementid
          AND (dv.value IS NULL OR dv.value = '')""";
    return getSession()
        .createNativeQuery(sql)
        .setLockOptions(new LockOptions(PESSIMISTIC_WRITE).setTimeOut(1000))
        .executeUpdate();
  }
}
