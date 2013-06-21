package net.adamcin.jenkins.granite;

import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.PackIdFilter;

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
