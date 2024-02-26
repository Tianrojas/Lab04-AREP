package com.example.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación utilizada para mapear un método HTTP GET a una URI específica.
 */
@Retention(RetentionPolicy.RUNTIME) // Anotación que se mantiene hasta tiempo de ejecución.
@Target(ElementType.METHOD) // Anotación aplicable solo a métodos.
public @interface GetMapping {
    /**
     * Define la URI a la cual se mapeará el método.
     *
     * @return La URI a la cual se mapeará el método.
     */
    String value();
}
