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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.Test;

/**
 * These classes each redeclare their own {@code formName} field rather than using the one inherited
 * from {@link BaseNameableObject}, so {@link BaseNameableObject#getFormNameFallback()} (compiled
 * against the superclass's field) would silently always fall back to the display name, ignoring any
 * form name actually set, unless each class overrides {@code getFormNameFallback()} itself.
 */
class FormNameFallbackTest {

  @Test
  void dataElementUsesFormNameWhenSet() {
    DataElement dataElement = new DataElement();
    dataElement.setName("Name");
    assertEquals("Name", dataElement.getDisplayFormName());

    dataElement.setFormName("Form Name");
    assertEquals("Form Name", dataElement.getDisplayFormName());

    dataElement.setFormName("");
    assertEquals("Name", dataElement.getDisplayFormName());
  }

  @Test
  void categoryOptionUsesFormNameWhenSet() {
    CategoryOption categoryOption = new CategoryOption();
    categoryOption.setName("Name");
    assertEquals("Name", categoryOption.getDisplayFormName());

    categoryOption.setFormName("Form Name");
    assertEquals("Form Name", categoryOption.getDisplayFormName());

    categoryOption.setFormName("");
    assertEquals("Name", categoryOption.getDisplayFormName());
  }

  @Test
  void dataSetUsesFormNameWhenSet() {
    DataSet dataSet = new DataSet();
    dataSet.setName("Name");
    assertEquals("Name", dataSet.getDisplayFormName());

    dataSet.setFormName("Form Name");
    assertEquals("Form Name", dataSet.getDisplayFormName());

    dataSet.setFormName("");
    assertEquals("Name", dataSet.getDisplayFormName());
  }

  @Test
  void indicatorUsesFormNameWhenSet() {
    Indicator indicator = new Indicator();
    indicator.setName("Name");
    assertEquals("Name", indicator.getDisplayFormName());

    indicator.setFormName("Form Name");
    assertEquals("Form Name", indicator.getDisplayFormName());

    indicator.setFormName("");
    assertEquals("Name", indicator.getDisplayFormName());
  }

  @Test
  void optionUsesFormNameWhenSet() {
    Option option = new Option();
    option.setName("Name");
    assertEquals("Name", option.getDisplayFormName());

    option.setFormName("Form Name");
    assertEquals("Form Name", option.getDisplayFormName());

    option.setFormName("");
    assertEquals("Name", option.getDisplayFormName());
  }

  @Test
  void programIndicatorUsesFormNameWhenSet() {
    ProgramIndicator programIndicator = new ProgramIndicator();
    programIndicator.setName("Name");
    assertEquals("Name", programIndicator.getDisplayFormName());

    programIndicator.setFormName("Form Name");
    assertEquals("Form Name", programIndicator.getDisplayFormName());

    programIndicator.setFormName("");
    assertEquals("Name", programIndicator.getDisplayFormName());
  }

  @Test
  void programSectionUsesFormNameWhenSet() {
    ProgramSection programSection = new ProgramSection();
    programSection.setName("Name");
    assertEquals("Name", programSection.getDisplayFormName());

    programSection.setFormName("Form Name");
    assertEquals("Form Name", programSection.getDisplayFormName());

    programSection.setFormName("");
    assertEquals("Name", programSection.getDisplayFormName());
  }

  @Test
  void programStageSectionUsesFormNameWhenSet() {
    ProgramStageSection programStageSection = new ProgramStageSection();
    programStageSection.setName("Name");
    assertEquals("Name", programStageSection.getDisplayFormName());

    programStageSection.setFormName("Form Name");
    assertEquals("Form Name", programStageSection.getDisplayFormName());

    programStageSection.setFormName("");
    assertEquals("Name", programStageSection.getDisplayFormName());
  }

  @Test
  void trackedEntityTypeUsesFormNameWhenSet() {
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setName("Name");
    assertEquals("Name", trackedEntityType.getDisplayFormName());

    trackedEntityType.setFormName("Form Name");
    assertEquals("Form Name", trackedEntityType.getDisplayFormName());

    trackedEntityType.setFormName("");
    assertEquals("Name", trackedEntityType.getDisplayFormName());
  }
}
