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
import java.util.Map;
import java.util.Stack;

public class FieldsParser {

  public static Fields parse(String input) {
    // TODO error handling: white space in a field name, special characters like * or : in a field
    // name, things like ', ,   ' or 'group[ ]' or a block without a name '[]'
    FieldsPredicate root = new FieldsPredicate();
    Stack<FieldsPredicate> stack = new Stack<>();
    stack.push(root);

    int i = 0;
    int fieldStart = i;
    boolean inField = false;
    boolean isFieldWithWhitespace = false;
    boolean isExclusion = false;
    while (i < input.length()) {
      if ((input.charAt(i) == ',')) {
        if (inField) {
          if (isExclusion) {
            stack.peek().exclude(parseField(input, fieldStart, i, isFieldWithWhitespace));
          } else {
            stack.peek().include(parseField(input, fieldStart, i, isFieldWithWhitespace));
          }

          inField = false;
          isFieldWithWhitespace = false;
          isExclusion = false;
        }
      } else if (input.charAt(i) == '[' || input.charAt(i) == '(') {
        // TODO fail if we are not inField? old parser allows this but the result makes no sense
        if (inField) {
          String parent = parseField(input, fieldStart, i, isFieldWithWhitespace);
          if (isExclusion) {
            stack.peek().exclude(parent);
          } else {
            stack.peek().include(parent);
          }

          FieldsPredicate child;
          if (stack.peek().getChildren().containsKey(parent)) {
            child = stack.peek().getChildren().get(parent);
          } else {
            // TODO what if the block is empty? what does that mean
            child = new FieldsPredicate();
            // TODO(ivo) does includeAll make sense if no fields follow? what about
            // fields=relationships,relationships[from] is this like :all,code where its already
            // settled that all is included? but relationships[from] should obviously not includeAll
            // child.includeAll();
            // start with tests on the FieldFilterServiceTest level or curl to see how the old
            // parser/service behaves. Interesting is also relationships[foo] that does not err but
            // does it count as relationships?
            stack.peek().getChildren().put(parent, child);
          }

          stack.push(child);
          inField = false;
          isFieldWithWhitespace = false;
          isExclusion = false;
        }
      } else if (input.charAt(i) == ']' || input.charAt(i) == ')') {
        if (stack.size() <= 1) {
          throw new IllegalArgumentException("Unbalanced brackets in input: " + input);
        }

        if (inField) {
          if (isExclusion) {
            stack.peek().exclude(parseField(input, fieldStart, i, isFieldWithWhitespace));
          } else {
            stack.peek().include(parseField(input, fieldStart, i, isFieldWithWhitespace));
          }
        }

        stack.pop();
        inField = false;
        isFieldWithWhitespace = false;
        isExclusion = false;
      } else if (input.charAt(i) == '*' && !inField) {
        stack.peek().includeAll();
      } else if (input.charAt(i) == '!' && !inField) {
        inField = true;
        isExclusion = true;
        fieldStart = i + 1; // do not include ! in field name
      } else if (Character.isWhitespace(input.charAt(i)) && inField) {
        isFieldWithWhitespace = true;
      } else if (!Character.isWhitespace(input.charAt(i)) && !inField) {
        inField = true;
        isFieldWithWhitespace = false;
        isExclusion = false;
        fieldStart = i;
      }
      i++;
    }

    if (inField) {
      if (isExclusion) {
        stack.peek().exclude(parseField(input, fieldStart, i, isFieldWithWhitespace));
      } else {
        stack.peek().include(parseField(input, fieldStart, i, isFieldWithWhitespace));
      }
    }
    // TODO this is where we could check if stack size is > 1 and err as a bracket/paren
    // "group[name" was not closed

    return convertToFields(root);
  }

  /**
   * The current {@code FieldFilterParser} has this behavior. We try to avoid building a string for
   * every field name as most will not have any whitespace inside of a field name. Ideally we would
   * not support this and only ignore leading and trailing whitespace.
   */
  private static String parseField(String input, int start, int end, boolean hasWhitespace) {
    String field = input.substring(start, end);

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

  private static Fields convertToFields(FieldsPredicate predicate) {
    Map<String, Fields> children = new HashMap<>();
    for (Map.Entry<String, FieldsPredicate> entry : predicate.getChildren().entrySet()) {
      children.put(entry.getKey(), convertToFields(entry.getValue()));
    }

    return new Fields(
        predicate.isIncludeAll(),
        predicate.getIncludes(),
        predicate.getExcludes(),
        children,
        Map.of() // TODO: add transformations when parsing is implemented
        );
  }
}
