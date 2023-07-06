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
package org.hisp.dhis.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelationshipUtilsTest extends DhisConvenienceTest {

  private TrackedEntityInstance teiA, teiB;

  private ProgramInstance piA;

  private ProgramStageInstance psiA;

  private RelationshipType relationshipType;

  @BeforeEach
  void setup() {
    OrganisationUnit ou = createOrganisationUnit('A');
    teiA = createTrackedEntityInstance('A', ou);
    teiB = createTrackedEntityInstance('B', ou);
    Program p = createProgram('A');
    piA = createProgramInstance(p, teiA, ou);
    ProgramStage ps = createProgramStage('A', p);
    psiA = createProgramStageInstance(ps, piA, ou);
    relationshipType = createRelationshipType('A');
  }

  @Test
  void testExtractRelationshipItemUid() {
    RelationshipItem itemA = new RelationshipItem();
    RelationshipItem itemB = new RelationshipItem();
    RelationshipItem itemC = new RelationshipItem();
    itemA.setTrackedEntityInstance(teiA);
    itemB.setProgramInstance(piA);
    itemC.setProgramStageInstance(psiA);
    assertEquals(teiA.getUid(), RelationshipUtils.extractRelationshipItemUid(itemA));
    assertEquals(piA.getUid(), RelationshipUtils.extractRelationshipItemUid(itemB));
    assertEquals(psiA.getUid(), RelationshipUtils.extractRelationshipItemUid(itemC));
  }

  @Test
  void testGenerateRelationshipKey() {
    RelationshipItem from = new RelationshipItem();
    RelationshipItem to = new RelationshipItem();
    from.setTrackedEntityInstance(teiA);
    to.setTrackedEntityInstance(teiB);
    Relationship relationship = new Relationship();
    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(from);
    relationship.setTo(to);
    String key = relationshipType.getUid() + "_" + teiA.getUid() + "_" + teiB.getUid();
    assertEquals(key, RelationshipUtils.generateRelationshipKey(relationship));
  }

  @Test
  void testGenerateRelationshipInvertedKey() {
    RelationshipItem from = new RelationshipItem();
    RelationshipItem to = new RelationshipItem();
    from.setTrackedEntityInstance(teiA);
    to.setTrackedEntityInstance(teiB);
    Relationship relationship = new Relationship();
    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(from);
    relationship.setTo(to);
    String invertedKey = relationshipType.getUid() + "_" + teiB.getUid() + "_" + teiA.getUid();
    assertEquals(invertedKey, RelationshipUtils.generateRelationshipInvertedKey(relationship));
  }
}
