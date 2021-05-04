import arsdkparser

#===============================================================================
class Writer(object):
    def __init__(self, fileobj):
        self.fileobj = fileobj

    def write(self, fmt, *args):
        if args:
            self.fileobj.write(fmt % (args))
        else:
            self.fileobj.write(fmt % ())

#===============================================================================

package = "com.parrot.drone.sdkcore.arsdk"

def java_class_name(name):
    components = name.split('_')
    return "".join(x.capitalize() for x in components)

def java_feature_class_name(name):
    return "ArsdkFeature" + java_class_name(name)

def java_feature_file_name(name):
    return java_feature_class_name(name) + ".java"

def java_method_name(name):
    components = name.split('_')
    return components[0].lower() + "".join(x[0].upper() + x[1:] for x in components[1:])

def java_param_name(name):
    components = name.split('_')
    return components[0].lower() + "".join(x[0].upper() + x[1:] for x in components[1:])

def java_enum_name(name):
    if name[0].isalpha():
        return name.upper()
    else:
        return "E" + name.upper()

def java_arg_name(arg):
    if isinstance(arg.argType, arsdkparser.ArEnum):
        argName = java_param_name(arg.name)
    elif isinstance(arg.argType, arsdkparser.ArBitfield):
        argName = java_param_name(arg.name) + "BitField"
    else:
        argName = java_param_name(arg.name)
    return argName

def java_arg_type(arg, jni, feature = None):
    java_args = {
        arsdkparser.ArArgType.I8: "int",
        arsdkparser.ArArgType.U8: "int",
        arsdkparser.ArArgType.I16: "int",
        arsdkparser.ArArgType.U16: "int",
        arsdkparser.ArArgType.I32: "int",
        arsdkparser.ArArgType.U32: "long",
        arsdkparser.ArArgType.I64: "long",
        arsdkparser.ArArgType.U64: "long",
        arsdkparser.ArArgType.FLOAT: "float",
        arsdkparser.ArArgType.DOUBLE: "double",
        arsdkparser.ArArgType.STRING: "String",
        arsdkparser.ArArgType.BINARY: "byte[]"
    }
    if isinstance(arg.argType, arsdkparser.ArEnum):
        if jni:
            argType = "int"
        else:
            if feature:
                argType = java_feature_class_name(feature.name) + "." + java_class_name(arg.argType.name)
            else:
                argType = java_class_name(arg.argType.name)
    elif isinstance(arg.argType, arsdkparser.ArBitfield):
        if arg.argType.btfType == arsdkparser.ArArgType.I64 or \
           arg.argType.btfType == arsdkparser.ArArgType.U64:
            argType = "long"
        else:
            argType = "int"
    else:
        argType = java_args[arg.argType]
    return argType

#===============================================================================

def jni_func_name(feature, cls):
    # 00024 is the standard jni separator for inner class (24 is the ascii value of '$')
    return "Java_" + package.replace('.','_') + "_ArsdkFeature" + java_class_name(feature.name) + \
           ("_00024" + cls.name if cls else "")

def jni_file_name(name):
    return "arsdkfeature_" + name.lower() + "-jni.c"

def jni_method_name(name):
    components = name.split('_')
    return "".join(x[0].upper() + x[1:] for x in components)

def jni_method_signature(args):
    jni_signature_args = {
        arsdkparser.ArArgType.I8: "I",
        arsdkparser.ArArgType.U8: "I",
        arsdkparser.ArArgType.I16: "I",
        arsdkparser.ArArgType.U16: "I",
        arsdkparser.ArArgType.I32: "I",
        arsdkparser.ArArgType.U32: "J",
        arsdkparser.ArArgType.I64: "J",
        arsdkparser.ArArgType.U64: "J",
        arsdkparser.ArArgType.FLOAT: "F",
        arsdkparser.ArArgType.DOUBLE: "D",
        arsdkparser.ArArgType.STRING: "Ljava/lang/String;",
        arsdkparser.ArArgType.BINARY: "[B"
    }
    sig = ""
    for arg in args:
        if isinstance(arg.argType, arsdkparser.ArEnum):
            sig +="I"
        elif isinstance(arg.argType, arsdkparser.ArBitfield):
            if arg.argType.btfType == arsdkparser.ArArgType.I64 or \
               arg.argType.btfType == arsdkparser.ArArgType.U64:
                sig +="J"
            else:
                sig +="I"
        else:
            sig += jni_signature_args[arg.argType]
    return sig

def jni_arg_type(arg):
    jni_args = {
        arsdkparser.ArArgType.I8: "jint",
        arsdkparser.ArArgType.U8: "jint",
        arsdkparser.ArArgType.I16: "jint",
        arsdkparser.ArArgType.U16: "jint",
        arsdkparser.ArArgType.I32: "jint",
        arsdkparser.ArArgType.U32: "jlong",
        arsdkparser.ArArgType.I64: "jlong",
        arsdkparser.ArArgType.U64: "jlong",
        arsdkparser.ArArgType.FLOAT: "jfloat",
        arsdkparser.ArArgType.DOUBLE: "jdouble",
        arsdkparser.ArArgType.STRING: "jstring",
        arsdkparser.ArArgType.BINARY: "jbyteArray"
    }
    if isinstance(arg.argType, arsdkparser.ArEnum):
        type = "jint"
    elif isinstance(arg.argType, arsdkparser.ArBitfield):
        if arg.argType.btfType == arsdkparser.ArArgType.I64 or \
           arg.argType.btfType == arsdkparser.ArArgType.U64:
            type = "jlong"
        else:
            type = "jint"
    else:
        type = jni_args[arg.argType]
    return type;

#===============================================================================

def c_arg_type(arg, feature=None):
    c_args = {
        arsdkparser.ArArgType.I8: "int8_t",
        arsdkparser.ArArgType.U8: "uint8_t",
        arsdkparser.ArArgType.I16: "int16_t",
        arsdkparser.ArArgType.U16: "uint16_t",
        arsdkparser.ArArgType.I32: "int32_t",
        arsdkparser.ArArgType.U32: "uint32_t",
        arsdkparser.ArArgType.I64: "int64_t",
        arsdkparser.ArArgType.U64: "uint64_t",
        arsdkparser.ArArgType.FLOAT: "float",
        arsdkparser.ArArgType.DOUBLE: "double",
        arsdkparser.ArArgType.STRING: "const char *",
        arsdkparser.ArArgType.BINARY: "struct arsdk_binary",
    }
    if isinstance(arg.argType, arsdkparser.ArEnum):
        type = "int32_t"
    elif isinstance(arg.argType, arsdkparser.ArBitfield):
        type = c_args[arg.argType.btfType]
    else:
        type = c_args[arg.argType]
    return type;

def c_name(val):
    return val[0].upper() + val[1:]
