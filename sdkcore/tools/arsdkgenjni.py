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
    out.write("#include \"arsdkcore_multiset.h\"\n")
    for featureId in sorted(ctx.featuresById.keys()):
        featureobj = ctx.featuresById[featureId]
        out.write("#include \"%s\"\n", jni_file_name(featureobj.name))

#===============================================================================
# generique multi settings file
#===============================================================================

def _walk_msets(ctx):
    for featureId in sorted(ctx.featuresById.keys()):
        featureobj = ctx.featuresById[featureId]
        for mset in featureobj.multisets:
            yield mset

def gen_jni_multiset(ctx, out):
    out.write("/** Generated, do not edit ! */\n\n")
    out.write("#ifndef _ARSDKCORE_MULTISET_H_\n");
    out.write("#define _ARSDKCORE_MULTISET_H_\n\n");
    out.write("union arsdkcore_multiset {\n")
    for ftrId in sorted(ctx.featuresById.keys()):
        ftrobj = ctx.featuresById[ftrId]
        for mset in ftrobj.multisets:
            out.write("\tstruct arsdk_%s_%s %s;\n", ftrobj.name,
                    mset.name.lower(),
                    mset.name.lower())
    out.write("};\n\n")
    out.write("#endif // _ARSDKCORE_MULTISET_H_\n\n");

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
        out.write("\t(*env)->CallStaticVoidMethod(env, clazz, s_cb_%s_cache.jmid_%s, callback", full_name, evt.name)
        for arg in evt.args:
            out.write(", ")
            if arg.argType == arsdkparser.ArArgType.STRING:
                out.write("(*env)->NewStringUTF(env, %s)", arg.name)
            elif isinstance(arg.argType, arsdkparser.ArMultiSetting):
                out.write("(jlong)(uintptr_t)&%s", arg.name)
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
        if cmd.args:
            out.write("JNIEXPORT jint JNICALL\n%s_nativeEncode%s(JNIEnv *env, jclass jcls, jlong nativeCmd, %s) {\n",
                      jni_func_name(feature, cls), jni_method_name(cmd.name),
                  ", ".join(jni_arg_type(arg) + " " + arg.name for arg in cmd.args))
        else:
            out.write("JNIEXPORT jint JNICALL\n%s_nativeEncode%s(JNIEnv *env, jclass jcls, jlong nativeCmd) {\n",
                      jni_func_name(feature, cls), jni_method_name(cmd.name))
        out.write("\tstruct arsdk_cmd *cmd = (struct arsdk_cmd *)(uintptr_t)nativeCmd;\n")
        for arg in cmd.args:
            if arg.argType == arsdkparser.ArArgType.STRING:
                out.write("\tconst char* c_%s = (*env)->GetStringUTFChars(env, %s, NULL);\n", arg.name, arg.name)
            elif isinstance(arg.argType, arsdkparser.ArMultiSetting):
                out.write("\t%s *c_%s = (%s *)(uintptr_t) %s;\n", c_arg_type(arg, feature), arg.name, c_arg_type(arg, feature), arg.name)
        if cmd.args:
            out.write("\tint res = arsdk_cmd_enc_%s_%s(cmd, %s);\n",
                  c_name(full_name), c_name(cmd.name),
                  ", ".join("c_" + arg.name if arg.argType == arsdkparser.ArArgType.STRING or
                                               isinstance(arg.argType, arsdkparser.ArMultiSetting)
                                            else arg.name
                            for arg in cmd.args))
        else:
            out.write("\tint res = arsdk_cmd_enc_%s_%s(cmd);\n",
                      c_name(full_name), c_name(cmd.name))
        for arg in cmd.args:
            if arg.argType == arsdkparser.ArArgType.STRING:
                out.write("\tif (c_%s != NULL) (*env)->ReleaseStringUTFChars(env, %s, c_%s);\n",
                          arg.name, arg.name, arg.name)
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

