package com.u.securekeys.mocks;

import com.u.securekeys.annotation.SecureKey;
import com.u.securekeys.annotation.SecureKeys;

/**
 * Created by saguilera on 6/18/17.
 */
public class Mocks {

    /**
     * MOCK AN EMPTY CLASS
     */

    public static final String MOCK_EMPTY = "final class EmptyClass {}";

    /**
     * MOCK A CLASS WITH A SINGLE SECURE_KEY
     */

    public static final String MOCK_SECURE_KEY = "" +
        "@com.u.securekeys.annotation.SecureKey(key = \"key\", value = \"value\")\n" +
        "final class SingleKeyClass {}";
    public static final String MOCK_SECURE_KEY_GEN_FILE = "package android.util;\n" +
        "\n" +
        "import java.util.HashMap;\n" +
        "\n" +
        "final class SCCache {\n" +
        "  public static final HashMap getElements() {\n" +
        "    java.util.HashMap<String, String> resultMap = new java.util.HashMap<String,String>();\n" +
        "    resultMap.put(\"2c70e12b7a0646f92279f427c7b38e7334d8e5389cff167a1dc30e73f826b683\", \"2/lhAK3rkMJXwau5KsBvEA==\");\n" +
        "    return resultMap;\n" +
        "  }\n" +
        "}";

    /**
     * MOCK A CLASS WITH MORE THAN ONE SECURE_KEY
     */

    public static final String MOCK_SECURE_KEY_MULTIPLE = "" +
        "    @com.u.securekeys.annotation.SecureKey(key = \"key\", value = \"value\")\n" +
        "    final class MultipleKeyClass {\n" +
        "        @com.u.securekeys.annotation.SecureKey(key = \"another\", value = \"anothervalue\")\n" +
        "        private int field;\n" +
        "    }";
    public static final String MOCK_SECURE_KEY_MULTIPLE_GEN_FILE = "package android.util;\n" +
        "\n" +
        "import java.util.HashMap;\n" +
        "\n" +
        "final class SCCache {\n" +
        "  public static final HashMap getElements() {\n" +
        "    java.util.HashMap<String, String> resultMap = new java.util.HashMap<String,String>();\n" +
        "    resultMap.put(\"2c70e12b7a0646f92279f427c7b38e7334d8e5389cff167a1dc30e73f826b683\", \"2/lhAK3rkMJXwau5KsBvEA==\");\n" +
        "    resultMap.put(\"ae448ac86c4e8e4dec645729708ef41873ae79c6dff84eff73360989487f08e5\", \"J2jHPSzh8VORCgND0L9A5g==\");\n" +
        "    return resultMap;\n" +
        "  }\n" +
        "}";

}
