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
package org.hisp.dhis.dataentryform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author david mackessy
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataEntryFormStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataEntryFormStore store;

  @Autowired private IdentifiableObjectManager manager;

  @Test
  @DisplayName("get data entry forms which contain indicator refs in its htmlCode")
  void testAddDataEntryForm() {
    // given 4 data entry forms exist
    DataEntryForm form1 = createDataEntryForm('a');
    DataEntryForm form2 = createDataEntryForm('b');
    DataEntryForm form3 = createDataEntryForm('c');
    DataEntryForm form4 = createDataEntryForm('d');

    // 3 of which have specific search refs in their html code fields
    form1.setHtmlCode("<body><p>findMe1</p></body>");
    form2.setHtmlCode("<body><p>findMe2</p></body>");
    form3.setHtmlCode("<body><p>findMe2</p></body>");

    // 1 of which has a ref which won't be searched for
    form4.setHtmlCode("<body><p>dontFindMe</p></body>");

    manager.save(form1);
    manager.save(form2);
    manager.save(form3);
    manager.save(form4);

    // when searching for forms that contain a specific indicator ref
    List<DataEntryForm> matchedForms1 = store.getDataEntryFormsHtmlContaining("findMe1");

    // then only 1 form is retrieved matching a specific ref
    assertEquals(1, matchedForms1.size());
    assertTrue(matchedForms1.contains(form1));
    assertFalse(matchedForms1.contains(form2));
    assertFalse(matchedForms1.contains(form3));
    assertFalse(matchedForms1.contains(form4));

    // then only 2 forms are retrieved matching a specific ref
    List<DataEntryForm> matchedForms2 = store.getDataEntryFormsHtmlContaining("findMe2");
    assertEquals(2, matchedForms2.size());
    assertTrue(matchedForms2.contains(form2));
    assertTrue(matchedForms2.contains(form3));
    assertFalse(matchedForms2.contains(form1));
    assertFalse(matchedForms2.contains(form4));
  }
}
