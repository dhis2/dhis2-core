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
package org.hisp.dhis.option;

import static org.hisp.dhis.utils.Assertions.assertThrowsErrorCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class OptionServiceTest extends TransactionalIntegrationTest {

  @Autowired private OptionService optionService;

  private List<Option> options = new ArrayList<>();

  private OptionSet optionSetA = new OptionSet("OptionSetA", ValueType.TEXT);

  private OptionSet optionSetB = new OptionSet("OptionSetB", ValueType.TEXT);

  private OptionSet optionSetC = new OptionSet("OptionSetC", ValueType.TEXT);

  private OptionGroup optionGroupA;

  private OptionGroup optionGroupB;

  private OptionGroup optionGroupC;

  private OptionGroupSet optionGroupSetA;

  private OptionGroupSet optionGroupSetB;

  private OptionGroupSet optionGroupSetC;

  private Option option1;

  private Option option2;

  private Option option3;

  private Option option4;

  @Override
  public void setUpTest() {
    option1 = new Option("OptA1", "OptA1");
    option2 = new Option("OptA2", "OptA2");
    option3 = new Option("OptB1", "OptB1");
    option4 = new Option("OptB2", "OptB2");
    options.add(option1);
    options.add(option2);
    options.add(option3);
    options.add(option4);
    optionSetA.setOptions(options);
    optionSetB.setOptions(options);
    optionGroupA = new OptionGroup("OptionGroupA");
    optionGroupA.setShortName("ShortNameA");
    optionGroupB = new OptionGroup("OptionGroupB");
    optionGroupB.setShortName("ShortNameB");
    optionGroupC = new OptionGroup("OptionGroupC");
    optionGroupC.setShortName("ShortNameC");
    optionGroupSetA = new OptionGroupSet("OptionGroupSetA");
    optionGroupSetB = new OptionGroupSet("OptionGroupSetB");
    optionGroupSetC = new OptionGroupSet("OptionGroupSetC");
  }

  @Test
  void testSaveGet() {
    long idA = optionService.saveOptionSet(optionSetA);
    long idB = optionService.saveOptionSet(optionSetB);
    long idC = optionService.saveOptionSet(optionSetC);
    OptionSet actualA = optionService.getOptionSet(idA);
    OptionSet actualB = optionService.getOptionSet(idB);
    OptionSet actualC = optionService.getOptionSet(idC);
    assertEquals(optionSetA, actualA);
    assertEquals(optionSetB, actualB);
    assertEquals(optionSetC, actualC);
    assertEquals(4, optionSetA.getOptions().size());
    assertEquals(4, optionSetB.getOptions().size());
    assertEquals(0, optionSetC.getOptions().size());
    assertTrue(optionSetA.getOptions().contains(option1));
    assertTrue(optionSetA.getOptions().contains(option2));
    assertTrue(optionSetA.getOptions().contains(option3));
    assertTrue(optionSetA.getOptions().contains(option4));
  }

  @Test
  void testSaveOptionSet_MultiTextWithSeparatorInCode() {
    OptionSet optionSet = createOptionSet('A', createOption('B'), createOption(','));
    optionSet.setValueType(ValueType.MULTI_TEXT);

    assertThrowsErrorCode(
        IllegalQueryException.class, ErrorCode.E1118, () -> optionService.saveOptionSet(optionSet));
  }

  @Test
  void testUpdateOptionSet_MultiTextWithSeparatorInCode() {
    OptionSet optionSet = createOptionSet('A', createOption('B'), createOption('C'));
    optionSet.setValueType(ValueType.MULTI_TEXT);

    assertDoesNotThrow(() -> optionService.saveOptionSet(optionSet));
    optionSet.addOption(createOption(','));

    assertThrowsErrorCode(
        IllegalQueryException.class,
        ErrorCode.E1118,
        () -> optionService.updateOptionSet(optionSet));
  }

  @Test
  void testGetList() {
    long idA = optionService.saveOptionSet(optionSetA);
    List<Option> options = optionService.getOptions(idA, "OptA", 10);
    assertEquals(2, options.size());
    options = optionService.getOptions(idA, "OptA1", 10);
    assertEquals(1, options.size());
    options = optionService.getOptions(idA, "OptA1", null);
    assertEquals(1, options.size());
    options = optionService.getOptions(idA, "Opt", null);
    assertEquals(4, options.size());
    options = optionService.getOptions(idA, "Opt", 3);
    assertEquals(3, options.size());
  }

  // -------------------------------------------------------------------------
  // OptionGroup
  // -------------------------------------------------------------------------
  @Test
  void testAddGetOptionGroup() {
    optionGroupA.getMembers().add(option1);
    optionGroupA.getMembers().add(option2);
    optionGroupB.getMembers().add(option3);
    long idA = optionService.saveOptionGroup(optionGroupA);
    long idB = optionService.saveOptionGroup(optionGroupB);
    long idC = optionService.saveOptionGroup(optionGroupC);
    assertEquals(optionGroupA, optionService.getOptionGroup(idA));
    assertEquals(optionGroupB, optionService.getOptionGroup(idB));
    assertEquals(optionGroupC, optionService.getOptionGroup(idC));
    assertEquals(2, optionService.getOptionGroup(idA).getMembers().size());
    assertEquals(1, optionService.getOptionGroup(idB).getMembers().size());
    assertEquals(0, optionService.getOptionGroup(idC).getMembers().size());
  }

  // -------------------------------------------------------------------------
  // OptionGroupSet
  // -------------------------------------------------------------------------
  @Test
  void testAddGetOptionGroupSet() {
    optionGroupA.getMembers().add(option1);
    optionGroupA.getMembers().add(option2);
    optionGroupB.getMembers().add(option3);
    optionService.saveOptionGroup(optionGroupA);
    optionService.saveOptionGroup(optionGroupB);
    optionService.saveOptionGroup(optionGroupC);
    optionGroupSetA.getMembers().add(optionGroupA);
    optionGroupSetA.getMembers().add(optionGroupB);
    optionGroupSetB.getMembers().add(optionGroupC);
    long idA = optionService.saveOptionGroupSet(optionGroupSetA);
    long idB = optionService.saveOptionGroupSet(optionGroupSetB);
    long idC = optionService.saveOptionGroupSet(optionGroupSetC);
    assertEquals(optionGroupSetA, optionService.getOptionGroupSet(idA));
    assertEquals(optionGroupSetB, optionService.getOptionGroupSet(idB));
    assertEquals(optionGroupSetC, optionService.getOptionGroupSet(idC));
    assertEquals(2, optionService.getOptionGroupSet(idA).getMembers().size());
    assertEquals(1, optionService.getOptionGroupSet(idB).getMembers().size());
    assertEquals(0, optionService.getOptionGroupSet(idC).getMembers().size());
  }
}
