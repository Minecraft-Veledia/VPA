package de.veledia.vpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a parameter of a command method.
 * This helps the ArgumentParser to map arguments to method parameters
 * and potentially provide better tab completion or type hints.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SubCommandParam {
    String value(); // The name of the parameter (e.g., "player", "amount")
}
