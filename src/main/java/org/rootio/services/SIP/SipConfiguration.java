package org.rootio.services.SIP;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.media.MediaMode;
import net.sourceforge.peers.media.SoundSource;
import net.sourceforge.peers.sdp.Codec;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;

import java.net.InetAddress;
import java.util.List;

public class SipConfiguration implements Config {

    private MediaMode mediaMode;
    private boolean mediaDebug;
    private List<Codec> supportedCodecs;
    private InetAddress publicInetAddress, localInetAddress;
    private String userPart, domain, password, mediaFile, AuthorizationUsername;
    private int sipPort, rtpPort;
    private SipURI outboundProxy;
    private SoundSource.DataFormat mediaFileDataFormat;

    @Override
    public InetAddress getPublicInetAddress() {
        return publicInetAddress;
    }

    @Override
    public void setPublicInetAddress(InetAddress publicInetAddress) {
        this.publicInetAddress = publicInetAddress;
    }

    @Override
    public InetAddress getLocalInetAddress() {
        return localInetAddress;
    }

    @Override
    public void setLocalInetAddress(InetAddress localInetAddress) {
        this.localInetAddress = localInetAddress;
    }

    @Override
    public String getUserPart() {
        return userPart;
    }

    @Override
    public void setUserPart(String userPart) {
        this.userPart = userPart;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getMediaFile() {
        return mediaFile;
    }

    @Override
    public void setMediaFile(String mediaFile) {
        this.mediaFile = mediaFile;
    }

    @Override
    public String getAuthorizationUsername() {
        return AuthorizationUsername;
    }

    @Override
    public void setAuthorizationUsername(String authorizationUsername) {
        AuthorizationUsername = authorizationUsername;
    }

    @Override
    public int getSipPort() {
        return sipPort;
    }

    @Override
    public void setSipPort(int sipPort) {
        this.sipPort = sipPort;
    }

    @Override
    public int getRtpPort() {
        return rtpPort;
    }

    @Override
    public void setRtpPort(int rtpPort) {
        this.rtpPort = rtpPort;
    }

    @Override
    public SipURI getOutboundProxy() {
        return outboundProxy;
    }

    @Override
    public void setOutboundProxy(SipURI outboundProxy) {
        this.outboundProxy = outboundProxy;
    }

    @Override
    public MediaMode getMediaMode() {
        return mediaMode;
    }

    @Override
    public void setMediaMode(MediaMode mediaMode) {
        this.mediaMode = mediaMode;
    }

    @Override
    public boolean isMediaDebug() {
        return mediaDebug;
    }

    @Override
    public SoundSource.DataFormat getMediaFileDataFormat() {
        return this.mediaFileDataFormat;
    }

    @Override
    public void setMediaDebug(boolean mediaDebug) {
        this.mediaDebug = mediaDebug;
    }

    @Override
    public void setMediaFileDataFormat(SoundSource.DataFormat dataFormat) {
        this.mediaFileDataFormat = dataFormat;
    }

    public List<Codec> getSupportedCodecs() {
        return supportedCodecs;
    }

    public void setSupportedCodecs(List<Codec> supportedCodecs) {
        this.supportedCodecs = supportedCodecs;
    }

    @Override
    public void save() {

    }

}
