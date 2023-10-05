import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {
    @GetMapping("/")
    fun homePage(model: Model): String {
        return "index" // Nome do arquivo HTML (sem a extensão .html)
    }
}
