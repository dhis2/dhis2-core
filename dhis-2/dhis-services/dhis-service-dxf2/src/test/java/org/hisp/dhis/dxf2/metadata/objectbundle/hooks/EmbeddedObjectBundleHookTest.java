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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.test.TestBase.createEventVisualization;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author david mackessy
 */
@ExtendWith(MockitoExtension.class)
class EmbeddedObjectBundleHookTest {
  @Mock private SchemaService schemaService;

  @InjectMocks private EmbeddedObjectObjectBundleHook embeddedObjectObjectBundleHook;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("Clearing an embedded collection only occurs when it is not null")
  void testValidateSuccessA() throws NoSuchMethodException {
    // given an existing EventVisualization has a null eventRepetitions collection
    Program program = createProgram('p');
    EventVisualization importViz = createEventVisualization('a', program);
    EventVisualization persistedViz = createEventVisualization('b', program);
    persistedViz.setEventRepetitions(null);

    ObjectBundleParams params = new ObjectBundleParams();
    Preheat preheat = new Preheat();
    ObjectBundle bundle = new ObjectBundle(params, preheat, Map.of());

    Schema schema = new Schema(EventVisualization.class, "", "");
    Property property = new Property(EventRepetition.class);
    property.setCollection(true);
    property.setGetterMethod(EventVisualization.class.getMethod("getEventRepetitions"));
    schema.setPropertyMap(Map.of("eventRepetitions", property));

    when(schemaService.getDynamicSchema(EventVisualization.class)).thenReturn(schema);

    // when the embedded bundle hook tries to clear an embedded collection
    // then no exception is thrown
    assertDoesNotThrow(
        () -> embeddedObjectObjectBundleHook.preUpdate(importViz, persistedViz, bundle));
  }
}
