package games.strategy.engine.framework.startup.login;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.DefaultObjectStreamFactory;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.ILoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.TestServerMessenger;

@Integration
final class ClientLoginIntegrationTest {
  private static final String PASSWORD = "password";
  private static final String OTHER_PASSWORD = "otherPassword";

  private IServerMessenger serverMessenger;
  private int serverPort;

  @BeforeEach
  void setUp() throws Exception {
    serverMessenger = newServerMessenger();
    serverPort = serverMessenger.getLocalNode().getSocketAddress().getPort();
  }

  private static IServerMessenger newServerMessenger() throws Exception {
    final IServerMessenger serverMessenger = new TestServerMessenger();
    serverMessenger.setAcceptNewConnections(true);
    serverMessenger.setLoginValidator(newLoginValidator(serverMessenger));
    return serverMessenger;
  }

  private static ILoginValidator newLoginValidator(final IServerMessenger serverMessenger) {
    final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(serverMessenger);
    clientLoginValidator.setGamePassword(PASSWORD);
    return clientLoginValidator;
  }

  @AfterEach
  void tearDown() {
    serverMessenger.shutDown();
  }

  private static class TestConnectionLogin extends ClientLogin {
    private final String password;

    TestConnectionLogin() {
      this(PASSWORD);
    }

    TestConnectionLogin(final String password) {
      super(null);

      this.password = password;
    }

    @Override
    protected String promptForPassword() {
      return password;
    }
  }

  private IClientMessenger newClientMessenger(final IConnectionLogin connectionLogin) throws Exception {
    return new ClientMessenger(
        "localhost",
        serverPort,
        "client",
        MacFinder.getHashedMacAddress(),
        new DefaultObjectStreamFactory(),
        connectionLogin);
  }

  @Nested
  final class LoginTest {
    @Test
    void shouldSucceedWhenPasswordMatches() {
      final IConnectionLogin connectionLogin = new TestConnectionLogin();

      assertDoesNotThrow(() -> newClientMessenger(connectionLogin).shutDown());
    }

    @Test
    void shouldFailWhenPasswordDoesNotMatch() {
      final IConnectionLogin connectionLogin = new TestConnectionLogin(OTHER_PASSWORD);

      assertThrows(CouldNotLogInException.class, () -> newClientMessenger(connectionLogin).shutDown());
    }
  }
}
