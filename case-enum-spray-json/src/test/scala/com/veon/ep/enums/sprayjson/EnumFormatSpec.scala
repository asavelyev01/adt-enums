package com.veon.ep.enums.sprayjson

import com.veon.ep.enums.CaseEnumCompanion
import org.scalatest.Matchers
import org.scalatest.WordSpec

trait YourLife { override val toString = getClass.getSimpleName.replace("$", "") }
case object Laughing extends YourLife
case object QuitSmocking extends YourLife
case object EatingHealthy extends YourLife

class EnumFormatSpec extends WordSpec with Matchers {

  "enumFormat" when {
    "using enumFormat" should {

      import spray.json._

      implicit val formatter = enumFormat[YourLife](Laughing, QuitSmocking)

      "produce proper json" in {
        (Laughing: YourLife).toJson shouldBe JsString("Laughing")
        (QuitSmocking: YourLife).toJson shouldBe JsString("QuitSmocking")
      }

      "convert from proper json" in {
        JsString("Laughing").convertTo[YourLife] shouldBe Laughing
        JsString("QuitSmocking").convertTo[YourLife] shouldBe QuitSmocking
      }

      "produce proper json from CaseEnum" in {
        TestJsonEnum.EnumMemberOne.asInstanceOf[TestJsonEnum].toJson shouldBe JsString(s"${TestJsonEnum.EnumMemberOne}")
      }

      "convert from proper json to CaseEnum" in {
        JsString("EnumMemberTwo").convertTo[TestJsonEnum] shouldBe TestJsonEnum.EnumMemberTwo
      }
    }

    "using enumFormatWithDefaults" should {

      import spray.json._

      implicit val formatter = enumFormatWithDefault[TestEnumWithUnknown](TestEnumWithUnknown.Unknown)

      import TestEnumWithUnknown._

      "produce proper json" in {
        (First: TestEnumWithUnknown).toJson shouldBe JsString("First")
        (Second: TestEnumWithUnknown).toJson shouldBe JsString("Second")
      }

      "convert from proper json" in {
        JsString("First").convertTo[TestEnumWithUnknown] shouldBe First
        JsString("Second").convertTo[TestEnumWithUnknown] shouldBe Second
      }

      "fallback to default on parsing" in {
        JsString("RANDOM").convertTo[TestEnumWithUnknown] shouldBe Unknown
      }
    }
  }
}

sealed trait TestJsonEnum

object TestJsonEnum extends CaseEnumCompanion[TestJsonEnum] {

  case object EnumMemberOne extends TestJsonEnum

  case object EnumMemberTwo extends TestJsonEnum
}

sealed trait TestEnumWithUnknown

object TestEnumWithUnknown extends CaseEnumCompanion[TestEnumWithUnknown] {
  case object First extends TestEnumWithUnknown
  case object Second extends TestEnumWithUnknown
  case object Unknown extends TestEnumWithUnknown
}
