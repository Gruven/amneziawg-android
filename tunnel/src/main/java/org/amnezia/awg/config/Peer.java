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
import org.amnezia.awg.util.NonNullForAll;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.Nullable;

/**
 * Represents the configuration for an AmneziaWG peer (a [Peer] block). Peers must have a public key,
 * and may optionally have several other attributes.
 * <p>
 * Instances of this class are immutable.
 */
@NonNullForAll
public final class Peer {
    private final Set<InetNetwork> allowedIps;
    @Nullable private final InetEndpoint endpoint;
    @Nullable private final Integer persistentKeepalive;
    @Nullable private final Key preSharedKey;
    private final Key publicKey;

    private Peer(final Builder builder) {
        // Defensively copy to ensure immutability even if the Builder is reused.
        allowedIps = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedIps));
        endpoint = builder.endpoint;
        persistentKeepalive = builder.persistentKeepalive;
        preSharedKey = builder.preSharedKey;
        publicKey = Objects.requireNonNull(builder.publicKey, "Peers must have a public key");
    }

    /**
     * Parses an series of "KEY = VALUE" lines into a {@code Peer}. Throws {@link ParseException} if
     * the input is not well-formed or contains unknown attributes.
     *
     * @param lines an iterable sequence of lines, containing at least a public key attribute
     * @return a {@code Peer} with all of its attributes set from {@code lines}
     */
    public static Peer parse(final Iterable<? extends CharSequence> lines)
            throws BadConfigException {
        final Builder builder = new Builder();
        for (final CharSequence line : lines) {
            final Attribute attribute = Attribute.parse(line);
            if (attribute == null)
                throw new BadConfigException(Section.PEER, Location.TOP_LEVEL,
                        Reason.SYNTAX_ERROR, line);
            switch (attribute.getKey().toLowerCase(Locale.ENGLISH)) {
                case "allowedips":
                    builder.parseAllowedIPs(attribute.getValue());
                    break;
                case "endpoint":
                    builder.parseEndpoint(attribute.getValue());
                    break;
                case "persistentkeepalive":
                    builder.parsePersistentKeepalive(attribute.getValue());
                    break;
                case "presharedkey":
                    builder.parsePreSharedKey(attribute.getValue());
                    break;
                case "publickey":
                    builder.parsePublicKey(attribute.getValue());
                    break;
                default:
                    throw new BadConfigException(Section.PEER, Location.TOP_LEVEL,
                            Reason.UNKNOWN_ATTRIBUTE, attribute.getKey());
            }
        }
        return builder.build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Peer))
            return false;
        final Peer other = (Peer) obj;
        return allowedIps.equals(other.allowedIps)
                && Objects.equals(endpoint, other.endpoint)
                && Objects.equals(persistentKeepalive, other.persistentKeepalive)
                && Objects.equals(preSharedKey, other.preSharedKey)
                && publicKey.equals(other.publicKey);
    }

    /**
     * Returns the peer's set of allowed IPs.
     *
     * @return the set of allowed IPs
     */
    public Set<InetNetwork> getAllowedIps() {
        // The collection is already immutable.
        return allowedIps;
    }

    /**
     * Returns the peer's endpoint.
     *
     * @return the endpoint, or {@code null} if none is configured
     */
    @Nullable
    public InetEndpoint getEndpoint() {
        return endpoint;
    }

    public boolean hasEndpoint() {
        return endpoint != null;
    }

    /**
     * Returns the peer's persistent keepalive.
     *
     * @return the persistent keepalive, or {@code null} if none is configured
     */
    @Nullable
    public Integer getPersistentKeepalive() {
        return persistentKeepalive;
    }

    public boolean hasPersistentKeepalive() {
        return persistentKeepalive != null;
    }

    public int getPersistentKeepaliveOrZero() {
        return persistentKeepalive != null ? persistentKeepalive : 0;
    }

    /**
     * Returns the peer's pre-shared key.
     *
     * @return the pre-shared key, or {@code null} if none is configured
     */
    @Nullable
    public Key getPreSharedKey() {
        return preSharedKey;
    }

    public boolean hasPreSharedKey() {
        return preSharedKey != null;
    }

    /**
     * Returns the peer's public key.
     *
     * @return the public key
     */
    public Key getPublicKey() {
        return publicKey;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 31 * hash + allowedIps.hashCode();
        hash = 31 * hash + Objects.hashCode(endpoint);
        hash = 31 * hash + Objects.hashCode(persistentKeepalive);
        hash = 31 * hash + Objects.hashCode(preSharedKey);
        hash = 31 * hash + publicKey.hashCode();
        return hash;
    }

    /**
     * Converts the {@code Peer} into a string suitable for debugging purposes. The {@code Peer} is
     * identified by its public key and (if known) its endpoint.
     *
     * @return a concise single-line identifier for the {@code Peer}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("(Peer ");
        sb.append(publicKey.toBase64());
        if (endpoint != null) sb.append(" @").append(endpoint);
        sb.append(')');
        return sb.toString();
    }

    /**
     * Converts the {@code Peer} into a string suitable for inclusion in a {@code awg-quick}
     * configuration file.
     *
     * @return the {@code Peer} represented as a series of "Key = Value" lines
     */
    public String toAwgQuickString() {
        final StringBuilder sb = new StringBuilder();
        if (!allowedIps.isEmpty())
            sb.append("AllowedIPs = ").append(Attribute.join(allowedIps)).append('\n');
        if (endpoint != null) sb.append("Endpoint = ").append(endpoint).append('\n');
        if (persistentKeepalive != null) sb.append("PersistentKeepalive = ").append(persistentKeepalive).append('\n');
        if (preSharedKey != null) sb.append("PreSharedKey = ").append(preSharedKey.toBase64()).append('\n');
        sb.append("PublicKey = ").append(publicKey.toBase64()).append('\n');
        return sb.toString();
    }

    /**
     * Serializes the {@code Peer} for use with the AmneziaWG cross-platform userspace API. Note
     * that not all attributes are included in this representation.
     *
     * @return the {@code Peer} represented as a series of "key=value" lines
     */
    public String toAwgUserspaceString() {
        final StringBuilder sb = new StringBuilder();
        // The order here is important: public_key signifies the beginning of a new peer.
        sb.append("public_key=").append(publicKey.toHex()).append('\n');
        for (final InetNetwork allowedIp : allowedIps)
            sb.append("allowed_ip=").append(allowedIp).append('\n');
        if (endpoint != null) {
            final InetEndpoint resolvedEndpoint = endpoint.getResolved();
            if (resolvedEndpoint != null) sb.append("endpoint=").append(resolvedEndpoint).append('\n');
        }
        if (persistentKeepalive != null) sb.append("persistent_keepalive_interval=").append(persistentKeepalive).append('\n');
        if (preSharedKey != null) sb.append("preshared_key=").append(preSharedKey.toHex()).append('\n');
        return sb.toString();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        // See awg(8)
        private static final int MAX_PERSISTENT_KEEPALIVE = 65535;

        // Defaults to an empty set.
        private final Set<InetNetwork> allowedIps = new LinkedHashSet<>();
        // Defaults to not present.
        @Nullable private InetEndpoint endpoint = null;
        // Defaults to not present.
        @Nullable private Integer persistentKeepalive = null;
        // Defaults to not present.
        @Nullable private Key preSharedKey = null;
        // No default; must be provided before building.
        @Nullable private Key publicKey;

        public Builder addAllowedIp(final InetNetwork allowedIp) {
            allowedIps.add(allowedIp);
            return this;
        }

        public Builder addAllowedIps(final Collection<InetNetwork> allowedIps) {
            this.allowedIps.addAll(allowedIps);
            return this;
        }

        public Peer build() throws BadConfigException {
            if (publicKey == null)
                throw new BadConfigException(Section.PEER, Location.PUBLIC_KEY,
                        Reason.MISSING_ATTRIBUTE, null);
            return new Peer(this);
        }

        public Builder parseAllowedIPs(final CharSequence allowedIps) throws BadConfigException {
            try {
                for (final String allowedIp : Attribute.split(allowedIps))
                    addAllowedIp(InetNetwork.parse(allowedIp));
                return this;
            } catch (final ParseException e) {
                throw new BadConfigException(Section.PEER, Location.ALLOWED_IPS, e);
            }
        }

        public Builder parseEndpoint(final String endpoint) throws BadConfigException {
            try {
                return setEndpoint(InetEndpoint.parse(endpoint));
            } catch (final ParseException e) {
                throw new BadConfigException(Section.PEER, Location.ENDPOINT, e);
            }
        }

        public Builder parsePersistentKeepalive(final String persistentKeepalive)
                throws BadConfigException {
            try {
                return setPersistentKeepalive(Integer.parseInt(persistentKeepalive));
            } catch (final NumberFormatException e) {
                throw new BadConfigException(Section.PEER, Location.PERSISTENT_KEEPALIVE,
                        persistentKeepalive, e);
            }
        }

        public Builder parsePreSharedKey(final String preSharedKey) throws BadConfigException {
            try {
                return setPreSharedKey(Key.fromBase64(preSharedKey));
            } catch (final KeyFormatException e) {
                throw new BadConfigException(Section.PEER, Location.PRE_SHARED_KEY, e);
            }
        }

        public Builder parsePublicKey(final String publicKey) throws BadConfigException {
            try {
                return setPublicKey(Key.fromBase64(publicKey));
            } catch (final KeyFormatException e) {
                throw new BadConfigException(Section.PEER, Location.PUBLIC_KEY, e);
            }
        }

        public Builder setEndpoint(final InetEndpoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder setPersistentKeepalive(final int persistentKeepalive)
                throws BadConfigException {
            if (persistentKeepalive < 0 || persistentKeepalive > MAX_PERSISTENT_KEEPALIVE)
                throw new BadConfigException(Section.PEER, Location.PERSISTENT_KEEPALIVE,
                        Reason.INVALID_VALUE, String.valueOf(persistentKeepalive));
            this.persistentKeepalive = persistentKeepalive == 0 ? null : persistentKeepalive;
            return this;
        }

        public Builder setPreSharedKey(final Key preSharedKey) {
            this.preSharedKey = preSharedKey;
            return this;
        }

        public Builder setPublicKey(final Key publicKey) {
            this.publicKey = publicKey;
            return this;
        }
    }
}
