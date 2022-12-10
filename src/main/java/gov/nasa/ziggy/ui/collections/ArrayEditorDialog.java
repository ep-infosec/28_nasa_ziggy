package gov.nasa.ziggy.ui.collections;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.ziggy.ui.common.FloatingPointTableCellRenderer;
import gov.nasa.ziggy.ui.common.MessageUtil;

/**
 * Dialog for editing the contents of a Java array
 *
 * @author Todd Klaus
 */
@SuppressWarnings("serial")
public class ArrayEditorDialog extends javax.swing.JDialog {
    private static final Logger log = LoggerFactory.getLogger(ArrayEditorDialog.class);

    private JPanel dataPanel;
    private JButton removeButton;
    private JButton addButton;
    private JTable elementsTable;
    private JScrollPane scrollPane;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel actionPanel;

    private boolean isCancelled = false;
    private JButton exportButton;
    private JButton importButton;
    private JTextField addTextField;
    private JPanel addPanel;

    private ArrayEditorTableModel arrayEditorTableModel;

    private Object array;

    public ArrayEditorDialog(JFrame frame) {
        super(frame);
        initGUI();
    }

    public ArrayEditorDialog(JFrame owner, Object array) {
        super(owner, true);
        init(array);
    }

    public ArrayEditorDialog(JDialog owner, Object array) {
        super(owner, true);
        init(array);
    }

    private void init(Object array) {
        this.array = array;

        initGUI();

        elementsTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        elementsTable.getColumnModel().getColumn(1).setPreferredWidth(300);
    }

    private void addButtonActionPerformed(ActionEvent evt) {
        log.debug("addButton.actionPerformed, event=" + evt);

        addElement();
    }

    private void addTextFieldActionPerformed(ActionEvent evt) {
        log.debug("addTextField.actionPerformed, event=" + evt);

        addElement();
    }

    private void addElement() {
        int selectedIndex = elementsTable.getSelectedRow();

        if (selectedIndex == -1) {
            arrayEditorTableModel.insertElementAtEnd(addTextField.getText());
        } else {
            arrayEditorTableModel.insertElementAt(selectedIndex, addTextField.getText());
            elementsTable.getSelectionModel()
                .setSelectionInterval(selectedIndex + 1, selectedIndex + 1);
        }

        addTextField.setText("");
    }

    private void removeButtonActionPerformed(ActionEvent evt) {
        log.debug("removeButton.actionPerformed, event=" + evt);

        int selectedIndex = elementsTable.getSelectedRow();

        if (selectedIndex != -1) {
            arrayEditorTableModel.removeElementAt(selectedIndex);

            int newSize = arrayEditorTableModel.getRowCount();
            if (selectedIndex < newSize) {
                elementsTable.getSelectionModel()
                    .setSelectionInterval(selectedIndex, selectedIndex);
            }
        }
    }

