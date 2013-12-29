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
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.security.ACL;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class GraniteCredentialsListBoxModel extends AbstractIdCredentialsListBoxModel<GraniteCredentialsListBoxModel, IdCredentials> {

    private static final long serialVersionUID = 6621529150670191089L;

    @NonNull
    @Override
    protected String describe(@NonNull IdCredentials idCredentials) {
        if (idCredentials instanceof GraniteNamedIdCredentials) {
            return ((GraniteNamedIdCredentials) idCredentials).getName();
        } else {
            return CredentialsNameProvider.name(idCredentials);
        }
    }

    public static AbstractIdCredentialsListBoxModel fillItems() {
        return fillItems(Collections.<DomainRequirement>emptyList());
    }

    public static AbstractIdCredentialsListBoxModel fillItems(final String baseUrl) {
        if (baseUrl != null) {
            return fillItems(URIRequirementBuilder.fromUri(baseUrl).build());
        } else {
            return fillItems();
        }
    }

    private static AbstractIdCredentialsListBoxModel fillItems(List<DomainRequirement> reqs) {

        List<SSHUserPrivateKey> keys = CredentialsProvider.lookupCredentials(SSHUserPrivateKey.class,
                (Item) null, ACL.SYSTEM, reqs);

        AbstractIdCredentialsListBoxModel<GraniteCredentialsListBoxModel, IdCredentials> model =
                new GraniteCredentialsListBoxModel().withEmptySelection();

        if (!keys.isEmpty()) {
            for (SSHUserPrivateKey key : keys) {
                model = model.with(GraniteNamedIdCredentials.wrap(key));
            }
        }

        List<StandardUsernamePasswordCredentials> basicAuthCredsList =
                CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                        (Item) null, ACL.SYSTEM, reqs);

        if (!basicAuthCredsList.isEmpty()) {
            for (StandardUsernamePasswordCredentials creds : basicAuthCredsList) {
                model = model.with(GraniteNamedIdCredentials.wrap(creds));
            }
        }

        return model;
    }
}
