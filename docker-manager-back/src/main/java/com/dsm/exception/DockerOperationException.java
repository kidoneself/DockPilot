package com.dsm.exception;

public class DockerOperationException extends RuntimeException {
    private final DockerErrorCode errorCode;
    private final String detail;


    public DockerOperationException(DockerErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public DockerErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorCode.getMessage();
    }

    public String getDetail() {
        return detail;
    }
}