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

package net.adamcin.jenkins.granite;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import net.adamcin.granite.client.packman.ListResponse;
import net.adamcin.granite.client.packman.ListResult;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.PackIdFilter;
import net.adamcin.granite.client.packman.PackageManagerClient;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: madamcin
 * Date: 12/20/13
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class PackageChoiceParameterDefinition extends ParameterDefinition {

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return "[Granite] Package Choice Parameter";
        }
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        Object value = jo.get("value");
        if (value instanceof String) {
            return constructValue((String) value);
        } else if (value instanceof JSONArray) {
            return constructValue((JSONArray) value);
        } else {
            return constructDefaultValue();
        }
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String[] requestValues = req.getParameterValues(getName());
        if (requestValues == null || requestValues.length == 0) {
            return constructDefaultValue();
        } else {
            return constructValue(requestValues);
        }
    }

    protected PackageChoiceParameterValue constructDefaultValue() {
        return new PackageChoiceParameterValue(getName(), "");
    }

    protected PackageChoiceParameterValue constructValue(String value) {
        return new PackageChoiceParameterValue(getName(), value);
    }

    protected PackageChoiceParameterValue constructValue(JSONArray values) {
        return constructValue(values.iterator());
    }

    protected PackageChoiceParameterValue constructValue(Iterator values) {
        return new PackageChoiceParameterValue(getName(), StringUtils.join(values, "\n"));
    }

    protected PackageChoiceParameterValue constructValue(String[] values) {
        return new PackageChoiceParameterValue(getName(), StringUtils.join(values, "\n"));
    }

    public List<PackId> getPackageList() {
        GraniteClientConfig config = getGraniteClientConfig();

        try {
            ListResponse response = GraniteClientExecutor.execute(new GraniteClientCallable<ListResponse>() {
                public ListResponse doExecute(PackageManagerClient client) throws Exception {
                    return client.list(query);
                }
            }, config);

            List<PackId> packIds = new ArrayList<PackId>();
            List<ListResult> results = response.getResults();
            if (results != null) {
                for (ListResult result : results) {
                    if ((!excludeNotInstalled || result.isHasSnapshot())
                            && (!excludeModified || !result.isNeedsRewrap())
                            && getPackIdFilter().includes(result.getPackId())) {
                        packIds.add(result.getPackId());
                    }
                }
            }
            return Collections.unmodifiableList(packIds);
        } catch (Exception e) {
            return getSelectedPackIds();
        }
    }

    public List<PackId> getSelectedPackIds() {
        List<PackId> packIds = new ArrayList<PackId>();

        String effectiveValue = getEffectiveValue();
        for (String pid : effectiveValue.split("\\r?\\n")) {
            PackId packId = PackId.parsePid(pid);
            if (packId != null) {
                packIds.add(packId);
            }
        }

        return Collections.unmodifiableList(packIds);
    }

    private String baseUrl;
    private String username;
    private String password;
    private boolean signatureLogin;
    private long requestTimeout;
    private long serviceTimeout;

    private boolean multiselect;
    private boolean excludeNotInstalled;
    private boolean excludeModified;
    private String query;
    private String packageIdFilter;
    private String value;

    @DataBoundConstructor
    public PackageChoiceParameterDefinition(String name, String description, String baseUrl, String username,
                                            String password, boolean signatureLogin, long requestTimeout,
                                            long serviceTimeout, boolean multiselect, boolean excludeNotInstalled,
                                            boolean excludeModified, String query, String packageIdFilter,
                                            String value) {
        super(name, description);
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.signatureLogin = signatureLogin;
        this.requestTimeout = requestTimeout;
        this.serviceTimeout = serviceTimeout;
        this.multiselect = multiselect;
        this.excludeNotInstalled = excludeNotInstalled;
        this.excludeModified = excludeModified;
        this.query = query;
        this.packageIdFilter = packageIdFilter;
        this.value = value;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSignatureLogin() {
        return signatureLogin;
    }

    public void setSignatureLogin(boolean signatureLogin) {
        this.signatureLogin = signatureLogin;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public long getServiceTimeout() {
        return serviceTimeout;
    }

    public void setServiceTimeout(long serviceTimeout) {
        this.serviceTimeout = serviceTimeout;
    }

    public boolean isMultiselect() {
        return multiselect;
    }

    public void setMultiselect(boolean multiselect) {
        this.multiselect = multiselect;
    }

    public boolean isExcludeNotInstalled() {
        return excludeNotInstalled;
    }

    public void setExcludeNotInstalled(boolean excludeNotInstalled) {
        this.excludeNotInstalled = excludeNotInstalled;
    }

    public boolean isExcludeModified() {
        return excludeModified;
    }

    public void setExcludeModified(boolean excludeModified) {
        this.excludeModified = excludeModified;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getPackageIdFilter() {
        return packageIdFilter;
    }

    public void setPackageIdFilter(String packageIdFilter) {
        this.packageIdFilter = packageIdFilter;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEffectiveValue() {
        if (this.value == null) {
            return "";
        } else {
            return this.value.trim();
        }
    }

    public PackIdFilter getPackIdFilter() {
        return DefaultPackIdFilter.parse(getPackageIdFilter());
    }

    private GraniteClientConfig getGraniteClientConfig() {
        return new GraniteClientConfig(
                getBaseUrl(), getUsername(), getPassword(), isSignatureLogin(),
                requestTimeout > 0L ? requestTimeout : -1L,
                serviceTimeout > 0L ? serviceTimeout : -1L
        );

    }
}
