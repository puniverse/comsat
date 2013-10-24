/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
/*
 * Modified class to work around Jersey dependency on old, non-compatible ASM
 */

package org.glassfish.jersey.server.internal.scanning;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.OsgiRegistry;
import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A scanner listener that processes Java class files (resource names
 * ending in ".class") annotated with one or more of a set of declared
 * annotations.
 * <p>
 * Java classes of a Java class file are processed, using ASM, to ascertain
 * if those classes are annotated with one or more of the set of declared
 * annotations.
 * <p>
 * Such an annotated Java class of a Java class file is loaded if the class
 * is public or is an inner class that is static and public.
 *
 * @author Paul Sandoz
 */
public final class AnnotationAcceptingListener implements ResourceProcessor {

    private final ClassLoader classloader;

    private final Set<Class<?>> classes;

    private final Set<String> annotations;

    private final AnnotationAcceptingListener.AnnotatedClassVisitor classVisitor;

    /**
     * Create a scanning listener to check for Java classes in Java
     * class files annotated with {@link javax.ws.rs.Path} or {@link javax.ws.rs.ext.Provider}.
     *
     * @return new instance of {@link AnnotationAcceptingListener} which looks for
     * {@link javax.ws.rs.Path} or {@link javax.ws.rs.ext.Provider} annotated classes.
     *
     */
    @SuppressWarnings({"unchecked"})
    public static AnnotationAcceptingListener newJaxrsResourceAndProviderListener() {
        return new AnnotationAcceptingListener(Path.class, Provider.class);
    }

    /**
     * Create a scanning listener to check for Java classes in Java
     * class files annotated with {@link Path} or {@link Provider}.
     *
     * @param classLoader the class loader to use to load Java classes that
     *        are annotated with any one of the annotations.
     * @return new instance of {@link AnnotationAcceptingListener} which looks for
     * {@link javax.ws.rs.Path} or {@link javax.ws.rs.ext.Provider} annotated classes.
     */
    @SuppressWarnings({"unchecked"})
    public static AnnotationAcceptingListener newJaxrsResourceAndProviderListener(ClassLoader classLoader) {
        return new AnnotationAcceptingListener(classLoader, Path.class, Provider.class);
    }

    /**
     * Create a scanner listener to check for annotated Java classes in Java
     * class files.
     *
     * @param annotations the set of annotation classes to check on Java class
     *        files.
     */
    public AnnotationAcceptingListener(Class<? extends Annotation>... annotations) {
        this(AccessController.doPrivileged(ReflectionHelper.getContextClassLoaderPA()), annotations);
    }

    /**
     * Create a scanner listener to check for annotated Java classes in Java
     * class files.
     *
     * @param classloader the class loader to use to load Java classes that
     *        are annotated with any one of the annotations.
     * @param annotations the set of annotation classes to check on Java class
     *        files.
     */
    public AnnotationAcceptingListener(ClassLoader classloader,
                                       Class<? extends Annotation>... annotations) {
        this.classloader = classloader;
        this.classes = new LinkedHashSet<Class<?>>();
        this.annotations = getAnnotationSet(annotations);
        this.classVisitor = new AnnotationAcceptingListener.AnnotatedClassVisitor();
    }

    /**
     * Get the set of annotated classes.
     *
     * @return the set of annotated classes.
     */
    public Set<Class<?>> getAnnotatedClasses() {
        return classes;
    }

    private Set<String> getAnnotationSet(Class<? extends Annotation>... annotations) {
        Set<String> a = new HashSet<String>();
        for (Class c : annotations) {
            a.add("L" + c.getName().replaceAll("\\.", "/") + ";");
        }
        return a;
    }

    // ScannerListener
    public boolean accept(String name) {
        return !(name == null || name.isEmpty()) && name.endsWith(".class");

    }

    public void process(String name, InputStream in) throws IOException {
        new ClassReader(in).accept(classVisitor, 0);
    }

    //

    private final class AnnotatedClassVisitor extends ClassVisitor {

        public AnnotatedClassVisitor() {
            super(Opcodes.ASM4);
        }

        /**
         * The name of the visited class.
         */
        private String className;
        /**
         * True if the class has the correct scope
         */
        private boolean isScoped;
        /**
         * True if the class has the correct declared annotations
         */
        private boolean isAnnotated;

        public void visit(int version, int access, String name,
                          String signature, String superName, String[] interfaces) {
            className = name;
            isScoped = (access & Opcodes.ACC_PUBLIC) != 0;
            isAnnotated = false;
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            isAnnotated |= annotations.contains(desc);
            return null;
        }

        public void visitInnerClass(String name, String outerName,
                                    String innerName, int access) {
            // If the name of the class that was visited is equal
            // to the name of this visited inner class then
            // this access field needs to be used for checking the scope
            // of the inner class
            if (className.equals(name)) {
                isScoped = (access & Opcodes.ACC_PUBLIC) != 0;

                // Inner classes need to be statically scoped
                isScoped &= (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
            }
        }

        public void visitEnd() {
            if (isScoped && isAnnotated) {
                // Correctly scoped and annotated
                // add to the set of matching classes.
                classes.add(getClassForName(className.replaceAll("/", ".")));
            }
        }

        public void visitOuterClass(String string, String string0,
                                    String string1) {
            // Do nothing
        }

        public FieldVisitor visitField(int i, String string,
                                       String string0, String string1,
                                       Object object) {
            // Do nothing
            return null;
        }

        public void visitSource(String string, String string0) {
            // Do nothing
        }

        public void visitAttribute(Attribute attribute) {
            // Do nothing
        }

        public MethodVisitor visitMethod(int i, String string,
                                         String string0, String string1,
                                         String[] string2) {
            // Do nothing
            return null;
        }

        private Class getClassForName(String className) {
            try {
                final OsgiRegistry osgiRegistry = ReflectionHelper.getOsgiRegistryInstance();

                if (osgiRegistry != null) {
                    return osgiRegistry.classForNameWithException(className);
                } else {
                    return AccessController.doPrivileged(ReflectionHelper.classForNameWithExceptionPEA(className, classloader));
                }
            } catch (ClassNotFoundException ex) {
                String s = "A class file of the class name, " +
                        className +
                        "is identified but the class could not be found";
                throw new RuntimeException(s, ex);
            } catch (PrivilegedActionException pae) {
                final Throwable cause = pae.getCause();
                if (cause instanceof ClassNotFoundException) {
                    String s = "A class file of the class name, " +
                        className +
                        "is identified but the class could not be found";
                    throw new RuntimeException(s, cause);
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }else {
                    throw new RuntimeException(cause);
                }
            }
        }

    }
}
