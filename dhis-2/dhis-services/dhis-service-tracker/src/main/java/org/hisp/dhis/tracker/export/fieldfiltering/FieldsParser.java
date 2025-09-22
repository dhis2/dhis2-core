/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.fieldfiltering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;

public class FieldsParser {
  /** Fields token that includes all fields. */
  private static final String TOKEN_ALL = "*";

  public static final Function<Schema, Set<String>> PRESET_ALL = s -> Set.of(TOKEN_ALL);

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile(
"""
          (?<COLONCOLON>::)|(?<TILDE>~)|(?<PIPE>\\|)|(?<BRACKETOPEN>\\[)|(?<BRACKETCLOSE>\\])|(?<PARENOPEN>\\()|(?<PARENCLOSE>\\))|(?<COMMA>,)|(?<SEMICOLON>;)|(?<BANG>!)|(?<NAME>(?:[^,\\[\\]()~|;:!]|:(?!:))+)
"""
              .trim());

  private static final Map<String, TokenType> GROUP_TO_TOKEN;

  static {
    Map<String, TokenType> map = new HashMap<>();
    map.put("NAME", TokenType.NAME);
    map.put("COMMA", TokenType.COMMA);
    map.put("COLONCOLON", TokenType.COLON_COLON);
    map.put("TILDE", TokenType.TILDE);
    map.put("PIPE", TokenType.PIPE);
    map.put("BRACKETOPEN", TokenType.BRACKET_OPEN);
    map.put("BRACKETCLOSE", TokenType.BRACKET_CLOSE);
    map.put("PARENOPEN", TokenType.PAREN_OPEN);
    map.put("PARENCLOSE", TokenType.PAREN_CLOSE);
    map.put("SEMICOLON", TokenType.SEMICOLON);
    map.put("BANG", TokenType.BANG);
    GROUP_TO_TOKEN = Map.copyOf(map);
  }

  enum TokenType {
    NAME,
    COMMA,
    COLON_COLON,
    TILDE,
    PIPE,
    BRACKET_OPEN,
    BRACKET_CLOSE,
    PAREN_OPEN,
    PAREN_CLOSE,
    SEMICOLON,
    BANG
  }

  record Token(TokenType type, String value, int start, int end) {}

  /**
   * Parse fields and expand presets using given {@code presets} functions. Presets cannot be
   * excluded i.e. fields=!:simple is equivalent to fields=:simple.
   *
   * <p>Use {@link #parse(String)} to parse fields without expanding presets.
   */
  @Nonnull
  public static Fields parse(
      @Nonnull String input,
      @Nonnull Schema schema,
      @Nonnull BiFunction<Schema, String, Schema> getSchema,
      @Nonnull Map<String, Function<Schema, Set<String>>> presets) {
    FieldsAccumulator root = parseFields(input, new HashSet<>(presets.keySet()));
    mapPresets(root, schema, getSchema, presets);
    expandPaths(root, schema, getSchema);
    return map(root, root.includes.contains(TOKEN_ALL));
  }

  /**
   * Parse fields without expanding presets. Presets will be treated like any other field name. The
   * only exception is that presets cannot be excluded i.e. fields=!:simple is equivalent to
   * fields=:simple.
   *
   * <p>Use {@link #parse(String, Schema, BiFunction, Map)} to register and expand presets.
   */
  @Nonnull
  public static Fields parse(@Nonnull String input) {
    FieldsAccumulator root = parseFields(input, new HashSet<>());
    return map(root, root.includes.contains(TOKEN_ALL));
  }

