package com.u.securekeys.internal;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.Assert;

public class NativeHeaderBuilderTest {

	@Test
	public void test_OnlyCreatingBuilderOnlyPrintsHeader() {
		String fileName = "test";
		NativeHeaderBuilder builder = new NativeHeaderBuilder(fileName);
		List<String> lines = builder.flatFile();

		Assert.assertFalse(lines.isEmpty());
		Assert.assertEquals("// Created by SecureKeys Annotation Processor - Santiago Aguilera\n\n", lines.get(0));
		Assert.assertEquals("#ifndef SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n", lines.get(1));
		Assert.assertEquals("#define SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n", lines.get(2));
		Assert.assertEquals("\n", lines.get(3));
		Assert.assertEquals("\n", lines.get(4));
		Assert.assertEquals("\n", lines.get(5));
		Assert.assertEquals("#endif //SECUREKEYS_EXTERN_" + fileName.toUpperCase() + "_H\n", lines.get(6));
		Assert.assertEquals(7, lines.size());
	}

	@Test
	public void test_AddImport() {
		String fileName = "test";
		NativeHeaderBuilder builder = new NativeHeaderBuilder(fileName);
		builder.addImport("map");

		List<String> lines = builder.flatFile();

		Assert.assertFalse(lines.isEmpty());
		Assert.assertTrue(lines.contains("#include <map>\n"));
	}

	@Test
	public void test_AddDefine() {
		String fileName = "test";
		NativeHeaderBuilder builder = new NativeHeaderBuilder(fileName);
		builder.addDefine("TEST_CONSTANT", "12345");

		List<String> lines = builder.flatFile();

		Assert.assertFalse(lines.isEmpty());
		Assert.assertTrue(lines.contains("#define TEST_CONSTANT 12345\n"));
	}

}
