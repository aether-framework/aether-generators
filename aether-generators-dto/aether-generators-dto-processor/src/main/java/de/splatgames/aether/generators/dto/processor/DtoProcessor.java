/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.generators.dto.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.splatgames.aether.generators.dto.processor.struct.DtoField;
import de.splatgames.aether.generators.dto.processor.struct.DtoGroup;
import de.splatgames.aether.generators.dto.processor.struct.SerialUid;
import de.splatgames.aether.generators.dto.processor.struct.SourceKey;
import de.splatgames.aether.generators.dto.processor.utils.GenUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor that generates classic DTO types from fields annotated with
 * {@code de.splatgames.aether.generators.dto.annotations.Dto}.
 *
 * <h2>Overview</h2>
 * <p>
 * For each declaring class, the processor:
 * </p>
 * <ol>
 *   <li>Collects all {@code @Dto}-annotated fields.</li>
 *   <li>Groups them by the annotation's {@code value()} (alias) into one or more {@link DtoGroup}s.</li>
 *   <li>Sorts each group by {@code order} (ascending) with a deterministic tie-break on declaration index.</li>
 *   <li>Emits one DTO class per group into {@code <entityPackage>.dto}, with:
 *     <ul>
 *       <li>private fields mirroring names and types (and copied annotations except {@code @Dto}),</li>
 *       <li>a no-args constructor (initializing {@code List} fields to new {@code ArrayList<>()}),</li>
 *       <li>a full-args constructor ({@code List} parameters are defensively copied to {@code ArrayList} or {@code LinkedList}),</li>
 *       <li>getters/setters ({@code isX()} for primitive booleans, else {@code getX()}),</li>
 *       <li>{@code equals}, {@code hashCode}, and {@code toString},</li>
 *       <li>{@code implements Serializable} with a deterministic {@code serialVersionUID} computed by {@link SerialUid}.</li>
 *     </ul>
 *   </li>
 *   <li>Appends a trailing block comment to the generated file containing suggested
 *       {@code change(...)} and {@code build...()} methods for the original entity.</li>
 * </ol>
 *
 * <h2>Determinism</h2>
 * <p>
 * Output is stable across builds. Field order is defined by {@code order}, then by declaration index
 * (see {@link GenUtils#declarationIndexOf(TypeElement, VariableElement)}). The {@code serialVersionUID}
 * is a SHA-256–based digest over package, DTO name, and each field's name + erased type
 * (see {@link SerialUid}).
 * </p>
 *
 * <h2>Error handling</h2>
 * <p>
 * The processor fails fast on unexpected exceptions and reports them via {@link Messager} with
 * {@link Diagnostic.Kind#ERROR}, preventing partial/ambiguous output. Conflicting class-level markers
 * are reported by {@link DtoGroup#mergeClassAnnotation(String)}.
 * </p>
 *
 * <h2>Generated source location</h2>
 * <p>
 * DTO classes are placed in the same module output as annotation processing sources and written through
 * {@link Filer#createSourceFile(CharSequence, Element...)} with the source type as the originating element.
 * The file includes a trailing comment block with recommended entity methods.
 * </p>
 *
 * @author Erik Pförtner
 * @implNote This processor is stateful only with respect to the current {@link ProcessingEnvironment}; all reusable,
 * stateless helpers live in {@link GenUtils} and the {@code struct} package. The processor returns
 * {@code true} from {@link #process(Set, RoundEnvironment)} to declare that it fully handles the
 * {@code @Dto} annotation.
 * @see de.splatgames.aether.generators.dto.annotations.Dto
 * @see DtoGroup
 * @see DtoField
 * @see SerialUid
 * @see GenUtils
 * @see javax.annotation.processing.Processor
 * @since 1.1.0
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes({
        "de.splatgames.aether.generators.dto.annotations.Dto"
})
public final class DtoProcessor extends AbstractProcessor {

    static final String DTO_ANNOTATION_FQN = "de.splatgames.aether.generators.dto.annotations.Dto";

    private Messager messager;
    private Filer filer;
    private Elements elements;
    private Types types;

    /**
     * Initializes the processor and captures the processing utilities.
     *
     * @param env the processing environment (never {@code null})
     */
    @Override
    public synchronized void init(@NotNull final ProcessingEnvironment env) {
        super.init(env);
        this.messager = env.getMessager();
        this.filer = env.getFiler();
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
    }

    /**
     * Processes {@code @Dto}-annotated fields, groups them by source type, and triggers code generation.
     *
     * @param annotations the set of annotation types requested in this round
     * @param roundEnv    the current round environment
     * @return {@code true} to indicate that this processor fully handles {@code @Dto}
     */
    @Override
    public boolean process(@NotNull final Set<? extends TypeElement> annotations, @NotNull final RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        TypeElement dtoAnn = this.elements.getTypeElement(DTO_ANNOTATION_FQN);
        if (dtoAnn == null) {
            return false;
        }

        // Collect all annotated fields
        Map<SourceKey, List<VariableElement>> bySource = new LinkedHashMap<>();
        for (Element e : roundEnv.getElementsAnnotatedWith(dtoAnn)) {
            if (!(e instanceof VariableElement field)) {
                continue;
            }
            TypeElement source = (TypeElement) field.getEnclosingElement();
            SourceKey key = new SourceKey(source);
            bySource.computeIfAbsent(key, k -> new ArrayList<>()).add(field);
        }

        for (Map.Entry<SourceKey, List<VariableElement>> entry : bySource.entrySet()) {
            try {
                emitForSource(entry.getKey().getSource(), entry.getValue());
            } catch (Exception ex) {
                this.messager.printMessage(Diagnostic.Kind.ERROR,
                        "DTO generation failed: " + ex.getMessage(), entry.getKey().getSource());
            }
        }
        return true;
    }

    /**
     * Scans one declaring source type, builds {@link DtoGroup} instances, and emits DTOs.
     *
     * @param source the declaring type
     * @param fields the annotated fields owned by {@code source}
     * @throws IOException if writing the generated Java sources fails
     */
    private void emitForSource(@NotNull final TypeElement source, @NotNull final List<VariableElement> fields) throws IOException {
        String sourcePkg = this.elements.getPackageOf(source).getQualifiedName().toString();
        String sourceSimple = source.getSimpleName().toString();
        String targetPkg = sourcePkg + ".dto";
        String sourceFqn = source.getQualifiedName().toString();

        Map<String, DtoGroup> groups = new LinkedHashMap<>();
        for (VariableElement field : fields) {
            AnnotationMirror dto = GenUtils.getAnnotation(field, DTO_ANNOTATION_FQN);
            if (dto == null) {
                continue;
            }
            String alias = GenUtils.getStringValue(dto, "value");

            int order = GenUtils.getIntValue(dto, "order", Integer.MAX_VALUE);
            String classAnno = GenUtils.getStringValue(dto, "classAnnotation");

            final String finalAlias = alias;
            DtoGroup g = groups.computeIfAbsent(alias, a -> new DtoGroup(
                    finalAlias,
                    finalAlias.isEmpty() ? sourceSimple + "Dto" : finalAlias + "Dto",
                    targetPkg,
                    sourceSimple,
                    sourceFqn
            ));
            if (!classAnno.isBlank()) {
                g.mergeClassAnnotation(classAnno);
            }

            boolean isBoolPrimitive = field.asType().getKind() == TypeKind.BOOLEAN;
            List<AnnotationSpec> mirroredAnnos = GenUtils.mirrorFieldAnnotations(field, DTO_ANNOTATION_FQN);
            int declIndex = GenUtils.declarationIndexOf(source, field);
            g.getFields().add(new DtoField(
                    field.getSimpleName().toString(),
                    field.asType(),
                    order,
                    declIndex,
                    isBoolPrimitive,
                    mirroredAnnos
            ));
        }

        for (DtoGroup g : groups.values()) {
            g.sort();
            emitDto(source, g);
        }
    }

    /**
     * Emits a single DTO class corresponding to the given {@link DtoGroup}.
     *
     * @param source the originating type (used for {@link Filer#createSourceFile(CharSequence, Element...)})
     * @param g      the DTO group to emit
     * @throws IOException if file creation or writing fails
     */
    private void emitDto(@NotNull final TypeElement source, @NotNull final DtoGroup g) throws IOException {
        ClassName dtoType = ClassName.get(g.getTargetPackage(), g.getDtoSimpleName());

        // Fields
        List<FieldSpec> dtoFields = new ArrayList<>();
        for (DtoField f : g.getFields()) {
            FieldSpec.Builder fb = FieldSpec.builder(TypeName.get(f.getType()), f.getName(), Modifier.PRIVATE)
                    .addJavadoc("{@link $L#$L}.\n", g.getSourceFqn(), f.getName());
            f.getAnnotations().forEach(fb::addAnnotation);
            dtoFields.add(fb.build());
        }

        // serialVersionUID (deterministic)
        long suid = SerialUid.compute(this.types, g);
        FieldSpec serial = FieldSpec.builder(TypeName.LONG, "serialVersionUID",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("The Version UID for java Serializability.\n")
                .initializer("$LL", suid)
                .build();

        // No-args ctor
        MethodSpec.Builder noArgs = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addComment("default-constructor.")
                .addStatement("super()");
        for (DtoField f : g.getFields()) {
            if (GenUtils.isListType(this.elements, this.types, f.getType())) {
                noArgs.addStatement("this.$L = new $T<>()", f.getName(), ArrayList.class);
            }
        }
        MethodSpec noArgsCtor = noArgs.build();

        // All-args ctor
        MethodSpec.Builder allArgs = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addComment("constructor.");
        for (DtoField f : g.getFields()) {
            allArgs.addParameter(ParameterSpec.builder(TypeName.get(f.getType()), f.getName(), Modifier.FINAL).build());
        }
        for (DtoField f : g.getFields()) {
            if (GenUtils.isListType(this.elements, this.types, f.getType())) {
                allArgs.beginControlFlow("if ($L == null)", f.getName())
                        .addStatement("this.$L = null", f.getName())
                        .nextControlFlow("else if ($L instanceof $T)", f.getName(), LinkedList.class)
                        .addStatement("this.$L = new $T<>($L)", f.getName(), LinkedList.class, f.getName())
                        .nextControlFlow("else")
                        .addStatement("this.$L = new $T<>($L)", f.getName(), ArrayList.class, f.getName())
                        .endControlFlow();
            } else {
                allArgs.addStatement("this.$L = $L", f.getName(), f.getName());
            }
        }
        MethodSpec allArgsCtor = allArgs.build();

        // Getters/Setters
        List<MethodSpec> accessors = new ArrayList<>();
        for (DtoField f : g.getFields()) {
            String cap = GenUtils.capitalize(f.getName());
            // getter
            MethodSpec getter = MethodSpec.methodBuilder((f.isBooleanPrimitive() ? "is" : "get") + cap)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.get(f.getType()))
                    .addStatement("return this.$L", f.getName())
                    .build();
            accessors.add(getter);
            // setter (void)
            MethodSpec setter = MethodSpec.methodBuilder("set" + cap)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addParameter(TypeName.get(f.getType()), "value")
                    .addStatement("this.$L = value", f.getName())
                    .build();
            accessors.add(setter);
        }

        // equals
        MethodSpec equals = buildEquals(dtoType, g);
        // hashCode
        MethodSpec hashCode = buildHashCode(g);
        // toString
        MethodSpec toString = buildToString(g);

        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(g.getDtoSimpleName())
                .addJavadoc("this is generated dto class, do not edit manually.\n")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(Serializable.class)
                .addField(serial)
                .addFields(dtoFields)
                .addMethod(noArgsCtor)
                .addMethod(allArgsCtor)
                .addMethods(accessors)
                .addMethod(hashCode)
                .addMethod(equals)
                .addMethod(toString);

        if (g.getClassAnnotationFqn() != null && !g.getClassAnnotationFqn().isBlank()) {
            typeBuilder.addAnnotation(AnnotationSpec.builder(ClassName.bestGuess(g.getClassAnnotationFqn())).build());
        }

        TypeSpec type = typeBuilder
                .addAnnotation(AnnotationSpec.builder(javax.annotation.processing.Generated.class)
                        .addMember("value", "$S", "Aether Generators DTO")
                        .build())
                .build();

        JavaFile javaFile = JavaFile.builder(g.getTargetPackage(), type)
                .skipJavaLangImports(true)
                .indent("    ")
                .build();

        // Prepare trailing change/build comment
        String trailing = buildChangeBuildComment(g);

        // Compose fully-qualified name and write with originating element
        String fqn = g.getTargetPackage() + "." + g.getDtoSimpleName();
        JavaFileObject jfo = this.filer.createSourceFile(fqn, source);
        try (Writer w = jfo.openWriter()) {
            javaFile.writeTo(w);      // emit class
            w.write(System.lineSeparator());
            w.write(System.lineSeparator());
            w.write(trailing);
            w.write(System.lineSeparator());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    /**
     * Builds the {@code toString()} method for the generated DTO class.
     *
     * @param g the DTO group
     * @return a {@link MethodSpec} for {@code toString()}
     */
    private MethodSpec buildToString(@NotNull final DtoGroup g) {
        CodeBlock.Builder expr = CodeBlock.builder();
        expr.add("$S", g.getDtoSimpleName() + " [");
        for (int i = 0; i < g.getFields().size(); i++) {
            DtoField f = g.getFields().get(i);
            String prefix = (i == 0) ? "" : ", ";
            expr.add(" + $S + this.$L", prefix + f.getName() + "=", f.getName());
        }
        expr.add(" + $S", "]");

        return MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $L", expr.build())
                .build();
    }

    /**
     * Builds the {@code hashCode()} method for the generated DTO class.
     *
     * @param g the DTO group
     * @return a {@link MethodSpec} for {@code hashCode()}
     */
    private MethodSpec buildHashCode(@NotNull final DtoGroup g) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.addStatement("final int prime = 31");
        b.addStatement("int result = 1");
        for (DtoField f : g.getFields()) {
            TypeKind k = f.getType().getKind();
            if (k.isPrimitive()) {
                switch (k) {
                    case BOOLEAN ->
                            b.addStatement("result = prime * result + Boolean.valueOf($L).hashCode()", f.getName());
                    case BYTE, SHORT, INT, CHAR -> b.addStatement("result = prime * result + $L", f.getName());
                    case LONG ->
                            b.addStatement("result = prime * result + (int) ($L ^ ($L >>> 32))", f.getName(), f.getName());
                    case FLOAT -> b.addStatement("result = prime * result + $T.hashCode($L)", Float.class, f.getName());
                    case DOUBLE ->
                            b.addStatement("result = prime * result + $T.hashCode($L)", Double.class, f.getName());
                    default -> b.addStatement("result = prime * result + ($L)", f.getName());
                }
            } else {
                b.addStatement("result = prime * result + (($1L == null) ? 0 : $1L.hashCode())", f.getName());
            }
        }
        b.addStatement("return result");
        return MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addCode(b.build())
                .build();
    }

    /**
     * Builds the {@code equals(Object)} method for the generated DTO class.
     *
     * @param dtoType the DTO {@link ClassName}
     * @param g       the DTO group
     * @return a {@link MethodSpec} for {@code equals(Object)}
     */
    private MethodSpec buildEquals(@NotNull final ClassName dtoType, @NotNull final DtoGroup g) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.addStatement("if (this == obj) return true");
        b.addStatement("if (obj == null) return false");
        b.addStatement("if (getClass() != obj.getClass()) return false");
        b.addStatement("$T other = ($T) obj", dtoType, dtoType);
        for (DtoField f : g.getFields()) {
            TypeKind k = f.getType().getKind();
            if (k.isPrimitive()) {
                b.addStatement("if (this.$L != other.$L) return false", f.getName(), f.getName());
            } else {
                b.beginControlFlow("if (this.$L == null)", f.getName())
                        .beginControlFlow("if (other.$L != null)", f.getName())
                        .addStatement("return false")
                        .endControlFlow()
                        .nextControlFlow("else if (!this.$L.equals(other.$L))", f.getName(), f.getName())
                        .addStatement("return false")
                        .endControlFlow();
            }
        }
        b.addStatement("return true");
        return MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(TypeName.OBJECT, "obj")
                .addCode(b.build())
                .build();
    }

    /**
     * Builds the trailing instructional block comment that is appended to the generated DTO file.
     * <p>
     * The comment contains a {@code change(...)} method and a {@code buildXxxDto()} method that can be
     * copied into the original entity as a convenience for mutation and DTO construction.
     * </p>
     *
     * @param g the DTO group
     * @return the block comment text
     */
    private String buildChangeBuildComment(DtoGroup g) {
        String dtoName = g.getDtoSimpleName();
        StringBuilder sb = new StringBuilder();
        sb.append("/* Change- and Build-methods, copy them to the source entity: \n\n");

        // change(..)
        sb.append("public void change(final ").append(dtoName).append(" dto) {\n")
                .append("    java.util.Objects.requireNonNull(dto, \"").append(GenUtils.decap(dtoName)).append(" must not be null\");\n\n");
        for (DtoField f : g.getFields()) {
            String getter = (f.isBooleanPrimitive() ? "is" : "get") + GenUtils.capitalize(f.getName()) + "()";
            sb.append("    this.").append(f.getName()).append(" = dto.").append(getter).append(";\n");
        }
        sb.append("}\n\n");

        // build..()
        sb.append("public ").append(dtoName).append(" build").append(dtoName).append("() {\n")
                .append("    return new ").append(dtoName).append("(");
        for (int i = 0; i < g.getFields().size(); i++) {
            DtoField f = g.getFields().get(i);
            sb.append("this.").append(f.getName());
            if (i < g.getFields().size() - 1) {
                sb.append(",\n        ");
            }
        }
        sb.append(");\n}\n\n");

        sb.append("*/");
        return sb.toString();
    }
}
