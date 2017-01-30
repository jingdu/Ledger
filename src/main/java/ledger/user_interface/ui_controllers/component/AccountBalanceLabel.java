package ledger.user_interface.ui_controllers.component;

import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import ledger.controller.DbController;
import ledger.controller.register.TaskWithArgsReturn;
import ledger.controller.register.TaskWithReturn;
import ledger.database.entity.Account;
import ledger.database.entity.AccountBalance;
import ledger.database.entity.Transaction;
import ledger.user_interface.ui_controllers.IUIController;
import ledger.user_interface.ui_controllers.Startup;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Label used to track the balance of the current account.
 */
public class AccountBalanceLabel extends Label implements IUIController, Initializable {

    private static final String pageLoc = "/fxml_files/Label.fxml";

    private Account currentAccount;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DbController.INSTANCE.registerTransationSuccessEvent(this::recalculateBalance);
        calculateBalanceForAllAccounts();
    }

    public AccountBalanceLabel() {
        this.initController(pageLoc, this, "Unable to load Account Balance Label");
    }

    public void recalculateBalance() {
        Startup.INSTANCE.runLater(this::calculateBalanceForAccount);
    }

    public void calculateBalance(Account account) {
        this.currentAccount = account;
        if(this.currentAccount == null) {
            calculateBalanceForAllAccounts();
            return;
        }
        calculateBalanceForAccount();
    }

    private void calculateBalanceForAccount() {

        TaskWithReturn<List<Transaction>> task =  DbController.INSTANCE.getAllTransactionsForAccount(this.currentAccount);
        task.RegisterFailureEvent((e) -> this.setupErrorPopup("Unable to fetch transactions from the database.", e));
        task.startTask();
        List<Transaction> transactions = task.waitForResult();

        int amountSpent = 0;
        for(Transaction t : transactions) {
            amountSpent += t.getAmount();
        }

        TaskWithArgsReturn<Account, AccountBalance> balanceTask = DbController.INSTANCE.getBalanceForAccount(this.currentAccount);
        balanceTask.RegisterFailureEvent((e) -> this.setupErrorPopup("Unable to fetch current account balance from the database.", e));
        balanceTask.startTask();
        AccountBalance balance = balanceTask.waitForResult();

        this.setText(this.currentAccount.getName() + ": $" + (balance.getAmount() + amountSpent)/100.0);
    }

    private void calculateBalanceForAllAccounts() {
        this.setText("All Accounts Balance: ");
    }
}
