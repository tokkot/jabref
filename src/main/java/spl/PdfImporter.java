package spl;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.PdfContentImporter;
import net.sf.jabref.importer.fileformat.PdfXmpImporter;
import net.sf.jabref.logic.id.IdGenerator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelPattern.LabelPatternUtil;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.util.Util;
import net.sf.jabref.logic.xmp.XMPUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import spl.filter.PdfFileFilter;
import spl.gui.ImportDialog;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 08.09.2010
 * Time: 14:49:08
 * To change this template use File | Settings | File Templates.
 */
public class PdfImporter {

    private final JabRefFrame frame;
    private final BasePanel panel;
    private MainTable entryTable;
    private int dropRow;
    
    private static final Log LOGGER = LogFactory.getLog(PdfImporter.class);


    /**
     * Creates the PdfImporter
     * 
     * @param frame the JabRef frame
     * @param panel the panel to use
     * @param entryTable the entry table to work on
     * @param dropRow the row the entry is dropped to. May be -1 to indicate that no row is selected.
     */
    public PdfImporter(JabRefFrame frame, BasePanel panel, MainTable entryTable, int dropRow) {
        this.frame = frame;
        this.panel = panel;
        this.entryTable = entryTable;
        this.dropRow = dropRow;
    }


    public class ImportPdfFilesResult {

        public String[] noPdfFiles;
        public List<BibtexEntry> entries;
    }


    /**
     * 
     * Imports the PDF files given by fileNames
     * 
     * @param fileNames states the names of the files to import
     * @return list of successful created BibTeX entries and list of non-PDF files
     */
    public ImportPdfFilesResult importPdfFiles(String[] fileNames, OutputPrinter status) {
        // sort fileNames in PDFfiles to import and other files
        // PDFfiles: variable files
        // other files: variable noPdfFiles
        List<String> files = new ArrayList<String>(Arrays.asList(fileNames));
        List<String> noPdfFiles = new ArrayList<String>();
        PdfFileFilter pdfFilter = new PdfFileFilter();
        for (String file : files) {
            if (!pdfFilter.accept(file)) {
                noPdfFiles.add(file);
            }
        }
        files.removeAll(noPdfFiles);
        // files and noPdfFiles correctly sorted

        // import the files
        List<BibtexEntry> entries = importPdfFiles(files, status);

        String[] noPdfFilesArray = new String[noPdfFiles.size()];
        noPdfFiles.toArray(noPdfFilesArray);

        ImportPdfFilesResult res = new ImportPdfFilesResult();
        res.noPdfFiles = noPdfFilesArray;
        res.entries = entries;
        return res;
    }

    /**
     * @param fileNames - PDF files to import
     * @return true if the import succeeded, false otherwise
     */
    private List<BibtexEntry> importPdfFiles(List<String> fileNames, OutputPrinter status) {
        if (panel == null) {
            return Collections.emptyList();
        }
        ImportDialog importDialog = null;
        boolean doNotShowAgain = false;
        boolean neverShow = Globals.prefs.getBoolean(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE);
        int globalChoice = Globals.prefs.getInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE);

        // Get a list of file directories:
        String[] dirsS = panel.metaData().getFileDirectory(Globals.FILE_FIELD);

        List<BibtexEntry> res = new ArrayList<BibtexEntry>();

