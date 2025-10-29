set(CMAKE_ANDROID_PAGE_SIZE 16384)
# Add this at the top of your CMakeLists.txt file
cmake_minimum_required(VERSION 3.22.1)
project(ed25519_wrapper)

# Add libsodium as an imported static library
add_library(
        libsodium_static
        STATIC
        IMPORTED
)

# This tells CMake where to find the static library file for each architecture
set_target_properties(
        libsodium_static
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libsodium.a
)

# This tells the compiler to look in the main libsodium-headers folder.
include_directories(${CMAKE_SOURCE_DIR}/libsodium-headers)

# Add your JNI library
add_library(
        ed25519_wrapper
        SHARED
        ed25519_jni.c
)

# Link your JNI library to the pre-built libsodium
target_link_libraries(
        ed25519_wrapper
        libsodium_static
)