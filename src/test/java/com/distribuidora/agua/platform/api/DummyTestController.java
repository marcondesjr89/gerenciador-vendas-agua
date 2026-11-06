package com.distribuidora.agua.platform.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class DummyTestController {

    public record DummyDto(
            @NotBlank(message = "O nome nao pode estar em branco")
            String nome,

            @Positive(message = "O valor deve ser positivo")
            Double valor
    ) {}

    @PostMapping("/validation")
    public void testValidation(@Valid @RequestBody DummyDto dto) {
    }

    @GetMapping("/business-error")
    public void testBusinessError() {
        throw new IllegalArgumentException("Parametro invalido informado");
    }

    @GetMapping("/state-error")
    public void testStateError() {
        throw new IllegalStateException("Operacao invalida para o estado atual");
    }
}
