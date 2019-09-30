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

package com.parrot.drone.sdkcore.arsdk.backend.net.mdnssdmin;

import android.net.Network;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_MDNS;

/**
 * A minimal implementation of Mdns service discovery.
 * <p>
 * This implementation doesn't handle caching and time to live. It's specially customised to discover one single device
 * on the network. It heavily send queries to ensure the first device is discovered quickly.
 */
public final class MdnsSdMin {

    /**
     * Listener notified when a service has been found or is about to be removed.
     * <p>
     * Note: callback are called in an internal thread!
     */
    public interface Listener {

        /**
         * Notify that a service has been found.
         *
         * @param name        unqualified service name
         * @param serviceType service type
         * @param ipAddress   service ip address
         * @param port        service port
         * @param txtRecord   service additional data
         */
        void onServiceAdded(@NonNull String name, @NonNull String serviceType, @NonNull String ipAddress, int port,
                            @NonNull String[] txtRecord);

        /**
         * Notify that a device is about to be removed.
         *
         * @param name        unqualified service name
         * @param serviceType service type
         */
        void onServiceRemoved(@NonNull String name, @NonNull String serviceType);
    }

    /**
     * Network configuration with an net interface and a network.
     */
    public static class NetConfig {

        /** net interface. */
        @NonNull
        final NetworkInterface mNetInterface;

        /** Network. */
        @NonNull
        final Network mNetwork;

        /**
         * Constructor.
         *
         * @param netInterface network interface
         * @param network      network
         */
        public NetConfig(@NonNull NetworkInterface netInterface, @NonNull Network network) {
            mNetInterface = netInterface;
            mNetwork = network;
        }
    }

    /** Array of service to search for and notify. */
    @NonNull
    private final String[] mServices;

    /** Configuration. */
    @NonNull
    private final NetConfig mNetConfig;

    /** Prebuilt mdns mQuery. */
    @NonNull
    private final MdnsSdOutgoingQuery mQuery;

    /** Client mListener. */
    @NonNull
    private final Listener mListener;

    /** mdns multicast mSocket. */
    @Nullable
    private MulticastSocket mSocket;

    /** Thread listening on the multicast mSocket and decoding received mdns packets. */
    @Nullable
    private Thread mReceiveThread;

    /** Thread sending mdns queries. */
    @Nullable
    private HandlerThread mQueryThread;

    /** Handler of the mQueryThread. */
    @Nullable
    private Handler mQueryHandler;

    /** Interval between queries. */
    private static final int QUERY_INTERVAL_MS = 250;

    /** Duration to send queries for at startup. */
    private static final int QUERY_DURATION_MS = 5 * 1000;

    /** mdns multicast group address. */
    private static final String MDNS_MULTICAST_ADDR = "224.0.0.251";

    /** mdns multicast port. */
    private static final int MDNS_MULTICAST_PORT = 5353;

    /**
     * Constructor.
     *
     * @param services  array of mServices to look-for an notify
     * @param listener  listener notified when a service has been found or is about to be removed
     * @param netConfig configuration
     */
    public MdnsSdMin(@NonNull String[] services, @NonNull Listener listener, @NonNull NetConfig netConfig) {
        mServices = services;
        mListener = listener;
        mNetConfig = netConfig;
        // create the mQuery
        mQuery = new MdnsSdOutgoingQuery(services);
    }

    /**
     * Starts service discovery.
     */
    public void start() {
        if (ULog.d(TAG_MDNS)) {
            ULog.d(TAG_MDNS, "Starting mdnsSd");
        }
        if (mSocket == null) {
            // create multicast mSocket
            try {
                mSocket = new MulticastSocket(MDNS_MULTICAST_PORT);
                mSocket.setNetworkInterface(mNetConfig.mNetInterface);
                mNetConfig.mNetwork.bindSocket(mSocket);
                mSocket.joinGroup(
                        new InetSocketAddress(InetAddress.getByName(MDNS_MULTICAST_ADDR), MDNS_MULTICAST_PORT),
                        mNetConfig.mNetInterface);
                mSocket.setTimeToLive(255);
                // start the receiver thread
                mReceiveThread = new ReceiverThread(mSocket);
                mReceiveThread.start();
                // start the mQuery thread
                mQueryThread = new QueryThread(mSocket);
                mQueryThread.start();
            } catch (IOException e) {
                ULog.e(TAG_MDNS, "unable to start MdnsSd", e);
            }
        }
    }

