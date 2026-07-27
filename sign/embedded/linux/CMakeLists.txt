# The Flutter tooling requires that developers have CMake 3.10 or later
# installed. Do not increase this version unless the minimum Flutter Linux
# toolchain requirement is also raised.
cmake_minimum_required(VERSION 3.10)

set(PROJECT_NAME "slr_input_kit")
project(${PROJECT_NAME} LANGUAGES CXX)

# Invoke the build for native code shared with the other target platforms.
add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/../src" "${CMAKE_CURRENT_BINARY_DIR}/shared")

# List of absolute paths to libraries that should be bundled with the plugin.
set(slr_input_kit_bundled_libraries
  $<TARGET_FILE:slr_input_kit>
  PARENT_SCOPE
)
