/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package org.jenkinsci.plugins.graniteclient;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.Credentials;

import java.io.Serializable;

/**
 * Pojo for capturing the group of configuration values for a single Granite Client connection
 */
public final class GraniteClientConfig implements Serializable {

    private static final long serialVersionUID = 5044980450351873759L;

    private final String baseUrl;
    private final String credentialsId;
    private final long requestTimeout;
    private final long serviceTimeout;
    private final Credentials credentials;

    public GraniteClientConfig(String baseUrl, String credentialsId, long requestTimeout, long serviceTimeout) {
        this.baseUrl = baseUrl;
        this.credentialsId = credentialsId;
        this.requestTimeout = requestTimeout > 0L ? requestTimeout : -1L;
        this.serviceTimeout = serviceTimeout > 0L ? serviceTimeout : -1L;
        this.credentials = GraniteNamedIdCredentials.getCredentialsById(credentialsId);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public boolean isSignatureLogin() {
        return credentials instanceof SSHUserPrivateKey;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public long getServiceTimeout() {
        return serviceTimeout;
    }

    public Credentials getCredentials() {
        return credentials;
    }

}
