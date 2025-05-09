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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DataValueTest {

  @Test
  void testIsZero() {
    DataElement dataElement = new DataElement("A");
    dataElement.setValueType(ValueType.NUMBER);
    DataValue dataValue = new DataValue();
    dataValue.setDataElement(dataElement);
    dataValue.setValue("5");
    assertFalse(dataValue.isZero());
    dataValue.setValue("0.2");
    assertFalse(dataValue.isZero());
    dataValue.setValue("0.00001");
    assertFalse(dataValue.isZero());
    dataValue.setValue("String");
    assertFalse(dataValue.isZero());
    dataValue.setValue("0");
    assertTrue(dataValue.isZero());
    dataValue.setValue("0.0");
    assertTrue(dataValue.isZero());
    dataValue.setValue("0.0000");
    assertTrue(dataValue.isZero());
  }
}
