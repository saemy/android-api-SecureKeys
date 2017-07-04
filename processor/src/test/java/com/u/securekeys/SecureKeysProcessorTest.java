package com.u.securekeys;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import com.u.securekeys.mocks.Mocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.Assert;
import org.junit.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

public class SecureKeysProcessorTest {

    @Test
    public void test_EmptyClassProcessedOk() {
        Compilation compilation = javac()
            .withProcessors(new SecureKeysProcessor())
            .compile(JavaFileObjects.forSourceString("EmptyClass", Mocks.MOCK_EMPTY));

        assertThat(compilation).succeeded();
        for (Diagnostic diagnostic : compilation.warnings()) {
            if (!diagnostic.getMessage(Locale.ENGLISH).contains("Supported source version 'RELEASE_7' from annotation processor") &&
                    !diagnostic.getMessage(Locale.ENGLISH).contains("No native files found for generating the full shared object. Maybe the plugin is missing?")) {
                Assert.fail("Warnings found in the compilation: " + diagnostic.getMessage(Locale.ENGLISH));
            }
        }
    }

    List<JavaFileObject> stripClasses(List<JavaFileObject> files) {
        List<JavaFileObject> newList = new ArrayList<>();
        for (JavaFileObject fileObject : files) {
            if (fileObject.getKind() != JavaFileObject.Kind.CLASS) {
                newList.add(fileObject);
            }
        }
        return newList;
    }

}
