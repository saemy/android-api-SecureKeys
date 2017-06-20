# SecureKeys

[![CircleCI](https://circleci.com/gh/saantiaguilera/android-api-SecureKeys/tree/develop.svg?style=svg)](https://circleci.com/gh/saantiaguilera/android-api-SecureKeys/tree/develop) [![Download](https://api.bintray.com/packages/saantiaguilera/maven/com.saantiaguilera.securekeys.core/images/download.svg) ](https://bintray.com/saantiaguilera/maven/com.saantiaguilera.securekeys.core/_latestVersion)

A tiny lib (Less than 10 methods) to store constants where attackers will have a harder time to find.


### Size

- aar: Neglegible (1 class of 7 methods)
- native libraries: 88kb the `.so` for each ABI (If no abi split, around 500kb)

### Requirements

SecureKeys can be included in any Android application.

SecureKeys supports Android 2.3 (Gingerbread / ApiLevel 9) and later.

### Description

This library uses an annotationProcessor to store the constants in a new file (where the constants are encrypted), and via JNI it will later retrieve them decoding them inside the `.so` file.

This way the attackers cant know the encoding system (because its inside the annotation processor), neither the decoding. 

**Note:** They can still "find" the class with the crypted constants or do a heapdump of the map inside the `.so` file. But since its encrypted they will have a (way too much) harder time figuring the constants out.

### Relevant notes

- The annotations used for the processor are removed in compile time, so they wont be shipped to the apk :)
- The generated class by the apt will be shipped inside your apk, but all the constants will be already encrypted. (attacker could still do a heapdump to know the encrypted constants or read that file)
- Current encryption system is AES (CBC + Padding5) + Base64. AES key and vector are private and local to the repository (planning to make them customizable)

### Usage

Add in your `build.gradle`:

```groovy
compile "com.saantiaguilera.securekeys:core:<latest_version>"
apt "com.saantiaguilera.securekeys:processor:<latest_version>"
```

For knowing the `<latest_version>` please check [Bintray](https://bintray.com/saantiaguilera/maven/com.saantiaguilera.securekeys.core) / the badge / GH releases

Annotate secure stuff wherever you like as:

```Java
@SecureKeys({
    @SecureKey(key = "client_secret", value = "my_client_secret..."),
    @SecureKey(key = "another_one_here", value = "...")
})
class MyClass {
  
  @SecureKey(key = "or_here_a_single_one", value = "...")
  public void myMethod() {}
  
}
```
This annotations wont be shipped with the apk, so fear not my friend :)

Possible places for annotating are:
- Classes
- Constructors
- Fields
- Methods

Thats all. Whenever you plan on using them simply call one of:
```Java
SecureEnvironment.getString("client_secret");
SecureEnvironment.getLong("crash_tracking_system_user_id");
SecureEnvironment.getDouble("time_for_destroying_the_world");
```

### Code generation

Generated code for this 2 annotations:
```Java
@SecureKey(key = "client-secret", value = "aD98E2GEk23TReYds9Zs9zdSdDBi23EAsdq29fXkpsDwp0W+h")
@SecureKey(key = "key22", value = "value2")
```
Will look like this:
```Java
   ...
   L1
    LINENUMBER 8 L1
    ALOAD 0
    // This string is "client-secret"
    LDC "fdce8e4a65b70d186bd77cba2e0c580dcf1c6497da9f1b70eed849497e1f8ba2"
    // This string is the value of "client-secret"
    LDC "jUvAlWYtbJJXOB5PWy1NMsgtAjOcBYdZpSgWcvBjnfwXtmyCsMFnPHeM4CrLdYPO2xmk2IAnOGhlsVn55eV6wA=="
    INVOKEVIRTUAL java/util/HashMap.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    POP
   L2
   ...
```

### Proguard

Currently the library supports transitively Proguard, so just by adding it you should be safe :)

### Benchmarks

Benchmark was ran on a Samsung Fame Lite (pretty old phone):
 * Android 4.1.2
 * CPU 1Ghz Single-Core ARM Cortex-A9
 * 512MB RAM
 * ISA ARMv7
 
There were 5000 different keys encoded, and was tested 100 times the retrieval of a key of various lengths. Which key doesnt matter, since lookup is O(1):

**Time to retrieve a key of length 1** (ms): 2

**Time to retrieve a key of length 10** (ms): 2

**Time to retrieve a key of length 50** (ms): 2

**Time to retrieve a key of length 5000** (ms): 4

### Contributing

Fork and submit a PR!

Modules:
- annotation: Provides annotations that are used by the processor
- core: The interaction of the user. Decrypts are done using C++, a java bridge is used for asking them
- processor: Custom APT that handles the annotations and produces crypted key/values (that can be handled by the c++ lib)
- testapp: Test application for testing all of the above

Relevant notes for developing it:
- JNI/Java bridge Tests are not supported by the platform so a "proxy" was created for giving it compatibility. Since this is not crucial for the project, it only works from the IDE, not from console (I should add all the classpaths dynamically before running the JUnit Starter. **Please ensure ALL the tests of the `:core` module pass from the Android Studio IDE**
- JNI Tests are not supported out of the box, so there are no tests for it. A PR is welcome adding them (using cppunit or some tool ofc)

### Future roadmap:
- [ ] Let the consumer set their own AES key (this is tricky, key shouldnt be exposed to APK but should be visible for apt AND JNI), maybe defer the `.so` compilation to the application and ship in the aar `.cpp/.h` classes?
- [ ] Add cppunit for testing c++ classes
- [ ] Let the dev change the filename where the constants are stored
