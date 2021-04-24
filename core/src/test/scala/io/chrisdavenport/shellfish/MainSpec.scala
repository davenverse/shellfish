package io.chrisdavenport.shellfish

import munit.CatsEffectSuite
import cats.effect._

class MainSpec extends CatsEffectSuite {

  test("Main should exit succesfully") {
    assert(clue(1) == clue(1))
  }

}
