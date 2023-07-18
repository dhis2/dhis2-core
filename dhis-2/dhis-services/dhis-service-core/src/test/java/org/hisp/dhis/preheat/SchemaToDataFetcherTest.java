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
package org.hisp.dhis.preheat;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.sms.command.SMSCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@SuppressWarnings("unchecked")
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class SchemaToDataFetcherTest extends DhisConvenienceTest {

  private SchemaToDataFetcher subject;

  @Mock private SessionFactory sessionFactory;

  @Mock private Session session;

  @Mock private Query query;

  @BeforeEach
  public void setUp() {
    when(sessionFactory.getCurrentSession()).thenReturn(session);
    subject = new SchemaToDataFetcher(sessionFactory);
  }

  @Test
  void verifyInput() {
    assertThat(subject.fetch(null), hasSize(0));
  }

  @Test
  void verifyUniqueFieldsAreMappedToHibernateObject() {
    Schema schema =
        createSchema(
            DataElement.class,
            "dataElement",
            Stream.of(
                    createUniqueProperty(Integer.class, "id", true, true),
                    createProperty(String.class, "name", true, true),
                    createUniqueProperty(String.class, "code", true, true),
                    createProperty(Date.class, "created", true, true),
                    createProperty(Date.class, "lastUpdated", true, true),
                    createProperty(Integer.class, "int", true, true))
                .collect(toList()));

    mockSession("SELECT code,id from " + schema.getKlass().getSimpleName());

    List<Object[]> l = new ArrayList<>();

    l.add(new Object[] {"abc", 123456});
    l.add(new Object[] {"bce", 123888});
    l.add(new Object[] {"def", 123999});

    when(query.getResultList()).thenReturn(l);

    List<DataElement> result = (List<DataElement>) subject.fetch(schema);

    assertThat(result, hasSize(3));

    assertThat(
        result,
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            allOf(hasProperty("code", is("abc")), hasProperty("id", is(123456L))),
            allOf(hasProperty("code", is("bce")), hasProperty("id", is(123888L))),
            allOf(hasProperty("code", is("def")), hasProperty("id", is(123999L)))));
  }

  @Test
  void verifyUniqueFieldsAreSkippedOnReflectionError() {
    Schema schema =
        createSchema(
            DummyDataElement.class,
            "dummyDataElement",
            Stream.of(
                    createUniqueProperty(String.class, "url", true, true),
                    createUniqueProperty(String.class, "code", true, true))
                .collect(toList()));

    mockSession("SELECT code,url from " + schema.getKlass().getSimpleName());

    List<Object[]> l = new ArrayList<>();

    l.add(new Object[] {"abc", "http://ok"});
    l.add(new Object[] {"bce", "http://-exception"});
    l.add(new Object[] {"def", "http://also-ok"});

    when(query.getResultList()).thenReturn(l);

    List<DataElement> result = (List<DataElement>) subject.fetch(schema);

    assertThat(result, hasSize(2));

    assertThat(
        result,
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            allOf(hasProperty("code", is("def")), hasProperty("url", is("http://also-ok"))),
            allOf(hasProperty("code", is("abc")), hasProperty("url", is("http://ok")))));
  }

  @Test
  void verifyUniqueFieldsAre() {
    Schema schema =
        createSchema(
            DummyDataElement.class,
            "dummyDataElement",
            Stream.of(
                    createProperty(String.class, "name", true, true),
                    createUniqueProperty(String.class, "url", true, true),
                    createProperty(String.class, "code", true, true))
                .collect(toList()));

    mockSession("SELECT url from " + schema.getKlass().getSimpleName());

    List<Object> l = new ArrayList<>();

    l.add("http://ok");
    l.add("http://is-ok");
    l.add("http://also-ok");

    when(query.getResultList()).thenReturn(l);

    List<DataElement> result = (List<DataElement>) subject.fetch(schema);

    assertThat(result, hasSize(3));

    assertThat(
        result,
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            allOf(hasProperty("url", is("http://also-ok"))),
            allOf(hasProperty("url", is("http://ok"))),
            allOf(hasProperty("url", is("http://is-ok")))));
  }

  @Test
  void verifyNoSqlWhenUniquePropertiesListIsEmpty() {
    Schema schema = createSchema(SMSCommand.class, "smsCommand", Lists.newArrayList());

    subject.fetch(schema);

    verify(sessionFactory, times(0)).getCurrentSession();
  }

  @Test
  void verifyNoSqlWhenNoUniquePropertyExist() {
    Schema schema =
        createSchema(
            SMSCommand.class,
            "smsCommand",
            Stream.of(
                    createProperty(String.class, "name", true, true),
                    createProperty(String.class, "id", true, true))
                .collect(toList()));

    subject.fetch(schema);

    verify(sessionFactory, times(0)).getCurrentSession();
  }

  private void mockSession(String hql) {
    when(session.createQuery(hql)).thenReturn(query);
    when(query.setReadOnly(true)).thenReturn(query);
  }

  private Schema createSchema(
      Class<? extends IdentifiableObject> klass, String singularName, List<Property> properties) {
    Schema schema = new Schema(klass, singularName, singularName + "s");

    for (Property property : properties) {
      schema.addProperty(property);
    }

    return schema;
  }

  private Property createProperty(Class<?> klazz, String name, boolean simple, boolean persisted) {
    Property property = new Property(klazz);
    property.setName(name);
    property.setFieldName(name);
    property.setSimple(simple);
    property.setOwner(true);
    property.setPersisted(persisted);

    return property;
  }

  public Property createUniqueProperty(
      Class<?> klazz, String name, boolean simple, boolean persisted) {
    Property property = createProperty(klazz, name, simple, persisted);
    property.setUnique(true);
    return property;
  }
}
