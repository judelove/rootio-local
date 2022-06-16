package org.rootio.services;

import net.sourceforge.peers.FileLogger;
import net.sourceforge.peers.media.MediaMode;
import net.sourceforge.peers.sdp.Codec;
import net.sourceforge.peers.sip.core.useragent.UserAgent;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import org.rootio.configuration.Configuration;
import org.rootio.launcher.Rootio;
import org.rootio.messaging.BroadcastReceiver;
import org.rootio.messaging.Message;
import org.rootio.messaging.MessageRouter;
import org.rootio.services.SIP.CallState;
import org.rootio.services.SIP.Listener;
import org.rootio.services.SIP.SipConfiguration;
import org.rootio.tools.utils.EventAction;
import org.rootio.tools.utils.EventCategory;
import org.rootio.tools.utils.Utils;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeerSIPService implements RootioService {
    private SipConfiguration sipConfig = new SipConfiguration();
    private SipRequest currentCallSipRequest;
    private UserAgent ua;
    private Listener lst;
    private Thread runnerThread;
    private int serviceId = 8;
    private boolean isRunning;

    @Override
    public void start() {
        Utils.logEvent(EventCategory.SERVICES, EventAction.START, "PeerSIP Service");
        runnerThread = new Thread(() -> {
            try {
                startListener();
            } catch (IOException e) {
                Logger.getLogger("RootIO").log(Level.WARNING, e.getMessage() == null ? "Null pointer[PeerSIPService.start]" : e.getMessage());
            }
        });
        runnerThread.start();
        this.isRunning = true;

        new ServiceState(serviceId, "PeerSIPService", 1).save();
        while (Rootio.isRunning()) {
            try {
                runnerThread.join();
            } catch (InterruptedException e) {
                if (!Rootio.isRunning()) {
                    stop();
                }
            }
        }
    }

    @Override
    public void stop() {
        if (Rootio.isInSIPCall()) {
            try {
                ua.terminate(currentCallSipRequest);
            } catch (Exception e) {
                Logger.getLogger("RootIO").log(Level.WARNING, e.getMessage() == null ? "Null pointer[PeerSIPService.loadConfiguration]" : e.getMessage());
            }
        }
        try {
            ua.unregister();
            ua.close();
        } catch (SipUriSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public int getServiceId() {
        return serviceId;
    }

    private void loadConfiguration() {
        sipConfig.setDomain(Configuration.getProperty("sip_server"));
        sipConfig.setUserPart(Configuration.getProperty("sip_username"));
        sipConfig.setPassword(Configuration.getProperty("sip_password"));
        try {
            sipConfig.setSipPort(0); //Integer.parseInt(Configuration.getProperty("sip_port", "5060")));
        } catch (NumberFormatException e) {
            Logger.getLogger("RootIO").log(Level.WARNING, e.getMessage() == null ? "Null pointer[PeerSIPService.loadConfiguration]" : e.getMessage());
            sipConfig.setSipPort(5060);
        }
        sipConfig.setMediaMode(MediaMode.captureAndPlayback);
        sipConfig.setLocalInetAddress(getLocalAddress());
        Codec ulaw = new Codec();
        ulaw.setPayloadType(0);
        ulaw.setName("PCMU");
        Codec alaw = new Codec();
        alaw.setPayloadType(0);
        alaw.setName("PCMA");
        sipConfig.setSupportedCodecs(Arrays.asList(new Codec[]{ulaw, alaw}));
    }

    private InetAddress getLocalAddress()
    {

        try {
            Thread.sleep(60000); //hack for reboot, waiting for network
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                try {
                    if (networkInterface.isUp() && !networkInterface.isVirtual() && !networkInterface.isLoopback()) {
                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress address = addresses.nextElement();
                            if (address instanceof Inet4Address) {
                                return address;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            Logger.getLogger("RootIO").log(Level.WARNING, e.getMessage() == null ? "Null pointer[PeerSIPService.loadConfiguration]" : e.getMessage());
        }
        return null;
    }

    private void startListener() throws IOException {
        lst = new Listener();
        try {
            loadConfiguration();
            ua = new UserAgent(lst, sipConfig, new FileLogger(null));
            lst.setController(PeerSIPService.this);
            try {
                while (Rootio.isRunning()) {
                    ua.register();
                    try {
                        Long sleepPeriod = 30l;
                        try {
                            sleepPeriod = Long.parseLong(Configuration.getProperty("sip_password", "30"));
                        } catch (NumberFormatException e) {
                            Logger.getLogger("RootIO").log(Level.WARNING, e.getMessage() == null ? "Null pointer[PeerSIPService.startListener]" : e.getMessage());
                        }
                        Thread.sleep(sleepPeriod * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (SipUriSyntaxException e) {
                e.printStackTrace();
            }

            listenForSIPConfigurationChange();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    public void notifyCallRinging(SipRequest request) {
        Utils.logEvent(EventCategory.SIP_CALL, EventAction.RINGING, request.getSipHeaders().get(new SipHeaderFieldName("From")).getValue());
        announceCallStatus(CallState.RINGING);
        answerCall(request);
    }

    private void answerCall(SipRequest sipRequest) {
        String callid = sipRequest.getSipHeaders().get(new SipHeaderFieldName("Call-ID")).getValue();
        DialogManager dm = ua.getDialogManager();
        Dialog dg = dm.getDialog(callid);
        ua.acceptCall(sipRequest, dg);
        notifyCallAnswer(sipRequest);
    }

    private void notifyCallAnswer(SipRequest request) {
        Utils.logEvent(EventCategory.SIP_CALL, EventAction.RECEIVE, request.getSipHeaders().get(new SipHeaderFieldName("From")).getValue());
        Rootio.setInSIPCall(true);
        currentCallSipRequest = request;
        announceCallStatus(CallState.INCALL);
    }

    public void notifyRegistering(SipRequest request) {
        Utils.logEvent(EventCategory.SIP_CALL, EventAction.REGISTRATION, request.getSipHeaders().get(new SipHeaderFieldName("From")).getValue());
    }

    public void notifyRegisteringSuccessful(SipResponse response) {
        Utils.logEvent(EventCategory.SIP_CALL, EventAction.REGISTRATION_FAILED, response.getSipHeaders().get(new SipHeaderFieldName("From")).getValue());
    }

    public void notifyRegisteringFailed(SipResponse response) {
        Utils.logEvent(EventCategory.SIP_CALL, EventAction.REGISTERED, response.getSipHeaders().get(new SipHeaderFieldName("From")).getValue());
    }

    public void notifyHangup(SipRequest request) {
        Rootio.setInSIPCall(false);
        Utils.logEvent(EventCategory.SIP_CALL, EventAction.STOP, request.getSipHeaders().get(new SipHeaderFieldName("From")).getValue());
        announceCallStatus(CallState.IDLE);
    }


    private void listenForSIPConfigurationChange() {
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Message m) {
                new Thread(() -> {
                    try {
                        loadConfiguration();
                        ua.unregister();
                        ua.register();
                    } catch (SipUriSyntaxException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        };
        MessageRouter.getInstance().register(br, "org.rootio.service.sip.CONFIGURATION");
    }

    private void announceCallStatus(CallState callState) {
        String filter = "org.rootio.services.sip.TELEPHONY";
        HashMap<String, Object> payLoad = new HashMap<>();
        payLoad.put("eventType", callState.name());
        Message message = new Message(callState.name(), "telephony", payLoad);
        MessageRouter.getInstance().specicast(message, filter);
    }
}
