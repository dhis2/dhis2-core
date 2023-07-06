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
package org.hisp.dhis.organisationunit;

import static java.util.Arrays.asList;

import org.hisp.dhis.DhisSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

abstract class OrganisationUnitBaseSpringTest extends DhisSpringTest {
  @Autowired protected OrganisationUnitStore unitStore;

  @Autowired protected OrganisationUnitGroupStore groupStore;

  @Autowired protected OrganisationUnitGroupSetStore groupSetStore;

  @Autowired protected OrganisationUnitLevelStore levelStore;

  protected final OrganisationUnit addOrganisationUnit(char uniqueCharacter) {
    return addOrganisationUnit(uniqueCharacter, null);
  }

  protected final OrganisationUnit addOrganisationUnit(
      char uniqueCharacter, OrganisationUnit parent) {
    OrganisationUnit unit =
        parent == null
            ? createOrganisationUnit(uniqueCharacter)
            : createOrganisationUnit(uniqueCharacter, parent);
    unitStore.save(unit);
    return unit;
  }

  protected final OrganisationUnitGroup addOrganisationUnitGroup(
      char uniqueCharacter, OrganisationUnit... units) {
    OrganisationUnitGroup group = createOrganisationUnitGroup(uniqueCharacter);
    asList(units).forEach(group::addOrganisationUnit);
    groupStore.save(group);
    return group;
  }

  protected final OrganisationUnitGroupSet addOrganisationUnitGroupSet(
      char uniqueCharacter, OrganisationUnitGroup... groups) {
    OrganisationUnitGroupSet groupSet = createOrganisationUnitGroupSet(uniqueCharacter);
    asList(groups).forEach(groupSet::addOrganisationUnitGroup);
    groupSetStore.save(groupSet);
    return groupSet;
  }

  protected final OrganisationUnitLevel addOrganisationUnitLevel(int level, String name) {
    OrganisationUnitLevel unitLevel = new OrganisationUnitLevel(level, name);
    levelStore.save(unitLevel);
    return unitLevel;
  }
}
