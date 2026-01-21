package com.example.IncidentPulse.Exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_EXISTED(404,"User already existed!!!"),
    USER_NON_EXISTED(404,"User is not existed!!!"),
    USER_NOT_FOUND(404,"User is not Found!!!"),
    INVALID_CREDENTIALS(401,"User's credentials is not valid"),
    NO_ON_CALL_ENGINEER(404,"No on-call engineer is currently scheduled");
    private final int code;
    private final String message;

     ErrorCode(int code,String message){
        this.code = code;
        this.message = message;

    }


    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
