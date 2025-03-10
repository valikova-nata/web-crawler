import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import org.jsoup.Jsoup
import spray.json._
import scala.util.{Failure, Success}

// Протокол сериализации для страницы
// Используем spray-json для серилизации и десерилизации данных
object JsonProtocol extends DefaultJsonProtocol {
  // Формат для преобразования PageTitleResult в JSON и наоборот
  implicit val resultFormat: RootJsonFormat[PageTitleResult] = jsonFormat3(PageTitleResult)
}

// Кейс-класс для представления результата (URL, заголовок страницы и self-ссылка)
// Это помогает создать структурированную и понятную модель данных, которая будет использоваться в REST API
case class PageTitleResult(url: String, title: String, self: String)

// Интерфейс для получения заголовков страниц (соблюдается принцип Dependency Inversion - DIP)
// Заголовок страницы можно извлечь разными способами, например, используя разные библиотеки
trait TitleFetcher {
  // Метод для получения заголовка страницы по URL
  def fetchTitle(url: String): Future[PageTitleResult]
}

// Реализация интерфейса TitleFetcher с использованием библиотеки Jsoup (соблюдается принцип Open/Closed - OCP)
// В данном случае мы создаем компонент для извлечения заголовков с веб-страниц
class JsoupTitleFetcher(implicit ec: ExecutionContext) extends TitleFetcher {
  override def fetchTitle(url: String): Future[PageTitleResult] = Future {
    try {
      // Загружаем HTML-документ по URL с помощью Jsoup
      val doc = Jsoup.connect(url).get()

      // Извлекаем текст из тега <title>
      val title = Option(doc.select("title").first()).map(_.text()).getOrElse("[No title]")

      // Возвращаем результат, включая URL и сам заголовок
      PageTitleResult(url, title, s"/api/v1/pages/titles?url=$url")
    } catch {
      // В случае ошибки возвращаем строку "[Error fetching title]" как заголовок
      // Это гарантирует, что API будет возвращать информацию о проблемах с запросом
      case _: Exception => PageTitleResult(url, "[Error fetching title]", s"/api/v1/pages/titles?url=$url")
    }
  }
}

// Сервис для обработки логики извлечения заголовков (соблюдается принцип Single Responsibility - SRP)
// Это разделяет бизнес-логику извлечения данных (fetching) и логику работы API
class TitleService(titleFetcher: TitleFetcher)(implicit ec: ExecutionContext) {
  // Метод для извлечения заголовков нескольких страниц
  // Используется Future.sequence для параллельного выполнения асинхронных запросов
  def fetchTitles(urls: Seq[String]): Future[Seq[PageTitleResult]] = {
    Future.sequence(urls.map(titleFetcher.fetchTitle))
  }
}

object WebCrawler extends App {
  import JsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  // Создаем необходимые элементы для работы с Akka HTTP
  implicit val system: ActorSystem = ActorSystem("web-crawler")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher // Используем Dispatcher Akka для управления потоками

  // Инициализируем необходимые компоненты: фетчер заголовков и сервис для работы с ними
  val titleFetcher = new JsoupTitleFetcher()
  val titleService = new TitleService(titleFetcher)

  // Определяем маршрут для обработки API-запросов
  val route = pathPrefix("api" / "v1" / "pages" / "titles") {
    post {
      entity(as[String]) { body =>
        // Разбиваем входной текст на URL-адреса, получаем заголовки и возвращаем их в формате JSON
        val urls = body.split("\n").map(_.trim).filter(_.nonEmpty)
        val titleFutures = titleService.fetchTitles(urls)

        // Обрабатываем результаты асинхронных запросов
        // Используем onComplete для обработки успешных или неудачных запросов
        onComplete(titleFutures) {
          case Success(results) => complete(results) // Возвращаем результаты в случае успеха
          case Failure(_)       => complete(StatusCodes.InternalServerError) // В случае ошибки отправляем код 500
        }
      }
    }
  }

  // Запускаем сервер на порту 8030
  // Это основная точка взаимодействия с клиентом
  Http().newServerAt("localhost", 8030).bind(route)
  println("Server running at http://localhost:8030/")
}
