package com.mongodb;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

/*
 * This class is experiment of javassist.
 * Last time it did not work as expected, because code m.setBody(methodBody); expects small code snippet instead
 * of whole method body.
 */
public class UnifiedTestTransformer implements ClassFileTransformer {

    private final String targetClassName;
    private final ClassLoader targetClassLoader;
    private final String testDescription;

    UnifiedTestTransformer(String targetClassName, ClassLoader classLoader, String testDescription) {
        this.targetClassName = targetClassName;
        this.targetClassLoader = classLoader;
        this.testDescription = testDescription;
    }

    private static void transformClass(
            String className, Instrumentation instrumentation, String testDescription) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        // see if we can get the class using forName
        try {
            targetCls = Class.forName(className);
            targetClassLoader = targetCls.getClassLoader();
            transform(targetCls, targetClassLoader, instrumentation, testDescription);
            return;
        } catch (Exception ex) {

        }
        // otherwise iterate all loaded classes and find what we want
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, instrumentation, testDescription);
                return;
            }
        }
        throw new RuntimeException("Failed to find class [" + className + "]" + Arrays.toString(instrumentation.getAllLoadedClasses()));
    }

    private static void transform(
            Class<?> clazz,
            ClassLoader classLoader,
            Instrumentation instrumentation, String testDescription) {
        UnifiedTestTransformer dt = new UnifiedTestTransformer(clazz.getName(), classLoader, testDescription);
        instrumentation.addTransformer(dt, true);
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Transform failed for: [" + clazz.getName() + "]", ex);
        }
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {
        byte[] byteCode = classfileBuffer;
        String finalTargetClassName = this.targetClassName
                .replaceAll("\\.", "/");
        if (!className.equals(finalTargetClassName)) {
            return byteCode;
        }

        if (className.equals(finalTargetClassName)
                && loader.equals(targetClassLoader)) {

            try {
                ClassPool cp = ClassPool.getDefault();
                CtClass cc = cp.get(targetClassName);
                CtMethod m = cc.getDeclaredMethod("getTestData");
                String methodBody = """
                        {
                                List<Object[]> data = new ArrayList<>();
                                for (File file : getTestFiles("/" + directory + "/")) {
                                    BsonDocument fileDocument = getTestDocument(file);
                                                
                                    for (BsonValue cur : fileDocument.getArray("tests")) {
                                        if(cur.asDocument().get("description").asString().getValue().equals(""" + testDescription + """
                                            )) {
                                            data.add(UnifiedTest.createTestData(fileDocument, cur.asDocument()));
                                        }
                                    }
                                }
                                return data;
                            }
                        """;
                m.setBody(methodBody);

                byteCode = cc.toBytecode();
                cc.detach();
            } catch (NotFoundException | CannotCompileException | IOException e) {
                throw new RuntimeException("Exception occurred while transforming class " + className, e);
            }
        }
        return byteCode;
    }
}