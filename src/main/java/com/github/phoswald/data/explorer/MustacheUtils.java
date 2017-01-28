package com.github.phoswald.data.explorer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MustacheUtils {

    static Map<String, Object> scope(Tag... tags) {
        Map<String, Object> scope = new HashMap<String, Object>();
        if(tags != null) {
            for(Tag tag : tags) {
                scope.put(tag.key, tag.value);
            }
        }
        return scope;
    }

    static Tag tag(String key, String value) {
        return new Tag(key, value);
    }

//    static Tag tag(String key, boolean value) {
//        return new Tag(key, value ? "1" : null);
//    }

    static Tag tag(String key, List<Map<String, Object>> value) {
        return new Tag(key, value);
    }

    static class Tag {
        final String key;
        final Object value;
        Tag(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }
}