  private static FieldsAccumulator parseFields(String input, Set<String> unexcludableTokens) {
    List<Token> tokens = tokenize(input);
    unexcludableTokens.add(TOKEN_ALL);

    FieldsAccumulator root = new FieldsAccumulator();
    Stack<FieldsAccumulator> stack = new Stack<>();
    stack.push(root);

    Parser parser = new Parser(unexcludableTokens);

    for (Token token : tokens) {
      switch (token.type) {
        case BANG:
          parser.isExclusion = true;
          break;

        case NAME:
          if (parser.parsingTransformationParams) {
            parser.transformationParams.add(token.value.trim());
          } else if (parser.pendingTransformation != null) {
            if (!parser.pendingTransformation.isEmpty()) {
              parser.currentTransformation.add(new Transformation(parser.pendingTransformation));
            }
            parser.pendingTransformation = token.value.trim();
          } else if (parser.currentField == null) {
            parser.currentField = removeAllWhitespace(token.value);
          } else {
            parser.consumeCurrentField(stack.peek());
            parser.currentField = removeAllWhitespace(token.value);
          }
          break;

        case COLON_COLON, TILDE, PIPE:
          if (parser.pendingTransformation != null && !parser.pendingTransformation.isEmpty()) {
            parser.currentTransformation.add(new Transformation(parser.pendingTransformation));
          }
          parser.pendingTransformation = "";
          break;

        case PAREN_OPEN:
          if (parser.pendingTransformation != null) {
            parser.parsingTransformationParams = true;
            parser.transformationParams.clear();
          } else {
            if (parser.currentField == null) {
              throw new IllegalArgumentException(
                  "Block must have a field name like orgUnits[code]");
            }
            String fieldName = parser.currentField;
            parser.consumeCurrentField(stack.peek());
            stack.push(stack.peek().getOrCreateChild(fieldName));
          }
          break;

        case PAREN_CLOSE:
          if (parser.parsingTransformationParams) {
            parser.currentTransformation.add(
                new Transformation(
                    parser.pendingTransformation,
                    parser.transformationParams.toArray(new String[0])));
            parser.pendingTransformation = null;
            parser.transformationParams.clear();
            parser.parsingTransformationParams = false;
          } else {
            if (stack.size() == 1) {
              throw new IllegalArgumentException("Unbalanced parens/brackets in input");
            }
            if (parser.currentField != null) {
              parser.consumeCurrentField(stack.peek());
            }
            stack.pop();
          }
          break;

        case BRACKET_OPEN:
          if (parser.currentField == null) {
            throw new IllegalArgumentException("Block must have a field name like orgUnits[code]");
          }
          String fieldName = parser.currentField;
          parser.consumeCurrentField(stack.peek());
          stack.push(stack.peek().getOrCreateChild(fieldName));
          break;

        case BRACKET_CLOSE:
          if (stack.size() == 1) {
            throw new IllegalArgumentException("Unbalanced parens/brackets in input");
          }
          if (parser.currentField != null) {
            parser.consumeCurrentField(stack.peek());
          }
          stack.pop();
          break;

        case COMMA:
          if (!parser.parsingTransformationParams && parser.currentField != null) {
            parser.consumeCurrentField(stack.peek());
          }
          break;

        case SEMICOLON:
          break;
      }
    }

    if (parser.currentField != null) {
      parser.consumeCurrentField(stack.peek());
    }

    return root;
  }

