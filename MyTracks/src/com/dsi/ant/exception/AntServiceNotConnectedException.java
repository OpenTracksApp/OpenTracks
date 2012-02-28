package com.dsi.ant.exception;

public class AntServiceNotConnectedException extends AntInterfaceException 
{
    /**
     * 
     */
    private static final long serialVersionUID = -2085081032170129309L;

    public AntServiceNotConnectedException()
    {
        this("ANT Interface error, ANT Radio Service not connected.");
    }
    
    public AntServiceNotConnectedException(String string)
    {
        super(string);
    }
}
