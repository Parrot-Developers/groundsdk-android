#!/usr/bin/env python3

import sys, os
import errno
from common import *

import textwrap


def make_cmd_javadoc(cmd, indent = ""):
    doc = indent + "/**\n"
    wrapper = textwrap.TextWrapper(width = 120, initial_indent = indent + " * ", subsequent_indent = indent + " * ")
    doc += wrapper.fill(cmd.doc.desc.replace('>', '&gt;'))
    if len(cmd.args) > 0:
        doc += "\n" + indent + " * "
    for arg in cmd.args:
        doc += "\n" + wrapper.fill("@param %s: %s" % (java_arg_name(arg), arg.doc.replace('>', '&gt;')))
    return doc + "\n" + indent + " */"

def make_javadoc(txt, indent = ""):
    if not txt.endswith('.'):
        txt += '.'
    doc = indent + "/**\n"
    wrapper = textwrap.TextWrapper(width = 120, initial_indent = indent + " * ", subsequent_indent = indent + " * ")
    doc += wrapper.fill(txt.replace('>', '&gt;'))
    return doc + "\n" + indent + " */"
#===============================================================================
#===============================================================================

def java_arg_enum_name(arg):
    return "enum%s" % java_arg_name(arg).capitalize()

def gen_java_feature_enum(enum, out):
    out.write("%s\n", make_javadoc(enum.doc, "    "))
    out.write("    public enum %s {\n", java_class_name(enum.name))
    for idx, enumVal in enumerate(enum.values):
        out.write("\n%s\n", make_javadoc(enumVal.doc, "        "))
        out.write("        %s(%s)", java_enum_name(enumVal.name), enumVal.value)
        if idx == len(enum.values) - 1:
            out.write(";\n")
        else:
            out.write(",\n")

    out.write("\n        /** Internal arsdk value. */\n")
    out.write("        public final int value;\n\n")
    out.write("        /**\n")
    out.write("         * Obtains an enum from its internal arsdk value.\n");
    out.write("         *\n")
    out.write("         * @param value arsdk enum internal value\n")
    out.write("         *\n")
    out.write("         * @returns the corresponding enum in case it exists, otherwise {@code null} \n")
    out.write("         */\n")
    out.write("        @Nullable\n");
    out.write("        public static %s fromValue(int value) {\n", java_class_name(enum.name))
    out.write("            return MAP.get(value, null);\n")
    out.write("        }\n")

    if enum.usedLikeBitfield:
        if len(enum.values) <= 32:
            bitFieldType = 'int'
            base = '1'
            javaTypeClass = 'Integer'
        else:
            bitFieldType = 'long'
            base = '1L'
            javaTypeClass = 'Long'
        out.write("\n")
        out.write("        /**\n")
        out.write("         * Tells whether this enum value is armed in a given bitfield.\n");
        out.write("         *\n")
        out.write("         * @param bitfield bitfield to process\n")
        out.write("         *\n")
        out.write("         * @returns {@code true} if this enum is in specified bitfield, otherwise {@code false}\n")
        out.write("         */\n")
        out.write("        public boolean inBitField(%s bitfield) {\n", bitFieldType)
        out.write("            return (bitfield & (%s << value)) != 0;\n", base)
        out.write("        }\n\n")
        out.write("        /**\n")
        out.write("         * Applies a function to each armed enum value in a given bitfield.\n");
        out.write("         *\n")
        out.write("         * @param bitfield bitfield to process\n")
        out.write("         * @param func     function to call with each armed enum value\n")
        out.write("         */\n")
        out.write("        public static void each(%s bitfield, @NonNull Consumer<%s> func) {\n", bitFieldType, java_class_name(enum.name))
        out.write("            while (bitfield != 0) {\n")
        out.write("                int value = %s.numberOfTrailingZeros(bitfield);\n", javaTypeClass)
        out.write("                if (value >= %d) {\n", len(enum.values))
        out.write("                    ULog.e(TAG, \"Unsupported %s bitfield value \" + value);\n", java_class_name(enum.name))
        out.write("                    break;\n")
        out.write("                }\n")
        out.write("                func.accept(fromValue(value));\n")
        out.write("                bitfield ^= %s << value;\n", base)
        out.write("            }\n")
        out.write("        }\n\n")
        out.write("        /**\n")
        out.write("         * Extracts armed enum value(s) from a given bitfield.\n");
        out.write("         *\n")
        out.write("         * @param bitfield bitfield to process\n")
        out.write("         *\n")
        out.write("         * @return a set containing enum value(s) armed in the specified bitfield\n")
        out.write("         */\n")
        out.write("        @NonNull\n")
        out.write("        public static EnumSet<%s> fromBitfield(%s bitfield) {\n", java_class_name(enum.name), bitFieldType)
        out.write("            EnumSet<%s> enums = EnumSet.noneOf(%s.class);\n", java_class_name(enum.name), java_class_name(enum.name))
        out.write("            each(bitfield, enums::add);\n")
        out.write("            return enums;\n")
        out.write("        }\n\n")
        out.write("        /**\n")
        out.write("         * Encodes a set of enum value(s) to a bitfield.\n");
        out.write("         *\n")
        out.write("         * @param enums enums to arm in the bitfield\n")
        out.write("         *\n")
        out.write("         * @return a bitfield where specified enum value(s) are armed\n")
        out.write("         */\n")
        out.write("        public static %s toBitField(@NonNull %s... enums) {\n", bitFieldType, java_class_name(enum.name))
        out.write("            %s bitField = 0;\n", bitFieldType)
        out.write("            for (%s e : enums)\n ", java_class_name(enum.name))
        out.write("                bitField |= %s << e.value;\n", base)
        out.write("            return bitField;\n")
        out.write("        }\n")

    out.write("\n        private %s(int value) {\n", java_class_name(enum.name))
    out.write("            this.value = value;\n")
    out.write("        }\n")
    out.write("\n        private static final SparseArray<%s> MAP;\n", java_class_name(enum.name))
    out.write("\n        static {\n")
    out.write("            MAP = new SparseArray<>();\n")
    out.write("            for (%s e: values())\n", java_class_name(enum.name))
    out.write("                MAP.put(e.value, e);\n")
    out.write("        }\n")

    out.write("    }\n\n")


