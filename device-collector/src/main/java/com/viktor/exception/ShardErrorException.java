package com.viktor.exception;

import lombok.Getter;

@Getter
public class ShardErrorException extends RuntimeException {

    private final String shardName;

    public ShardErrorException(String message, String shardName) {
        super(message);
        this.shardName = shardName;
    }
}
