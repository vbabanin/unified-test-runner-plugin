package com.mongodb;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class Main {
    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    /**
     * Used for debugging purposes or development.
     */
    public static final Logger FILE_LOGGER = LogManager.getLogger("file");
    public static String expectedTestDescription;

    public static void premain(String arguments, Instrumentation instrumentation) {
        expectedTestDescription = arguments;
        LOGGER.info("Unified test runner agent started");
        setupTransformer(instrumentation);
    }

    private static void setupTransformer(final Instrumentation instrumentation) {
        new AgentBuilder.Default()
                //with(new LoggingListener())
                .type(ElementMatchers.named("com.mongodb.client.unified.UnifiedTest"))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> builder
                        .method(named("getTestData"))
                        .intercept(Advice.to(GetTestDataAdvice.class))
                )
                .type(ElementMatchers.named("com.mongodb.MongoClientSettings"))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> builder
                        .method(named("getCommandListeners"))
                        .intercept(Advice.to(GetCommandListenersAdvice.class))
                )
//                .type(ElementMatchers.hasSuperType(ElementMatchers.named("com.mongodb.event.CommandListener")))
//                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> builder
//                        .method(named("commandStarted"))
//                        .intercept(Advice.to(CommandStartedAdvice.class))
//                        .method(named("commandSucceeded"))
//                        .intercept(Advice.to(CommandSucceededAdvice.class))
//                        .method(named("commandFailed"))
//                        .intercept(Advice.to(CommandFailedAdvice.class)))
                .installOn(instrumentation);
    }


    public static class GetTestDataAdvice {
        @Advice.OnMethodExit
        public static void onExit(@Advice.Return(readOnly = false) Collection<Object[]> returned) {
            LOGGER.info("Agent intercepted com.mongodb.client.unified.UnifiedTest.getTestData()");
            Iterator<Object[]> iterator = returned.iterator();
            while (iterator.hasNext()) {
                Object[] objects = iterator.next();
                String testDescription = (String) objects[1];
                if (!testDescription.equals(expectedTestDescription)) {
                    iterator.remove();
                }
            }
        }
    }

    public static class GetCommandListenersAdvice {

        public static final Map<Object, CommandListener> COMMAND_LISTENER_CACHE = new ConcurrentHashMap<>();

        @Advice.OnMethodExit
        public static void onExit(@Advice.This Object settings,
                                  @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) List<CommandListener> listeners) {
            LOGGER.info("Agent intercepted com.mongodb.MongoClientSettings.getCommandListeners(). Injecting command listener.");
            List<CommandListener> newListeners = new ArrayList<>(listeners);

            MongoClientSettings mongoClientSettings = (MongoClientSettings) settings;
            if (!COMMAND_LISTENER_CACHE.containsKey(settings)) {
                CommandListener loggingListener = new CommandListener() {
                    @Override
                    public void commandStarted(final CommandStartedEvent event) {
                        LOGGER.info("Client id: {}\nCommand Started:\n  Command Name: {}\n  Command Body: {}\n  Connection Description: {}",
                                mongoClientSettings.getApplicationName(),
                                event.getCommandName(),
                                event.getCommand(),
                                event.getConnectionDescription());
                    }

                    @Override
                    public void commandSucceeded(final CommandSucceededEvent event) {
                        LOGGER.info(
                                "Client id:{}\nCommand Succeeded:\n  Command Name: {}\n  Response Body: {}\n  Connection Description: {}\n  Connection elapsed ms: {}",
                                mongoClientSettings.getApplicationName(),
                                event.getCommandName(),
                                event.getResponse(),
                                event.getConnectionDescription(),
                                event.getElapsedTime(TimeUnit.MILLISECONDS));
                    }

                    @Override
                    public void commandFailed(final CommandFailedEvent event) {
                        LOGGER.info("Client id:{}\nCommand Failed:\n  Command Name: {}\n  Exception: {}\n  Connection Description: {}\n  Connection elapsed ms: {}",
                                mongoClientSettings.getApplicationName(),
                                event.getCommandName(),
                                event.getThrowable().getMessage(),
                                event.getConnectionDescription(),
                                event.getElapsedTime(TimeUnit.MILLISECONDS));
                    }
                };
                COMMAND_LISTENER_CACHE.put(settings, loggingListener);
            }
            newListeners.add(COMMAND_LISTENER_CACHE.get(settings));
            listeners = newListeners;
        }
    }

//    public class CommandStartedAdvice {
//        @Advice.OnMethodEnter
//        public static void enter(@Advice.Argument(0) CommandStartedEvent event) {
//            LOGGER.info("Command Started:\n  Command Name: {}\n  Command Body: {}\n  Connection Description: {}",
//                    event.getCommandName(),
//                    event.getCommand(),
//                    event.getConnectionDescription());
//        }
//    }
//
//    public class CommandSucceededAdvice {
//        @Advice.OnMethodEnter
//        public static void enter(@Advice.Argument(0) CommandSucceededEvent event) {
//            LOGGER.info("Command Succeeded:\n  Command Name: {}\n  Response Body: {}\n  Connection elapsed ms: {}",
//                    event.getCommandName(),
//                    event.getResponse(),
//                    event.getElapsedTime(TimeUnit.MILLISECONDS));
//        }
//    }
//
//    public class CommandFailedAdvice {
//        @Advice.OnMethodEnter
//        public static void enter(@Advice.Argument(0) CommandFailedEvent event) {
//            LOGGER.info("Command Failed:\n  Command Name: {}\n  Exception: {}\n  Connection elapsed ms: {}",
//                    event.getCommandName(),
//                    event.getThrowable().getMessage(),
//                    event.getElapsedTime(TimeUnit.MILLISECONDS));
//        }
//    }


//    public static void premain(String testDescription, Instrumentation inst) throws IOException {
//        // JAVASIST
//        logger.info("Agent started");
//        String className = "com.mongodb.agent.TestClass";
//        transformClass(className, inst, testDescription);
//    }


    public static class LoggingListener implements AgentBuilder.Listener {

        @Override
        public void onIgnored(final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module,
                              final boolean loaded) {
            LOGGER.info("Ignored: " + typeDescription);
        }

        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
            LOGGER.info("Error: " + typeName, throwable);
        }

        @Override
        public void onComplete(final String typeName, final ClassLoader classLoader, final JavaModule module, final boolean loaded) {
            LOGGER.info("Complete: " + typeName);
        }

        @Override
        public void onDiscovery(final String typeName, final ClassLoader classLoader, final JavaModule module, final boolean loaded) {
            LOGGER.info("Discovery: " + typeName);
        }

        @Override
        public void onTransformation(final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module,
                                     final boolean loaded,
                                     final DynamicType dynamicType) {
            LOGGER.info("Transformation: " + typeDescription);
        }
    }
}