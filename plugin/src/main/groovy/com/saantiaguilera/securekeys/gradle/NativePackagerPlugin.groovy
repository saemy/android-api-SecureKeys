package com.saantiaguilera.securekeys.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

/**
 * Created by saantiaguilera on 6/22/17.
 */
class NativePackagerPlugin implements Plugin<Project> {

    /**
     * Extension of the android archive package
     */
    private static final String AAR = 'aar'

    /**
     * Task name for developing (packages all the native files into the aar)
     */
    private static final String TASK_PACKAGE_NATIVE_FILES = 'packageNativeFilesToAar'

    /**
     * Package name and local name. The local one is added sine for testing here we wont add it via gradle
     * (we add it as a local library, check the value of it)
     */
    private static final String SECUREKEYS_PACKAGE_NAME = 'com.saantiaguilera.securekeys/core'
    private static final String SECUREKEYS_PACKAGE_LOCAL_NAME = 'SecureKeys/testapp/libs/core'

    /**
     * The root destination of the native files when the aar will be uncompressed
     */
    private static final String AAR_UNCOMPRESSED_ROOT_DESTINATION = 'build'

    /**
     * The generated folder name. Files will be placed in this path, inside the {@link #AAR_UNCOMPRESSED_ROOT_DESTINATION}}
     */
    private static final String AAR_GENERATED_FOLDER = 'secure-keys/include'

    /**
     * Native files information, for knowing what to pack/exclude/etc
     */
    private static final String FILE_CMAKE = 'main/cpp/CMakeLists.txt'
    private static final String FILE_BLOB_CPP = 'main/cpp/**/*.cpp'
    private static final String FILE_BLOB_H = 'main/cpp/**/*.h'
    private static final String FILE_BLOB_SO = '**/*.so'
    private static final String FILE_BLOB_ALL = '**'
    //private static final String FILE_BLOB_EXTERNAL_HEADERS = 'main/cpp/**/extern_*.h'

    /**
     * CPP Flags to add to the gradle build system
     */
    private static final HashMap<String, String> CPP_FLAGS = [
            std: "c++11",
            fexceptions: "NAN"
    ]

    @Override
    void apply(Project project) {
        // If the property is not found, set it to true as default so we always add the compilation automatically
        if (!project.extensions.hasProperty("autoCompileSecureKeys")) {
            project.extensions.autoCompileSecureKeys = true
        }

        // This property is just for us, its for packaging our native files inside the aar we will publish to bintray
        if (project.properties.nativeAarRepackage) {
            // Iterate the variants and add a task for repackaging the aar.
            // This should be use be me only.
            project.android.libraryVariants.all {
                addNativeAarPackageTask(project, it.name)
            }
        } else {
            // Add tasks for retrieveing native files from aar
            addNativeAarRetrieval(project)
            // Add to the project the flags for building the native library
            addNativeBuildingSupport(project)
        }
    }

    /**
     * Add for a variant the tasks that will remove the .so files from our publishable aar and add the
     * native files for making the developers library generate the .so from them
     * @param project ref
     * @param variant to add the task to
     */
    void addNativeAarPackageTask(Project project, def variant) {
        def path = 'build/outputs/aar/'
        def newExt = "${AAR}.x"
        def task = project.tasks.create "${TASK_PACKAGE_NATIVE_FILES}${variant.capitalize()}", Zip
        task.baseName = "${project.name}-${variant}"
        task.extension = newExt
        task.destinationDir = project.file(path) // Where to save the new aar.

        task.from project.zipTree("${path}${project.name}-${variant}.${AAR}")
        task.exclude(FILE_BLOB_SO) // Do not include shared libraries into final AAR
        //task.exclude(FILE_BLOB_EXTERNAL_HEADERS) // Do not include extern_**.h classes to the final AAR
        task.from("src") {
            include(FILE_BLOB_H)
            include(FILE_BLOB_CPP)
            include(FILE_CMAKE)
            into(AAR_GENERATED_FOLDER)
        }
        task.outputs.upToDateWhen { return false }

        project.tasks["assemble${variant.capitalize()}"].finalizedBy task
        
        task.doLast {
            // Zip tasks dont support overwrite, so we "support it"
            def oldFile = "${path}${project.name}-${variant}.${AAR}"
            def newFile = "${path}${project.name}-${variant}.${newExt}"

            project.file(oldFile).delete()
            project.file(newFile).renameTo(project.file(oldFile))
        }
    }

    /**
     * Adds to the developer project native building support of the native files (extracted from our aar).
     * @param project ref
     */
    void addNativeBuildingSupport(Project project) {
        // This is because android studio sync wont trigger any gradle task,
        // Which means our native aar retrieval wont be run, and this will break it.
        if (!project.gradle.startParameter.taskNames.toListString().contentEquals("[]") &&
                project.properties.autoCompileSecureKeys) {
            project.android.defaultConfig.externalNativeBuild {
                cmake {
                    String currentFlags = cppFlags ?: ""
                    CPP_FLAGS.each { key, value ->
                        if (!currentFlags.contains("-$key")) {
                            // We have to add the key, check if it has value or not
                            if (value.contentEquals('NAN')) {
                                currentFlags += " -$key"
                            } else {
                                currentFlags += " -$key=$value"
                            }
                        }
                    }
                    cppFlags currentFlags
                }
            }
            project.android.externalNativeBuild {
                cmake {
                    if (path) {
                        throw new GradleException("We have detected that your project already has a declared CMake. Please " +
                                "if you have multiple ndk projects in your module build a root CMake with both your lists and " +
                                "the securekeys one. \n" +
                                "More information at: https://developer.android.com/studio/projects/add-native-code.html - Include other CMake projects.\n" +
                                "The CMakeLists.txt from securekeys can be found in ${project.name}/build/secure-keys/include/main/cpp/CMakeLists.txt.\n" +
                                "Once this is done, please add the property autoCompileSecureKeys to the module.\n" +
                                "ext.autoCompileSecureKeys = false")
                    }
                    path "${AAR_UNCOMPRESSED_ROOT_DESTINATION}/${AAR_GENERATED_FOLDER}/${FILE_CMAKE}"
                }
            }
        }
    }

    /**
     * This task will extract all the native files
     * from our aar (if found) and place them somewhere the {@link #addNativeBuildingSupport} method can
     * later find
     * @param project ref
     */
    void addNativeAarRetrieval(Project project) {
        // We need to evaluate the project else we wont have all deps resolved
        project.afterEvaluate {
            project.configurations.findAll {
                // If gradle is 4.0 or later, we have to check that the dep is resolvable
                project.gradle.gradleVersion >= '4.0' ?
                        it.isCanBeResolved() :
                        true
            }.each { config ->
                config.files.each {
                    def file = it.absoluteFile
                    // Check if the file is of the package name.
                    // Also we check if its our local environment since we dont add it via gradle
                    if (file.absolutePath.contains(SECUREKEYS_PACKAGE_NAME) ||
                            file.absolutePath.contains(SECUREKEYS_PACKAGE_LOCAL_NAME)) {
                        // Copy all the native files from the aar into the project build
                        project.copy {
                            from project.zipTree(file)
                            into AAR_UNCOMPRESSED_ROOT_DESTINATION
                            include "${AAR_GENERATED_FOLDER}/${FILE_BLOB_ALL}"
                        }
                    }
                }
            }
        }
    }

}
