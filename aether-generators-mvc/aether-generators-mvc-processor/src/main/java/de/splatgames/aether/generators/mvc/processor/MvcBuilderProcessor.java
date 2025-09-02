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

package de.splatgames.aether.generators.mvc.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import de.splatgames.aether.generators.mvc.annotations.MvcBuilder;
import de.splatgames.aether.generators.mvc.annotations.MvcField;
import de.splatgames.aether.generators.mvc.annotations.MvcIgnore;
import de.splatgames.aether.generators.mvc.processor.struct.FieldModel;
import de.splatgames.aether.generators.mvc.processor.struct.RelKind;
import de.splatgames.aether.generators.mvc.processor.utils.GenHelpers;
import de.splatgames.aether.generators.mvc.processor.utils.ProcessorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Annotation processor that generates fluent test-data builders for domain classes
 * annotated with {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder @MvcBuilder}.
 *
 * <h2>Overview</h2>
 * <p>
 * For each annotated entity class <code>X</code>, this processor generates a final builder
 * class <code>XBuilder</code> (or the name from {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#builderName()})
 * in the same package by default (or in {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#generatedPackage()}).
 * The generated builder extends <code>AbstractBuilder&lt;X, XBuilder&gt;</code> from the runtime module and provides a fluent API:
 * <ul>
 *   <li><code>withX(...)</code> for singular fields</li>
 *   <li><code>addX(...)</code>, <code>addAllX(...)</code>, <code>clearX()</code> for {@code Collection}-like fields</li>
 *   <li><code>withXId(...)</code> for <em>asIdOnly</em> to-one relations</li>
 *   <li><code>create()</code>, <code>createMany(int)</code>, and mode switches (<code>transientMode()</code>, <code>persistent()</code>) inherited from {@code AbstractBuilder}</li>
 * </ul>
 * </p>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#fieldPolicy()} to control which fields are exposed
 *       (all vs. {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder.FieldPolicy#EXPLICIT explicit via
 *       {@link de.splatgames.aether.generators.mvc.annotations.MvcField @MvcField}})</li>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#includeSuper()} to include superclass fields</li>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#autoRelations()} to pre-persist related entities (persistent mode)</li>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder#defaultProvider()} optional defaults provider
 *       (zero-arg methods named after fields; for <em>asIdOnly</em> to-one fields, use <code>&lt;field&gt;Id()</code>)</li>
 *   <li>{@link de.splatgames.aether.generators.mvc.annotations.MvcField @MvcField} and
 *       {@link de.splatgames.aether.generators.mvc.annotations.MvcIgnore @MvcIgnore} for per-field tuning</li>
 * </ul>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>The target entity must declare a <strong>non-private no-arg constructor</strong>.
 *       Missing this constructor yields a compile error from the processor.</li>
 *   <li>API method names are validated to avoid collisions (e.g., multiple fields mapping to the same fluent name).</li>
 * </ul>
 *
 * <h2>Generation semantics</h2>
 * <ul>
 *   <li>For non-collection fields, the builder uses the entity setter if present; otherwise, it falls back to reflection.</li>
 *   <li>Collection fields get <code>with/add/addAll/clear</code> methods and are initialized to sensible defaults
 *       (ArrayList/LinkedHashSet) when needed.</li>
 *   <li>To-one relations marked <em>asIdOnly</em> produce <code>withXId(...)</code>; in persistent mode the adapter may
 *       look up or otherwise manage the relation.</li>
 *   <li>Defaults provider methods are invoked reflectively and are optional per field.</li>
 * </ul>
 *
 * <h2>Diagnostics</h2>
 * <ul>
 *   <li>Hard errors: wrong target kind, missing no-arg constructor, or API name conflicts.</li>
 *   <li>Warnings: defaults provider methods returning {@code void} (likely a misconfiguration).</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>
 * The processor follows the standard annotation processing contract and does not maintain mutable static state.
 * It relies on the processing environment provided by the compiler for each round.
 * </p>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.generators.mvc.annotations.MvcBuilder
 * @see de.splatgames.aether.generators.mvc.annotations.MvcField
 * @see de.splatgames.aether.generators.mvc.annotations.MvcIgnore
 * @since 1.0.0
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("de.splatgames.aether.generators.mvc.annotations.MvcBuilder")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class MvcBuilderProcessor extends AbstractProcessor {
    private static final ClassName ABSTRACT_BUILDER =
            ClassName.get("de.splatgames.aether.generators.mvc.runtime", "AbstractBuilder");
    private static final ClassName ABSTRACT_BUILDER_MODE =
            ClassName.get("de.splatgames.aether.generators.mvc.runtime", "AbstractBuilder", "Mode");
    private static final ClassName PERSIST_ADAPTER =
            ClassName.get("de.splatgames.aether.generators.mvc.runtime", "PersistAdapter");

    private Messager log;
    private Elements elements;
    private Types types;

    // ---------- build / helpers generation ----------

    /**
     * Initializes this processor with the current compilation {@link ProcessingEnvironment}.
     * Called once by the annotation processing framework.
     *
     * @param env the processing environment (never {@code null})
     */
    @Override
    public synchronized void init(@NotNull final ProcessingEnvironment env) {
        super.init(env);
        this.log = env.getMessager();
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
    }

    /**
     * Entry point for an annotation processing round. Finds all elements annotated with
     * {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder} and generates a builder per class.
     *
     * <p>Errors are reported via {@link Messager}; hard failures short-circuit generation for that element.</p>
     *
     * @param annotations the set of annotation types requested in this round (never {@code null})
     * @param roundEnv    the round environment with the elements to process (never {@code null})
     * @return {@code true} to indicate the annotations are fully handled by this processor
     */
    @Override
    public boolean process(@NotNull final Set<? extends TypeElement> annotations,
                           @NotNull final RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(MvcBuilder.class)) {
            if (e.getKind() != ElementKind.CLASS) {
                this.log.printMessage(Diagnostic.Kind.ERROR, "@MvcBuilder is only valid on classes.", e);
                continue;
            }
            try {
                generateBuilder((TypeElement) e);
            } catch (StopProcessing ex) {
                // hard error already reported
            } catch (Exception ex) {
                this.log.printMessage(Diagnostic.Kind.ERROR, "Failed to generate builder: " + ex.getMessage(), e);
            }
        }
        return true;
    }

    /**
     * Generates the builder class for the given entity type.
     *
     * <p>Performs configuration inspection, field scanning, defaults provider validation,
     * conflict detection, and finally writes the Java source via {@link JavaFile}.</p>
     *
     * @param entityType the annotated entity type element (never {@code null})
     * @throws IOException if writing the builder source file fails
     */
    private void generateBuilder(@NotNull final TypeElement entityType) throws IOException {
        requireNoArgCtor(entityType);

        String entityPkg = this.elements.getPackageOf(entityType).getQualifiedName().toString();
        String entitySimple = entityType.getSimpleName().toString();
        ClassName entity = ClassName.get(entityPkg, entitySimple);

        MvcBuilder cfg = entityType.getAnnotation(MvcBuilder.class);

        String outPkg = (cfg != null && !cfg.generatedPackage().isBlank())
                ? cfg.generatedPackage() : entityPkg;

        String builderSimple = (cfg != null && !cfg.builderName().isBlank())
                ? cfg.builderName() : entitySimple + "Builder";

        boolean includeSuper = cfg == null || cfg.includeSuper();
        boolean explicitPolicy = cfg != null && cfg.fieldPolicy() == MvcBuilder.FieldPolicy.EXPLICIT;
        boolean autoRelations = cfg == null || cfg.autoRelations();

        TypeName defaultsType = resolveDefaultsType(cfg); // may be null
        TypeElement defaultsTypeEl = resolveDefaultsTypeElement(cfg);

        ClassName builderType = ClassName.get(outPkg, builderSimple);
        TypeName superType = ParameterizedTypeName.get(
                ABSTRACT_BUILDER,
                entity,
                builderType
        );

        // scan fields and sort deterministically by alias
        List<FieldModel> fields = collectFields(entityType, includeSuper, explicitPolicy)
                .stream()
                .sorted(Comparator.comparing(FieldModel::alias, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        // validate defaults provider signatures (names exist & zero args). Type-compat warnings only.
        if (defaultsTypeEl != null) {
            validateDefaultsProvider(entityType, defaultsTypeEl, fields);
        }

        // detect API method name conflicts
        checkApiConflicts(entityType, builderSimple, fields);

        // override storage
        List<FieldSpec> overrideFields = new ArrayList<>(fields.size());
        for (FieldModel fm : fields) {
            if (fm.asIdOnly() && fm.relationKind() == RelKind.TO_ONE) {
                overrideFields.add(FieldSpec.builder(TypeName.OBJECT, "_" + fm.name() + "Id", Modifier.PRIVATE).build());
            } else {
                TypeName storeType = fm.isCollection() ? fm.type() : (fm.primitive() ? fm.type().box() : fm.type());
                overrideFields.add(FieldSpec.builder(storeType, "_" + fm.name(), Modifier.PRIVATE).build());
            }
        }

        // adapter + defaults + config
        FieldSpec adapterField = FieldSpec.builder(
                ClassName.get("de.splatgames.aether.generators.mvc.runtime", "PersistAdapter"),
                "adapter", Modifier.PRIVATE, Modifier.FINAL).build();

        FieldSpec defaultsField = (defaultsType != null)
                ? FieldSpec.builder(defaultsType, "defaults", Modifier.PRIVATE, Modifier.FINAL).build()
                : null;

        FieldSpec autoRelField = FieldSpec.builder(TypeName.BOOLEAN, "autoRelations", Modifier.PRIVATE, Modifier.FINAL)
                .initializer(autoRelations ? "true" : "false")
                .build();

        FieldSpec depthLimit = FieldSpec.builder(TypeName.INT, "REL_DEPTH_LIMIT", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", 2)
                .build();

        // ctors
        MethodSpec.Builder ctorDefaultB = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super($T.TRANSIENT)", ABSTRACT_BUILDER_MODE)
                .addStatement("this.adapter = null");
        if (defaultsField != null) {
            ctorDefaultB.addStatement("this.defaults = null");
        }
        MethodSpec ctorDefault = ctorDefaultB.build();

        MethodSpec.Builder ctorFullB = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ProcessorUtils.p(PERSIST_ADAPTER, "adapter"))
                .addStatement("super($T.TRANSIENT)", ABSTRACT_BUILDER_MODE)
                .addStatement("this.adapter = adapter");

        if (defaultsField != null) {
            ctorFullB.addParameter(ProcessorUtils.p(defaultsType, "defaults"))
                    .addStatement("this.defaults = defaults");
        }
        MethodSpec ctorFull = ctorFullB.build();

        // API methods (with/add/addAll/clear + asIdOnly)
        List<MethodSpec> apiMethods = new ArrayList<>();
        for (FieldModel fm : fields) {
            String baseAlias = fm.alias();
            String withName = "with" + ProcessorUtils.capitalize(baseAlias);

            if (fm.asIdOnly() && fm.relationKind() == RelKind.TO_ONE) {
                apiMethods.add(MethodSpec.methodBuilder(withName + "Id")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(builderType)
                        .addParameter(ProcessorUtils.p(TypeName.OBJECT, fm.name() + "Id"))
                        .addStatement("this._$LId = $L", fm.name(), fm.name() + "Id")
                        .addStatement("return this")
                        .addJavadoc(Javadocs.withIdDoc(fm))
                        .build());
                continue;
            }

            // withX(...)
            apiMethods.add(MethodSpec.methodBuilder(withName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(builderType)
                    .addParameter(ProcessorUtils.p(fm.type(), fm.name()))
                    .addStatement("this._$L = $L", fm.name(), fm.name())
                    .addStatement("return this")
                    .addJavadoc(Javadocs.withDoc(fm))
                    .build());


            if (fm.isCollection()) {
                String addName = "add" + ProcessorUtils.capitalize(GenHelpers.singularize(baseAlias));
                String addAllName = "addAll" + ProcessorUtils.capitalize(baseAlias);
                String clearName = "clear" + ProcessorUtils.capitalize(baseAlias);

                // addX(E)
                apiMethods.add(MethodSpec.methodBuilder(addName)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(builderType)
                        .addParameter(ProcessorUtils.p(fm.elementType(), ProcessorUtils.singularVar(fm.name())))
                        .beginControlFlow("if (this._$L == null)", fm.name())
                        .addStatement("this._$L = $L", fm.name(), ProcessorUtils.newCollectionExpr(fm))
                        .endControlFlow()
                        .addStatement("this._$L.add($L)", fm.name(), ProcessorUtils.singularVar(fm.name()))
                        .addStatement("return this")
                        // [DOCS]
                        .addJavadoc(Javadocs.addDoc(fm))
                        .build());

                // addAllX(Collection<? extends E>)
                TypeName collOfE = ParameterizedTypeName.get(
                        ClassName.get(Collection.class), WildcardTypeName.subtypeOf(fm.elementType()));
                apiMethods.add(MethodSpec.methodBuilder(addAllName)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(builderType)
                        .addParameter(ProcessorUtils.p(collOfE, fm.name()))
                        .beginControlFlow("if ($L == null || $L.isEmpty())", fm.name(), fm.name())
                        .addStatement("return this")
                        .endControlFlow()
                        .beginControlFlow("if (this._$L == null)", fm.name())
                        .addStatement("this._$L = $L", fm.name(), ProcessorUtils.newCollectionExpr(fm))
                        .endControlFlow()
                        .addStatement("this._$L.addAll($L)", fm.name(), fm.name())
                        .addStatement("return this")
                        .addJavadoc(Javadocs.addAllDoc(fm))
                        .build());

                // clearX()
                apiMethods.add(MethodSpec.methodBuilder(clearName)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(builderType)
                        .beginControlFlow("if (this._$L == null)", fm.name())
                        .addStatement("this._$L = $L", fm.name(), ProcessorUtils.newCollectionExpr(fm))
                        .nextControlFlow("else")
                        .addStatement("this._$L.clear()", fm.name())
                        .endControlFlow()
                        .addStatement("return this")
                        .addJavadoc(Javadocs.clearDoc(fm))
                        .build());
            }
        }

        // build()
        MethodSpec buildM = buildMethod(entityType, entity, fields, defaultsField);

        // helpers: defaults, reflection, collections, relations
        List<MethodSpec> defaultsHelpers = defaultsField != null ? defaultsHelpers(entityType, fields, defaultsField) : List.of();
        MethodSpec setFieldReflect = setFieldReflect();
        MethodSpec getFieldValue = getFieldValue();
        MethodSpec readIdVal = readIdValue();
        MethodSpec writeIdVal = writeIdValue();
        MethodSpec prePersistRoot = prePersistRelationsRoot();
        MethodSpec prePersistDeep = prePersistRelationsDeep(fields);


        // persistIfNeeded(): call prePersistRelations(obj) if autoRelations
        MethodSpec persistIfNeeded = MethodSpec.methodBuilder("persistIfNeeded")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entity)
                .addParameter(ProcessorUtils.p(entity, "obj"))
                .beginControlFlow("if (this.adapter == null)")
                .addStatement("return obj")
                .endControlFlow()
                .beginControlFlow("if (this.autoRelations)")
                .addStatement("prePersistRelations(obj)")
                .endControlFlow()
                .addStatement("return this.adapter.save(obj)")
                .build();

        // assemble class
        TypeSpec.Builder typeB = TypeSpec.classBuilder(builderSimple)
                .addJavadoc("Auto-generated builder for {@link $T}.\n", entity)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(superType)
                .addField(adapterField)
                .addField(autoRelField)
                .addField(depthLimit)
                .addMethod(ctorDefault)
                .addMethod(ctorFull)
                .addMethods(apiMethods)
                .addMethod(buildM)
                .addMethod(setFieldReflect)
                .addMethod(getFieldValue)
                .addMethod(readIdVal)
                .addMethod(writeIdVal)
                .addMethod(prePersistRoot)
                .addMethod(prePersistDeep)
                .addMethod(persistIfNeeded)
                .addMethod(hasAnnoRuntime())
                .addMethod(ensurePersistent())
                .addJavadoc(Javadocs.classDoc(entitySimple, defaultsField != null, autoRelations, fields))
                .addAnnotation(
                        AnnotationSpec.builder(javax.annotation.processing.Generated.class)
                                .addMember("value", "$S", "Aether Generators MVC")
                                .build());

        for (FieldSpec fs : overrideFields) typeB.addField(fs);

        if (defaultsField != null) {
            typeB.addField(defaultsField);
            defaultsHelpers.forEach(typeB::addMethod);
        }

        TypeSpec builder = typeB.build();

        JavaFile javaFile = JavaFile.builder(outPkg, builder).indent("    ").build();
        JavaFileObject out = this.processingEnv.getFiler().createSourceFile(outPkg + "." + builderSimple, entityType);
        try (var w = out.openWriter()) {
            javaFile.writeTo(w);
        }
    }

    /**
     * Verifies that the target entity declares a non-private no-arg constructor.
     * This is required for reflective instantiation in the generated builder.
     *
     * @param type the entity type to check (never {@code null})
     * @throws StopProcessing if the constructor is missing; a compiler error is emitted
     */
    private void requireNoArgCtor(@NotNull final TypeElement type) {
        boolean ok = type.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .anyMatch(c -> c.getParameters().isEmpty() && !c.getModifiers().contains(Modifier.PRIVATE));
        if (!ok) {
            this.log.printMessage(Diagnostic.Kind.ERROR,
                    "@MvcBuilder target must declare a non-private no-arg constructor (required for codegen)", type);
            throw new StopProcessing();
        }
    }

    /**
     * Validates the defaults provider API against the selected fields:
     * checks for zero-arg methods with names matching fields (and <code>&lt;field&gt;Id</code> for asIdOnly to-one).
     * Emits warnings for suspicious signatures (e.g., methods returning {@code void}).
     *
     * @param entity         the entity type (for diagnostics) (never {@code null})
     * @param defaultsTypeEl the defaults provider type element (never {@code null})
     * @param fields         the collected field models (never {@code null})
     */
    private void validateDefaultsProvider(@NotNull final TypeElement entity,
                                          @NotNull final TypeElement defaultsTypeEl,
                                          @NotNull final List<FieldModel> fields) {
        Map<String, ExecutableElement> zeroArg = new HashMap<>();
        for (Element e : defaultsTypeEl.getEnclosedElements()) {
            if (e.getKind() == ElementKind.METHOD) {
                ExecutableElement m = (ExecutableElement) e;
                if (m.getParameters().isEmpty()) zeroArg.put(m.getSimpleName().toString(), m);
            }
        }
        for (FieldModel fm : fields) {
            if (fm.asIdOnly() && fm.relationKind() == RelKind.TO_ONE) {
                String name = fm.name() + "Id";
                ExecutableElement m = zeroArg.get(name);
                if (m == null) continue; // optional
                // no strict return-type check; warn if void
                if (m.getReturnType().getKind() == TypeKind.VOID) {
                    this.log.printMessage(Diagnostic.Kind.WARNING,
                            "Defaults method '" + name + "()' returns void; it should return an id value.", defaultsTypeEl);
                }
            } else {
                String name = fm.name();
                ExecutableElement m = zeroArg.get(name);
                if (m == null) continue;
                if (m.getReturnType().getKind() == TypeKind.VOID) {
                    this.log.printMessage(Diagnostic.Kind.WARNING,
                            "Defaults method '" + name + "()' returns void; it should return a value compatible with field '" + fm.name() + "'.",
                            defaultsTypeEl);
                }
            }
        }
    }

    /**
     * Detects conflicts in the generated fluent API (e.g., duplicate <code>withX</code> / <code>addX</code> names).
     * If any conflicts are found, emits a compile error and aborts generation for this entity.
     *
     * @param entity        the entity type (for diagnostics) (never {@code null})
     * @param builderSimple the simple name of the builder to be generated (never {@code null})
     * @param fields        the collected field models (never {@code null})
     * @throws StopProcessing if conflicts are detected
     */
    private void checkApiConflicts(@NotNull final TypeElement entity,
                                   @NotNull final String builderSimple,
                                   @NotNull final List<FieldModel> fields) {
        Set<String> methods = new HashSet<>();
        List<String> conflicts = new ArrayList<>();
        for (FieldModel fm : fields) {
            String base = ProcessorUtils.capitalize(fm.alias());
            String w = "with" + base;
            ProcessorUtils.addOrConflict(methods, conflicts, w);
            if (fm.asIdOnly() && fm.relationKind() == RelKind.TO_ONE) {
                ProcessorUtils.addOrConflict(methods, conflicts, w + "Id");
            }
            if (fm.isCollection()) {
                ProcessorUtils.addOrConflict(methods, conflicts, "add" + ProcessorUtils.capitalize(GenHelpers.singularize(fm.alias())));
                ProcessorUtils.addOrConflict(methods, conflicts, "addAll" + base);
                ProcessorUtils.addOrConflict(methods, conflicts, "clear" + base);
            }
        }
        if (!conflicts.isEmpty()) {
            this.log.printMessage(Diagnostic.Kind.ERROR,
                    "Generated API method name conflicts in " + builderSimple + ": " + conflicts,
                    entity);
            throw new StopProcessing();
        }
    }

    /**
     * Creates the {@code build()} method for the generated builder.
     *
     * <p>The method instantiates the entity, applies overrides or defaults, prefers setters where available,
     * and falls back to reflection otherwise. Collections are initialized to sensible defaults.</p>
     *
     * @param entityType    the entity type element (never {@code null})
     * @param entity        the {@link ClassName} of the entity (never {@code null})
     * @param fields        the collected field models (never {@code null})
     * @param defaultsField the defaults field spec, or {@code null} if not configured
     * @return the {@link MethodSpec} for {@code build()} (never {@code null})
     */
    @NotNull
    private MethodSpec buildMethod(@NotNull final TypeElement entityType,
                                   @NotNull final ClassName entity,
                                   @NotNull final List<FieldModel> fields,
                                   @Nullable final FieldSpec defaultsField) {
        MethodSpec.Builder b = MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entity)
                .addStatement("$T obj = new $T()", entity, entity);

        for (FieldModel fm : fields) {
            if (fm.asIdOnly() && fm.relationKind() == RelKind.TO_ONE) {
                String idTmp = fm.name() + "Id_val";
                if (defaultsField != null) {
                    b.addStatement("Object $L = (this._$LId != null) ? this._$LId : (this.defaults != null ? tryDefaults_$LId() : null)",
                            idTmp, fm.name(), fm.name(), fm.name());
                } else {
                    b.addStatement("Object $L = this._$LId", idTmp, fm.name());
                }
                b.beginControlFlow("if ($L != null)", idTmp)
                        .addStatement("Object ref = new $T()", fm.type())
                        .addStatement("writeId(ref, $L)", idTmp)
                        .addStatement("setFieldReflect(obj, $S, ref)", fm.name())
                        .endControlFlow();
                continue;
            }

            String store = "_" + fm.name();
            String tmpVal = fm.name() + "_val";

            if (defaultsField != null) {
                b.addStatement("$T $L = (this.$L != null) ? this.$L : (this.defaults != null ? tryDefaults_$L() : null)",
                        fm.primitive() ? fm.type().box() : fm.type(), tmpVal, store, store, fm.name());
            } else {
                b.addStatement("$T $L = this.$L", fm.primitive() ? fm.type().box() : fm.type(), tmpVal, store);
            }

            boolean hasSetter = hasSetter(entityType, fm.setterName(), fm.type());
            if (hasSetter) {
                if (fm.primitive()) {
                    b.addStatement("obj.$L(($L != null) ? $L : $L)", fm.setterName(), tmpVal, tmpVal, ProcessorUtils.primitiveDefaultLiteral(fm.type()));
                } else if (fm.isCollection()) {
                    b.addStatement("obj.$L(($L != null) ? $L : $L)", fm.setterName(), tmpVal, tmpVal, ProcessorUtils.newCollectionExpr(fm));
                } else {
                    b.addStatement("obj.$L($L)", fm.setterName(), tmpVal);
                }
            } else {
                if (fm.primitive()) {
                    b.addStatement("setFieldReflect(obj, $S, ($L != null ? $L : $L))", fm.name(), tmpVal, tmpVal, ProcessorUtils.boxedDefaultExpr(fm.type()));
                } else if (fm.isCollection()) {
                    b.addStatement("setFieldReflect(obj, $S, ($L != null ? $L : $L))", fm.name(), tmpVal, tmpVal, ProcessorUtils.newCollectionExpr(fm));
                } else {
                    b.addStatement("setFieldReflect(obj, $S, $L)", fm.name(), tmpVal);
                }
            }
        }

        b.addStatement("return obj");
        return b.build();
    }

    /**
     * Generates private helper methods that invoke zero-arg defaults provider methods reflectively.
     * One helper is created per configured field (<code>tryDefaults_field()</code> or <code>tryDefaults_fieldId()</code>).
     *
     * @param entityType    the entity type element (never {@code null})
     * @param fields        the collected field models (never {@code null})
     * @param defaultsField the non-null defaults field in the generated builder
     * @return a list of helper methods to add to the generated type (never {@code null})
     */

    @NotNull
    private List<MethodSpec> defaultsHelpers(@NotNull final TypeElement entityType,
                                             @NotNull final List<FieldModel> fields,
                                             @NotNull final FieldSpec defaultsField) {
        List<MethodSpec> methods = new ArrayList<>();
        for (FieldModel fm : fields) {
            if (fm.asIdOnly() && fm.relationKind() == RelKind.TO_ONE) {
                methods.add(MethodSpec.methodBuilder("tryDefaults_" + fm.name() + "Id")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(TypeName.OBJECT)
                        .beginControlFlow("try")
                        .addStatement("$T m = this.defaults.getClass().getMethod($S)", java.lang.reflect.Method.class, fm.name() + "Id")
                        .addStatement("return m.invoke(this.defaults)")
                        .nextControlFlow("catch (Exception ignored)")
                        .addStatement("return null")
                        .endControlFlow()
                        .build());
            } else {
                methods.add(MethodSpec.methodBuilder("tryDefaults_" + fm.name())
                        .addModifiers(Modifier.PRIVATE)
                        .returns(fm.primitive() ? fm.type().box() : fm.type())
                        .beginControlFlow("try")
                        .addStatement("$T m = this.defaults.getClass().getMethod($S)", java.lang.reflect.Method.class, fm.name())
                        .addStatement("Object v = m.invoke(this.defaults)")
                        .addStatement("return ($T) v", fm.primitive() ? fm.type().box() : fm.type())
                        .nextControlFlow("catch (Exception ignored)")
                        .addStatement("return null")
                        .endControlFlow()
                        .build());
            }
        }
        return methods;
    }

    /**
     * Produces a private helper method {@code setFieldReflect(Object target, String field, Object value)}
     * that assigns a field via reflection (declared field, setAccessible=true).
     *
     * @return the {@link MethodSpec} for the reflection setter (never {@code null})
     */
    @NotNull
    private MethodSpec setFieldReflect() {
        return MethodSpec.methodBuilder("setFieldReflect")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ProcessorUtils.p(Object.class, "target"))
                .addParameter(ProcessorUtils.p(String.class, "field"))
                .addParameter(ProcessorUtils.p(Object.class, "value"))
                .beginControlFlow("if (target == null || field == null)")
                .addStatement("return")
                .endControlFlow()
                .addStatement("Class<?> t = target.getClass()")
                .beginControlFlow("while (t != null && t != Object.class)")
                .beginControlFlow("try")
                .addStatement("$T f = t.getDeclaredField(field)", java.lang.reflect.Field.class)
                .addStatement("f.setAccessible(true)")
                .addStatement("f.set(target, value)")
                .addStatement("return")
                .nextControlFlow("catch ($T e)", NoSuchFieldException.class)
                .addStatement("t = t.getSuperclass()")
                .nextControlFlow("catch ($T e)", IllegalAccessException.class)
                .addStatement("return")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return")
                .build();
    }

    // ---------- scans ----------

    /**
     * Produces a private helper method {@code getFieldValue(Object target, String field)}
     * that reads a declared field via reflection.
     *
     * @return the {@link MethodSpec} for the reflection getter (never {@code null})
     */
    @NotNull
    private MethodSpec getFieldValue() {
        return MethodSpec.methodBuilder("getFieldValue")
                .addModifiers(Modifier.PRIVATE)
                .returns(Object.class)
                .addParameter(ProcessorUtils.p(Object.class, "target"))
                .addParameter(ProcessorUtils.p(String.class, "field"))
                .beginControlFlow("if (target == null || field == null)")
                .addStatement("return null")
                .endControlFlow()

                .addStatement("String cap = Character.toUpperCase(field.charAt(0)) + field.substring(1)")
                .addStatement("String[] candidates = new String[] { $S + cap, $S + cap }", "get", "is")
                .beginControlFlow("for (String name : candidates)")
                .beginControlFlow("try")
                .addStatement("$T m = target.getClass().getMethod(name)", java.lang.reflect.Method.class)
                .addStatement("m.setAccessible(true)")
                .addStatement("return m.invoke(target)")
                .nextControlFlow("catch ($T ignored)", NoSuchMethodException.class)
                .nextControlFlow("catch ($T e)", ReflectiveOperationException.class)
                .addStatement("return null")
                .endControlFlow()
                .endControlFlow()

                .addStatement("Class<?> t = target.getClass()")
                .beginControlFlow("while (t != null && t != Object.class)")
                .beginControlFlow("try")
                .addStatement("$T f = t.getDeclaredField(field)", java.lang.reflect.Field.class)
                .addStatement("f.setAccessible(true)")
                .addStatement("return f.get(target)")
                .nextControlFlow("catch ($T e)", NoSuchFieldException.class)
                .addStatement("t = t.getSuperclass()")
                .nextControlFlow("catch ($T e)", IllegalAccessException.class)
                .addStatement("return null")
                .endControlFlow()
                .endControlFlow()

                .addStatement("return null")
                .build();
    }


    /**
     * Produces a private helper method {@code readId(Object entity)} that tries to obtain the identifier value
     * by scanning the class hierarchy for a field annotated with {@code jakarta.persistence.Id} /
     * {@code javax.persistence.Id} or named {@code id}.
     *
     * @return the {@link MethodSpec} for the ID reader (never {@code null})
     */
    @NotNull
    private MethodSpec readIdValue() {
        return MethodSpec.methodBuilder("readId")
                .addModifiers(Modifier.PRIVATE)
                .returns(Object.class)
                .addParameter(ProcessorUtils.p(Object.class, "entity"))
                .beginControlFlow("try")
                .addStatement("Class<?> t = entity.getClass()")
                .beginControlFlow("while (t != null && t != Object.class)")
                .addStatement("$T[] fields = t.getDeclaredFields()", java.lang.reflect.Field.class)
                .beginControlFlow("for ($T f : fields)", java.lang.reflect.Field.class)
                .addStatement("f.setAccessible(true)")
                .beginControlFlow(
                        "if (hasAnno(f, $S) || hasAnno(f, $S) || f.getName().equals($S))",
                        "jakarta.persistence.Id", "javax.persistence.Id", "id"
                )
                .addStatement("return f.get(entity)")
                .endControlFlow()
                .endControlFlow()
                .addStatement("t = t.getSuperclass()")
                .endControlFlow()
                .addStatement("return null")
                .nextControlFlow("catch (Exception e)")
                .addStatement("throw new RuntimeException(e)")
                .endControlFlow()
                .build();
    }

    /**
     * Produces a private helper method {@code writeId(Object target, Object idVal)} that tries to assign
     * the given id value to the target object by scanning for a {@code setId(...)} method or an {@code id} field.
     *
     * @return the {@link MethodSpec} for the ID writer (never {@code null})
     * @since 1.1.1
     */
    @NotNull
    private MethodSpec writeIdValue() {
        return MethodSpec.methodBuilder("writeId")
                .addModifiers(Modifier.PRIVATE)
                .returns(void.class)
                .addParameter(ProcessorUtils.p(Object.class, "target"))
                .addParameter(ProcessorUtils.p(Object.class, "idVal"))
                .beginControlFlow("if (target == null)")
                .addStatement("return")
                .endControlFlow()
                .addStatement("Class<?> cls = target.getClass()")

                .beginControlFlow("for ($T m : cls.getMethods())", java.lang.reflect.Method.class)
                .beginControlFlow("if (m.getName().equals($S) && m.getParameterCount() == 1)", "setId")
                .beginControlFlow("try")
                .addStatement("m.setAccessible(true)")
                .addStatement("m.invoke(target, idVal)")
                .addStatement("return")
                .nextControlFlow("catch ($T | $T | $T ignored)",
                        IllegalAccessException.class,
                        java.lang.reflect.InvocationTargetException.class,
                        IllegalArgumentException.class)
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()

                .addStatement("Class<?> c = cls")
                .beginControlFlow("while (c != null && c != Object.class)")
                .beginControlFlow("try")
                .addStatement("$T f = c.getDeclaredField($S)", java.lang.reflect.Field.class, "id")
                .addStatement("f.setAccessible(true)")
                .addStatement("f.set(target, idVal)")
                .addStatement("return")
                .nextControlFlow("catch ($T e)", NoSuchFieldException.class)
                .addStatement("c = c.getSuperclass()")
                .nextControlFlow("catch ($T ignored)", IllegalAccessException.class)
                .addStatement("return")
                .endControlFlow()
                .endControlFlow()

                .addStatement("return")
                .build();
    }


    /**
     * Produces a private helper method {@code hasAnno(Field field, String fqcn)} that checks
     * at runtime whether the given reflected field has an annotation with the given FQCN.
     *
     * @return the {@link MethodSpec} for the runtime annotation check (never {@code null})
     */
    @NotNull
    private MethodSpec hasAnnoRuntime() {
        return MethodSpec.methodBuilder("hasAnno")
                .addModifiers(Modifier.PRIVATE)
                .returns(boolean.class)
                .addParameter(ProcessorUtils.p(java.lang.reflect.Field.class, "field"))
                .addParameter(ProcessorUtils.p(String.class, "fqcn"))
                .beginControlFlow("for ($T a : field.getAnnotations())", java.lang.annotation.Annotation.class)
                .beginControlFlow("if (a.annotationType().getName().equals(fqcn))")
                .addStatement("return true")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return false")
                .build();
    }

    /**
     * Produces a private helper method {@code ensurePersistent(Object rel, int depth, Set<Object> visited)}
     * that, if a related object has no identifier, pre-persists its relations recursively (bounded by depth)
     * and invokes the persistence adapter to save it. Cycles are guarded via {@code visited}.
     *
     * @return the {@link MethodSpec} for the persistence helper (never {@code null})
     */
    @NotNull
    private MethodSpec ensurePersistent() {
        return MethodSpec.methodBuilder("ensurePersistent")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ProcessorUtils.p(Object.class, "rel"))
                .addParameter(ProcessorUtils.p(TypeName.INT, "depth"))
                .addParameter(ProcessorUtils.p(ParameterizedTypeName.get(Set.class, Object.class), "visited"))
                .beginControlFlow("if (rel == null || visited.contains(rel))")
                .addStatement("return")
                .endControlFlow()
                .addStatement("visited.add(rel)")
                .addStatement("Object id = readId(rel)")
                .beginControlFlow("if (id == null && this.adapter != null)")
                .addStatement("prePersistRelations(rel, depth, visited)")
                .addStatement("this.adapter.save(rel)")
                .endControlFlow()
                .build();
    }

    /**
     * Produces a private helper method {@code prePersistRelations(Object obj)} that initializes relation
     * pre-persistence with a fixed depth limit and a fresh visited set.
     *
     * @return the {@link MethodSpec} for the root pre-persist dispatcher (never {@code null})
     */
    @NotNull
    private MethodSpec prePersistRelationsRoot() {
        return MethodSpec.methodBuilder("prePersistRelations")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ProcessorUtils.p(Object.class, "obj"))
                .addStatement("$T<Object> visited = $T.newSetFromMap(new $T<>())",
                        java.util.Set.class, java.util.Collections.class, java.util.IdentityHashMap.class)
                .addStatement("prePersistRelations(obj, REL_DEPTH_LIMIT, visited)")
                .build();
    }

    /**
     * Produces a private helper method {@code prePersistRelations(Object obj, int depth, Set<Object> visited)}
     * that walks over relation fields (to-one / to-many) and ensures persistence of related objects
     * before the root is saved, respecting a depth limit and cycle guard.
     *
     * @param fields the collected field models used to generate the traversal (never {@code null})
     * @return the {@link MethodSpec} for the deep pre-persist traversal (never {@code null})
     */
    @NotNull
    private MethodSpec prePersistRelationsDeep(@NotNull final List<FieldModel> fields) {
        MethodSpec.Builder b = MethodSpec.methodBuilder("prePersistRelations")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ProcessorUtils.p(Object.class, "obj"))
                .addParameter(ProcessorUtils.p(TypeName.INT, "depth"))
                .addParameter(ProcessorUtils.p(ParameterizedTypeName.get(Set.class, Object.class), "visited"))
                .beginControlFlow("if (obj == null || depth <= 0)")
                .addStatement("return")
                .endControlFlow()
                .beginControlFlow("if (visited.contains(obj))")
                .addStatement("return")
                .endControlFlow()
                .addStatement("visited.add(obj)");

        for (FieldModel fm : fields) {
            if (fm.relationKind() == RelKind.NONE) continue;

            String safeName = fm.name().replaceAll("[^A-Za-z0-9_]", "_");
            if (fm.relationKind() == RelKind.TO_ONE) {
                String relVar = "rel_" + safeName;
                b.addStatement("Object $L = getFieldValue(obj, $S)", relVar, fm.name())
                        .beginControlFlow("if ($L != null)", relVar)
                        .addStatement("ensurePersistent($L, depth - 1, visited)", relVar)
                        .endControlFlow();
            } else {
                String colVar = "col_" + safeName;
                b.addStatement("Object $L = getFieldValue(obj, $S)", colVar, fm.name())
                        .beginControlFlow("if ($L instanceof $T c)", colVar, java.util.Collection.class)
                        .beginControlFlow("for (Object e : c)")
                        .addStatement("ensurePersistent(e, depth - 1, visited)")
                        .endControlFlow()
                        .endControlFlow();
            }
        }

        b.addStatement("return");
        return b.build();
    }


    /**
     * Collects eligible fields from the entity type (and optionally its superclasses), honoring
     * {@link de.splatgames.aether.generators.mvc.annotations.MvcIgnore} and
     * {@link de.splatgames.aether.generators.mvc.annotations.MvcField} with the configured field policy.
     *
     * <p>Determines the effective alias, relation kind, collection characteristics, and setter name.</p>
     *
     * @param type           the entity type element (never {@code null})
     * @param includeSuper   whether superclass fields should be considered
     * @param explicitPolicy whether only {@link de.splatgames.aether.generators.mvc.annotations.MvcField}-annotated
     *                       fields are included
     * @return an ordered list of {@link FieldModel} instances (never {@code null})
     */
    @NotNull
    private List<FieldModel> collectFields(@NotNull final TypeElement type,
                                           final boolean includeSuper,
                                           final boolean explicitPolicy) {
        Iterable<? extends Element> members = includeSuper ? this.elements.getAllMembers(type) : type.getEnclosedElements();

        TypeMirror collErasure = this.elements.getTypeElement("java.util.Collection").asType();
        TypeMirror listErasure = this.elements.getTypeElement("java.util.List").asType();
        TypeMirror setErasure = this.elements.getTypeElement("java.util.Set").asType();

        List<FieldModel> result = new ArrayList<>();
        for (Element m : members) {
            if (m.getKind() != ElementKind.FIELD) continue;
            VariableElement f = (VariableElement) m;

            Set<Modifier> mods = f.getModifiers();
            if (mods.contains(Modifier.STATIC) || mods.contains(Modifier.TRANSIENT)) continue;

            MvcIgnore ign = f.getAnnotation(MvcIgnore.class);
            MvcField mf = f.getAnnotation(MvcField.class);

            boolean ignore = (ign != null) || (mf != null && mf.ignore());
            if (ignore) continue;
            if (explicitPolicy && mf == null) continue;

            String rawName = f.getSimpleName().toString();
            boolean isBoolType = f.asType().getKind() == TypeKind.BOOLEAN;

            String alias = (mf != null && !mf.alias().isBlank()) ? mf.alias() : ProcessorUtils.defaultAliasFor(rawName, isBoolType);
            boolean asIdOnly = (mf != null && mf.asIdOnly());

            TypeMirror tm = f.asType();
            TypeName tn = TypeName.get(tm);
            boolean primitive = tm.getKind().isPrimitive();

            boolean isCollection = this.types.isAssignable(this.types.erasure(tm), this.types.erasure(collErasure));
            boolean isList = this.types.isAssignable(this.types.erasure(tm), this.types.erasure(listErasure));
            boolean isSet = this.types.isAssignable(this.types.erasure(tm), this.types.erasure(setErasure));

            TypeName elementType = isCollection ? ProcessorUtils.resolveElementType(tm) : TypeName.OBJECT;

            RelKind rel = ProcessorUtils.relationKind(f);

            String setterName = "set" + ProcessorUtils.capitalize(rawName);

            result.add(new FieldModel(
                    rawName, tn, setterName, primitive, isBoolType,
                    alias, asIdOnly, isCollection, isList, isSet, elementType,
                    rel
            ));
        }
        return result;
    }

    /**
     * Checks whether the entity declares a setter with the given name and parameter type.
     *
     * @param entity     the entity type to inspect (never {@code null})
     * @param setterName the expected setter name (never {@code null})
     * @param paramType  the expected parameter type (never {@code null})
     * @return {@code true} if such a setter exists; {@code false} otherwise
     */
    private boolean hasSetter(@NotNull final TypeElement entity,
                              @NotNull final String setterName,
                              @NotNull final TypeName paramType) {
        for (Element e : this.elements.getAllMembers(entity)) {
            if (e.getKind() == ElementKind.METHOD) {
                ExecutableElement m = (ExecutableElement) e;
                if (m.getSimpleName().contentEquals(setterName) && m.getParameters().size() == 1) {
                    TypeName p = TypeName.get(m.getParameters().get(0).asType());
                    if (p.equals(paramType)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolves the configured defaults provider type from {@link MvcBuilder#defaultProvider()}.
     * Handles {@link javax.lang.model.type.MirroredTypeException} to obtain the {@link TypeName}
     * without loading the class.
     *
     * @param cfg the annotation instance (may be {@code null})
     * @return the provider type name, or {@code null} if none / {@code Void}
     */
    @Nullable
    private TypeName resolveDefaultsType(@Nullable final MvcBuilder cfg) {
        if (cfg == null) return null;
        try {
            Class<?> c = cfg.defaultProvider();
            return TypeName.get(c);
        } catch (MirroredTypeException mte) {
            TypeMirror tm = mte.getTypeMirror();
            if (tm == null) return null;
            if ("java.lang.Void".equals(tm.toString())) return null;
            return TypeName.get(tm);
        }
    }

    /**
     * Resolves the {@link TypeElement} of the configured defaults provider, if any.
     * Uses {@link javax.lang.model.type.MirroredTypeException} to access the {@link TypeMirror}
     * in the current processing environment.
     *
     * @param cfg the annotation instance (may be {@code null})
     * @return the provider type element, or {@code null} if none / {@code Void}
     */
    @Nullable
    private TypeElement resolveDefaultsTypeElement(@Nullable final MvcBuilder cfg) {
        if (cfg == null) return null;
        try {
            cfg.defaultProvider();
            return null;
        } catch (MirroredTypeException mte) {
            TypeMirror tm = mte.getTypeMirror();
            if (tm == null) return null;
            if ("java.lang.Void".equals(tm.toString())) return null;
            return (TypeElement) this.types.asElement(tm);
        }
    }

    // ---------- stop processing helper ----------
    private static final class StopProcessing extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
