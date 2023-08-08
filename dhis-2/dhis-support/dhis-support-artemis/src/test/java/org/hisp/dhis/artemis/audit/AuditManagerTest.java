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
package org.hisp.dhis.artemis.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.artemis.AuditProducerConfiguration;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditManagerTest {
  private AuditManager auditManager;

  @Mock private AuditProducerSupplier auditProducerSupplier;

  @Mock private AuditScheduler auditScheduler;

  @Mock private AuditMatrix auditMatrix;

  @Mock private AuditObjectFactory auditObjectFactory;

  @Mock private UsernameSupplier usernameSupplier;

  @BeforeEach
  public void setUp() {
    auditManager =
        new AuditManager(
            auditProducerSupplier,
            auditScheduler,
            AuditProducerConfiguration.builder().build(),
            auditMatrix,
            auditObjectFactory,
            usernameSupplier);
  }

  @Test
  void testCollectAuditAttributes() {
    DataElement dataElement = new DataElement();
    dataElement.setUid("DataElementUID");
    dataElement.setName("DataElementA");

    AuditAttributes attributes =
        auditManager.collectAuditAttributes(dataElement, DataElement.class);
    assertNotNull(attributes);
    assertEquals(dataElement.getUid(), attributes.get("uid"));

    Map<String, Object> map = new HashMap<>();
    map.put("name", dataElement.getName());
    map.put("uid", dataElement.getUid());
    map.put("code", "CODEA");

    attributes = auditManager.collectAuditAttributes(map, DataElement.class);
    assertNotNull(attributes);
    assertEquals(dataElement.getUid(), attributes.get("uid"));
    assertEquals("CODEA", attributes.get("code"));
  }
}
