# The Flutter tooling requires that developers have CMake 3.10 or later
# installed. You should not increase this version, as doing so will cause
# the plugin to fail to compile for some customers of the plugin.
cmake_minimum_required(VERSION 3.10)

project(slr_input_kit_library VERSION 0.0.1 LANGUAGES C)

add_library(slr_input_kit SHARED
  "slr_input_kit.c"
)

set_target_properties(slr_input_kit PROPERTIES
  PUBLIC_HEADER slr_input_kit.h
  OUTPUT_NAME "slr_input_kit"
)

target_compile_definitions(slr_input_kit PUBLIC DART_SHARED_LIB)

if (ANDROID)
  # Support Android 15 16k page size
  target_link_options(slr_input_kit PRIVATE "-Wl,-z,max-page-size=16384")
endif()
