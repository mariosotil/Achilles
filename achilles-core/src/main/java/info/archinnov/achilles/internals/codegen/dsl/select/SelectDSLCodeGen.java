/*
 * Copyright (C) 2012-2016 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.internals.codegen.dsl.select;

import static info.archinnov.achilles.internals.codegen.dsl.AbstractDSLCodeGen.ReturnType.NEW;
import static info.archinnov.achilles.internals.codegen.dsl.AbstractDSLCodeGen.ReturnType.THIS;
import static info.archinnov.achilles.internals.parser.TypeUtils.*;

import java.util.StringJoiner;
import javax.lang.model.element.Modifier;

import org.apache.commons.lang3.ArrayUtils;

import com.squareup.javapoet.*;

import info.archinnov.achilles.internals.codegen.dsl.AbstractDSLCodeGen;
import info.archinnov.achilles.internals.codegen.meta.EntityMetaCodeGen.EntityMetaSignature;
import info.archinnov.achilles.internals.metamodel.columns.ColumnType;
import info.archinnov.achilles.internals.metamodel.columns.ComputedColumnInfo;
import info.archinnov.achilles.internals.metamodel.columns.PartitionKeyInfo;
import info.archinnov.achilles.internals.parser.FieldParser.TypeParsingResult;
import info.archinnov.achilles.type.tuples.Tuple2;

public class SelectDSLCodeGen extends AbstractDSLCodeGen {


    public static TypeSpec buildSelectClass(EntityMetaSignature signature) {

        final String firstPartitionKey = signature.parsingResults
                .stream()
                .filter(x -> x.context.columnType == ColumnType.PARTITION)
                .map(x -> Tuple2.of(x.context.fieldName, (PartitionKeyInfo) x.context.columnInfo))
                .sorted(TUPLE2_PARTITION_KEY_SORTER)
                .map(Tuple2::_1)
                .findFirst()
                .get();

        TypeName selectFromTypeName = ClassName.get(DSL_PACKAGE, signature.selectFromReturnType());
        TypeName selectColumnsTypeName = ClassName.get(DSL_PACKAGE, signature.selectColumnsReturnType());

        final TypeSpec.Builder selectClassBuilder = TypeSpec.classBuilder(signature.selectClassName())
                .superclass(ABSTRACT_SELECT)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(buildSelectConstructor(signature))
                .addField(buildExactEntityMetaField(signature))
                .addField(buildEntityClassField(signature))
                .addType(buildSelectColumns(signature))
                .addType(buildSelectFrom(signature, firstPartitionKey));

        signature.parsingResults
                .stream()
                .filter(x -> x.context.columnType != ColumnType.COMPUTED)
                .forEach(x -> selectClassBuilder.addMethod(buildSelectColumnMethod(selectColumnsTypeName, x, "select", NEW)));

        signature.parsingResults
                .stream()
                .filter(x -> x.context.columnType == ColumnType.COMPUTED)
                .forEach(x -> selectClassBuilder.addMethod(buildSelectComputedColumnMethod(selectColumnsTypeName, x, "select", NEW)));


        selectClassBuilder.addMethod(buildAllColumns(selectFromTypeName, SELECT_WHERE, "select"));
        selectClassBuilder.addMethod(buildAllColumnsWithSchemaProvider(selectFromTypeName, SELECT_WHERE, "select"));


        SelectWhereDSLCodeGen.buildWhereClasses(signature).forEach(selectClassBuilder::addType);

        return selectClassBuilder.build();
    }

    private static MethodSpec buildSelectConstructor(EntityMetaSignature signature) {
        String metaClassName = signature.className + META_SUFFIX;
        TypeName metaClassType = ClassName.get(ENTITY_META_PACKAGE, metaClassName);

        final MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(RUNTIME_ENGINE, "rte")
                .addParameter(metaClassType, "meta")
                .addStatement("super(rte)")
                .addStatement("this.meta = meta");

        return builder.build();
    }

    private static TypeSpec buildSelectColumns(EntityMetaSignature signature) {

        TypeName selectColumnsTypeName = ClassName.get(DSL_PACKAGE, signature.selectColumnsReturnType());

        TypeName selectFromTypeName = ClassName.get(DSL_PACKAGE, signature.selectFromReturnType());

        final TypeSpec.Builder builder = TypeSpec.classBuilder(signature.className + SELECT_COLUMNS_DSL_SUFFIX)
                .superclass(ABSTRACT_SELECT_COLUMNS)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(SELECT_COLUMNS, "selection")
                        .addStatement("super(selection)")
                        .build());

        signature.parsingResults
                .stream()
                .filter(x -> x.context.columnType != ColumnType.COMPUTED)
                .forEach(x -> builder.addMethod(buildSelectColumnMethod(selectColumnsTypeName, x, "selection", THIS)));

        signature.parsingResults
                .stream()
                .filter(x -> x.context.columnType == ColumnType.COMPUTED)
                .forEach(x -> builder.addMethod(buildSelectComputedColumnMethod(selectColumnsTypeName, x, "selection", THIS)));

        builder.addMethod(buildFrom(selectFromTypeName, SELECT_WHERE, "selection"));
        builder.addMethod(buildFromWithSchemaProvider(selectFromTypeName, SELECT_WHERE, "selection"));

        return builder.build();
    }

    private static TypeSpec buildSelectFrom(EntityMetaSignature signature, String firstPartitionKey) {
        TypeName selectWhereTypeName = ClassName.get(DSL_PACKAGE, signature.selectWhereReturnType(firstPartitionKey));

        TypeName selectEndTypeName = ClassName.get(DSL_PACKAGE, signature.selectEndReturnType());

        return TypeSpec.classBuilder(signature.className + SELECT_FROM_DSL_SUFFIX)
                .superclass(ABSTRACT_SELECT_FROM)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(SELECT_WHERE, "where")
                        .addStatement("super(where)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("where")
                        .addJavadoc("Generate a SELECT ... FROM ... <strong>WHERE</strong> ...")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addStatement("return new $T(where)", selectWhereTypeName)
                        .returns(selectWhereTypeName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("without_WHERE_Clause")
                        .addJavadoc("Generate a SELECT statement <strong>without</strong> the <strong>WHERE</strong> clause")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addStatement("return new $T(where)", selectEndTypeName)
                        .returns(selectEndTypeName)
                        .build())
                .build();
    }

    private static MethodSpec buildSelectColumnMethod(TypeName newTypeName, TypeParsingResult parsingResult, String fieldName, ReturnType returnType) {

        final MethodSpec.Builder builder = MethodSpec.methodBuilder(parsingResult.context.fieldName)
                .addJavadoc("Generate a SELECT ... <strong>$L</strong> ...", parsingResult.context.cqlColumn)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addStatement("$L.column($S)", fieldName, parsingResult.context.cqlColumn)
                .returns(newTypeName);

        if (returnType == NEW) {
            return builder.addStatement("return new $T(select)", newTypeName).build();
        } else {
            return builder.addStatement("return this").build();
        }
    }


    private static MethodSpec buildSelectComputedColumnMethod(TypeName newTypeName, TypeParsingResult parsingResult, String fieldName, ReturnType returnType) {

        final ComputedColumnInfo columnInfo = (ComputedColumnInfo) parsingResult.context.columnInfo;
        StringJoiner joiner = new StringJoiner(",", fieldName + ".fcall($S,", ").as($S)");
        columnInfo.functionArgs.forEach(x -> joiner.add("$L"));

        final Object[] functionName = new Object[]{columnInfo.functionName};
        final Object[] functionArgs = columnInfo
                .functionArgs
                .stream()
                .map(arg -> CodeBlock.builder().add("$T.column($S)", QUERY_BUILDER, arg).build())
                .toArray();
        final Object[] alias = new Object[]{columnInfo.alias};

        final Object[] varargs = ArrayUtils.addAll(functionName, ArrayUtils.addAll(functionArgs, alias));
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(parsingResult.context.fieldName)
                .addJavadoc("Generate a SELECT ... <strong>$L($L) AS $L</strong> ...", varargs)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addStatement(joiner.toString(), varargs)
                .returns(newTypeName);

        if (returnType == NEW) {
            return builder.addStatement("return new $T(select)", newTypeName).build();
        } else {
            return builder.addStatement("return this").build();
        }
    }
}
