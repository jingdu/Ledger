package ledger.io.input;

import ledger.database.entity.Account;
import ledger.database.entity.Transaction;
import ledger.exception.ConverterException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by CJ on 12/12/2016.
 */
public abstract class AbstractUFXConverter implements IInAdapter<Transaction> {

    private File qfxFile;
    private Account account;

    public AbstractUFXConverter(File file, Account account) {
        this.qfxFile = file;
        this.account = account;
    }

    /**
     * Parses the given file into the application's internal transaction objects.
     *
     * @return A list of Transaction objects created from the transactions in the provided file.
     * @throws IOException When unable to read the given file
     */
    @Override
    public List<Transaction> convert() throws ConverterException {
        List<Transaction> transactions = new ArrayList();

        // read in given file
        String sgml = null;
        try {
            sgml = new Scanner(qfxFile).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            throw new ConverterException("The QFX file could not be found.", e);
        }

        // chop off everything before and after transactions (before/after STMTTRN)
        int indexOfFirstTrans = sgml.indexOf("<STMTTRN>");
        sgml = sgml.substring(indexOfFirstTrans);

        int lastIndexOfTrans = sgml.indexOf("</BANKTRANLIST>");
        if (lastIndexOfTrans == -1)
            throw new ConverterException("The provided QFX file is malformed.", new IndexOutOfBoundsException());
        sgml = sgml.substring(0, lastIndexOfTrans);

        StringBuilder correctedXml = correctXml(sgml);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ConverterException("Unable to create new XML parser.", e);
        }
        InputSource is = new InputSource(new StringReader(correctedXml.toString()));
        Document xml;
        try {
            xml = builder.parse(is);
        } catch (SAXException e) {
            throw new ConverterException("Unable to parse the given file.", e);
        } catch (IOException e) {
            throw new ConverterException("Unable to parse the given file.", e);
        }
        parseXml(transactions, xml);

        // return list
        return transactions;
    }

    protected abstract void parseXml(List<Transaction> transactions, Document xml) throws ConverterException;

    private StringBuilder correctXml(String sgml) {
        // add tags to the all rows that are not STMTTRN
        String[] splitPieces = sgml.split("<");
        LinkedList<String> modifiedPieces = new LinkedList();
        for (String piece : splitPieces) {
            piece = piece.trim();
            boolean matches = Pattern.matches("(?!STMTTRN[>].*|[/]STMTTRN[>].*).*[>].*", piece);
            if (matches) {
                int lastClosingTag = piece.indexOf(">");
                String xmlTag = piece.substring(0, lastClosingTag);
                String correctXml = "<" + piece + "</" + xmlTag + ">";
                modifiedPieces.add(correctXml + "\n");
            } else if (!piece.equals("")) {
                modifiedPieces.add("<" + piece + "\n");
            }
        }

        StringBuilder correctedXml = new StringBuilder();
        correctedXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" + "<QFX>");
        for (String piece : modifiedPieces) {
            correctedXml.append(piece);
        }
        correctedXml.append("\n" + "</QFX>");
        return correctedXml;
    }

    protected Account getAccount() {
        return account;
    }
}
