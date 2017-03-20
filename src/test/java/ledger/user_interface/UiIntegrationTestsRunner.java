package ledger.user_interface;

import javafx.stage.Stage;
import ledger.user_interface.ui_controllers.Startup;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Integration runner for our UI Integration tests. This runner will be picked up by the Junit runner.
 * Test methods in here can call methods in the various IntegrationTest classes to form an integration test.
 */
public class UiIntegrationTestsRunner extends ApplicationTest {

    @Rule public RetryRule retryRule = new RetryRule(1);

    private static final LoginIntegrationTests loginTests = new LoginIntegrationTests();
    private static final AccountIntegrationTests accountTests = new AccountIntegrationTests();
    private static final TransactionIntegrationTests transactionTests = new TransactionIntegrationTests();

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("Start Method");
        if (!Boolean.getBoolean("headless")) {
            new Startup().start(stage);
        }
    }

    @BeforeClass
    public static void setupHeadlessRun() throws Exception {
        System.out.println("BeforeClass");
        if ("true".equals(System.getenv("headless"))) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        }
    }

    @Before
    public void beforeAllTests() {
        System.out.println("Before");
        removeExistingDBFile();
        assumeTrue(!"true".equals(System.getenv("headless")));
    }

    @After
    public void afterAllTests() {
        System.out.println("After");
        removeExistingDBFile();
    }

    /**
     * Remove a database file with the default name, if one exists
     */
    private void removeExistingDBFile() {
        String home = System.getProperty("user.home");
        File dbFile = new File(home, "LedgerDB.mv.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test(timeout = 60000)
    public void testLogin() {
        System.out.println("Test Login");
        loginTests.createDatabase();
        loginTests.logout();
    }

    @Test(timeout = 60000)
    public void testCreateAccounts() {
        System.out.println("Test Create Accounts");
        loginTests.createDatabase();
        accountTests.createAccounts();
        loginTests.logout();
    }

    @Test(timeout = 60000)
    public void testDeleteAccount() {
        System.out.println("Test Delete Account");
        loginTests.createDatabase();
        accountTests.addSingleAccount("Hello", "World", "1234");
        accountTests.deleteAccount("Hello");
        loginTests.logout();
    }

    @Test(timeout = 60000)
    public void testTransactionInsertionViaWindow() {
        System.out.println("Test T Insert Via Window");
        loginTests.createDatabase();
        accountTests.addSingleAccount("Hello", "World", "1234");
        transactionTests.insertTransactions();
        loginTests.logout();

    }

    private class RetryRule implements TestRule {
        private int retryCount;

        public RetryRule(int retryCount) {
            this.retryCount = retryCount;
        }

        public Statement apply(Statement base, Description description) {
            return statement(base, description);
        }
        private Statement statement(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Throwable caughtThrowable = null;

                    // implement retry logic here
                    for (int i = 0; i < retryCount; i++) {
                        try {
                            base.evaluate();
                            return;
                        } catch (Throwable t) {
                            caughtThrowable = t;

                            //  System.out.println(": run " + (i+1) + " failed");
                            System.err.println(description.getDisplayName() + ": run " + (i + 1) + " failed");
                        }
                    }
                    System.err.println(description.getDisplayName() + ": giving up after " + retryCount + " failures");
                    throw caughtThrowable;
                }
            };
        }
    }

}