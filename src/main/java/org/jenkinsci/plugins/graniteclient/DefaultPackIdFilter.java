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

import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.PackIdFilter;

/**
 * Default implementation of {@link PackIdFilter} which parses a standard filter string
 * format, matching "*:*:*", for "group:name:version"
 */
public final class DefaultPackIdFilter implements PackIdFilter {
    public static final DefaultPackIdFilter INCLUDE_ALL_FILTER = new DefaultPackIdFilter(null, null, null);

    public static final String WILDCARD = "*";

    private final String group;
    private final String name;
    private final String version;

    public DefaultPackIdFilter(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public boolean includes(PackId packId) {
        boolean includes = true;
        if (group != null && group.length() > 0) {
            includes = includes && (group.equals(WILDCARD) || group.equals(packId.getGroup()));
        }
        if (name != null && name.length() > 0) {
            includes = includes && (name.equals(WILDCARD) || name.equals(packId.getName()));
        }
        if (version != null && version.length() > 0) {
            includes = includes && (version.equals(WILDCARD) || version.equals(packId.getVersion()));
        }
        return includes;
    }

    public static DefaultPackIdFilter parse(String filterString) {
        if (filterString == null) {
            return INCLUDE_ALL_FILTER;
        } else {
            String[] parts = filterString.trim().split(":");
            switch (parts.length) {
                case 1: return new DefaultPackIdFilter(null, parts[0], null);
                case 2: return new DefaultPackIdFilter(parts[0], parts[1], null);
                case 3: return new DefaultPackIdFilter(parts[0], parts[1], parts[2]);
                default: return INCLUDE_ALL_FILTER;
            }
        }
    }

}
