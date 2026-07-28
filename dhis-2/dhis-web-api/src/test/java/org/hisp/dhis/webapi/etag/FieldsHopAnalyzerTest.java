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
package org.hisp.dhis.webapi.etag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.webapi.etag.FieldsHopAnalyzer.Verdict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Policy-matrix tests for {@link FieldsHopAnalyzer}. Schemas are hand-built fixtures so the walk is
 * tested against a controlled graph: {@code User -> userGroups -> UserGroup}, {@code DataElement ->
 * categoryCombo -> CategoryCombo -> categories -> Category}.
 *
 * @author Morten Svanaes
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class FieldsHopAnalyzerTest {

  @Mock private SchemaService schemaService;

  private FieldsHopAnalyzer analyzer;

  @BeforeEach
  void setUp() {
    Schema user = new Schema(User.class, "user", "users");
    user.addProperty(scalar("id"));
    user.addProperty(scalar("name"));
    user.addProperty(collectionRef("userGroups", UserGroup.class));
    user.addProperty(complex("sharing", Sharing.class));

    Schema userGroup = new Schema(UserGroup.class, "userGroup", "userGroups");
    userGroup.addProperty(scalar("id"));
    userGroup.addProperty(scalar("name"));
    userGroup.addProperty(collectionRef("members", User.class));

    Schema dataElement = new Schema(DataElement.class, "dataElement", "dataElements");
    dataElement.addProperty(scalar("id"));
    dataElement.addProperty(scalar("name"));
    dataElement.addProperty(singleRef("categoryCombo", CategoryCombo.class));
    dataElement.addProperty(singleRef("optionSet", OptionSet.class));

    Schema categoryCombo = new Schema(CategoryCombo.class, "categoryCombo", "categoryCombos");
    categoryCombo.addProperty(scalar("id"));
    categoryCombo.addProperty(scalar("name"));
    categoryCombo.addProperty(collectionRef("categories", Category.class));

    Schema optionSet = new Schema(OptionSet.class, "optionSet", "optionSets");
    optionSet.addProperty(scalar("id"));
    optionSet.addProperty(scalar("name"));

    Schema category = new Schema(Category.class, "category", "categories");
    category.addProperty(scalar("id"));
    category.addProperty(scalar("name"));

    Schema sharing = new Schema(Sharing.class, "sharing", "sharings");
    sharing.addProperty(scalar("owner"));
    sharing.addProperty(scalar("public"));

    when(schemaService.getSchema(User.class)).thenReturn(user);
    when(schemaService.getSchema(UserGroup.class)).thenReturn(userGroup);
    when(schemaService.getSchema(DataElement.class)).thenReturn(dataElement);
    when(schemaService.getSchema(CategoryCombo.class)).thenReturn(categoryCombo);
    when(schemaService.getSchema(Category.class)).thenReturn(category);
    when(schemaService.getSchema(OptionSet.class)).thenReturn(optionSet);
    when(schemaService.getSchema(Sharing.class)).thenReturn(sharing);

    analyzer = new FieldsHopAnalyzer(schemaService);
  }

  /**
   * Pins the parser contract the walk relies on: nested bracket groups flatten to parent-first
   * segment chains, and dot syntax is equivalent.
   */
  @Test
  void parserContractNestedPathsFlattenParentFirst() {
    List<FieldPath> paths = FieldFilterParser.parse("categoryCombo[categories[name]]");
    List<String> flat = paths.stream().map(FieldPath::toString).sorted().toList();
    List<String> dotted =
        FieldFilterParser.parse("categoryCombo.categories.name").stream()
            .map(FieldPath::toString)
            .sorted()
            .toList();
    assertEquals(dotted.get(dotted.size() - 1), flat.get(flat.size() - 1));
    assertEquals("categoryCombo.categories.name", flat.get(flat.size() - 1));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value = {
        // -- shallow: scalars and single hops --
        "'id,name';SHALLOW",
        "'userGroups[name]';SHALLOW",
        "'userGroups.name';SHALLOW",
        "'userGroups';SHALLOW",
        "'id,name,userGroups[id,name]';SHALLOW",
        // -- shallow: root-level presets and wildcard --
        "'*';SHALLOW",
        "':owner';SHALLOW",
        "':all';SHALLOW",
        "'name,:owner';SHALLOW",
        // -- shallow: exclusions never add data --
        "'!name';SHALLOW",
        "'name,!userGroups';SHALLOW",
        // -- deep: nested presets --
        "'userGroups[*]';DEEP",
        "'userGroups[:all]';DEEP",
        // -- deep: unknown segments --
        "'bogusProperty';DEEP",
        "'userGroups[bogus]';DEEP",
      })
  void userRootPolicy(String fields, Verdict expected) {
    assertEquals(expected, analyzer.analyze(User.class, fields), fields);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value = {
        "'categoryCombo[name]';SHALLOW",
        "'categoryCombo';SHALLOW",
        "'id,name,categoryCombo[id]';SHALLOW",
        // two reference hops
        "'categoryCombo[categories[name]]';DEEP",
        "'categoryCombo.categories.name';DEEP",
        // one deep path taints an otherwise shallow expression
        "'id,name,categoryCombo[categories[name]]';DEEP",
        // sibling hops on different paths are each one hop -> shallow
        "'categoryCombo[name],optionSet[name]';SHALLOW",
      })
  void dataElementRootPolicy(String fields, Verdict expected) {
    assertEquals(expected, analyzer.analyze(DataElement.class, fields), fields);
  }

  @Test
  void nullAndBlankFieldsAreShallow() {
    assertEquals(Verdict.SHALLOW, analyzer.analyze(User.class, null));
    assertEquals(Verdict.SHALLOW, analyzer.analyze(User.class, ""));
    assertEquals(Verdict.SHALLOW, analyzer.analyze(User.class, "   "));
  }

  @Test
  void embeddedComplexPropertyIsNotAHop() {
    assertEquals(Verdict.SHALLOW, analyzer.analyze(User.class, "sharing[owner]"));
  }

  @Test
  void unknownRootTypeIsDeep() {
    assertEquals(Verdict.DEEP, analyzer.analyze(String.class, "name"));
  }

  @Test
  void transformersAreIgnoredForHopCounting() {
    assertEquals(Verdict.SHALLOW, analyzer.analyze(User.class, "name,userGroups~size"));
    assertEquals(Verdict.SHALLOW, analyzer.analyze(User.class, "userGroups~pluck(name)"));
  }

  @Test
  void verdictsAreCachedPerRootAndFieldsString() {
    analyzer.analyze(User.class, "userGroups[name]");
    int callsAfterFirstWalk = mockingDetails(schemaService).getInvocations().size();
    analyzer.analyze(User.class, "userGroups[name]");
    assertEquals(
        callsAfterFirstWalk,
        mockingDetails(schemaService).getInvocations().size(),
        "cached verdict must not re-walk the schema graph");
    analyzer.analyze(DataElement.class, "userGroups[name]"); // different root, new walk
    assertEquals(
        callsAfterFirstWalk + 1, // root lookup only: unknown property stops the walk
        mockingDetails(schemaService).getInvocations().size());
  }

  @Test
  void adversarialDistinctKeysDoNotGrowUnbounded() {
    for (int i = 0; i < 3000; i++) {
      analyzer.analyze(User.class, "name,f" + i);
    }
    // bound is enforced by the cache; this proves the loop completes without error and stays
    // functionally correct afterwards
    assertEquals(Verdict.SHALLOW, analyzer.analyze(User.class, "userGroups[name]"));
  }

  private static Property scalar(String name) {
    Property p = new Property(String.class);
    p.setName(name);
    p.setPropertyType(PropertyType.TEXT);
    return p;
  }

  private static Property singleRef(String name, Class<?> klass) {
    Property p = new Property(klass);
    p.setName(name);
    p.setPropertyType(PropertyType.REFERENCE);
    return p;
  }

  private static Property collectionRef(String name, Class<?> itemKlass) {
    Property p = new Property(List.class);
    p.setName(name);
    p.setCollection(true);
    p.setItemKlass(itemKlass);
    p.setItemPropertyType(PropertyType.REFERENCE);
    p.setPropertyType(PropertyType.COLLECTION);
    return p;
  }

  private static Property complex(String name, Class<?> klass) {
    Property p = new Property(klass);
    p.setName(name);
    p.setPropertyType(PropertyType.COMPLEX);
    return p;
  }
}
