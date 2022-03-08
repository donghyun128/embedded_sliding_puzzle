
cmake_minimum_required(VERSION 3.10.2)

add_library(GLES_mali STATIC IMPORTED)

project("Android-Slider-Puzzle-master")

add_library(
        CLDriver

        SHARED

        CLDriver.c
)
add_library(
        ButtonDriver

        SHARED

        ButtonDriver.c)

add_library(
         Segment

         SHARED

         Segment.c)

add_library(
        LEDDriver

        SHARED

        LEDDriver.c
)
find_library(
        log-lib

        log)

find_library(
        jnigraphics-lib

        jnigraphics)

target_link_libraries(
        Segment

        ${log-lib})

target_link_libraries(
        ButtonDriver

        ${log-lib})
target_link_libraries(
        LEDDriver

        ${log-lib})
target_link_libraries(
        CLDriver

        "C:/Users/parlab/AppData/Local/Android/Sdk/ndk/21.4.7075529/toolchains/llvm/libGLES_mali.so" ${log-lib} ${jnigraphics-lib})



target_include_directories(CLDriver PRIVATE src/main/cpp)
target_include_directories(ButtonDriver PRIVATE src/main/cpp)