    /**
     * Stops service discovery.
     */
    public void stop() {
        if (ULog.d(TAG_MDNS)) {
            ULog.d(TAG_MDNS, "Stopping MdnsSd");
        }
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
            mReceiveThread = null;
            if (mQueryThread != null) {
                mQueryThread.quit();
                mQueryThread = null;
            }
        }
    }

    /**
     * Cancel sending queries previously queued by {@link #sendQueries}.
     */
    public void cancelSendQueries() {
        if (ULog.d(TAG_MDNS)) {
            ULog.d(TAG_MDNS, "Cancel sending queries");
        }
        if (mQueryHandler != null) {
            mQueryHandler.removeMessages(0);
        }
    }

    /**
     * Send queries to discover mServices.
     * <p>
     * This methods send a lot of queries at regular interval. Pending queries can be cancelled by calling {@link
     * #cancelSendQueries}
     */
    private void sendQueries() {
        if (ULog.d(TAG_MDNS)) {
            ULog.d(TAG_MDNS, "Sending queries");
        }
        if (mQueryHandler != null) {
            for (int t = 0; t < QUERY_DURATION_MS; t += QUERY_INTERVAL_MS) {
                mQueryHandler.sendMessageDelayed(mQueryHandler.obtainMessage(0), t);
            }
        }
    }

    /**
     * Thread receiving mdns udp packets.
     */
    private class ReceiverThread extends Thread {

        /** UDP mSocket. */
        @NonNull
        private final DatagramSocket mSocket;

        /**
         * Constructor.
         *
         * @param socket udp mSocket (already opened) to use
         */
        ReceiverThread(@NonNull DatagramSocket socket) {
            super("MdnsSd-receiver");
            mSocket = socket;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1500];
            while (!mSocket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    mSocket.receive(packet);
                    MdnsSdIncomingResponse r = new MdnsSdIncomingResponse(packet.getData());
                    handleResponse(r);
                } catch (Throwable e) {
                    // Catch all exceptions, to protect against bad dns packets
                    ULog.w(TAG_MDNS, "Ignoring received packet due to " + e.getMessage());
                }
            }
        }

        /**
         * Handled a mdns response .
         *
         * @param response the response message
         */
        private void handleResponse(@NonNull MdnsSdIncomingResponse response) {
            // iterate expected PTRs
            for (String question : mServices) {
                String ptr = response.getPtr(question);
                if (ptr != null) {
                    // found a ptr, the corresponding service
                    MdnsSrvData srv = response.getService(ptr);
                    if (srv != null) {
                        String address = response.getAddress(srv.getTarget());
                        String[] txtRecords = response.getTexts(ptr);
                        if ((address != null) && (txtRecords != null)) {
                            // ptr is the full qualified name. extract device and service name
                            int pos = -1;
                            if (ptr.endsWith(question)) {
                                pos = ptr.length() - question.length();
                            }
                            String name = ptr.substring(0, pos > 0 ? pos - 1 : ptr.length());
                            if (srv.getTtl() > 0) {
                                if (ULog.i(TAG_MDNS)) {
                                    ULog.i(TAG_MDNS, "New service " + name);
                                }
                                mListener.onServiceAdded(name, question, address, srv.getPort(), txtRecords);
                            } else {
                                if (ULog.d(TAG_MDNS)) {
                                    ULog.d(TAG_MDNS, "Service removed " + name);
                                }
                                mListener.onServiceRemoved(name, question);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Thread used to send queries.
     */
    private class QueryThread extends HandlerThread {

        /** UDP mSocket. */
        @NonNull
        private final DatagramSocket mSocket;

        /**
         * Constructor.
         *
         * @param socket udp mSocket (already opened) to use
         */
        QueryThread(@NonNull DatagramSocket socket) {
            super("MdnsSd-mQuery");
            mSocket = socket;
        }

        @Override
        protected void onLooperPrepared() {
            if (mSocket.isClosed()) {
                // mSocket has been closed, exit the looper
                getLooper().quit();
            } else {
                mQueryHandler = new Handler(getLooper()) {

                    @Override
                    public void handleMessage(Message msg) {
                        try {
                            byte[] buf = mQuery.encode();
                            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                                    InetAddress.getByName(MDNS_MULTICAST_ADDR), MDNS_MULTICAST_PORT);
                            mSocket.send(packet);
                        } catch (IOException e) {
                            ULog.e(TAG_MDNS, "unable to start mQuery", e);
                        }
                    }
                };
                // do the first mQuery
                sendQueries();
            }
        }
    }
}
