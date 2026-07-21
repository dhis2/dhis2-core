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
package org.hisp.dhis.fieldfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * Covers DHIS2-21856: a property annotated with {@code @PropertyTransformer} always serializes a
 * fixed shape regardless of what's requested underneath it (verified empirically: `fields=users`,
 * `fields=users[href]` and `fields=users[*]` all produce byte-identical responses against a live
 * server), so {@link FieldPathHelper#apply} should prune every descendant path of such a property
 * rather than expanding presets/defaults into it -- the earlier, narrower fix (DHIS2-21867) only
 * stopped {@code visitFieldPath} from *walking* those descendants; this stops them from being
 * generated at all, which is what starves every consumer (not just the ACL/sharing visitor) of a
 * reason to touch the real collection.
 */
class FieldPathHelperPropertyTransformerPruningTest {

  /** Stands in for a Hibernate-backed member type with its own nested collection. */
  public static class Member extends BaseIdentifiableObject {
    public String getExtra() {
      return "extra";
    }
  }

  public static class Root extends BaseIdentifiableObject {
    public Set<Member> getMembers() {
      return Set.of();
    }
  }

  private FieldPathHelper fieldPathHelper;

  @BeforeEach
  void setUp() throws NoSuchMethodException {
    Property idProperty = new Property(String.class, Member.class.getMethod("getUid"), null);
    idProperty.setName("id");
    idProperty.setSimple(true);
    idProperty.setPersisted(true);

    Property extraProperty = new Property(String.class, Member.class.getMethod("getExtra"), null);
    extraProperty.setName("extra");
    extraProperty.setSimple(true);

    Schema memberSchema = new Schema(Member.class, "member", "members");
    memberSchema.addProperty(idProperty);
    memberSchema.addProperty(extraProperty);

    Property membersProperty = new Property(Set.class, Root.class.getMethod("getMembers"), null);
    membersProperty.setName("members");
    membersProperty.setCollection(true);
    membersProperty.setItemKlass(Member.class);
    membersProperty.setPropertyTransformer(UserPropertyTransformer.class);

    Schema rootSchema = new Schema(Root.class, "root", "roots");
    rootSchema.addProperty(membersProperty);

    SchemaService schemaService = new StubSchemaService(rootSchema, memberSchema);
    fieldPathHelper = new FieldPathHelper(schemaService);
  }

  @Test
  void wildcardUnderPropertyTransformerCollapsesToTheBarePath() {
    List<FieldPath> result =
        fieldPathHelper.apply(FieldFilterParser.parse("members[*]"), Root.class);

    assertEquals(List.of("members"), result.stream().map(FieldPath::toString).toList());
  }

  @Test
  void explicitSubFieldsUnderPropertyTransformerCollapseToTheBarePath() {
    List<FieldPath> result =
        fieldPathHelper.apply(FieldFilterParser.parse("members[id,extra]"), Root.class);

    assertEquals(List.of("members"), result.stream().map(FieldPath::toString).toList());
  }

  @Test
  void barePropertyTransformerFieldIsUnaffected() {
    List<FieldPath> result = fieldPathHelper.apply(FieldFilterParser.parse("members"), Root.class);

    assertEquals(List.of("members"), result.stream().map(FieldPath::toString).toList());
  }

  private record StubSchemaService(Schema rootSchema, Schema memberSchema)
      implements SchemaService {
    @Override
    public void register(SchemaDescriptor descriptor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Schema getSchema(Class<?> klass) {
      if (klass == Root.class) return rootSchema;
      if (klass == Member.class) return memberSchema;
      return null;
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
