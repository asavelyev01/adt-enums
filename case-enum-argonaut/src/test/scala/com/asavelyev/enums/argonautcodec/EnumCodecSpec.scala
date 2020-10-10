package com.asavelyev.enums.argonautcodec

import com.asavelyev.enums.CaseEnumCompanion
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers


class EnumCodecSpec extends AnyWordSpec with Matchers {

  "enumFormat" when {
    "using enumFormat" should {

      import argonaut._
      import Argonaut._

      "produce proper json from CaseEnum" in {
        TestJsonEnum.EnumMemberOne.asInstanceOf[TestJsonEnum].asJson shouldBe jString(s"${TestJsonEnum.EnumMemberOne}")
      }

      "convert from proper json to CaseEnum" in {
        val `key` = "key"
        s"""{"$key": "${TestJsonEnum.EnumMemberTwo}"}""".decodeEither[Map[String, TestJsonEnum]] shouldBe Right(
          Map(key -> TestJsonEnum.EnumMemberTwo)
        )
      }

      "report bad enum string" in {
        val `key` = "key"
        s"""{"$key": "EnumMemberFive"}""".decodeEither[Map[String, TestJsonEnum]] should matchPattern{
          case Left(errorMsg: String) if errorMsg.startsWith("Enum value EnumMemberFive doesn't exist") =>
        }
      }
    }
  }
}

sealed trait TestJsonEnum

object TestJsonEnum extends CaseEnumCompanion[TestJsonEnum] {

  case object EnumMemberOne extends TestJsonEnum

  case object EnumMemberTwo extends TestJsonEnum

}
