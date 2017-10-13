package com.u.securekeys.internal;

import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Builder class for writing a simple native header (*.h/*.hpp) file made of precompile
 * macro directives (#define)
 */
public class NativeHeaderBuilder {

    private List<String> imports;
    private List<String> defines;

    private String fileName;

    public NativeHeaderBuilder(String fileName) {
        imports = new ArrayList<>();
        defines = new ArrayList<>();

        this.fileName = fileName;
    }

    public void addImport(String libName) {
        imports.add("#include <" + libName + ">\n");
    }

    public void addDefine(String name, String value) {
        defines.add("#define " + name + " " + value + "\n");
    }

    public void writeTo(Writer writer) throws IOException {
        List<String> lines = flatFile();

        for (String line : lines) {
            writer.append(line);
        }

        if (!lines.isEmpty()) {
            writer.flush();
            writer.close();
        }
    }

    /**
     * Flattens the class builder into a list of strings, where each string is a
     * file line.
     * @return list of strings composing the whole .h/.hpp file
     */
    public List<String> flatFile() {
        List<String> lines =  new ArrayList<>();
        lines.add("// Created by SecureKeys Annotation Processor - Santiago Aguilera\n\n");
        lines.add("#ifndef SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");
        lines.add("#define SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");

        lines.add("\n");

        for (String line : imports) {
            lines.add(line);
        }

        lines.add("\n");

        for (String line : defines) {
            lines.add(line);
        }

        lines.add("\n");

        lines.add("#endif //SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");

        return lines;
    }

}