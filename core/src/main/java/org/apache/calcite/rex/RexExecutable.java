/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.rex;

import org.apache.calcite.DataContext;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Executable interface for a {@link RexNode} expression.
 */
public interface RexExecutable {

  /** Reduces expressions, and writes their results into {@code reducedValues}.
   *
   * <p>If an expression cannot be reduced, writes the original expression.
   * For example, {@code CAST('abc' AS INTEGER)} gives an error when executed, so the executor
   * ignores the error and writes the original expression.
   *
   * @param rexBuilder Rex builder
   * @param constExps Expressions to be reduced
   * @param reducedValues List to which reduced expressions are appended
   */
  void reduce(RexBuilder rexBuilder, List<RexNode> constExps, List<RexNode> reducedValues);

  /** Set input data for the {@link RexNode} expression.
   *
   * @param dataContext input data
   */
  void setDataContext(DataContext dataContext);

  /** Execute the {@link RexNode} expression.
   *
   * @return execute result
   */
  @Nullable Object @Nullable [] execute();

}
