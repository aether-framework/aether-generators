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

package de.splatgames.aether.generators.mvc.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Marks a class as a defaults provider for generated builders.
 * <p>
 * A defaults provider is a simple class with zero-arg methods returning default values used
 * by the builder when a field is not explicitly set. Example:
 * </p>
 *
 * <pre>{@code
 * @MvcDefaults
 * public class RoleDefaults {
 *   public String roleName() { return "USER"; }
 *   public boolean isDefault() { return false; }
 * }
 * }</pre>
 *
 * <p>
 * The processor will try to instantiate this class reflectively using a no-arg constructor.
 * You can also reference a provider directly via {@link MvcBuilder#defaultProvider()}.
 * </p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@Documented
@Target(TYPE)
@Retention(SOURCE)
public @interface MvcDefaults {
}
