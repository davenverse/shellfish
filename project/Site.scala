import laika.sbt.LaikaConfig
import laika.config.*

object Site {

  private object SelectionsConfig {

    val apiSelections = SelectionConfig(
      "api-style",
      ChoiceConfig("syntax", "Syntax"),
      ChoiceConfig("static", "Static"),
      ChoiceConfig("fs2", "Fs2")
    )

  }

  object SiteConfig {

    val laikaConfig = LaikaConfig.defaults
      .withConfigValue(
        Selections(SelectionsConfig.apiSelections)
      )

  }

}
