package no.hvl.dat250.rest.todos;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * Tests for the Todos-REST-API.
 */
public class TodoAPITest {

    private static final String SERVER_PORT = "6000";
    private static final String BASE_URL = "http://localhost:" + SERVER_PORT + "/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final Type TODO_LIST_TYPE = new TypeToken<List<Todo>>() {
    }.getType();

    /**
     * Start the REST-API before we run the tests.
     */
    @BeforeClass
    public static void startRESTServer() {
        TodoAPI.main(new String[]{SERVER_PORT});
    }

    @Test
    public void testCreate() {
        Todo todo = new Todo("test summary", "test description");

        // Execute post request
        final String postResult = doPostRequest(todo);

        // Parse the created todo.
        final Todo createdTodo = gson.fromJson(postResult, Todo.class);

        // Make sure our created todo is correct.
        assertThat(createdTodo.getDescription(), is(todo.getDescription()));
        assertThat(createdTodo.getSummary(), is(todo.getSummary()));
        assertNotNull(createdTodo.getId());
    }

    private String doPostRequest(Todo todo) {
        // Prepare request and add the body
        RequestBody body = RequestBody.create(gson.toJson(todo), JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "todos")
                .post(body)
                .build();

        return doRequest(request);
    }

    private String doRequest(Request request) {
        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testReadOne() {
        // Save one todo.
        final Todo todo = new Todo("summary1", "description1");
        final Todo createdTodo = gson.fromJson(doPostRequest(todo), Todo.class);

        // Execute get request
        final String getResult = doGetRequest(createdTodo.getId());

        // Parse returned todo.
        final Todo returnedTodo = gson.fromJson(getResult, Todo.class);

        // The returned todo must be the one we created earlier.
        assertThat(returnedTodo, is(createdTodo));
    }

    @Test
    public void testReadAll() {
        // Save 2 todos.
        final Todo todo1 = new Todo("summary1", "description1");
        final Todo todo2 = new Todo("summary2", "description2");
        final Todo createdTodo1 = gson.fromJson(doPostRequest(todo1), Todo.class);
        final Todo createdTodo2 = gson.fromJson(doPostRequest(todo2), Todo.class);

        // Execute get request
        final String getResult = doGetRequest();

        // Parse returned list of todos.
        final List<Todo> todos = parseTodos(getResult);

        // We have at least the two created todos.
        assertTrue(todos.size() >= 2);

        // The todos are contained in the list.
        assertTrue(todos.contains(createdTodo1));
        assertTrue(todos.contains(createdTodo2));
    }

    private List<Todo> parseTodos(String result) {
        return gson.fromJson(result, TODO_LIST_TYPE);
    }

    /**
     * Gets the todo with the given id.
     */
    private String doGetRequest(Long todoId) {
        return this.doGetRequest(BASE_URL + "todos/" + todoId);
    }

    /**
     * Gets all todos.
     */
    private String doGetRequest() {
        return this.doGetRequest(BASE_URL + "todos");
    }

    private String doGetRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return doRequest(request);
    }

    @Test
    public void testUpdate() {
        // Save an element, which we can update later.
        final Todo todo = new Todo("summary", "description");
        final Todo createdTodo = gson.fromJson(doPostRequest(todo), Todo.class);

        // Execute put request
        final Todo updatedTodo = new Todo(createdTodo.getId(), "updated summary", "updated description");
        doPutRequest(updatedTodo);

        // Read the todo again and check if it is correct.
        final Todo returnedTodo = gson.fromJson(doGetRequest(updatedTodo.getId()), Todo.class);
        assertThat(returnedTodo, is(updatedTodo));
    }

    private void doPutRequest(Todo todo) {
        // Prepare request and add the body
        RequestBody body = RequestBody.create(gson.toJson(todo), JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "todos/" + todo.getId())
                .put(body)
                .build();

        doRequest(request);
    }

    @Test
    public void testDelete() {
        // Save an element, which we can delete later.
        final Todo todo = new Todo("summary", "description");
        final Todo createdTodo = gson.fromJson(doPostRequest(todo), Todo.class);

        final List<Todo> todosBeforeDelete = parseTodos(doGetRequest());

        // Execute delete request
        doDeleteRequest(createdTodo.getId());

        final List<Todo> todosAfterDelete = parseTodos(doGetRequest());

        assertTrue(todosBeforeDelete.contains(createdTodo));
        // Todo not contained anymore.
        assertFalse(todosAfterDelete.contains(createdTodo));
        // The size was reduced by one due to the deletion.
        assertThat(todosBeforeDelete.size() - 1, is(todosAfterDelete.size()));
    }

    private String doDeleteRequest(Long todoId) {
        Request request = new Request.Builder()
                .url(BASE_URL + "todos/" + todoId)
                .delete()
                .build();

        return doRequest(request);
    }

    @Test
    public void testNonExistingTodo() {
        final long todoId = 9999L;
        // Execute get request
        String result = doGetRequest(todoId);

        // Expect a appropriate result message.
        assertThat(result, is(String.format("Todo with the id \"%s\" not found!", todoId)));

        // Execute delete request
        result = doDeleteRequest(todoId);

        // Expect a appropriate result message.
        assertThat(result, is(String.format("Todo with the id \"%s\" not found!", todoId)));
    }

    @Test
    public void testIdNotANumber() {
        String id = "notANumber";
        // Execute get request
        Request getRequest = new Request.Builder()
                .url(BASE_URL + "todos/" + id)
                .get()
                .build();

        final String getResult = doRequest(getRequest);

        // Expect a appropriate result message.
        assertThat(getResult, is(String.format("The id \"%s\" is not a number!", id)));

        // Execute put request
        Request putRequest = new Request.Builder()
                .url(BASE_URL + "todos/" + id)
                .put(RequestBody.create("", JSON))
                .build();

        final String putResult = doRequest(putRequest);

        // Expect a appropriate result message.
        assertThat(putResult, is(String.format("The id \"%s\" is not a number!", id)));

        // Execute delete request
        Request deleteRequest = new Request.Builder()
                .url(BASE_URL + "todos/" + id)
                .delete()
                .build();

        final String deleteResult = doRequest(deleteRequest);

        // Expect a appropriate result message.
        assertThat(deleteResult, is(String.format("The id \"%s\" is not a number!", id)));
    }
}
