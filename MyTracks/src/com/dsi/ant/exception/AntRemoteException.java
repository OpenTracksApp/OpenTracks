package com.dsi.ant.exception;

import android.os.RemoteException;

public class AntRemoteException extends AntInterfaceException 
{
    /**
     * 
     */
    private static final long serialVersionUID = 8950974759973459561L;

    public AntRemoteException(RemoteException e)
    {
        this("ANT Interface error, ANT Radio Service remote error: "+e.toString(), e);
    }
    
    public AntRemoteException(String string, RemoteException e)
    {
        super(string);
        
        this.initCause(e);
    }
}
