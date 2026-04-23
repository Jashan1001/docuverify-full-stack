package com.docuverify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DocuVerifyApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocuVerifyApplication.class, args);
    }
}
