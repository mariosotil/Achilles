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

package info.archinnov.achilles.internals.codegen.meta;

import static info.archinnov.achilles.internals.parser.TypeUtils.*;
import static info.archinnov.achilles.internals.strategy.naming.InternalNamingStrategy.inferNamingStrategy;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.UDTValue;
import com.squareup.javapoet.*;

import info.archinnov.achilles.annotations.Strategy;
import info.archinnov.achilles.annotations.UDT;
import info.archinnov.achilles.internals.apt.AptUtils;
import info.archinnov.achilles.internals.parser.FieldParser.FieldMetaSignature;
import info.archinnov.achilles.internals.parser.context.EntityParsingContext;

public class UDTMetaCodeGen extends AbstractBeanMetaCodeGen {

    private final AptUtils aptUtils;

    public UDTMetaCodeGen(AptUtils aptUtils) {
        this.aptUtils = aptUtils;
    }

    public TypeSpec buildUDTClassProperty(TypeElement elm, EntityParsingContext context, List<FieldMetaSignature> parsingResults) {

        final TypeName rawBeanType = TypeName.get(aptUtils.erasure(elm));

        final Optional<Strategy> strategy = aptUtils.getAnnotationOnClass(elm, Strategy.class);

        final String className = elm.getSimpleName() + META_SUFFIX;
        TypeName classType = ClassName.get(UDT_META_PACKAGE, className);
        final TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(genericType(ABSTRACT_UDT_CLASS_PROPERTY, rawBeanType))
                .addMethod(buildGetStaticKeyspace(elm))
                .addMethod(buildGetStaticUDTName(elm))
                .addMethod(buildGetStaticNamingStrategy(strategy))
                .addMethod(buildGetUdtName(elm, context))
                .addMethod(buildGetUdtClass(rawBeanType))
                .addMethod(buildComponentsProperty(rawBeanType, parsingResults))
                .addMethod(buildCreateUDTFromBeanT(rawBeanType, parsingResults))
                .addMethod(buildCreateBeanFromUDT(rawBeanType, parsingResults));

        for (FieldMetaSignature x : parsingResults) {
            builder.addField(x.buildPropertyAsField());
        }

        /**
         * REALLY IMPORTANT, generate the INSTANCE field
         * right AFTER ALL the meta data
         */
        builder.addField(build_INSTANCE_Field(classType));

        return builder.build();
    }

    private FieldSpec build_INSTANCE_Field(TypeName classType) {
        return FieldSpec
                .builder(classType, "INSTANCE", Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .initializer("new $T()", classType)
                .build();
    }

    private MethodSpec buildGetStaticUDTName(TypeElement elm) {
        final UDT udt = aptUtils.getAnnotationOnClass(elm, UDT.class).get();
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("getStaticUdtName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(genericType(OPTIONAL, STRING));

        final Optional<String> udtName = Optional.ofNullable(StringUtils.isBlank(udt.name()) ? null : udt.name());

        if (udtName.isPresent()) {
            return builder.addStatement("return $T.of($S)", OPTIONAL, udtName.get()).build();
        } else {
            return emptyOption(builder);
        }
    }

    private MethodSpec buildGetUdtName(TypeElement elm, EntityParsingContext context) {
        final UDT udt = aptUtils.getAnnotationOnClass(elm, UDT.class).get();
        final Optional<Strategy> strategy = aptUtils.getAnnotationOnClass(elm, Strategy.class);
        final String udtName = isBlank(udt.name())
                ? inferNamingStrategy(strategy, context.namingStrategy).apply(elm.getSimpleName().toString())
                : udt.name();

        return MethodSpec.methodBuilder("getUdtName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(String.class)
                .addStatement("return $S", udtName)
                .build();

    }

    private MethodSpec buildGetStaticKeyspace(TypeElement elm) {
        final UDT udt = aptUtils.getAnnotationOnClass(elm, UDT.class).get();
        final CodeBlock keyspaceCodeBlock = Optional
                .ofNullable(isBlank(udt.keyspace()) ? null : udt.keyspace())
                .map(x -> CodeBlock.builder().addStatement("return $T.of($S)", OPTIONAL, x).build())
                .orElse(CodeBlock.builder().addStatement("return $T.empty()", OPTIONAL).build());

        return MethodSpec.methodBuilder("getStaticKeyspace")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(genericType(OPTIONAL, STRING))
                .addCode(keyspaceCodeBlock)
                .build();

    }

    private MethodSpec buildGetUdtClass(TypeName rawBeanType) {
        return MethodSpec.methodBuilder("getUdtClass")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(classTypeOf(rawBeanType))
                .addStatement("return $T.class", rawBeanType)
                .build();

    }

    private MethodSpec buildComponentsProperty(TypeName rawBeanType, List<FieldMetaSignature> parsingResults) {
        TypeName returnType = genericType(LIST, genericType(ABSTRACT_PROPERTY, rawBeanType, WILDCARD, WILDCARD));
        final StringJoiner allFields = new StringJoiner(", ");
        parsingResults
                .stream()
                .map(x -> x.context.fieldName)
                .forEach(x -> allFields.add(x));

        StringBuilder fieldList = new StringBuilder();
        fieldList.append("return $T.asList(").append(allFields.toString()).append(")");

        return MethodSpec.methodBuilder("getComponentsProperty")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addStatement(fieldList.toString(), ARRAYS)
                .build();

    }

    private MethodSpec buildCreateUDTFromBeanT(TypeName rawBeanType, List<FieldMetaSignature> parsingResults) {

        final MethodSpec.Builder builder = MethodSpec.methodBuilder("createUDTFromBean")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(rawBeanType, "instance")
                .returns(JAVA_DRIVER_UDT_VALUE_TYPE)
                .addStatement("final $T udtValue = userType.newValue()", JAVA_DRIVER_UDT_VALUE_TYPE);

        for (FieldMetaSignature x : parsingResults) {
            builder.addStatement("$L.encodeFieldToUdt(instance, udtValue)", x.context.fieldName);
        }

        builder.addStatement("return udtValue");

        return builder.build();
    }

    private MethodSpec buildCreateBeanFromUDT(TypeName rawBeanType, List<FieldMetaSignature> parsingResults) {
        final ClassName udtType = ClassName.get(UDTValue.class);

        final MethodSpec.Builder builder = MethodSpec.methodBuilder("createBeanFromUDT")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(udtType, "udtValue")
                .returns(rawBeanType)
                .addStatement("final $T instance = udtFactory.newInstance(udtClass)", rawBeanType);

        for (FieldMetaSignature x : parsingResults) {
            builder.addStatement("$L.decodeField(udtValue, instance)", x.context.fieldName);
        }

        builder.addStatement("return instance");

        return builder.build();
    }
}
