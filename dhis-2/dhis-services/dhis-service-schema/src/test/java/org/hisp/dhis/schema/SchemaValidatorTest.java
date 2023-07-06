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
package org.hisp.dhis.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class SchemaValidatorTest extends DhisSpringTest {

  @Autowired private SchemaValidator schemaValidator;

  @Test
  void testCollectionOutOfMinRange() {
    TestCollectionSize objectUnderTest = new TestCollectionSize();
    List<ErrorReport> errorReports = schemaValidator.validate(objectUnderTest, false);
    assertEquals(1, errorReports.size());
    assertEquals(ErrorCode.E4007, errorReports.get(0).getErrorCode());
  }

  @Test
  void testCollectionOutOfMaxRange() {
    TestCollectionSize objectUnderTest = new TestCollectionSize();
    objectUnderTest.getItems().add("Item #1");
    objectUnderTest.getItems().add("Item #2");
    objectUnderTest.getItems().add("Item #3");
    List<ErrorReport> errorReports = schemaValidator.validate(objectUnderTest, false);
    assertEquals(1, errorReports.size());
    assertEquals(ErrorCode.E4007, errorReports.get(0).getErrorCode());
  }

  @Test
  void testCollectionInRange() {
    TestCollectionSize objectUnderTest = new TestCollectionSize();
    objectUnderTest.getItems().add("Item #1");
    objectUnderTest.getItems().add("Item #2");
    List<ErrorReport> errorReports = schemaValidator.validate(objectUnderTest, false);
    assertTrue(errorReports.isEmpty());
  }

  public static class TestCollectionSize {

    private List<String> items = new ArrayList<>();

    public TestCollectionSize() {}

    @JsonProperty
    @PropertyRange(min = 1, max = 2)
    public List<String> getItems() {
      return items;
    }

    public void setItems(List<String> items) {
      this.items = items;
    }
  }
}
