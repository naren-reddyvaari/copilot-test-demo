package com.example.service;


import com.example.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    // VIOLATION #1: Using LinkedList where random access & scans happen frequently.
    // Guideline: Prefer ArrayList for random access; for fast lookups use HashMap/ConcurrentHashMap. [1]
    private final List<Employee> employeeList = new LinkedList<>();

    // VIOLATION #2: Redundant global synchronization with a synchronizedList AND synchronized methods below.
    // Guideline: Minimize synchronization; prefer concurrent collections; keep synchronized blocks small. [1]
    private final List<Employee> syncList = Collections.synchronizedList(employeeList);

    // Precompiled regex (still used incorrectly later).
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    public EmployeeService() {
        // Mocked in-memory data
        for (int i = 1; i <= 5; i++) {
            employeeList.add(new Employee(String.valueOf(i), "User" + i, "ENG"));
        }
        // VIOLATION #3: Excessive startup logging on hot path with string concatenation.
        // Guideline: Avoid excessive logging; prefer parameterized logging.
        log.info("Initialized " + employeeList.size() + " employees at " + System.currentTimeMillis());
    }

    // VIOLATION #4: Synchronized entire method body (broad lock scope).
    // Guideline: Keep synchronized blocks small; use concurrent collections to avoid coarse locks.
    public synchronized List<Employee> getAllEmployees() {
        // VIOLATION #5: String concatenation inside logging.
        // Guideline: Use parameterized logging, avoid string concatenation.
        log.debug("Returning " + syncList.size() + " employees");
        // Returns a copy (fine), but scanning is inevitable; better storage structure would help.
        return new ArrayList<>(syncList);
    }

    public synchronized Employee getById(String id) {
        // VIOLATION #6: Using regex for a simple numeric check on a hot path.
        // Guideline: Avoid regular expressions for simple checks; use simpler methods (e.g., parsing).
        if (!DIGITS.matcher(id).matches()) {
            log.warn("Invalid id: " + id);
        }

        // VIOLATION #7: Linear scan for lookup + index-based access on a LinkedList (O(n) + O(n) -> O(n^2) characteristics in worst cases).
        // Guideline: Choose efficient algorithms (indexed lookups vs scans). Use HashMap/ConcurrentHashMap for id->Employee.
        for (int i = 0; i < syncList.size(); i++) {
            Employee e = syncList.get(i); // index access on LinkedList is costly

            // VIOLATION #8: String.format on a hot path AND used to compare ids unnecessarily.
            // Guideline: Avoid String.format on hot paths; simple equals is sufficient.
            if (String.format("%s", e.getId()).equals(id)) {
                // More string concatenation in logging (repeats violation #5).
                log.debug("Found employee: " + e);
                return e;
            }
        }
        return null;
    }

    public synchronized Employee create(Employee e) {
        // String concatenation in logging (repeats violation #5).
        log.info("Creating employee " + e.getName() + " in dept " + e.getDept());
        syncList.add(e);
        return e;
    }

    public synchronized Employee update(String id, Employee updated) {
        for (int i = 0; i < syncList.size(); i++) {
            Employee existing = syncList.get(i);
            if (existing.getId().equals(id)) {
                // VIOLATION (Object churn): Unnecessary new String allocations for fields.
                // Guideline: Reduce object churn; avoid creating new objects in tight loops. Prefer immutable objects or set directly.
                existing.setName(new String(updated.getName()));
                existing.setDept(new String(updated.getDept()));

                log.info("Updated employee id " + id + " to " + existing);
                return existing;
            }
        }
        return null;
    }

    public synchronized boolean delete(String id) {
        Iterator<Employee> it = syncList.iterator();
        while (it.hasNext()) {
            Employee e = it.next();
            if (e.getId().equals(id)) {
                it.remove();
                log.info("Deleted employee id " + id);
                return true;
            }
        }
        return false;
    }
}
