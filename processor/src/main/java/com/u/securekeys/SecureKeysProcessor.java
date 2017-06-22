package com.u.securekeys;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.u.securekeys.annotation.SecureConfigurations;
import com.u.securekeys.annotation.SecureKey;
import com.u.securekeys.annotation.SecureKeys;
import com.u.securekeys.internal.Encoder;
import com.u.securekeys.internal.Protocol;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({ SecureKey.CLASSPATH, SecureKeys.CLASSPATH, SecureConfigurations.CLASSPATH })
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SecureKeysProcessor extends AbstractProcessor {

    /**
     * Remember that the SecureKeys.java inside core references this class!
     */
    private static final String CLASS_NAME = "SCCache";
    private static final String CLASS_CLASSPATH = "android.util";

    private static final String MAP_VARIABLE_NAME = "_var";

    private static final int RANDOM_STRING_LENGTH_MIN = 30;
    private static final int RANDOM_STRING_LENGTH_MAX = 60;

    private Encoder encoder;

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment) {
        List<SecureKey> annotations = flattenElements(
            roundEnvironment.getElementsAnnotatedWith(SecureKey.class),
            roundEnvironment.getElementsAnnotatedWith(SecureKeys.class)
        );
        HashMap<String, String> resultMap = new HashMap<>();

        MethodSpec.Builder retrieveMethodBuilder = MethodSpec.methodBuilder("getElements")
            .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
            .returns(resultMap.getClass())
            .addStatement("java.util.HashMap<String, String> $L = new java.util.HashMap<String,String>()", MAP_VARIABLE_NAME);

        configure(roundEnvironment, retrieveMethodBuilder);

        for (SecureKey annotation : annotations) {
            String key = encoder.hash(annotation.key());
            String value = encoder.encode(annotation.value());

            addToMap(retrieveMethodBuilder, key, value);
        }

        retrieveMethodBuilder.addStatement("return $L", MAP_VARIABLE_NAME);

        TypeSpec createdClass = TypeSpec.classBuilder(CLASS_NAME)
            .addModifiers(Modifier.FINAL)
            .addMethod(retrieveMethodBuilder.build())
            .build();

        JavaFile javaFile = JavaFile.builder(CLASS_CLASSPATH, createdClass)
            .build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) { /* Silent. */ }

        return true;
    }

    private List<SecureKey> flattenElements(Set<? extends Element> secureKeyElements,
            Set<? extends Element> secureKeysElements) {
        List<SecureKey> result = new ArrayList<>();

        for (Element element : secureKeyElements) {
            result.add(element.getAnnotation(SecureKey.class));
        }

        for (Element element : secureKeysElements) {
            result.addAll(Arrays.asList(element.getAnnotation(SecureKeys.class).value()));
        }

        return result;
    }

    private void configure(final RoundEnvironment roundEnvironment, MethodSpec.Builder builder) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(SecureConfigurations.class);
        List<SecureConfigurations> configurations = new ArrayList<>();

        for (Element element : elements) {
            configurations.add(element.getAnnotation(SecureConfigurations.class));
        }

        if (configurations.size() > 1) {
            throw new IllegalStateException("More than one SecureConfigurations found. Only one can be used.");
        }

        try {
            if (!configurations.isEmpty()) {
                SecureConfigurations config = configurations.get(0);

                if (config.useAesRandomly()) {
                    String seedString = Encoder.hash(String.valueOf(System.nanoTime()));
                    byte[] seed = seedString.getBytes(Charset.forName("UTF-8"));
                    byte[] iv = new byte[16];
                    byte[] key = new byte[32];

                    // Create iv from start and key from end in reverse
                    for (int i = 0 ; i < 32 ; i++) {
                        if (i < 16) {
                            iv[i] = seed[i];
                        }
                        key[i] = seed[seed.length - i - 1];
                    }

                    encoder = new Encoder(iv, key);

                    addToMap(builder, Encoder.hash(Protocol.AES_RANDOM_SEED), seedString);
                } else {
                    encoder = new Encoder(config.aesInitialVector(), config.aesKey());

                    if (SecureConfigurations.class.getDeclaredMethod("aesKey").getDefaultValue() !=
                            config.aesKey()) {
                        addToMap(builder,
                            Encoder.hash(Protocol.AES_KEY),
                            Encoder.base64(config.aesKey()));
                    }

                    if (SecureConfigurations.class.getDeclaredMethod("aesInitialVector").getDefaultValue() !=
                            config.aesInitialVector()) {
                        addToMap(builder,
                            Encoder.hash(Protocol.AES_INITIAL_VECTOR),
                            Encoder.base64(config.aesInitialVector()));
                    }
                }

                if (config.blockIfADB()) {
                    addToMap(builder,
                        Encoder.hash(Protocol.HALT_IF_ADB_ON),
                        getSaltString() + "==");
                }

                if (config.blockIfDebugging()) {
                    addToMap(builder,
                        Encoder.hash(Protocol.HALT_IF_DEBUGGABLE),
                        getSaltString() + "==");
                }

                if (config.blockIfEmulator()) {
                    addToMap(builder,
                        Encoder.hash(Protocol.HALT_IF_EMULATOR),
                        getSaltString() + "==");
                }

                if (config.blockIfPhoneNotSecure()) {
                    addToMap(builder,
                        Encoder.hash(Protocol.HALT_IF_PHONE_NOT_SECURE),
                        getSaltString() + "==");
                }

            } else {
                byte[] iv = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesInitialVector").getDefaultValue();
                byte[] key = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesKey").getDefaultValue();
                encoder = new Encoder(iv, key);
            }
        } catch (Exception ex) {
            throw new RuntimeException("This shouldnt happen. Please fill a issue with the stacktrace :)", ex);
        }
    }

    private void addToMap(MethodSpec.Builder builder, String key, String value) {
        builder.addStatement("$L.put($S, $S)", MAP_VARIABLE_NAME, key, value);
    }

    private String getSaltString() {
        final int stringLength = ThreadLocalRandom.current().nextInt(RANDOM_STRING_LENGTH_MIN,
            RANDOM_STRING_LENGTH_MAX + 1);

        String saltChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder salt = new StringBuilder();
        while (salt.length() < stringLength) {
            int index = (int) (ThreadLocalRandom.current().nextFloat() * saltChars.length());
            salt.append(saltChars.charAt(index));
        }
        return salt.toString();
    }

}