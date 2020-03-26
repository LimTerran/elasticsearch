/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.idp.action;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class SamlValidateAuthnRequestResponse extends ActionResponse {

    private final String spEntityId;
    private final boolean forceAuthn;
    private final Map<String, Object> authnState;

    public SamlValidateAuthnRequestResponse(StreamInput in) throws IOException {
        super(in);
        this.spEntityId = in.readString();
        this.forceAuthn = in.readBoolean();
        this.authnState = in.readMap();
    }

    public SamlValidateAuthnRequestResponse(String spEntityId, boolean forceAuthn, Map<String, Object> authnState) {
        this.spEntityId = Objects.requireNonNull(spEntityId, "spEntityId is required for successful responses");
        this.forceAuthn = forceAuthn;
        this.authnState = Map.copyOf(Objects.requireNonNull(authnState));
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public boolean isForceAuthn() {
        return forceAuthn;
    }

    public Map<String, Object> getAuthnState() {
        return authnState;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(spEntityId);
        out.writeBoolean(forceAuthn);
        out.writeMap(authnState);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{ spEntityId='" + getSpEntityId() + "',\n" +
            " forceAuthn='" + isForceAuthn() + "',\n" +
            " authnState='" + getAuthnState() + "' }";
    }
}
