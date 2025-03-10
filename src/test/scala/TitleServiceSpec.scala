import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// Класс для тестирования TitleService
class TitleServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {

  // Тестируем поведение метода fetchTitles
  "TitleService" should {

    // Тест 1: Проверка успешного получения заголовков для списка URL
    "fetch titles for a list of URLs" in {
      // Создаем mock для TitleFetcher
      val mockFetcher = mock(classOf[TitleFetcher])
      
      // Настроим mock так, чтобы при вызове fetchTitle с любым URL возвращался успешный результат
      when(mockFetcher.fetchTitle(any[String])) thenReturn Future.successful(
        PageTitleResult("https://test.com", "Test Title", "/api/v1/pages/titles?url=https://test.com")
      )

      // Создаем экземпляр TitleService с mockFetcher
      val service = new TitleService(mockFetcher)
      
      // Вызываем метод fetchTitles с тестовым URL
      val result = service.fetchTitles(Seq("https://test.com"))

      // Проверяем результат
      whenReady(result) { titles =>
        // Проверяем, что вернулось одно значение
        titles should have size 1
        
        // Проверяем, что заголовок равен "Test Title"
        titles.head.title shouldBe "Test Title"
      }
    }

    // Тест 2: Проверка обработки ошибок при получении заголовков
    "handle errors when fetching titles" in {
      // Создаем mock для TitleFetcher
      val mockFetcher = mock(classOf[TitleFetcher])
      
      // Настроим mock так, чтобы при вызове fetchTitle с любым URL возвращался результат с ошибкой
      when(mockFetcher.fetchTitle(any[String])) thenReturn Future.successful(
        PageTitleResult("https://fail.com", "[Error fetching title]", "/api/v1/pages/titles?url=https://fail.com")
      )

      // Создаем экземпляр TitleService с mockFetcher
      val service = new TitleService(mockFetcher)
      
      // Вызываем метод fetchTitles с тестовым URL, который приведет к ошибке
      val result = service.fetchTitles(Seq("https://fail.com"))

      // Проверяем результат
      whenReady(result) { titles =>
        // Проверяем, что заголовок равен ожидаемой ошибке
        titles.head.title shouldBe "[Error fetching title]"
      }
    }
  }
}