  private static List<Token> tokenize(String input) {
    List<Token> tokens = new ArrayList<>();
    Matcher matcher = TOKEN_PATTERN.matcher(input);
    while (matcher.find()) {
      TokenType tokenType =
          GROUP_TO_TOKEN.entrySet().stream()
              .filter(entry -> matcher.group(entry.getKey()) != null)
              .map(Map.Entry::getValue)
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Regex matched text '"
                              + matcher.group()
                              + "' at position "
                              + matcher.start()
                              + " but no named group captured it. This indicates a programmer error in the regex pattern or GROUP_TO_TOKEN mapping."));
      tokens.add(new Token(tokenType, matcher.group(), matcher.start(), matcher.end()));
    }

    return tokens;
  }

  /**
   * This is the behavior of the {@code FieldFilterParser} so we kept it for backwards
   * compatibility.
   */
  private static String removeAllWhitespace(String fieldName) {
    StringBuilder sb = new StringBuilder(fieldName.length());
    for (int i = 0; i < fieldName.length(); i++) {
      char c = fieldName.charAt(i);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static class Parser {
    /** {@code *} and presets cannot be excluded, {@code !} is ignored. */
    final Set<String> unexcludableTokens;

    String currentField = null;
    List<Transformation> currentTransformation = new ArrayList<>();
    boolean isExclusion = false;

    String pendingTransformation = null;
    List<String> transformationParams = new ArrayList<>();
    boolean parsingTransformationParams = false;

    Parser(Set<String> unexcludableTokens) {
      this.unexcludableTokens = unexcludableTokens;
    }

    void consumeCurrentField(FieldsAccumulator accumulator) {
      if (currentField != null && !currentField.isEmpty()) {
        if (pendingTransformation != null && !pendingTransformation.isEmpty()) {
          currentTransformation.add(new Transformation(pendingTransformation));
        }
        accumulator.add(
            currentField, isExclusion, unexcludableTokens, new ArrayList<>(currentTransformation));
      }
      reset();
    }

    private void reset() {
      currentField = null;
      currentTransformation.clear();
      isExclusion = false;
      pendingTransformation = null;
      transformationParams.clear();
      parsingTransformationParams = false;
    }
  }

  private static void mapPresets(
      FieldsAccumulator acc,
      Schema schema,
      BiFunction<Schema, String, Schema> getSchema,
      Map<String, Function<Schema, Set<String>>> presets) {
    acc.includes =
        acc.includes.stream()
            .flatMap(
                field -> presets.getOrDefault(field, s -> Set.of(field)).apply(schema).stream())
            .collect(Collectors.toSet());

    for (Entry<String, FieldsAccumulator> entry : acc.children.entrySet()) {
      Schema parent = getSchema.apply(schema, entry.getKey());
      if (parent == null) {
        continue; // invalid field
      }
      mapPresets(entry.getValue(), parent, getSchema, presets);
    }
  }

  /**
   * Expands paths for metadata objects similar to FieldPathHelper.applyDefaults(). For reference
   * objects: expands "dataSets" to "dataSets.id" For complex objects: expands "complexField" to
   * "complexField[*]" Only applies to objects with proper metadata schemas. Tracker view classes
   * and other objects without registered schemas are automatically skipped.
   */
  private static void expandPaths(
      FieldsAccumulator acc, Schema schema, BiFunction<Schema, String, Schema> getSchema) {
    if (schema == null) {
      // No schema available - skip expansion
      return;
    }

    Set<String> fieldsToExpand = new HashSet<>();
    Set<String> expandedFields = new HashSet<>();

    for (String fieldName : acc.includes) {
      if (fieldName.equals(TOKEN_ALL)) {
        continue; // Skip *
      }
      // Note: Valid presets like :identifiable have already been expanded by mapPresets()
      // Any remaining :prefixed fields are invalid and will be treated as regular field names

      Property property = schema.getProperty(fieldName);
      if (property == null) {
        continue; // Invalid field
      }

      // Check if this field needs expansion and doesn't already have children
      if (needsExpansion(property) && !acc.children.containsKey(fieldName)) {
        fieldsToExpand.add(fieldName);

        if (isReference(property)) {
          // Reference objects expand to .id
          expandedFields.add(fieldName);
          FieldsAccumulator child = acc.getOrCreateChild(fieldName);
          child.includes.add("id");
        } else if (isComplex(property)) {
          // Complex objects expand to [*]
          expandedFields.add(fieldName);
          FieldsAccumulator child = acc.getOrCreateChild(fieldName);
          child.includes.add(TOKEN_ALL);
        }
      }
    }

    // Remove the original unexpanded fields, keep the expanded ones
    acc.includes.removeAll(fieldsToExpand);
    acc.includes.addAll(expandedFields);

    // Recursively expand child paths
    for (Entry<String, FieldsAccumulator> entry : acc.children.entrySet()) {
      Schema childSchema = getSchema.apply(schema, entry.getKey());
      if (childSchema != null) {
        expandPaths(entry.getValue(), childSchema, getSchema);
      }
    }
  }

  private static boolean needsExpansion(Property property) {
    return isReference(property) || isComplex(property);
  }

  private static boolean isReference(Property property) {
    return property.is(PropertyType.REFERENCE) || property.itemIs(PropertyType.REFERENCE);
  }

  private static boolean isComplex(Property property) {
    return property.is(PropertyType.COMPLEX) || property.itemIs(PropertyType.COMPLEX);
  }

  /** Maps in depth-first search order each field and its children to {@link Fields}. */
  private static Fields map(FieldsAccumulator acc, boolean includesAll) {
    // fields with `[]` i.e. fields=dataValues[value] will have accumulated children processed here
    Map<String, Fields> children = new HashMap<>();
    for (Entry<String, FieldsAccumulator> entry : acc.children.entrySet()) {
      // Don't process children of excluded parents as they will be excluded anyway
      if (isFieldExcluded(entry.getKey(), acc, includesAll)) {
        continue;
      }

      boolean includeChildren =
          entry.getValue().includes.contains(TOKEN_ALL) // fields=dataValues[*] all are included
              || entry
                  .getValue()
                  .includes
                  .isEmpty(); // fields=dataValues[!value] all but value are included
      children.put(entry.getKey(), map(entry.getValue(), includeChildren));
    }
    acc.includes.remove(TOKEN_ALL);

    Set<String> fields = includesAll ? acc.excludes : new HashSet<>(acc.includes);
    if (!includesAll) {
      // exclusion has precedence over inclusion
      fields.removeAll(acc.excludes);
    }

    Map<String, List<Fields.Transformation>> transformations =
        mapTransformations(acc.transformations);

    return new Fields(includesAll, fields, children, transformations);
  }

  private static boolean isFieldExcluded(
      String fieldName, FieldsAccumulator acc, boolean includesAll) {
    if (includesAll) {
      return acc.excludes.contains(fieldName);
    } else {
      return acc.excludes.contains(fieldName) || !acc.includes.contains(fieldName);
    }
  }

  private static Map<String, List<Fields.Transformation>> mapTransformations(
      Map<String, List<Transformation>> transformations) {
    validateTransformations(transformations);
    return sortAndConvertTransformations(transformations);
  }

  private static void validateTransformations(Map<String, List<Transformation>> transformations) {
    validateUnknownTransformations(transformations);
    validateDuplicateTransformations(transformations);
    validateTransformationArguments(transformations);
  }

  private static void validateUnknownTransformations(
      Map<String, List<Transformation>> transformations) {
    String unknown =
        transformations.entrySet().stream()
            .flatMap(e -> e.getValue().stream())
            .map(t -> t.name)
            .filter(n -> !FieldsTransformer.TRANSFORMERS.containsKey(n))
            .collect(Collectors.joining(", "));
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid field transformer(s): "
              + unknown
              + ". Valid ones are: "
              + FieldsTransformer.TRANSFORMERS.keySet());
    }
  }

  private static void validateDuplicateTransformations(
      Map<String, List<Transformation>> transformations) {
    String duplicates =
        transformations.entrySet().stream()
            .map(
                e -> {
                  Set<String> transformers =
                      CollectionUtils.findDuplicates(
                          e.getValue().stream().map(t -> t.name).toList());
                  if (!transformers.isEmpty()) {
                    return "'" + e.getKey() + "' " + String.join(", ", transformers);
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException("Duplicate transformers for fields: " + duplicates + ".");
    }
  }

  private static void validateTransformationArguments(
      Map<String, List<Transformation>> transformations) {
    String errors =
        transformations.entrySet().stream()
            .flatMap(
                e ->
                    e.getValue().stream()
                        .map(
                            t ->
                                FieldsTransformer.TRANSFORMERS_VALIDATOR
                                    .getOrDefault(t.name, (name, f, a) -> null)
                                    .validate(t.name, e.getKey(), t.arguments)))
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(errors);
    }
  }

  private static Map<String, List<Fields.Transformation>> sortAndConvertTransformations(
      Map<String, List<Transformation>> transformations) {
    Map<String, List<Transformation>> sortedTransformations =
        sortTransformationsByRenameRule(transformations);
    return convertToFieldsTransformations(sortedTransformations);
  }

  /**
   * Renaming must be the last transformation applied to a field, so we sort transformations
   * accordingly.
   */
  private static Map<String, List<Transformation>> sortTransformationsByRenameRule(
      Map<String, List<Transformation>> transformations) {
    Map<String, List<Transformation>> sortedTransformations = new HashMap<>();
    for (Entry<String, List<Transformation>> entry : transformations.entrySet()) {
      List<Transformation> sorted =
          entry.getValue().stream()
              .sorted(
                  (t1, t2) -> {
                    boolean t1IsRename = "rename".equals(t1.name());
                    boolean t2IsRename = "rename".equals(t2.name());
                    if (t1IsRename && !t2IsRename) return 1;
                    if (!t1IsRename && t2IsRename) return -1;
                    return 0;
                  })
              .toList();
      sortedTransformations.put(entry.getKey(), sorted);
    }
    return sortedTransformations;
  }

  /** Transformations only use one argument, if any so we only keep the first one. */
  private static Map<String, List<Fields.Transformation>> convertToFieldsTransformations(
      Map<String, List<Transformation>> sortedTransformations) {
    Map<String, List<Fields.Transformation>> result = new HashMap<>(sortedTransformations.size());
    for (Entry<String, List<Transformation>> entry : sortedTransformations.entrySet()) {
      List<Fields.Transformation> ts =
          entry.getValue().stream()
              .map(
                  t -> {
                    String argument =
                        (t.arguments == null || t.arguments.length == 0) ? null : t.arguments[0];
                    return new Fields.Transformation(
                        t.name, FieldsTransformer.TRANSFORMERS.get(t.name), argument);
                  })
              .toList();
      result.put(entry.getKey(), ts);
    }
    return result;
  }

  /**
   * Accumulates included and excluded field names as well as presets and * in a tree like structure
   * representing the (nested) fields expressions.
   */
  private static final class FieldsAccumulator {
    Set<String> includes = new HashSet<>();
    final Set<String> excludes = new HashSet<>();
    final Map<String, FieldsAccumulator> children = new HashMap<>();
    final Map<String, List<Transformation>> transformations = new HashMap<>();

    void add(
        String field,
        boolean isExclusion,
        Set<String> unexcludableTokens,
        List<Transformation> transformers) {
      if (!isExclusion || unexcludableTokens.contains(field)) {
        this.includes.add(field);
      } else {
        this.excludes.add(field);
      }

      if (!transformers.isEmpty()) {
        this.transformations.put(field, transformers);
      }
    }

    FieldsAccumulator getOrCreateChild(String field) {
      return children.computeIfAbsent(field, k -> new FieldsAccumulator());
    }
  }

  private record Transformation(String name, String... arguments) {
    @Override
    public @Nonnull String toString() {
      return name + "(" + String.join(",", arguments) + ")";
    }
  }
}
