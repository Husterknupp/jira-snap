package de.bschandera;

import java.util.Optional;

public class Password {

    public Optional<String> readFromConsole() {
        return Optional.ofNullable(System.console())
                .map(console -> {
                    char password[] = console.readPassword("Please enter your jira password: ");
                    return new String(password);
                });
    }
}
