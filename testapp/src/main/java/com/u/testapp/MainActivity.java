package com.u.testapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.u.securekeys.SecureEnvironment;
import com.u.securekeys.annotation.SecureConfigurations;
import com.u.securekeys.annotation.SecureKey;
import com.u.securekeys.annotation.SecureKeys;
import junit.framework.Assert;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.util.Log;
import java.lang.Exception;

/**
 * For having configurations, we could add here:
 \@SecureConfigurations() {
    useAesRandomly = bool,
 //   aesKey = { 32 length byte array }
 //   aesInitialVector = { 16 length byte array }, // if you want to use specials like 0xca, cast it to byte! "(byte) 0xca"
    aesInitialVector = { 0x0f, 0x0e, 0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00 },
    aesKey = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x00,
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x00,
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x00,
        0x01, 0x02 },
    blockIfDebugging = bool,
    blockIfEmulator = bool,
    blockIfABD = bool,
    blockIfPhoneNotSecure = bool
 }
 */
@SecureConfigurations(
    useAesRandomly = true,
    certificateSignature = "1501784074"
)
@SecureKeys({
    @SecureKey(key = "a", value = "e"),
    @SecureKey(key = "b", value = "f"),
    @SecureKey(key = "c", value = "g"),
    @SecureKey(key = "d", value = "h"),
    @SecureKey(key = "long_from_BuildConfig", value = BuildConfig.TESTING_VALUE_1),
    @SecureKey(key = "double_from_BuildConfig", value = BuildConfig.TESTING_VALUE_2)
})
public class MainActivity extends AppCompatActivity {

    boolean initialized = false;

    @Override
    @SecureKey(key = "client-secret", value = "aD98E2GEk23TReYds9Zs9zdSdDBi23EAsdq29fXkpsDwp0W+h")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!initialized) {
            SecureEnvironment.initialize(this);
            initialized = true;
        }

        setContentView(R.layout.activity_main);

        Assert.assertEquals("aD98E2GEk23TReYds9Zs9zdSdDBi23EAsdq29fXkpsDwp0W+h", SecureEnvironment.getString("client-secret"));
        Assert.assertEquals("e", SecureEnvironment.getString("a"));
        Assert.assertEquals("f", SecureEnvironment.getString("b"));
        Assert.assertEquals("g", SecureEnvironment.getString("c"));
        Assert.assertEquals("h", SecureEnvironment.getString("d"));
        Assert.assertEquals(Long.valueOf(BuildConfig.TESTING_VALUE_1), Long.valueOf(SecureEnvironment.getLong("long_from_BuildConfig")));
        Assert.assertEquals(Double.valueOf(BuildConfig.TESTING_VALUE_2), SecureEnvironment.getDouble("double_from_BuildConfig"));

        ((TextView) findViewById(R.id.activity_main_key)).setText(SecureEnvironment.getString("client-secret"));
    }

}
