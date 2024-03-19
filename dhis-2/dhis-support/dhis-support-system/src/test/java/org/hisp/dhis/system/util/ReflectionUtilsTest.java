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
package org.hisp.dhis.system.util;

import static org.hisp.dhis.system.util.ReflectionUtils.getClassName;
import static org.hisp.dhis.system.util.ReflectionUtils.getId;
import static org.hisp.dhis.system.util.ReflectionUtils.getProperty;
import static org.hisp.dhis.system.util.ReflectionUtils.isCollection;
import static org.hisp.dhis.system.util.ReflectionUtils.setProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class ReflectionUtilsTest {
  private DataElement dataElementA;

  @BeforeEach
  void before() {
    dataElementA = new DataElement();
    dataElementA.setId(8);
    dataElementA.setName("NameA");
    dataElementA.setAggregationType(AggregationType.SUM);
  }

  @Test
  void testGetId() {
    assertEquals(8, getId(dataElementA));
  }

  @Test
  void testGetProperty() {
    assertEquals("NameA", getProperty(dataElementA, "name"));
    assertNull(getProperty(dataElementA, "color"));
  }

  @Test
  void testSetProperty() {
    setProperty(dataElementA, "shortName", "ShortNameA");
    assertEquals("ShortNameA", dataElementA.getShortName());
  }

  @Test
  void testSetPropertyException() {
    assertThrows(
        UnsupportedOperationException.class, () -> setProperty(dataElementA, "color", "Blue"));
  }

  @Test
  void testGetClassName() {
    assertEquals("DataElement", getClassName(dataElementA));
  }

  @Test
  void testIsCollection() {
    List<Object> colA = new ArrayList<>();
    Collection<DataElement> colB = new HashSet<>();
    Collection<DataElement> colC = new ArrayList<>();
    assertTrue(isCollection(colA));
    assertTrue(isCollection(colB));
    assertTrue(isCollection(colC));
    assertFalse(isCollection(dataElementA));
  }
}
