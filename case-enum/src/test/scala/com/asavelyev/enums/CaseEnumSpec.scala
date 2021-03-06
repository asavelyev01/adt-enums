package com.asavelyev.enums

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec



class CaseEnumSpec extends AnyWordSpec with Matchers {
  "CaseEnum" should {
    "Should find enum member by name" in {
      CaseEnum[TestEnum].fromString("EnumMemberTwo").get should be theSameInstanceAs TestEnum.EnumMemberTwo
      CaseEnum[TestEnum].fromString("EnumMemberOne").get should be theSameInstanceAs TestEnum.EnumMemberOne
    }

    "should provide enum members list" in {
      CaseEnum[TestEnum].all should contain theSameElementsAs Seq(TestEnum.EnumMemberOne, TestEnum.EnumMemberTwo)
    }

    "should report error for non-static descendant" in {
      the[IllegalStateException] thrownBy CaseEnum[TestEnumWithClass].all should have message
        s"Malformed enum $pckg.TestEnumWithClass: " +
          "descendant `class Free` is not an object. " +
          "Enum should be statically accessible sealed type with `case object` descendants."
    }

    "should report error for non-sealed type" in {
      the[IllegalStateException] thrownBy CaseEnum[NonSealedTestEnum].all should have message
        s"Malformed enum $pckg.NonSealedTestEnum: " +
          "not sealed. " +
          "Enum should be statically accessible sealed type with `case object` descendants."
    }

    "should report error for path-dependent type" in {
      the[IllegalStateException] thrownBy {
        sealed trait InnerEnum
        object InnerEnum extends CaseEnumCompanion[InnerEnum] {

          case object One extends InnerEnum

        }
        CaseEnum[InnerEnum].all
      } should have message "Malformed enum InnerEnum: " +
        "non-static, probably inner 'path-dependent', symbol. " +
        "Enum should be statically accessible sealed type with `case object` descendants."
    }

  }

  private def pckg ={
    getClass.getPackage.getName
  }
}

sealed trait TestEnum

object TestEnum extends CaseEnumCompanion[TestEnum] {

  case object EnumMemberOne extends TestEnum

  case object EnumMemberTwo extends TestEnum

}

trait NonSealedTestEnum

object NonSealedTestEnum extends CaseEnumCompanion[NonSealedTestEnum] {

  case object EnumMemberOne extends NonSealedTestEnum

}

sealed trait TestEnumWithClass

object TestEnumWithClass extends CaseEnumCompanion[TestEnumWithClass] {

  case object EnumMemberOne extends TestEnumWithClass

  case object EnumMemberTwo extends TestEnumWithClass

  class Free extends TestEnumWithClass

}