#===============================================================================
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

def gen_java_feature_callback(indent, evts, out):
    out.write("%s/** Callback receiving decoded events. */\n", indent);
    out.write("%spublic interface Callback {\n", indent)
    for evt in sorted(evts, key=lambda evt: evt.cmdId):
        out.write("\n%s\n", make_cmd_javadoc(evt, "    " + indent))

        if evt.isDeprecated:
            out.write("%s    @Deprecated\n", indent)

        def formatArg(arg):
            argStr = "%s %s" % (java_arg_type(arg, False), java_arg_name(arg))
            if isinstance(arg.argType, arsdkparser.ArEnum):
                argStr = "@Nullable %s" % argStr
            return argStr

        out.write("%s    public default void %s(%s) {}\n", indent, java_method_name("on_" + evt.name),
                  ", ".join(formatArg(arg) for arg in evt.args))
    out.write("%s}\n", indent)

    for evt in sorted(evts, key=lambda evt: evt.cmdId):
        out.write("\n%sprivate static void %s(Callback cb%s) {\n", indent, java_method_name(evt.name),
                  "".join(", " + java_arg_type(arg, True) + " " + java_arg_name(arg) for arg in evt.args))

        for arg in filter(lambda arg: isinstance(arg.argType, arsdkparser.ArEnum), evt.args):
            out.write("%(0)s %(1)s %(2)s = %(1)s.fromValue(%(3)s);\n"
                      "%(0)s if (%(2)s == null) ULog.e(TAG, \"Unsupported %(1)s value \" + %(3)s);\n" % {
                '0': "%s   " % indent,
                '1': "%s.%s" % (java_feature_class_name(evt.ftr.name), java_class_name(arg.argType.name)),
                '2': java_arg_enum_name(arg),
                '3': java_arg_name(arg),
            })

        def formatArg(arg):
            if isinstance(arg.argType, arsdkparser.ArEnum):
                return java_arg_enum_name(arg)
            else:
                return java_arg_name(arg)

        out.write("%s    try {\n", indent)
        out.write("%s        cb.%s(%s);\n", indent, java_method_name("on_" + evt.name),
            ", ".join(formatArg(arg) for arg in evt.args))
        out.write("%s    } catch (ArsdkCommand.RejectedEventException e) {\n", indent)
        out.write("%s        ULog.e(TAG, \"Rejected event: %s", indent, evt.full_name())
        if len(evt.args) > 0:
            out.write(" [%s]", ', '.join(['{}: " + {} + "'.format(arg.name, java_arg_name(arg)) for arg in evt.args]))
        out.write("\", e);\n");
        out.write("%s    }\n", indent)
        out.write("%s}\n", indent)
    out.write("%s\n", indent)


