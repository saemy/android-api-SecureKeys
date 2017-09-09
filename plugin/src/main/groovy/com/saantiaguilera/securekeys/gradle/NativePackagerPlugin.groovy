package com.saantiaguilera.securekeys.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

/**
 * Created by saantiaguilera on 6/22/17.
 */
class NativePackagerPlugin implements Plugin<Project> {

    private static final String AAR = 'aar'

    private static final String TASK_PACKAGE_NATIVE_FILES = 'packageNativeFilesToAar'

    private static final String SECUREKEYS_PACKAGE_NAME = 'com.saantiaguilera.securekeys/core'
    private static final String SECUREKEYS_PACKAGE_LOCAL_NAME = 'SecureKeys/testapp/libs/core'

    private static final String AAR_UNCOMPRESSED_ROOT_DESTINATION = 'build'
    private static final String AAR_GENERATED_FOLDER = 'secure-keys/include'

    private static final String FILE_CMAKE = 'main/cpp/CMakeLists.txt'
    private static final String FILE_BLOB_CPP = 'main/cpp/**/*.cpp'
    private static final String FILE_BLOB_H = 'main/cpp/**/*.h'
    private static final String FILE_BLOB_SO = '**/*.so'
    private static final String FILE_BLOB_ALL = '**'
    private static final String FILE_BLOB_EXTERNAL_HEADERS = 'main/cpp/**/extern_*.h'

    private static final String CPP_FLAGS = "-std=c++11 -fexceptions"

    @Override
    void apply(Project project) {
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

    def addNativeAarPackageTask(Project project, def variant) {
        def path = 'build/outputs/aar/'
        def newExt = "${AAR}.x"
        def task = project.tasks.create "${TASK_PACKAGE_NATIVE_FILES}${variant.capitalize()}", Zip
        task.baseName = "${project.name}-${variant}"
        task.extension = newExt
        task.destinationDir = project.file(path) // Where to save the new aar.

        task.from project.zipTree("${path}${project.name}-${variant}.${AAR}")
        task.exclude(FILE_BLOB_SO) // Do not include shared libraries into final AAR
        task.exclude(FILE_BLOB_EXTERNAL_HEADERS) // Do not include extern_**.h classes to the final AAR
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

    def addNativeBuildingSupport(Project project) {
        // This is because android studio sync wont trigger any gradle task,
        // Which means our native aar retrieval wont be run, and this will break it.
        if (!project.gradle.startParameter.taskNames.toListString().contentEquals("[]") &&
                !project.properties.dontCompileNdk) {
            project.android.defaultConfig.externalNativeBuild {
                cmake {
                    cppFlags CPP_FLAGS
                }
            }
            project.android.externalNativeBuild {
                cmake {
                    path "${AAR_UNCOMPRESSED_ROOT_DESTINATION}/${AAR_GENERATED_FOLDER}/${FILE_CMAKE}"
                }
            }
        }
    }

    def addNativeAarRetrieval(Project project) {
        project.afterEvaluate {
            project.configurations.findAll {
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
