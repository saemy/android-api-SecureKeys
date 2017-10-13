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

@SupportedAnnotationTypes({ SecureKey.CLASSPATH, SecureKeys.CLASSPATH, SecureConfigurations.CLASSPATH, "*" })
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SecureKeysProcessor extends AbstractProcessor {

    /**
     * File output to generate, as this apt doesnt generate java code
     */
    private static final String FILE_NAME = "consts";
    private static final String FILE_FULL_NAME = "extern_consts.h";

    /**
     * The storage dir
     */
    private static final String FILE_PATH_BLOB = "build/secure-keys/include/main/cpp";

    /**
     * Builder class that will build the c++ header file
     */
    private NativeHeaderBuilder headerBuilder;

    /**
     * Encoder for storing mangled names / encripted values
     */
    private Encoder encoder;

    /**
     * List of the annotations to track and process
     */
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
            // Instantiate a header builder and add the required imports
            headerBuilder = new NativeHeaderBuilder(FILE_NAME);
            headerBuilder.addImport("map");
            headerBuilder.addImport("string");

            // Process the configuration annotations
            configure();
            // Process the keys
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

    /**
     * Since we need the absolute path, we will look for it from the pwd
     * @param root file that points to the pwd
     * @return a list of files matching the {@link #FILE_PATH_BLOB}
     */
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

    /**
     * Flattens the list of secure keys and the list of lists of secure keys into
     * a single list of secure keys
     * @param secureKeyElements list of securekeys
     * @param secureKeysElements list of lists of securekeys
     * @return a single list with the params flattened
     */
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

    /**
     * Parses the SecureKey annotations adding them to a single map as encoded constants
     *
     * Note that the header will only contain macros (precompiler #define directive), hence
     * this will just inline a function in the native code with the mapping.
     */
    private void addConstants() {
        String mapVariable = "_map";
        // Variable were we wil store the whole constant. Eg: load_map(var) var['asdf']="asdf";
        String defineValue;

        if (annotations.isEmpty()) {
            // Calling the method will do nothing, this will just be removed by the compiler
            defineValue = ";";
        } else {
            // Add a \ followed by a \n, this will make the file readable, nothing else.
            defineValue = "\\\n";

            // Iterate over the annotations, encode both key and value and add them to the map variable
            for (int i = 0; i < annotations.size(); i++) {
                SecureKey annotation = annotations.get(i);

                String key = Encoder.hash(annotation.key());
                String value = encoder.encode(annotation.value());

                defineValue += ("    " + mapVariable + "[\"" + key + "\"] = \"" + value + "\";");

                // Add another \ followed by a \n to make it readable (if its not the last element)
                if (i != annotations.size() - 1) {
                    defineValue += " \\\n";
                }
            }
        }

        // Add to the header builder the macro method + definition
        headerBuilder.addDefine("SECUREKEYS_LOAD_MAP(" + mapVariable + ")", defineValue);
    }

    /**
     * Processes the configure annotation, customizing necessary classes (eg the encoder aes keys)
     * and adding other information to the native header if needed by the ndk in runtime
     */
    private void configure() {
        // If more than one configure annotation is found, break the javac
        if (configurations.size() > 1) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "More than one SecureConfigurations found. Only one should be used");
            throw new IllegalStateException("More than one SecureConfigurations found. Only one should be used.");
        }

        try {
            Configurations nativeConfigurations = new Configurations();

            // Get the default values and create an encoder with them
            byte[] iv = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesInitialVector").getDefaultValue();
            byte[] key = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesKey").getDefaultValue();

            encoder = new Encoder(iv, key);

            // Set also the configurations DTO with the default values
            nativeConfigurations.setAesKey(key);
            nativeConfigurations.setAesVector(iv);
            nativeConfigurations.setHaltAdbOn((boolean) SecureConfigurations.class.getDeclaredMethod("blockIfADB").getDefaultValue());
            nativeConfigurations.setHaltNotSecure((boolean) SecureConfigurations.class.getDeclaredMethod("blockIfPhoneNotSecure").getDefaultValue());
            nativeConfigurations.setHaltDebuggable((boolean) SecureConfigurations.class.getDeclaredMethod("blockIfDebugging").getDefaultValue());
            nativeConfigurations.setHaltEmulator((boolean) SecureConfigurations.class.getDeclaredMethod("blockIfEmulator").getDefaultValue());
            nativeConfigurations.setCertificate((String) SecureConfigurations.class.getDeclaredMethod("certificateSignature").getDefaultValue());
            nativeConfigurations.setInstallers((String[]) SecureConfigurations.class.getDeclaredMethod("allowedInstallers").getDefaultValue());

            if (!configurations.isEmpty()) {
                // We have a configurations annotation, use it and start overwriting default values
                SecureConfigurations config = configurations.get(0);

                // If aes is set to random, it will have precedence over defined key/value fields
                if (config.useAesRandomly()) {
                    // If aes is randomized, hash the nanotime (which should have a way too high entropy)
                    // Get the seed and construct both key and iv from the hash (one from the start, the other from end)
                    // Since it is sha256, key and value cant have a same chunk of bytes
                    String seedString = Encoder.hash(String.valueOf(System.nanoTime()));
                    byte[] seed = seedString.getBytes(Charset.forName("UTF-8"));

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
                    // Use the configuration ones
                    encoder = new Encoder(config.aesInitialVector(), config.aesKey());

                    nativeConfigurations.setAesKey(config.aesKey());
                    nativeConfigurations.setAesVector(config.aesInitialVector());
                }

                // Overwrite values from the configuration
                nativeConfigurations.setHaltAdbOn(config.blockIfADB());
                nativeConfigurations.setHaltNotSecure(config.blockIfPhoneNotSecure());
                nativeConfigurations.setHaltDebuggable(config.blockIfDebugging());
                nativeConfigurations.setHaltEmulator(config.blockIfEmulator());
                nativeConfigurations.setInstallers(config.allowedInstallers());
                nativeConfigurations.setCertificate(config.certificateSignature());
            }

            // Write the native configurations into the header builder
            nativeConfigurations.writeTo(headerBuilder);
        } catch (Exception ex) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "This shouldnt happen. Please fill a issue with the stacktrace");
            throw new RuntimeException("This shouldnt happen. Please fill a issue with the stacktrace :)", ex);
        }
    }

}
