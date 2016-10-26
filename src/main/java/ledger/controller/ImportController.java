package ledger.controller;

import ledger.controller.register.TaskWithArgs;
import ledger.controller.register.TaskWithArgsReturn;
import ledger.database.IDatabase;
import ledger.database.entity.Account;
import ledger.database.entity.Transaction;
import ledger.io.importer.Importer;
import ledger.io.input.*;

import java.io.File;
import java.util.List;

/**
 * Handles all translation from File to putting into storage
 */
public class ImportController {
    public static ImportController INSTANCE;

    static {
        INSTANCE = new ImportController();
    }

    private ImportController() {
    }

    public Converter[] getAvaliableConverters() {
        return Converter.values();
    }

    /**
     * Returns a task that handles the file import with a given Converter type, file path and an account to link too.
     * It will also run duplicate detection.
     */
    public TaskWithArgs<Account> importTransactions(Converter type, File path, Account account) {
        return new TaskWithArgs<Account>((acc) -> {
            IInAdapter<Transaction> converter = type.method.create(path, acc);

            List<Transaction> trans = converter.convert();

            DuplicateDetector dups = new DuplicateDetector(trans);
            DetectionResult result = dups.detectDuplicates(DbController.INSTANCE.getDb());

            if (result.getPossibleDuplicates().size() != 0) {
                //TODO how to show user
                // Throw Some Exception
                return;
            }

            TaskWithArgsReturn<List<Transaction>,List<Transaction>> task = DbController.INSTANCE.batchInsertTransaction(result.getVerifiedTransactions());
            task.startTask();
            List<Transaction> failedTransactions = task.waitForResult();

        }, account);
    }

    /**
     * Enum that holds the available converters in an easy to instantiate format
     */
    public enum Converter {
        Chase_CSV("Chase Bank CSV", ChaseConverter::new),
        Qfx("Quicken QFX", QfxConverter::new);

        private String niceName;
        private ConverterConstructor method;

        Converter(String niceName, ConverterConstructor method) {
            this.niceName = niceName;
            this.method = method;
        }

        @Override
        public String toString() {
            return niceName;
        }

        /**
         * @return A Class with one method that instantiates a IInAdapter
         */
        public ConverterConstructor getMethod() {
            return method;
        }

        /**
         * Abstract Lambda for creating an IInAdapter
         */
        public interface ConverterConstructor {
            IInAdapter<Transaction> create(File file, Account account);
        }

    }
}