        fileNameLoop: for (String fileName : fileNames) {
            List<BibtexEntry> xmpEntriesInFile = readXmpEntries(fileName);
            if (!neverShow && !doNotShowAgain) {
                importDialog = new ImportDialog(dropRow >= 0, fileName);
                if (!hasXmpEntries(xmpEntriesInFile)) {
                    importDialog.disableXMPChoice();
                }
                Tools.centerRelativeToWindow(importDialog, frame);
                importDialog.showDialog();
                doNotShowAgain = importDialog.getDoNotShowAgain();
            }
            if (neverShow || importDialog.getResult() == JOptionPane.OK_OPTION) {
                int choice = neverShow ? globalChoice : importDialog.getChoice();
                DroppedFileHandler dfh;
                BibtexEntry entry;
                BibtexEntryType type;
                InputStream in = null;
                List<BibtexEntry> localRes = null;
                switch (choice) {
                case ImportDialog.XMP:
                    //SplDatabaseChangeListener dataListener = new SplDatabaseChangeListener(frame, panel, entryTable, fileName);
                    //panel.database().addDatabaseChangeListener(dataListener);
                    //ImportMenuItem importer = new ImportMenuItem(frame, (entryTable == null));
                    PdfXmpImporter importer = new PdfXmpImporter();
                    try {
                        in = new FileInputStream(fileName);
                        localRes = importer.importEntries(in, frame);
                        //importer.automatedImport(new String[]{ fileName });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (Exception ignored) {
                        }
                    }

                    if (localRes == null || localRes.isEmpty()) {
                        // import failed -> generate default entry
                        LOGGER.info(Localization.lang("Import failed"));
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    }

                    // only one entry is imported
                    entry = localRes.get(0);

                    // insert entry to database and link file
                    panel.database().insertEntry(entry);
                    panel.markBaseChanged();
                    FileListTableModel tm = new FileListTableModel();
                    File toLink = new File(fileName);
                    tm.addEntry(0, new FileListEntry(toLink.getName(),
                            FileUtil.shortenFileName(toLink, dirsS).getPath(),
                            Globals.prefs.getExternalFileTypeByName("pdf")));
                    entry.setField(Globals.FILE_FIELD, tm.getStringRepresentation());
                    res.add(entry);
                    break;

                case ImportDialog.CONTENT:
                    PdfContentImporter contentImporter = new PdfContentImporter();

                    File file = new File(fileName);

                    try {
                        in = new FileInputStream(file);
                    } catch (Exception e) {
                        // import failed -> generate default entry
                        LOGGER.info(Localization.lang("Import failed"), e);
                        e.printStackTrace();
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    }
                    try {
                        localRes = contentImporter.importEntries(in, status);
                    } catch (Exception e) {
                        // import failed -> generate default entry
                        LOGGER.info(Localization.lang("Import failed"), e);
                        e.printStackTrace();
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    } finally {
                        try {
                            in.close();
                        } catch (Exception ignored) {
                        }
                    }

                    // import failed -> generate default entry
                    if (localRes == null || localRes.isEmpty()) {
                        entry = createNewBlankEntry(fileName);
                        res.add(entry);
                        continue fileNameLoop;
                    }

                    // only one entry is imported
                    entry = localRes.get(0);

                    // insert entry to database and link file

                    panel.database().insertEntry(entry);
                    panel.markBaseChanged();
                    LabelPatternUtil.makeLabel(panel.metaData(), panel.database(), entry);
                    dfh = new DroppedFileHandler(frame, panel);
                    dfh.linkPdfToEntry(fileName, entryTable, entry);
                    panel.highlightEntry(entry);
                    if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                        EntryEditor editor = panel.getEntryEditor(entry);
                        panel.showEntryEditor(editor);
                        panel.adjustSplitter();
                    }
                    res.add(entry);
                    break;
                case ImportDialog.NOMETA:
                    entry = createNewBlankEntry(fileName);
                    res.add(entry);
                    break;
                case ImportDialog.ONLYATTACH:
                    dfh = new DroppedFileHandler(frame, panel);
                    dfh.linkPdfToEntry(fileName, entryTable, dropRow);
                    break;
                }
            }

        }
        return res;
    }

    private BibtexEntry createNewBlankEntry(String fileName) {
        BibtexEntry newEntry = createNewEntry();
        if (newEntry != null) {
            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
            dfh.linkPdfToEntry(fileName, entryTable, newEntry);
        }
        return newEntry;
    }

    private boolean fieldExists(String string) {
        return string != null && !string.isEmpty();
    }

    private BibtexEntry createNewEntry() {

        // Find out what type is wanted.
        EntryTypeDialog etd = new EntryTypeDialog(frame);
        // We want to center the dialog, to make it look nicer.
        Util.placeDialog(etd, frame);
        etd.setVisible(true);
        BibtexEntryType type = etd.getChoice();

        if (type != null) { // Only if the dialog was not cancelled.
            String id = IdGenerator.next();
            final BibtexEntry be = new BibtexEntry(id, type);
            try {
                panel.database().insertEntry(be);

                // Set owner/timestamp if options are enabled:
                ArrayList<BibtexEntry> list = new ArrayList<BibtexEntry>();
                list.add(be);
                Util.setAutomaticFields(list, true, true, false);

                // Create an UndoableInsertEntry object.
                panel.undoManager.addEdit(new UndoableInsertEntry(panel.database(), be, panel));
                panel.output(Localization.lang("Added new") + " '" + type.getName().toLowerCase() + "' "
                        + Localization.lang("entry") + ".");

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (panel.getMode() != BasePanel.SHOWING_EDITOR) {
                    panel.setMode(BasePanel.WILL_SHOW_EDITOR);
                }

                /*int row = entryTable.findEntry(be);
                if (row >= 0)
                    // Selects the entry. The selection listener will open the editor.                      
                     if (row >= 0) {
                        try{
                            entryTable.setRowSelectionInterval(row, row);
                        }catch(IllegalArgumentException e){
                            System.out.println("RowCount: " + entryTable.getRowCount());
                        }

                        //entryTable.setActiveRow(row);
                        entryTable.ensureVisible(row);
                     }
                else {
                    // The entry is not visible in the table, perhaps due to a filtering search
                    // or group selection. Show the entry editor anyway:
                    panel.showEntry(be);
                }   */
                panel.showEntry(be);
                panel.markBaseChanged(); // The database just changed.
                new FocusRequester(panel.getEntryEditor(be));
                return be;
            } catch (KeyCollisionException ex) {
                LOGGER.info("Key collision occured", ex);
            }
        }
        return null;
    }

    private List<BibtexEntry> readXmpEntries(String fileName) {
        List<BibtexEntry> xmpEntriesInFile = null;
        try {
            xmpEntriesInFile = XMPUtil.readXMP(fileName);
        } catch (Exception e) {
            // Todo Logging
        }
        return xmpEntriesInFile;
    }

    private boolean hasXmpEntries(List<BibtexEntry> xmpEntriesInFile) {
        return !(xmpEntriesInFile == null || xmpEntriesInFile.isEmpty());
    }

    public MainTable getEntryTable() {
        return entryTable;
    }

    public void setEntryTable(MainTable entryTable) {
        this.entryTable = entryTable;
    }

    public int getDropRow() {
        return dropRow;
    }

    public void setDropRow(int dropRow) {
        this.dropRow = dropRow;
    }
}
