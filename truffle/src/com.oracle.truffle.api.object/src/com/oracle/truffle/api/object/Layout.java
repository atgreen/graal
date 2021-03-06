/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api.object;

import java.util.EnumSet;
import java.util.ServiceLoader;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.object.Shape.Allocator;

/**
 * Describes layout and behavior of a {@link DynamicObject} subclass and is used to create shapes.
 *
 * An object may change its shape but only to shapes of the same layout.
 *
 * @since 0.8 or earlier
 */
public abstract class Layout {
    /** @since 0.8 or earlier */
    public static final String OPTION_PREFIX = "truffle.object.";

    private static final LayoutFactory LAYOUT_FACTORY = loadLayoutFactory();

    /**
     * Constructor for subclasses.
     *
     * @since 0.8 or earlier
     */
    protected Layout() {
    }

    /**
     * Specifies the allowed implicit casts between primitive types without losing type information.
     *
     * @since 0.8 or earlier
     */
    public enum ImplicitCast {
        /** @since 0.8 or earlier */
        IntToDouble,
        /** @since 0.8 or earlier */
        IntToLong
    }

    /**
     * Creates a new {@link Builder}.
     *
     * @since 0.8 or earlier
     */
    public static Builder newLayout() {
        return new Builder();
    }

    /**
     * Equivalent to {@code Layout.newLayout().build()}.
     *
     * @since 0.8 or earlier
     */
    public static Layout createLayout() {
        return newLayout().build();
    }

    /** @since 0.8 or earlier */
    public abstract DynamicObject newInstance(Shape shape);

    /** @since 0.8 or earlier */
    public abstract Class<? extends DynamicObject> getType();

    /**
     * Create a root shape.
     *
     * @param objectType that describes the object instance with this shape.
     * @since 0.8 or earlier
     */
    public abstract Shape createShape(ObjectType objectType);

    /**
     * Create a root shape.
     *
     * @param objectType that describes the object instance with this shape.
     * @param sharedData for language-specific use
     * @since 0.8 or earlier
     */
    public abstract Shape createShape(ObjectType objectType, Object sharedData);

    /**
     * Create a root shape.
     *
     * @param objectType that describes the object instance with this shape.
     * @param sharedData for language-specific use
     * @param id for language-specific use
     * @return new instance of a shape
     * @since 0.8 or earlier
     */
    public abstract Shape createShape(ObjectType objectType, Object sharedData, int id);

    /**
     * Create an allocator for static property creation. Reserves all array extension slots.
     *
     * @since 0.8 or earlier
     */
    public abstract Allocator createAllocator();

    /** @since 0.8 or earlier */
    protected static LayoutFactory getFactory() {
        return LAYOUT_FACTORY;
    }

    private static LayoutFactory loadLayoutFactory() {
        LayoutFactory layoutFactory = Truffle.getRuntime().getCapability(LayoutFactory.class);
        if (layoutFactory == null) {
            ServiceLoader<LayoutFactory> serviceLoader = ServiceLoader.load(LayoutFactory.class, Layout.class.getClassLoader());
            layoutFactory = selectLayoutFactory(serviceLoader);
            if (layoutFactory == null) {
                throw new AssertionError("LayoutFactory not found");
            }
        }
        return layoutFactory;
    }

    private static LayoutFactory selectLayoutFactory(Iterable<LayoutFactory> availableLayoutFactories) {
        String layoutFactoryImplName = System.getProperty(OPTION_PREFIX + "LayoutFactory");
        LayoutFactory bestLayoutFactory = null;
        for (LayoutFactory currentLayoutFactory : availableLayoutFactories) {
            if (layoutFactoryImplName != null) {
                if (currentLayoutFactory.getClass().getName().equals(layoutFactoryImplName)) {
                    return currentLayoutFactory;
                }
            } else {
                if (bestLayoutFactory == null) {
                    bestLayoutFactory = currentLayoutFactory;
                } else if (currentLayoutFactory.getPriority() >= bestLayoutFactory.getPriority()) {
                    assert currentLayoutFactory.getPriority() != bestLayoutFactory.getPriority();
                    bestLayoutFactory = currentLayoutFactory;
                }
            }
        }
        return bestLayoutFactory;
    }

    /**
     * Layout builder.
     *
     * @see Layout
     * @since 0.8 or earlier
     */
    public static final class Builder {
        private EnumSet<ImplicitCast> allowedImplicitCasts;
        private boolean polymorphicUnboxing;

        /**
         * Create a new layout builder.
         */
        private Builder() {
            this.allowedImplicitCasts = EnumSet.noneOf(ImplicitCast.class);
        }

        /**
         * Build {@link Layout} from the configuration in this builder.
         *
         * @since 0.8 or earlier
         */
        public Layout build() {
            return Layout.getFactory().createLayout(this);
        }

        /**
         * Set the allowed implicit casts in this layout.
         *
         * @see Layout.ImplicitCast
         * @since 0.8 or earlier
         */
        public Builder setAllowedImplicitCasts(EnumSet<ImplicitCast> allowedImplicitCasts) {
            this.allowedImplicitCasts = allowedImplicitCasts;
            return this;
        }

        /**
         * Add an allowed implicit cast in this layout.
         *
         * @see Layout.ImplicitCast
         * @since 0.8 or earlier
         */
        public Builder addAllowedImplicitCast(ImplicitCast allowedImplicitCast) {
            this.allowedImplicitCasts.add(allowedImplicitCast);
            return this;
        }

        /**
         * If {@code true}, try to keep properties with polymorphic primitive types unboxed.
         *
         * @since 0.8 or earlier
         */
        public Builder setPolymorphicUnboxing(boolean polymorphicUnboxing) {
            this.polymorphicUnboxing = polymorphicUnboxing;
            return this;
        }
    }

    /** @since 0.8 or earlier */
    protected static EnumSet<ImplicitCast> getAllowedImplicitCasts(Builder builder) {
        return builder.allowedImplicitCasts;
    }

    /** @since 0.8 or earlier */
    protected static boolean getPolymorphicUnboxing(Builder builder) {
        return builder.polymorphicUnboxing;
    }
}
