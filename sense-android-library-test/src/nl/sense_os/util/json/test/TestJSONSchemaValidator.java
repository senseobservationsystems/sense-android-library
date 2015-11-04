package nl.sense_os.util.json.test;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.sense_os.util.json.JSONSchemaValidator;
import nl.sense_os.util.json.SchemaException;
import nl.sense_os.util.json.ValidationException;

public class TestJSONSchemaValidator extends TestCase {

    public void testFailOnUnknownProperties () throws JSONException, SchemaException {
        try {
            new JSONSchemaValidator(new JSONObject("{\"foobar\":\"string\"}"));

            fail("should throw an exception");
        }
        catch (SchemaException e) {
            assertEquals("Unknown schema property 'foobar'", e.getMessage());
        }
    }

    public void testDescriptionProperty () throws JSONException, SchemaException {
        // should allow (ignore) "description"
        new JSONSchemaValidator(new JSONObject("{\"description\":\"bla bla bla\"}"));
    }

    public void testValidateString () throws JSONException, ValidationException, SchemaException {
        JSONSchemaValidator validator = new JSONSchemaValidator(new JSONObject("{\"type\":\"string\"}"));

        validator.validate("hello");

        try {
            validator.validate(2);
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid type. string expected", e.getMessage());
        }

        try {
            validator.validate(null);
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid type. string expected", e.getMessage());
        }
    }

    public void testValidateEnum () throws JSONException, ValidationException, SchemaException {
        JSONSchemaValidator validator = new JSONSchemaValidator(new JSONObject("{\"enum\":[42, null,\"hello\"]}"));

        validator.validate(42);
        validator.validate("hello");
        validator.validate(null);

        try {
            validator.validate(44);
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid value 44. Expected any of [42,null,\"hello\"]", e.getMessage());
        }

        try {
            validator.validate("foo");
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid value foo. Expected any of [42,null,\"hello\"]", e.getMessage());
        }
    }

    public void testValidateRequired () throws JSONException, ValidationException, SchemaException {
        JSONSchemaValidator validator = new JSONSchemaValidator(new JSONObject("{\"required\":[\"name\", \"age\"]}"));

        validator.validate(new JSONObject("{\"name\":\"Jo\",\"age\":18}"));
        validator.validate(new JSONObject("{\"name\":\"Jo\",\"age\":18,\"foo\":\"bar\"}")); // should ignore unknown fields

        try {
            validator.validate(new JSONObject("{\"name\":\"Jo\"}"));
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Required property 'age' missing", e.getMessage());
        }

        try {
            validator.validate(new JSONObject("{\"age\":18}"));
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Required property 'name' missing", e.getMessage());
        }
    }

    public void testValidateObject () throws JSONException, ValidationException, SchemaException {
        JSONSchemaValidator validator = new JSONSchemaValidator(new JSONObject("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}"));

        validator.validate(new JSONObject());
        validator.validate(new JSONObject("{\"name\":\"Jo\",\"age\":18}"));

        // should ignore unknown field
        validator.validate(new JSONObject("{\"foo\":\"bar\"}"));

        try {
            validator.validate(new JSONObject("{\"name\":\"Jo\",\"age\":22.3}"));
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid type. integer expected for property 'age'", e.getMessage());
        }

        try {
            validator.validate(new JSONObject("{\"name\":123,\"age\":123}"));
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid type. string expected for property 'name'", e.getMessage());
        }
    }

    public void testValidateNestedObject () throws JSONException, ValidationException, SchemaException {
        JSONSchemaValidator validator = new JSONSchemaValidator(new JSONObject("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"address\":{\"type\":\"object\",\"properties\":{\"street\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"}},\"required\":[\"city\",\"street\"]}}}"));

        validator.validate(new JSONObject("{\"name\":\"Jo\",\"address\":{\"street\":\"55th Street\",\"city\":\"New York\"}}"));

        try {
            validator.validate(new JSONObject("{\"name\":\"Jo\",\"address\":{\"street\":\"55th Street\",\"city\":123}}"));
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid type. string expected for property 'city'", e.getMessage());
        }

        try {
            validator.validate(new JSONObject("{\"name\":\"Jo\",\"address\":{\"city\":\"New York\"}}"));
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Required property 'street' missing", e.getMessage());
        }
    }


    public void testValidateArray () throws JSONException, ValidationException, SchemaException {
        JSONSchemaValidator validator = new JSONSchemaValidator(new JSONObject("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}"));

        validator.validate(new JSONArray("[]"));
        validator.validate(new JSONArray("[\"hello\", \"world\"]"));

        try {
            validator.validate(new JSONArray("[\"hello\", 123]"));
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid type. string expected", e.getMessage());
        }

        try {
            validator.validate(new JSONArray("[null, \"foo\"]"));
            fail("should throw an exception");
        }
        catch (Exception e) {
            assertEquals("Invalid type. string expected", e.getMessage());
        }
    }

}