#===============================================================================

def gen_java_feature_encode(indent, cmds, out):
    for cmd in sorted(cmds, key=lambda cmd: cmd.cmdId):
        out.write("%s\n", make_cmd_javadoc(cmd, indent))
        if cmd.isDeprecated:
            out.write("%s@Deprecated\n", indent)

        def formatArg(arg):
            argStr = "%s %s" % (java_arg_type(arg, False), java_arg_name(arg))
            if isinstance(arg.argType, arsdkparser.ArEnum):
                argStr = "@NonNull %s" % argStr
            return argStr

        out.write("%spublic static ArsdkCommand %s(%s) {\n", indent, java_method_name("encode_" + cmd.name),
                  ", ".join(formatArg(arg) for arg in cmd.args))
        out.write("%s    ArsdkCommand cmd = ArsdkCommand.Pool.DEFAULT.obtain();\n", indent)
        out.write("%s    %s(cmd.getNativePtr()", indent, java_method_name("native_encode_" + cmd.name))
        for arg in cmd.args:
           if isinstance(arg.argType, arsdkparser.ArEnum):
               out.write(", %s.value", java_arg_name(arg))
           else:
               out.write(", %s", java_arg_name(arg))
        out.write(");\n")
        out.write("%s    return cmd;\n", indent)
        out.write("%s}\n\n", indent)

#===============================================================================

def gen_java_feature_encode_native(indent, cmds, out):
    for cmd in sorted(cmds, key=lambda cmd: cmd.cmdId):
        if cmd.args:
            out.write("%sprivate static native int %s(long nativeCmd, %s);\n", indent,
                      java_method_name("native_encode_" + cmd.name),
                  ", ".join(java_arg_type(arg, True) + " " + arg.name for arg in cmd.args))
        else:
            out.write("%sprivate static native int %s(long nativeCmd);\n", indent,
                      java_method_name("native_encode_" + cmd.name))

#===============================================================================

