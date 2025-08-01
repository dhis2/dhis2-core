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
package org.hisp.dhis.fieldfiltering.better;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hisp.dhis.schema.Schema;

public class FieldsParser {
  /** Fields token that includes all fields. */
  private static final String TOKEN_ALL = "*";

  public static final Function<Schema, Set<String>> PRESET_ALL = (s) -> Set.of(TOKEN_ALL);

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile(
"""
          (?<COLONCOLON>::)|(?<TILDE>~)|(?<PIPE>\\|)|(?<BRACKETOPEN>\\[)|(?<BRACKETCLOSE>\\])|(?<PARENOPEN>\\()|(?<PARENCLOSE>\\))|(?<COMMA>,)|(?<NAME>!?(?:[^,\\[\\]()~|;:]|:(?!:))+)
"""
              .trim());

  private static final Map<String, TokenType> GROUP_TO_TOKEN =
      Map.of(
          "NAME", TokenType.NAME,
          "COMMA", TokenType.COMMA,
          "COLONCOLON", TokenType.COLON_COLON,
          "TILDE", TokenType.TILDE,
          "PIPE", TokenType.PIPE,
          "BRACKETOPEN", TokenType.BRACKET_OPEN,
          "BRACKETCLOSE", TokenType.BRACKET_CLOSE,
          "PARENOPEN", TokenType.PAREN_OPEN,
          "PARENCLOSE", TokenType.PAREN_CLOSE);

  private static final Set<TokenType> TRANSFORMERS =
      Set.of(TokenType.COLON_COLON, TokenType.TILDE, TokenType.PIPE);

  enum TokenType {
    NAME,
    COMMA,
    COLON_COLON,
    TILDE,
    PIPE,
    BRACKET_OPEN,
    BRACKET_CLOSE,
    PAREN_OPEN,
    PAREN_CLOSE
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

    tokens = mergeTransformerTokens(tokens);

    Token currentField = null;
    boolean isExclusion = false;

    for (Token token : tokens) {
      switch (token.type) {
        case NAME:
          if (currentField != null) {
            // Process previous field
            String fieldName = parseFieldName(currentField.value);
            List<Fields.Transformation> transformers = parseTransformers(currentField.value);
            stack.peek().add(fieldName, isExclusion, unexcludableTokens, transformers);
          }

          // Set up new field
          currentField = token;
          isExclusion = token.value.startsWith("!");
          break;

        case COMMA:
          if (currentField != null) {
            String fieldName = parseFieldName(currentField.value);
            List<Fields.Transformation> transformers = parseTransformers(currentField.value);
            stack.peek().add(fieldName, isExclusion, unexcludableTokens, transformers);
            currentField = null;
            isExclusion = false;
          }
          break;

        case BRACKET_OPEN:
        case PAREN_OPEN:
          if (currentField == null) {
            throw new IllegalArgumentException("Block must have a field name like orgUnits[code]");
          }

          String parent = parseFieldName(currentField.value);
          List<Fields.Transformation> parentTransformers = parseTransformers(currentField.value);
          stack.peek().add(parent, isExclusion, unexcludableTokens, parentTransformers);
          stack.push(stack.peek().getOrCreateChild(parent));
          currentField = null;
          isExclusion = false;
          break;

        case BRACKET_CLOSE:
        case PAREN_CLOSE:
          if (stack.size() == 1) {
            throw new IllegalArgumentException("Unbalanced parens/brackets in input");
          }

          if (currentField != null) {
            String fieldName = parseFieldName(currentField.value);
            List<Fields.Transformation> transformers = parseTransformers(currentField.value);
            stack.peek().add(fieldName, isExclusion, unexcludableTokens, transformers);
            currentField = null;
            isExclusion = false;
          }

          stack.pop();
          break;
      }
    }

