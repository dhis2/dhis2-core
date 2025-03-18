/*
 * Copyright (c) 2004-2024, University of Oslo
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.EventProgramEnrollmentStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository("org.hisp.dhis.program.EventProgramEnrollmentStore")
public class HibernateEventProgramEnrollmentStore implements EventProgramEnrollmentStore {

  private final EntityManager entityManager;

  @Override
  public List<Enrollment> get(Program program) {
    try (Session session = entityManager.unwrap(Session.class)) {

      return session
          .createQuery(
              "from Enrollment e where e.program.uid = :programUid and e.program.programType = :programType",
              Enrollment.class)
          .setParameter("programUid", program.getUid())
          .setParameter("programType", ProgramType.WITHOUT_REGISTRATION)
          .list();
    }
  }

  @Override
  public List<Enrollment> get(Program program, EnrollmentStatus enrollmentStatus) {
    try (Session session = entityManager.unwrap(Session.class)) {

      return session
          .createQuery(
              "from Enrollment e where e.program.uid = :programUid and status = :enrollmentStatus and e.program.programType = :programType",
              Enrollment.class)
          .setParameter("programUid", program.getUid())
          .setParameter("enrollmentStatus", enrollmentStatus)
          .setParameter("programType", ProgramType.WITHOUT_REGISTRATION)
          .list();
    }
  }
}
