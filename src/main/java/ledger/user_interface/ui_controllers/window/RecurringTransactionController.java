package ledger.user_interface.ui_controllers.window;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import ledger.controller.DbController;
import ledger.controller.register.TaskNoReturn;
import ledger.controller.register.TaskWithReturn;
import ledger.database.entity.*;
import ledger.user_interface.ui_controllers.IUIController;
import ledger.user_interface.ui_controllers.Startup;
import ledger.user_interface.ui_controllers.component.AccountDropdown;
import ledger.user_interface.ui_controllers.component.PayeeDropdown;
import ledger.user_interface.utils.InputSanitization;
import ledger.user_interface.utils.TypeStringConverter;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Add Recurring Transaction Window.
 */
public class RecurringTransactionController extends GridPane implements IUIController, Initializable {
    private static final String pageLoc = "/fxml_files/RecurringTransactionWindow.fxml";

    @FXML
    DatePicker startDatePicker;
    @FXML
    DatePicker endDatePicker;
    @FXML
    PayeeDropdown payeeDropdown;
    @FXML
    TextField amountField;
    @FXML
    AccountDropdown accountDropdown;
    @FXML
    ChoiceBox<Type> typeText;
    @FXML
    ChoiceBox<String> frequencyText;
    @FXML
    Button addButton;

    public RecurringTransactionController() {
        this.initController(pageLoc, this, "Unable to load recurring bill payment window");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TaskWithReturn<List<Frequency>> freqTask = DbController.INSTANCE.getAllFrequencies();
        freqTask.startTask();
        List<Frequency> frequencies = freqTask.waitForResult();
        for (Frequency freq : frequencies) {
            this.frequencyText.getItems().add(Frequency.convertFrequencyToString(freq));
        }
        this.frequencyText.setVisible(true);

        this.typeText.setDisable(true);
        TaskWithReturn<List<Type>> typeTask = DbController.INSTANCE.getAllTypes();
        typeTask.RegisterFailureEvent(Throwable::printStackTrace);
        typeTask.RegisterSuccessEvent((list) -> {
            this.typeText.setItems(FXCollections.observableArrayList(list));
            this.typeText.setConverter(new TypeStringConverter());
            this.typeText.setDisable(false);
        });
        typeTask.startTask();

        this.addButton.setOnAction(this::addRecurringTransaction);
    }

    private void addRecurringTransaction(ActionEvent actionEvent) {
        LocalDate localStartDate = this.startDatePicker.getValue();
        if (localStartDate == null) {
            this.setupErrorPopup("An invalid start date has been provided.");
            return;
        }
        Instant instant = Instant.from(localStartDate.atStartOfDay(ZoneId.systemDefault()));
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(instant.toEpochMilli());

        LocalDate localEndDate = this.endDatePicker.getValue();
        if (localEndDate == null) {
            this.setupErrorPopup("An invalid ending date has been provided.");
            return;
        }
        Calendar endDate = Calendar.getInstance();
        instant = Instant.from(localEndDate.atStartOfDay(ZoneId.systemDefault()));
        endDate.setTimeInMillis(instant.toEpochMilli());

        if (!startDate.before(endDate)) {
            this.setupErrorPopup("The Start Date must be before the End Date");
            return;
        }

        String frequencyName = this.frequencyText.getValue();
        if (frequencyName == null || frequencyName == "") {
            this.setupErrorPopup("Invalid Frequency selection.");
            return;
        }
        Frequency frequency = Frequency.convertStringToFrequency(frequencyName);

        Payee payee = this.payeeDropdown.getSelectedPayee();
        if (InputSanitization.isInvalidPayee(payee)) {
            this.setupErrorPopup("An invalid Payee has been provided.");
            return;
        }

        Account account = this.accountDropdown.getSelectedAccount();
        if (account == null) {
            this.setupErrorPopup("No account has been selected.");
            return;
        }

        if (InputSanitization.isInvalidAmount(this.amountField.getText())) {
            this.setupErrorPopup("Invalid amount entry.");
            return;
        }
        String amountString = this.amountField.getText();
        if (amountString.charAt(0) == '$') {
            amountString = amountString.substring(1);
        }
        double amountToSetDecimal = Double.parseDouble(amountString);
        long amount = Math.round(amountToSetDecimal * 100);

        Note note = new Note("Automatically generated from a Recurring Transaction.");

        Type type = this.typeText.getValue();
        if (type == null) {
            this.setupErrorPopup("No type selected.", new Exception());
            return;
        }

        if ((amount < 0) && (type.getName().equals("Account Credit") || type.getName().equals("Misc Credit"))) {
            setupErrorPopup("Transactions of the " + type.getName() + " type must have a positive amount.");
            return;
        }

        if ((amount > 0) && !(type.getName().equals("Account Credit") || type.getName().equals("Misc Credit"))) {
            setupErrorPopup("Transactions of the " + type.getName() + " type must have a negative amount.");
            return;
        }

        RecurringTransaction recurringTrans = new RecurringTransaction(startDate, endDate, type, amount, account, payee,
                null, note, frequency);
        TaskNoReturn addRecurringTransTask = DbController.INSTANCE.insertRecurringTransaction(recurringTrans);
        addRecurringTransTask.RegisterSuccessEvent(this::closeWindow);
        addRecurringTransTask.RegisterFailureEvent(Throwable::printStackTrace);
        addRecurringTransTask.startTask();
    }

    private void closeWindow() {
        Startup.INSTANCE.runLater(() -> ((Stage) this.getScene().getWindow()).close());
    }
}