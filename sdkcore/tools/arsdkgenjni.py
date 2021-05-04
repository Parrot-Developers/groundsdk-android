#!/usr/bin/env python

import sys, os
import errno
import arsdkparser
from common import *

#===============================================================================
# root file that includes all generated c files
#===============================================================================
def gen_root_jni(ctx, out):
    out.write("/** Generated, do not edit ! */\n")
    out.write("#include <stdio.h>\n")
    out.write("#include <string.h>\n")
    out.write("#include <jni.h>\n")
    out.write("#include <arsdk/arsdk.h>\n")
    for featureId in sorted(ctx.featuresById.keys()):
        featureobj = ctx.featuresById[featureId]
        out.write("#include \"%s\"\n", jni_file_name(featureobj.name))

#===============================================================================

def gen_jni_callback_cache(feature, cls, evts, out):
    full_name = feature.name + ("_" + cls.name if cls else "")
    java_cb_name = (
          "Lcom/parrot/drone/sdkcore/arsdk/"
        + java_feature_class_name(feature.name)
        + ("$" + cls.name if cls else "")
        + "$Callback;"
    )

    # callback methods id cache
    out.write("static struct {\n")
    for evt in sorted(evts, key=lambda evt: evt.cmdId):
        out.write("\tjmethodID jmid_%s;\n", evt.name)
    out.write("} s_cb_%s_cache;\n\n", full_name)
    # class init
    out.write("JNIEXPORT void JNICALL\n%s_nativeClassInit(JNIEnv *env, jclass jcls) {\n",
              jni_func_name(feature, cls))
    for evt in sorted(evts, key=lambda evt: evt.cmdId):
        out.write("\ts_cb_%s_cache.jmid_%s = (*env)->GetStaticMethodID(env, jcls, \"%s\", \"(%s%s)V\");\n",
                  full_name, evt.name, java_method_name(evt.name), java_cb_name, jni_method_signature(evt.args))
    out.write("}\n\n")

#===============================================================================

def gen_jni_decode(feature, cls, evts, out):
    full_name = feature.name + ("_" + cls.name if cls else "")
    for evt in sorted(evts, key=lambda evt: evt.cmdId):
        # static decode fn
        out.write("static int evt_%s_%s(JNIEnv *env, jclass clazz, struct arsdk_cmd *cmd, jobject callback) {\n", full_name, evt.name)
        for arg in evt.args:
            out.write("\t%s %s;\n", c_arg_type(arg, feature), arg.name)
        if evt.args:
            out.write("\tint res = arsdk_cmd_dec_%s_%s(cmd, %s);\n", c_name(full_name), c_name(evt.name),
                      ", ".join("&" + arg.name for arg in evt.args))
        else:
            out.write("\tint res = arsdk_cmd_dec_%s_%s(cmd);\n", c_name(full_name), c_name(evt.name))

        out.write("\tif (res < 0)\n\t\treturn res;\n")

        for arg in evt.args:
            if arg.argType == arsdkparser.ArArgType.BINARY:
                out.write("\tjbyteArray j_%s = (*env)->NewByteArray(env, %s.len);\n", arg.name, arg.name)
                out.write("\t(*env)->SetByteArrayRegion(env, j_%s, 0, %s.len, %s.cdata);\n", arg.name, arg.name, arg.name)

        out.write("\t(*env)->CallStaticVoidMethod(env, clazz, s_cb_%s_cache.jmid_%s, callback", full_name, evt.name)
        for arg in evt.args:
            out.write(", ")
            if arg.argType == arsdkparser.ArArgType.STRING:
                out.write("(*env)->NewStringUTF(env, %s)", arg.name)
            elif arg.argType == arsdkparser.ArArgType.BINARY:
                out.write("j_%s", arg.name)
            else:
                out.write("(%s)%s", jni_arg_type(arg), arg.name)
        out.write(");\n")
        out.write("\treturn 0;\n")
        out.write("}\n\n")

    # decode jni fn
    out.write("JNIEXPORT jint JNICALL\n%s_nativeDecode(JNIEnv *env, jclass jcls, jlong nativeCmd, jobject callback) {\n",
              jni_func_name(feature, cls))
    out.write("\tstruct arsdk_cmd *cmd = (struct arsdk_cmd *)(uintptr_t)nativeCmd;\n")
    out.write("\tif (cmd->prj_id != %d || cmd->cls_id != %d)\n\t\treturn -1;\n", feature.featureId,
              cls.classId if cls else 0)
    out.write("\tswitch(cmd->cmd_id) {\n")
    for evt in sorted(evts, key=lambda evt: evt.cmdId):
        out.write("\t\tcase %d: return evt_%s_%s(env, jcls, cmd, callback);\n", evt.cmdId, full_name, evt.name)
    out.write("\t}\n")
    out.write("\treturn -1;\n")

    out.write("}\n\n")

