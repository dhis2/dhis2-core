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
package org.hisp.dhis.tracker.imports.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class RelationshipTrackerConverterServiceTest extends TestBase {

  private static final String TE_TO_ENROLLMENT_RELATIONSHIP_TYPE = "xLmPUYJX8Ks";

  private static final String TE_TO_EVENT_RELATIONSHIP_TYPE = "TV9oB9LT3sh";

  private static final String TE = CodeGenerator.generateUid();

  private static final String ENROLLMENT = "ENROLLMENT_UID";

  private static final String EVENT = "EVENT_UID";

  private static final String RELATIONSHIP_A = "RELATIONSHIP_A_UID";

  private RelationshipType teToEnrollment;

  private TrackedEntity trackedEntity;

  private Enrollment enrollment;

  private TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship>
      relationshipConverterService;

  @Mock public TrackerPreheat preheat;

  @BeforeEach
  protected void setupTest() {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    Program program = createProgram('A');
    TrackedEntityType teType = createTrackedEntityType('A');

    teToEnrollment = createTeToEnrollmentRelationshipType('A', program, teType, false);
    teToEnrollment.setUid(TE_TO_ENROLLMENT_RELATIONSHIP_TYPE);

    trackedEntity = createTrackedEntity(organisationUnit);
    trackedEntity.setTrackedEntityType(teType);
    trackedEntity.setUid(TE);
    enrollment = createEnrollment(program, trackedEntity, organisationUnit);
    enrollment.setUid(ENROLLMENT);

    relationshipConverterService = new RelationshipTrackerConverterService();
  }

  @Test
  void testConverterFromRelationship() {
    when(preheat.getRelationship(RELATIONSHIP_A)).thenReturn(relationshipAFromDB());
    when(preheat.getRelationshipType(MetadataIdentifier.ofUid(TE_TO_ENROLLMENT_RELATIONSHIP_TYPE)))
        .thenReturn(teToEnrollment);
    when(preheat.getTrackedEntity(TE)).thenReturn(trackedEntity);
    when(preheat.getEnrollment(ENROLLMENT)).thenReturn(enrollment);

    org.hisp.dhis.relationship.Relationship from =
        relationshipConverterService.from(preheat, relationshipA(), new SystemUser());

    assertNotNull(from);
    assertEquals(TE, from.getFrom().getTrackedEntity().getUid());
    assertEquals(ENROLLMENT, from.getTo().getEnrollment().getUid());
  }

  private Relationship relationshipA() {
    return Relationship.builder()
        .relationship(RELATIONSHIP_A)
        .relationshipType(MetadataIdentifier.ofUid(TE_TO_ENROLLMENT_RELATIONSHIP_TYPE))
        .from(RelationshipItem.builder().trackedEntity(TE).build())
        .to(RelationshipItem.builder().enrollment(ENROLLMENT).build())
        .build();
  }

  private org.hisp.dhis.relationship.Relationship relationshipAFromDB() {
    return createTeToEnrollmentRelationship(trackedEntity, enrollment, teToEnrollment);
  }
}
