/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kafka.common.security.oauthbearer.secured;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Collection;
import org.apache.kafka.common.utils.MockTime;
import org.apache.kafka.common.utils.Time;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.ReservedClaimNames;
import org.jose4j.lang.JoseException;

public class AccessTokenBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String audience;

    private String subject = "jdoe";

    private String subjectClaimName = ReservedClaimNames.SUBJECT;

    private Object scope = "engineering";

    private String scopeClaimName = "scope";

    private Long issuedAtSeconds;

    private Long expirationSeconds;

    private RsaJsonWebKey jwk;

    public AccessTokenBuilder() throws JoseException {
        this(new MockTime());
    }

    public AccessTokenBuilder(Time time) throws JoseException {
        this.issuedAtSeconds = time.milliseconds() / 1000;
        this.expirationSeconds = this.issuedAtSeconds + 60;
        this.jwk = createJwk();
    }

    public static RsaJsonWebKey createJwk() throws JoseException {
        RsaJsonWebKey jwk = RsaJwkGenerator.generateJwk(2048);
        jwk.setKeyId("key-1");
        return jwk;
    }

    public String audience() {
        return audience;
    }

    public AccessTokenBuilder audience(String audience) {
        this.audience = audience;
        return this;
    }

    public String subject() {
        return subject;
    }

    public AccessTokenBuilder subject(String subject) {
        this.subject = subject;
        return this;
    }

    public String subjectClaimName() {
        return subjectClaimName;
    }

    public AccessTokenBuilder subjectClaimName(String subjectClaimName) {
        this.subjectClaimName = subjectClaimName;
        return this;
    }

    public Object scope() {
        return scope;
    }

    public AccessTokenBuilder scope(Object scope) {
        this.scope = scope;

        if (scope instanceof String) {
            return this;
        } else if (scope instanceof Collection) {
            return this;
        } else {
            throw new IllegalArgumentException(String.format("%s parameter must be a %s or a %s containing %s",
                scopeClaimName,
                String.class.getName(),
                Collection.class.getName(),
                String.class.getName()));
        }
    }

    public String scopeClaimName() {
        return scopeClaimName;
    }

    public AccessTokenBuilder scopeClaimName(String scopeClaimName) {
        this.scopeClaimName = scopeClaimName;
        return this;
    }

    public Long issuedAtSeconds() {
        return issuedAtSeconds;
    }

    public AccessTokenBuilder issuedAtSeconds(Long issuedAtSeconds) {
        this.issuedAtSeconds = issuedAtSeconds;
        return this;
    }

    public Long expirationSeconds() {
        return expirationSeconds;
    }

    public AccessTokenBuilder expirationSeconds(Long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
        return this;
    }

    public RsaJsonWebKey jwk() {
        return jwk;
    }

    public AccessTokenBuilder jwk(RsaJsonWebKey jwk) {
        this.jwk = jwk;
        return this;
    }

    @SuppressWarnings("unchecked")
    public String build() throws JoseException, IOException {
        ObjectNode node = objectMapper.createObjectNode();

        if (audience != null)
            node.put(ReservedClaimNames.AUDIENCE, audience);

        if (subject != null)
            node.put(subjectClaimName, subject);

        if (scope instanceof String) {
            node.put(scopeClaimName, (String) scope);
        } else if (scope instanceof Collection) {
            ArrayNode child = node.putArray(scopeClaimName);
            ((Collection<String>) scope).forEach(child::add);
        } else {
            throw new IllegalArgumentException(String.format("%s claim must be a %s or a %s containing %s",
                scopeClaimName,
                String.class.getName(),
                Collection.class.getName(),
                String.class.getName()));
        }

        if (issuedAtSeconds != null)
            node.put(ReservedClaimNames.ISSUED_AT, issuedAtSeconds);

        if (expirationSeconds != null)
            node.put(ReservedClaimNames.EXPIRATION_TIME, expirationSeconds);

        String json = objectMapper.writeValueAsString(node);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(json);
        jws.setKey(jwk.getPrivateKey());
        jws.setKeyIdHeaderValue(jwk.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws.getCompactSerialization();
    }

}
