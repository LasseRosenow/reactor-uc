
package org.lflang.generator.uc

import org.lflang.FileConfig
import org.lflang.target.TargetConfig
import org.lflang.generator.PrependOperator
import org.lflang.joinWithLn
import org.lflang.target.property.BuildTypeProperty
import org.lflang.target.property.PlatformProperty
import org.lflang.target.property.type.PlatformType
import org.lflang.toUnixString
import org.lflang.unreachable
import java.nio.file.Path

class UcCmakeGenerator(private val targetConfig: TargetConfig, private val fileConfig: FileConfig) {
    private val S = '$' // a little trick to escape the dollar sign with $S
    private val platform = targetConfig.get(PlatformProperty.INSTANCE).platform
    private val platformName = if (platform == PlatformType.Platform.AUTO) "POSIX" else platform.name.uppercase()

    fun generatePlatformSpecific() = with(PrependOperator) {
        if (platform == PlatformType.Platform.AUTO) {
            """
                |# For POSIX we directly generate an executable.
                |# and install it to the bin directory.
                |add_executable($S{LF_MAIN_TARGET} $S{SOURCES})
                |install(TARGETS $S{LF_MAIN_TARGET}
                |        RUNTIME DESTINATION $S{CMAKE_INSTALL_BINDIR}
                |        OPTIONAL
                |)
            """.trimMargin()
        } else if (platform == PlatformType.Platform.ZEPHYR) {
            """
                |# For Zephyr we generate a library that can be included
                |# in a Zephyr project.
                |zephyr_library_named($S{LF_MAIN_TARGET})
                |zephyr_library_sources($S{SOURCES})
                |zephyr_library_link_libraries(kernel)
            """.trimMargin()
        } else {
            unreachable()
        }
    }
    fun generateCmake(sources: List<Path>) = with(PrependOperator) {
        """
            |cmake_minimum_required(VERSION 3.5)
            |project(${fileConfig.name} VERSION 0.0.0 LANGUAGES C)
            |set(PLATFORM $platformName CACHE STRING "Target platform")
            |
            |# Get the value of the environment variable
            |if(NOT DEFINED ENV{REACTOR_UC_PATH})
            |   message(FATAL_ERROR "Environment variable REACTOR_UC_PATH is not set. Please source the env.bash in reactor-uc.")
            |endif()
            |
            |set(LF_MAIN_TARGET ${fileConfig.name})
            |set(SOURCES
        ${" |    "..sources.joinWithLn { it.toUnixString() }}
            |)
        ${" |"..generatePlatformSpecific()}
            |
            |add_subdirectory(${S}ENV{REACTOR_UC_PATH} reactor-uc)
            |
            |target_include_directories($S{LF_MAIN_TARGET} PUBLIC $S{CMAKE_CURRENT_SOURCE_DIR})
            |target_link_libraries($S{LF_MAIN_TARGET} PUBLIC reactor-uc)
            |
        """.trimMargin()
    }
}
