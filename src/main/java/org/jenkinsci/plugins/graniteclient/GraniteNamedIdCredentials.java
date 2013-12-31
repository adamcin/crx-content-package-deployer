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
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.api.KeyId;
import net.adamcin.httpsig.ssh.bc.PEMUtil;
import net.adamcin.httpsig.ssh.jce.FingerprintableKey;
import net.adamcin.httpsig.ssh.jce.UserKeysFingerprintKeyId;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Wrapper for {@link SSHUserPrivateKey} credentials implementing {@link IdCredentials} for selection widgets
 */
abstract class GraniteNamedIdCredentials implements IdCredentials {
    private static final Logger LOGGER = Logger.getLogger(GraniteNamedIdCredentials.class.getName());

    private static final long serialVersionUID = -7611025520557823267L;

    public static Credentials getCredentialsById(String credentialsId) {
        CredentialsMatcher matcher = new CredentialsIdMatcher(credentialsId);
        List<Credentials> credentialsList =
                DomainCredentials.getCredentials(
                        SystemCredentialsProvider.getInstance().getDomainCredentialsMap(),
                        Credentials.class, Collections.<DomainRequirement>emptyList(), matcher
                );

        if (!credentialsList.isEmpty()) {
            return credentialsList.iterator().next();
        } else {
            return null;
        }
    }

    public CredentialsScope getScope() {
        return getWrappedCredentials().getScope();
    }

    protected abstract Credentials getWrappedCredentials();

    public abstract String getName();

    @NonNull
    public CredentialsDescriptor getDescriptor() {
        return getWrappedCredentials().getDescriptor();
    }

    static class SSHPrivateKeyNamedIdCredentials extends GraniteNamedIdCredentials {

        private static final long serialVersionUID = -8908675817671402062L;

        private final SSHUserPrivateKey wrapped;

        SSHPrivateKeyNamedIdCredentials(SSHUserPrivateKey wrapped) {
            this.wrapped = wrapped;
        }

        @NonNull
        public String getId() {
            return wrapped.getId();
        }

        public String getName() {
            Key key = getKeyFromCredentials(wrapped);

            if (key == null) {
                return "[Signature] <failed to read SSH key> " + getId();
            }

            KeyId keyId = new UserKeysFingerprintKeyId(wrapped.getUsername());
            StringBuilder nameBuilder = new StringBuilder("[Signature] ").append(keyId.getId(key));
            if (wrapped.getDescription() != null && !wrapped.getDescription().trim().isEmpty()) {
                nameBuilder.append(" (").append(wrapped.getDescription()).append(")");
            }

            return nameBuilder.toString();
        }

        @Override
        protected Credentials getWrappedCredentials() {
            return wrapped;
        }
    }

    static class UserPassNamedIdCredentials extends GraniteNamedIdCredentials {

        private static final long serialVersionUID = -7566342113168803477L;

        private final StandardUsernamePasswordCredentials wrapped;

        UserPassNamedIdCredentials(StandardUsernamePasswordCredentials wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public String getName() {
            return CredentialsNameProvider.name(wrapped);
        }

        @Override
        protected Credentials getWrappedCredentials() {
            return wrapped;
        }

        @NonNull
        public String getId() {
            return wrapped.getId();
        }
    }

    public static GraniteNamedIdCredentials wrap(final SSHUserPrivateKey creds) {
        return new SSHPrivateKeyNamedIdCredentials(creds);
    }

    public static GraniteNamedIdCredentials wrap(final StandardUsernamePasswordCredentials creds) {
        return new UserPassNamedIdCredentials(creds);
    }

    private static class CredentialsIdMatcher implements CredentialsMatcher {
        final String credentialsId;

        private CredentialsIdMatcher(String credentialsId) {
            this.credentialsId = credentialsId;
        }

        public boolean matches(@NonNull Credentials item) {
            if (credentialsId != null && !credentialsId.isEmpty()) {
                if (item instanceof SSHUserPrivateKey) {
                    return credentialsId.equals(((SSHUserPrivateKey) item).getId());
                } else if (item instanceof IdCredentials) {
                    return credentialsId.equals(((IdCredentials) item).getId());
                }
            }
            return false;
        }
    }

    public static Key getKeyFromCredentials(SSHUserPrivateKey creds) {
        try {
            char[] passphrase = null;

            if (creds.getPassphrase() != null) {
                passphrase = creds.getPassphrase().getEncryptedValue().toCharArray();
            }

            return PEMUtil.readKey(creds.getPrivateKey().getBytes(Charset.forName("UTF-8")), passphrase);
        } catch (IOException e) {
            LOGGER.severe("[getKeyFromCredentials] failed to read key from SSHUserPrivateKey: " + e.getMessage());
        }

        return null;
    }
}
