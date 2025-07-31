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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hisp.dhis.schema.Schema;

public class FieldsParser {
  /** Fields token that includes all fields. */
  private static final String TOKEN_ALL = "*";

  public static final Function<Schema, Set<String>> PRESET_ALL = (s) -> Set.of(TOKEN_ALL);

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

  @Nonnull
  private static FieldsAccumulator parseFields(String input, Set<String> unexcludableTokens) {
    unexcludableTokens.add(TOKEN_ALL);

    FieldsAccumulator root = new FieldsAccumulator();
    Stack<FieldsAccumulator> stack = new Stack<>();
    stack.push(root);

    int i = 0;
    int fieldStart = i;
    boolean inField = false;
    boolean isExclusion = false;
    while (i < input.length()) {
      if ((input.charAt(i) == ',')) {
        if (inField) {
          stack.peek().add(parseField(input, fieldStart, i), isExclusion, unexcludableTokens);

          inField = false;
          isExclusion = false;
        }
      } else if (input.charAt(i) == '[' || input.charAt(i) == '(') {
        if (!inField) { // fields=[value] is ignored, ideally we would reject this
          throw new IllegalArgumentException("Block must have a field name like orgUnits[code]");
        }

        String parent = parseField(input, fieldStart, i);
        stack.peek().add(parent, isExclusion, unexcludableTokens);

        stack.push(stack.peek().getOrCreateChild(parent));
        inField = false;
        isExclusion = false;
      } else if (input.charAt(i) == ']' || input.charAt(i) == ')') {
        if (stack.size() == 1) {
          throw new IllegalArgumentException("Unbalanced parens/brackets in input: " + input);
        }

        if (inField) {
          stack.peek().add(parseField(input, fieldStart, i), isExclusion, unexcludableTokens);
        }

        stack.pop();
        inField = false;
        isExclusion = false;
      } else if (input.charAt(i) == '!' && !inField) {
        inField = true;
        isExclusion = true;
        fieldStart = i + 1; // do not includes ! in field name
      } else if (!Character.isWhitespace(input.charAt(i)) && !inField) {
        inField = true;
        isExclusion = false;
        fieldStart = i;
      }
      i++;
    }

    if (inField) {
      stack.peek().add(parseField(input, fieldStart, i), isExclusion, unexcludableTokens);
    }
    // this is where we should check if stack size is > 1 and err as a bracket/paren
    // fields="group[name" was not closed
    return root;
  }

  /**
   * The current {@code FieldFilterParser} has this behavior. We check for whitespace in the field
   * and remove it if present. Ideally we would not support this and only ignore leading and
   * trailing whitespace.
   */
  private static String parseField(String input, int start, int end) {
    String field = input.substring(start, end);

    boolean hasWhitespace = false;
    for (int j = 0; j < field.length(); j++) {
      if (Character.isWhitespace(field.charAt(j))) {
        hasWhitespace = true;
        break;
      }
    }

    if (!hasWhitespace) {
      return field;
    }

    StringBuilder sb = new StringBuilder(field.length());
    for (int j = 0; j < field.length(); j++) {
      char c = field.charAt(j);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
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

    return new Fields(includesAll, fields, children, Map.of());
  }

  /**
   * Accumulates included and excluded field names as well as presets and * in a tree like structure
   * representing the (nested) fields expressions.
   */
  private static final class FieldsAccumulator {
    Set<String> includes = new HashSet<>();
    final Set<String> excludes = new HashSet<>();
    final Map<String, FieldsAccumulator> children = new HashMap<>();

    void add(String field, boolean isExclusion, Set<String> unexcludableTokens) {
      if (!isExclusion || unexcludableTokens.contains(field)) {
        this.includes.add(field);
      } else {
        this.excludes.add(field);
      }
    }

    FieldsAccumulator getOrCreateChild(String field) {
      return children.computeIfAbsent(field, k -> new FieldsAccumulator());
    }
  }
}
