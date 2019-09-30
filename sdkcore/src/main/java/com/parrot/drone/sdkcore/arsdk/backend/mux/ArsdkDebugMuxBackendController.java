/*
 *     Copyright (C) 2019 Parrot Drones SAS
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions
 *     are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of the Parrot Company nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *     PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *     BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *     OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *     AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *     OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *     OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *     SUCH DAMAGE.
 *
 */

package com.parrot.drone.sdkcore.arsdk.backend.mux;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.sdkcore.R;
import com.parrot.drone.sdkcore.arsdk.backend.ArsdkBackendController;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_MUX;

/**
 * Debug MUX backend controller.
 * <p>
 * This backend controller connects to the configured USB Debug Bridge server and forwards this connection
 * to a MUX backend.
 */
public final class ArsdkDebugMuxBackendController extends ArsdkBackendController {

    /** NSD Manager service string. */
    private static final String NSD_SERVICE = "_arsdk-mux._tcp.";

    /**
     * Creates a new ArsdkDebugMuxBackendController instance.
     * <p>
     * Controller instance is created if config parameter 'arsdk_debug_bridge_address' is not empty and
     * config parameter 'arsdk_debug_usb_bridge_port' is in a valid [0, 65535] port range.
     *
     * @param context            android application context
     * @param discoverableModels list of discoverable models
     *
     * @return a new ArsdkDebugMuxBackendController instance, or {@code null} if the config is invalid
     */
    @Nullable
    public static ArsdkDebugMuxBackendController create(@NonNull Context context,
                                                        @ArsdkDevice.Type int[] discoverableModels) {
        String host = context.getString(R.string.arsdk_debug_usb_bridge_address);
        int port = context.getResources().getInteger(R.integer.arsdk_debug_usb_bridge_port);
        if (!TextUtils.isEmpty(host) && port > 0 && port < 0xFFFF) {
            return new ArsdkDebugMuxBackendController(host, port, discoverableModels);
        } else {
            NsdManager nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
            if (nsdManager != null) {
                return new ArsdkDebugMuxBackendController(nsdManager, discoverableModels);
            }
        }
        return null;
    }

    /** Nsd manager instance, null if a static ip has been provided. */
    @Nullable
    private NsdManager mNsdManager;

    /** Mux ip connection, null while not started. */
    @Nullable
    private Connection mConnection;

    /** list of Mux discoverable models. */
    @ArsdkDevice.Type
    private final int[] mDiscoverableModels;

    /**
     * Constructor using Nsd Service.
     *
     * @param nsdManager         Nsd manager service instance
     * @param discoverableModels list of discoverable models
     */
    private ArsdkDebugMuxBackendController(@NonNull NsdManager nsdManager, @ArsdkDevice.Type int[] discoverableModels) {
        mNsdManager = nsdManager;
        mDiscoverableModels = discoverableModels;
    }

    /**
     * Constructor using static ip address.
     *
     * @param host               USB debug bridge server address
     * @param port               USB debug bridge server port
     * @param discoverableModels list of discoverable models
     */
    private ArsdkDebugMuxBackendController(@NonNull String host, int port, @ArsdkDevice.Type int[] discoverableModels) {
        mConnection = new Connection(new InetSocketAddress(host, port));
        mDiscoverableModels = discoverableModels;
    }

    @Override
    protected void onStart() {
        if (mConnection != null) {
            mConnection.start();
        } else if (mNsdManager != null) {
            mNsdManager.discoverServices(NSD_SERVICE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }
    }

    @Override
    protected void onStop() {
        if (mConnection != null) {
            mConnection.stop();
        } else if (mNsdManager != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }

    /** NSD Manager listener. */
    private final NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            assert mNsdManager != null;
            mNsdManager.resolveService(serviceInfo, mResolveListener);
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            if (mConnection != null && mConnection.getSockAddress().getAddress().equals(serviceInfo.getHost())) {
                mConnection.stop();
                mConnection = null;
            }
        }
    };

