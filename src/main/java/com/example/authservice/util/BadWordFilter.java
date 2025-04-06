package com.example.authservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class BadWordFilter {

    private static final Set<String> BAD_WORDS = new HashSet<>();

    @Value("${badword.file-path}")
    private String badWordFilePath;

    @PostConstruct
    public void loadBadWords() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(new ClassPathResource(badWordFilePath).getFile());
            node.get("badWords").forEach(word -> BAD_WORDS.add(word.asText()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean containsBadWord(String nickname) {
        for (String badWord : BAD_WORDS) {
            if (nickname.contains(badWord)) {
                return true;
            }
        }
        return false;
    }
}
