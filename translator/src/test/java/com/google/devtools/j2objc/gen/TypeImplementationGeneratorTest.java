/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.gen;

import com.google.devtools.j2objc.GenerationTest;
import com.google.devtools.j2objc.Options;

import java.io.IOException;

/**
 * Tests for {@link TypeImplementationGenerator}.
 *
 * @author Keith Stanger
 */
public class TypeImplementationGeneratorTest extends GenerationTest {

  public void testFieldAnnotationMethodForAnnotationType() throws IOException {
    String translation = translateSourceFile(
        "import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) "
        + "@interface A { @Deprecated int I = 5; }", "A", "A.m");
    assertTranslatedLines(translation,
        "+ (IOSObjectArray *)__annotations_I_ {",
        "  return [IOSObjectArray arrayWithObjects:(id[]) { "
          + "[[[JavaLangDeprecated alloc] init] autorelease] } count:1 "
          + "type:JavaLangAnnotationAnnotation_class_()];",
        "}");
  }

  public void testFieldAnnotationMethodForInterfaceType() throws IOException {
    String translation = translateSourceFile(
        "interface I { @Deprecated int I = 5; }", "I", "I.m");
    assertTranslatedLines(translation,
        "+ (IOSObjectArray *)__annotations_I_ {",
        "  return [IOSObjectArray arrayWithObjects:(id[]) { "
          + "[[[JavaLangDeprecated alloc] init] autorelease] } count:1 "
          + "type:JavaLangAnnotationAnnotation_class_()];",
        "}");
  }

  public void testFunctionLineNumbers() throws IOException {
    Options.setEmitLineDirectives(true);
    String translation = translateSourceFile("class A {\n\n"
        + "  static void test() {\n"
        + "    System.out.println(A.class);\n"
        + "  }}", "A", "A.m");
    assertTranslatedLines(translation,
        "#line 3", "void A_test() {", "A_initialize();", "", "#line 4",
        "[((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out_))) "
          + "printlnWithId:A_class_()];");
  }

  // Regression for non-static constants used in switch statements.
  // https://github.com/google/j2objc/issues/492
  public void testNonStaticPrimitiveConstant() throws IOException {
    String translation = translateSourceFile(
        "class Test { final int I = 1; void test(int i) { switch(i) { case I: return; } } }",
        "Test", "Test.h");
    assertTranslation(translation, "#define Test_I 1");
    translation = getTranslatedFile("Test.m");
    assertTranslation(translation, "case Test_I:");
  }

  public void testDesignatedInitializer() throws IOException {
    String translation = translateSourceFile(
        "class Test extends Number { Test(int i) {} public double doubleValue() { return 0; } "
        + " public float floatValue() { return 0; } public int intValue() { return 0; } "
        + " public long longValue() { return 0; }}", "Test", "Test.m");
    assertTranslatedLines(translation,
        "#pragma clang diagnostic push",
        "#pragma clang diagnostic ignored \"-Wobjc-designated-initializers\"",
        "- (instancetype)initWithInt:(jint)i {",
        "  Test_initWithInt_(self, i);",
        "  return self;",
        "}",
        "#pragma clang diagnostic pop");
  }

  // Verify that accessor methods for static vars and constants are generated on request.
  public void testStaticFieldAccessorMethods() throws IOException {
    Options.setStaticAccessorMethods(true);
    String source = "class Test { "
        + "static String ID; "
        + "private static int i; "
        + "public static final int VERSION = 1; "
        + "static final Test DEFAULT = new Test(); }";
    String translation = translateSourceFile(source, "Test", "Test.m");
    assertTranslatedLines(translation, "+ (NSString *)ID {", "return Test_ID_;");
    assertTranslatedLines(translation, "+ (void)setID:(NSString *)value {", "Test_set_ID_(value);");
    assertTranslatedLines(translation, "+ (jint)VERSION {", "return Test_VERSION;");
    assertTranslatedLines(translation, "+ (Test *)DEFAULT {", "return Test_DEFAULT_;");
    assertNotInTranslation(translation, "+ (void)setDEFAULT:(Test *)value"); // Read-only
    assertNotInTranslation(translation, "+ (jint)i");                        // Private
    assertNotInTranslation(translation, "+ (void)setI:(jint)value");         // Private
  }

  // Verify that accessor methods for static vars and constants aren't generated by default.
  public void testNoStaticFieldAccessorMethods() throws IOException {
    String source = "class Test { "
        + "static String ID; "
        + "private static int i; "
        + "public static final int VERSION = 1; "
        + "static final Test DEFAULT = new Test(); }";
    String translation = translateSourceFile(source, "Test", "Test.m");
    assertNotInTranslation(translation, "+ (NSString *)ID");
    assertNotInTranslation(translation, "+ (void)setID:(NSString *)value");
    assertNotInTranslation(translation, "+ (void)setI:(jint)value");
    assertNotInTranslation(translation, "+ (jint)VERSION");
    assertNotInTranslation(translation, "+ (Test *)DEFAULT");
  }

  // Verify that accessor methods for enum constants are generated on request.
  public void testEnumConstantAccessorMethods() throws IOException {
    Options.setStaticAccessorMethods(true);
    String source = "enum Test { ONE, TWO }";
    String translation = translateSourceFile(source, "Test", "Test.m");
    assertTranslatedLines(translation, "+ (TestEnum *)ONE {", "return TestEnum_ONE;");
    assertTranslatedLines(translation, "+ (TestEnum *)TWO {", "return TestEnum_TWO;");
  }

  // Verify that accessor methods for enum constants are not generated by default.
  public void testNoEnumConstantAccessorMethods() throws IOException {
    String source = "enum Test { ONE, TWO }";
    String translation = translateSourceFile(source, "Test", "Test.m");
    assertNotInTranslation(translation, "+ (TestEnum *)ONE");
    assertNotInTranslation(translation, "+ (TestEnum *)TWO");
  }
}
