#include <jni.h>
#include <stdlib.h>
#include <cstring>
#include <vector>
#include <android/log.h>
#include "fpcalc_result.h"

#define LOGI(...)   __android_log_print((int)ANDROID_LOG_INFO, "CHROMAPRINT", __VA_ARGS__)

extern int fpcalc_main(FpcalcResult &result,int &argc, char **argv);

static jobject fromStruct(JNIEnv *env, jobject thiz, FpcalcResult &result) {

    jclass targetClass   = env->FindClass("com/rrtry/tagify/natives/FpCalcResult");
    jobject targetObject = env->AllocObject(targetClass);

    jfieldID successId = env->GetFieldID(targetClass, "success", "Z");
    jfieldID errorId   = env->GetFieldID(targetClass, "error", "Ljava/lang/String;");
    jfieldID fpId      = env->GetFieldID(targetClass, "fingerprint", "Ljava/lang/String;");

    env->SetBooleanField(targetObject, successId, (jboolean) result.success);

    jstring fingerprint = env->NewStringUTF(result.fingerprint.c_str());
    env->SetObjectField(targetObject, fpId, fingerprint);

    jstring error = env->NewStringUTF(result.error.c_str());
    env->SetObjectField(targetObject, errorId, error);

    return targetObject;
}

std::string format(const char* format, ...) {

    va_list argptr;
    va_start(argptr, format);
    int size = std::vsnprintf(nullptr, 0, format, argptr);
    va_end(argptr);

    if (size < 0) {
        return "";
    }

    std::vector<char> buffer(size + 1);
    va_start(argptr, format);
    std::vsnprintf(buffer.data(), buffer.size(), format, argptr);
    va_end(argptr);

    return std::string(buffer.data());
}

extern "C"
JNIEXPORT jobject JNICALL fpcalc(JNIEnv *env, jobject thiz, jobjectArray args) {

    int argc    = env->GetArrayLength(args) + 1;
    char** argv = new char*[argc];

    argv[0] = new char[7];
    strcpy(argv[0], "fpCalc");

    FpcalcResult result = { false, "", "" };
    for (int i = 1; i < argc; i++) {

        jstring obj = (jstring)env->GetObjectArrayElement(args, i - 1);
        const char *chars = env->GetStringUTFChars(obj, 0);

        argv[i] = new char[strlen(chars) + 1];
        strcpy(argv[i], chars);

        env->ReleaseStringUTFChars(obj, chars);
        env->DeleteLocalRef(obj);
    }

    fpcalc_main(result, argc, argv);

    for (int i = 0; i < argc; i++) delete[] argv[i];
    delete[] argv;

    return fromStruct(env, thiz, result);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved __unused) {

    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass c = env->FindClass("com/rrtry/tagify/natives/FpCalc");
    if (c == nullptr) return JNI_ERR;

    static const JNINativeMethod methods[] = {
            { "exec", "([Ljava/lang/String;)Lcom/rrtry/tagify/natives/FpCalcResult;", reinterpret_cast<void *>(fpcalc) },
    };

    int rc = env->RegisterNatives(c, methods, sizeof(methods) / sizeof(JNINativeMethod));
    if (rc != JNI_OK) return rc;
    return JNI_VERSION_1_6;
}
