/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.amnezia.awg.config;

import org.amnezia.awg.config.BadConfigException.Location;
import org.amnezia.awg.config.BadConfigException.Reason;
import org.amnezia.awg.config.BadConfigException.Section;
import org.amnezia.awg.crypto.Key;
import org.amnezia.awg.crypto.KeyFormatException;
import org.amnezia.awg.crypto.KeyPair;
import org.amnezia.awg.util.NonNullForAll;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.Nullable;

/**
 * Represents the configuration for an AmneziaWG interface (an [Interface] block). Interfaces must
 * have a private key (used to initialize a {@code KeyPair}), and may optionally have several other
 * attributes.
 * <p>
 * Instances of this class are immutable.
 */
@NonNullForAll
public final class Interface {
    private static final int MAX_UDP_PORT = 65535;
    private static final int MIN_UDP_PORT = 0;

    private final Set<InetNetwork> addresses;
    private final Set<InetAddress> dnsServers;
    private final Set<String> dnsSearchDomains;
    private final KeyPair keyPair;
    @Nullable private final Integer listenPort;
    @Nullable private final Integer mtu;
    @Nullable private final Integer junkPacketCount;
    @Nullable private final Integer junkPacketMinSize;
    @Nullable private final Integer junkPacketMaxSize;
    @Nullable private final Integer initPacketJunkSize;
    @Nullable private final Integer responsePacketJunkSize;
    @Nullable private final Integer cookieReplyPacketJunkSize;
    @Nullable private final Integer transportPacketJunkSize;
    @Nullable private final String initPacketMagicHeader;
    @Nullable private final String responsePacketMagicHeader;
    @Nullable private final String underloadPacketMagicHeader;
    @Nullable private final String transportPacketMagicHeader;
    @Nullable private final String specialJunkI1;
    @Nullable private final String specialJunkI2;
    @Nullable private final String specialJunkI3;
    @Nullable private final String specialJunkI4;
    @Nullable private final String specialJunkI5;

    private Interface(final Builder builder) {
        // Defensively copy to ensure immutability even if the Builder is reused.
        addresses = Collections.unmodifiableSet(new LinkedHashSet<>(builder.addresses));
        dnsServers = Collections.unmodifiableSet(new LinkedHashSet<>(builder.dnsServers));
        dnsSearchDomains = Collections.unmodifiableSet(new LinkedHashSet<>(builder.dnsSearchDomains));
        keyPair = Objects.requireNonNull(builder.keyPair, "Interfaces must have a private key");
        listenPort = builder.listenPort;
        mtu = builder.mtu;
        junkPacketCount = builder.junkPacketCount;
        junkPacketMinSize = builder.junkPacketMinSize;
        junkPacketMaxSize = builder.junkPacketMaxSize;
        initPacketJunkSize = builder.initPacketJunkSize;
        responsePacketJunkSize = builder.responsePacketJunkSize;
        cookieReplyPacketJunkSize = builder.cookieReplyPacketJunkSize;
        transportPacketJunkSize = builder.transportPacketJunkSize;
        initPacketMagicHeader = builder.initPacketMagicHeader;
        responsePacketMagicHeader = builder.responsePacketMagicHeader;
        underloadPacketMagicHeader = builder.underloadPacketMagicHeader;
        transportPacketMagicHeader = builder.transportPacketMagicHeader;
        specialJunkI1 = builder.specialJunkI1;
        specialJunkI2 = builder.specialJunkI2;
        specialJunkI3 = builder.specialJunkI3;
        specialJunkI4 = builder.specialJunkI4;
        specialJunkI5 = builder.specialJunkI5;
    }

