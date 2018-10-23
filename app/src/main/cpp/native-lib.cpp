#include <jni.h>
#include <string>
#include <vector>

// Both of these throw 'file not found' errors so we have to find some other way.
//#include <filesystem>
//#include <experimental/filesystem>

#include <stdio.h>
#include <sys/types.h>
#include <dirent.h>
#include <unistd.h>

char path_buffer[1024];

static int list_dir( const char *dir_pathname, std::vector<const char*>& entries ) {
    DIR *dp;
    struct dirent *ep;

    //dp = opendir("/storage/emulated/0/");
    dp = opendir(dir_pathname);
    if(dp != NULL) {
        // readdir() returns a pointer to a dirent structure representing the next directory entry in the directory stream pointed to by the argument
        while(ep = readdir(dp))
            //puts(ep->d_name);
            entries.push_back(ep->d_name);
        (void) closedir (dp);
    } else {
        //perror("Couldn't open the directory");
        entries.push_back("Couldn't open the directory");

        getcwd(path_buffer,sizeof(path_buffer)-1);
        entries.push_back(path_buffer);
    }

    return 0;
}

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
    list_dir(dir_pathname,entries);

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
    list_dir(dir_pathname,entries);
    std::string buffer;
    for(auto& entry : entries) {
        buffer += entry;
        buffer += "\n";
    }
    return env->NewStringUTF(buffer.c_str());
}
