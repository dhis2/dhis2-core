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
package org.hisp.dhis.resourcetable.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@link UniqueNameContext}.
 *
 * @author Jan Bernitt
 */
class UniqueNameContextTest {

  private final UniqueNameContext context = new UniqueNameContext();

  @Test
  void alreadyUniqueNameIsKept() {
    assertEquals("Foo", context.uniqueName("Foo"));
    assertEquals("Bar", context.uniqueName("Bar"));
    assertEquals("Baz", context.uniqueName("Baz"));
  }

  @Test
  void nonUniqueNameIsExtendedWithCounter() {
    assertEquals("Foo", context.uniqueName("Foo"));
    assertEquals("Foo1", context.uniqueName("Foo"));
    assertEquals("Foo2", context.uniqueName("Foo"));
    assertEquals("Foo3", context.uniqueName("Foo"));
  }

  @Test
  void nonUniqueNameExtensionDoesNotCollideWithExistingNames() {
    assertEquals("Foo", context.uniqueName("Foo"));
    assertEquals("Foo2", context.uniqueName("Foo2"));
    assertEquals("Foo3", context.uniqueName("Foo"));
    assertEquals("Foo23", context.uniqueName("Foo2"));
    assertEquals("Foo4", context.uniqueName("Foo"));
    assertEquals("Foo25", context.uniqueName("Foo2"));
  }
}
