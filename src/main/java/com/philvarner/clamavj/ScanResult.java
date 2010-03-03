package com.philvarner.clamavj;

public class ScanResult {

    private String result = "";
    private Status status = Status.FAILED;
    private String signature = "";

    public enum Status { PASSED, FAILED, ERROR }

    public static final String STREAM_PREFIX = "stream: ";
    public static final String RESPONSE_OK = "stream: OK";
    public static final String FOUND_SUFFIX = "FOUND";

    public static final String RESPONSE_SIZE_EXCEEDED = "INSTREAM size limit exceeded. ERROR";
    public static final String RESPONSE_ERROR_WRITING_FILE = "Error writing to temporary file. ERROR";

    public ScanResult(String result){
        setResult(result);
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;

        if (RESPONSE_OK.equals(result)) {
            setStatus(Status.PASSED);
        } else if (result.endsWith(FOUND_SUFFIX)){
            setSignature(result.substring(STREAM_PREFIX.length(), result.lastIndexOf(FOUND_SUFFIX) - 1 ));
        } else if (RESPONSE_SIZE_EXCEEDED.equals(result)) {
            setStatus(Status.ERROR);
        } else if (RESPONSE_ERROR_WRITING_FILE.equals(result)) {
            setStatus(Status.ERROR);                           
        }

    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
