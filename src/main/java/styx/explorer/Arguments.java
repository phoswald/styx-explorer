package styx.explorer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class Arguments {

    private final List<String> args;

    Arguments(String[] args) {
        this.args = Arrays.asList(args);
    }

    Optional<Boolean> getBoolean(String name) {
        return getString(name).map(Boolean::parseBoolean);
    }

    Optional<Integer> getInt(String name) {
        return getString(name).map(Integer::parseInt);
    }

    Optional<String> getString(String name) {
        for(String arg : args) {
            if(arg.startsWith("-" + name + "=")) {
                return Optional.of(arg.substring(name.length() + 2));
            }
        }
        String property = System.getProperty("styx." + name.replace("-", "."));
        if(property != null) {
            return Optional.of(property);
        }
        String env = System.getenv("STYX_" + name.replace("-", "_").toUpperCase());
        if(env != null) {
            return Optional.of(env);
        }
        return Optional.empty();
    }
}
