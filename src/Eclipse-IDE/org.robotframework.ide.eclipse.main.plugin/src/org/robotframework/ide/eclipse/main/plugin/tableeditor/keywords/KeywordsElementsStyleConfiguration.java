/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.keywords;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.ImagePainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.CellPainterDecorator;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.ui.util.CellEdgeEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.InactiveCellPainter;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.TableConfigurationLabels;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.TableThemes.TableTheme;
import org.robotframework.red.graphics.ColorsManager;
import org.robotframework.red.graphics.FontsManager;
import org.robotframework.red.graphics.ImagesManager;
import org.robotframework.red.nattable.painter.RedTableTextPainter;

public class KeywordsElementsStyleConfiguration extends AbstractRegistryConfiguration {

    private final Font font;

    private final boolean isEditable;

    private final boolean wrapCellContent;

    public KeywordsElementsStyleConfiguration(final TableTheme theme, final boolean isEditable,
            final boolean wrapCellContent) {
        this.font = theme.getFont();
        this.isEditable = isEditable;
        this.wrapCellContent = wrapCellContent;
    }

    @Override
    public void configureRegistry(final IConfigRegistry configRegistry) {
        final Style keywordStyle = new Style();
        final Style argumentStyle = new Style();
        final Style settingStyle = new Style();

        final Color argumentForegroundColor = isEditable ? ColorsManager.getColor(30, 127, 60)
                : ColorsManager.getColor(200, 200, 200);
        argumentStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, argumentForegroundColor);
        final Color settingForegroundColor = isEditable ? ColorsManager.getColor(149, 0, 85)
                : ColorsManager.getColor(200, 200, 200);
        settingStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, settingForegroundColor);
        keywordStyle.setAttributeValue(CellStyleAttributes.FONT, getFont(font, SWT.BOLD));

        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, keywordStyle, DisplayMode.NORMAL,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_CONFIG_LABEL);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, keywordStyle, DisplayMode.HOVER,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_CONFIG_LABEL);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, keywordStyle, DisplayMode.SELECT,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_CONFIG_LABEL);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, keywordStyle, DisplayMode.SELECT_HOVER,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_CONFIG_LABEL);

        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, argumentStyle, DisplayMode.NORMAL,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_ARGUMENT_CONFIG_LABEL);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, argumentStyle, DisplayMode.SELECT,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_ARGUMENT_CONFIG_LABEL);

        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, settingStyle, DisplayMode.NORMAL,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_SETTING_CONFIG_LABEL);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, settingStyle, DisplayMode.SELECT,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_SETTING_CONFIG_LABEL);

        final ImageDescriptor keywordImage = RedImages.getUserKeywordImage();
        final Image imageToUse = ImagesManager
                .getImage(isEditable ? keywordImage : RedImages.getGrayedImage(keywordImage));

        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, new InactiveCellPainter(),
                DisplayMode.NORMAL, TableConfigurationLabels.CELL_NOT_EDITABLE_LABEL);

        final ICellPainter cellPainter = new CellPainterDecorator(new RedTableTextPainter(wrapCellContent, 2),
                CellEdgeEnum.LEFT, new ImagePainter(imageToUse));
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, cellPainter, DisplayMode.NORMAL,
                KeywordsElementsLabelAccumulator.KEYWORD_DEFINITION_CONFIG_LABEL);
    }

    private Font getFont(final Font fontToReuse, final int style) {
        final Font currentFont = fontToReuse == null ? Display.getCurrent().getSystemFont() : fontToReuse;
        final FontDescriptor fontDescriptor = FontDescriptor.createFrom(currentFont).setStyle(style);
        return FontsManager.getFont(fontDescriptor);
    }

}
