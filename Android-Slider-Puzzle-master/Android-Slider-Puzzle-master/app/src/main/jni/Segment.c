#include <jni.h>
#include <fcntl.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/time.h>
#include <jni.h>
#include <jni.h>

int fd = 0;


JNIEXPORT jint JNICALL
Java_vivek_com_sliddingpuzzle_MainActivity_openSegment(JNIEnv *env, jclass clazz, jstring path)
{
    jboolean iscopy;
    const char *path_utf = (*env) -> GetStringUTFChars(env,path, path_utf);
    fd = open(path_utf, O_WRONLY);
    (*env)->ReleaseStringUTFChars(env, path, path_utf);

    if (fd < 0) return -1;
    else return 1;
}

JNIEXPORT void JNICALL
Java_vivek_com_sliddingpuzzle_MainActivity_closeSegment(JNIEnv *env, jclass class)
{
if (fd>0) close(fd);
}

JNIEXPORT void JNICALL
Java_vivek_com_sliddingpuzzle_MainActivity_writeSegment(JNIEnv *env, jclass class, jbyteArray arr, jint count)
{
jbyte* chars = (*env)->GetByteArrayElements(env,arr,0);
if (fd>0) write(fd, (unsigned char*)chars, count);
(*env)->ReleaseByteArrayElements(env,arr,chars,0);
}