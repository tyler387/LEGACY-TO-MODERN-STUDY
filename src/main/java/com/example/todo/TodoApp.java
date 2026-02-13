package com.example.todo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TodoApp {
    private static final Path STORAGE = Path.of("todos.db");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final List<Todo> todos = new ArrayList<>();

    public static void main(String[] args) {
        TodoApp app = new TodoApp();
        app.load();
        app.run();
    }

    private void run() {
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                printMenu();
                System.out.print("선택 > ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> addTodo(scanner);
                    case "2" -> listTodos("all");
                    case "3" -> listTodos("active");
                    case "4" -> listTodos("completed");
                    case "5" -> toggleTodo(scanner);
                    case "6" -> deleteTodo(scanner);
                    case "7" -> clearCompleted();
                    case "0" -> {
                        save();
                        System.out.println("저장 후 종료합니다.");
                        return;
                    }
                    default -> System.out.println("잘못된 입력입니다.");
                }
            }
        }
    }

    private void printMenu() {
        long activeCount = todos.stream().filter(todo -> !todo.completed()).count();

        System.out.println("\n==== JAVA TODO LIST ====");
        System.out.printf("전체: %d | 남은 할 일: %d%n", todos.size(), activeCount);
        System.out.println("1) 할 일 추가");
        System.out.println("2) 전체 보기");
        System.out.println("3) 진행중 보기");
        System.out.println("4) 완료 보기");
        System.out.println("5) 완료/해제 토글");
        System.out.println("6) 삭제");
        System.out.println("7) 완료 항목 일괄 삭제");
        System.out.println("0) 저장 후 종료");
    }

    private void addTodo(Scanner scanner) {
        System.out.print("할 일 내용 > ");
        String text = scanner.nextLine().trim();
        if (text.isEmpty()) {
            System.out.println("빈 내용은 추가할 수 없습니다.");
            return;
        }

        int nextId = todos.stream().mapToInt(Todo::id).max().orElse(0) + 1;
        todos.add(new Todo(nextId, text, false, LocalDateTime.now()));
        save();
        System.out.println("추가되었습니다.");
    }

    private void listTodos(String filter) {
        List<Todo> filtered = todos.stream()
                .filter(todo -> switch (filter) {
                    case "active" -> !todo.completed();
                    case "completed" -> todo.completed();
                    default -> true;
                })
                .toList();

        if (filtered.isEmpty()) {
            System.out.println("표시할 항목이 없습니다.");
            return;
        }

        System.out.println("\n--- TODO 목록 ---");
        for (Todo todo : filtered) {
            String status = todo.completed() ? "[완료]" : "[진행]";
            System.out.printf("%d. %s %s (%s)%n",
                    todo.id(), status, todo.text(), todo.createdAt().format(DATE_FORMAT));
        }
    }

    private void toggleTodo(Scanner scanner) {
        if (todos.isEmpty()) {
            System.out.println("토글할 항목이 없습니다.");
            return;
        }

        System.out.print("토글할 ID > ");
        int id = parseInt(scanner.nextLine());
        if (id < 0) {
            System.out.println("숫자를 입력해 주세요.");
            return;
        }

        for (int i = 0; i < todos.size(); i++) {
            Todo todo = todos.get(i);
            if (todo.id() == id) {
                todos.set(i, todo.withCompleted(!todo.completed()));
                save();
                System.out.println("상태가 변경되었습니다.");
                return;
            }
        }

        System.out.println("해당 ID를 찾을 수 없습니다.");
    }

    private void deleteTodo(Scanner scanner) {
        if (todos.isEmpty()) {
            System.out.println("삭제할 항목이 없습니다.");
            return;
        }

        System.out.print("삭제할 ID > ");
        int id = parseInt(scanner.nextLine());
        if (id < 0) {
            System.out.println("숫자를 입력해 주세요.");
            return;
        }

        boolean removed = todos.removeIf(todo -> todo.id() == id);
        if (!removed) {
            System.out.println("해당 ID를 찾을 수 없습니다.");
            return;
        }

        save();
        System.out.println("삭제되었습니다.");
    }

    private void clearCompleted() {
        long before = todos.size();
        todos.removeIf(Todo::completed);
        long removed = before - todos.size();
        save();
        System.out.printf("완료 항목 %d개 삭제됨%n", removed);
    }

    private void load() {
        if (!Files.exists(STORAGE)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(STORAGE, StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] parts = line.split("\\|", 4);
                if (parts.length != 4) {
                    continue;
                }

                int id = Integer.parseInt(parts[0]);
                boolean completed = Boolean.parseBoolean(parts[1]);
                LocalDateTime createdAt = LocalDateTime.parse(parts[2]);
                String text = parts[3].replace("\\n", "\n");
                todos.add(new Todo(id, text, completed, createdAt));
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("저장 파일을 읽는 중 오류가 발생했습니다. 새 목록으로 시작합니다.");
            todos.clear();
        }
    }

    private void save() {
        List<String> lines = todos.stream()
                .map(todo -> String.format("%d|%s|%s|%s",
                        todo.id(),
                        todo.completed(),
                        todo.createdAt(),
                        todo.text().replace("\n", "\\n")))
                .toList();

        try {
            Files.write(STORAGE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("저장 실패: " + e.getMessage());
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

record Todo(int id, String text, boolean completed, LocalDateTime createdAt) {
    Todo withCompleted(boolean done) {
        return new Todo(id, text, done, createdAt);
    }
}
