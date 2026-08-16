package com.gameplatform.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final Map<Integer, Task> tasks = new LinkedHashMap<>();
    private int nextId = 1;

    @GetMapping
    public Collection<Task> all() {
        return tasks.values();
    }

    @PostMapping
    public Task create(@RequestBody CreateTaskRequest request) {
        Task saved = new Task(nextId++, request.title(), false);
        tasks.put(saved.id(), saved);
        return saved;
    }

    record Task(Integer id, String title, boolean done) {}
    record CreateTaskRequest(String title) {}
}