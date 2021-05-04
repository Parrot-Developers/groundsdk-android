#!/usr/bin/env python

import sys, os
import errno
from common import *

#===============================================================================

arg_reader = {
    arsdkparser.ArArgType.I8: "dec.readSignedByte()",
    arsdkparser.ArArgType.U8: "dec.readUnsignedByte()",
    arsdkparser.ArArgType.I16: "dec.readSignedShort()",
    arsdkparser.ArArgType.U16: "dec.readUnsignedShort()",
    arsdkparser.ArArgType.I32: "dec.readSignedInt()",
    arsdkparser.ArArgType.U32: "dec.readUnsignedInt()",
    arsdkparser.ArArgType.I64: "dec.readSignedLong()",
    arsdkparser.ArArgType.U64: "dec.readUnsignedLong()",
    arsdkparser.ArArgType.FLOAT: "dec.readFloat()",
    arsdkparser.ArArgType.DOUBLE: "dec.readDouble()",
    arsdkparser.ArArgType.STRING: "dec.readString()",
    arsdkparser.ArArgType.BINARY: "dec.readBinary()"
}

arg_writer = {
    arsdkparser.ArArgType.I8: "enc.writeByte((byte)%s)",
    arsdkparser.ArArgType.U8: "enc.writeByte((byte)%s)",
    arsdkparser.ArArgType.I16: "enc.writeShort((short)%s)",
    arsdkparser.ArArgType.U16: "enc.writeShort((short)%s)",
    arsdkparser.ArArgType.I32: "enc.writeInt((int)%s)",
    arsdkparser.ArArgType.U32: "enc.writeInt((int)%s)",
    arsdkparser.ArArgType.I64: "enc.writeLong(%s)",
    arsdkparser.ArArgType.U64: "enc.writeLong(%s)",
    arsdkparser.ArArgType.FLOAT: "enc.writeFloat(%s)",
    arsdkparser.ArArgType.DOUBLE: "enc.writeDouble(%s)",
    arsdkparser.ArArgType.STRING: "enc.writeString(%s)",
    arsdkparser.ArArgType.BINARY: "enc.writeBinary(%s)"
}

def java_comparator(arg, val):
    if isinstance(arg.argType, arsdkparser.ArEnum):
        return "(" + val + " == " + java_arg_name(arg) + ".value)"
    elif arg.argType == arsdkparser.ArArgType.STRING:
        return "(" + val + ".equals(" + java_arg_name(arg) + "))"
    elif arg.argType == arsdkparser.ArArgType.BINARY:
        return "java.util.Arrays.equals(" + val + ", " + java_arg_name(arg) + ")"
    else:
         return "(" + val + " == "+ java_arg_name(arg)+ ")"

def java_arg_reader(arg):
    if isinstance(arg.argType, arsdkparser.ArEnum):
        return arg_reader[arsdkparser.ArArgType.I32]
    elif isinstance(arg.argType, arsdkparser.ArBitfield):
        return arg_reader[arg.argType.btfType]
    else:
        return arg_reader[arg.argType]

def java_arg_writer(arg, val):
    if isinstance(arg.argType, arsdkparser.ArEnum):
        return "enc.writeInt(%s == null ? -1 : %s.value)" % (val, val)
    elif isinstance(arg.argType, arsdkparser.ArBitfield):
        return arg_writer[arg.argType.btfType] % val
    else:
        return arg_writer[arg.argType] % val

def java_arg_value (arg, arg_name, feature):
    if isinstance(arg.argType, arsdkparser.ArEnum):
        return "%s.fromValue(%s)" % (java_arg_type(arg, False, feature), arg_name)
    else:
        return arg_name

#===============================================================================
#===============================================================================

def gen_feature_expected_cmd(feature, cls, cmds, out):
    for cmd in sorted(cmds, key=lambda cmd: cmd.cmdId):
        out.write("    public static ExpectedCmd %s(%s) {\n",
            java_method_name(feature.name + ("_" + cls.name if cls else "") + "_" + cmd.name),
            ", ".join("final " + java_arg_type(arg, False, feature) + " " + java_arg_name(arg) for arg in cmd.args))
        out.write("        return new ExpectedCmd() {\n");
        out.write("            @Override\n");
        out.write("            public boolean match(ArsdkCommand cmd, boolean checkParams) {\n");
        out.write("                ArsdkCommandDecoder dec = new ArsdkCommandDecoder(cmd);\n")
        out.write("                if (dec.readUnsignedByte() != %d || dec.readUnsignedByte() != %d || dec.readUnsignedShort() != %d)\n",
                  feature.featureId, (cls.classId if cls else 0), cmd.cmdId)
        out.write("                    return false;\n")
        if cmd.args:
            out.write("                return !checkParams || %s;\n",
                " && ".join(java_comparator(arg, java_arg_reader(arg)) for arg in cmd.args))
        else:
            out.write("                return true;\n")

        out.write("            }\n");
        out.write("            @Override\n")
        out.write("            public void describeExpected(Description description) {\n")
        out.write("                description.appendText(ArsdkCommand.getName((short)%d,(short)%d));\n",
                  (feature.featureId << 8) + (cls.classId if cls else 0), cmd.cmdId)
        for arg in cmd.args:
            out.write("                description.appendText(\", %s \").appendValue(%s);\n",
                      java_arg_name(arg), java_arg_name(arg))
        out.write("            }\n");
        out.write("            @Override\n")
        out.write("            public void describeMismatch(ArsdkCommand cmd, Description description) {\n")
        out.write("                ArsdkCommandDecoder dec = new ArsdkCommandDecoder(cmd);\n")
        out.write("                if (dec.readUnsignedByte() != %d || dec.readUnsignedByte() != %d || dec.readUnsignedShort() != %d)\n",
                  feature.featureId, (cls.classId if cls else 0), cmd.cmdId)
        out.write("                    description.appendText(\"was \").appendText(cmd.getName());\n")
        out.write("                else {\n")
        out.write("                    int i = 0;\n")
        for arg in cmd.args:
            out.write("                    %s _%s = %s;\n",
                      java_arg_type(arg, True, feature), java_arg_name(arg), java_arg_reader(arg))
            out.write("                    if (!%s) {\n", java_comparator(arg, "_%s" % java_arg_name(arg)))
            out.write("                        if (i++ > 0) description.appendText(\", \");\n")
            out.write("                        description.appendText(\"%s was \").appendValue(%s);\n",
                      java_arg_name(arg), java_arg_value(arg, "_%s" % java_arg_name(arg), feature))
            out.write("                    }\n")
        out.write("                }\n")
        out.write("            }\n")
        out.write("        };\n")
        out.write("    }\n\n")