    /** NSD Manager resolver callback. */
    private final NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            if (mConnection == null) {
                mConnection = new Connection(new InetSocketAddress(serviceInfo.getHost(), serviceInfo.getPort()));
                mConnection.start();
            }
        }
    };

    /** Delay to honour after a connection attempt before trying again. */
    private static final long CONNECT_RETRY_DELAY = TimeUnit.SECONDS.toMillis(1);

    /** Mux ip connection . */
    private class Connection {

        /** USB Debug Bridge server address/port. */
        @NonNull
        private final InetSocketAddress mSockAddress;

        /** Thread where the automatic connection loop runs. */
        @Nullable
        private Thread mThread;

        /** Backend managing MUX discovery and device connections. */
        @Nullable
        private ArsdkMuxBackend mBackend;

        /**
         * Constructor.
         *
         * @param sockAddress usb debug bridge socket address
         */
        Connection(@NonNull InetSocketAddress sockAddress) {
            mSockAddress = sockAddress;
        }

        /**
         * Gets connection target socket address.
         *
         * @return connection target socket address
         */
        @NonNull
        InetSocketAddress getSockAddress() {
            return mSockAddress;
        }

        /**
         * Starts the connection.
         */
        void start() {
            mThread = new Thread(mConnectionLoop, "arsdk-debug-mux");
            mThread.start();
        }

        /**
         * Stops the connection.
         */
        void stop() {
            assert mThread != null;
            // Thread must be stopped before mux backend in order to properly shutdown the socket. Otherwise mux_stop
            // may block while joining the mux reader thread, itself blocked in a read
            try {
                mThread.interrupt();
                mThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                mThread = null;
            }

            if (mBackend != null) {
                onDisconnected();
            }
        }

        /**
         * Called back when the connection to USB debug bridge server has been successfully established.
         *
         * @param fd native file descriptor of the socket managing the connection
         */
        private void onConnected(int fd) {
            if (ULog.i(TAG_MUX)) {
                ULog.i(TAG_MUX, "Connected to debug bridge, starting MUX backend and discovery");
            }

            assert mArsdkCore != null;
            mBackend = new ArsdkMuxBackend(mArsdkCore, fd, mDiscoverableModels, mMuxEofListener);
            mBackend.startDiscovery();
        }

        /**
         * Called back when the connection to USB debug bridge server is closed.
         */
        private void onDisconnected() {
            if (ULog.i(TAG_MUX)) {
                ULog.i(TAG_MUX, "Disconnected from debug bridge, stopping MUX backend and discovery");
            }

            assert mBackend != null;
            mBackend.stopDiscovery();
            mBackend.destroy();
            mBackend = null;
        }

        /** Listener notified when the mux backend detects and EOF or error condition. */
        private final ArsdkMuxBackend.EofListener mMuxEofListener = new ArsdkMuxBackend.EofListener() {

            @Override
            public void onEof() {
                synchronized (this) {
                    notifyAll();
                }
            }
        };

        /** Runnable managing automatic (re/)connection to the USB debug bridge server. */
        private final Runnable mConnectionLoop = new Runnable() {

            @Override
            public void run() {
                assert mArsdkCore != null;
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Socket socket = null;
                        ParcelFileDescriptor parcelFd = null;
                        try {
                            // SocketChannel.open used for interrupt support
                            SocketChannel channel = SocketChannel.open(mSockAddress);
                            socket = channel.socket();
                            socket.setTcpNoDelay(true);
                            // Beware ParcelFileDescriptor.fromSocket documentation, which wrongly states that the
                            // returned ParcelFileDescriptor holds a 'dup' of the one in the socket. This is plain
                            // wrong; this is in fact the very same fd in both objects. As a consequence, parcelFd
                            // has to be kept away from GC for as long as the socket object (otherwise the internal
                            // fd would get closed)
                            parcelFd = ParcelFileDescriptor.fromSocket(socket);
                            int fd = parcelFd.getFd();

                            mArsdkCore.dispatchToPomp(() -> onConnected(fd));
                            // now wait until disconnection occurs
                            synchronized (mMuxEofListener) {
                                mMuxEofListener.wait();
                            }
                            mArsdkCore.dispatchToPomp(Connection.this::onDisconnected);
                        } catch (IOException | IllegalArgumentException ignored) {
                        } finally {
                            if (socket != null) {
                                try {
                                    // this will wake up the mux rx thread from a blocked read with an EOF if necessary.
                                    socket.shutdownInput();
                                } catch (IOException ignored) {
                                }
                                try {
                                    socket.shutdownOutput();
                                } catch (IOException ignored) {
                                }
                                try {
                                    socket.close();
                                    if (parcelFd != null) {
                                        // not very useful per se, just there to make sure parcelFd outlives socket
                                        // object.
                                        parcelFd.close();
                                    }
                                } catch (IOException ignored) {
                                }
                            }
                        }

                        Thread.sleep(CONNECT_RETRY_DELAY);
                    }
                } catch (InterruptedException ignored) {
                }
            }
        };
    }
}
