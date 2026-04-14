package com.trucdnd.gpu_hub_backend.common.exception;

public class KubernetesOperationException extends RuntimeException {
    public KubernetesOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