def gen_jni_ftr_multisets(feature, cls, multisets, out):
    for mset in multisets:
        full_name = feature.name + ("_" + mset.name)
        # decode multisetting evts
        if list(_evts(mset.msgs)):
            # callback methods id cache
            out.write("static struct {\n")
            for evt in _evts(mset.msgs):
                out.write("\tjmethodID jmid_%s;\n", evt.name)
            out.write("} s_cb_%s_cache;\n\n", full_name)
            # class init
            out.write("JNIEXPORT void JNICALL\n%s_nativeClassInit(JNIEnv *env, jclass jcls, jobject callbackClass) {\n",
                      jni_func_name(feature, mset))
            for evt in _evts(mset.msgs):
                out.write("\ts_cb_%s_cache.jmid_%s = (*env)->GetMethodID(env, callbackClass, \"%s\", \"(%s)V\");\n",
                          full_name, evt.name, java_method_name(evt.name), jni_method_signature(evt.args))
            out.write("}\n\n")

        # encode multisetting cmds
        for cmd in _cmds(mset.msgs):
            if cmd.args:
                out.write("JNIEXPORT jint JNICALL\n%s_nativeEncode%s(JNIEnv *env, jclass jcls, jlong nativeMSet, %s) {\n",
                          jni_func_name(feature, mset), jni_method_name(cmd.name),
                      ", ".join(jni_arg_type(arg) + " " + arg.name for arg in cmd.args))
            else:
                out.write("JNIEXPORT jint JNICALL\n%s_nativeEncode%s(JNIEnv *env, jclass jcls, jlong nativeMSet) {\n",
                          jni_func_name(feature, mset), jni_method_name(cmd.name))
            out.write("\tunion arsdkcore_multiset *mset = (union arsdkcore_multiset *)(uintptr_t)nativeMSet;\n")
            for arg in cmd.args:
                if arg.argType == arsdkparser.ArArgType.STRING:
                    out.write("\tconst char* c_%s = (*env)->GetStringUTFChars(env, %s, NULL);\n", arg.name, arg.name)
                    #TODO see when release the c string
                    raise Exception("Multi settings with string argument is not yet correctly managed :"
                            " mset: %s cmd: %s arg: %s" % ( mset.name, cmd.name, arg.name))
                elif isinstance(arg.argType, arsdkparser.ArMultiSetting):
                    out.write("\t%s *c_%s = (%s *)(uintptr_t) %s;\n",
                            c_arg_type(arg, feature), arg.name,
                            c_arg_type(arg, feature), arg.name)
            for arg in cmd.args:
                out.write("\tmset->%s.%s.%s = %s;\n",
                        mset.name.lower(),
                        "%s_%s_%s" %(cmd.ftr.name, cmd.cls.name, cmd.name) if cmd.cls
                                else "%s_%s" %(cmd.ftr.name, cmd.name),
                        arg.name,
                        "c_" + arg.name if arg.argType == arsdkparser.ArArgType.STRING or
                                            isinstance(arg.argType, arsdkparser.ArMultiSetting)
                                        else arg.name)
            out.write("\tmset->%s.%s.is_set = 1;\n", mset.name.lower(),
                    "%s_%s_%s" %(cmd.ftr.name, cmd.cls.name, cmd.name) if cmd.cls
                    else "%s_%s" %(cmd.ftr.name, cmd.name))

            out.write("\treturn 0;\n")
            out.write("}\n\n")

#===============================================================================

def gen_jni_feature(feature, cls, evts, cmds, out):
    if evts:
        # decode evts
        gen_jni_callback_cache(feature, cls, evts, out)
        gen_jni_decode(feature, cls, evts, out)
    if cmds:
        # encode cmds
        gen_jni_encode(feature, cls, cmds, out)
    if feature.multisets:
        # specific multi settings
        gen_jni_ftr_multisets(feature, cls, feature.multisets, out)

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

    # jni multi settings file
    filepath = os.path.join(outdir, "arsdkcore_multiset.h")
    with open(filepath, "w") as fileobj:
        print("generating %s" % filepath)
        gen_jni_multiset(ctx, Writer(fileobj))

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
