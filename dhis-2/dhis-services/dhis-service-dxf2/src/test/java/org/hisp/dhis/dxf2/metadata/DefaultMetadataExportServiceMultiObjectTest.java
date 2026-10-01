/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dxf2.metadata;

import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createDataElementGroup;
import static org.hisp.dhis.test.TestBase.createOption;
import static org.hisp.dhis.test.TestBase.createOptionSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.SchemaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the multi-root dependency export in {@link DefaultMetadataExportService}.
 *
 * <p>Pins the three properties the feature rests on: the fold of one root equals the single-root
 * call, closures of different types merge, and an object reached from more than one root appears
 * once.
 *
 * @author David Mackessy
 */
@ExtendWith(MockitoExtension.class)
class DefaultMetadataExportServiceMultiObjectTest {

  @Mock private SchemaService schemaService;
  @Mock private ProgramRuleService programRuleService;
  @Mock private ProgramRuleVariableService programRuleVariableService;
  @Mock private QueryService queryService;

  @InjectMocks private DefaultMetadataExportService service;

  @Test
  @DisplayName("No roots yields an empty result")
  void emptyInputYieldsEmptyResult() {
    assertTrue(service.getMetadataWithDependencies(List.of()).isEmpty());
    assertTrue(service.getMetadataWithDependencies((List<IdentifiableObject>) null).isEmpty());
  }

  @Test
  @DisplayName("Identity law: folding a single root equals the single-root call")
  void foldOfSingleObjectEqualsLegacySingleCall() {
    OptionSet optionSet = createOptionSet('A', createOption('A'));

    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> folded =
        service.getMetadataWithDependencies(List.of(optionSet));
    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> single =
        service.getMetadataWithDependencies(optionSet);

    assertEquals(single, folded);
  }

  @Test
  @DisplayName("Roots of different types merge into one result")
  void mergesRootsOfDifferentTypesIntoOnePayload() {
    OptionSet optionSet = createOptionSet('A', createOption('A'));
    DataElementGroup group = createDataElementGroup('A', createDataElement('A'));

    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> result =
        service.getMetadataWithDependencies(List.of(optionSet, group));

    assertEquals(Set.of(optionSet), result.get(OptionSet.class));
    assertEquals(Set.of(group), result.get(DataElementGroup.class));
    assertEquals(1, result.get(DataElement.class).size());
  }

  @Test
  @DisplayName("A dependency shared by two roots appears once")
  void sharedDependencyAppearsOnce() {
    OptionSet shared = createOptionSet('A', createOption('A'));
    DataElementGroup first = createDataElementGroup('A', dataElementUsing('A', shared));
    DataElementGroup second = createDataElementGroup('B', dataElementUsing('B', shared));

    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> result =
        service.getMetadataWithDependencies(List.of(first, second));

    assertEquals(2, result.get(DataElementGroup.class).size());
    assertEquals(2, result.get(DataElement.class).size());
    assertEquals(
        Set.of(shared), result.get(OptionSet.class), "the shared OptionSet was duplicated");
    assertEquals(1, result.get(Option.class).size());
  }

  @Test
  @DisplayName("A root that is also another root's dependency appears once")
  void rootThatIsAlsoAnotherRootsDependencyAppearsOnce() {
    OptionSet optionSet = createOptionSet('A', createOption('A'));
    DataElementGroup group = createDataElementGroup('A', dataElementUsing('A', optionSet));

    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> result =
        service.getMetadataWithDependencies(List.of(group, optionSet));

    assertEquals(Set.of(optionSet), result.get(OptionSet.class));
    assertEquals(Set.of(group), result.get(DataElementGroup.class));
  }

  @Test
  @DisplayName("Two instances of the same row collapse to one entry")
  void equalButDistinctInstancesCollapse() {
    // two separate objects for the same row, as two traversals in different sessions could
    // produce. createOptionSet/createOption generate a fresh uid per call, so align them: without
    // that these are two different rows and the test would prove nothing.
    OptionSet first = createOptionSet('A', createOption('A'));
    OptionSet second = createOptionSet('A', createOption('A'));
    second.setUid(first.getUid());
    second.getOptions().get(0).setUid(first.getOptions().get(0).getUid());

    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> result =
        service.getMetadataWithDependencies(List.of(first, second));

    assertEquals(1, result.get(OptionSet.class).size(), "objects compare by uid, code and name");
    assertEquals(1, result.get(Option.class).size());
  }

  @Test
  @DisplayName("The same root requested twice contributes once")
  void repeatedRootAppearsOnce() {
    OptionSet optionSet = createOptionSet('A', createOption('A'));

    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> result =
        service.getMetadataWithDependencies(List.of(optionSet, optionSet));

    assertEquals(Set.of(optionSet), result.get(OptionSet.class));
  }

  @Test
  @DisplayName("An unsupported root type contributes nothing, and does not fail the others")
  void unsupportedRootTypeContributesNothing() {
    OptionSet optionSet = createOptionSet('A', createOption('A'));
    DataElement loose = createDataElement('A');

    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> result =
        service.getMetadataWithDependencies(List.of(optionSet, loose));

    assertEquals(Set.of(optionSet), result.get(OptionSet.class));
    assertNull(
        result.get(DataElement.class),
        "a DataElement is not a supported root, so it must contribute nothing on its own");
  }

  @Test
  @DisplayName("The declared supported root types are the ones the dispatch actually handles")
  void supportedRootTypesMatchTheDispatch() {
    assertEquals(
        Set.of(
            OptionSet.class,
            DataSet.class,
            Program.class,
            CategoryCombo.class,
            Dashboard.class,
            DataElementGroup.class),
        service.getDependencyRootTypes());
  }

  /** {@link org.hisp.dhis.test.TestBase} has no data element factory taking an option set. */
  private static DataElement dataElementUsing(char uniqueCharacter, OptionSet optionSet) {
    DataElement dataElement = createDataElement(uniqueCharacter);
    dataElement.setOptionSet(optionSet);
    return dataElement;
  }
}
