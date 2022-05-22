package debug.io.model.reactor.multiplereactormultiplethread;

import debug.io.model.reactor.ClientTest;

/**
 *
 * @since 2022/5/22
 * @author dingrui
 */
public class MultipleReactorMultipleThreadTest {

    public static void main(String[] args) {
        MainReactor mainReactor = new MainReactor(ClientTest.PORT);
        mainReactor.run();
    }
}
