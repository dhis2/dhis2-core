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
package org.hisp.dhis.fieldfilter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.beanutils.PropertyUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.NoOpCache;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link DefaultFieldFilterService}. */
@ExtendWith(MockitoExtension.class)
class DefaultFieldFilterServiceTest {
  private FieldParser fieldParser = new DefaultFieldParser();

  @Mock private SchemaService schemaService;

  @Mock private AclService aclService;

  @Mock CurrentUserService currentUserService;

  @Mock private AttributeService attributeService;

  @Mock private UserGroupService userGroupService;

  @Mock private UserService userService;

  private DefaultFieldFilterService service;

  @BeforeEach
  public void setUp() throws Exception {
    CacheProvider cacheProvider = mock(CacheProvider.class);
    when(cacheProvider.createPropertyTransformerCache()).thenReturn(new NoOpCache<>());
    service =
        new DefaultFieldFilterService(
            fieldParser,
            schemaService,
            aclService,
            currentUserService,
            attributeService,
            cacheProvider,
            userGroupService,
            userService,
            new HashSet<>());
  }

  @Test
  void toCollectionNodeSkipSharingNoFields() throws Exception {
    final Attribute attribute = new Attribute();
    final Map<String, Property> propertyMap = new HashMap<>();
    addProperty(propertyMap, attribute, "dataElementAttribute");
    addProperty(propertyMap, attribute, "user");
    addProperty(propertyMap, attribute, "publicAccess");
    addProperty(propertyMap, attribute, "userGroupAccesses");
    addProperty(propertyMap, attribute, "userAccesses");
    addProperty(propertyMap, attribute, "externalAccess");

    final Schema rootSchema = new Schema(Attribute.class, "attribute", "attributes");
    rootSchema.setPropertyMap(propertyMap);
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(Attribute.class)))
        .thenReturn(rootSchema);

    final Schema booleanSchema = new Schema(boolean.class, "boolean", "booleans");
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(boolean.class)))
        .thenReturn(booleanSchema);

    final FieldFilterParams params =
        new FieldFilterParams(
            Collections.singletonList(attribute), Collections.emptyList(), Defaults.INCLUDE, true);

    CollectionNode node = service.toCollectionNode(Attribute.class, params);
    Assertions.assertEquals(1, node.getChildren().size());
    Set<String> names = extractNodeNames(node.getChildren().get(0).getChildren());
    Assertions.assertTrue(names.contains("dataElementAttribute"));
    Assertions.assertFalse(names.contains("user"));
    Assertions.assertFalse(names.contains("publicAccess"));
    Assertions.assertFalse(names.contains("userGroupAccesses"));
    Assertions.assertFalse(names.contains("userAccesses"));
    Assertions.assertFalse(names.contains("externalAccess"));
  }

  @Test
  void toCollectionNodeSkipSharingOwner() throws Exception {
    final Attribute attribute = new Attribute();
    final Map<String, Property> propertyMap = new HashMap<>();
    addProperty(propertyMap, attribute, "dataElementAttribute");
    Property p = addProperty(propertyMap, attribute, "dataSetAttribute");
    p.setOwner(true);
    p.setPersisted(true);
    addProperty(propertyMap, attribute, "user");
    addProperty(propertyMap, attribute, "publicAccess");
    addProperty(propertyMap, attribute, "userGroupAccesses");
    addProperty(propertyMap, attribute, "userAccesses");
    addProperty(propertyMap, attribute, "externalAccess");

    final Schema rootSchema = new Schema(Attribute.class, "attribute", "attributes");
    rootSchema.setPropertyMap(propertyMap);
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(Attribute.class)))
        .thenReturn(rootSchema);

    final Schema booleanSchema = new Schema(boolean.class, "boolean", "booleans");
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(boolean.class)))
        .thenReturn(booleanSchema);

    final FieldFilterParams params =
        new FieldFilterParams(
            Collections.singletonList(attribute),
            Collections.singletonList(":owner"),
            Defaults.INCLUDE,
            true);

    CollectionNode node = service.toCollectionNode(Attribute.class, params);
    Assertions.assertEquals(1, node.getChildren().size());
    Set<String> names = extractNodeNames(node.getChildren().get(0).getChildren());
    Assertions.assertFalse(names.contains("dataElementAttribute"));
    Assertions.assertTrue(names.contains("dataSetAttribute"));
    Assertions.assertFalse(names.contains("user"));
    Assertions.assertFalse(names.contains("publicAccess"));
    Assertions.assertFalse(names.contains("userGroupAccesses"));
    Assertions.assertFalse(names.contains("userAccesses"));
    Assertions.assertFalse(names.contains("externalAccess"));
  }

  private static Set<String> extractNodeNames(Collection<Node> nodes) {
    return nodes.stream().map(Node::getName).collect(Collectors.toSet());
  }

  private static Property addProperty(
      Map<String, Property> propertyMap, Object bean, String property) throws Exception {
    PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(bean, property);
    Property p = new Property(pd.getPropertyType(), pd.getReadMethod(), pd.getWriteMethod());
    p.setName(pd.getName());
    p.setReadable(true);
    propertyMap.put(pd.getName(), p);
    return p;
  }
}
