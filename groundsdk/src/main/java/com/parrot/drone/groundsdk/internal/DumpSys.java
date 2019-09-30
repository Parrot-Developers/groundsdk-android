/*
 * Copyright (C) 2019 Parrot Drones SAS
 */

package com.parrot.drone.groundsdk.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.BuildConfig;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that provides a dumpsys backend, on DEBUG builds.
 */
final class DumpSys {

    /**
     * Allows to write to dumpsys output.
     */
    interface Dumpable {

        /**
         * Called to write debug dump to dumpsys.
         *
         * @param writer writer to dump to
         * @param args   command line arguments to process
         */
        void dump(@NonNull PrintWriter writer, @NonNull Set<String> args);
    }

    /** Dump source. */
    @Nullable
    private static Dumpable sDumpSrc;

    /**
     * Binds dumpsys service.
     * <p>
     * This method does nothing on non-DEBUG builds, and also in case dumpsys service is already bound.
     *
     * @param context android context
     * @param dumpSrc source to invoke to output debug dump
     */
    static void startService(@NonNull Context context, @NonNull Dumpable dumpSrc) {
        if (!BuildConfig.DEBUG || sDumpSrc != null) {
            return;
        }

        sDumpSrc = dumpSrc;

        if (!context.bindService(new Intent(context, Service.class), SERVICE_CONNECTION,
                Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND | Context.BIND_WAIVE_PRIORITY
                | Context.BIND_ALLOW_OOM_MANAGEMENT)) {
            sDumpSrc = null;
        }
    }

    /**
     * Unbinds dumpsys service.
     * <p>
     * This method does nothing on non-DEBUG builds, and also in case dumpsys service is not yet bound.
     *
     * @param context android context
     */
    static void stopService(@NonNull Context context) {
        if (!BuildConfig.DEBUG || sDumpSrc == null) {
            return;
        }
        context.unbindService(SERVICE_CONNECTION);
        sDumpSrc = null;
    }

    /**
     * Android service used for debug dump of the whole SDK internal status.
     * <p>
     * Only registered in debug build's manifest.
     */
    public static final class Service extends android.app.Service {

        @NonNull
        @Override
        public IBinder onBind(Intent intent) {
            return new Binder(); // we need to be able to bind to this service
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter writer, String[] argList) {
            try {
                Set<String> args = new HashSet<>(Arrays.asList(argList));
                if (args.isEmpty() || args.contains("--help")) {
                    writer.write("Usage:\n");
                    writer.write("\t--help: prints this help\n");
                }
                //noinspection ConstantConditions : if null, NPE dumped below
                sDumpSrc.dump(writer, args);
            } catch (Exception e) {
                writer.write("Exception during dump:\n");
                e.printStackTrace(writer);
            }
        }
    }

    /** Receive dumpsys service connection events. Unused except as an unbind token. */
    private static final ServiceConnection SERVICE_CONNECTION = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    /**
     * Private constructor for static utility class.
     */
    private DumpSys() {
    }
}
