package org.robotframework.ide.core.testData.model;

public abstract class AModelElement implements IOptional {

    public abstract ModelType getModelType();


    public abstract FilePosition getBeginPosition();

}
