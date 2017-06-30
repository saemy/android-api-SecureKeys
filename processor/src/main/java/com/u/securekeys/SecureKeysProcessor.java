package com.u.securekeys;

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
import javax.tools.FileObject;
import javax.tools.JavaFileManager;

@SupportedAnnotationTypes({ SecureKey.CLASSPATH, SecureKeys.CLASSPATH, SecureConfigurations.CLASSPATH })
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SecureKeysProcessor extends AbstractProcessor {

    private NativeHeaderBuilder headerBuilder;

    private Encoder encoder;

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment) {
        headerBuilder = new NativeHeaderBuilder("consts");
        headerBuilder.addImport("map");
        headerBuilder.addImport("string")

        List<SecureKey> annotations = flattenElements(
            roundEnvironment.getElementsAnnotatedWith(SecureKey.class),
            roundEnvironment.getElementsAnnotatedWith(SecureKeys.class)
        );

        configure(roundEnvironment);
        addConstants(annotations);

        try {
            FileObject nativeFile = processingEnv.getFiler().createResource(
                new JavaFileManager.Location() {

                    @Override
                    public String getName() {
                        return "build/secure-keys/include/main/cpp";
                    }

                    @Override
                    public boolean isOutputLocation()() {
                        return true;
                    }

                }, // Location
                "", // Package
                "extern_consts.h"
            );
            headerBuilder.writeTo(nativeFile.openWriter());
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

    private void addConstants(List<SecureKeys> annotations) {
        String mapVariable = "_map";
        String defineValue = "\\\n";
        for (int i = 0 ; i < annotations.size() ; i++) {
            SecureKeys annotation = annotations.get(i);

            String key = encoder.hash(annotation.key());
            String value = encoder.encode(annotation.value());

            defineValue += ("    " + mapVariable + "[\"" + key + "\"] = \"" + value + "\";");
            if (i != annotations.size() - 1) {
                defineValue += "\\\n";
            }
        }

        headerBuilder.addDefine("SECUREKEYS_LOAD_MAP(" + mapVariable + ")", defineValue);
    }

    private void configure(final RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(SecureConfigurations.class);
        List<SecureConfigurations> configurations = new ArrayList<>();

        for (Element element : elements) {
            configurations.add(element.getAnnotation(SecureConfigurations.class));
        }

        if (configurations.size() > 1) {
            throw new IllegalStateException("More than one SecureConfigurations found. Only one can be used.");
        }

        Configurations nativeConfigurations = new Configurations();

        byte[] iv = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesInitialVector").getDefaultValue();
        byte[] key = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesKey").getDefaultValue();

        encoder = new Encoder(iv, key);

        nativeConfigurations.setAesKey(key);
        nativeConfigurations.setAesVector(iv);
        nativeConfigurations.setHaltAdbOn(SecureConfigurations.class.getDeclaredMethod("blockIfADB").getDefaultValue());
        nativeConfigurations.setHaltNotSecure(SecureConfigurations.class.getDeclaredMethod("blockIfPhoneNotSecure").getDefaultValue());
        nativeConfigurations.setHaltDebuggable(SecureConfigurations.class.getDeclaredMethod("blockIfDebugging").getDefaultValue());
        nativeConfigurations.setHaltEmulator(SecureConfigurations.class.getDeclaredMethod("blockIfEmulator").getDefaultValue());

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

                    nativeConfigurations.setAesKey(key);
                    nativeConfigurations.setAesVector(iv);
                } else {
                    encoder = new Encoder(config.aesInitialVector(), config.aesKey());

                    nativeConfigurations.setAesKey(config.aesKey());
                    nativeConfigurations.setAesVector(config.aesInitialVector());
                }

                nativeConfigurations.setHaltAdbOn(config.blockIfADB());
                nativeConfigurations.setHaltNotSecure(config.blockIfPhoneNotSecure());
                nativeConfigurations.setHaltDebuggable(config.blockIfDebugging());
                nativeConfigurations.setHaltEmulator(config.blockIfEmulator());
            }
        } catch (Exception ex) {
            throw new RuntimeException("This shouldnt happen. Please fill a issue with the stacktrace :)", ex);
        }

        nativeConfigurations.writeTo(headerBuilder);
    }

}