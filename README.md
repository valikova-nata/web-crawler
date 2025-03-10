Вот полный текст в формате разметки для `README.md`:

# Web Crawler API

## Принципы SOLID

Этот проект реализует архитектуру с соблюдением принципов SOLID, что помогает создавать легко расширяемый и поддерживаемый код.

### S - **Single Responsibility Principle** (Принцип Единой Ответственности)
Каждый класс и метод в проекте отвечает за одну задачу:
- **`JsoupTitleFetcher`**: отвечает за извлечение заголовков страниц с помощью библиотеки Jsoup.
- **`TitleService`**: отвечает за получение заголовков с нескольких URL-адресов.

Пример:
```scala
class JsoupTitleFetcher(implicit ec: ExecutionContext) extends TitleFetcher {
  override def fetchTitle(url: String): Future[PageTitleResult] = Future {
    val doc = Jsoup.connect(url).get()
    val title = Option(doc.select("title").first()).map(_.text()).getOrElse("[No title]")
    PageTitleResult(url, title, s"/api/v1/pages/titles?url=$url")
  }
}
```

### O - **Open/Closed Principle** (Принцип Открытости/Закрытости)
Система открыта для расширения, но закрыта для изменения. Например, если необходимо использовать другой способ получения заголовков, можно создать новую реализацию интерфейса `TitleFetcher`, не изменяя существующий код.

Пример:
```scala
trait TitleFetcher {
  def fetchTitle(url: String): Future[PageTitleResult]
}
```

### L - **Liskov Substitution Principle** (Принцип Подстановки Лисков)
Классы и интерфейсы могут быть заменены их подтипами без нарушения логики программы. Все реализации `TitleFetcher` могут быть использованы в `TitleService` без изменения функциональности.

Пример:
```scala
class TitleService(titleFetcher: TitleFetcher)(implicit ec: ExecutionContext) {
  def fetchTitles(urls: Seq[String]): Future[Seq[PageTitleResult]] = {
    Future.sequence(urls.map(titleFetcher.fetchTitle))
  }
}
```

### I - **Interface Segregation Principle** (Принцип Разделения Интерфейсов)
Интерфейсы не содержат лишних методов, которые могут быть не нужны в реализации. Интерфейс `TitleFetcher` имеет только один метод — `fetchTitle`, который нужен для извлечения заголовков.

Пример:
```scala
trait TitleFetcher {
  def fetchTitle(url: String): Future[PageTitleResult]
}
```

### D - **Dependency Inversion Principle** (Принцип Инверсии Зависимостей)
Высокоуровневые компоненты зависят от абстракций, а не от конкретных реализаций. В `TitleService` используется абстракция `TitleFetcher`, что позволяет легко изменять способ получения заголовков, не изменяя код сервиса.

Пример:
```scala
class TitleService(titleFetcher: TitleFetcher)(implicit ec: ExecutionContext) {
  def fetchTitles(urls: Seq[String]): Future[Seq[PageTitleResult]] = {
    Future.sequence(urls.map(titleFetcher.fetchTitle))
  }
}
```

---

## Зрелость REST API

Мы придерживаемся принципов зрелости REST API, чтобы обеспечить ясность, эффективность и предсказуемость взаимодействия с нашим сервисом.

### HTTP Метод
- **POST** используется для получения данных, что соответствует операциям, не изменяющим состояние сервера. Это идеальный выбор для получения информации о заголовках страниц, не внося изменений в ресурс.

### Формат данных
- Взаимодействие с API осуществляется через **JSON** формат, что является стандартом для REST API и упрощает интеграцию с другими системами.

### Статусы HTTP
- Респонсы API описаны с использованием стандартных HTTP статусов:
  - **200 OK** — успешный запрос.
  - **500 InternalServerError** — ошибка на сервере, например, при получении заголовков.

Пример:
```scala
onComplete(titleFutures) {
  case Success(results) => complete(results)
  case Failure(_)       => complete(StatusCodes.InternalServerError)
}
```

### Описание ресурсов
- Ресурсы API описаны с понятными и логичными URL:
  - **`/api/v1/pages/titles`** — запросы для получения заголовков с нескольких URL.

---

## Асинхронность и производительность

### Асинхронность
- Все операции с удалёнными ресурсами (например, извлечение заголовков) выполняются **асинхронно** с использованием **`Future`**, что позволяет эффективно обрабатывать несколько URL одновременно.

Пример:
```scala
Future.sequence(urls.map(titleFetcher.fetchTitle))
```

### Управление потоками
- Для управления асинхронными операциями используется **`ExecutionContext`** и **Akka**, что делает систему масштабируемой и продуктивной.

Пример:
```scala
implicit val executionContext: ExecutionContext = system.dispatcher
```

Система использует **Akka ActorSystem** для распределения задач между потоками и управления асинхронными операциями, что обеспечивает высокую производительность при работе с большим количеством запросов.

---

### Запуск сервера

Чтобы запустить сервер, выполните команду:

```bash
sbt run
```

Сервер будет доступен по адресу:

```
http://localhost:8030/
```

---

Этот проект реализует принципы SOLID и зрелости REST API, что делает его легко поддерживаемым, расширяемым и масштабируемым.

### Тестирование

Проект включает тесты, которые можно запускать с помощью команды:

```bash
sbt test
```