const $ = (selector, scope = document) => scope.querySelector(selector);
const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

const FILTER = Object.freeze({
  ALL: 'all',
  ACTIVE: 'active',
  COMPLETED: 'completed',
});

const PRIORITY_LABEL = Object.freeze({
  high: '높음',
  medium: '중간',
  low: '낮음',
});

class TodoApp {
  static STORAGE_KEY = 'es6-plus-todos';

  #todos = [];
  #filter = FILTER.ALL;
  #els;

  constructor(els) {
    this.#els = els;
  }

  static async create(els) {
    const app = new TodoApp(els);
    await app.#restore();
    app.#bindEvents();
    app.render();
    return app;
  }

  get #activeCount() {
    return this.#todos.reduce((count, { completed }) => count + Number(!completed), 0);
  }

  get #completionRate() {
    const total = this.#todos.length || 1;
    return Math.round(((total - this.#activeCount) / total) * 100);
  }

  async #restore() {
    await Promise.resolve();
    const raw = localStorage.getItem(TodoApp.STORAGE_KEY) ?? '[]';
    const parsed = JSON.parse(raw);

    this.#todos = parsed.map(({ id, text, completed = false, priority = 'medium', tags = [] }) => ({
      id,
      text,
      completed,
      priority,
      tags: [...new Set(tags)],
    }));
  }

  #save = () => {
    localStorage.setItem(TodoApp.STORAGE_KEY, JSON.stringify(this.#todos));
  };

  #bindEvents() {
    const { form, list, clearButton, filterButtons } = this.#els;

    form.addEventListener('submit', (event) => {
      event.preventDefault();

      const text = this.#els.input.value.trim();
      const priority = this.#els.prioritySelect.value;
      const tags = this.#parseTags(this.#els.tagInput.value);

      if (!text) return;

      this.#addTodo({ text, priority, tags });
      form.reset();
      this.#els.prioritySelect.value = 'medium';
      this.render();
    });

    list.addEventListener('click', ({ target }) => {
      const item = target.closest('.todo-item');
      const id = item?.dataset?.id;
      if (!id) return;

      if (target.matches('input[type="checkbox"]')) this.#toggle(id);
      if (target.matches('.delete')) this.#delete(id);

      this.render();
    });

    filterButtons.forEach((button) => {
      button.addEventListener('click', () => {
        this.#filter = button.dataset.filter ?? FILTER.ALL;
        filterButtons.forEach((el) => el.classList.toggle('active', el === button));
        this.render();
      });
    });

    clearButton.addEventListener('click', () => {
      this.#todos = this.#todos.filter(({ completed }) => !completed);
      this.#save();
      this.render();
    });
  }

  #parseTags = (value = '') =>
    [...new Set(value.split(',').map((tag) => tag.trim()).filter(Boolean))];

  #addTodo({ text, priority = 'medium', tags = [], ...rest }) {
    const todo = {
      id: crypto.randomUUID(),
      text,
      priority,
      completed: false,
      tags,
      ...rest,
    };

    this.#todos = [...this.#todos, todo];
    this.#save();
  }

  #toggle(id) {
    const idx = this.#todos.findIndex((todo) => todo.id === id);
    if (idx < 0) return;

    const target = this.#todos[idx];
    const updated = { ...target, completed: !target.completed };
    this.#todos = [...this.#todos.slice(0, idx), updated, ...this.#todos.slice(idx + 1)];
    this.#save();
  }

  #delete(id) {
    this.#todos = this.#todos.filter((todo) => todo.id !== id);
    this.#save();
  }

  *#filteredTodos() {
    for (const todo of this.#todos) {
      if (this.#filter === FILTER.ACTIVE && todo.completed) continue;
      if (this.#filter === FILTER.COMPLETED && !todo.completed) continue;
      yield todo;
    }
  }

  #renderBadges({ priority, tags = [] }) {
    const priorityBadge = `<span class="badge priority-${priority}">우선순위: ${PRIORITY_LABEL[priority] ?? '중간'}</span>`;
    const tagBadges = tags.map((tag) => `<span class="badge">#${tag}</span>`).join('');
    return `${priorityBadge}${tagBadges}`;
  }

  render() {
    const { list, meta } = this.#els;

    const visibleTodos = [...this.#filteredTodos()];
    const allCompleted = this.#todos.length > 0 && this.#todos.every(({ completed }) => completed);
    const hasHighPriority = this.#todos.some(({ priority }) => priority === 'high');

    list.innerHTML = visibleTodos
      .map(({ id, text, completed, ...rest }) => `
        <li class="todo-item ${completed ? 'completed' : ''}" data-id="${id}">
          <input type="checkbox" ${completed ? 'checked' : ''} aria-label="완료 여부" />
          <div class="todo-main">
            <span class="todo-title">${text}</span>
            <div class="badges">${this.#renderBadges(rest)}</div>
          </div>
          <button class="delete">삭제</button>
        </li>
      `)
      .join('');

    meta.textContent = `남은 할 일: ${this.#activeCount} · 완료율: ${this.#completionRate}%${
      allCompleted ? ' · 모두 완료!' : ''
    }${hasHighPriority ? ' · 높은 우선순위 존재' : ''}`;
  }
}

const els = {
  form: $('#todo-form'),
  input: $('#todo-input'),
  prioritySelect: $('#priority-select'),
  tagInput: $('#tag-input'),
  list: $('#todo-list'),
  meta: $('#todo-meta'),
  clearButton: $('#clear-completed'),
  filterButtons: $$('.filter'),
};

TodoApp.create(els);
