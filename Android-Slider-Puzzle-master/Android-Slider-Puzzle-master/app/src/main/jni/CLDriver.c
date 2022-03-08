#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <CL/opencl.h>
#define CL_FILE "/data/local/tmp/grid.cl"
#define LOG_TAG "DEBUG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define CHECK_CL(err) {\
    cl_int er = (err);\
    if(er<0 && er > -64){\
        LOGE("%d line, OpenCL Error:%d\n",__LINE__,er);\
    }\
}





JNIEXPORT jobject JNICALL Java_vivek_com_sliddingpuzzle_GameSetting_gridImage(JNIEnv *env,jclass class,jobject bitmap,jint size)
{
    int err = 0;

    LOGD("reading bitmap info...");
    AndroidBitmapInfo info;
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return NULL;
    }
    LOGD("width:%d height:%d stride:%d", info.width, info.height, info.stride);
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        return NULL;
    }

    LOGD("reading bitmap pixels...");
    void* bitmapPixels;
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return NULL;
    }
    uint32_t* src = (uint32_t*) bitmapPixels;
    uint32_t* tempPixels = (uint32_t*)malloc(info.height * info.width*4);
    int pixelsCount = info.height * info.width;
    memcpy(tempPixels, src, sizeof(uint32_t) * pixelsCount);
    LOGD("info.height : %d ",info.height);
    // OPNECL

    FILE *file_handle = fopen(CL_FILE, "r");
    char *kernel_file_buffer, *fle_log;
    size_t kernel_file_size, log_size;

    // Device input buffers
    cl_mem d_src;
    // Device output buffer
    cl_mem d_dst;
    cl_platform_id cpPlatform;        // OpenCL platform
    cl_device_id device_id;           // device ID
    cl_context context;               // context
    cl_command_queue queue;           // command queue
    cl_program program;               // program
    cl_kernel kernel;                 // kernel

    if (file_handle == NULL)
    {
        printf("Couldn't find the file");
        exit(1);
    }

    // read kernel file
    fseek(file_handle,0,SEEK_END);
    kernel_file_size = ftell(file_handle);
    rewind(file_handle);
    kernel_file_buffer = (char*)malloc(kernel_file_size+1) ;
    kernel_file_buffer[kernel_file_size] = '\0';
    fread(kernel_file_buffer,sizeof(char),kernel_file_size,file_handle);
    fclose(file_handle);
    LOGD("%s",kernel_file_buffer);
    LOGD("file_buffer_read");
    int i;

    size_t globalSize, localSize, grid;

    localSize = 100;
    grid = ((pixelsCount)%localSize)? ((pixelsCount / localSize)+1) : pixelsCount/localSize;
    globalSize = grid*localSize;
    LOGD("calc grid and globalSize");
    //
    CHECK_CL(clGetPlatformIDs(1,&cpPlatform,NULL));
    CHECK_CL(clGetDeviceIDs(cpPlatform,CL_DEVICE_TYPE_GPU,1,&device_id,NULL));
    int a =0;
    int b = (a<=1);
    context = clCreateContext(0,1,&device_id,NULL,NULL,&err);
    CHECK_CL(err);
    queue = clCreateCommandQueue(context,device_id,0,&err);
    CHECK_CL(err);
    // create program
    program = clCreateProgramWithSource(context,1,(const char**)&kernel_file_buffer,&kernel_file_size,&err);
    CHECK_CL(err);
    // Build program
    err = clBuildProgram(program,0,NULL,NULL,NULL,NULL);
    CHECK_CL(err);
    //create Device buffer
    kernel = clCreateKernel(program,"kernel_grid",&err);
    CHECK_CL(err);
    d_src = clCreateBuffer(context,CL_MEM_READ_ONLY,sizeof(uint32_t)*info.height*info.width,NULL,NULL);
    d_dst = clCreateBuffer(context,CL_MEM_WRITE_ONLY,sizeof(uint32_t)*info.height*info.width,NULL,NULL);

    // transfer host buffer to Device Buffer
    CHECK_CL(clEnqueueWriteBuffer(queue,d_src,CL_TRUE,0,sizeof(uint32_t)*info.height*info.width,tempPixels,0,NULL,NULL));
    // 커널의 매개변수를 입력
    CHECK_CL(clSetKernelArg(kernel,0,sizeof(cl_mem),&d_src));
    CHECK_CL(clSetKernelArg(kernel,1,sizeof(cl_mem),&d_dst));
    CHECK_CL(clSetKernelArg(kernel,2,sizeof(uint32_t),&info.width));
    CHECK_CL(clSetKernelArg(kernel,3,sizeof(uint32_t),&info.height));
    CHECK_CL(clSetKernelArg(kernel,4,sizeof(uint32_t),&size));
// 커널 수행
    CHECK_CL(clEnqueueNDRangeKernel(queue,kernel,1,NULL,&globalSize,&localSize,0,NULL,NULL));

    // 커널 수행 완료 대기
    CHECK_CL(clFinish(queue));

    // 디바이스 버퍼의 내용을 호스트에 입력
    CHECK_CL(clEnqueueReadBuffer(queue,d_dst,CL_TRUE,0,sizeof(uint32_t)*pixelsCount,src,0,NULL,NULL));
    // 메모리 반환
    clReleaseMemObject(d_src);
    clReleaseMemObject(d_dst);
    clReleaseProgram(program);
    clReleaseKernel(kernel);
    clReleaseCommandQueue(queue);
    clReleaseContext(context);

    AndroidBitmap_unlockPixels(env, bitmap);
    free(tempPixels);

    return bitmap;

}

