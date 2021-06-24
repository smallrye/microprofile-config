package io.smallrye.config.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.Pos;

import jakarta.enterprise.inject.spi.InjectionPoint;

@MessageBundle(projectCode = "SRCFG", length = 5)
public interface InjectionMessages {
    InjectionMessages msg = Messages.getBundle(InjectionMessages.class);

    @Message(id = 2000, value = "Failed to Inject @ConfigProperty for key %s into %s since the config property could not be found in any config source")
    ConfigException noConfigValue(@Param @Pos(1) String configPropertyName, @Pos(2) String location);

    @Message(id = 2001, value = "Failed to Inject @ConfigProperty for key %s into %s %s")
    ConfigException retrieveConfigFailure(@Param @Pos(1) String configPropertyName, @Pos(2) String location,
            @Pos(3) String causeMessage, @Cause Exception e);

    @Message(id = 2002, value = "Could not find default name for @ConfigProperty InjectionPoint %s")
    IllegalStateException noConfigPropertyDefaultName(InjectionPoint injectionPoint);

    @Message(id = 2003, value = "Unhandled ConfigProperty")
    IllegalStateException unhandledConfigProperty();

    @Message(id = 2004, value = "Required property %s not found")
    NoSuchElementException propertyNotFound(String name);

    @Message(id = 2005, value = "Type has no raw type class: %s")
    IllegalArgumentException noRawType(Type type);

    @Message(id = 2006, value = "The property %s cannot be converted to %s")
    IllegalArgumentException illegalConversion(String name, Type type);

    @Message(id = 2007, value = "No Converter registered for %s")
    IllegalArgumentException noRegisteredConverter(Class<?> type);

    /**
     * 
     * Formats InjectPoint information for Exception messages.<br>
     * <br>
     * 
     * 3 possible InjectionPoint types are considered:<br>
     * <br>
     * 
     * <b>Fields</b><br>
     * Given: java.lang.String
     * io.smallrye.config.inject.ValidateInjectionTest$SkipPropertiesTest$SkipPropertiesBean.missingProp<br>
     * Returns: io.smallrye.config.inject.ValidateInjectionTest$SkipPropertiesTest$SkipPropertiesBean.missingProp<br>
     * <br>
     * 
     * <b>Method parameters</b><br>
     * Given: private void
     * io.smallrye.config.inject.ValidateInjectionTest$MethodUnnamedPropertyTest$MethodUnnamedPropertyBean.methodUnnamedProperty(java.lang.String)<br>
     * Returns:
     * io.smallrye.config.inject.ValidateInjectionTest$MethodUnnamedPropertyTest$MethodUnnamedPropertyBean.methodUnnamedProperty(String)<br>
     * <br>
     * 
     * <b>Constructor parameters</b><br>
     * Given: public
     * io.smallrye.config.inject.ValidateInjectionTest$ConstructorUnnamedPropertyTest$ConstructorUnnamedPropertyBean(java.lang.String)<br>
     * Returns:
     * io.smallrye.config.inject.ValidateInjectionTest$ConstructorUnnamedPropertyTest$ConstructorUnnamedPropertyBean(String)
     * 
     */
    public static String formatInjectionPoint(InjectionPoint injectionPoint) {

        Member member = injectionPoint.getMember();

        StringBuilder sb = new StringBuilder();
        sb.append(member.getDeclaringClass().getName());

        if (member instanceof Field) {
            sb.append("." + member.getName());
        } else if (member instanceof Method) {
            sb.append("." + member.getName());
            appendParameterTypes(sb, (Method) member);
        } else if (member instanceof Constructor) {
            appendParameterTypes(sb, (Constructor<?>) member);
        }
        return sb.toString();
    }

    static void appendParameterTypes(StringBuilder sb, Executable executable) {
        sb.append("(" +
                Arrays.stream(executable.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", "))
                + ")");
    }
}
