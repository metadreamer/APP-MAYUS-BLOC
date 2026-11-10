# Compila LAME (codificador MP3, licencia LGPL) como biblioteca aparte: libmayusmp3.so
cmake_minimum_required(VERSION 3.22.1)
project(mayusmp3 C)

file(GLOB LAME_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/lame/*.c)

add_library(mayusmp3 SHARED ${LAME_SOURCES} mp3_jni.c)
target_include_directories(mayusmp3 PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/lame)
target_compile_definitions(mayusmp3 PRIVATE STDC_HEADERS HAVE_STDINT_H HAVE_LIMITS_H ieee754_float32_t=float)
target_compile_options(mayusmp3 PRIVATE -O2 -w)
target_link_libraries(mayusmp3 m)