#===============================================================================

def gen_feature_encoder(feature, cls, evts, out):
    for evt in sorted(evts, key=lambda evt: evt.cmdId):
        out.write("    public static ArsdkCommand %s(%s) {\n",
                  java_method_name("encode_" + feature.name +  ("_"+ cls.name if cls else "") + "_"+ evt.name),
                  ", ".join(java_arg_type(arg, False, feature) + " " + java_arg_name(arg) for arg in evt.args))
        out.write("        ArsdkCommandEncoder enc = new ArsdkCommandEncoder();\n")
        out.write("        enc.writeByte((byte)%d).writeByte((byte)%d).writeShort((short)%d);\n",
                  feature.featureId, (cls.classId if cls else 0), evt.cmdId)
        for arg in evt.args:
            out.write("        %s;\n", java_arg_writer(arg, java_arg_name(arg)))
        out.write("        return enc.encode();\n")
        out.write("    }\n")

#===============================================================================
#===============================================================================

def gen_expected_cmd(ctx, out):
    out.write("/** Generated, do not edit ! */\n")
    out.write("package %s;\n\n", package)
    out.write("import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;\n")
    out.write("import androidx.annotation.NonNull;\n")
    out.write("import org.hamcrest.Description;\n")
    out.write("import java.util.List;\n")
    out.write("@SuppressWarnings(\"All\")\n")
    out.write("public abstract class ExpectedCmd {\n")
    out.write("    public abstract boolean match(ArsdkCommand cmd, boolean checkParams);\n")
    out.write("    public abstract void describeExpected(Description description);\n\n")
    out.write("    public abstract void describeMismatch(ArsdkCommand cmd, Description description);\n\n")
    out.write("    public static boolean cmpCmds(@NonNull List<ArsdkCommand> cmds1, @NonNull List<ArsdkCommand> cmds2) {\n")
    out.write("        for (ArsdkCommand cmd1 : cmds1) {\n")
    out.write("            boolean found = false;\n")
    out.write("            for (ArsdkCommand cmd2 : cmds2) {\n")
    out.write("                if ((cmd2.getCommandId() == cmd1.getCommandId()) &&\n")
    out.write("                    (cmd2.getData().equals(cmd2.getData()))) {\n")
    out.write("                    cmds2.remove(cmd2);\n")
    out.write("                    found = true;\n")
    out.write("                    break;\n")
    out.write("                }\n")
    out.write("            }\n")
    out.write("            if (!found) {\n")
    out.write("                return false;\n")
    out.write("            }\n")
    out.write("        }\n")
    out.write("        return (cmds2.size() == 0);\n")
    out.write("    }\n\n")
    for featureId in sorted(ctx.featuresById.keys()):
        feature = ctx.featuresById[featureId]
        if feature.classes:
            # Project based (old)
            for clsId in sorted(feature.classesById.keys()):
                cls = feature.classesById[clsId]
                gen_feature_expected_cmd(feature, cls,
                            [cmd for cmd in feature.cmds if clsId == cmd.cls.classId], out)
        else:
            # feature based (new)
            gen_feature_expected_cmd(feature, None, feature.cmds, out)
    out.write("}\n")

#===============================================================================

def gen_encoder(ctx, out):
    out.write("/** Generated, do not edit ! */\n")
    out.write("package %s;\n\n", package)
    out.write("import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;\n")
    out.write("import java.util.List;\n\n")
    out.write("public class ArsdkEncoder {\n")
    for featureId in sorted(ctx.featuresById.keys()):
        feature = ctx.featuresById[featureId]
        if feature.classes:
            # Project based (old)
            for clsId in sorted(feature.classesById.keys()):
                cls = feature.classesById[clsId]
                gen_feature_encoder(feature, cls,
                            [evt for evt in feature.evts if clsId == evt.cls.classId], out)
        else:
            # feature based (new)
            gen_feature_encoder(feature, None, feature.evts, out)
    out.write("}\n")

#===============================================================================
#===============================================================================
def list_files(ctx, outdir, extra):
    # not used
    pass

#===============================================================================
#===============================================================================
def generate_files(ctx, outdir, extra):

    filepath = os.path.join(os.path.join(outdir, *package.split(".")), "ExpectedCmd.java")
    try:
        os.makedirs(os.path.dirname(filepath))
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise
    print("generating %s" % filepath)
    with open(filepath, "w") as fileobj:
        gen_expected_cmd(ctx, Writer(fileobj))

    filepath = os.path.join(os.path.join(outdir, *package.split(".")), "ArsdkEncoder.java")
    try:
        os.makedirs(os.path.dirname(filepath))
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise
    print("generating %s" % filepath)
    with open(filepath, "w") as fileobj:
        gen_encoder(ctx, Writer(fileobj))
