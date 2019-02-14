package com.asavelyev.enums.slick

import com.asavelyev.enums.CaseEnum
import slick.jdbc.JdbcProfile

import scala.reflect.ClassTag

trait EnumColumnTypes {
  val profile: JdbcProfile

  import profile.api._

  implicit def enumColumnType[E: CaseEnum : ClassTag]: BaseColumnType[E] ={
    val enum: CaseEnum[E] = CaseEnum[E]
    MappedColumnType.base[E, String](String.valueOf, (s: String) => {
      enum.fromString(s).getOrElse(throw new IllegalArgumentException(s"Unrecognizable value $s in $enum"))
    })
  }
}
