package org.wso2.carbon.identity.configuration.mgt.server.cors.exception;

public class CORSServiceException extends Exception {

    public CORSServiceException(String message) {

        super(message);
    }

    public CORSServiceException(String message, Throwable cause) {

        super(message, cause);
    }

}