def gen_java_feature(indent, uid, evts, cmds, out):
    # feature uid
    out.write("\n%s/** Feature uid. */\n", indent)
    out.write("%spublic static final int UID = 0x%04X;\n\n", indent, uid)
    # evts
    if evts:
        # events uid
        for evt in sorted(evts, key=lambda evt: evt.cmdId):
            out.write("%s\n", textwrap.fill(indent + "/** Uid of %s event. */;" % evt.name, width = 120))
            out.write("%spublic static final int %s_UID = 0x%04X;\n\n", indent, evt.name.upper(), evt.cmdId)
        # decode
        out.write("%s/**\n", indent)
        out.write("%s * Decodes a command.\n", indent)
        out.write("%s *\n", indent)
        out.write("%s * @param command : command to decode\n", indent)
        out.write("%s * @param callback: callback receiving decoded events\n", indent)
        out.write("%s */\n", indent);
        out.write("%spublic static void decode(@NonNull ArsdkCommand command, @NonNull Callback callback) {\n", indent)
        out.write("%s    nativeDecode(command.getNativePtr(), callback);\n", indent)
        out.write("%s}\n\n", indent)
        # callback
        gen_java_feature_callback(indent, evts, out)

    # encode cmds
    if cmds:
        gen_java_feature_encode(indent, cmds, out)

    # evts native
    if evts:
        # native decode
        out.write("%sprivate static native int nativeDecode(long nativeCmd, Callback callback);\n", indent)
        # native class init
        out.write("%sprivate static native void nativeClassInit();\n", indent)
        # class init
        out.write("\n%sstatic { nativeClassInit(); }\n", indent)

    # cmds encode natives
    if cmds:
        gen_java_feature_encode_native(indent, cmds, out)

#===============================================================================
def gen_java(feature, out):
    out.write("/** Generated, do not edit ! */\n")
    out.write("package %s;\n\n", package)

    out.write(
        "import androidx.annotation.NonNull;\n"
        "import androidx.annotation.Nullable;\n"
        "\n"
        "import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;\n"
        "import com.parrot.drone.sdkcore.ulog.ULog;\n"
        "\n"
        "import static com.parrot.drone.sdkcore.arsdk.Logging.TAG;\n"
        "\n"
    )

    # TODO; make it conditional in case the feature has no binary messages
    out.write("import java.nio.ByteBuffer;\n")

    # add enum specific imports
    if feature.enums:
        out.write("import android.util.SparseArray;\n\n")
        if any(enum.usedLikeBitfield for enum in feature.enums):
            out.write("import java.util.function.Consumer;\n");
            out.write("import java.util.EnumSet;\n\n")

    out.write("%s\n", make_javadoc(feature.name.capitalize() + " feature command/event interface."))
    out.write("public class %s {\n", java_feature_class_name(feature.name))
    out.write("\n")

    # Enums
    for enum in feature.enums:
        gen_java_feature_enum(enum, out)

    # features
    if feature.classes:
        # Project based (old)
        for clsId in sorted(feature.classesById.keys()):
            cls = feature.classesById[clsId]
            out.write("    /**\n")
            out.write("     * %s \n", cls.doc)
            out.write("     */\n")
            out.write("    public static class %s {\n", cls.name)
            gen_java_feature("        ", feature.featureId * 256 + clsId,
                             [evt for evt in feature.evts if clsId == evt.cls.classId],
                             [cmd for cmd in feature.cmds if clsId == cmd.cls.classId], out)
            out.write("    }\n\n")

    else:
        # feature based (new)
         gen_java_feature("    ", feature.featureId * 256, feature.evts, feature.cmds, out)

    out.write("}\n")


#===============================================================================
#===============================================================================
def list_files(ctx, outdir, extra):
    for featureId in sorted(ctx.featuresById.keys()):
        featureobj = ctx.featuresById[featureId]
        print(os.path.join(os.path.join(outdir, *package.split(".")), java_feature_file_name(featureobj.name)))


#===============================================================================
#===============================================================================
def generate_files(ctx, outdir, extra):
    for featureId in sorted(ctx.featuresById.keys()):
        featureobj = ctx.featuresById[featureId]
        filepath = os.path.join(os.path.join(outdir, *package.split(".")), java_feature_file_name(featureobj.name))
        try:
            os.makedirs(os.path.dirname(filepath))
        except OSError as exc:
            if exc.errno != errno.EEXIST:
                raise
        print("generating %s" % filepath)
        with open(filepath, "w") as fileobj:
            gen_java(featureobj, Writer(fileobj))
