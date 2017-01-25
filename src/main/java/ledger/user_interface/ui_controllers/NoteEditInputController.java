package ledger.user_interface.ui_controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import ledger.controller.DbController;
import ledger.controller.register.CallableMethod;
import ledger.controller.register.TaskWithArgs;
import ledger.database.entity.Note;
import ledger.user_interface.ui_models.TransactionModel;
import org.controlsfx.control.table.TableRowExpanderColumn;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Control that allows for the editing of notes inside of the TransactionTableview.
 */
public class NoteEditInputController extends GridPane implements IUIController, Initializable {
    private static final String pageLoc = "/fxml_files/NoteEditInput.fxml";
    @FXML
    private TextArea noteText;
    @FXML
    private Button saveButton;
    private Note note;
    private CallableMethod<Boolean> collapseMethod;

    /**
     * Basic Constructor
     */
    public NoteEditInputController(){
        this.initController(pageLoc,this,"Unable to Load Note Editor");

    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        saveButton.setOnAction((event -> {
            note.setNoteText(noteText.getText());
            TaskWithArgs<Note> updateNoteTask = DbController.INSTANCE.editNote(note);
            updateNoteTask.RegisterSuccessEvent(()->{
                collapseMethod.call(false);
            });
            updateNoteTask.startTask();
            updateNoteTask.waitForComplete();
        }));
    }

    /**
     *
     * @param param this is the TableRowData that will be used to edit the Note data.
     */
    public void setTableRowData(TableRowExpanderColumn.TableRowDataFeatures<TransactionModel> param){
        if(param.getValue().getTransaction().getNote()!= null){
        this.note = param.getValue().getTransaction().getNote();
        this.noteText.setText(this.note.getNoteText());}
        else{
            note = new Note("");
        }
        collapseMethod = param::setExpanded;
    }
}
