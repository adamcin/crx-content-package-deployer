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
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilderBase;
import com.ning.http.client.Response;
import com.ning.http.client.SignatureCalculator;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import net.adamcin.granite.client.packman.async.AsyncPackageManagerClient;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.api.KeyId;
import net.adamcin.httpsig.api.Signer;
import net.adamcin.httpsig.http.ning.AsyncUtil;
import net.adamcin.httpsig.ssh.bc.PEMUtil;
import net.adamcin.httpsig.ssh.jce.UserKeysFingerprintKeyId;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes {@link PackageManagerClientCallable} instances by injecting an {@link AsyncPackageManagerClient}
 * as the implementation for {@link net.adamcin.granite.client.packman.PackageManagerClient}
 */
public final class GraniteClientExecutor {

    private static final Logger LOGGER = Logger.getLogger(GraniteClientExecutor.class.getName());

    private static final TaskListener DEFAULT_LISTENER = new LogTaskListener(LOGGER, Level.INFO);

    private static final AsyncCompletionHandler<Boolean> LOGIN_HANDLER = new AsyncCompletionHandler<Boolean>() {
        @Override
        public Boolean onCompleted(Response response) throws Exception {
            return response.getStatusCode() == 405 || response.getStatusCode() == 200;
        }
    };

    public static <T> T execute(PackageManagerClientCallable<T> callable, GraniteClientConfig config) throws Exception {
        return execute(callable, config, null);
    }

    public static <T> T execute(PackageManagerClientCallable<T> callable, GraniteClientConfig config, TaskListener _listener) throws Exception {
        final TaskListener listener = _listener != null ? _listener : DEFAULT_LISTENER;
        GraniteAHCFactory ahcFactory = GraniteAHCFactory.getFactoryInstance();

        AsyncHttpClient ahcClient = ahcFactory.newInstance();

        AsyncPackageManagerClient client = new AsyncPackageManagerClient(ahcClient);

        client.setBaseUrl(config.getBaseUrl());
        client.setRequestTimeout(config.getRequestTimeout());
        client.setServiceTimeout(config.getServiceTimeout());

        try {
            if (doLogin(client, config.getCredentials(), listener)) {
                return callable.doExecute(client);
            } else {
                throw new IOException("Failed to login to " + config.getBaseUrl());
            }
        } finally {
            ahcClient.closeAsynchronously();
        }
    }

    private static boolean doLogin(AsyncPackageManagerClient client, Credentials credentials,
                                   final TaskListener listener) throws IOException {
        final Credentials _creds = credentials != null ? credentials :
                GraniteAHCFactory.getFactoryInstance().getDefaultCredentials();

        if (_creds instanceof SSHUserPrivateKey) {
            return doLoginSignature(client, (SSHUserPrivateKey) _creds, listener);
        } else if (_creds instanceof StandardUsernamePasswordCredentials) {
            String username = ((StandardUsernamePasswordCredentials) _creds).getUsername();
            String password = ((StandardUsernamePasswordCredentials) _creds).getPassword().getPlainText();
            return doLoginPOST(client, username, password, listener);
        } else {
            return doLoginPOST(client, "admin", "admin", listener);
        }
    }

    private static boolean doLoginSignature(AsyncPackageManagerClient client, SSHUserPrivateKey key,
                                   final TaskListener listener) throws IOException {

        Key sshkey = GraniteNamedIdCredentials.getKeyFromCredentials(key);
        if (sshkey == null) {
            return false;
        }

        /*
        SignatureCalculator calculator = new SignatureCalculator() {
            public void calculateAndAddSignature(String url, Request request, RequestBuilderBase<?> requestBuilder) {
                request.getBodyGenerator().createBody().
            }
        }
        */

        KeyId keyId = new UserKeysFingerprintKeyId(key.getUsername());
        Signer signer = new Signer(sshkey, keyId);
        Future<Boolean> fResponse = AsyncUtil.login(client.getClient(),
                signer, client.getClient().prepareGet(client.getBaseUrl() + "?sling:authRequestLogin=Signature&j_validate=true").build(), LOGIN_HANDLER);

        try {
            if (client.getServiceTimeout() > 0) {
                return fResponse.get(client.getServiceTimeout(), TimeUnit.MILLISECONDS);
            } else {
                return fResponse.get();
            }
        } catch (Exception e) {
            throw new IOException("Failed to login using HTTP Signature authentication.", e);
        }
    }

    private static boolean doLoginPOST(AsyncPackageManagerClient client, String username, String password,
                                        final TaskListener listener) throws IOException {
        return client.login(username, password);
    }

    public static boolean checkLogin(final GraniteClientConfig config) throws IOException {
        final AsyncHttpClient asyncHttpClient = GraniteAHCFactory.getFactoryInstance().newInstance();

        AsyncPackageManagerClient client = new AsyncPackageManagerClient(asyncHttpClient);

        client.setBaseUrl(config.getBaseUrl());
        client.setRequestTimeout(config.getRequestTimeout());
        client.setServiceTimeout(config.getServiceTimeout());

        try {
            return doLogin(client, config.getCredentials(), DEFAULT_LISTENER);
        } finally {
            asyncHttpClient.closeAsynchronously();
        }
    }
}
