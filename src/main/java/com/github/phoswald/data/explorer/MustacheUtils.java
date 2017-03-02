package com.github.phoswald.data.explorer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MustacheUtils {

    static Map<String, Object> scope(Tag... tags) {
        Map<String, Object> scope = new HashMap<String, Object>();
        if(tags != null) {
            for(Tag tag : tags) {
                scope.put(tag.key, tag.value);
                if(tag.flags.contains(Flag.JS)) {
                    scope.put(tag.key + "JS", encodeJS((String) tag.value));
                }
            }
        }
        return scope;
    }

    static Tag tag(String key, String value) {
        return new Tag(key, value, EnumSet.noneOf(Flag.class));
    }

    static Tag tag(String key, String value, Flag flag, Flag... flags) {
        return new Tag(key, value, EnumSet.of(flag, flags));
    }

    static Tag tag(String key, List<Map<String, Object>> value) {
        return new Tag(key, value, EnumSet.noneOf(Flag.class));
    }

    static class Tag {
        final String key;
        final Object value;
        final EnumSet<Flag> flags;

        Tag(String key, Object value, EnumSet<Flag> flags) {
            this.key = key;
            this.value = value;
            this.flags = flags;
        }
    }

    static enum Flag {
        JS
    }

    private static String encodeJS(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for(char c : value.toCharArray()) {
            switch(c) {
                case '"':
                case '\'':
                case '\\': sb.append('\\').append(c); break;
                case '\t': sb.append("\\t"); break;
                case '\r': sb.append("\\r"); break;
                case '\n': sb.append("\\n"); break;
                default: sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
