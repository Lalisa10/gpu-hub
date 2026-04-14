package com.trucdnd.gpu_hub_backend.common.utils;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RandomK8sResourceNameGenerator {
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom(); 

    public String generateString(int length) {
        return IntStream.range(0, length)
            .map(i -> CHARS.charAt(RANDOM.nextInt(CHARS.length())))
            .mapToObj(c -> String.valueOf((char) c))
            .collect(Collectors.joining());
    }
}
