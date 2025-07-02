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
package org.hisp.dhis.program.hibernate;

import jakarta.persistence.EntityManager;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTempOwner;
import org.hisp.dhis.program.ProgramTempOwnerStore;
import org.hisp.dhis.user.UserDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Repository("org.hisp.dhis.program.ProgramTempOwnerStore")
public class HibernateProgramTempOwnerStore extends HibernateGenericStore<ProgramTempOwner>
    implements ProgramTempOwnerStore {
  public HibernateProgramTempOwnerStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, ProgramTempOwner.class, false);
  }

  // -------------------------------------------------------------------------
  // ProgramTempOwnerStore implementation
  // -------------------------------------------------------------------------

  @Override
  public void addProgramTempOwner(ProgramTempOwner programTempOwner) {
    getSession().save(programTempOwner);
  }

  @Override
  public int getValidTempOwnerCount(Program program, String trackedEntity, UserDetails user) {
    final String sql =
        """
        select count(1) from programtempowner \
        join trackedentity t on t.trackedentityid = programtempowner.trackedentityid \
        where programid = ? \
        and t.uid=? \
        and userid=? \
        and extract(epoch from validtill)-extract (epoch from now()::timestamp) > 0;""";
    Object[] args = new Object[] {program.getId(), trackedEntity, user.getId()};
    return jdbcTemplate.queryForObject(sql, Integer.class, args);
  }
}
