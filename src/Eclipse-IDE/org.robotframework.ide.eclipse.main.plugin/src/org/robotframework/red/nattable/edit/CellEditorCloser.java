package org.robotframework.red.nattable.edit;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.edit.editor.ICellEditor;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.robotframework.red.swt.SwtThread;

public class CellEditorCloser {

    public static void closeForcibly(final NatTable table) {
        final ICellEditor cellEditor = table.getActiveCellEditor();
        if (cellEditor != null && !cellEditor.isClosed()) {
            final boolean committed = cellEditor.commit(MoveDirectionEnum.NONE);
            if (!committed) {
                cellEditor.close();
            }
            SwtThread.asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (table != null && !table.isDisposed()) {
                        table.setFocus();
                    }
                }
            });
        }
    }
}
