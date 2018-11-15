package com.veon.scalalibs.enums.argonautcodec

import com.veon.scalalibs.enums.CaseEnumCompanion
import org.scalatest.Matchers
import org.scalatest.WordSpec

class EnumCodecSpec extends WordSpec with Matchers {

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
        s"""{"$key": "EnumMemberFive"}""".decodeEither[Map[String, TestJsonEnum]] should matchPattern {
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
