package com.u.securekeys;

import com.u.securekeys.annotation.SecureConfigurations;
import com.u.securekeys.annotation.SecureKey;
import com.u.securekeys.annotation.SecureKeys;
import com.u.securekeys.internal.Configurations;
import com.u.securekeys.internal.Encoder;
import com.u.securekeys.internal.NativeHeaderBuilder;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({ SecureKey.CLASSPATH, SecureKeys.CLASSPATH, SecureConfigurations.CLASSPATH })
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SecureKeysProcessor extends AbstractProcessor {

    private static final String FILE_NAME = "consts";
    private static final String FILE_FULL_NAME = "extern_consts.h";

    private static final String FILE_PATH_BLOB = "build/secure-keys/include/main/cpp";

    private NativeHeaderBuilder headerBuilder;

    private Encoder encoder;

    private List<SecureKey> annotations;
    private List<SecureConfigurations> configurations;

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment) {
        // Initialize variables if they are null
        if (annotations == null) {
            annotations = new ArrayList<>();
        }

        if (configurations == null) {
            configurations = new ArrayList<>();
        }

        // Since the apt might make more than one pass, add them all without processing
        annotations.addAll(flattenElements(
            roundEnvironment.getElementsAnnotatedWith(SecureKey.class),
            roundEnvironment.getElementsAnnotatedWith(SecureKeys.class)
        ));
        
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(SecureConfigurations.class);
        for (Element element : elements) {
            configurations.add(element.getAnnotation(SecureConfigurations.class));
        }

        // Check if this will be the last processing pass
        if (roundEnvironment.processingOver()) {
            headerBuilder = new NativeHeaderBuilder(FILE_NAME);
            headerBuilder.addImport("map");
            headerBuilder.addImport("string");

            configure();
            addConstants();

            try {
                // Look for the directory we should drop the file into.
                List<File> files = findNativeFiles(new File("."));
                if (!files.isEmpty()) {
                    String path = files.get(0).getAbsolutePath();
                    headerBuilder.writeTo(new FileWriter(path + File.separatorChar + FILE_FULL_NAME));
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "No native files found for generating the full shared object. Maybe the plugin is missing?");
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Exception ocurred writing file: " + e.getMessage());
            }
        }

        return true;
    }

    public List<File> findNativeFiles(File root) {
        List<File> resultList = new ArrayList<>();

        File[] auxlist = root.listFiles();

        if (auxlist != null) {
            for (File file : auxlist) {
                if (file.isDirectory()) {
                    if (file.getAbsolutePath().contains(FILE_PATH_BLOB)) {
                        resultList.add(file);
                        return resultList;
                    } else {
                        resultList.addAll(findNativeFiles(file));

                        // We only need 1 sample, so as soon as we find one return
                        if (!resultList.isEmpty()) {
                            return resultList;
                        }
                    }
                }
            }
        }

        return resultList;
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

    private void addConstants() {
        String mapVariable = "_map";
        String defineValue;
        if (annotations.isEmpty()) {
            defineValue = ";";
        } else {
            defineValue = "\\\n";
            for (int i = 0; i < annotations.size(); i++) {
                SecureKey annotation = annotations.get(i);

                String key = Encoder.hash(annotation.key());
                String value = encoder.encode(annotation.value());

                defineValue += ("    " + mapVariable + "[\"" + key + "\"] = \"" + value + "\";");
                if (i != annotations.size() - 1) {
                    defineValue += " \\\n";
                }
            }
        }

        headerBuilder.addDefine("SECUREKEYS_LOAD_MAP(" + mapVariable + ")", defineValue);
    }

    private void configure() {
        if (configurations.size() > 1) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "More than one SecureConfigurations found. Only one should be used");
            throw new IllegalStateException("More than one SecureConfigurations found. Only one should be used.");
        }

        try {
            Configurations nativeConfigurations = new Configurations();

            byte[] iv = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesInitialVector").getDefaultValue();
            byte[] key = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesKey").getDefaultValue();

            encoder = new Encoder(iv, key);

            nativeConfigurations.setAesKey(key);
            nativeConfigurations.setAesVector(iv);
            nativeConfigurations.setHaltAdbOn((boolean) SecureConfigurations.class.getDeclaredMethod("blockIfADB").getDefaultValue());
            nativeConfigurations.setHaltNotSecure((boolean) SecureConfigurations.class.getDeclaredMethod("blockIfPhoneNotSecure").getDefaultValue());
            nativeConfigurations.setHaltDebuggable((boolean) SecureConfigurations.class.getDeclaredMethod("blockIfDebugging").getDefaultValue());
            nativeConfigurations.setHaltEmulator((boolean) SecureConfigurations.class.getDeclaredMethod("blockIfEmulator").getDefaultValue());

            if (!configurations.isEmpty()) {
                SecureConfigurations config = configurations.get(0);

                if (config.useAesRandomly()) {
                    String seedString = Encoder.hash(String.valueOf(System.nanoTime()));
                    byte[] seed = seedString.getBytes(Charset.forName("UTF-8"));

                    // Create key from end in reverse
                    for (int i = 0 ; i < 32 ; i++) {
                        key[i] = seed[seed.length - i - 1];
                    }

                    seedString = Encoder.hash(String.valueOf(System.nanoTime()));
                    seed = seedString.getBytes(Charset.forName("UTF-8"));

                    // Create iv from end in reverse
                    for (int i = 0 ; i < 16 ; i++) {
                        iv[i] = seed[seed.length - i - 1];
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

            nativeConfigurations.writeTo(headerBuilder);
        } catch (Exception ex) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "This shouldnt happen. Please fill a issue with the stacktrace");
            throw new RuntimeException("This shouldnt happen. Please fill a issue with the stacktrace :)", ex);
        }
    }

}
