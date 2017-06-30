package com.u.securekeys.internal;

import java.util.List;
import java.util.ArrayList;

import java.lang.reflect.Field;
import java.lang.Exception;

import org.junit.Test;
import org.junit.Assert;

public class ConfigurationsTest {

	@Test
	public void test_SetAesKey() {
		byte arr[] = new byte[] { 0x01, 0x02, 0x03, 0x04 };
		Configurations configurations = new Configurations();
		configurations.setAesKey(arr);

        try {
            Field field = Configurations.class.getDeclaredField("aesKey");
            field.setAccessible(true);
            byte[] key = (byte[]) field.get(configurations);

            Assert.assertEquals(arr, key);
        } catch (Exception e) {
        	Assert.fail(e.getMessage());
        }
	}

	@Test
	public void test_SetAesVector() {
		byte arr[] = new byte[] { 0x01, 0x02, 0x03, 0x04 };
		Configurations configurations = new Configurations();
		configurations.setAesVector(arr);

        try {
            Field field = Configurations.class.getDeclaredField("aesVector");
            field.setAccessible(true);
            byte[] vector = (byte[]) field.get(configurations);

            Assert.assertEquals(arr, vector);
        } catch (Exception e) {
        	Assert.fail(e.getMessage());
        }
	}

	@Test
	public void test_SetDebuggable() {
		Configurations configurations = new Configurations();
		configurations.setHaltDebuggable(true);

        try {
            Field field = Configurations.class.getDeclaredField("haltDebuggable");
            field.setAccessible(true);
            boolean haltDebuggable = field.getBoolean(configurations);

            Assert.assertEquals(true, haltDebuggable);
        } catch (Exception e) {
        	Assert.fail(e.getMessage());
        }
	}

	@Test
	public void test_SetADB() {
		Configurations configurations = new Configurations();
		configurations.setHaltAdbOn(true);

        try {
            Field field = Configurations.class.getDeclaredField("haltAdbOn");
            field.setAccessible(true);
            boolean haltAdbOn = field.getBoolean(configurations);

            Assert.assertEquals(true, haltAdbOn);
        } catch (Exception e) {
        	Assert.fail(e.getMessage());
        }
	}

	@Test
	public void test_SetEmulator() {
		Configurations configurations = new Configurations();
		configurations.setHaltEmulator(true);

        try {
            Field field = Configurations.class.getDeclaredField("haltEmulator");
            field.setAccessible(true);
            boolean haltEmulator = field.getBoolean(configurations);

            Assert.assertEquals(true, haltEmulator);
        } catch (Exception e) {
        	Assert.fail(e.getMessage());
        }
	}

	@Test
	public void test_SetNotSecure() {
		Configurations configurations = new Configurations();
		configurations.setHaltNotSecure(true);

        try {
            Field field = Configurations.class.getDeclaredField("haltNotSecure");
            field.setAccessible(true);
            boolean haltNotSecure = field.getBoolean(configurations);

            Assert.assertEquals(true, haltNotSecure);
        } catch (Exception e) {
        	Assert.fail(e.getMessage());
        }
	}

	@Test
	public void test_WritingNonDefault() {
		NativeHeaderBuilder builder = new NativeHeaderBuilder("test");
		Configurations configurations = new Configurations();

		configurations.setHaltDebuggable(true);
		configurations.setAesKey(new byte[] { 0x01, 0x03, 0x05 });
		configurations.setAesVector(new byte[] { 0x01, 0x03, 0x05 });

		configurations.writeTo(builder);

		List<String> lines = builder.flatFile();

		Assert.assertEquals(13, lines.size());
		Assert.assertTrue(lines.contains("#define SECUREKEYS_HALT_IF_DEBUGGABLE true\n"));
		Assert.assertTrue(lines.contains("#define SECUREKEYS_AES_KEY { 0x01, 0x03, 0x05 }\n"));
		Assert.assertTrue(lines.contains("#define SECUREKEYS_AES_INITIAL_VECTOR { 0x01, 0x03, 0x05 }\n"));
	}

	@Test
	public void test_WritingDefaults() {
		NativeHeaderBuilder builder = new NativeHeaderBuilder("test");
		Configurations configurations = new Configurations();

		configurations.setAesKey(new byte[] { 0x01, 0x03, 0x05 });
		configurations.setAesVector(new byte[] { 0x01, 0x03, 0x05 });

		configurations.writeTo(builder);

		List<String> lines = builder.flatFile();

		Assert.assertEquals(13, lines.size());
		Assert.assertTrue(lines.contains("#define SECUREKEYS_HALT_IF_DEBUGGABLE false\n"));
		Assert.assertTrue(lines.contains("#define SECUREKEYS_AES_INITIAL_VECTOR { 0x01, 0x03, 0x05 }\n"));
	}

	@Test
	public void test_NPEIfNoAesValues() {
		NativeHeaderBuilder builder = new NativeHeaderBuilder("test");
		Configurations configurations = new Configurations();

		try {
			configurations.writeTo(builder);
			Assert.fail("Shouldnt be here");
		} catch (NullPointerException e) {
			Assert.assertEquals("Missing aesKey/Vector in configurations. Please set them", e.getMessage());
		}
	}

}