    if (currentField != null) {
      String fieldName = parseFieldName(currentField.value);
      List<Fields.Transformation> transformers = parseTransformers(currentField.value);
      stack.peek().add(fieldName, isExclusion, unexcludableTokens, transformers);
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

  private static List<Token> mergeTransformerTokens(List<Token> tokens) {
    List<Token> merged = new ArrayList<>();

    for (int i = 0; i < tokens.size(); i++) {
      Token token = tokens.get(i);

      // Check if this is a field name that starts a transformer sequence
      if (token.type == TokenType.NAME && hasTransformerSequence(tokens, i)) {

        // Merge field name + transformer sequence + parameters
        StringBuilder fullField = new StringBuilder(token.value);
        int j = i + 1;

        // Merge transformer tokens (::, ~, |) and transformer names and parameters
        while (j < tokens.size()) {
          Token t = tokens.get(j);

          // If we hit a comma (field separator), stop merging
          if (t.type == TokenType.COMMA) {
            break;
          }

          // If we hit bracket delimiters (structural), stop merging
          if (t.type == TokenType.BRACKET_OPEN || t.type == TokenType.BRACKET_CLOSE) {
            break;
          }

          // Special case: if it's PAREN_OPEN after a transformer name, merge parameters
          if (t.type == TokenType.PAREN_OPEN && isAfterTransformer(tokens, j)) {
            // Merge parentheses and their contents, including semicolons as commas
            int parenCount = 0;
            while (j < tokens.size()) {
              Token pToken = tokens.get(j);
              if (pToken.type == TokenType.PAREN_OPEN) {
                fullField.append(pToken.value);
                parenCount++;
              } else if (pToken.type == TokenType.PAREN_CLOSE) {
                fullField.append(pToken.value);
                parenCount--;
              } else if (pToken.type == TokenType.NAME) {
                // Add the parameter name
                fullField.append(pToken.value);
                // Check if next token might be a separator for this parameter
                if (j + 1 < tokens.size()
                    && tokens.get(j + 1).type == TokenType.NAME
                    && parenCount > 0) {
                  fullField.append(";"); // Add semicolon separator between parameters
                }
              }

              j++;
              if (parenCount == 0) break;
            }
            break;
          }

          // Merge transformer tokens and names
          if (TRANSFORMERS.contains(t.type) || t.type == TokenType.NAME) {
            fullField.append(t.value);
            j++;
          } else {
            break;
          }
        }

        // Create merged token
        merged.add(
            new Token(TokenType.NAME, fullField.toString(), token.start, tokens.get(j - 1).end));
        i = j - 1; // Skip the merged tokens

      } else {
        merged.add(token);
      }
    }

    return merged;
  }

  private static boolean hasTransformerSequence(List<Token> tokens, int startIndex) {
    // Look ahead to see if there's a transformer sequence after this name
    for (int i = startIndex + 1; i < tokens.size(); i++) {
      Token t = tokens.get(i);
      if (TRANSFORMERS.contains(t.type)) {
        return true;
      }
      if (t.type != TokenType.NAME) {
        break;
      }
    }
    return false;
  }

  private static boolean isAfterTransformer(List<Token> tokens, int parenIndex) {
    // Check if the previous non-NAME token was a transformer
    for (int i = parenIndex - 1; i >= 0; i--) {
      Token t = tokens.get(i);
      if (TRANSFORMERS.contains(t.type)) {
        return true;
      }
      if (t.type != TokenType.NAME) {
        break;
      }
    }
    return false;
  }

  /**
   * Parses field name and extracts transformers. Returns the base field name without transformers.
   */
  private static String parseFieldName(String field) {
    // Remove leading ! if present
    if (field.startsWith("!")) {
      field = field.substring(1);
    }

    // Extract base field name (everything before first transformer separator)
    int transformerStart = -1;
    for (int i = 0; i < field.length() - 1; i++) {
      if ((field.charAt(i) == ':' && field.charAt(i + 1) == ':')
          || field.charAt(i) == '~'
          || field.charAt(i) == '|') {
        transformerStart = i;
        break;
      }
    }

    String baseField = transformerStart >= 0 ? field.substring(0, transformerStart) : field;

    // Remove whitespace from base field name
    boolean hasWhitespace = false;
    for (int j = 0; j < baseField.length(); j++) {
      if (Character.isWhitespace(baseField.charAt(j))) {
        hasWhitespace = true;
        break;
      }
    }

    if (!hasWhitespace) {
      return baseField;
    }

    StringBuilder sb = new StringBuilder(baseField.length());
    for (int j = 0; j < baseField.length(); j++) {
      char c = baseField.charAt(j);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /** Parses transformers from a field string. */
  private static List<Fields.Transformation> parseTransformers(String field) {
    List<Fields.Transformation> transformers = new ArrayList<>();

    // Remove leading ! if present
    if (field.startsWith("!")) {
      field = field.substring(1);
    }

    // Find transformer part
    int transformerStart = -1;
    for (int i = 0; i < field.length() - 1; i++) {
      if ((field.charAt(i) == ':' && field.charAt(i + 1) == ':')
          || field.charAt(i) == '~'
          || field.charAt(i) == '|') {
        transformerStart = i;
        break;
      }
    }

    if (transformerStart < 0) {
      return transformers; // No transformers
    }

    String transformerPart = field.substring(transformerStart);

    // Parse individual transformers
    int i = 0;
    while (i < transformerPart.length()) {
      // Skip separator
      if (transformerPart.charAt(i) == ':'
          && i + 1 < transformerPart.length()
          && transformerPart.charAt(i + 1) == ':') {
        i += 2;
      } else if (transformerPart.charAt(i) == '~' || transformerPart.charAt(i) == '|') {
        i++;
      }

      // Extract transformer name
      int nameStart = i;
      while (i < transformerPart.length()
          && Character.isJavaIdentifierPart(transformerPart.charAt(i))) {
        i++;
      }

      if (nameStart == i) break; // No valid transformer name

      String transformerName = transformerPart.substring(nameStart, i);

      // Extract parameters if present
      List<String> params = new ArrayList<>();
      if (i < transformerPart.length() && transformerPart.charAt(i) == '(') {
        i++; // Skip opening paren
        int paramStart = i;
        int parenCount = 1;

        while (i < transformerPart.length() && parenCount > 0) {
          if (transformerPart.charAt(i) == '(') {
            parenCount++;
          } else if (transformerPart.charAt(i) == ')') {
            parenCount--;
          }
          i++;
        }

        if (parenCount == 0) {
          String paramString = transformerPart.substring(paramStart, i - 1);
          if (!paramString.isEmpty()) {
            // Split by semicolon
            for (String param : paramString.split(";")) {
              params.add(param.trim());
            }
          }
        }
      }

      transformers.add(new Fields.Transformation(transformerName, params.toArray(new String[0])));
    }

    return transformers;
  }

  private static void mapPresets(
      FieldsAccumulator acc,
      Schema schema,
      BiFunction<Schema, String, Schema> getSchema,
      Map<String, Function<Schema, Set<String>>> presets) {
    acc.includes =
        acc.includes.stream()
            .flatMap(
                field -> presets.getOrDefault(field, (s) -> Set.of(field)).apply(schema).stream())
            .collect(Collectors.toSet());

    for (Entry<String, FieldsAccumulator> entry : acc.children.entrySet()) {
      Schema parent = getSchema.apply(schema, entry.getKey());
      if (parent == null) {
        continue; // invalid field
      }
      mapPresets(entry.getValue(), parent, getSchema, presets);
    }
  }

  /** Maps in depth-first search order each field and its children to {@link Fields}. */
  private static Fields map(FieldsAccumulator acc, boolean includesAll) {
    // fields with `[]` i.e. fields=dataValues[value] will have accumulated children processed here
    Map<String, Fields> children = new HashMap<>();
    for (Entry<String, FieldsAccumulator> entry : acc.children.entrySet()) {
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

    return new Fields(includesAll, fields, children, acc.transformations);
  }

  /**
   * Accumulates included and excluded field names as well as presets and * in a tree like structure
   * representing the (nested) fields expressions.
   */
  private static final class FieldsAccumulator {
    Set<String> includes = new HashSet<>();
    final Set<String> excludes = new HashSet<>();
    final Map<String, FieldsAccumulator> children = new HashMap<>();
    final Map<String, List<Fields.Transformation>> transformations = new HashMap<>();

    void add(
        String field,
        boolean isExclusion,
        Set<String> unexcludableTokens,
        List<Fields.Transformation> transformers) {
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
}
