package nl.sense_os.util.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A minimal JSON Schema Validator for Java.
 *
 * Supported types:
 *     "array", "boolean", "integer", "number", "null", "object", "string"
 *
 * Supported JSON schema features:
 *     "properties", "items", "required", "type", "enum", "description"
 *
 * Example usage:
 *
 *     JSONObject schema = new JSONObject("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}");
 *     JSONSchemaValidator validator = new JSONSchemaValidator(schema);
 *
 *     validator.validate(new JSONObject("{\"name\":\"Jo\",\"age\":18}"));      // ok
 *     validator.validate(new JSONObject("{\"name\":\"Jo\",\"age\":\"18\"}"));  // throws ValidationException "Invalid type. integer expected for property 'age'"
 *
 * Relevant docs:
 *     http://json-schema.org/documentation.html
 *     http://json-schema.org/latest/json-schema-core.html
 *     http://json-schema.org/latest/json-schema-validation.html
 *
 */
public class JSONSchemaValidator {
    protected static final Set<String> KNOWN_FIELDS =
            new HashSet<>(Arrays.asList(new String[]{"$schema", "properties", "items", "required", "type", "enum", "description"}));
    protected static final Set<String> VALID_TYPES =
            new HashSet<>(Arrays.asList(new String[]{"array", "boolean", "integer", "number", "null", "object", "string"}));

    private List<Object> enumz = null;
    private List<String> required = null;
    private String name = null;
    private Integer index = null; // used when iterating over the items of an array
    private String type = null;
    private JSONSchemaValidator items; // for arrays
    private List<JSONSchemaValidator> properties; // for objects


    public JSONSchemaValidator (JSONObject schema) throws JSONException, SchemaException {
        validateSchema(schema);

        if (schema.has("type")) {
            type = schema.getString("type");

            if (!VALID_TYPES.contains(type)) {
                throw new SchemaException("Invalid type '" + type + "', choose from: " + VALID_TYPES.toString() + ".");
            }
        }

        if (schema.has("enum")) {
            enumz = new ArrayList<>();
            JSONArray array = schema.getJSONArray("enum");
            for (int i = 0; i < array.length(); i++) {
                enumz.add(array.get(i));
            }
        }

        if (schema.has("required")) {
            required = new ArrayList<>();
            JSONArray array = schema.getJSONArray("required");
            for (int i = 0; i < array.length(); i++) {
                required.add(array.getString(i));
            }
        }

        if (schema.has("properties")) {
            JSONObject object = schema.getJSONObject("properties");
            properties = new ArrayList<>();
            Iterator<String> names = object.keys();
            while (names.hasNext()) {
                String name = names.next();
                properties.add(new JSONSchemaValidator(name, object.getJSONObject(name)));
            }
        }

        if (schema.has("items")) {
            items = new JSONSchemaValidator(schema.getJSONObject("items"));
        }
    }

    protected JSONSchemaValidator (String name, JSONObject schema) throws JSONException, SchemaException {
        this(schema);
        this.name = name;
    }

    public void validate (Object object) throws ValidationException, JSONException {
        if (type != null) {
            validateType(object, type);
        }

        if (enumz != null) {
            validateEnum(object);
        }

        if (required != null) {
            validateType(object, "object");
            validateRequired( (JSONObject) object );
        }

        if (properties != null) {
            validateType(object, "object");
            validateProperties( (JSONObject)object );
        }

        if (items != null) {
            validateType(object, "array");
            validateItems( (JSONArray)object );
        }
    }

    protected void validateType (Object object, String type) throws ValidationException {
        if ("array".equals(type) && !(object instanceof JSONArray)) {
            throw createInvalidTypeError(type);
        }

        if ("boolean".equals(type) && !(object instanceof Boolean)) {
            throw createInvalidTypeError(type);
        }

        if ("integer".equals(type) && !isInteger(object)) {
            throw createInvalidTypeError(type);
        }

        if ("number".equals(type) && !(object instanceof Number)) {
            throw createInvalidTypeError(type);
        }

        if ("null".equals(type) && object != null) {
            throw createInvalidTypeError(type);
        }

        if ("object".equals(type) && !(object instanceof JSONObject)) {
            throw createInvalidTypeError(type);
        }

        if ("string".equals(type) && !(object instanceof String)) {
            throw createInvalidTypeError(type);
        }
    }

    protected ValidationException createInvalidTypeError(String type) {
        return new ValidationException("Invalid type" + describeLocation() + ". " + type + " expected.");
    }

    protected void validateProperties (JSONObject object) throws JSONException, ValidationException {
        for (JSONSchemaValidator child : properties) {
            if (object.has(child.name)) {
                child.validate(object.get(child.name));
            }
        }
    }

    protected void validateItems (JSONArray array) throws JSONException, ValidationException {
        for (int i = 0; i < array.length(); i++) {
            items.index = i;
            items.validate(array.get(i));
        }
        items.index = null; // reset to null again
    }

    protected void validateEnum (Object object) throws JSONException, ValidationException {
        for (Object entry : enumz) {
            if (entry == null) {
                if (object == null) {
                    return;
                }
            }
            else {
                if (entry.equals(object)) {
                    return;
                }
            }
        }

        throw new ValidationException("Invalid value " + object + describeLocation() + ". Expected any of " + new JSONArray(enumz).toString() + ".");
    }

    protected void validateRequired (JSONObject object) throws JSONException, ValidationException {
        for (String name : required) {
            if (!object.has(name)) {
                throw new ValidationException("Required property '" + name + "' missing.");
            }
        }
    }

    /**
     * Check if the schema does not contain unknown properties.
     * If the schema contains unknown properties, and exception is thrown
     * @param schema  A JSONObject containing a JSON schema
     * @throws SchemaException
     */
    protected void validateSchema (JSONObject schema) throws SchemaException {
        Iterator<String> keys = schema.keys();
        while (keys.hasNext()) {
            String key = keys.next();

            if (!KNOWN_FIELDS.contains(key)) {
                throw new SchemaException("Unknown schema property '" + key + "'.");
            }
        }
    }

    /**
     * Describe the location of the current property.
     * @return Returns a description like:
     *     ""
     *     " for property 'age'"
     *     " at index 12"
     */
    protected String describeLocation () {
        String description = "";

        if (name != null) {
            description += " for property '" + name + "'";
        }

        if (index != null) {
            description += " at index " + index;
        }

        return description;
    }

    /**
     * Check of given object is an integer number
     * @param object  An object, can be anything including null.
     * @return        Returns true when object is an integer number
     */
    protected boolean isInteger(Object object) {
        if (!(object instanceof Number)) {
            return false;
        }

        Number number = (Number) object;
        return number.equals(number.longValue());
    }
}
