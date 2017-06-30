package com.u.securekeys.internal;

import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

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
        writer.append("// Created by SecureKeys Annotation Processor - Santiago Aguilera\n\n");
        writer.append("#ifndef SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");
        writer.append("#define SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");

        writer.append("\n");

        for (String line : imports) {
            writer.append(line);
        }

        writer.append("\n");

        for (String line : defines) {
            writer.append(line);
        }

        writer.append("\n");

        writer.append("#endif //SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");

        writer.flush();
        writer.close();
    }

}