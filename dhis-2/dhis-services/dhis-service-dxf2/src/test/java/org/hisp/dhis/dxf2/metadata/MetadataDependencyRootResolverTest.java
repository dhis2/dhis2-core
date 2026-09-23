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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link MetadataDependencyRootResolver}.
 *
 * <p>The contract under test is that resolution is all-or-nothing and reports every problem, not
 * just the first: a caller with several bad references learns about all of them in one response.
 *
 * @author David Mackessy
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetadataDependencyRootResolverTest {

  private static final String OPTION_SET_UID = "nghVC4wtyzi";
  private static final String OTHER_OPTION_SET_UID = "BfMAe6Itzgt";
  private static final String DATA_SET_UID = "lyLU2wR22tC";

  @Mock private SchemaService schemaService;
  @Mock private AclService aclService;
  @Mock private IdentifiableObjectManager manager;
  @Mock private MetadataExportService metadataExportService;

  @InjectMocks private MetadataDependencyRootResolver resolver;

  @BeforeEach
  void setUp() {
    when(metadataExportService.getDependencyRootTypes())
        .thenReturn(Set.of(DataSet.class, OptionSet.class));

    stubSchema(DataSet.class, "dataSet", "dataSets");
    stubSchema(OptionSet.class, "optionSet", "optionSets");
    stubSchema(DataElement.class, "dataElement", "dataElements");
  }

  @Test
  @DisplayName("Valid references resolve, in request order")
  void resolvesInRequestOrder() {
    OptionSet first = optionSet(OPTION_SET_UID);
    OptionSet second = optionSet(OTHER_OPTION_SET_UID);
    stubLoad(OptionSet.class, first, second);

    MetadataDependencyRoots roots =
        resolver.resolve(
            List.of("optionSet:" + OPTION_SET_UID, "optionSet:" + OTHER_OPTION_SET_UID));

    assertFalse(roots.hasErrors());
    assertEquals(List.of(first, second), roots.objects());
  }

  @Test
  @DisplayName("A traversal root type that is not yet enabled here is reported separately")
  void notYetEnabledRootTypeIsReported() {
    // DataSet is a real dependency-export root, /api/dataSets/{id}/metadata still serves it,
    // but it is gated out of the multi-object endpoint until its N+1s are fixed
    assertErrorCodes(resolver.resolve(List.of("dataSet:" + DATA_SET_UID)), ErrorCode.E6029);
  }

  @Test
  @DisplayName("The plural schema name resolves too")
  void resolvesPluralTypeName() {
    OptionSet optionSet = optionSet(OPTION_SET_UID);
    stubLoad(OptionSet.class, optionSet);

    MetadataDependencyRoots roots = resolver.resolve(List.of("optionSets:" + OPTION_SET_UID));

    assertFalse(roots.hasErrors());
    assertEquals(List.of(optionSet), roots.objects());
  }

  @Test
  @DisplayName("One token may name several objects of the same type")
  void resolvesCommaSeparatedIdsInOneToken() {
    OptionSet first = optionSet(OPTION_SET_UID);
    OptionSet second = optionSet(OTHER_OPTION_SET_UID);
    stubLoad(OptionSet.class, first, second);

    MetadataDependencyRoots roots =
        resolver.resolve(List.of("optionSet:" + OPTION_SET_UID + "," + OTHER_OPTION_SET_UID));

    assertFalse(roots.hasErrors());
    assertEquals(List.of(first, second), roots.objects());
  }

  @Test
  @DisplayName("A comma list and repeated parameters are equivalent")
  void commaListMatchesRepeatedParameters() {
    OptionSet first = optionSet(OPTION_SET_UID);
    OptionSet second = optionSet(OTHER_OPTION_SET_UID);
    stubLoad(OptionSet.class, first, second);

    assertEquals(
        resolver
            .resolve(List.of("optionSet:" + OPTION_SET_UID, "optionSet:" + OTHER_OPTION_SET_UID))
            .objects(),
        resolver
            .resolve(List.of("optionSet:" + OPTION_SET_UID + "," + OTHER_OPTION_SET_UID))
            .objects());
  }

  @Test
  @DisplayName("A bad id inside a comma list is reported against that id alone")
  void badIdInsideCommaListIsReportedPrecisely() {
    stubLoad(OptionSet.class, optionSet(OPTION_SET_UID));

    MetadataDependencyRoots roots =
        resolver.resolve(List.of("optionSet:" + OPTION_SET_UID + ",aaaaaaaaaaa"));

    assertEquals(List.of(ErrorCode.E1113), errorCodes(roots));
    assertEquals("optionSet:aaaaaaaaaaa", roots.errors().get(0).getMainId());
  }

  @Test
  @DisplayName("Several references of one type cost a single query")
  void loadsOneQueryPerType() {
    stubLoad(OptionSet.class, optionSet(OPTION_SET_UID), optionSet(OTHER_OPTION_SET_UID));

    resolver.resolve(List.of("optionSet:" + OPTION_SET_UID, "optionSet:" + OTHER_OPTION_SET_UID));

    verify(manager, times(1)).getByUid(eq(OptionSet.class), anyCollection());
  }

  @Test
  @DisplayName("A repeated reference is not resolved, or walked, twice")
  void duplicateTokensAreCollapsed() {
    OptionSet optionSet = optionSet(OPTION_SET_UID);
    stubLoad(OptionSet.class, optionSet);

    MetadataDependencyRoots roots =
        resolver.resolve(List.of("optionSet:" + OPTION_SET_UID, "optionSet:" + OPTION_SET_UID));

    assertEquals(List.of(optionSet), roots.objects());
  }

  @Test
  @DisplayName("No references at all is an error rather than an empty payload")
  void noReferencesIsAnError() {
    assertErrorCodes(resolver.resolve(List.of()), ErrorCode.E6028);
    assertErrorCodes(resolver.resolve(null), ErrorCode.E6028);
  }

  @Test
  @DisplayName("A malformed token is reported")
  void malformedTokenIsReported() {
    assertErrorCodes(resolver.resolve(List.of("optionSet")), ErrorCode.E6024);
  }

  @Test
  @DisplayName("An unknown type is reported")
  void unknownTypeIsReported() {
    assertErrorCodes(resolver.resolve(List.of("optionSetz:" + OPTION_SET_UID)), ErrorCode.E6002);
  }

  @Test
  @DisplayName("A known type that is not a dependency export root is reported")
  void unsupportedRootTypeIsReported() {
    assertErrorCodes(resolver.resolve(List.of("dataElement:" + OPTION_SET_UID)), ErrorCode.E6026);
  }

  @Test
  @DisplayName("An id that is not a valid UID is reported without hitting the database")
  void invalidUidIsReported() {
    assertErrorCodes(resolver.resolve(List.of("optionSet:not-a-uid")), ErrorCode.E1113);
    verify(manager, times(0)).getByUid(eq(OptionSet.class), anyCollection());
  }

  @Test
  @DisplayName("An id that does not exist, or that the user may not read, is reported")
  void unresolvedIdIsReported() {
    stubLoad(OptionSet.class);

    assertErrorCodes(resolver.resolve(List.of("optionSet:" + OPTION_SET_UID)), ErrorCode.E1113);
  }

  @Test
  @DisplayName("Every bad reference is reported, not just the first")
  void collectsAllFailuresNotJustTheFirst() {
    stubLoad(OptionSet.class);

    MetadataDependencyRoots roots =
        resolver.resolve(
            List.of(
                "optionSet",
                "optionSetz:" + OPTION_SET_UID,
                "dataElement:" + OPTION_SET_UID,
                "dataSet:" + DATA_SET_UID,
                "optionSet:" + OPTION_SET_UID));

    assertErrorCodes(
        roots, ErrorCode.E6024, ErrorCode.E6002, ErrorCode.E6026, ErrorCode.E6029, ErrorCode.E1113);
  }

  @Test
  @DisplayName("Nothing is returned when any reference fails, so a payload is never partial")
  void resolutionIsAllOrNothing() {
    stubLoad(OptionSet.class, optionSet(OPTION_SET_UID));

    MetadataDependencyRoots roots =
        resolver.resolve(List.of("optionSet:" + OPTION_SET_UID, "optionSet"));

    assertTrue(roots.hasErrors());
    assertTrue(roots.objects().isEmpty(), "a failed resolution must not return partial roots");
  }

  @Test
  @DisplayName("The reported error names the offending token")
  void errorReportsCarryTheOffendingToken() {
    MetadataDependencyRoots roots = resolver.resolve(List.of("optionSetz:" + OPTION_SET_UID));

    assertEquals("optionSetz:" + OPTION_SET_UID, roots.errors().get(0).getMainId());
  }

  private static List<ErrorCode> errorCodes(MetadataDependencyRoots roots) {
    return roots.errors().stream().map(ErrorReport::getErrorCode).toList();
  }

  private void assertErrorCodes(MetadataDependencyRoots roots, ErrorCode... expected) {
    assertEquals(
        List.of(expected), roots.errors().stream().map(ErrorReport::getErrorCode).toList());
  }

  @SuppressWarnings("unchecked")
  private <T extends IdentifiableObject> void stubLoad(Class<T> type, T... found) {
    when(manager.getByUid(eq(type), anyCollection())).thenReturn(List.of(found));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void stubSchema(Class<?> type, String singular, String plural) {
    Schema schema = new Schema(type, singular, plural);
    when(schemaService.getSchemaBySingularName(singular)).thenReturn(schema);
    when(aclService.classForType(singular)).thenReturn((Class) type);
    when(schemaService.getSchemaByPluralName(plural)).thenReturn(schema);
    when(schemaService.getSchema(type)).thenReturn(schema);
  }

  private OptionSet optionSet(String uid) {
    OptionSet optionSet = new OptionSet();
    optionSet.setUid(uid);
    optionSet.setName("OptionSet " + uid);
    return optionSet;
  }
}
