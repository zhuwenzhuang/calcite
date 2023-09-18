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
import org.apache.calcite.linq4j.function.Function1;
import org.apache.calcite.runtime.Hook;
import org.apache.calcite.runtime.Utilities;
import org.apache.calcite.util.Pair;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.Scanner;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Result of compiling code generated from a {@link RexNode} expression.
 */
public class RexExecutableImpl implements RexExecutable {
  private static final String GENERATED_CLASS_NAME = "Reducer";

  private final Function1<DataContext, @Nullable Object @Nullable []> compiledFunction;
  private final String code;
  private @Nullable DataContext dataContext;

  public RexExecutableImpl(String code, Object reason) {
    this.code = code;
    this.compiledFunction = compile(code, reason);
  }

  private static Function1<DataContext, @Nullable Object @Nullable []> compile(String code,
      Object reason) {
    try {
      final ClassBodyEvaluator cbe = new ClassBodyEvaluator();
      cbe.setClassName(GENERATED_CLASS_NAME);
      cbe.setExtendedClass(Utilities.class);
      cbe.setImplementedInterfaces(new Class[] {Function1.class, Serializable.class});
      cbe.setParentClassLoader(RexExecutableImpl.class.getClassLoader());
      cbe.cook(new Scanner(null, new StringReader(code)));
      Class c = cbe.getClazz();
      //noinspection unchecked
      final Constructor<Function1<DataContext, @Nullable Object @Nullable []>> constructor =
          c.getConstructor();
      return constructor.newInstance();
    } catch (CompileException | IOException | InstantiationException
        | IllegalAccessException | InvocationTargetException
        | NoSuchMethodException e) {
      throw new RuntimeException("While compiling " + reason, e);
    }
  }

  @Override public void setDataContext(DataContext dataContext) {
    this.dataContext = dataContext;
  }

  @Override public void reduce(RexBuilder rexBuilder, List<RexNode> constExps,
      List<RexNode> reducedValues) {
    @Nullable Object[] values;
    try {
      values = execute();
      if (values == null) {
        reducedValues.addAll(constExps);
        values = new Object[constExps.size()];
      } else {
        assert values.length == constExps.size();
        final List<@Nullable Object> valueList = Arrays.asList(values);
        for (Pair<RexNode, @Nullable Object> value : Pair.zip(constExps, valueList)) {
          reducedValues.add(
              rexBuilder.makeLiteral(value.right, value.left.getType(), true));
        }
      }
    } catch (RuntimeException e) {
      // One or more of the expressions failed.
      // Don't reduce any of the expressions.
      reducedValues.clear();
      reducedValues.addAll(constExps);
      values = new Object[constExps.size()];
    }
    Hook.EXPRESSION_REDUCER.run(Pair.of(code, values));
  }

  public Function1<DataContext, @Nullable Object @Nullable []> getFunction() {
    return compiledFunction;
  }

  @Override public @Nullable Object @Nullable [] execute() {
    return compiledFunction.apply(requireNonNull(dataContext, "dataContext"));
  }

  public String getSource() {
    return code;
  }
}