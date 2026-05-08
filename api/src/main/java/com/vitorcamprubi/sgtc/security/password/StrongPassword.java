package com.vitorcamprubi.sgtc.security.password;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um campo de senha que deve respeitar a politica minima do SGTC:
 * - 8 caracteres ou mais
 * - ao menos uma letra
 * - ao menos um digito
 *
 * Em campos opcionais (ex: troca de senha onde valor em branco mantem a senha
 * atual), use {@link #allowBlank()} para aceitar valores vazios.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Senha fraca: use pelo menos 8 caracteres, com letras e numeros";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * Quando true, permite valor null/blank (uso tipico em update onde campo
     * vazio = manter senha atual). Quando false (default), exige senha forte.
     */
    boolean allowBlank() default false;
}
