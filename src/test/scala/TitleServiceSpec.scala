import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TitleServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {
  
  "TitleService" should {
    "fetch titles for a list of URLs" in {
      val mockFetcher = mock(classOf[TitleFetcher])
      when(mockFetcher.fetchTitle(any[String])) thenReturn Future.successful(PageTitleResult("https://test.com", "Test Title", "/api/v1/pages/titles?url=https://test.com"))

      val service = new TitleService(mockFetcher)
      val result = service.fetchTitles(Seq("https://test.com"))

      whenReady(result) { titles =>
        titles should have size 1
        titles.head.title shouldBe "Test Title"
      }
    }
    
    "handle errors when fetching titles" in {
      val mockFetcher = mock(classOf[TitleFetcher])
      when(mockFetcher.fetchTitle(any[String])) thenReturn Future.successful(PageTitleResult("https://fail.com", "[Error fetching title]", "/api/v1/pages/titles?url=https://fail.com"))

      val service = new TitleService(mockFetcher)
      val result = service.fetchTitles(Seq("https://fail.com"))

      whenReady(result) { titles =>
        titles.head.title shouldBe "[Error fetching title]"
      }
    }
  }
}
