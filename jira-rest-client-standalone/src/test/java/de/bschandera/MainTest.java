package de.bschandera;

import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public class MainTest {

    @Test
    public void testPasswordIsUsed() throws IOException, InterruptedException {
        Main.setJiraClient(null);
        Password password = Mockito.mock(Password.class);
        Mockito.when(password.readFromConsole()).thenReturn(Optional.of("pw"));
        Main.setPassword(password);

        try {
            Main.main(new String[]{"test mode"});
            Assert.fail("Password read method should have been called");
        } catch (UncheckedIOException e) {
            // expected due to missing config.json - continue with verify
        }
        Mockito.verify(password, Mockito.times(1)).readFromConsole();
    }

}
