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
package org.hisp.dhis.fieldfiltering;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.query.planner.PropertyPath;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers DHIS2-21867: {@link FieldPathHelper#visitFieldPath} must not invoke the getter (or recurse
 * further) for a property annotated with {@code @PropertyTransformer}, since the transformer
 * serializes a fixed shape independent of any nested field selection -- invoking the getter for
 * such a property is exactly what forces the N+1 Hibernate lazy-load storm described in the ticket.
 */
class FieldPathHelperVisitFieldPathTest {

  /** Stands in for a Hibernate-backed member whose collection must never be touched. */
  public static class Member extends BaseIdentifiableObject {}

  public static class Root extends BaseIdentifiableObject {
    boolean membersGetterInvoked;

    public Set<Member> getMembers() {
      membersGetterInvoked = true;
      return Set.of();
    }
  }

  private FieldPathHelper fieldPathHelper;
  private Root root;

  @BeforeEach
  void setUp() throws NoSuchMethodException {
    Property membersProperty = new Property(Set.class, Root.class.getMethod("getMembers"), null);
    membersProperty.setName("members");
    membersProperty.setCollection(true);
    membersProperty.setItemKlass(Member.class);
    membersProperty.setPropertyTransformer(UserPropertyTransformer.class);

    Schema rootSchema = new Schema(Root.class, "root", "roots");
    rootSchema.addProperty(membersProperty);

    fieldPathHelper = new FieldPathHelper(new StubSchemaService(rootSchema));
    root = new Root();
  }

  @Test
  void visitPathsDoesNotInvokeGetterForPropertyWithTransformer() {
    FieldPath membersAccess = new FieldPath("access", List.of("members"));
    fieldPathHelper.visitFieldPaths(root, List.of(membersAccess), obj -> {});

    assertFalse(
        root.membersGetterInvoked,
        "getter for a property with a PropertyTransformer must not be invoked -- its subtree is "
            + "serialized as a fixed shape regardless of what's requested underneath");
  }

  private record StubSchemaService(Schema rootSchema) implements SchemaService {
    @Override
    public void register(SchemaDescriptor descriptor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Schema getSchema(Class<?> klass) {
      return klass == Root.class ? rootSchema : null;
    }

    @Override
    public Schema getSchemaBySingularName(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Schema getSchemaByPluralName(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Schema> getSchemas() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Schema> getSortedSchemas() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Schema> getMetadataSchemas() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> collectAuthorities() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PropertyPath getPropertyPath(Class<?> klass, String path) {
      throw new UnsupportedOperationException();
    }
  }
}
