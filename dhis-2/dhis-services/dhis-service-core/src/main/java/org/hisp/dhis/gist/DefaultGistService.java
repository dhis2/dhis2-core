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
package org.hisp.dhis.gist;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.gist.GistBuilder.createCountBuilder;
import static org.hisp.dhis.gist.GistBuilder.createFetchBuilder;
import static org.hisp.dhis.gist.GistLogic.isPersistentReferenceField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.object.ObjectOutput;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jan Bernitt
 */
@Service
@RequiredArgsConstructor
public class DefaultGistService implements GistService {
  /**
   * Instead of an actual date value users may use string {@code now} to always get current moment
   * as time for a {@link Date} value.
   */
  private static final String NOW_PARAMETER_VALUE = "now";

  private final EntityManager entityManager;

  private final SchemaService schemaService;

  private final UserService userService;

  private final AclService aclService;

  private final AttributeService attributeService;

  private final ObjectMapper jsonMapper;

  private final GistBuilder.GistBuilderSupport builderSupport = new GistBuilderSupportAdapter();

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
  public GistObjectList exportObjectList(@Nonnull GistQuery query) {
    GistQuery planned = plan(query);
    Stream<Object[]> values = gist(planned);
    return new GistObjectList(pager(query), properties(planned), values);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
  public GistObjectList exportPropertyObjectList(@Nonnull GistQuery query) {
    return exportObjectList(query);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public GistObject exportObject(@Nonnull GistQuery query) {
    GistQuery planned = plan(query);
    Object[] values = gist(planned).findFirst().orElse(null);
    return new GistObject(properties(planned), values);
  }

  private List<ObjectOutput.Property> properties(GistQuery query) {
    RelativePropertyContext context = createPropertyContext(query);
    List<ObjectOutput.Property> res = new ArrayList<>(query.getFields().size());
    for (GistQuery.Field f : query.getFields()) {
      String name = f.getName();
      if (f.isAttribute()) {
        res.add(new ObjectOutput.Property(name, ObjectOutput.Type.STRING, false));
      } else if (GistQuery.Field.REFS_PATH.equals(f.getPropertyPath())) {
        res.add(
            new ObjectOutput.Property(
                "apiEndpoints", new ObjectOutput.Type(Map.class, String.class), false));
      } else {
        Property p = context.resolveMandatory(f.getPropertyPath());
        ObjectOutput.Type type =
            switch (f.getTransformation()) {
              case IS_EMPTY, IS_NOT_EMPTY, MEMBER, NOT_MEMBER -> ObjectOutput.Type.BOOLEAN;
              case SIZE -> ObjectOutput.Type.INTEGER;
              case IDS, PLUCK -> new ObjectOutput.Type(String[].class);
              case ID_OBJECTS -> new ObjectOutput.Type(JsonBuilder.JsonEncodable[].class);
              default -> type(p);
            };
        res.add(new ObjectOutput.Property(name, type, f.getTransformation().isArrayAggregate()));
      }
    }
    return res;
  }

  private static ObjectOutput.Type type(Property p) {
    if (isPersistentReferenceField(p)) return ObjectOutput.Type.STRING;
    if (p.isCollection() && p.getOwningRole() != null) return ObjectOutput.Type.INTEGER;
    return new ObjectOutput.Type(p.getKlass(), p.getItemKlass());
  }

  private GistQuery plan(GistQuery query) {
    return new GistPlanner(query, createPropertyContext(query), createGistAccessControl()).plan();
  }

  private Stream<Object[]> gist(GistQuery query) {
    GistAccessControl access = createGistAccessControl();
    RelativePropertyContext context = createPropertyContext(query);
    new GistValidator(query, context, access).validateQuery();
    GistBuilder queryBuilder = createFetchBuilder(query, context, access, builderSupport);
    Stream<Object[]> rows =
        fetchWithParameters(
            query,
            queryBuilder,
            getSession().createQuery(queryBuilder.buildFetchHQL(), Object[].class));
    return queryBuilder.transform(rows);
  }

  private GistPager pager(GistQuery query) {
    int page = 1 + (query.getPageOffset() / query.getPageSize());
    Schema schema = schemaService.getDynamicSchema(query.getElementType());
    String prev = null;
    String next = null;
    Integer total = null;
    if (query.isTotal()) {
      GistAccessControl access = createGistAccessControl();
      RelativePropertyContext context = createPropertyContext(query);
      GistBuilder countBuilder = createCountBuilder(query, context, access, builderSupport);
      total =
          countWithParameters(
              countBuilder, getSession().createQuery(countBuilder.buildCountHQL(), Long.class));
    }
    if (schema.hasApiEndpoint()) {
      URI queryURI = URI.create(query.getRequestURL());
      if (page > 1) {
        prev =
            UriComponentsBuilder.fromUri(queryURI)
                .replaceQueryParam("page", page - 1)
                .build()
                .toString();
      }
      Integer pageCount = GistPager.getPageCount(total, query.getPageSize());
      if (pageCount == null || pageCount > page) {
        next =
            UriComponentsBuilder.fromUri(queryURI)
                .replaceQueryParam("page", page + 1)
                .build()
                .toString();
      }
    }
    return new GistPager(page, query.getPageSize(), total, prev, next);
  }

  private GistAccessControl createGistAccessControl() {
    return new DefaultGistAccessControl(
        CurrentUserUtil.getCurrentUserDetails(), aclService, userService, this);
  }

  private RelativePropertyContext createPropertyContext(GistQuery query) {
    return new RelativePropertyContext(query.getElementType(), schemaService::getDynamicSchema);
  }

  private Stream<Object[]> fetchWithParameters(
      GistQuery gistQuery, GistBuilder builder, Query<?> query) {
    builder.addFetchParameters(query::setParameter, this::parseFilterArgument);
    if (gistQuery.isPaging()) {
      query.setMaxResults(Math.max(1, gistQuery.getPageSize()));
      query.setFirstResult(gistQuery.getPageOffset());
    }
    query.setCacheable(false);
    // The map is required because querying a single property will not result in Object[] returned
    // by query API
    return query.stream()
        .map(
            e -> {
              if (e == null) return null;
              return e instanceof Object[] row ? row : new Object[] {e};
            });
  }

  private int countWithParameters(GistBuilder builder, Query<Long> query) {
    builder.addCountParameters(query::setParameter, this::parseFilterArgument);
    query.setCacheable(false);
    return query.getSingleResult().intValue();
  }

  @SuppressWarnings("unchecked")
  private <T> T parseFilterArgument(String value, Class<T> type) {
    if (type == Date.class && NOW_PARAMETER_VALUE.equals(value)) {
      return (T) new Date();
    }
    String valueAsJson = value;
    if (!(Number.class.isAssignableFrom(type) || type == Boolean.class || type == boolean.class)) {
      valueAsJson = '"' + value + '"';
    }
    try {
      return jsonMapper.readValue(valueAsJson, type);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          String.format("Type %s is not compatible with provided filter value: `%s`", type, value));
    }
  }

  private class GistBuilderSupportAdapter implements GistBuilder.GistBuilderSupport {

    @Override
    public List<String> getUserGroupIdsByUserId(String userId) {
      return userService.getUser(userId).getGroups().stream()
          .map(IdentifiableObject::getUid)
          .collect(toList());
    }

    @Override
    public Attribute getAttributeById(String attributeId) {
      return attributeService.getAttribute(attributeId);
    }

    @Override
    public Object getTypedAttributeValue(Attribute attribute, String value) {
      if (value == null || value.isBlank()) {
        return value;
      }
      try {
        return attribute.getValueType().isJson() ? jsonMapper.readTree(value) : value;
      } catch (JsonProcessingException e) {
        return value;
      }
    }
  }
}
