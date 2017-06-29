package com.saantiaguilera.securekeys.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

/**
 * Created by saantiaguilera on 6/22/17.
 */
public class NativePackagerPlugin implements Plugin<Project> {

    private static final String TASK_EXTRACT_NATIVE_FILES = 'extractSecureKeysNativeFiles'
    private static final String TASK_PACKAGE_NATIVE_FILES = 'packageReleaseAar'

    private static final List<String> TASK_DEPENDANTS = [
            'generate',
            'compile',
            'assemble'
    ]

    private static final String SECUREKEYS_PACKAGE_NAME = 'com.saantiaguilera.securekeys/core'
    private static final String AAR_UNCOMPRESSED_ROOT_DESTINATION = 'build'
    private static final String AAR_GENERATED_FOLDER = 'secure-keys/include'

    private static final String FILE_CMAKE = 'main/cpp/CMakeLists.txt'
    private static final String FILE_BLOB_CPP = 'main/cpp/**/*.cpp'
    private static final String FILE_BLOB_H = 'main/cpp/**/*.h'
    private static final String FILE_BLOB_SO = '**/*.so'
    private static final String FILE_BLOB_ALL = '**'
    private static final String FILE_BLOB_EXTERNAL_HEADERS = 'main/cpp/**/extern_*.h'

    @Override
    void apply(Project project) {
        if (project.properties.nativeAarRepackage) {
            addNativeAarPackageTask(project)
        } else {
            // Add tasks for retrieveing native files from aar
            addNativeAarRetrievalTasks(project)
            // Add to the project the flags for building the native library
            addNativeBuildingSupport(project)
        }
    }

    def addNativeAarPackageTask(Project project) {
        def aarName = project.name
        def task = project.tasks.create TASK_PACKAGE_NATIVE_FILES, Zip
        task.baseName = aarName + "-release"
        task.extension = 'aar'
        task.destinationDir = project.file('build/secure-keys/') // Where to save the new aar.

        task.from project.zipTree("build/outputs/aar/" + aarName + "-release.aar")
        task.exclude(FILE_BLOB_SO) // Do not include shared libraries into final AAR
        task.exclude(FILE_BLOB_EXTERNAL_HEADERS) // Do not include extern_**.h classes to the final AAR
        task.from("src") {
            include(FILE_BLOB_H)
            include(FILE_BLOB_CPP)
            include(FILE_CMAKE)
            into(AAR_GENERATED_FOLDER)
        }
    }

    def addNativeBuildingSupport(Project project) {
        project.android.defaultConfig.externalNativeBuild {
            cmake {
                cppFlags "-std=c++11 -fexceptions"
            }
        }
        project.android.externalNativeBuild {
            cmake {
                path "${AAR_UNCOMPRESSED_ROOT_DESTINATION}/${AAR_GENERATED_FOLDER}/${FILE_CMAKE}"
            }
        }
    }

    def addNativeAarRetrievalTasks(Project project) {
        project.task(TASK_EXTRACT_NATIVE_FILES) {
            doLast {
                project.configurations.all*.each {
                    def file = it.absoluteFile
                    if (file.absolutePath.contains(SECUREKEYS_PACKAGE_NAME)) {
                        project.copy {
                            from project.zipTree(file)
                            into AAR_UNCOMPRESSED_ROOT_DESTINATION
                            include "${AAR_GENERATED_FOLDER}/${FILE_BLOB_ALL}"
                        }
                    }
                }
            }
        }

        project.tasks.build.dependsOn(TASK_EXTRACT_NATIVE_FILES)
        project.tasks.whenTaskAdded { task ->
            TASK_DEPENDANTS.each {
                if (task.name.startsWith(it)) {
                    task.dependsOn(TASK_EXTRACT_NATIVE_FILES)
                }
            }
        }
    }

}