package ru.programpark.mirah.index.elements;
import java.util.List;
import java.util.Objects;

public interface MethodElement {

    /**
     * Gets the list of the {@link MethodParameter}s.
     *
     * @return list of the {@link MethodParameter}s.
     */
    List<MethodParameter> getParameters();

    /**
     * Gets only the parameter types of the method.
     *
     * @return parameter types of the method
     */
    List<String> getParameterTypes();

    /**
     * Gets the return type of the method.
     *
     * @return return type of the method
     */
    String getReturnType();


    /**
     * Information about method parameter such as parameter type and name.
     */
    public static final class MethodParameter {

        private final String fqnType;
        private final String type;
        private final String name;

        public MethodParameter(String fqnType, String type) {
            this(fqnType, type, null);
        }

        public MethodParameter(String fqnType, String type, String name) {
            this.fqnType = fqnType;
            this.type = type;
            this.name = name;
        }

        public String getFqnType() {
            return fqnType;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return type + " " + name; // NOI18N
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 13 * hash + Objects.hashCode(this.fqnType);
            hash = 13 * hash + Objects.hashCode(this.type);
            hash = 13 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MethodParameter other = (MethodParameter) obj;
            if (!Objects.equals(this.fqnType, other.fqnType)) {
                return false;
            }
            if (!Objects.equals(this.type, other.type)) {
                return false;
            }
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }
    }
}
