package com.u.securekeys;

import java.io.Writer;
import java.util.List;
import java.util.ArrayList;

public class NativeHeaderBuilder {

    private List<String> imports;
    private List<String> defines;

    private String fileName;

    public NativeHeaderBuilder(String fileName) {
        imports = new ArrayList<String>();
        defines = new ArrayList<String>();

        this.fileName = fileName;
    }

    public void addImport(String libName) {
        orderedLines.add("#include <" + libName + ">\n")
    }

    public void addDefine(String name, String value) {
        orderedLines.add("#define " + name + " " + value + "\n");
    }

    public void writeTo(Writer writer) {
        writer.append("// Created by Santiago Aguilera\n");
        writer.append("#ifndef SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");
        writer.append("#define SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");

        for (String line : imports) {
            writer.append(line);
        }

        for (String line : defines) {
            writer.append(line);
        }

        writer.append("#endif //SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n");

        writer.flush();
    }

}