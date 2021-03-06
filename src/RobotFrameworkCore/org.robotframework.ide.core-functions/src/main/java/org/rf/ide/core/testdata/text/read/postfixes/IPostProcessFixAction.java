/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.postfixes;

import org.rf.ide.core.testdata.model.RobotFileOutput;

/**
 * @author wypych
 */
public interface IPostProcessFixAction {

    public void applyFix(final RobotFileOutput parsingOutput);
}