#===============================================================================

def gen_jni_encode(feature, cls, cmds, out):
    full_name = feature.name + ("_" + cls.name if cls else "")
    for cmd in sorted(cmds, key=lambda cmd: cmd.cmdId):
        out.write("JNIEXPORT jint JNICALL\n%s_nativeEncode%s(JNIEnv *env, jclass jcls, jlong nativeCmd",
                jni_func_name(feature, cls), jni_method_name(cmd.name))
        for arg in cmd.args:
            out.write(", %s %s", jni_arg_type(arg), arg.name)
        out.write(") {\n")

        out.write("\tstruct arsdk_cmd *cmd = (struct arsdk_cmd *)(uintptr_t)nativeCmd;\n")

        for arg in cmd.args:
            if arg.argType == arsdkparser.ArArgType.STRING:
                out.write("\tconst char* c_%s = (*env)->GetStringUTFChars(env, %s, NULL);\n", arg.name, arg.name)
            elif arg.argType == arsdkparser.ArArgType.BINARY:
                out.write("\tstruct arsdk_binary c_%s = {\n", arg.name)
                out.write("\t\t.len = (*env)->GetArrayLength(env, %s),\n", arg.name)
                out.write("\t\t.cdata = (*env)->GetPrimitiveArrayCritical(env, %s, NULL)\n", arg.name)
                out.write("\t};\n")

        out.write("\tint res = arsdk_cmd_enc_%s_%s(cmd", c_name(full_name), c_name(cmd.name))
        for arg in cmd.args:
            arg_prefix = ""
            if arg.argType == arsdkparser.ArArgType.STRING:
                arg_prefix = "c_"
            elif arg.argType == arsdkparser.ArArgType.BINARY:
                arg_prefix = "&c_"
            out.write(", %s%s", arg_prefix, arg.name)
        out.write(");\n")

        for arg in cmd.args:
            if arg.argType == arsdkparser.ArArgType.STRING:
                out.write("\tif (c_%s != NULL) (*env)->ReleaseStringUTFChars(env, %s, c_%s);\n",
                          arg.name, arg.name, arg.name)
            if arg.argType == arsdkparser.ArArgType.BINARY:
                out.write("\t(*env)->ReleasePrimitiveArrayCritical(env, %s, (void *) c_%s.cdata, JNI_ABORT);\n", arg.name, arg.name)

        out.write("\treturn res;\n")
        out.write("}\n\n")

#===============================================================================

def _evts(msgs):
    for msg in sorted(msgs, key=lambda msg: msg.cmdId):
        if isinstance(msg, arsdkparser.ArEvt):
            yield msg

def _cmds(msgs):
    for msg in sorted(msgs, key=lambda msg: msg.cmdId):
        if isinstance(msg, arsdkparser.ArCmd):
            yield msg

#===============================================================================

def gen_jni_feature(feature, cls, evts, cmds, out):
    if evts:
        # decode evts
        gen_jni_callback_cache(feature, cls, evts, out)
        gen_jni_decode(feature, cls, evts, out)
    if cmds:
        # encode cmds
        gen_jni_encode(feature, cls, cmds, out)

#===============================================================================

def gen_jni(feature, out):
    out.write("/** Generated, do not edit ! */\n\n")
    if feature.classes:
        # Project based (old)
        for clsId in sorted(feature.classesById.keys()):
            cls = feature.classesById[clsId]
            gen_jni_feature(feature, cls,
                [evt for evt in feature.evts if clsId == evt.cls.classId],
                [cmd for cmd in feature.cmds if clsId == cmd.cls.classId], out)
    else:
        # feature based (new)
        gen_jni_feature(feature, None, feature.evts, feature.cmds, out)

#===============================================================================
#===============================================================================
def list_files(ctx, outdir, extra):
    for featureId in sorted(ctx.featuresById.keys()):
        featureobj = ctx.featuresById[featureId]
        print(jni_file_name(featureobj.name))

#===============================================================================
#===============================================================================
def generate_files(ctx, outdir, extra):
    # main file that include all other files
    filepath = os.path.join(outdir, "arsdkgen.c")
    with open(filepath, "w") as fileobj:
        print("generating %s" % filepath)
        gen_root_jni(ctx, Writer(fileobj))

    # features
    for featureId in sorted(ctx.featuresById.keys()):
        featureobj = ctx.featuresById[featureId]
        filepath = os.path.join(outdir, jni_file_name(featureobj.name))
        try:
            os.makedirs(os.path.dirname(filepath))
        except OSError as exc:
            if exc.errno != errno.EEXIST:
                raise
        print("generating %s" % filepath)
        with open(filepath, "w") as fileobj:
            gen_jni(featureobj, Writer(fileobj))
