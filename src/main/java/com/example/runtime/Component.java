package com.example.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación utilizada para marcar una clase como un componente.
 * Los componentes marcados con esta anotación pueden ser escaneados y utilizados por el framework.
 */
@Retention(RetentionPolicy.RUNTIME) // Anotación que se mantiene hasta tiempo de ejecución.
@Target(ElementType.TYPE) // Anotación aplicable a tipos, como clases, interfaces, enumeraciones o anotaciones.
public @interface Component {
}

