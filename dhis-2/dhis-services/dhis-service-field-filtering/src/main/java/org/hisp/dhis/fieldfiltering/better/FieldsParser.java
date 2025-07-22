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

import java.util.Stack;

public class FieldsParser {

  public static FieldsPredicate parse(String input) {
    // TODO should we limit the depth? I think the current FieldFilterParser can cause a
    // StackOverFlow show that in a test + note stackoverflow possibility in Jackson due to it using
    // recursion which we could limit
    // TODO error handling: white space in a field name, special characters like * or : in a field
    // name, things like ', ,   ' or 'group[ ]' or a block without a name '[]'
    FieldsPredicate root = new FieldsPredicate();
    Stack<FieldsPredicate> stack = new Stack<>();
    stack.push(root);

    int i = 0;
    int fieldStart = i;
    boolean inField = false;
    boolean isExclusion = false;
    while (i < input.length()) {
      if ((input.charAt(i) == ',' || Character.isWhitespace(input.charAt(i)))) {
        if (inField) {
          if (isExclusion) {
            stack.peek().exclude(input.substring(fieldStart, i));
          } else {
            stack.peek().include(input.substring(fieldStart, i));
          }

          inField = false;
          isExclusion = false;
        }
      } else if (input.charAt(i) == '[' || input.charAt(i) == '(') {
        // TODO fail if we are not inField? old parser allows this but the result makes no sense
        if (inField) {
          String parent = input.substring(fieldStart, i);
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
            stack.peek().getChildren().put(parent, child);
          }

          stack.push(child);
          inField = false;
          isExclusion = false;
        }
      } else if (input.charAt(i) == ']' || input.charAt(i) == ')') {
        if (stack.size() <= 1) {
          throw new IllegalArgumentException("Unbalanced brackets in input: " + input);
        }

        if (inField) {
          if (isExclusion) {
            stack.peek().exclude(input.substring(fieldStart, i));
          } else {
            stack.peek().include(input.substring(fieldStart, i));
          }
        }

        stack.pop();
        inField = false;
        isExclusion = false;
      } else if (input.charAt(i) == '*' && !inField) {
        stack.peek().includeAll();
      } else if (input.charAt(i) == '!' && !inField) {
        inField = true;
        isExclusion = true;
        fieldStart = i + 1; // do not include ! in field name
      } else if (!Character.isWhitespace(input.charAt(i)) && !inField) {
        inField = true;
        isExclusion = false;
        fieldStart = i;
      }
      i++;
    }

    if (inField) {
      if (isExclusion) {
        stack.peek().exclude(input.substring(fieldStart, i));
      } else {
        stack.peek().include(input.substring(fieldStart, i));
      }
    }
    // TODO this is where we could check if stack size is > 1 and err as a bracket/paren
    // "group[name" was not closed

    return root;
  }
}
