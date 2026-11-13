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
package org.hisp.dhis.config.sqlobserver;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Tests that HibernateTableEntityRegistry fails fast when the EntityManagerFactory is broken,
 * preventing the server from starting with a silently broken cache invalidation layer.
 */
class HibernateTableEntityRegistryFailureTest {

  @Test
  void buildTableMap_throwsIllegalStateException_whenEmfProviderThrows() {
    @SuppressWarnings("unchecked")
    ObjectProvider<EntityManagerFactory> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenThrow(new RuntimeException("EMF not available"));

    HibernateTableEntityRegistry registry = new HibernateTableEntityRegistry(provider);

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> registry.getTableInfo("any_table"));
    assertTrue(
        ex.getMessage().contains("failed to build table"),
        "Exception should indicate registry build failure");
    assertTrue(
        ex.getCause() instanceof RuntimeException, "Should wrap the original exception as cause");
  }

  @Test
  void buildTableMap_throwsIllegalStateException_whenUnwrapFails() {
    @SuppressWarnings("unchecked")
    ObjectProvider<EntityManagerFactory> provider = mock(ObjectProvider.class);
    EntityManagerFactory emf = mock(EntityManagerFactory.class);
    when(provider.getObject()).thenReturn(emf);
    when(emf.unwrap(org.hibernate.engine.spi.SessionFactoryImplementor.class))
        .thenThrow(new IllegalArgumentException("Cannot unwrap"));

    HibernateTableEntityRegistry registry = new HibernateTableEntityRegistry(provider);

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> registry.getTableInfo("any_table"));
    assertTrue(ex.getMessage().contains("Cannot start server"));
  }

  @Test
  void buildTableMap_throwsIllegalStateException_whenEmfProviderReturnsNull() {
    @SuppressWarnings("unchecked")
    ObjectProvider<EntityManagerFactory> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(null);

    HibernateTableEntityRegistry registry = new HibernateTableEntityRegistry(provider);

    assertThrows(IllegalStateException.class, () -> registry.getTableInfo("any_table"));
  }
}
