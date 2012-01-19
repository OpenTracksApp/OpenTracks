package com.dsi.ant.exception;

public class AntInterfaceException extends Exception 
{

    /**
     * 
     */
    private static final long serialVersionUID = -7278855366167722274L;

    public AntInterfaceException()
    {
        this("Unknown ANT Interface error");
    }

    public AntInterfaceException(String string)
    {
        super(string);
    }
}
