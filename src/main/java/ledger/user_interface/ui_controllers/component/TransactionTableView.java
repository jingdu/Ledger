package ledger.user_interface.ui_controllers.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import ledger.controller.DbController;
import ledger.controller.register.TaskWithArgs;
import ledger.controller.register.TaskWithReturn;
import ledger.database.entity.Account;
import ledger.database.entity.Tag;
import ledger.database.entity.Transaction;
import ledger.user_interface.ui_controllers.IUIController;
import ledger.user_interface.ui_controllers.Startup;
import ledger.user_interface.ui_models.TransactionModel;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controls all input and interaction with the Main Page of the application
 */

public class TransactionTableView extends TableView<TransactionModel> implements IUIController, Initializable {

    private final static String pageLoc = "/fxml_files/TransactionTableView.fxml";

    private Account accountFilter;
    private String searchFilterString = "";

    public TransactionTableView() {
        this.initController(pageLoc, this, "Error on main page startup: ");
    }

    private void asyncTableUpdate() {
        Startup.INSTANCE.runLater(this::updateTransactionTableView);
    }

    private void configureTransactionTableView() {
        // Add ability to delete transactions form tableView
        this.setOnKeyPressed(t -> {
            //Put your awesome application specific logic here
            if (t.getCode() == KeyCode.DELETE) {
                handleDeleteSelectedTransactionsFromTableView();
            }
        });

        this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ContextMenu menu = new ContextMenu();
        MenuItem removeMenuItem = new MenuItem("Delete Selected Transaction(s)");
        menu.getItems().add(removeMenuItem);
        this.setContextMenu(menu);
        // removeMenuItem will remove the row from the table:
        removeMenuItem.setOnAction(event -> handleDeleteSelectedTransactionsFromTableView());
    }

    public void updateTransactionTableView() {
        // Update table rows
        TaskWithReturn<List<Transaction>> task;
        if (accountFilter == null) {
            task = DbController.INSTANCE.getAllTransactions();
        } else {
            task = DbController.INSTANCE.getAllTransactionsForAccount(accountFilter);
        }
        task.startTask();
        List<Transaction> transactions = task.waitForResult();

        ArrayList<TransactionModel> models = new ArrayList<>();
        for (Transaction trans : transactions) {
            models.add(new TransactionModel(trans));
        }
        ObservableList<TransactionModel> observableTransactionModels = FXCollections.observableList(models);

//        this.setItems(observableTransactionModels);

        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<TransactionModel> filteredData = new FilteredList<>(observableTransactionModels, p -> true);

        // 2. Set the filter Predicate.
        filteredData.setPredicate(transactionModel -> {
            // If filter text is empty, display all persons.
            if (searchFilterString == null || searchFilterString.isEmpty()) {
                return true;
            }

            // Compare first name and last name of every person with filter text.
            String lowerCaseFilter = searchFilterString.toLowerCase();

            if (transactionModel.getAmount().toLowerCase().contains(lowerCaseFilter)) {
                return true; // Filter matches amount.
            } else if (transactionModel.getPayee().getName().toLowerCase().contains(lowerCaseFilter)) {
                return true; // Filter matches Payee name.
            } else if (transactionModel.getTags().stream().map(Tag::getName).anyMatch(s -> s.toLowerCase().contains(lowerCaseFilter))) {
                return true; // Filter matches tags.
            } else {
                return false; // Filter does not match.
            }
        });

        // 3. Wrap the FilteredList in a SortedList.
        SortedList<TransactionModel> sortedData = new SortedList<>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(this.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        this.setItems(sortedData);
    }

    private void handleDeleteSelectedTransactionsFromTableView() {
        List<Integer> indices = new ArrayList<>();
        // Add indices to new list so they aren't observable
        indices.addAll(this.getSelectionModel().getSelectedIndices());
        if (indices.size() != 0) {

            //TODO: Get around this scary mess
            if (indices.contains(new Integer(-1))) {
                indices = this.getSelectionModel().getSelectedIndices();
            }

            for (int i : indices) {
                Transaction transactionToDelete = this.getItems().get(i).getTransaction();

                TaskWithArgs<Transaction> task = DbController.INSTANCE.deleteTransaction(transactionToDelete);
                task.RegisterFailureEvent((e) -> {
                    asyncTableUpdate();
                    setupErrorPopup("Error deleting transaction.", e);
                });
                task.startTask();
                task.waitForComplete();
            }
            updateTransactionTableView();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate listView w/ transactions from DB
        configureTransactionTableView();
        updateTransactionTableView();

        DbController.INSTANCE.registerTransationSuccessEvent(this::asyncTableUpdate);
        DbController.INSTANCE.registerPayyeeSuccessEvent(this::asyncTableUpdate);
    }

    public void updateAccountFilter(Account accountToFilterBy) {
        this.accountFilter = accountToFilterBy;
        this.asyncTableUpdate();
    }

    public void updateSearchFilterString(String searchFilterString) {
        this.searchFilterString = searchFilterString;
        this.asyncTableUpdate();
    }
}