    /**
     * Parses an series of "KEY = VALUE" lines into an {@code Interface}. Throws
     * {@link ParseException} if the input is not well-formed or contains unknown attributes.
     *
     * @param lines An iterable sequence of lines, containing at least a private key attribute
     * @return An {@code Interface} with all of the attributes from {@code lines} set
     */
    public static Interface parse(final Iterable<? extends CharSequence> lines)
            throws BadConfigException {
        final Builder builder = new Builder();
        for (final CharSequence line : lines) {
            final Attribute attribute = Attribute.parse(line);
            if (attribute == null)
                throw new BadConfigException(Section.INTERFACE, Location.TOP_LEVEL,
                        Reason.SYNTAX_ERROR, line);
            switch (attribute.getKey().toLowerCase(Locale.ENGLISH)) {
                case "address":
                    builder.parseAddresses(attribute.getValue());
                    break;
                case "dns":
                    builder.parseDnsServers(attribute.getValue());
                    break;
                case "excludedapplications":
                case "includedapplications":
                    break;
                case "listenport":
                    builder.parseListenPort(attribute.getValue());
                    break;
                case "mtu":
                    builder.parseMtu(attribute.getValue());
                    break;
                case "privatekey":
                    builder.parsePrivateKey(attribute.getValue());
                    break;
                case "jc":
                    builder.parseJunkPacketCount(attribute.getValue());
                    break;
                case "jmin":
                    builder.parseJunkPacketMinSize(attribute.getValue());
                    break;
                case "jmax":
                    builder.parseJunkPacketMaxSize(attribute.getValue());
                    break;
                case "s1":
                    builder.parseInitPacketJunkSize(attribute.getValue());
                    break;
                case "s2":
                    builder.parseResponsePacketJunkSize(attribute.getValue());
                    break;
                case "s3":
                    builder.parseCookieReplyPacketJunkSize(attribute.getValue());
                    break;
                case "s4":
                    builder.parseTransportPacketJunkSize(attribute.getValue());
                    break;
                case "h1":
                    builder.parseInitPacketMagicHeader(attribute.getValue());
                    break;
                case "h2":
                    builder.parseResponsePacketMagicHeader(attribute.getValue());
                    break;
                case "h3":
                    builder.parseUnderloadPacketMagicHeader(attribute.getValue());
                    break;
                case "h4":
                    builder.parseTransportPacketMagicHeader(attribute.getValue());
                    break;
                case "i1":
                    builder.parseSpecialJunkI1(attribute.getValue());
                    break;
                case "i2":
                    builder.parseSpecialJunkI2(attribute.getValue());
                    break;
                case "i3":
                    builder.parseSpecialJunkI3(attribute.getValue());
                    break;
                case "i4":
                    builder.parseSpecialJunkI4(attribute.getValue());
                    break;
                case "i5":
                    builder.parseSpecialJunkI5(attribute.getValue());
                    break;
                default:
                    throw new BadConfigException(Section.INTERFACE, Location.TOP_LEVEL,
                            Reason.UNKNOWN_ATTRIBUTE, attribute.getKey());
            }
        }
        return builder.build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Interface))
            return false;
        final Interface other = (Interface) obj;
        return addresses.equals(other.addresses)
                && dnsServers.equals(other.dnsServers)
                && dnsSearchDomains.equals(other.dnsSearchDomains)
                && keyPair.equals(other.keyPair)
                && Objects.equals(listenPort, other.listenPort)
                && Objects.equals(mtu, other.mtu)
                && Objects.equals(junkPacketCount, other.junkPacketCount)
                && Objects.equals(junkPacketMinSize, other.junkPacketMinSize)
                && Objects.equals(junkPacketMaxSize, other.junkPacketMaxSize)
                && Objects.equals(initPacketJunkSize, other.initPacketJunkSize)
                && Objects.equals(responsePacketJunkSize, other.responsePacketJunkSize)
                && Objects.equals(cookieReplyPacketJunkSize, other.cookieReplyPacketJunkSize)
                && Objects.equals(transportPacketJunkSize, other.transportPacketJunkSize)
                && Objects.equals(initPacketMagicHeader, other.initPacketMagicHeader)
                && Objects.equals(responsePacketMagicHeader, other.responsePacketMagicHeader)
                && Objects.equals(underloadPacketMagicHeader, other.underloadPacketMagicHeader)
                && Objects.equals(transportPacketMagicHeader, other.transportPacketMagicHeader)
                && Objects.equals(specialJunkI1, other.specialJunkI1)
                && Objects.equals(specialJunkI2, other.specialJunkI2)
                && Objects.equals(specialJunkI3, other.specialJunkI3)
                && Objects.equals(specialJunkI4, other.specialJunkI4)
                && Objects.equals(specialJunkI5, other.specialJunkI5);
    }

    /**
     * Returns the set of IP addresses assigned to the interface.
     *
     * @return a set of {@link InetNetwork}s
     */
    public Set<InetNetwork> getAddresses() {
        // The collection is already immutable.
        return addresses;
    }

    /**
     * Returns the set of DNS servers associated with the interface.
     *
     * @return a set of {@link InetAddress}es
     */
    public Set<InetAddress> getDnsServers() {
        // The collection is already immutable.
        return dnsServers;
    }

    /**
     * Returns the set of DNS search domains associated with the interface.
     *
     * @return a set of strings
     */
    public Set<String> getDnsSearchDomains() {
        // The collection is already immutable.
        return dnsSearchDomains;
    }

    /**
     * Returns the public/private key pair used by the interface.
     *
     * @return a key pair
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * Returns the UDP port number that the AmneziaWG interface will listen on.
     *
     * @return a UDP port number, or {@code null} if none is configured
     */
    @Nullable
    public Integer getListenPort() {
        return listenPort;
    }

    public boolean hasListenPort() {
        return listenPort != null;
    }

    /**
     * Returns the MTU used for the AmneziaWG interface.
     *
     * @return the MTU, or {@code null} if none is configured
     */
    @Nullable
    public Integer getMtu() {
        return mtu;
    }

    public boolean hasMtu() {
        return mtu != null;
    }

    /**
     * Returns the junkPacketCount used for the AmneziaWG interface.
     *
     * @return the junkPacketCount, or {@code null} if none is configured
     */
    @Nullable
    public Integer getJunkPacketCount() {
        return junkPacketCount;
    }

    public boolean hasJunkPacketCount() {
        return junkPacketCount != null;
    }

    /**
     * Returns the junkPacketMinSize used for the AmneziaWG interface.
     *
     * @return the junkPacketMinSize, or {@code null} if none is configured
     */
    @Nullable
    public Integer getJunkPacketMinSize() {
        return junkPacketMinSize;
    }

    public boolean hasJunkPacketMinSize() {
        return junkPacketMinSize != null;
    }

    /**
     * Returns the junkPacketMaxSize used for the AmneziaWG interface.
     *
     * @return the junkPacketMaxSize, or {@code null} if none is configured
     */
    @Nullable
    public Integer getJunkPacketMaxSize() {
        return junkPacketMaxSize;
    }

    public boolean hasJunkPacketMaxSize() {
        return junkPacketMaxSize != null;
    }

    /**
     * Returns the initPacketJunkSize used for the AmneziaWG interface.
     *
     * @return the initPacketJunkSize, or {@code null} if none is configured
     */
    @Nullable
    public Integer getInitPacketJunkSize() {
        return initPacketJunkSize;
    }

    public boolean hasInitPacketJunkSize() {
        return initPacketJunkSize != null;
    }

    /**
     * Returns the responsePacketJunkSize used for the AmneziaWG interface.
     *
     * @return the responsePacketJunkSize, or {@code null} if none is configured
     */
    @Nullable
    public Integer getResponsePacketJunkSize() {
        return responsePacketJunkSize;
    }

    public boolean hasResponsePacketJunkSize() {
        return responsePacketJunkSize != null;
    }

    /**
     * Returns the cookieReplyPacketJunkSize used for the AmneziaWG interface.
     *
     * @return the cookieReplyPacketJunkSize, or {@code null} if none is configured
     */
    @Nullable
    public Integer getCookieReplyPacketJunkSize() {
        return cookieReplyPacketJunkSize;
    }

    public boolean hasCookieReplyPacketJunkSize() {
        return cookieReplyPacketJunkSize != null;
    }

    /**
     * Returns the transportPacketJunkSize used for the AmneziaWG interface.
     *
     * @return the transportPacketJunkSize, or {@code null} if none is configured
     */
    @Nullable
    public Integer getTransportPacketJunkSize() {
        return transportPacketJunkSize;
    }

    public boolean hasTransportPacketJunkSize() {
        return transportPacketJunkSize != null;
    }

    /**
     * Returns the initPacketMagicHeader used for the AmneziaWG interface.
     *
     * @return the initPacketMagicHeader, or {@code null} if none is configured
     */
    @Nullable
    public String getInitPacketMagicHeader() {
        return initPacketMagicHeader;
    }

    public boolean hasInitPacketMagicHeader() {
        return initPacketMagicHeader != null;
    }

    /**
     * Returns the responsePacketMagicHeader used for the AmneziaWG interface.
     *
     * @return the responsePacketMagicHeader, or {@code null} if none is configured
     */
    @Nullable
    public String getResponsePacketMagicHeader() {
        return responsePacketMagicHeader;
    }

    public boolean hasResponsePacketMagicHeader() {
        return responsePacketMagicHeader != null;
    }

    /**
     * Returns the underloadPacketMagicHeader used for the AmneziaWG interface.
     *
     * @return the underloadPacketMagicHeader, or {@code null} if none is configured
     */
    @Nullable
    public String getUnderloadPacketMagicHeader() {
        return underloadPacketMagicHeader;
    }

    public boolean hasUnderloadPacketMagicHeader() {
        return underloadPacketMagicHeader != null;
    }

    /**
     * Returns the transportPacketMagicHeader used for the AmneziaWG interface.
     *
     * @return the transportPacketMagicHeader, or {@code null} if none is configured
     */
    @Nullable
    public String getTransportPacketMagicHeader() {
        return transportPacketMagicHeader;
    }

    public boolean hasTransportPacketMagicHeader() {
        return transportPacketMagicHeader != null;
    }

    /**
     * Returns the specialJunkI1 used for the AmneziaWG interface.
     *
     * @return the specialJunkI1, or {@code null} if none is configured
     */
    @Nullable
    public String getSpecialJunkI1() {
        return specialJunkI1;
    }

    public boolean hasSpecialJunkI1() {
        return specialJunkI1 != null;
    }

    /**
     * Returns the specialJunkI2 used for the AmneziaWG interface.
     *
     * @return the specialJunkI2, or {@code null} if none is configured
     */
    @Nullable
    public String getSpecialJunkI2() {
        return specialJunkI2;
    }

    public boolean hasSpecialJunkI2() {
        return specialJunkI2 != null;
    }

    /**
     * Returns the specialJunkI3 used for the AmneziaWG interface.
     *
     * @return the specialJunkI3, or {@code null} if none is configured
     */
    @Nullable
    public String getSpecialJunkI3() {
        return specialJunkI3;
    }

    public boolean hasSpecialJunkI3() {
        return specialJunkI3 != null;
    }

    /**
     * Returns the specialJunkI4 used for the AmneziaWG interface.
     *
     * @return the specialJunkI4, or {@code null} if none is configured
     */
    @Nullable
    public String getSpecialJunkI4() {
        return specialJunkI4;
    }

    public boolean hasSpecialJunkI4() {
        return specialJunkI4 != null;
    }

    /**
     * Returns the specialJunkI5 used for the AmneziaWG interface.
     *
     * @return the specialJunkI5, or {@code null} if none is configured
     */
    @Nullable
    public String getSpecialJunkI5() {
        return specialJunkI5;
    }

    public boolean hasSpecialJunkI5() {
        return specialJunkI5 != null;
    }


    @Override
    public int hashCode() {
        int hash = 1;
        hash = 31 * hash + addresses.hashCode();
        hash = 31 * hash + dnsServers.hashCode();
        hash = 31 * hash + keyPair.hashCode();
        hash = 31 * hash + Objects.hashCode(listenPort);
        hash = 31 * hash + Objects.hashCode(mtu);
        hash = 31 * hash + Objects.hashCode(junkPacketCount);
        hash = 31 * hash + Objects.hashCode(junkPacketMinSize);
        hash = 31 * hash + Objects.hashCode(junkPacketMaxSize);
        hash = 31 * hash + Objects.hashCode(initPacketJunkSize);
        hash = 31 * hash + Objects.hashCode(responsePacketJunkSize);
        hash = 31 * hash + Objects.hashCode(cookieReplyPacketJunkSize);
        hash = 31 * hash + Objects.hashCode(transportPacketJunkSize);
        hash = 31 * hash + Objects.hashCode(initPacketMagicHeader);
        hash = 31 * hash + Objects.hashCode(responsePacketMagicHeader);
        hash = 31 * hash + Objects.hashCode(underloadPacketMagicHeader);
        hash = 31 * hash + Objects.hashCode(transportPacketMagicHeader);
        hash = 31 * hash + Objects.hashCode(specialJunkI1);
        hash = 31 * hash + Objects.hashCode(specialJunkI2);
        hash = 31 * hash + Objects.hashCode(specialJunkI3);
        hash = 31 * hash + Objects.hashCode(specialJunkI4);
        hash = 31 * hash + Objects.hashCode(specialJunkI5);
        return hash;
    }

    /**
     * Converts the {@code Interface} into a string suitable for debugging purposes. The {@code
     * Interface} is identified by its public key and (if set) the port used for its UDP socket.
     *
     * @return A concise single-line identifier for the {@code Interface}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("(Interface ");
        sb.append(keyPair.getPublicKey().toBase64());
        if (listenPort != null)
            sb.append(" @").append(listenPort);
        sb.append(')');
        return sb.toString();
    }

    /**
     * Converts the {@code Interface} into a string suitable for inclusion in a {@code awg-quick}
     * configuration file.
     *
     * @return The {@code Interface} represented as a series of "Key = Value" lines
     */
    public String toAwgQuickString() {
        final StringBuilder sb = new StringBuilder();
        if (!addresses.isEmpty())
            sb.append("Address = ").append(Attribute.join(addresses)).append('\n');
        if (!dnsServers.isEmpty()) {
            final List<String> dnsServerStrings = new ArrayList<>();
            for (final InetAddress dnsServer : dnsServers)
                dnsServerStrings.add(dnsServer.getHostAddress());
            dnsServerStrings.addAll(dnsSearchDomains);
            sb.append("DNS = ").append(Attribute.join(dnsServerStrings)).append('\n');
        }
        if (listenPort != null)
            sb.append("ListenPort = ").append(listenPort).append('\n');
        if (mtu != null)
            sb.append("MTU = ").append(mtu).append('\n');
        if (junkPacketCount != null)
            sb.append("Jc = ").append(junkPacketCount).append('\n');
        if (junkPacketMinSize != null)
            sb.append("Jmin = ").append(junkPacketMinSize).append('\n');
        if (junkPacketMaxSize != null)
            sb.append("Jmax = ").append(junkPacketMaxSize).append('\n');
        if (initPacketJunkSize != null)
            sb.append("S1 = ").append(initPacketJunkSize).append('\n');
        if (responsePacketJunkSize != null)
            sb.append("S2 = ").append(responsePacketJunkSize).append('\n');
        if (cookieReplyPacketJunkSize != null)
            sb.append("S3 = ").append(cookieReplyPacketJunkSize).append('\n');
        if (transportPacketJunkSize != null)
            sb.append("S4 = ").append(transportPacketJunkSize).append('\n');
        if (initPacketMagicHeader != null)
            sb.append("H1 = ").append(initPacketMagicHeader).append('\n');
        if (responsePacketMagicHeader != null)
            sb.append("H2 = ").append(responsePacketMagicHeader).append('\n');
        if (underloadPacketMagicHeader != null)
            sb.append("H3 = ").append(underloadPacketMagicHeader).append('\n');
        if (transportPacketMagicHeader != null)
            sb.append("H4 = ").append(transportPacketMagicHeader).append('\n');
        if (specialJunkI1 != null)
            sb.append("I1 = ").append(specialJunkI1).append('\n');
        if (specialJunkI2 != null)
            sb.append("I2 = ").append(specialJunkI2).append('\n');
        if (specialJunkI3 != null)
            sb.append("I3 = ").append(specialJunkI3).append('\n');
        if (specialJunkI4 != null)
            sb.append("I4 = ").append(specialJunkI4).append('\n');
        if (specialJunkI5 != null)
            sb.append("I5 = ").append(specialJunkI5).append('\n');
        sb.append("PrivateKey = ").append(keyPair.getPrivateKey().toBase64()).append('\n');
        return sb.toString();
    }

    /**
     * Serializes the {@code Interface} for use with the AmneziaWG cross-platform userspace API.
     * Note that not all attributes are included in this representation.
     *
     * @return the {@code Interface} represented as a series of "KEY=VALUE" lines
     */
    public String toAwgUserspaceString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("private_key=").append(keyPair.getPrivateKey().toHex()).append('\n');
        if (listenPort != null)
            sb.append("listen_port=").append(listenPort).append('\n');
        if (junkPacketCount != null)
            sb.append("jc=").append(junkPacketCount).append('\n');
        if (junkPacketMinSize != null)
            sb.append("jmin=").append(junkPacketMinSize).append('\n');
        if (junkPacketMaxSize != null)
            sb.append("jmax=").append(junkPacketMaxSize).append('\n');
        if (initPacketJunkSize != null)
            sb.append("s1=").append(initPacketJunkSize).append('\n');
        if (responsePacketJunkSize != null)
            sb.append("s2=").append(responsePacketJunkSize).append('\n');
        if (cookieReplyPacketJunkSize != null)
            sb.append("s3=").append(cookieReplyPacketJunkSize).append('\n');
        if (transportPacketJunkSize != null)
            sb.append("s4=").append(transportPacketJunkSize).append('\n');
        if (initPacketMagicHeader != null)
            sb.append("h1=").append(initPacketMagicHeader).append('\n');
        if (responsePacketMagicHeader != null)
            sb.append("h2=").append(responsePacketMagicHeader).append('\n');
        if (underloadPacketMagicHeader != null)
            sb.append("h3=").append(underloadPacketMagicHeader).append('\n');
        if (transportPacketMagicHeader != null)
            sb.append("h4=").append(transportPacketMagicHeader).append('\n');
        if (specialJunkI1 != null)
            sb.append("i1=").append(specialJunkI1).append('\n');
        if (specialJunkI2 != null)
            sb.append("i2=").append(specialJunkI2).append('\n');
        if (specialJunkI3 != null)
            sb.append("i3=").append(specialJunkI3).append('\n');
        if (specialJunkI4 != null)
            sb.append("i4=").append(specialJunkI4).append('\n');
        if (specialJunkI5 != null)
            sb.append("i5=").append(specialJunkI5).append('\n');
        return sb.toString();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        // Defaults to an empty set.
        private final Set<InetNetwork> addresses = new LinkedHashSet<>();
        // Defaults to an empty set.
        private final Set<InetAddress> dnsServers = new LinkedHashSet<>();
        // Defaults to an empty set.
        private final Set<String> dnsSearchDomains = new LinkedHashSet<>();
        // No default; must be provided before building.
        @Nullable private KeyPair keyPair;
        // Defaults to not present.
        @Nullable private Integer listenPort;
        // Defaults to not present.
        @Nullable private Integer mtu;
        // Defaults to not present.
        @Nullable private Integer junkPacketCount;
        // Defaults to not present.
        @Nullable private Integer junkPacketMinSize;
        // Defaults to not present.
        @Nullable private Integer junkPacketMaxSize;
        // Defaults to not present.
        @Nullable private Integer initPacketJunkSize;
        // Defaults to not present.
        @Nullable private Integer responsePacketJunkSize;
        // Defaults to not present.
        @Nullable private Integer cookieReplyPacketJunkSize;
        // Defaults to not present.
        @Nullable private Integer transportPacketJunkSize;
        // Defaults to not present.
        @Nullable private String initPacketMagicHeader;
        // Defaults to not present.
        @Nullable private String responsePacketMagicHeader;
        // Defaults to not present.
        @Nullable private String underloadPacketMagicHeader;
        // Defaults to not present.
        @Nullable private String transportPacketMagicHeader;
        // Defaults to not present.
        @Nullable private String specialJunkI1;
        // Defaults to not present.
        @Nullable private String specialJunkI2;
        // Defaults to not present.
        @Nullable private String specialJunkI3;
        // Defaults to not present.
        @Nullable private String specialJunkI4;
        // Defaults to not present.
        @Nullable private String specialJunkI5;


        public Builder addAddress(final InetNetwork address) {
            addresses.add(address);
            return this;
        }

        public Builder addAddresses(final Collection<InetNetwork> addresses) {
            this.addresses.addAll(addresses);
            return this;
        }

        public Builder addDnsServer(final InetAddress dnsServer) {
            dnsServers.add(dnsServer);
            return this;
        }

        public Builder addDnsServers(final Collection<? extends InetAddress> dnsServers) {
            this.dnsServers.addAll(dnsServers);
            return this;
        }

        public Builder addDnsSearchDomain(final String dnsSearchDomain) {
            dnsSearchDomains.add(dnsSearchDomain);
            return this;
        }

        public Builder addDnsSearchDomains(final Collection<String> dnsSearchDomains) {
            this.dnsSearchDomains.addAll(dnsSearchDomains);
            return this;
        }

        public Interface build() throws BadConfigException {
            if (keyPair == null)
                throw new BadConfigException(Section.INTERFACE, Location.PRIVATE_KEY,
                        Reason.MISSING_ATTRIBUTE, null);
            return new Interface(this);
        }

        public Builder parseAddresses(final CharSequence addresses) throws BadConfigException {
            try {
                for (final String address : Attribute.split(addresses))
                    addAddress(InetNetwork.parse(address));
                return this;
            } catch (final ParseException e) {
                throw new BadConfigException(Section.INTERFACE, Location.ADDRESS, e);
            }
        }

        public Builder parseDnsServers(final CharSequence dnsServers) throws BadConfigException {
            try {
                for (final String dnsServer : Attribute.split(dnsServers)) {
                    try {
                        addDnsServer(InetAddresses.parse(dnsServer));
                    } catch (final ParseException e) {
                        if (e.getParsingClass() != InetAddress.class || !InetAddresses.isHostname(dnsServer))
                            throw e;
                        addDnsSearchDomain(dnsServer);
                    }
                }
                return this;
            } catch (final ParseException e) {
                throw new BadConfigException(Section.INTERFACE, Location.DNS, e);
            }
        }

        public Builder parseListenPort(final String listenPort) throws BadConfigException {
            try {
                return setListenPort(Integer.parseInt(listenPort));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.LISTEN_PORT, listenPort, e);
            }
        }

        public Builder parseMtu(final String mtu) throws BadConfigException {
            try {
                return setMtu(Integer.parseInt(mtu));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.MTU, mtu, e);
            }
        }

        public Builder parseJunkPacketCount(final String junkPacketCount) throws BadConfigException {
            try {
                return setJunkPacketCount(Integer.parseInt(junkPacketCount));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.JUNK_PACKET_COUNT, junkPacketCount, e);
            }
        }

        public Builder parseJunkPacketMinSize(final String junkPacketMinSize) throws BadConfigException {
            try {
                return setJunkPacketMinSize(Integer.parseInt(junkPacketMinSize));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.JUNK_PACKET_MIN_SIZE, junkPacketMinSize, e);
            }
        }

        public Builder parseJunkPacketMaxSize(final String junkPacketMaxSize) throws BadConfigException {
            try {
                return setJunkPacketMaxSize(Integer.parseInt(junkPacketMaxSize));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.JUNK_PACKET_MAX_SIZE, junkPacketMaxSize, e);
            }
        }

        public Builder parseInitPacketJunkSize(final String initPacketJunkSize) throws BadConfigException {
            try {
                return setInitPacketJunkSize(Integer.parseInt(initPacketJunkSize));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.INIT_PACKET_JUNK_SIZE, initPacketJunkSize, e);
            }
        }

        public Builder parseResponsePacketJunkSize(final String responsePacketJunkSize) throws BadConfigException {
            try {
                return setResponsePacketJunkSize(Integer.parseInt(responsePacketJunkSize));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.RESPONSE_PACKET_JUNK_SIZE, responsePacketJunkSize, e);
            }
        }

        public Builder parseCookieReplyPacketJunkSize(final String cookieReplyPacketJunkSize) throws BadConfigException {
            try {
                return setCookieReplyPacketJunkSize(Integer.parseInt(cookieReplyPacketJunkSize));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.COOKIE_REPLY_PACKET_JUNK_SIZE, cookieReplyPacketJunkSize, e);
            }
        }

        public Builder parseTransportPacketJunkSize(final String transportPacketJunkSize) throws BadConfigException {
            try {
                return setTransportPacketJunkSize(Integer.parseInt(transportPacketJunkSize));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.TRANSPORT_PACKET_JUNK_SIZE, transportPacketJunkSize, e);
            }
        }

        public Builder parseInitPacketMagicHeader(final String initPacketMagicHeader) throws BadConfigException {
            if (initPacketMagicHeader == null || initPacketMagicHeader.trim().isEmpty()) {
                this.initPacketMagicHeader = null;
            } else {
                this.initPacketMagicHeader = initPacketMagicHeader.trim();
            }
            return this;
        }

        public Builder parseResponsePacketMagicHeader(final String responsePacketMagicHeader) throws BadConfigException {
            if (responsePacketMagicHeader == null || responsePacketMagicHeader.trim().isEmpty()) {
                this.responsePacketMagicHeader = null;
            } else {
                this.responsePacketMagicHeader = responsePacketMagicHeader.trim();
            }
            return this;
        }

        public Builder parseUnderloadPacketMagicHeader(final String underloadPacketMagicHeader) throws BadConfigException {
            if (underloadPacketMagicHeader == null || underloadPacketMagicHeader.trim().isEmpty()) {
                this.underloadPacketMagicHeader = null;
            } else {
                this.underloadPacketMagicHeader = underloadPacketMagicHeader.trim();
            }
            return this;
        }

        public Builder parseTransportPacketMagicHeader(final String transportPacketMagicHeader) throws BadConfigException {
            if (transportPacketMagicHeader == null || transportPacketMagicHeader.trim().isEmpty()) {
                this.transportPacketMagicHeader = null;
            } else {
                this.transportPacketMagicHeader = transportPacketMagicHeader.trim();
            }
            return this;
        }

        public Builder parseSpecialJunkI1(final String specialJunkI1) throws BadConfigException {
            if (specialJunkI1 == null || specialJunkI1.trim().isEmpty()) {
                this.specialJunkI1 = null;
            } else {
                this.specialJunkI1 = specialJunkI1.trim();
            }
            return this;
        }

        public Builder parseSpecialJunkI2(final String specialJunkI2) throws BadConfigException {
            if (specialJunkI2 == null || specialJunkI2.trim().isEmpty()) {
                this.specialJunkI2 = null;
            } else {
                this.specialJunkI2 = specialJunkI2.trim();
            }
            return this;
        }

        public Builder parseSpecialJunkI3(final String specialJunkI3) throws BadConfigException {
            if (specialJunkI3 == null || specialJunkI3.trim().isEmpty()) {
                this.specialJunkI3 = null;
            } else {
                this.specialJunkI3 = specialJunkI3.trim();
            }
            return this;
        }

        public Builder parseSpecialJunkI4(final String specialJunkI4) throws BadConfigException {
            if (specialJunkI4 == null || specialJunkI4.trim().isEmpty()) {
                this.specialJunkI4 = null;
            } else {
                this.specialJunkI4 = specialJunkI4.trim();
            }
            return this;
        }

        public Builder parseSpecialJunkI5(final String specialJunkI5) throws BadConfigException {
            if (specialJunkI5 == null || specialJunkI5.trim().isEmpty()) {
                this.specialJunkI5 = null;
            } else {
                this.specialJunkI5 = specialJunkI5.trim();
            }
            return this;
        }

        public Builder parsePrivateKey(final String privateKey) throws BadConfigException {
            try {
                return setKeyPair(new KeyPair(Key.fromBase64(privateKey)));
            } catch (final KeyFormatException e) {
                throw new BadConfigException(Section.INTERFACE, Location.PRIVATE_KEY, e);
            }
        }

        public Builder setKeyPair(final KeyPair keyPair) {
            this.keyPair = keyPair;
            return this;
        }

        public Builder setListenPort(final int listenPort) throws BadConfigException {
            if (listenPort < MIN_UDP_PORT || listenPort > MAX_UDP_PORT)
                throw new BadConfigException(Section.INTERFACE, Location.LISTEN_PORT,
                        Reason.INVALID_VALUE, String.valueOf(listenPort));
            this.listenPort = listenPort == 0 ? null : listenPort;
            return this;
        }

        public Builder setMtu(final int mtu) throws BadConfigException {
            if (mtu < 0)
                throw new BadConfigException(Section.INTERFACE, Location.MTU,
                        Reason.INVALID_VALUE, String.valueOf(mtu));
            this.mtu = mtu == 0 ? null : mtu;
            return this;
        }

        public Builder setJunkPacketCount(final int junkPacketCount) throws BadConfigException {
            if (junkPacketCount < 0)
                throw new BadConfigException(Section.INTERFACE, Location.JUNK_PACKET_COUNT,
                        Reason.INVALID_VALUE, String.valueOf(junkPacketCount));
            this.junkPacketCount = junkPacketCount == 0 ? null : junkPacketCount;
            return this;
        }

        public Builder setJunkPacketMinSize(final int junkPacketMinSize) throws BadConfigException {
            if (junkPacketMinSize < 0)
                throw new BadConfigException(Section.INTERFACE, Location.JUNK_PACKET_MIN_SIZE,
                        Reason.INVALID_VALUE, String.valueOf(junkPacketMinSize));
            this.junkPacketMinSize = junkPacketMinSize == 0 ? null : junkPacketMinSize;
            return this;
        }

        public Builder setJunkPacketMaxSize(final int junkPacketMaxSize) throws BadConfigException {
            if (junkPacketMaxSize < 0)
                throw new BadConfigException(Section.INTERFACE, Location.JUNK_PACKET_MAX_SIZE,
                        Reason.INVALID_VALUE, String.valueOf(junkPacketMaxSize));
            this.junkPacketMaxSize = junkPacketMaxSize == 0 ? null : junkPacketMaxSize;
            return this;
        }

        public Builder setInitPacketJunkSize(final int initPacketJunkSize) throws BadConfigException {
            if (initPacketJunkSize < 0)
                throw new BadConfigException(Section.INTERFACE, Location.INIT_PACKET_JUNK_SIZE,
                        Reason.INVALID_VALUE, String.valueOf(initPacketJunkSize));
            this.initPacketJunkSize = initPacketJunkSize == 0 ? null : initPacketJunkSize;
            return this;
        }

        public Builder setResponsePacketJunkSize(final int responsePacketJunkSize) throws BadConfigException {
            if (responsePacketJunkSize < 0)
                throw new BadConfigException(Section.INTERFACE, Location.RESPONSE_PACKET_JUNK_SIZE,
                        Reason.INVALID_VALUE, String.valueOf(responsePacketJunkSize));
            this.responsePacketJunkSize = responsePacketJunkSize == 0 ? null : responsePacketJunkSize;
            return this;
        }

        public Builder setCookieReplyPacketJunkSize(final int cookieReplyPacketJunkSize) throws BadConfigException {
            if (cookieReplyPacketJunkSize < 0)
                throw new BadConfigException(Section.INTERFACE, Location.COOKIE_REPLY_PACKET_JUNK_SIZE,
                        Reason.INVALID_VALUE, String.valueOf(cookieReplyPacketJunkSize));
            this.cookieReplyPacketJunkSize = cookieReplyPacketJunkSize == 0 ? null : cookieReplyPacketJunkSize;
            return this;
        }

        public Builder setTransportPacketJunkSize(final int transportPacketJunkSize) throws BadConfigException {
            if (transportPacketJunkSize < 0)
                throw new BadConfigException(Section.INTERFACE, Location.TRANSPORT_PACKET_JUNK_SIZE,
                        Reason.INVALID_VALUE, String.valueOf(transportPacketJunkSize));
            this.transportPacketJunkSize = transportPacketJunkSize == 0 ? null : transportPacketJunkSize;
            return this;
        }

        public Builder setInitPacketMagicHeader(final String initPacketMagicHeader) throws BadConfigException {
            if (initPacketMagicHeader == null || initPacketMagicHeader.trim().isEmpty()) {
                this.initPacketMagicHeader = null;
            } else {
                this.initPacketMagicHeader = initPacketMagicHeader.trim();
            }
            return this;
        }

        public Builder setResponsePacketMagicHeader(final String responsePacketMagicHeader) throws BadConfigException {
            if (responsePacketMagicHeader == null || responsePacketMagicHeader.trim().isEmpty()) {
                this.responsePacketMagicHeader = null;
            } else {
                this.responsePacketMagicHeader = responsePacketMagicHeader.trim();
            }
            return this;
        }

        public Builder setUnderloadPacketMagicHeader(final String underloadPacketMagicHeader) throws BadConfigException {
            if (underloadPacketMagicHeader == null || underloadPacketMagicHeader.trim().isEmpty()) {
                this.underloadPacketMagicHeader = null;
            } else {
                this.underloadPacketMagicHeader = underloadPacketMagicHeader.trim();
            }
            return this;
        }

        public Builder setTransportPacketMagicHeader(final String transportPacketMagicHeader) throws BadConfigException {
            if (transportPacketMagicHeader == null || transportPacketMagicHeader.trim().isEmpty()) {
                this.transportPacketMagicHeader = null;
            } else {
                this.transportPacketMagicHeader = transportPacketMagicHeader.trim();
            }
            return this;
        }

        public Builder setSpecialJunkI1(final String specialJunkI1) throws BadConfigException {
            if (specialJunkI1 == null || specialJunkI1.trim().isEmpty()) {
                this.specialJunkI1 = null;
            } else {
                this.specialJunkI1 = specialJunkI1.trim();
            }
            return this;
        }

        public Builder setSpecialJunkI2(final String specialJunkI2) throws BadConfigException {
            if (specialJunkI2 == null || specialJunkI2.trim().isEmpty()) {
                this.specialJunkI2 = null;
            } else {
                this.specialJunkI2 = specialJunkI2.trim();
            }
            return this;
        }

        public Builder setSpecialJunkI3(final String specialJunkI3) throws BadConfigException {
            if (specialJunkI3 == null || specialJunkI3.trim().isEmpty()) {
                this.specialJunkI3 = null;
            } else {
                this.specialJunkI3 = specialJunkI3.trim();
            }
            return this;
        }

        public Builder setSpecialJunkI4(final String specialJunkI4) throws BadConfigException {
            if (specialJunkI4 == null || specialJunkI4.trim().isEmpty()) {
                this.specialJunkI4 = null;
            } else {
                this.specialJunkI4 = specialJunkI4.trim();
            }
            return this;
        }

        public Builder setSpecialJunkI5(final String specialJunkI5) throws BadConfigException {
            if (specialJunkI5 == null || specialJunkI5.trim().isEmpty()) {
                this.specialJunkI5 = null;
            } else {
                this.specialJunkI5 = specialJunkI5.trim();
            }
            return this;
        }
    }
}
