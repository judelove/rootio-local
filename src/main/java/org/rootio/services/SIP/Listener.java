package org.rootio.services.SIP;

import net.sourceforge.peers.rtp.RFC4733;
import net.sourceforge.peers.sip.core.useragent.SipListener;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import org.rootio.services.PeerSIPService;


public class Listener implements SipListener {
    private PeerSIPService controller;

    public void setController(PeerSIPService controller)
    {
        this.controller = controller;
    }

    @Override
    public void registering(SipRequest sipRequest) {
        controller.notifyRegistering(sipRequest);
    }

    @Override
    public void registerSuccessful(SipResponse sipResponse) {
        controller.notifyRegisteringSuccessful(sipResponse);    }

    @Override
    public void registerFailed(SipResponse sipResponse) {
        controller.notifyRegisteringFailed(sipResponse);    }

    @Override
    public void incomingCall(SipRequest sipRequest, SipResponse sipResponse) {
        controller.notifyCallRinging(sipRequest);
    }

    @Override
    public void remoteHangup(SipRequest sipRequest) {
        controller.notifyHangup(sipRequest);
    }

    @Override
    public void ringing(SipResponse sipResponse) {

    }

    @Override
    public void calleePickup(SipResponse sipResponse) {

    }

    @Override
    public void error(SipResponse sipResponse) {

    }

    @Override
    public void dtmfEvent(RFC4733.DTMFEvent dtmfEvent, int i) {
        System.out.println("DTMF: " + dtmfEvent.getValue());
    }
}
