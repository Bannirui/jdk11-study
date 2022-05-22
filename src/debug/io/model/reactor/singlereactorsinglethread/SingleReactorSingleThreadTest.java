package debug.io.model.reactor.singlereactorsinglethread;

import debug.io.model.reactor.ClientTest;

/**
 *
 * @since 2022/5/22
 * @author dingrui
 */
public class SingleReactorSingleThreadTest {

    public static void main(String[] args) {
        Reactor reactor = new Reactor(ClientTest.PORT);
        reactor.run();
    }
}
