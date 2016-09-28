package ledger.database.storage;

import ledger.database.IDatabase;
import ledger.database.enity.*;
import org.junit.*;
import ledger.database.enity.Transaction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

import java.util.*;

public class SQLiteDatabaseTest {

    private static IDatabase database;
    private static Transaction sampleTransaction1;
    private static Transaction sampleTransaction2;
    private static Transaction sampleTransaction3;
    private static Type sampleType;
    private static Account sampleAccount;
    private static Payee samplePayee;
    private static Tag sampleTag;
    private static Note sampleNote;

    @BeforeClass
    public static void setupSampleObjects() {
        sampleType = new Type("Credit", "Purchased with a credit card");
        sampleAccount = new Account("Chase", "Credit account with Chase Bank");
        samplePayee = new Payee("Meijer", "Grocery store");
        sampleTag = new Tag("Groceries", "Money spent on groceries");
        sampleNote = new Note("This is a note");

        ArrayList<Tag> sampleTagList = new ArrayList<>();
        sampleTagList.add(sampleTag);

        sampleTransaction1 = new Transaction(new Date(), sampleType, 4201, sampleAccount, samplePayee, true, sampleTagList, sampleNote);
        sampleTransaction2 = new Transaction(new Date(), sampleType, 103, sampleAccount, samplePayee, true, sampleTagList, sampleNote);
        sampleTransaction3 = new Transaction(new Date(), sampleType, 3304, sampleAccount, samplePayee, false, sampleTagList, sampleNote);
    }

    @Before
    public void setupDatabase() throws Exception {
        database = new SQLiteDatabase(null, "src/test/resources/test.db");
    }

    @Test
    public void insertTransaction() throws Exception {
        database.insertTransaction(sampleTransaction1);

        List<Transaction> trans = database.getAllTransactions();

        assertEquals(1, trans.size());
    }

    @Test
    public void deleteTransaction() throws Exception {
        database.insertTransaction(this.sampleTransaction1);
        database.insertTransaction(this.sampleTransaction2);
        database.insertTransaction(this.sampleTransaction3);

        List<Transaction> transactionsBeforeDelete = database.getAllTransactions();
        int countBeforeDelete = transactionsBeforeDelete.size();

        Transaction transactionToDelete = transactionsBeforeDelete.get(0);
        database.deleteTransaction(transactionToDelete);

        List<Transaction> transactionsAfterDelete = database.getAllTransactions();
        int countAfterDelete = transactionsAfterDelete.size();

        assertEquals(countBeforeDelete - 1, countAfterDelete);

        ArrayList<Integer> IDsAfterDelete = new ArrayList<>();
        for (Transaction currentTransaction : transactionsAfterDelete) {
            IDsAfterDelete.add(currentTransaction.getId());
        }

        assertFalse(IDsAfterDelete.contains(transactionToDelete.getId()));
    }

    @Test
    public void editTransaction() throws Exception {

    }

    @Test
    public void getAllTransactions() throws Exception {
        database.insertTransaction(sampleTransaction1);
        database.insertTransaction(sampleTransaction2);
        database.insertTransaction(sampleTransaction3);

        List<Transaction> trans = database.getAllTransactions();

        assertEquals(3, trans.size());
    }

    @Test
    public void insertAccount() throws Exception {

    }

    @Test
    public void deleteAccount() throws Exception {

    }

    @Test
    public void editAccount() throws Exception {

    }

    @Test
    public void insertPayee() throws Exception {

    }

    @Test
    public void deletePayee() throws Exception {

    }

    @Test
    public void editPayee() throws Exception {

    }

    @Test
    public void insertNote() throws Exception {

    }

    @Test
    public void deleteNote() throws Exception {

    }

    @Test
    public void editNote() throws Exception {

    }

    @Test
    public void insertType() throws Exception {

    }

    @Test
    public void deleteType() throws Exception {

    }

    @Test
    public void editType() throws Exception {

    }

    @Test
    public void insertTag() throws Exception {

    }

    @Test
    public void deleteTag() throws Exception {

    }

    @Test
    public void editTag() throws Exception {

    }

    @After
    public void afterTests() throws Exception {
        database.shutdown();

        Path dbPath = Paths.get("src/test/resources/test.db");
        Files.delete(dbPath);
    }
}
