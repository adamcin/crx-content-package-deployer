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
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import net.adamcin.granite.client.packman.async.AsyncPackageManagerClient;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.api.KeyId;
import net.adamcin.httpsig.api.Signer;
import net.adamcin.httpsig.http.ning.AsyncUtil;
import net.adamcin.httpsig.ssh.bc.PEMUtil;
import net.adamcin.httpsig.ssh.jce.UserFingerprintKeyId;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes {@link PackageManagerClientCallable} instances by injecting an {@link AsyncPackageManagerClient}
 * as the implementation for {@link net.adamcin.granite.client.packman.PackageManagerClient}
 */
public final class GraniteClientExecutor {

    private static final Logger LOGGER = Logger.getLogger(GraniteAHCFactory.class.getName());
    private static final TaskListener DEFAULT_LISTENER = new LogTaskListener(LOGGER, Level.INFO);

    public static <T> T execute(PackageManagerClientCallable<T> callable, GraniteClientConfig config) throws Exception {
        return execute(callable, config, null);
    }

    public static <T> T execute(PackageManagerClientCallable<T> callable, GraniteClientConfig config, TaskListener _listener) throws Exception {
        final TaskListener listener = _listener != null ? _listener : DEFAULT_LISTENER;
        GraniteAHCFactory ahcFactory = GraniteAHCFactory.getFactoryInstance();

        AsyncHttpClient ahcClient = ahcFactory.newInstance(config);

        AsyncPackageManagerClient client = new AsyncPackageManagerClient(ahcClient);

        client.setBaseUrl(config.getBaseUrl());
        client.setRequestTimeout(config.getRequestTimeout());
        client.setServiceTimeout(config.getServiceTimeout());

        try {
            if (config.isSignatureLogin()) {
                if (!doSignatureLogin(client, config.getUsername(), listener)) {
                    listener.fatalError("Failed to login to %s", config.getBaseUrl());
                }
            }
            return callable.doExecute(client);
        } finally {
            ahcClient.closeAsynchronously();
        }
    }

    private static boolean doSignatureLogin(AsyncPackageManagerClient client, String username,
                                            final TaskListener listener) throws IOException {

        List<SSHUserPrivateKey> keys = CredentialsProvider.lookupCredentials(SSHUserPrivateKey.class);
        DefaultKeychain keychain = new DefaultKeychain();
        for (SSHUserPrivateKey key : keys) {
            char[] passphrase = null;
            if (key.getPassphrase() != null) {
                passphrase = key.getPassphrase().getEncryptedValue().toCharArray();
            }

            Key sshkey = PEMUtil.readKey(key.getPrivateKey().getBytes("UTF-8"), passphrase);
            if (sshkey != null) {
                keychain.add(sshkey);
            }
        }

        KeyId keyIdentifier = new UserFingerprintKeyId(username);
        Signer signer = new Signer(keychain, keyIdentifier);
        Future<Boolean> fResponse = AsyncUtil.login(
                client.getClient(), signer, client.getClient().prepareGet(client.getLoginUrl()).build(),
                new AsyncCompletionHandler<Boolean>() {
                    @Override
                    public Boolean onCompleted(Response response) throws Exception {
                        //listener.getLogger().printf("login status code %s%n", response.getStatusCode());
                        return response.getStatusCode() == 405;
                    }
                }
        );

        boolean loginResult = false;
        try {
            if (client.getServiceTimeout() > 0) {
                loginResult = fResponse.get(client.getServiceTimeout(), TimeUnit.MILLISECONDS);
            } else {
                loginResult = fResponse.get();
            }
        } catch (Exception e) {
            throw new IOException("Failed to login using HTTP Signature.", e);
        }
        return loginResult;
    }
}
