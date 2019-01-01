#include <jni.h>
#include <string>
#include <vector>

// Both of these throw 'file not found' errors so we have to find some other way.
//#include <filesystem>
//#include <experimental/filesystem>



//#include "StorageSelector.h"

char path_buffer[1024];

/**
 * Which properties should a listdir() function of a media player return
 * - Entry name (file name or directory name)
 * - information of whether it's a file or a directory
 */
extern "C" JNIEXPORT jstring JNICALL
Java_space_nyanko_nyankoapplication_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    //return env->NewStringUTF(hello.c_str());

    const char *dir_pathname = "/sdcard";
    std::vector<const char*> entries;
    const size_t reserve_amount = 16;
    entries.reserve(reserve_amount);
    //list_dir(dir_pathname,entries);

    std::string names;
    for(auto& entry : entries) {
        names += std::string(entry) + ", ";
    }
    return env->NewStringUTF(names.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_space_nyanko_nyankoapplication_MainActivity_listDir(
        JNIEnv *env,
        jobject /* this */) {

    const char *dir_pathname = "/sdcard";

    std::vector<const char*> entries;
    //list_dir(dir_pathname,entries);
    std::string buffer;
    for(auto& entry : entries) {
        buffer += entry;
        buffer += "\n";
    }
    return env->NewStringUTF(buffer.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_space_nyanko_nyankoapplication_MainActivity_moveToParentDirectory(
        JNIEnv *env,
        jobject /* this */) {

    //MovetoParentDirectory();

    return env->NewStringUTF("a");
}

extern "C" JNIEXPORT jstring JNICALL
Java_space_nyanko_nyankoapplication_MainActivity_getCurrentDirectoryEntries(
        JNIEnv *env,
        jobject /* this */) {

    return env->NewStringUTF("123");//GetCurrentDirectoryEntries().c_str());
}
