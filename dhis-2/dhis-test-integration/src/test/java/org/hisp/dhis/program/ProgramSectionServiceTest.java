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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author viet@dhis2.org
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class ProgramSectionServiceTest extends PostgresIntegrationTestBase {
  @Autowired private IdentifiableObjectManager manager;

  @Autowired private AclService aclService;

  @Autowired private CategoryService _categoryService;

  @BeforeEach
  final void setup() throws Exception {
    categoryService = _categoryService;
    injectAdminIntoSecurityContext();
  }

  @Test
  void testUpdateWithAuthority() {
    Program program = createProgram('A');
    manager.save(program);

    ProgramSection programSection = createProgramSection('A', program);

    manager.save(programSection);

    User userA = createUserWithAuth("A", "F_PROGRAM_PUBLIC_ADD");
    assertTrue(aclService.canUpdate(userA, programSection));

    User userB = createUserWithAuth("B");
    assertFalse(aclService.canUpdate(userB, programSection));
  }

  @Test
  void testSaveWithoutAuthority() {
    Program program = createProgram('A');
    manager.save(program);

    createUserAndInjectSecurityContext(false);
    ProgramSection programSection = createProgramSection('A', program);
    manager.save(programSection);

    assertNotNull(manager.get(ProgramSection.class, programSection.getId()));
  }
}
