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
package org.hisp.dhis.option;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Transactional
class OptionServiceTest extends PostgresIntegrationTestBase {

  @Autowired private OptionService optionService;

  @Autowired private OptionStore optionStore;

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

  @BeforeEach
  void setUp() {
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
  void testSaveGet() throws ConflictException {
    optionService.saveOptionSet(optionSetA);
    optionService.saveOptionSet(optionSetB);
    optionService.saveOptionSet(optionSetC);
    OptionSet actualA = optionService.getOptionSet(optionSetA.getUid());
    OptionSet actualB = optionService.getOptionSet(optionSetB.getUid());
    OptionSet actualC = optionService.getOptionSet(optionSetC.getUid());
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

    optionSet.getOptions().forEach(optionStore::save);

    ConflictException ex =
        assertThrows(ConflictException.class, () -> optionService.saveOptionSet(optionSet));
    assertEquals(ErrorCode.E1118, ex.getCode());
  }

  @Test
  void testUpdateOptionSet_MultiTextWithSeparatorInCode() {
    OptionSet optionSet = createOptionSet('A', createOption('B'), createOption('C'));
    optionSet.setValueType(ValueType.MULTI_TEXT);

    optionSet.getOptions().forEach(optionStore::save);

    assertDoesNotThrow(() -> optionService.saveOptionSet(optionSet));
    optionSet.addOption(createOption(','));

    ConflictException ex =
        assertThrows(ConflictException.class, () -> optionService.updateOptionSet(optionSet));
    assertEquals(ErrorCode.E1118, ex.getCode());
  }

  @Test
  void testGetList() throws ConflictException {
    optionService.saveOptionSet(optionSetA);
    String uidA = optionSetA.getUid();
    List<Option> options = optionService.findOptionsByNamePattern(uidA, "OptA", 10);
    assertEquals(2, options.size());
    options = optionService.findOptionsByNamePattern(uidA, "OptA1", 10);
    assertEquals(1, options.size());
    options = optionService.findOptionsByNamePattern(uidA, "OptA1", null);
    assertEquals(1, options.size());
    options = optionService.findOptionsByNamePattern(uidA, "Opt", null);
    assertEquals(4, options.size());
    options = optionService.findOptionsByNamePattern(uidA, "Opt", 3);
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
    optionService.saveOptionGroup(optionGroupA);
    optionService.saveOptionGroup(optionGroupB);
    optionService.saveOptionGroup(optionGroupC);
    assertEquals(optionGroupA, optionService.getOptionGroup(optionGroupA.getUid()));
    assertEquals(optionGroupB, optionService.getOptionGroup(optionGroupB.getUid()));
    assertEquals(optionGroupC, optionService.getOptionGroup(optionGroupC.getUid()));
    assertEquals(2, optionService.getOptionGroup(optionGroupA.getUid()).getMembers().size());
    assertEquals(1, optionService.getOptionGroup(optionGroupB.getUid()).getMembers().size());
    assertEquals(0, optionService.getOptionGroup(optionGroupC.getUid()).getMembers().size());
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
    optionService.saveOptionGroupSet(optionGroupSetA);
    optionService.saveOptionGroupSet(optionGroupSetB);
    optionService.saveOptionGroupSet(optionGroupSetC);
    assertEquals(optionGroupSetA, optionService.getOptionGroupSet(optionGroupSetA.getUid()));
    assertEquals(optionGroupSetB, optionService.getOptionGroupSet(optionGroupSetB.getUid()));
    assertEquals(optionGroupSetC, optionService.getOptionGroupSet(optionGroupSetC.getUid()));
    assertEquals(2, optionService.getOptionGroupSet(optionGroupSetA.getUid()).getMembers().size());
    assertEquals(1, optionService.getOptionGroupSet(optionGroupSetB.getUid()).getMembers().size());
    assertEquals(0, optionService.getOptionGroupSet(optionGroupSetC.getUid()).getMembers().size());
  }
}