    private void importButtonActionPerformed(ActionEvent evt) {
        log.debug("importButton.actionPerformed, event=" + evt);

        try {
            JFileChooser fc = new JFileChooser();
            int returnVal = fc.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();

                List<String> newArray = ArrayImportExportUtils.importArray(file);
                arrayEditorTableModel.replaceWith(newArray);
            }
        } catch (Exception e) {
            MessageUtil.showError(this, e);
        }
    }

    private void exportButtonActionPerformed(ActionEvent evt) {
        log.debug("exportButton.actionPerformed, event=" + evt);

        try {
            JFileChooser fc = new JFileChooser();
            int returnVal = fc.showSaveDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();

                List<String> values = arrayEditorTableModel.asStringList();
                ArrayImportExportUtils.exportArray(file, values);
            }
        } catch (Exception e) {
            MessageUtil.showError(this, e);
        }
    }

    private void okButtonActionPerformed(ActionEvent evt) {
        log.debug("okButton.actionPerformed, event=" + evt);

        setVisible(false);
    }

    private void cancelButtonActionPerformed(ActionEvent evt) {
        log.debug("cancelButton.actionPerformed, event=" + evt);

        isCancelled = true;
        setVisible(false);
    }

    public static ArrayEditorDialog newDialog(Component owner, Object array) {
        Window ownerWindow = findParentWindow(owner);

        ArrayEditorDialog editor;
        if (ownerWindow instanceof JFrame) {
            editor = new ArrayEditorDialog((JFrame) ownerWindow, array);
        } else {
            editor = new ArrayEditorDialog((JDialog) ownerWindow, array);
        }

        editor.setLocationRelativeTo(owner);
        return editor;
    }

    public Object editedArray() {
        return arrayEditorTableModel.asArray();
    }

    private static Window findParentWindow(Component c) {
        Component root = c;

        while (!(root instanceof JFrame) && !(root instanceof JDialog)) {
            root = root.getParent();
            if (root == null) {
                return null;
            }
        }
        return (Window) root;
    }

    private void initGUI() {
        try {
            setTitle("Array Editor");
            setPreferredSize(new java.awt.Dimension(491, 762));
            getContentPane().add(getDataPanel(), BorderLayout.CENTER);
            getContentPane().add(getActionPanel(), BorderLayout.SOUTH);
            this.setSize(491, 762);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JPanel getDataPanel() {
        if (dataPanel == null) {
            dataPanel = new JPanel();
            BorderLayout dataPanelLayout = new BorderLayout();
            dataPanel.setLayout(dataPanelLayout);
            dataPanel.add(getScrollPane(), BorderLayout.CENTER);
            dataPanel.add(getAddPanel(), BorderLayout.SOUTH);
        }
        return dataPanel;
    }

    private JPanel getActionPanel() {
        if (actionPanel == null) {
            actionPanel = new JPanel();
            FlowLayout actionPanelLayout = new FlowLayout();
            actionPanelLayout.setHgap(50);
            actionPanel.setLayout(actionPanelLayout);
            actionPanel.add(getOkButton());
            actionPanel.add(getCancelButton());
        }
        return actionPanel;
    }

    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setText("ok");
            okButton.addActionListener(evt -> okButtonActionPerformed(evt));
        }
        return okButton;
    }

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setText("cancel");
            cancelButton.addActionListener(evt -> cancelButtonActionPerformed(evt));
        }
        return cancelButton;
    }

    private JScrollPane getScrollPane() {
        if (scrollPane == null) {
            scrollPane = new JScrollPane();
            scrollPane.setViewportView(getElementsTable());
        }
        return scrollPane;
    }

    private JTable getElementsTable() {
        if (elementsTable == null) {
            elementsTable = new JTable();
            elementsTable.setDefaultRenderer(Float.class, new FloatingPointTableCellRenderer());
            elementsTable.setDefaultRenderer(Double.class, new FloatingPointTableCellRenderer());
            arrayEditorTableModel = new ArrayEditorTableModel(array);
            elementsTable.setModel(arrayEditorTableModel);
        }
        return elementsTable;
    }

    private JButton getAddButton() {
        if (addButton == null) {
            addButton = new JButton();
            addButton.setText("+");
            addButton.setToolTipText(
                "Insert the specified element before the selected row (or at the end if no row is selected)");
            addButton.addActionListener(evt -> addButtonActionPerformed(evt));
        }
        return addButton;
    }

    private JButton getRemoveButton() {
        if (removeButton == null) {
            removeButton = new JButton();
            removeButton.setText("-");
            removeButton.setToolTipText("Remove the element at the selected row");
            removeButton.addActionListener(evt -> removeButtonActionPerformed(evt));
        }
        return removeButton;
    }

    /**
     * Auto-generated main method to display this JDialog
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            ArrayEditorDialog inst = new ArrayEditorDialog(frame);
            inst.setVisible(true);
        });
    }

    private JPanel getAddPanel() {
        if (addPanel == null) {
            addPanel = new JPanel();
            GridBagLayout addPanelLayout = new GridBagLayout();
            addPanel.setBorder(BorderFactory.createTitledBorder("add/remove elements"));
            addPanelLayout.rowWeights = new double[] { 0.1 };
            addPanelLayout.rowHeights = new int[] { 7 };
            addPanelLayout.columnWeights = new double[] { 1.0, 0.1, 0.1 };
            addPanelLayout.columnWidths = new int[] { 7, 7, 7 };
            addPanel.setLayout(addPanelLayout);
            addPanel.add(getAddTextField(),
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            addPanel.add(getAddButton(), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            addPanel.add(getRemoveButton(), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            addPanel.add(getImportButton(), new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            addPanel.add(getExportButton(), new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        }
        return addPanel;
    }

    private JTextField getAddTextField() {
        if (addTextField == null) {
            addTextField = new JTextField();
            addTextField.addActionListener(evt -> addTextFieldActionPerformed(evt));
        }
        return addTextField;
    }

    private JButton getImportButton() {
        if (importButton == null) {
            importButton = new JButton();
            importButton.setText("import");
            importButton.addActionListener(evt -> importButtonActionPerformed(evt));
        }
        return importButton;
    }

    private JButton getExportButton() {
        if (exportButton == null) {
            exportButton = new JButton();
            exportButton.setText("export");
            exportButton.addActionListener(evt -> exportButtonActionPerformed(evt));
        }
        return exportButton;
    }

    public boolean wasCancelled() {
        return isCancelled;
    }

}
