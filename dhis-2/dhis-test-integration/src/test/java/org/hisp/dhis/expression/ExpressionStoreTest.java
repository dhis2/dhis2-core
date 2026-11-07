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
package org.hisp.dhis.expression;

import static org.hisp.dhis.expression.Operator.equal_to;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BooleanType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class ExpressionStoreTest extends PostgresIntegrationTestBase {

  @Autowired
  @Qualifier("org.hisp.dhis.expression.ExpressionStore")
  private GenericStore<Expression> expressionStore;

  @Autowired private DataElementService dataElementService;

  @Autowired private CategoryService categoryService;

  @Autowired private ValidationRuleService validationRuleService;

  @Autowired private PredictorService predictorService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  private final PeriodType periodType = PeriodType.getAvailablePeriodTypes().iterator().next();

  private long dataElementIdA;

  private long dataElementIdB;

  private long dataElementIdC;

  private long dataElementIdD;

  private String expressionA;

  private String expressionB;

  private String descriptionA;

  private String descriptionB;

  @BeforeAll
  void setUp() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    DataElement dataElementD = createDataElement('D');
    dataElementIdA = dataElementService.addDataElement(dataElementA);
    dataElementIdB = dataElementService.addDataElement(dataElementB);
    dataElementIdC = dataElementService.addDataElement(dataElementC);
    dataElementIdD = dataElementService.addDataElement(dataElementD);
    expressionA = "[" + dataElementIdA + "] + [" + dataElementIdB + "]";
    expressionB = "[" + dataElementIdC + "] - [" + dataElementIdD + "]";
    descriptionA = "Expression A";
    descriptionB = "Expression B";
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void testAddGetExpression() {
    Expression expr = new Expression(expressionA, descriptionA);
    expressionStore.save(expr);
    long id = expr.getId();
    expr = expressionStore.get(id);
    assertEquals(expr.getExpression(), expressionA);
    assertEquals(expr.getDescription(), descriptionA);
  }

  @Test
  void testUpdateExpression() {
    Expression expr = new Expression(expressionA, descriptionA);
    expressionStore.save(expr);
    long id = expr.getId();
    expr = expressionStore.get(id);
    assertEquals(expr.getExpression(), expressionA);
    assertEquals(expr.getDescription(), descriptionA);
    expr.setExpression(expressionB);
    expr.setDescription(descriptionB);
    expressionStore.update(expr);
    expr = expressionStore.get(id);
    assertEquals(expr.getExpression(), expressionB);
    assertEquals(expr.getDescription(), descriptionB);
  }

  @Test
  void testDeleteExpression() {
    Expression exprA = new Expression(expressionA, descriptionA);
    Expression exprB = new Expression(expressionB, descriptionB);
    expressionStore.save(exprA);
    long idA = exprA.getId();
    expressionStore.save(exprB);
    long idB = exprB.getId();
    assertNotNull(expressionStore.get(idA));
    assertNotNull(expressionStore.get(idB));
    expressionStore.delete(exprA);
    assertNull(expressionStore.get(idA));
    assertNotNull(expressionStore.get(idB));
    expressionStore.delete(exprB);
    assertNull(expressionStore.get(idA));
    assertNull(expressionStore.get(idB));
  }

  @Test
  void testGetAllExpressions() {
    Expression exprA = new Expression(expressionA, descriptionA);
    Expression exprB = new Expression(expressionB, descriptionB);
    expressionStore.save(exprA);
    expressionStore.save(exprB);
    List<Expression> expressions = expressionStore.getAll();
    assertTrue(expressions.size() == 2);
    assertTrue(expressions.contains(exprA));
    assertTrue(expressions.contains(exprB));
  }

  // -------------------------------------------------------------------------
  // Mapping
  // -------------------------------------------------------------------------

  @Test
  void testIdGeneratorUsesHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
  }

  @Test
  void testPersisterMapsTypesNullabilityAndReadWriteCache() {
    EntityPersister persister = persister();

    assertInstanceOf(TextType.class, persister.getPropertyType("expression"));
    assertInstanceOf(StringType.class, persister.getPropertyType("description"));
    assertInstanceOf(BooleanType.class, persister.getPropertyType("slidingWindow"));
    CustomType type =
        assertInstanceOf(CustomType.class, persister.getPropertyType("missingValueStrategy"));
    EnumType<?> enumType = assertInstanceOf(EnumType.class, type.getUserType());
    assertFalse(enumType.isOrdinal());
    assertSame(MissingValueStrategy.class, enumType.returnedClass());
    assertArrayEquals(new int[] {Types.VARCHAR}, enumType.sqlTypes());

    // missingValueStrategy is deliberately nullable (Flyway has NOT NULL) to keep schema
    // required:false
    for (String property :
        List.of(
            "expression", "description", "slidingWindow", "missingValueStrategy", "translations")) {
      assertTrue(isNullable(persister, property), property);
    }

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  void testColumnsAreWrittenAndReadBack() {
    Expression expression =
        new Expression("#{deUid.cocUid} * 2", "Round trip", MissingValueStrategy.NEVER_SKIP);
    expression.setSlidingWindow(true);
    expression.setTranslations(
        new HashSet<>(Set.of(new Translation(Locale.of("fr"), "DESCRIPTION", "Aller-retour"))));
    expressionStore.save(expression);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            select expression, description, slidingwindow, missingvaluestrategy,
              jsonb_array_length(translations) as translationcount,
              translations->0->>'locale' as locale, translations->0->>'property' as property,
              translations->0->>'value' as value
            from expression where expressionid = ?""",
            expression.getId());
    assertEquals("#{deUid.cocUid} * 2", row.get("expression"));
    assertEquals("Round trip", row.get("description"));
    assertEquals(true, row.get("slidingwindow"));
    assertEquals("NEVER_SKIP", row.get("missingvaluestrategy"));
    assertEquals(1, row.get("translationcount"));
    assertEquals("fr", row.get("locale"));
    assertEquals("DESCRIPTION", row.get("property"));
    assertEquals("Aller-retour", row.get("value"));

    jdbcTemplate.update(
        """
        update expression set expression = '#{deUid} + 1', description = 'Changed',
          slidingwindow = false, missingvaluestrategy = 'SKIP_IF_ANY_VALUE_MISSING',
          translations = '[{"locale": "es", "property": "DESCRIPTION", "value": "Ida y vuelta"}]'
        where expressionid = ?""",
        expression.getId());

    Expression reloaded = reload(expression);
    assertEquals("#{deUid} + 1", reloaded.getExpression());
    assertEquals("Changed", reloaded.getDescription());
    assertEquals(false, reloaded.getSlidingWindow());
    assertSame(MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING, reloaded.getMissingValueStrategy());
    assertEquals(
        Set.of(new Translation(Locale.of("es"), "DESCRIPTION", "Ida y vuelta")),
        reloaded.getTranslations());
  }

  @Test
  void testNullTranslationsAreStoredAsEmptyArray() {
    Expression expression = new Expression("1", "No translations");
    expression.setTranslations(null);
    expressionStore.save(expression);
    entityManager.flush();

    assertEquals(
        "[]",
        jdbcTemplate.queryForObject(
            "select translations::text from expression where expressionid = ?",
            String.class,
            expression.getId()));
    assertEquals(Set.of(), reload(expression).getTranslations());
  }

  // -------------------------------------------------------------------------
  // Ownership by validation rules and predictors
  // -------------------------------------------------------------------------

  @Test
  void testValidationRuleCascadesSaveUpdateReplaceAndDeleteToItsExpressions() {
    Expression left = new Expression("1 + 1", "Left");
    Expression right = new Expression("2 + 2", "Right");
    ValidationRule rule = createValidationRule('Q', equal_to, left, right, periodType);
    validationRuleService.saveValidationRule(rule);
    clearSessionAndSecondLevelCache();
    assertEquals(
        Map.of(left.getId(), "1 + 1", right.getId(), "2 + 2"),
        expressionRows(left.getId(), right.getId()));

    ValidationRule loaded = validationRuleService.getValidationRule(rule.getId());
    loaded.getLeftSide().setExpression("3 + 3");
    Expression replacement = new Expression("4 + 4", "Replacement");
    loaded.setRightSide(replacement);
    validationRuleService.updateValidationRule(loaded);
    clearSessionAndSecondLevelCache();
    assertEquals(
        Map.of(left.getId(), "3 + 3", replacement.getId(), "4 + 4"),
        expressionRows(left.getId(), right.getId(), replacement.getId()));
    assertEquals(
        Map.of("leftexpressionid", left.getId(), "rightexpressionid", replacement.getId()),
        jdbcTemplate.queryForMap(
            "select leftexpressionid, rightexpressionid from validationrule"
                + " where validationruleid = ?",
            rule.getId()));

    validationRuleService.deleteValidationRule(
        validationRuleService.getValidationRule(rule.getId()));
    clearSessionAndSecondLevelCache();
    assertEquals(Map.of(), expressionRows(left.getId(), right.getId(), replacement.getId()));
  }

  @Test
  void testPredictorCascadesSaveUpdateReplaceAndDeleteToItsExpressions() {
    DataElement output = createDataElement('X');
    dataElementService.addDataElement(output);
    Expression generator = new Expression("#{deUid}", "Generator");
    Expression skipTest = new Expression("#{deUid} == 0", "Skip test");
    Predictor predictor =
        createPredictor(
            output,
            categoryService.getDefaultCategoryOptionCombo(),
            "Q",
            generator,
            skipTest,
            periodType,
            new HashSet<>(),
            1,
            0,
            0);
    predictorService.addPredictor(predictor);
    clearSessionAndSecondLevelCache();
    assertEquals(
        Map.of(generator.getId(), "#{deUid}", skipTest.getId(), "#{deUid} == 0"),
        expressionRows(generator.getId(), skipTest.getId()));

    Predictor loaded = predictorService.getPredictor(predictor.getId());
    loaded.getGenerator().setExpression("#{deUid} * 2");
    Expression replacement = new Expression("#{deUid} < 0", "Replacement");
    loaded.setSampleSkipTest(replacement);
    predictorService.updatePredictor(loaded);
    clearSessionAndSecondLevelCache();
    assertEquals(
        Map.of(generator.getId(), "#{deUid} * 2", replacement.getId(), "#{deUid} < 0"),
        expressionRows(generator.getId(), skipTest.getId(), replacement.getId()));
    assertEquals(
        Map.of(
            "generatorexpressionid",
            generator.getId(),
            "skiptestexpressionid",
            replacement.getId()),
        jdbcTemplate.queryForMap(
            "select generatorexpressionid, skiptestexpressionid from predictor"
                + " where predictorid = ?",
            predictor.getId()));

    predictorService.deletePredictor(predictorService.getPredictor(predictor.getId()));
    clearSessionAndSecondLevelCache();
    assertEquals(
        Map.of(), expressionRows(generator.getId(), skipTest.getId(), replacement.getId()));
  }

  /** Reloads must hit the database, not the session or the read-write second-level cache. */
  private void clearSessionAndSecondLevelCache() {
    clearSession();
    entityManagerFactory.getCache().evictAll();
  }

  private Expression reload(Expression expression) {
    clearSessionAndSecondLevelCache();
    return expressionStore.get(expression.getId());
  }

  /** The expression column of the existing rows among the given ids, by id. */
  private Map<Long, String> expressionRows(long... ids) {
    String idList = LongStream.of(ids).mapToObj(Long::toString).collect(Collectors.joining(","));
    Map<Long, String> rows = new HashMap<>();
    jdbcTemplate.query(
        "select expressionid, expression from expression where expressionid in (" + idList + ")",
        rs -> {
          rows.put(rs.getLong("expressionid"), rs.getString("expression"));
        });
    return rows;
  }

  private static boolean isNullable(EntityPersister persister, String property) {
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(Expression.class);
  }
}
