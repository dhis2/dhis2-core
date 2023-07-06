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
package org.hisp.dhis.webapi.service;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.collection.spi.PersistentCollection;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultLinkService implements LinkService {
  /** The default URL encoding that is used for query parameter values. */
  private static final String DEFAULT_URL_ENCODING = "UTF-8";

  private final SchemaService schemaService;

  private final ContextService contextService;

  // Since classes are static, use a map to cache setHref lookups
  private final Map<Class<?>, Method> setterCache = new HashMap<>();

  @Override
  public void generatePagerLinks(Pager pager, Class<?> klass) {
    if (pager == null) {
      return;
    }

    Schema schema = schemaService.getDynamicSchema(klass);

    if (!schema.hasApiEndpoint()) {
      return;
    }

    generatePagerLinks(pager, schema.getRelativeApiEndpoint());
  }

  @Override
  public void generatePagerLinks(Pager pager, String relativeApiEndpoint) {
    if (pager == null || trimToNull(relativeApiEndpoint) == null) {
      return;
    }

    final String endpoint =
        contextService.getApiPath() + relativeApiEndpoint + getContentTypeSuffix();
    final String parameters = getParametersString();

    if (pager.getPage() < pager.getPageCount()) {
      String nextPath = endpoint + "?page=" + (pager.getPage() + 1);
      nextPath += pager.pageSizeIsDefault() ? "" : "&pageSize=" + pager.getPageSize();

      if (!parameters.isEmpty()) {
        nextPath += "&" + parameters;
      }

      pager.setNextPage(nextPath);
    }

    if (pager.getPage() > 1) {
      if ((pager.getPage() - 1) == 1) {
        String prevPath = endpoint;

        if (pager.pageSizeIsDefault()) {
          if (!parameters.isEmpty()) {
            prevPath += "?" + parameters;
          }
        } else {
          prevPath += "?pageSize=" + pager.getPageSize();

          if (!parameters.isEmpty()) {
            prevPath += "&" + parameters;
          }
        }

        pager.setPrevPage(prevPath);
      } else {
        String prevPath = endpoint + "?page=" + (pager.getPage() - 1);
        prevPath += pager.pageSizeIsDefault() ? "" : "&pageSize=" + pager.getPageSize();

        if (!parameters.isEmpty()) {
          prevPath += "&" + parameters;
        }

        pager.setPrevPage(prevPath);
      }
    }
  }

  @Override
  public <T> void generateLinks(T object, boolean deepScan) {
    generateLinks(object, contextService.getApiPath(), deepScan);
  }

  @Override
  public <T> void generateLinks(T object, String hrefBase, boolean deepScan) {
    if (Collection.class.isInstance(object)) {
      Collection<?> collection = (Collection<?>) object;

      for (Object collectionObject : collection) {
        generateLink(collectionObject, hrefBase, deepScan);
      }
    } else {
      generateLink(object, hrefBase, deepScan);
    }
  }

  @Override
  public void generateSchemaLinks(List<Schema> schemas) {
    schemas.forEach(this::generateSchemaLinks);
  }

  @Override
  public void generateSchemaLinks(Schema schema) {
    generateSchemaLinks(schema, contextService.getServletPath());
  }

  @Override
  public void generateSchemaLinks(Schema schema, String hrefBase) {
    schema.setHref(hrefBase + "/schemas/" + schema.getSingular());

    if (schema.hasApiEndpoint()) {
      schema.setApiEndpoint(hrefBase + schema.getRelativeApiEndpoint());
    }

    for (Property property : schema.getProperties()) {
      if (PropertyType.REFERENCE == property.getPropertyType()) {
        Schema klassSchema = schemaService.getDynamicSchema(property.getKlass());
        property.setHref(hrefBase + "/schemas/" + klassSchema.getSingular());

        if (klassSchema.hasApiEndpoint()) {
          property.setRelativeApiEndpoint(klassSchema.getRelativeApiEndpoint());
          property.setApiEndpoint(hrefBase + klassSchema.getRelativeApiEndpoint());
        }
      } else if (PropertyType.REFERENCE == property.getItemPropertyType()) {
        Schema klassSchema = schemaService.getDynamicSchema(property.getItemKlass());
        property.setHref(hrefBase + "/schemas/" + klassSchema.getSingular());

        if (klassSchema.hasApiEndpoint()) {
          property.setRelativeApiEndpoint(klassSchema.getRelativeApiEndpoint());
          property.setApiEndpoint(hrefBase + klassSchema.getRelativeApiEndpoint());
        }
      }
    }
  }

  @Nonnull
  private String getParametersString() {
    final Map<String, List<String>> parameters = contextService.getParameterValuesMap();
    final StringBuilder result = new StringBuilder();

    parameters.forEach(
        (name, values) -> {
          if (!"page".equals(name) && !"pageSize".equals(name)) {
            values.forEach(
                value -> {
                  if (result.length() > 0) {
                    result.append('&');
                  }

                  try {
                    result.append(URLEncoder.encode(name, DEFAULT_URL_ENCODING));
                    result.append('=');
                    result.append(URLEncoder.encode(value, DEFAULT_URL_ENCODING));
                  } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(
                        "Encoding for URL values is not supported: " + DEFAULT_URL_ENCODING);
                  }
                });
          }
        });

    return result.toString();
  }

  @Nonnull
  protected String getContentTypeSuffix() {
    String requestUri = contextService.getRequest().getRequestURI();

    if (requestUri == null) {
      return StringUtils.EMPTY;
    }

    int index = requestUri.lastIndexOf('/');

    if (index >= 0) {
      // the request URI itself may have dots inside
      requestUri = requestUri.substring(index);
    }

    index = requestUri.indexOf('.');

    if (index < 0) {
      return StringUtils.EMPTY;
    }

    return requestUri.substring(index);
  }

  private <T> void generateLink(T object, String hrefBase, boolean deepScan) {
    Schema schema = schemaService.getDynamicSchema(HibernateProxyUtils.getRealClass(object));

    if (schema == null) {
      log.warn("Could not find schema for object of type " + object.getClass().getName() + ".");
      return;
    }

    generateHref(object, hrefBase);

    if (!deepScan) {
      return;
    }

    for (Property property : schema.getProperties()) {
      try {
        // TODO should we support non-idObjects?
        if (property.isIdentifiableObject()) {
          Object propertyObject = property.getGetterMethod().invoke(object);

          if (propertyObject == null) {
            continue;
          }

          // unwrap hibernate PersistentCollection
          if (PersistentCollection.class.isAssignableFrom(propertyObject.getClass())) {
            PersistentCollection collection = (PersistentCollection) propertyObject;
            propertyObject = collection.getValue();
          }

          if (!property.isCollection()) {
            generateHref(propertyObject, hrefBase);
          } else {
            Collection<?> collection = (Collection<?>) propertyObject;

            for (Object collectionObject : collection) {
              generateHref(collectionObject, hrefBase);
            }
          }
        }
      } catch (InvocationTargetException | IllegalAccessException ignored) {
      }
    }
  }

  private <T> void generateHref(T object, String hrefBase) {
    if (object == null || getSetter(HibernateProxyUtils.getRealClass(object)) == null) {
      return;
    }

    Class<?> klass = HibernateProxyUtils.getRealClass(object);

    Schema schema = schemaService.getDynamicSchema(klass);

    if (!schema.hasApiEndpoint()
        || schema.getProperty("id") == null
        || schema.getProperty("id").getGetterMethod() == null) {
      return;
    }

    Property id = schema.getProperty("id");

    try {
      Object value = id.getGetterMethod().invoke(object);

      if (!String.class.isInstance(value)) {
        log.warn(
            "id on object of type " + object.getClass().getName() + " does not return a String.");
        return;
      }

      Method setHref = getSetter(object.getClass());
      setHref.invoke(object, hrefBase + schema.getRelativeApiEndpoint() + "/" + value);
    } catch (InvocationTargetException | IllegalAccessException ignored) {
    }
  }

  private Method getSetter(Class<?> klass) {
    if (!setterCache.containsKey(klass)) {
      try {
        setterCache.put(klass, klass.getMethod("setHref", String.class));
      } catch (NoSuchMethodException ignored) {
      }
    }

    return setterCache.get(klass);
  }
}
