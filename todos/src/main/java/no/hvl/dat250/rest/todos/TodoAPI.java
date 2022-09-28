package no.hvl.dat250.rest.todos;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Rest-Endpoint.
 */
public class TodoAPI {

    public static void main(String[] args) {

        final Map <Long, Todo> tasks = new HashMap<>();

        if (args.length > 0) {
            port(Integer.parseInt(args[0]));
        } else {
            port(8080);
        }

        after((req, res) -> res.type("application/json"));

        // TODO: Implement API, such that the testcases succeed.

        post("/todos", (req, res) -> {
            Gson gson = new Gson();
            Todo t = gson.fromJson(req.body(), Todo.class);
            if (t.getId() == null){
                t = new Todo(new Long(tasks.size()+1) ,t.getSummary() ,t.getDescription());
            }
            tasks.put(t.getId(), t);
            return gson.toJson(t);
        });

        get("/todos", (req, res) -> {
            List result = new ArrayList();
            Gson gson = new Gson();
            for (Todo t : tasks.values()){
                result.add(gson.toJson(t));
            }
            return result;
        });

        get("/todos/:id", (req, res) -> {

            Gson gson = new Gson();
            Long key = null;
            try{
                key = new Long(req.params(":id"));
            }catch (NumberFormatException e){
                return "The id \"" + req.params(":id") + "\" is not a number!";
            }

            if (!tasks.containsKey(key)) return "Todo with the id  \"" + key + "\" not found!";
            Todo t = tasks.get(key);
            return gson.toJson(t);

        });

        put("/todos/:id", (req, res) -> {
            Gson gson = new Gson();
            Long key = null;
            try{
                key = new Long(req.params(":id"));
            }catch (NumberFormatException e){
                return "The id \"" + req.params(":id") + "\" is not a number!";
            }
            if (!tasks.containsKey(key)) return "Todo with the id  \"" + key + "\" not found!";
            Todo t = tasks.get(key);
            t = gson.fromJson(req.body(), Todo.class);
            tasks.put(key, t);
            return gson.toJson(t);
        });

        delete("/todos/:id", (req, res) -> {
            Gson gson = new Gson();
            Long key = null;
            try{
                key = new Long(req.params(":id"));
            }catch (NumberFormatException e){
                return "The id \"" + req.params(":id") + "\" is not a number!";
            }
            Todo t = null;
            if (!tasks.containsKey(key)) return "Todo with the id  \"" + key + "\" not found!";
            t = tasks.remove(key);
            return gson.toJson(t);
        });

    }

}
